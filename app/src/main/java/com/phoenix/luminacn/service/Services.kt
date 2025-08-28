package com.phoenix.luminacn.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.phoenix.luminacn.R
import com.phoenix.luminacn.constructors.AccountManager
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.model.CaptureModeModel
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.relay.LuminaRelay
import com.project.lumina.relay.LuminaRelaySession
import com.project.lumina.relay.address.LuminaAddress
import com.project.lumina.relay.definition.Definitions
import com.project.lumina.relay.listener.AutoCodecPacketListener
import com.project.lumina.relay.listener.EncryptedLoginPacketListener
import com.project.lumina.relay.listener.GamingPacketHandler
import com.project.lumina.relay.listener.XboxLoginPacketListener
import com.project.lumina.relay.util.XboxIdentityTokenCacheFileSystem
import com.project.lumina.relay.util.captureLuminaRelay
import android.app.ActivityManager
import android.graphics.PixelFormat
import android.view.WindowManager
import com.phoenix.luminacn.game.module.impl.visual.EspElement
import com.phoenix.luminacn.remlink.TerminalViewModel
import com.phoenix.luminacn.shiyi.RenderOverlayView
import java.io.File
import kotlin.concurrent.thread

/**
 * Android Foreground Service to handle the CaptureMode functionality
 */
class Services : Service() {

    companion object {
        const val ACTION_CAPTURE_START = "com.project.lumina.relay.capture.start"
        const val ACTION_CAPTURE_STOP = "com.project.lumina.relay.capture.stop"
        private const val NOTIFICATION_CHANNEL_ID = "lumina_capture_channel"
        private const val NOTIFICATION_ID = 1001

        private val handler = Handler(Looper.getMainLooper())
        private var luminaRelay: LuminaRelay? = null
        private var thread: Thread? = null
        var isActive by mutableStateOf(false)
        var RemisOnline by mutableStateOf(false)
        var RemInGame by mutableStateOf(false)
        var isLaunchingMinecraft by mutableStateOf(false)

        private var renderView: RenderOverlayView? = null
        private var windowManager: WindowManager? = null

        fun toggle(context: Context, captureModeModel: CaptureModeModel) {
            if (!isActive) {
                val intent = Intent(ACTION_CAPTURE_START)
                intent.setPackage(context.packageName)
                context.startForegroundService(intent)
                TerminalViewModel.addTerminalLog("连接", "服务启动中...")
                return
            }

            val intent = Intent(ACTION_CAPTURE_STOP)
            intent.setPackage(context.packageName)
            context.startForegroundService(intent)
        }

        private fun on(context: Context, captureModeModel: CaptureModeModel) {
            if (thread != null) {
                return
            }

            val tokenCacheFile = File(context.cacheDir, "token_cache.json")

            isActive = true

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val currentActivity = activityManager.appTasks
                .flatMap { it.taskInfo.topActivity?.className?.let { listOf(it) } ?: emptyList() }
                .firstOrNull()
            RemisOnline = false

            if (!RemisOnline) {
                handler.post {
                    OverlayManager.show(context)
                }
            }

            setupOverlay(context)

            thread = thread(name = "LuminaRelayThread") {
                runCatching {
                    GameManager.loadConfig()
                }.exceptionOrNull()?.let {
                    it.printStackTrace()
                    context.toast("配置加载失败: ${it.message}")
                }

                runCatching {
                    Definitions.loadBlockPalette()
                }.exceptionOrNull()?.let {
                    it.printStackTrace()
                    context.toast("加载失败: ${it.message}")
                }

                val sessionEncryptor = if (AccountManager.currentAccount == null) {
                    EncryptedLoginPacketListener()
                } else {
                    AccountManager.currentAccount?.let { account ->
                        Log.e("LuminaRelay", "Logged in as ${account.remark}")
                        TerminalViewModel.addTerminalLog("连接", "作为 ${account.remark}")
                        TerminalViewModel.addTerminalLog("帮助", "输入 '!help' 来获取帮助.")
                        XboxLoginPacketListener({ account.refresh() }, account.platform).also {
                            it.tokenCache =
                                XboxIdentityTokenCacheFileSystem(tokenCacheFile, account.remark)
                        }
                    }
                }

                runCatching {
                    luminaRelay = captureLuminaRelay(
                        remoteAddress = LuminaAddress(
                            captureModeModel.serverHostName,
                            captureModeModel.serverPort
                        )
                    ) {
                        initModules(this)

                        listeners.add(AutoCodecPacketListener(this))
                        sessionEncryptor?.let {
                            it.luminaRelaySession = this
                            listeners.add(it)
                        }
                        listeners.add(GamingPacketHandler(this))
                    }
                }.exceptionOrNull()?.let {
                    it.printStackTrace()
                    context.toast("启动 LuminaRelay 时遇到错误: ${it.stackTraceToString()}")
                }
            }
        }

        private fun off() {
            thread(name = "LuminaRelayThread") {
                GameManager.saveConfig()
                isActive = false
                RemisOnline = false
                luminaRelay?.disconnect()
                thread?.interrupt()
                thread = null

                isLaunchingMinecraft = false

                handler.post {
                    OverlayManager.dismiss()
                    ConnectionInfoOverlay.dismiss()
                }

                removeOverlay()

                TerminalViewModel.addTerminalLog("连接", "服务已停止.")
            }
        }

        private fun Context.toast(message: String) {
            handler.post {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        private fun initModules(luminaRelaySession: LuminaRelaySession) {
            try {
                val session = NetBound(luminaRelaySession)
                luminaRelaySession.listeners.add(session)

                for (module in GameManager.elements) {
                    try {
                        module.session = session
                    } catch (e: Exception) {
                        Log.e("Services", "Failed to initialize session for module ${module.name}: ${e.message}")
                    }
                }

                TerminalViewModel.addTerminalLog("Connection", "Initializing Modules...")
            } catch (e: Exception) {
                Log.e("Services", "Failed to initialize modules: ${e.message}")
                TerminalViewModel.addTerminalLog("错误", "初始化模块时遇到错误: ${e.message}")
            }
        }

        private fun setupOverlay(context: Context) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alpha = 0.8f
                    flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    setFitInsetsTypes(0)
                    setFitInsetsSides(0)
                }
            }

            renderView = RenderOverlayView(context)
            EspElement.setRenderView(renderView!!)

            handler.post {
                try {
                    windowManager?.addView(renderView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                    context.toast("Failed to add overlay view: ${e.message}")
                }
            }
        }

        private fun removeOverlay() {
            renderView?.let { view ->
                windowManager?.removeView(view)
                renderView = null
            }
        }
    }

    private var isStoppedByUser = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Lumina Capture 服务正在运行"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CAPTURE_START -> {
                isStoppedByUser = false
                val captureModeModel = CaptureModeModel.from(
                    getSharedPreferences("game_settings", Context.MODE_PRIVATE)
                )
                on(applicationContext, captureModeModel)
            }
            ACTION_CAPTURE_STOP -> {
                isStoppedByUser = true
                off()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!isStoppedByUser && isActive) {
            val restartServiceIntent = Intent(applicationContext, Services::class.java)
            restartServiceIntent.setPackage(packageName)
            restartServiceIntent.action = ACTION_CAPTURE_START
            startService(restartServiceIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isStoppedByUser && isActive) {
            val restartServiceIntent = Intent(applicationContext, Services::class.java)
            restartServiceIntent.setPackage(packageName)
            restartServiceIntent.action = ACTION_CAPTURE_START
            startService(restartServiceIntent)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isActive || RemisOnline) return

        handler.post {
            OverlayManager.show(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Lumina Capture 服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "当 Lumina Capture 模式启用时保持活跃"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        TerminalViewModel.addTerminalLog("连接", "通知初始化成功.")
    }

    private fun createNotification(text: String) = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Lumina Capture")
        .setContentText(text)
        .setSmallIcon(R.drawable.img)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止",
            createPendingIntent(ACTION_CAPTURE_STOP)
        )
        .build()

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action)
        intent.setPackage(packageName)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getService(
            this,
            0,
            intent,
            flags
        )
    }
}