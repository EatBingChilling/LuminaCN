package com.phoenix.luminacn.shiyi

import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow
import com.phoenix.luminacn.overlay.manager.OverlayManager

class RenderOverlay : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            // 完整的全屏触摸穿透设置
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            
            // 全屏设置
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            
            // 确保窗口类型正确
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            // 添加格式设置确保透明度
            format = android.graphics.PixelFormat.TRANSLUCENT
            
            // Android P+ 的刘海屏处理
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        val overlayInstance by lazy { RenderOverlay() }
        private var shouldShowOverlay = false
        private var currentSession: com.phoenix.luminacn.constructors.NetBound? = null

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

        fun setSession(session: com.phoenix.luminacn.constructors.NetBound?) {
            currentSession = session
        }
    }

    private fun getFullscreenSystemUIFlags(): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 使用新的API
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                // Android 4.4+
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            else -> {
                // 旧版本Android
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val context = LocalContext.current
        val systemUIFlags = remember { getFullscreenSystemUIFlags() }
        
        Box(modifier = Modifier.fillMaxSize()) {
            // 渲染层视图 - 需要session
            currentSession?.let { session ->
                AndroidView(
                    factory = { ctx ->
                        RenderLayerView(ctx, session).apply {
                            // 确保View透明且全屏
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            // 设置完整的全屏UI
                            systemUiVisibility = systemUIFlags
                            
                            // 确保View获得焦点以应用SystemUI设置
                            isFocusable = true
                            isFocusableInTouchMode = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // 强制重新布局以确保全屏
                        view.requestLayout()
                        // 重新应用SystemUI设置
                        view.systemUiVisibility = systemUIFlags
                        
                        // 强制进入全屏模式
                        view.post {
                            view.systemUiVisibility = systemUIFlags
                        }
                    }
                )
            }
            
            // ESP渲染视图 - 叠加在上层
            AndroidView(
                factory = { ctx ->
                    RenderOverlayView(ctx).apply {
                        // 确保View透明且全屏
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // 设置完整的全屏UI
                        systemUiVisibility = systemUIFlags
                        
                        // 确保View获得焦点以应用SystemUI设置
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // 强制重新布局以确保全屏
                    view.requestLayout()
                    // 重新应用SystemUI设置
                    view.systemUiVisibility = systemUIFlags
                    
                    // 强制进入全屏模式
                    view.post {
                        view.systemUiVisibility = systemUIFlags
                    }
                }
            )
        }

        // 确保在组件销毁时清理资源
        DisposableEffect(Unit) {
            onDispose {
                // 清理工作
            }
        }
    }
}