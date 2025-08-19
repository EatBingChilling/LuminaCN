package com.phoenix.luminacn.util

import android.content.Context
import android.content.Intent
import com.phoenix.luminacn.service.DynamicIslandService

class DynamicIslandController(private val context: Context) {

    fun setPersistentText(text: String) {
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_UPDATE_TEXT
            putExtra(DynamicIslandService.EXTRA_TEXT, text)
        }
        context.startService(intent)
    }

    fun updateYOffset(yOffsetDp: Float) {
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_UPDATE_Y_OFFSET
            putExtra(DynamicIslandService.EXTRA_Y_OFFSET_DP, yOffsetDp)
        }
        context.startService(intent)
    }

    fun updateScale(scale: Float) {
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_UPDATE_SCALE
            putExtra(DynamicIslandService.EXTRA_SCALE, scale)
        }
        context.startService(intent)
    }
}