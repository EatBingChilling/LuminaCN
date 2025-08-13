package com.project.lumina.client.overlay.mods

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.project.lumina.client.service.DynamicIslandService

object TargetHudOverlay {

    private var appContext: Context? = null

    // 【修改】使用新的专用 Action 和 Extra
    private const val ACTION_UPDATE_TARGET_HUD = "com.project.lumina.client.ACTION_UPDATE_TARGET_HUD"
    private const val EXTRA_TARGET_USERNAME = "extra_target_username"
    private const val EXTRA_TARGET_DISTANCE = "extra_target_distance"

    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    fun showTargetHud(
        username: String,
        image: Bitmap?,
        distance: Float,
        maxDistance: Float = 50f,
        hurtTime: Float = 0f
    ) {
        val context = appContext
        if (context == null) {
            Log.e("TargetHudOverlay", "Error: Context is null. Did you forget to call init()?")
            return
        }
        
        // 【修改】创建并发送新的 Intent
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = ACTION_UPDATE_TARGET_HUD
            putExtra(EXTRA_TARGET_USERNAME, username)
            putExtra(EXTRA_TARGET_DISTANCE, distance)
        }

        context.startService(intent)
    }

    fun dismissTargetHud() { /* No-op */ }
    fun isTargetHudVisible(): Boolean = false
}