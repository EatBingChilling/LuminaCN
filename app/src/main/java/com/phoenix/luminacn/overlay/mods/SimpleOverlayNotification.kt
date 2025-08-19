package com.phoenix.luminacn.overlay.mods

import android.content.Context
import android.content.Intent
import android.util.Log
import com.phoenix.luminacn.service.DynamicIslandService

// 定义通知类型，这个枚举仍然有用，用于逻辑判断
enum class NotificationType {
    SUCCESS, WARNING, ERROR, INFO
}

/**
 * 【重构 & 适配灵动岛】
 * 此类现在是一个静态中继点，用于显示简单的状态通知 (成功、警告、错误、信息)。
 * 它将通知请求转换为符合 DynamicIslandService API 的 Intent，并启动服务进行处理。
 *
 * 它不再创建或管理任何 Jetpack Compose UI。
 */
object SimpleOverlayNotification {

    private var appContext: Context? = null

    /**
     * 为这类通知定义一个固定的标识符。
     * 这确保了所有来自此类的通知都会更新灵动岛上*同一个*UI元素，而不是创建多个。
     */
    private const val SIMPLE_NOTIFICATION_IDENTIFIER = "simple_notification"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 在灵动岛上显示一个简单的状态通知。
     *
     * @param message 通知的核心信息，将作为灵动岛的副标题。
     * @param type 通知的类型 (SUCCESS, WARNING, ERROR, INFO)，将决定图标和标题。
     * @param durationMs 通知显示的毫秒时长。
     */
    fun show(
        message: String,
        type: NotificationType = NotificationType.INFO,
        durationMs: Long = 3000L
    ) {
        val context = appContext
        if (context == null) {
            Log.e("SimpleOverlayNotification", "错误: Context 为空。请确保在应用启动时调用了 init()。")
            return
        }

        // 1. 根据通知类型，映射到灵动岛所需的标题和图标资源ID
        val (title, iconResId) = mapTypeToTitleAndIcon(type)

        // 2. 创建指向 DynamicIslandService 的 Intent
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS
            
            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, SIMPLE_NOTIFICATION_IDENTIFIER)
            putExtra(DynamicIslandService.EXTRA_TITLE, title)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, message) // 消息内容作为副标题
            putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, iconResId)
            putExtra(DynamicIslandService.EXTRA_DURATION_MS, durationMs)
        }

        // 3. 启动服务以显示通知
        context.startService(intent)
    }

    /**
     * 私有辅助函数，将我们的 NotificationType 映射为灵动岛需要的字符串标题和 Drawable 资源 ID。
     */
    private fun mapTypeToTitleAndIcon(type: NotificationType): Pair<String, Int> {
        return when (type) {
            NotificationType.SUCCESS -> "操作成功" to android.R.drawable.stat_sys_upload_done
            NotificationType.WARNING -> "警告" to android.R.drawable.stat_sys_warning
            NotificationType.ERROR -> "发生错误" to android.R.drawable.ic_dialog_alert
            NotificationType.INFO -> "提示" to android.R.drawable.ic_dialog_info
        }
    }
}