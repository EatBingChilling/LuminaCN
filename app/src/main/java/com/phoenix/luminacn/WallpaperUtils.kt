package com.phoenix.luminacn

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 壁纸获取工具类
 * 用于获取系统壁纸和锁屏壁纸
 */
object WallpaperUtils {
    
    private const val PREFS_NAME = "wallpaper_prefs"
    private const val KEY_CUSTOM_WALLPAPER_PATH = "custom_wallpaper_path"
    private const val CUSTOM_WALLPAPER_FILENAME = "custom_wallpaper.jpg"
    
    // 高版本SDK阈值，超过此版本需要用户手动设置壁纸
    private const val HIGH_SDK_THRESHOLD = Build.VERSION_CODES.TIRAMISU // Android 13
    
    /**
     * 壁纸类型枚举
     */
    enum class WallpaperType {
        SYSTEM,     // 系统壁纸
        LOCK_SCREEN // 锁屏壁纸 (API 24+)
    }
    
    /**
     * SAF选择器接口
     */
    interface WallpaperSelectorCallback {
        fun onWallpaperSelected()
        fun onWallpaperSelectionCancelled()
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
            // 检查是否为高版本SDK且需要自定义壁纸
            if (isHighSdkVersion()) {
                val customDrawable = getCustomWallpaperDrawable(context)
                if (customDrawable != null) {
                    return applyNightModeFilter(context, customDrawable)
                }
                
                // 如果没有自定义壁纸，显示提示
                if (context is Activity) {
                    showWallpaperSetupDialog(context)
                }
                return null
            }
            
            // 低版本SDK直接获取系统壁纸
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = when (type) {
                WallpaperType.SYSTEM -> {
                    wallpaperManager.drawable
                }
                WallpaperType.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.drawable
                    }
                }
            }
            
            drawable?.let { applyNightModeFilter(context, it) }
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
            // 检查是否为高版本SDK且需要自定义壁纸
            if (isHighSdkVersion()) {
                val customBitmap = getCustomWallpaperBitmap(context)
                if (customBitmap != null) {
                    return applyNightModeFilter(context, customBitmap)
                }
                
                // 如果没有自定义壁纸，显示提示
                if (context is Activity) {
                    showWallpaperSetupDialog(context)
                }
                return null
            }
            
            val drawable = getSystemWallpaperDrawable(context, type) ?: return null
            val bitmap = drawableToBitmap(drawable)
            bitmap?.let { applyNightModeFilter(context, it) }
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
            // 检查是否为高版本SDK且需要自定义壁纸
            if (isHighSdkVersion()) {
                val customBitmap = getCustomWallpaperBitmap(context, width, height)
                if (customBitmap != null) {
                    return applyNightModeFilter(context, customBitmap)
                }
                
                // 如果没有自定义壁纸，显示提示
                if (context is Activity) {
                    showWallpaperSetupDialog(context)
                }
                return null
            }
            
            val drawable = getSystemWallpaperDrawable(context, type) ?: return null
            val bitmap = drawableToBitmap(drawable, width, height)
            bitmap?.let { applyNightModeFilter(context, it) }
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
            // 高版本SDK优先获取自定义壁纸信息
            if (isHighSdkVersion()) {
                val customBitmap = getCustomWallpaperBitmap(context)
                if (customBitmap != null) {
                    return WallpaperInfo(
                        width = customBitmap.width,
                        height = customBitmap.height,
                        hasLockScreenWallpaper = false
                    )
                }
            }
            
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
     * 设置自定义壁纸（新增接口）
     * 用于高版本SDK时用户手动设置壁纸
     */
    fun setupCustomWallpaper(activity: AppCompatActivity, callback: WallpaperSelectorCallback? = null) {
        try {
            val launcher = createImagePickerLauncher(activity, callback)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            callback?.onWallpaperSelectionCancelled()
        }
    }
    
    /**
     * 检查是否已设置自定义壁纸
     */
    fun hasCustomWallpaper(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        val path = prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null)
        return !path.isNullOrEmpty() && File(path).exists()
    }
    
    /**
     * 清除自定义壁纸
     */
    fun clearCustomWallpaper(context: Context) {
        val prefs = getSharedPreferences(context)
        val path = prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null)
        if (!path.isNullOrEmpty()) {
            File(path).delete()
        }
        prefs.edit().remove(KEY_CUSTOM_WALLPAPER_PATH).apply()
    }
    
    // =========================== 私有方法 ===========================
    
    private fun isHighSdkVersion(): Boolean {
        return Build.VERSION.SDK_INT >= HIGH_SDK_THRESHOLD
    }
    
    private fun getSystemWallpaperDrawable(context: Context, type: WallpaperType): Drawable? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            when (type) {
                WallpaperType.SYSTEM -> wallpaperManager.drawable
                WallpaperType.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.drawable
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getCustomWallpaperDrawable(context: Context): Drawable? {
        val bitmap = getCustomWallpaperBitmap(context) ?: return null
        return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
    }
    
    private fun getCustomWallpaperBitmap(context: Context): Bitmap? {
        return try {
            val prefs = getSharedPreferences(context)
            val path = prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null)
            if (!path.isNullOrEmpty() && File(path).exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getCustomWallpaperBitmap(context: Context, width: Int, height: Int): Bitmap? {
        return try {
            val originalBitmap = getCustomWallpaperBitmap(context) ?: return null
            Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun showWallpaperSetupDialog(activity: Activity) {
        if (activity !is AppCompatActivity) return
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("需要设置壁纸")
            .setMessage("由于系统权限限制，需要您手动选择一张图片作为壁纸。")
            .setPositiveButton("选择图片") { _, _ ->
                setupCustomWallpaper(activity)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun createImagePickerLauncher(
        activity: AppCompatActivity, 
        callback: WallpaperSelectorCallback?
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    if (saveCustomWallpaper(activity, uri)) {
                        callback?.onWallpaperSelected()
                    } else {
                        callback?.onWallpaperSelectionCancelled()
                    }
                } ?: callback?.onWallpaperSelectionCancelled()
            } else {
                callback?.onWallpaperSelectionCancelled()
            }
        }
    }
    
    private fun saveCustomWallpaper(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val internalDir = File(context.filesDir, "wallpapers")
                if (!internalDir.exists()) {
                    internalDir.mkdirs()
                }
                
                val wallpaperFile = File(internalDir, CUSTOM_WALLPAPER_FILENAME)
                FileOutputStream(wallpaperFile).use { output ->
                    input.copyTo(output)
                }
                
                // 保存路径到SharedPreferences
                val prefs = getSharedPreferences(context)
                prefs.edit().putString(KEY_CUSTOM_WALLPAPER_PATH, wallpaperFile.absolutePath).apply()
                
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun isNightMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
    
    private fun applyNightModeFilter(context: Context, drawable: Drawable): Drawable {
        if (!isNightMode(context)) return drawable
        
        val bitmap = drawableToBitmap(drawable) ?: return drawable
        val filteredBitmap = applyNightModeFilter(context, bitmap)
        return android.graphics.drawable.BitmapDrawable(context.resources, filteredBitmap)
    }
    
    private fun applyNightModeFilter(context: Context, bitmap: Bitmap): Bitmap {
        if (!isNightMode(context)) return bitmap
        
        return try {
            val filteredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            val canvas = Canvas(filteredBitmap)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    // 降低亮度，增加一点蓝色调
                    setScale(0.6f, 0.6f, 0.7f, 1.0f)
                })
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            filteredBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
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