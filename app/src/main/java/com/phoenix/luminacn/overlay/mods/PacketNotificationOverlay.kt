/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 */

package com.phoenix.luminacn.overlay.mods

import android.content.Context
import android.content.Intent
import android.util.Log
import com.phoenix.luminacn.service.DynamicIslandService

/**
 * 【重构 & 适配灵动岛】
 * 此类作为向灵动岛发送“数据包”类型通知的专用静态中继点 (Relay)。
 * 它将通知请求转换为符合 DynamicIslandService API 的 Intent，并启动服务进行处理。
 *
 * 它不再创建自己的UI或悬浮窗。
 */
object PacketNotificationOverlay {

    private var appContext: Context? = null

    /**
     * 【应用所学】为这类通知定义一个固定的标识符。
     * 这确保了所有来自此类的通知都会更新灵动岛上*同一个*UI元素，而不是创建多个。
     * 就像 MainActivity 中的 "time_demo" 或 "value_demo"。
     */
    private const val PACKET_NOTIFICATION_IDENTIFIER = "packet_notification_overlay"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 在灵动岛上显示一个基于持续时间的进度条通知（模拟数据包收发）。
     *
     * @param title 通知的主标题。
     * @param subtitle 通知的副标题 (为将来保留，当前UI可能不显示)。
     * @param iconRes 可选的图标资源ID。如果为null，灵动岛将不显示图标。
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
            Log.e("PacketNotificationOverlay", "错误: Context 为空。请确保在应用启动时调用了 init()。")
            return
        }

        // 【应用所学】创建指向 DynamicIslandService 的 Intent，并使用其定义的常量。
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            // 1. 使用 Service 定义的正确 Action
            action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS

            // 2. 传入 Service 要求的必要参数和可选参数
            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, PACKET_NOTIFICATION_IDENTIFIER)
            putExtra(DynamicIslandService.EXTRA_TITLE, title)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, subtitle)
            
            // 3. 传入图标资源 (如果提供)
            iconRes?.let { putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, it) }

            // 4. 【应用所学】这是一个基于时间的进度条，所以我们传入 duration，
            //    而不是 progress_value，完全模仿 MainActivity 的做法。
            putExtra(DynamicIslandService.EXTRA_DURATION_MS, duration)
        }

        // 【应用所学】通过 startService 将命令发送给服务。
        context.startService(intent)
    }
}