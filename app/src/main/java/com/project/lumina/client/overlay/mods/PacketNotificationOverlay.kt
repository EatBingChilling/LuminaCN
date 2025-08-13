/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.content.Context
import android.content.Intent
import android.util.Log
import com.project.lumina.client.service.DynamicIslandService

/**
 * 【重构】
 * 此类现在作为一个静态中继点 (Relay/Broker) 用于进度条类型的通知。
 * 它会将通知请求转换为Intent，并发送给 DynamicIslandService 进行实际处理。
 *
 * 它不再创建自己的UI或悬浮窗。
 */
object PacketNotificationOverlay {

    // 这个 appContext 将由其他中继点（如 OverlayNotification）在初始化时设置
    // 我们假设 init() 总是会被调用
    private var appContext: Context? = null

    // 定义 Action 和 Extra Key，与 Service 保持一致
    private const val ACTION_SHOW_PROGRESS = "com.project.lumina.client.ACTION_SHOW_PROGRESS"
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_SUBTITLE = "extra_subtitle" // subtitle 暂不用于灵动岛，但保留以备将来扩展
    private const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
    private const val EXTRA_DURATION_MS = "extra_duration_ms"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     * 推荐在 Application 类或主 Activity 的 onCreate 中调用。
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 显示一个进度条类型的通知。
     *
     * @param title 通知的主标题，将显示在灵动岛上。
     * @param subtitle 通知的副标题（当前版本灵动岛未使用，为将来保留）。
     * @param iconRes 可选的图标资源ID。如果为null，灵动岛可能不显示图标。
     * @param duration 进度条从满到空所需的毫秒时长。
     */
    fun showNotification(
        title: String,
        subtitle: String,
        iconRes: Int? = null,
        duration: Long = 1000L
    ) {
        val context = appContext
        if (context == null) {
            Log.e("PacketNotificationOverlay", "Error: Context is null. Did you forget to call init() in your MainActivity/Application?")
            return
        }

        // 创建指向 DynamicIslandService 的 Intent
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = ACTION_SHOW_PROGRESS
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_SUBTITLE, subtitle) // 保留数据
            // 可空类型需要特殊处理
            iconRes?.let { putExtra(EXTRA_ICON_RES_ID, it) }
            putExtra(EXTRA_DURATION_MS, duration)
        }

        // 启动服务以处理此通知
        context.startService(intent)
    }
}