package com.phoenix.luminacn.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.phoenix.luminacn.service.DynamicIslandService

/**
 * 灵动岛控制器
 */
class DynamicIslandController(private val context: Context) {
    
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
    
    // 🆕 音乐模式控制
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
}