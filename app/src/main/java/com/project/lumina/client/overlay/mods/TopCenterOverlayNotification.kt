package com.project.lumina.client.overlay.mods

import android.content.Context
import android.content.Intent
import android.util.Log
import com.project.lumina.client.service.DynamicIslandService

/**
 * 【重构 & 适配灵动岛】
 * 此类现在是一个静态中继点，用于在屏幕顶部（通过灵动岛）显示一个带进度条的通知。
 * 它将通知请求转换为符合 DynamicIslandService API 的 Intent，并启动服务进行处理。
 *
 * 它不再创建自己的悬浮窗或管理任何 Jetpack Compose UI。
 */
object TopCenterOverlayNotification {

    private var appContext: Context? = null

    /**
     * 为这类通知定义一个固定的标识符。
     * 这确保了所有来自此类的通知都会更新灵动岛上*同一个*UI元素，而不是创建多个。
     */
    private const val TOP_NOTIFICATION_IDENTIFIER = "top_center_notification"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 在灵动岛上显示一个通知。
     *
     * @param title 通知的标题。
     * @param subtitle 通知的副标题。
     * @param iconRes 可选的图标资源ID。
     * @param progressDuration 通知显示的毫秒时长。
     */
    fun addNotification(
        title: String,
        subtitle: String,
        iconRes: Int? = null,
        progressDuration: Long = 2500
    ) {
        val context = appContext
        if (context == null) {
            Log.e("TopCenterOverlay", "错误: Context 为空。请确保在应用启动时调用了 init()。")
            return
        }

        // 1. 创建指向 DynamicIslandService 的 Intent
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS

            // 2. 传入所有必要的参数
            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, TOP_NOTIFICATION_IDENTIFIER)
            putExtra(DynamicIslandService.EXTRA_TITLE, title)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, subtitle)
            
            // 3. 传入图标资源 (如果提供)
            iconRes?.let { putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, it) }

            // 4. 这是一个基于时间的进度条
            putExtra(DynamicIslandService.EXTRA_DURATION_MS, progressDuration)
        }

        // 5. 启动服务以显示通知
        context.startService(intent)
    }
}