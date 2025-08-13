package com.project.lumina.client.util

import android.content.Context
import android.content.Intent
import com.project.lumina.client.service.DynamicIslandService

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
}