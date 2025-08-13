/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.project.lumina.client.service.DynamicIslandService

/**
 * 【重构】
 * 此类现在作为一个静态中继点 (Relay/Broker) 用于显示目标 HUD 通知。
 * 它会将 TargetHUD 的信息转换为灵动岛的进度条模式，并发送给 DynamicIslandService 处理。
 *
 * 它不再创建自己的UI或悬浮窗。
 */
object TargetHudOverlay {

    // 这个 appContext 将由其他中继点在初始化时设置
    // 我们假设 init() 总是会被调用
    private var appContext: Context? = null

    // 【复用】我们复用为 PacketNotification 创建的 Action 和 Extra，因为它们都使用进度条模式
    private const val ACTION_SHOW_PROGRESS = "com.project.lumina.client.ACTION_SHOW_PROGRESS"
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
    private const val EXTRA_DURATION_MS = "extra_duration_ms"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 显示目标HUD信息。
     * 这个方法会将目标信息适配到灵动岛的进度条通知上。
     *
     * @param username 目标用户名。
     * @param image 目标头像 (当前版本灵动岛未使用，为将来保留)。
     * @param distance 目标距离。
     * @param maxDistance 最大距离（未使用）。
     * @param hurtTime 目标受伤时间（未使用）。
     */
    fun showTargetHud(
        username: String,
        image: Bitmap?, // image 暂不传递，因为灵动岛目前只接受资源ID
        distance: Float,
        maxDistance: Float = 50f,
        hurtTime: Float = 0f
    ) {
        val context = appContext
        if (context == null) {
            Log.e("TargetHudOverlay", "Error: Context is null. Did you forget to call init()?")
            return
        }
        
        // 将 TargetHUD 的信息转换为灵动岛进度条模式所需的参数
        val title = "$username (${"%.1f".format(distance)}m)"
        val duration = 3000L // 固定显示时长为3秒，与原版逻辑一致

        // 创建指向 DynamicIslandService 的 Intent
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = ACTION_SHOW_PROGRESS
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DURATION_MS, duration)
            // 当前没有合适的通用图标，所以不传递图标
            // 如果有需要，可以添加一个固定的 "target" 图标资源ID
        }

        // 启动服务以处理此通知
        context.startService(intent)
    }

    /**
     * 这个方法现在是空操作，因为灵动岛通知会自动消失。
     * 保留此方法以实现向后兼容。
     */
    fun dismissTargetHud() {
        // No-op
    }

    /**
     * 这个方法现在总是返回 false，因为中继点本身没有可见状态。
     * 保留此方法以实现向后兼容。
     * @return 总是 false。
     */
    fun isTargetHudVisible(): Boolean {
        return false
    }
}