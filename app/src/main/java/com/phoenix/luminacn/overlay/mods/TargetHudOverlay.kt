package com.phoenix.luminacn.overlay.mods

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.phoenix.luminacn.service.DynamicIslandService
import java.util.Locale

/**
 * 【重构 & 适配灵动岛】
 * 此类作为显示“目标HUD”的中继点。
 * 它将目标信息（名称、距离等）转换为一个数值驱动的进度条任务，并发送给 DynamicIslandService。
 */
object TargetHudOverlay {

    private var appContext: Context? = null

    /**
     * 【关键】为 Target HUD 定义一个固定的标识符。
     * 确保所有关于目标的信息都更新在同一个UI元素上。
     */
    private const val TARGET_HUD_IDENTIFIER = "target_hud"

    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 在灵动岛上显示或更新目标HUD。
     *
     * @param username 目标名称，将作为标题。
     * @param image 目标的图像。【注意】当前灵动岛不支持直接传递Bitmap，此参数将被忽略。
     * @param distance 与目标的当前距离。
     * @param maxDistance 可视最大距离，用于计算进度条。
     * @param hurtTime 受伤时间。（当前版本未集成，为将来保留）。
     */
    fun showTargetHud(
        username: String,
        image: Bitmap?, // 参数保留，但功能暂不实现
        distance: Float,
        maxDistance: Float = 50f,
        hurtTime: Float = 0f // 参数保留
    ) {
        val context = appContext
        if (context == null) {
            Log.e("TargetHudOverlay", "错误: Context 为空。请确保在应用启动时调用了 init()。")
            return
        }

        // 1. 计算进度值。距离越近，进度越高。使用 coerceIn 保证值在 0.0 到 1.0 之间。
        val progress = (1.0f - (distance / maxDistance)).coerceIn(0f, 1f)

        // 2. 格式化副标题
        val subtitle = String.format(Locale.getDefault(), "距离: %.1fm", distance)

        // 3. 创建 Intent，复用服务已有的接口
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS

            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, TARGET_HUD_IDENTIFIER)
            putExtra(DynamicIslandService.EXTRA_TITLE, username)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, subtitle)

            // 【关键限制】由于服务不接受 Bitmap，我们使用一个固定的系统图标作为替代。
            // android.R.drawable.ic_menu_view 看起来有点像准星。
            putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, android.R.drawable.ic_menu_view)

            // 这是一个由外部数值驱动的进度条
            putExtra(DynamicIslandService.EXTRA_PROGRESS_VALUE, progress)
        }

        context.startService(intent)
    }

    /**
     * 【逻辑说明】隐藏目标HUD。
     * 无需主动调用。由于灵动岛的任务有超时机制，
     * 当你停止调用 showTargetHud() 约3秒后，HUD会自动消失。
     * 因此，此方法可为空。
     */
    fun dismissTargetHud() {
        // No-op. The island view will automatically remove the task after a timeout
        // if showTargetHud is no longer being called.
    }

    /**
     * 【逻辑说明】此中继点不维护UI状态，所以永远返回 false。
     */
    fun isTargetHudVisible(): Boolean = false
}