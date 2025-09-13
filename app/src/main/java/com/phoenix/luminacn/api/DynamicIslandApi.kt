package com.phoenix.luminacn.api

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import com.phoenix.luminacn.service.DynamicIslandService
import java.io.ByteArrayOutputStream

// 音乐操作类型
enum class MusicAction {
    UPDATE, STOP
}

// 保持原有的API调用方式不变的扩展函数
fun Context.showDynamicIslandMusic(
    identifier: String,
    title: String,
    subtitle: String,
    albumArt: Drawable?,
    progressText: String,
    progress: Float,
    action: MusicAction
) {
    if (action == MusicAction.STOP) {
        // 停止音乐显示
        val intent = Intent(this, DynamicIslandService::class.java).apply {
            this.action = DynamicIslandService.ACTION_REMOVE_TASK
            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, identifier)
        }
        startService(intent)
    } else {
        // 更新或显示音乐
        val intent = Intent(this, DynamicIslandService::class.java).apply {
            this.action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_MUSIC
            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, identifier)
            putExtra(DynamicIslandService.EXTRA_TITLE, title)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, subtitle)
            putExtra(DynamicIslandService.EXTRA_PROGRESS_TEXT, progressText)
            putExtra(DynamicIslandService.EXTRA_PROGRESS_VALUE, progress)
            putExtra(DynamicIslandService.EXTRA_IS_MAJOR_UPDATE, true)
            
            // 传递专辑封面数据
            albumArt?.let { drawable ->
                drawableToByteArray(drawable)?.let { byteArray ->
                    putExtra(DynamicIslandService.EXTRA_ALBUM_ART_DATA, byteArray)
                }
            }
        }
        startService(intent)
    }
}

fun Context.updateDynamicIslandMusicProgress(
    identifier: String,
    progressText: String,
    progress: Float
) {
    val intent = Intent(this, DynamicIslandService::class.java).apply {
        action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_MUSIC
        putExtra(DynamicIslandService.EXTRA_IDENTIFIER, identifier)
        putExtra(DynamicIslandService.EXTRA_PROGRESS_TEXT, progressText)
        putExtra(DynamicIslandService.EXTRA_PROGRESS_VALUE, progress)
        putExtra(DynamicIslandService.EXTRA_IS_MAJOR_UPDATE, false)
    }
    startService(intent)
}

/**
 * 新增：显示带图像的进度任务
 */
fun Context.showDynamicIslandProgressWithImage(
    identifier: String,
    title: String,
    subtitle: String? = null,
    image: Bitmap?,
    progress: Float
) {
    val intent = Intent(this, DynamicIslandService::class.java).apply {
        action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS
        putExtra(DynamicIslandService.EXTRA_IDENTIFIER, identifier)
        putExtra(DynamicIslandService.EXTRA_TITLE, title)
        subtitle?.let { putExtra(DynamicIslandService.EXTRA_SUBTITLE, it) }
        putExtra(DynamicIslandService.EXTRA_PROGRESS_VALUE, progress)
        
        // 传递图像数据
        image?.let { bitmap ->
            bitmapToByteArray(bitmap)?.let { byteArray ->
                putExtra(DynamicIslandService.EXTRA_IMAGE_DATA, byteArray)
            }
        }
    }
    startService(intent)
}

/**
 * 将Bitmap转换为字节数组
 */
private fun bitmapToByteArray(bitmap: Bitmap): ByteArray? {
    return try {
        val stream = ByteArrayOutputStream()
        // 使用JPEG格式压缩，质量85%，在大小和质量间取平衡
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        stream.toByteArray()
    } catch (e: Exception) {
        Log.e("DynamicIslandApi", "Failed to compress bitmap", e)
        null
    }
}

/**
 * 将Drawable转换为字节数组
 */
private fun drawableToByteArray(drawable: Drawable): ByteArray? {
    return try {
        val bitmap = when (drawable) {
            is android.graphics.drawable.BitmapDrawable -> drawable.bitmap
            else -> {
                // 将其他类型的Drawable转换为Bitmap
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth.takeIf { it > 0 } ?: 200,
                    drawable.intrinsicHeight.takeIf { it > 0 } ?: 200,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        }
        bitmapToByteArray(bitmap)
    } catch (e: Exception) {
        Log.e("DynamicIslandApi", "Failed to convert drawable to byte array", e)
        null
    }
}