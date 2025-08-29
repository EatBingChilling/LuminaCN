package com.phoenix.luminacn

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
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
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.cloudy.Cloudy
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
    private const val KEY_BLUR_ENABLED = "blur_enabled"
    private const val KEY_BLUR_RADIUS = "blur_radius"
    private const val KEY_BLUR_SAMPLING = "blur_sampling"
    private const val CUSTOM_WALLPAPER_FILENAME = "custom_wallpaper.jpg"
    
    // 高版本SDK阈值，超过此版本需要用户手动设置壁纸
    private const val HIGH_SDK_THRESHOLD = Build.VERSION_CODES.TIRAMISU // Android 13
    
    // 模糊配置
    private const val DEFAULT_BLUR_RADIUS = 15f
    private const val DEFAULT_BLUR_SAMPLING = 1f
    private const val MAX_BLUR_RADIUS = 25f
    private const val MIN_BLUR_RADIUS = 1f
    
    /**
     * 壁纸类型枚举
     */
    enum class WallpaperType {
        SYSTEM,     // 系统壁纸
        LOCK_SCREEN // 锁屏壁纸 (API 24+)
    }
    
    /**
     * 壁纸效果配置
     */
    data class WallpaperEffectConfig(
        val enableBlur: Boolean = true,
        val blurRadius: Float = DEFAULT_BLUR_RADIUS,
        val blurSampling: Float = DEFAULT_BLUR_SAMPLING,
        val enableNightMode: Boolean = true
    )
    
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
                
                // 高版本SDK且没有自定义壁纸时，不再显示对话框
                // 对话框由AppContext和Activity统一管理
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
                
                // 高版本SDK且没有自定义壁纸时，不再显示对话框
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
                
                // 高版本SDK且没有自定义壁纸时，不再显示对话框
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
     * 设置自定义壁纸（保留原有接口）
     * 用于高版本SDK时用户手动设置壁纸
     */
    fun setupCustomWallpaper(activity: AppCompatActivity, callback: WallpaperSelectorCallback? = null) {
        try {
            val launcher = createImagePickerLauncher(activity, callback)
            val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            callback?.onWallpaperSelectionCancelled()
        }
    }
    
    /**
     * 处理用户选择的壁纸（新增方法）
     * 统一处理壁纸保存、模糊等操作
     */
    fun handleSelectedWallpaper(context: Context, uri: Uri, callback: WallpaperSelectorCallback? = null) {
        Thread {
            try {
                Log.d("WallpaperUtils", "Processing selected wallpaper: $uri")
                // 这里可以添加各种处理逻辑，比如Cloudy模糊等
                val success = processAndSaveWallpaper(context, uri)
                
                // 回到主线程通知结果
                if (context is Activity) {
                    context.runOnUiThread {
                        if (success) {
                            Log.d("WallpaperUtils", "Wallpaper processed successfully")
                            callback?.onWallpaperSelected()
                        } else {
                            Log.e("WallpaperUtils", "Failed to process wallpaper")
                            callback?.onWallpaperSelectionCancelled()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WallpaperUtils", "Failed to handle selected wallpaper", e)
                if (context is Activity) {
                    context.runOnUiThread {
                        callback?.onWallpaperSelectionCancelled()
                    }
                }
            }
        }.start()
    }
    
    /**
     * 设置模糊效果配置
     */
    fun setBlurConfig(context: Context, enableBlur: Boolean, blurRadius: Float = DEFAULT_BLUR_RADIUS, blurSampling: Float = DEFAULT_BLUR_SAMPLING) {
        val prefs = getSharedPreferences(context)
        prefs.edit().apply {
            putBoolean(KEY_BLUR_ENABLED, enableBlur)
            putFloat(KEY_BLUR_RADIUS, blurRadius.coerceIn(MIN_BLUR_RADIUS, MAX_BLUR_RADIUS))
            putFloat(KEY_BLUR_SAMPLING, blurSampling.coerceIn(0.1f, 1.0f))
            apply()
        }
        Log.d("WallpaperUtils", "Blur config updated: enabled=$enableBlur, radius=$blurRadius, sampling=$blurSampling")
    }
    
    /**
     * 获取模糊效果配置
     */
    fun getBlurConfig(context: Context): WallpaperEffectConfig {
        val prefs = getSharedPreferences(context)
        return WallpaperEffectConfig(
            enableBlur = prefs.getBoolean(KEY_BLUR_ENABLED, true),
            blurRadius = prefs.getFloat(KEY_BLUR_RADIUS, DEFAULT_BLUR_RADIUS),
            blurSampling = prefs.getFloat(KEY_BLUR_SAMPLING, DEFAULT_BLUR_SAMPLING),
            enableNightMode = true
        )
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
    
    /**
     * 处理并保存壁纸（私有方法）
     * 在这里可以添加模糊、裁剪、压缩等处理
     */
    private fun processAndSaveWallpaper(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                // 先读取原始bitmap
                val originalBitmap = BitmapFactory.decodeStream(input)
                    ?: return false
                
                Log.d("WallpaperUtils", "Original bitmap size: ${originalBitmap.width}x${originalBitmap.height}")
                
                // 这里可以添加各种处理
                val processedBitmap = applyWallpaperEffects(context, originalBitmap)
                
                // 保存处理后的bitmap
                saveProcessedBitmap(context, processedBitmap)
            } ?: false
        } catch (e: Exception) {
            Log.e("WallpaperUtils", "Failed to process and save wallpaper", e)
            false
        }
    }
    
    /**
     * 应用壁纸效果（包含Cloudy模糊）
     * 可以在这里添加Cloudy模糊、调色等效果
     */
    private fun applyWallpaperEffects(context: Context, bitmap: Bitmap): Bitmap {
        var processedBitmap = bitmap
        
        try {
            val config = getBlurConfig(context)
            
            // 应用Cloudy模糊效果
            if (config.enableBlur) {
                processedBitmap = applyCloudyBlur(context, processedBitmap, config)
            }
            
            // 可以在这里添加更多效果
            // processedBitmap = adjustColors(processedBitmap)
            // processedBitmap = applySaturation(processedBitmap)
            
            Log.d("WallpaperUtils", "Applied wallpaper effects: blur=${config.enableBlur}")
        } catch (e: Exception) {
            Log.e("WallpaperUtils", "Failed to apply wallpaper effects", e)
            // 如果处理失败，返回原始bitmap
            processedBitmap = bitmap
        }
        
        return processedBitmap
    }
    
    /**
     * 应用Cloudy模糊效果
     */
    private fun applyCloudyBlur(context: Context, bitmap: Bitmap, config: WallpaperEffectConfig): Bitmap {
        return try {
            // 使用Cloudy库进行模糊处理
            val blurredBitmap = Cloudy.with(context)
                .radius(config.blurRadius.toInt())
                .sampling(config.blurSampling.toInt())
                .animate(0) // 不使用动画
                .bitmap(bitmap)
            
            Log.d("WallpaperUtils", "Applied Cloudy blur: radius=${config.blurRadius}, sampling=${config.blurSampling}")
            blurredBitmap ?: bitmap
        } catch (e: Exception) {
            Log.e("WallpaperUtils", "Failed to apply Cloudy blur", e)
            // 如果Cloudy模糊失败，尝试使用原生模糊
            applyNativeBlur(bitmap, config.blurRadius)
        }
    }
    
    /**
     * 原生模糊处理（备用方案）
     */
    private fun applyNativeBlur(bitmap: Bitmap, radius: Float): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // 使用RenderScript模糊（API 17+）
                applyRenderScriptBlur(bitmap, radius)
            } else {
                // 对于旧版本，返回原始bitmap
                bitmap
            }
        } catch (e: Exception) {
            Log.e("WallpaperUtils", "Failed to apply native blur", e)
            bitmap
        }
    }
    
    /**
     * RenderScript模糊处理
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun applyRenderScriptBlur(bitmap: Bitmap, radius: Float): Bitmap {
        return try {
            // 这里可以实现RenderScript模糊
            // 由于RenderScript较复杂且已被弃用，这里简单返回原始bitmap
            // 实际项目中建议使用Cloudy或其他现代模糊库
            Log.d("WallpaperUtils", "RenderScript blur not implemented, returning original bitmap")
            bitmap
        } catch (e: Exception) {
            Log.e("WallpaperUtils", "RenderScript blur failed", e)
            bitmap
        }
    }
    
    /**
     * 保存处理后的bitmap
     */
    private fun saveProcessedBitmap(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val internalDir = File(context.filesDir, "wallpapers")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            
            val wallpaperFile = File(internalDir, CUSTOM_WALLPAPER_FILENAME)
            FileOutputStream(wallpaperFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            
            // 保存路径到SharedPreferences
            val prefs = getSharedPreferences(context)
            prefs.edit().putString(KEY_CUSTOM_WALLPAPER_PATH, wallpaperFile.absolutePath).apply()
            
            Log.d("WallpaperUtils", "Processed wallpaper saved: ${wallpaperFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("WallpaperUtils", "Failed to save processed bitmap", e)
            false
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
    ): ActivityResultLauncher<android.content.Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // 使用新的统一处理方法
                    handleSelectedWallpaper(activity, uri, callback)
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
            // 修复：为config提供默认值，避免null pointer异常
            val config = bitmap.config ?: Bitmap.Config.ARGB_8888
            val filteredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
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