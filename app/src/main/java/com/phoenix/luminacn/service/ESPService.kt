package com.phoenix.luminacn.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import com.phoenix.luminacn.ui.ESPOverlayView

/**
 * A background service to manage the ESP floating window.
 * This service uses the WindowManager to display the ESPOverlayView on top of all other apps.
 */
class ESPService : Service() {

    private lateinit var windowManager: WindowManager
    private var espOverlayView: ESPOverlayView? = null

    override fun onBind(intent: Intent?): IBinder? {
        // This is a non-binding service.
        return null
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        espOverlayView = ESPOverlayView(this)

        // Define the parameters for the floating window.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // Width: fill the screen
            WindowManager.LayoutParams.MATCH_PARENT, // Height: fill the screen
            // Window type: This is crucial for displaying over other apps.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            // Window flags: These define the window's behavior.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or    // The window can't get focus (e.g., for keyboard input).
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or   // The window doesn't receive touch events; they pass through to the game. VERY IMPORTANT!
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,  // The window can extend over the entire screen.
            PixelFormat.TRANSLUCENT // The window's background is transparent.
        )

        try {
            // Add the view to the window manager. This makes it visible.
            windowManager.addView(espOverlayView, params)
            Log.i("ESPService", "ESP overlay view added to WindowManager.")
        } catch (e: Exception) {
            Log.e("ESPService", "Failed to add ESP overlay view to WindowManager.", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // It's very important to remove the view when the service is destroyed to avoid window leaks.
        if (espOverlayView != null) {
            try {
                windowManager.removeView(espOverlayView)
                Log.i("ESPService", "ESP overlay view removed from WindowManager.")
            } catch (e: Exception) {
                Log.e("ESPService", "Failed to remove ESP overlay view from WindowManager.", e)
            }
        }
    }
}