package com.phoenix.luminacn.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.phoenix.luminacn.service.DynamicIslandService

/**
 * 灵动岛控制器
 * 提供统一的API来控制灵动岛的各种功能
 */
class DynamicIslandController(private val context: Context) {
    
    /**
     * 设置持久显示的文本（用户名）
     */
    fun setPersistentText(text: String) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_UPDATE_TEXT
                putExtra(DynamicIslandService.EXTRA_TEXT, text)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Updated persistent text: $text")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to update persistent text", e)
        }
    }
    
    /**
     * 更新Y轴偏移量
     */
    fun updateYOffset(yOffset: Float) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_UPDATE_Y_OFFSET
                putExtra(DynamicIslandService.EXTRA_Y_OFFSET_DP, yOffset)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Updated Y offset: $yOffset dp")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to update Y offset", e)
        }
    }
    
    /**
     * 更新缩放比例
     */
    fun updateScale(scale: Float) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_UPDATE_SCALE
                putExtra(DynamicIslandService.EXTRA_SCALE, scale)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Updated scale: $scale")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to update scale", e)
        }
    }
    
    /**
     * 控制音乐模式的开启/关闭
     */
    fun enableMusicMode(enabled: Boolean) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SET_MUSIC_MODE
                putExtra(DynamicIslandService.EXTRA_MUSIC_MODE_ENABLED, enabled)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Music mode ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to set music mode", e)
        }
    }
    
    /**
     * 控制无任务时是否隐藏灵动岛
     */
    fun setHideWhenNoTasks(hide: Boolean) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SET_HIDE_WHEN_NO_TASKS
                putExtra(DynamicIslandService.EXTRA_HIDE_WHEN_NO_TASKS, hide)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Hide when no tasks: $hide")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to set hide when no tasks", e)
        }
    }
    
    /**
     * 显示开关通知
     */
    fun showSwitchNotification(moduleName: String, isEnabled: Boolean) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SHOW_NOTIFICATION_SWITCH
                putExtra(DynamicIslandService.EXTRA_MODULE_NAME, moduleName)
                putExtra(DynamicIslandService.EXTRA_MODULE_STATE, isEnabled)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Showed switch notification: $moduleName = $isEnabled")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to show switch notification", e)
        }
    }
    
    /**
     * 显示或更新进度任务
     */
    fun showProgress(
        identifier: String,
        title: String,
        subtitle: String? = null,
        iconResId: Int = -1,
        progress: Float? = null,
        duration: Long? = null
    ) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS
                putExtra(DynamicIslandService.EXTRA_IDENTIFIER, identifier)
                putExtra(DynamicIslandService.EXTRA_TITLE, title)
                subtitle?.let { putExtra(DynamicIslandService.EXTRA_SUBTITLE, it) }
                if (iconResId != -1) putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, iconResId)
                progress?.let { putExtra(DynamicIslandService.EXTRA_PROGRESS_VALUE, it) }
                duration?.let { putExtra(DynamicIslandService.EXTRA_DURATION_MS, it) }
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Showed progress: $identifier - $title")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to show progress", e)
        }
    }
    
    /**
     * 移除指定的任务
     */
    fun removeTask(identifier: String) {
        try {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_REMOVE_TASK
                putExtra(DynamicIslandService.EXTRA_IDENTIFIER, identifier)
            }
            context.startService(intent)
            Log.d("DynamicIslandController", "Removed task: $identifier")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to remove task", e)
        }
    }
    
    /**
     * 检查灵动岛服务是否正在运行
     */
    fun isServiceRunning(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)
            services.any { it.service.className == DynamicIslandService::class.java.name }
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to check service status", e)
            false
        }
    }
    
    /**
     * 启动灵动岛服务
     */
    fun startService() {
        try {
            val intent = Intent(context, DynamicIslandService::class.java)
            context.startService(intent)
            Log.d("DynamicIslandController", "Started DynamicIslandService")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to start service", e)
        }
    }
    
    /**
     * 停止灵动岛服务
     */
    fun stopService() {
        try {
            val intent = Intent(context, DynamicIslandService::class.java)
            context.stopService(intent)
            Log.d("DynamicIslandController", "Stopped DynamicIslandService")
        } catch (e: Exception) {
            Log.e("DynamicIslandController", "Failed to stop service", e)
        }
    }
}