package com.project.lumina.client.overlay.mods

import android.content.Context
import android.content.Intent
import android.util.Log
import com.project.lumina.client.service.DynamicIslandService

/**
 * 【重构】
 * 此类现在作为一个静态中继点 (Relay/Broker)。
 * 它的唯一作用是提供一个稳定的、向后兼容的API，供应用的其余部分调用。
 * 它会将通知请求转换为Intent，并发送给 DynamicIslandService 进行实际处理。
 *
 * 它不再创建自己的UI或悬浮窗。
 */
object OverlayNotification {

    private var appContext: Context? = null

    // 定义 Action 和 Extra Key，与 Service 保持一致
    private const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.project.lumina.client.ACTION_SHOW_NOTIFICATION_SWITCH"
    private const val EXTRA_MODULE_NAME = "extra_module_name"
    private const val EXTRA_MODULE_STATE = "extra_module_state"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     * 推荐在 Application 类或主 Activity 的 onCreate 中调用。
     */
    fun init(context: Context) {
        // 使用 applicationContext 防止内存泄漏
        this.appContext = context.applicationContext
    }

    /**
     * 旧接口：保持零改动兼容。
     * @param moduleName 要通知的模块名称。
     */
    fun addNotification(moduleName: String) = onModuleEnabled(moduleName)

    /**
     * 新增双参数接口，这是所有调用的核心。
     * @param moduleName 模块名称。
     * @param enabled 模块是启用还是禁用。
     */
    fun addNotification(moduleName: String, enabled: Boolean) {
        val context = appContext
        if (context == null) {
            Log.e("OverlayNotification", "Error: Context is null. Did you forget to call init() in your MainActivity/Application?")
            return
        }

        // 创建指向 DynamicIslandService 的 Intent
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = ACTION_SHOW_NOTIFICATION_SWITCH
            putExtra(EXTRA_MODULE_NAME, moduleName)
            putExtra(EXTRA_MODULE_STATE, enabled)
        }

        // 启动服务以处理此通知
        context.startService(intent)
    }

    /**
     * 当一个模块被启用时调用。
     * @param moduleName 启用的模块名称。
     */
    fun onModuleEnabled(moduleName: String) = addNotification(moduleName, true)

    /**
     * 当一个模块被禁用时调用。
     * @param moduleName 禁用的模块名称。
     */
    fun onModuleDisabled(moduleName: String) = addNotification(moduleName, false)
}