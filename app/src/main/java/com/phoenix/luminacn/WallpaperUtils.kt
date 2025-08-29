package com.phoenix.luminacn

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 壁纸获取工具类
 * 用于获取系统壁纸和锁屏壁纸
 */
object WallpaperUtils {
    
    /**
     * 壁纸类型枚举
     */
    enum class WallpaperType {
        SYSTEM,     // 系统壁纸
        LOCK_SCREEN // 锁屏壁纸 (API 24+)
    }
    
    /**
     * 获取壁纸 Drawable
     * @param context 上下文
     * @param type 壁纸类型，默认为系统壁纸
     * @return 壁纸 Drawable，获取失败返回 null
     */
    fun getWallpaperDrawable(
        context: Context, 
        type: WallpaperType = WallpaperType.SYSTEM
    ): Drawable? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            when (type) {
                WallpaperType.SYSTEM -> {
                    wallpaperManager.drawable
                }
                WallpaperType.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                    } else {
                        // API 24 以下不支持锁屏壁纸，返回系统壁纸
                        wallpaperManager.drawable
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取壁纸 Bitmap
     * @param context 上下文
     * @param type 壁纸类型，默认为系统壁纸
     * @return 壁纸 Bitmap，获取失败返回 null
     */
    fun getWallpaperBitmap(
        context: Context, 
        type: WallpaperType = WallpaperType.SYSTEM
    ): Bitmap? {
        return try {
            val drawable = getWallpaperDrawable(context, type) ?: return null
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取指定尺寸的壁纸 Bitmap
     * @param context 上下文
     * @param width 目标宽度
     * @param height 目标高度
     * @param type 壁纸类型，默认为系统壁纸
     * @return 指定尺寸的壁纸 Bitmap，获取失败返回 null
     */
    fun getWallpaperBitmap(
        context: Context,
        width: Int,
        height: Int,
        type: WallpaperType = WallpaperType.SYSTEM
    ): Bitmap? {
        return try {
            val drawable = getWallpaperDrawable(context, type) ?: return null
            drawableToBitmap(drawable, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 检查是否支持锁屏壁纸
     * @return true 如果支持锁屏壁纸
     */
    fun isLockScreenWallpaperSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    /**
     * 获取壁纸信息
     * @param context 上下文
     * @return 壁纸信息 WallpaperInfo，获取失败返回 null
     */
    fun getWallpaperInfo(context: Context): WallpaperInfo? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = wallpaperManager.drawable
            
            WallpaperInfo(
                width = drawable?.intrinsicWidth ?: 0,
                height = drawable?.intrinsicHeight ?: 0,
                hasLockScreenWallpaper = isLockScreenWallpaperSupported() && 
                    hasCustomLockScreenWallpaper(context)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 检查是否设置了自定义锁屏壁纸
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun hasCustomLockScreenWallpaper(context: Context): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val systemWallpaper = wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)
            val lockWallpaper = wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
            systemWallpaper != lockWallpaper
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 将 Drawable 转换为 Bitmap
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 将 Drawable 转换为指定尺寸的 Bitmap
     */
    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 壁纸信息数据类
     */
    data class WallpaperInfo(
        val width: Int,
        val height: Int,
        val hasLockScreenWallpaper: Boolean
    )
}