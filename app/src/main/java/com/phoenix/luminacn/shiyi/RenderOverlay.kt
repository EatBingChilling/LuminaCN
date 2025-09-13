package com.phoenix.luminacn.shiyi

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
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

    // --- THE CRITICAL FIX FOR LANDSCAPE FULLSCREEN ---
    // We override layoutParams to programmatically calculate the true screen size,
    // ignoring the system's incorrect assumptions for landscape overlays.
    override val layoutParams by lazy {
        val wm = OverlayManager.currentContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Get the real, physical screen dimensions.
        val realScreenWidth: Int
        val realScreenHeight: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val bounds = windowMetrics.bounds
            realScreenWidth = bounds.width()
            realScreenHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            val size = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(size)
            realScreenWidth = size.x
            realScreenHeight = size.y
        }

        WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // --- KEY CHANGE: Set exact pixel dimensions instead of MATCH_PARENT ---
            width = realScreenWidth
            height = realScreenHeight
            
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            format = android.graphics.PixelFormat.TRANSLUCENT
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

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

        // We can still use fillMaxSize here because our root window is now correctly sized.
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    RenderOverlayView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.post { setImmersiveMode(view) }
                    view.invalidate()
                }
            )
        }
    }
}