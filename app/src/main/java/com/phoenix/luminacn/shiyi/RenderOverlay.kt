package com.phoenix.luminacn.shiyi

import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow

class RenderOverlay : OverlayWindow() {

    // --- 关键改动：覆写父类的 layoutParams 以实现真正的全屏 ---
    override val layoutParams by lazy {
        // 使用一套干净、无冲突的全屏参数
        WindowManager.LayoutParams().apply {
            // 核心标志：允许触摸穿透，硬件加速，并允许绘制到屏幕边缘
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            // 确保窗口类型正确
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 关键：设置为 MATCH_PARENT 来填满屏幕
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0

            // 确保透明背景
            format = android.graphics.PixelFormat.TRANSLUCENT

            // 适配刘海屏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    // --- Companion Object 用于管理悬浮窗的显示和隐藏 ---
    companion object {
        val overlayInstance by lazy { RenderOverlay() }
        private var shouldShowOverlay = false
        // 你代码中原来的 session 逻辑可以按需保留
        // private var currentSession: com.phoenix.luminacn.constructors.NetBound? = null

        fun showOverlay() {
            if (shouldShowOverlay) {
                try {
                    OverlayManager.showOverlayWindow(overlayInstance)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun dismissOverlay() {
            try {
                OverlayManager.dismissOverlayWindow(overlayInstance)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (enabled) showOverlay() else dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        // fun setSession(session: com.phoenix.luminacn.constructors.NetBound?) {
        //     currentSession = session
        // }
    }
    
    // 辅助函数，用于设置沉浸式全屏模式
    private fun setImmersiveMode(view: View) {
        @Suppress("DEPRECATION")
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    RenderOverlayView(ctx).apply {
                        // 确保 View 背景透明
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // 在 update 回调中持续应用沉浸式模式，防止系统UI意外弹出
                    view.post { setImmersiveMode(view) }
                    // 手动请求重绘以启动/刷新渲染循环
                    view.invalidate()
                }
            )
        }
    }
}