package com.project.lumina.client.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class ServerInit {

    companion object {
        fun addMinecraftServer(context: Context, localIp: String) {
            val uri = "minecraft://?addExternalServer=§bLuminaCN 进服选我|127.0.0.1:19132".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }


    }

}