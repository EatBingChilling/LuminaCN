package com.phoenix.luminacn.shiyi

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.phoenix.luminacn.overlay.manager.OverlayWindow
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.constructors.GameManager

class NameTagOverlayService : Service() {

    private var nameTagOverlay: NameTagOverlay? = null
    private var isOverlayShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nameTagOverlay = NameTagOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showNameTagOverlay()
            ACTION_HIDE_OVERLAY -> hideNameTagOverlay()
            ACTION_STOP_SERVICE -> {
                hideNameTagOverlay()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showNameTagOverlay() {
        if (!isOverlayShown) {
            nameTagOverlay?.let { overlay ->
                try {
                    OverlayManager.showOverlayWindow(overlay)
                    isOverlayShown = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun hideNameTagOverlay() {
        if (isOverlayShown) {
            nameTagOverlay?.let { overlay ->
                try {
                    OverlayManager.dismissOverlayWindow(overlay)
                    isOverlayShown = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        hideNameTagOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.phoenix.luminacn.SHOW_NAMETAG_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.phoenix.luminacn.HIDE_NAMETAG_OVERLAY"
        const val ACTION_STOP_SERVICE = "com.phoenix.luminacn.STOP_NAMETAG_SERVICE"
    }
}

class NameTagOverlay : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN

            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START // 修复: Start -> START
            x = 0
            y = 0
            format = PixelFormat.TRANSLUCENT

            type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    NameTagRenderView(ctx).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        )
                        updateSession(GameManager.netBound)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.updateSession(GameManager.netBound)
                    view.requestLayout()
                }
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                // 清理资源
            }
        }
    }
}