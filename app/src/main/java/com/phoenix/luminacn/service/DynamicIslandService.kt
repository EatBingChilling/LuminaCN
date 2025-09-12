@file:OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
package com.phoenix.luminacn.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.text.ExperimentalTextApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt
import com.hud.test.modules.dynamicisland.DynamicIslandView
import com.phoenix.luminacn.phoenix.CompatDynamicIslandState
import com.phoenix.luminacn.phoenix.rememberCompatDynamicIslandState
import com.phoenix.luminacn.music.MusicObserver

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    fun performRestore(savedState: Bundle?) { savedStateRegistryController.performRestore(savedState) }
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}

class DynamicIslandService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private var dynamicIslandState: CompatDynamicIslandState? = null
    private lateinit var windowParams: WindowManager.LayoutParams
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lifecycleOwner = ServiceLifecycleOwner()
    
    // 用于控制首次显示的透明度，实现预热
    private var isWarmedUp = mutableStateOf(false)
    
    // 🆕 新增配置状态
    private var musicModeEnabled = mutableStateOf(true)
    private var hideWhenNoTasks = mutableStateOf(false)

    companion object {
        // 保持原有的常量不变
        const val ACTION_UPDATE_TEXT = "com.phoenix.luminacn.ACTION_UPDATE_TEXT"
        const val ACTION_UPDATE_Y_OFFSET = "com.phoenix.luminacn.ACTION_UPDATE_Y_OFFSET"
        const val ACTION_UPDATE_SCALE = "com.phoenix.luminacn.ACTION_UPDATE_SCALE"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_Y_OFFSET_DP = "extra_y_offset_dp"
        const val EXTRA_SCALE = "extra_scale"
        const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.phoenix.luminacn.ACTION_SHOW_NOTIFICATION_SWITCH"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_STATE = "extra_module_state"
        const val ACTION_SHOW_OR_UPDATE_PROGRESS = "com.phoenix.luminacn.ACTION_SHOW_OR_UPDATE_PROGRESS"
        const val EXTRA_IDENTIFIER = "extra_identifier"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_PROGRESS_VALUE = "extra_progress_value"
        
        // 音乐相关的常量
        const val ACTION_SHOW_OR_UPDATE_MUSIC = "com.phoenix.luminacn.ACTION_SHOW_OR_UPDATE_MUSIC"
        const val ACTION_REMOVE_TASK = "com.phoenix.luminacn.ACTION_REMOVE_TASK"
        const val EXTRA_PROGRESS_TEXT = "extra_progress_text"
        const val EXTRA_IS_MAJOR_UPDATE = "extra_is_major_update"
        
        // 图像数据的常量
        const val EXTRA_IMAGE_DATA = "extra_image_data"
        const val EXTRA_ALBUM_ART_DATA = "extra_album_art_data"
        
        // 🆕 新增配置相关常量
        const val ACTION_SET_MUSIC_MODE = "com.phoenix.luminacn.ACTION_SET_MUSIC_MODE"
        const val ACTION_SET_HIDE_WHEN_NO_TASKS = "com.phoenix.luminacn.ACTION_SET_HIDE_WHEN_NO_TASKS"
        const val EXTRA_MUSIC_MODE_ENABLED = "extra_music_mode_enabled"
        const val EXTRA_HIDE_WHEN_NO_TASKS = "extra_hide_when_no_tasks"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val isDarkTheme = isSystemInDarkTheme()
                val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { 
                    if (isDarkTheme) dynamicDarkColorScheme(this@DynamicIslandService) else dynamicLightColorScheme(this@DynamicIslandService) 
                } else { 
                    if (isDarkTheme) darkColorScheme() else lightColorScheme() 
                }
                
                // 动画化透明度，用于预热
                val alpha by animateFloatAsState(targetValue = if (isWarmedUp.value) 1.0f else 0.0f, label = "warmup")

                MaterialTheme(colorScheme = colorScheme) {
                    val state = rememberCompatDynamicIslandState(this@DynamicIslandService)
                    
                    // 监听Y位置变化，同步到WindowManager
                    LaunchedEffect(state.yPosition) {
                        windowParams.y = dpToPx(state.yPosition)
                        windowManager.updateViewLayout(composeView, windowParams)
                    }
                    
                    LaunchedEffect(state) { 
                        this@DynamicIslandService.dynamicIslandState = state 
                    }
                    
                    // 🆕 使用新的DynamicIslandView，支持隐藏模式
                    EnhancedDynamicIslandView(
                        state = state.getUnderlyingState(),
                        hideWhenNoTasks = hideWhenNoTasks.value,
                        modifier = Modifier.alpha(alpha)
                    )
                }
            }
        }
        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, 
            PixelFormat.TRANSLUCENT
        ).apply { 
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0 
        }
        windowManager.addView(composeView, windowParams)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        
        // 从SharedPreferences读取配置
        loadSettings()
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        musicModeEnabled.value = prefs.getBoolean("musicModeEnabled", true)
        hideWhenNoTasks.value = prefs.getBoolean("hideWhenNoTasks", false)
        
        // 如果音乐模式启用，启动音乐观察器
        if (musicModeEnabled.value) {
            startMusicObserver()
        }
    }
    
    private fun startMusicObserver() {
        try {
            MusicObserver.start(this)
            Log.d("DynamicIslandService", "Music observer started")
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Failed to start music observer", e)
        }
    }
    
    private fun stopMusicObserver() {
        try {
            MusicObserver.stop(this)
            Log.d("DynamicIslandService", "Music observer stopped")
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Failed to stop music observer", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 接收到任何指令后，将isWarmedUp设为true，让灵动岛可见
        if (!isWarmedUp.value) {
            isWarmedUp.value = true
        }

        intent ?: return START_STICKY
        when (intent.action) {
            ACTION_UPDATE_TEXT -> intent.getStringExtra(EXTRA_TEXT)?.let { text -> 
                dynamicIslandState?.updatePersistentText(text) 
            }
            
            ACTION_UPDATE_Y_OFFSET -> { 
                val yOffsetDp = intent.getFloatExtra(EXTRA_Y_OFFSET_DP, 0f)
                dynamicIslandState?.updateYPosition(yOffsetDp)
            }
            
            ACTION_UPDATE_SCALE -> { 
                val newScale = intent.getFloatExtra(EXTRA_SCALE, 1.0f)
                dynamicIslandState?.updateScale(newScale)
            }
            
            ACTION_SHOW_NOTIFICATION_SWITCH -> intent.getStringExtra(EXTRA_MODULE_NAME)?.let { name -> 
                val state = intent.getBooleanExtra(EXTRA_MODULE_STATE, false)
                dynamicIslandState?.addSwitch(name, name, state) 
            }
            
            ACTION_SHOW_OR_UPDATE_PROGRESS -> handleShowOrUpdateProgress(intent)
            
            ACTION_SHOW_OR_UPDATE_MUSIC -> handleShowOrUpdateMusic(intent)
            
            ACTION_REMOVE_TASK -> intent.getStringExtra(EXTRA_IDENTIFIER)?.let { identifier ->
                dynamicIslandState?.removeTask(identifier)
            }
            
            // 🆕 新增：处理配置更新
            ACTION_SET_MUSIC_MODE -> {
                val enabled = intent.getBooleanExtra(EXTRA_MUSIC_MODE_ENABLED, true)
                musicModeEnabled.value = enabled
                if (enabled) {
                    startMusicObserver()
                } else {
                    stopMusicObserver()
                }
            }
            
            ACTION_SET_HIDE_WHEN_NO_TASKS -> {
                val hide = intent.getBooleanExtra(EXTRA_HIDE_WHEN_NO_TASKS, false)
                hideWhenNoTasks.value = hide
            }
        }
        return START_STICKY
    }

    override fun onDestroy() { 
        super.onDestroy()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        stopMusicObserver()
        windowManager.removeView(composeView)
        serviceScope.cancel() 
    }
    
    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).roundToInt()
    
    private fun handleShowOrUpdateProgress(intent: Intent) { 
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
        val progress = intent.takeIf { it.hasExtra(EXTRA_PROGRESS_VALUE) }?.getFloatExtra(EXTRA_PROGRESS_VALUE, 0f)
        val duration = if (progress == null && intent.hasExtra(EXTRA_DURATION_MS)) { 
            intent.getLongExtra(EXTRA_DURATION_MS, 5000L) 
        } else { 
            null 
        }
        
        // 优先使用传递的图像数据，然后是资源ID
        val iconDrawable = when {
            intent.hasExtra(EXTRA_IMAGE_DATA) -> {
                intent.getByteArrayExtra(EXTRA_IMAGE_DATA)?.let { imageData ->
                    createDrawableFromByteArray(imageData)
                }
            }
            intent.getIntExtra(EXTRA_ICON_RES_ID, -1) != -1 -> {
                val resId = intent.getIntExtra(EXTRA_ICON_RES_ID, -1)
                runCatching { ContextCompat.getDrawable(this, resId) }.getOrNull()
            }
            else -> null
        }
        
        dynamicIslandState?.addOrUpdateProgress(identifier, title, subtitle, iconDrawable, progress, duration) 
    }
    
    private fun handleShowOrUpdateMusic(intent: Intent) {
        // 只有在音乐模式启用时才处理音乐任务
        if (!musicModeEnabled.value) {
            Log.d("DynamicIslandService", "Music mode disabled, ignoring music task")
            return
        }
        
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
        val progressText = intent.getStringExtra(EXTRA_PROGRESS_TEXT) ?: ""
        val progress = intent.getFloatExtra(EXTRA_PROGRESS_VALUE, 0f)
        val isMajorUpdate = intent.getBooleanExtra(EXTRA_IS_MAJOR_UPDATE, true)
        
        // 处理专辑封面数据
        val albumArt = intent.getByteArrayExtra(EXTRA_ALBUM_ART_DATA)?.let { albumArtData ->
            createDrawableFromByteArray(albumArtData)
        }
        
        dynamicIslandState?.addOrUpdateMusic(
            identifier = identifier,
            text = title,
            subtitle = subtitle,
            albumArt = albumArt,
            progressText = progressText,
            progress = progress,
            isMajorUpdate = isMajorUpdate
        )
    }
    
    /**
     * 将字节数组转换为BitmapDrawable
     */
    private fun createDrawableFromByteArray(byteArray: ByteArray): android.graphics.drawable.BitmapDrawable? {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            bitmap?.let { android.graphics.drawable.BitmapDrawable(resources, it) }
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Failed to decode image data", e)
            null
        }
    }
}