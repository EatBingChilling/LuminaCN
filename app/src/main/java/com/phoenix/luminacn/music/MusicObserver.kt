package com.phoenix.luminacn.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.phoenix.luminacn.api.MusicAction
import com.phoenix.luminacn.api.showDynamicIslandMusic
import com.phoenix.luminacn.api.updateDynamicIslandMusicProgress
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

private const val TAG = "MusicObserver"
private const val MUSIC_IDENTIFIER = "global_music_player"

/**
 * 音乐观察者 - 外部调用入口
 * 负责检查权限和启动后台监听服务
 */
object MusicObserver {
    
    private var isEnabled = true
    
    /**
     * 设置音乐观察器是否启用
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "Music observer enabled: $enabled")
    }
    
    /**
     * 检查音乐观察器是否启用
     */
    fun isEnabled(): Boolean = isEnabled

    fun checkAndRequestPermissions(
        activity: ComponentActivity,
        notificationPermissionLauncher: ActivityResultLauncher<String>,
        onPermissionsGranted: () -> Unit
    ) {
        val hasPostNotificationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 以下不需要此权限
        }

        val hasNotificationListenerPerm = isNotificationListenerEnabled(activity)

        when {
            !hasNotificationListenerPerm -> {
                Log.w(TAG, "Notification Listener permission not granted. Opening settings.")
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                activity.startActivity(intent)
            }
            !hasPostNotificationPerm -> {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            else -> {
                Log.d(TAG, "All permissions are granted.")
                onPermissionsGranted()
            }
        }
    }

    fun start(context: Context) {
        if (!isNotificationListenerEnabled(context)) {
            Log.e(TAG, "Cannot start service: Notification Listener permission is not granted.")
            return
        }

        rebindNotificationListenerService(context)

        val intent = Intent(context, MusicObserverService::class.java)
        try {
            context.startService(intent)
            Log.i(TAG, "MusicObserverService start command sent.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MusicObserverService", e)
        }
    }

    fun stop(context: Context) {
        try {
            val componentName = ComponentName(context, MusicObserverService::class.java)
            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.i(TAG, "MusicObserverService component disabled.")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling service component", e)
        }
        
        context.stopService(Intent(context, MusicObserverService::class.java))
        Log.i(TAG, "MusicObserverService stop command sent.")
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) ?: false
    }
    
    private fun rebindNotificationListenerService(context: Context) {
        try {
            val componentName = ComponentName(context, MusicObserverService::class.java)
            val packageManager = context.packageManager
            
            Log.d(TAG, "Rebinding Notification Listener Service...")
            
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Rebind complete.")
        } catch (e: Exception) {
            Log.e(TAG, "Could not rebind notification listener service", e)
        }
    }
}

/**
 * 后台服务，用于监听系统媒体会话变化
 */
class MusicObserverService : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active media sessions changed.")
        handleActiveSessionsChanged(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Playback state changed: ${state?.state}")
            updateMusicInfo()
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            Log.d(TAG, "Metadata changed: ${metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)}")
            updateMusicInfo()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification Listener connected. Initializing media session listener.")
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            val componentName = ComponentName(this, this.javaClass)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            handleActiveSessionsChanged(mediaSessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to add session listener. Is permission granted?", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification Listener disconnected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MusicObserverService is being destroyed.")
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing session listener", e)
        }
        activeController?.unregisterCallback(controllerCallback)
        serviceScope.cancel()
    }
    
    private fun handleActiveSessionsChanged(controllers: List<MediaController>?) {
        val newController = controllers?.find {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        if (newController?.sessionToken == activeController?.sessionToken && activeController != null) {
            updateMusicInfo()
            return
        }

        activeController?.unregisterCallback(controllerCallback)
        activeController = newController
        activeController?.registerCallback(controllerCallback)
        Log.d(TAG, "Switched to new media controller: ${newController?.packageName}")

        updateMusicInfo()
    }

    private fun updateMusicInfo() {
        // 🆕 检查音乐模式是否启用
        val prefs = getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val musicModeEnabled = prefs.getBoolean("musicModeEnabled", true)
        
        if (!musicModeEnabled) {
            Log.d(TAG, "Music mode disabled, stopping music info updates")
            showDynamicIslandMusic(
                identifier = MUSIC_IDENTIFIER,
                title = "", subtitle = "", albumArt = null, progressText = "", progress = 0f,
                action = MusicAction.STOP
            )
            stopProgressUpdater()
            return
        }
        
        val controller = activeController
        val metadata = controller?.metadata
        val playbackState = controller?.playbackState

        if (controller == null || metadata == null || playbackState == null) {
            Log.d(TAG, "No active media session. Stopping UI.")
            showDynamicIslandMusic(
                identifier = MUSIC_IDENTIFIER,
                title = "", subtitle = "", albumArt = null, progressText = "", progress = 0f,
                action = MusicAction.STOP
            )
            stopProgressUpdater()
            return
        }

        val state = playbackState.state
        if (state == PlaybackState.STATE_STOPPED || state == PlaybackState.STATE_NONE) {
            Log.d(TAG, "Media session stopped. Stopping UI.")
            showDynamicIslandMusic(
                identifier = MUSIC_IDENTIFIER,
                title = "", subtitle = "", albumArt = null, progressText = "", progress = 0f,
                action = MusicAction.STOP
            )
            stopProgressUpdater()
            return
        }
        
        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val albumArtBitmap = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
        val albumArtDrawable = albumArtBitmap?.let { BitmapDrawable(resources, it) }
        val duration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
        
        showDynamicIslandMusic(
            identifier = MUSIC_IDENTIFIER,
            title = title,
            subtitle = artist,
            albumArt = albumArtDrawable,
            progressText = getProgressText(playbackState, duration),
            progress = getProgress(playbackState, duration),
            action = MusicAction.UPDATE
        )

        if (state == PlaybackState.STATE_PLAYING) {
            startProgressUpdater(duration)
        } else {
            stopProgressUpdater()
        }
    }
    
    private fun startProgressUpdater(duration: Long) {
        // 🆕 再次检查音乐模式状态，防止在进度更新过程中被禁用
        val prefs = getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val musicModeEnabled = prefs.getBoolean("musicModeEnabled", true)
        
        if (!musicModeEnabled || duration <= 0) {
            stopProgressUpdater()
            return
        }
        
        if (progressJob?.isActive == true) return

        progressJob = serviceScope.launch {
            while (isActive) {
                // 在循环中持续检查音乐模式状态
                val currentPrefs = getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
                val currentMusicModeEnabled = currentPrefs.getBoolean("musicModeEnabled", true)
                
                if (!currentMusicModeEnabled) {
                    Log.d(TAG, "Music mode disabled during progress update, stopping")
                    break
                }
                
                activeController?.playbackState?.let { state ->
                    updateDynamicIslandMusicProgress(
                        identifier = MUSIC_IDENTIFIER,
                        progressText = getProgressText(state, duration),
                        progress = getProgress(state, duration)
                    )
                }
                delay(1000)
            }
        }
        Log.d(TAG, "Progress updater started.")
    }

    private fun stopProgressUpdater() {
        progressJob?.cancel()
        progressJob = null
        Log.d(TAG, "Progress updater stopped.")
    }

    private fun getProgressText(playbackState: PlaybackState, duration: Long): String {
        val currentPosition = playbackState.position
        val formattedPosition = formatDuration(currentPosition)
        val formattedDuration = formatDuration(duration)
        return if (duration > 0) "$formattedPosition / $formattedDuration" else formattedPosition
    }

    private fun getProgress(playbackState: PlaybackState, duration: Long): Float {
        return if (duration > 0) {
            (playbackState.position.toFloat() / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "--:--"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}