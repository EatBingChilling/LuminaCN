// File: com/phoenix/luminacn/activity/NewMainActivity.kt

package com.phoenix.luminacn.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.phoenix.luminacn.WallpaperUtils
import com.phoenix.luminacn.application.AppContext
import com.phoenix.luminacn.constructors.ArrayListManager
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.game.module.api.config.ConfigManagerElement
import com.phoenix.luminacn.navigation.Navigation
import com.phoenix.luminacn.overlay.mods.OverlayNotification
import com.phoenix.luminacn.overlay.mods.PacketNotificationOverlay
import com.phoenix.luminacn.overlay.mods.TargetHudOverlay
import com.phoenix.luminacn.ui.theme.LuminaClientTheme
import com.phoenix.luminacn.util.HashCat
import com.phoenix.luminacn.music.MusicObserver
import com.phoenix.luminacn.viewmodel.MainScreenViewModel
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.JdkLoggerFactory

class NewMainActivity : ComponentActivity() {

    // This line instantiates the ViewModel, causing its `init` block to execute
    // and load all application data and settings at launch.
    private val mainScreenViewModel: MainScreenViewModel by viewModels()

    companion object {
        private var currentInstance: NewMainActivity? = null

        fun launchConfigImport() {
            if (currentInstance == null) {
                Log.e("NewMainActivity", "Error: NewMainActivity instance is null when trying to import config")
                return
            }

            currentInstance?.let { activity ->
                try {
                    Log.d("NewMainActivity", "Launching config import from activity: ${activity.javaClass.simpleName}")
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    activity.importConfigLauncher.launch(intent)
                    Log.d("NewMainActivity", "Import launcher launched successfully")
                } catch (e: Exception) {
                    Log.e("NewMainActivity", "Error launching import: ${e.message}", e)
                }
            }
        }

        /**
         * 启动壁纸选择器（公共方法）
         * 可以从其他地方调用来让用户选择壁纸
         */
        fun launchWallpaperPicker() {
            if (currentInstance == null) {
                Log.e("NewMainActivity", "Error: NewMainActivity instance is null when trying to launch wallpaper picker")
                return
            }

            currentInstance?.let { activity ->
                try {
                    Log.d("NewMainActivity", "Launching wallpaper picker from external call")
                    activity.launchWallpaperPickerInternal()
                } catch (e: Exception) {
                    Log.e("NewMainActivity", "Error launching wallpaper picker: ${e.message}", e)
                }
            }
        }

        /**
         * 检查当前是否有Activity实例可用
         */
        fun isAvailable(): Boolean = currentInstance != null &&
            !currentInstance!!.isFinishing && !currentInstance!!.isDestroyed
    }

    // ====================== 【壁纸相关】 ======================
    private lateinit var wallpaperCallback: AppContext.WallpaperStatusCallback
    private var hasShownWallpaperDialog = false
    
    // 壁纸选择器 - 修改为传回WallpaperUtils处理
    private val wallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d("NewMainActivity", "Wallpaper selected: $uri")
                // 传回给WallpaperUtils统一处理
                WallpaperUtils.handleSelectedWallpaper(this, uri, object : WallpaperUtils.WallpaperSelectorCallback {
                    override fun onWallpaperSelected() {
                        Toast.makeText(this@NewMainActivity, "壁纸设置成功", Toast.LENGTH_SHORT).show()
                        Log.d("NewMainActivity", "Wallpaper set successfully")
                        
                        // 更新AppContext中的壁纸状态
                        if (AppContext.instance.getWallpaperStatus() != AppContext.WallpaperStatus.AVAILABLE) {
                            // 重新检查壁纸状态，触发预加载
                            Handler(Looper.getMainLooper()).postDelayed({
                                AppContext.instance.preloadWallpaper()
                            }, 500)
                        }
                    }
                    
                    override fun onWallpaperSelectionCancelled() {
                        Toast.makeText(this@NewMainActivity, "壁纸设置失败", Toast.LENGTH_SHORT).show()
                        Log.w("NewMainActivity", "Wallpaper setting failed")
                    }
                })
            } ?: run {
                Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show()
                Log.w("NewMainActivity", "No image selected")
            }
        } else {
            Log.d("NewMainActivity", "User cancelled wallpaper selection")
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.i("NewMainActivity", "Overlay permission has been granted by user.")
                (applicationContext as AppContext)//.checkOverlayPermissionAndStartServices()
            } else {
                Log.w("NewMainActivity", "Overlay permission was not granted by user.")
                Toast.makeText(this, "悬浮窗权限未授予，部分功能无法显示", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("NewMainActivity", "Storage permissions granted")
        } else {
            Log.w("NewMainActivity", "Storage permissions not granted")
        }
    }

    // ====================== 【新增：通知权限申请】 ======================
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("NewMainActivity", "Notification permission granted")
            Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
            
            // 权限授予后，启动音乐观察服务（如果需要的话）
            startMusicObserverIfNeeded()
        } else {
            Log.w("NewMainActivity", "Notification permission denied")
            showNotificationPermissionDeniedDialog()
        }
    }

    val importConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                (GameManager.elements.find { it.name == "config_manager" } as? ConfigManagerElement)?.let { configManager ->
                    configManager.importConfig(uri)
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNotGranted)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("NewMainActivity", "Failed to request MANAGE_EXTERNAL_STORAGE permission", e)
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.i("NewMainActivity", "Overlay permission not found. Requesting...")
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }
    }

    // ====================== 【新增：通知权限相关方法】 ======================
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NewMainActivity", "Notification permission already granted")
                    startMusicObserverIfNeeded()
                }
                
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionRationaleDialog()
                }
                
                else -> {
                    Log.d("NewMainActivity", "Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13 以下不需要POST_NOTIFICATIONS权限
            Log.d("NewMainActivity", "Notification permission not required on this Android version")
            startMusicObserverIfNeeded()
        }
    }

    private fun showNotificationPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("需要通知权限")
            .setMessage("LuminaCN需要通知权限来显示音乐播放信息和系统状态。请授予通知权限以获得完整功能。")
            .setPositiveButton("授予权限") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("稍后设置") { _, _ ->
                Log.d("NewMainActivity", "User chose to grant notification permission later")
            }
            .setCancelable(false)
            .show()
    }

    private fun showNotificationPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("通知权限被拒绝")
            .setMessage("没有通知权限，音乐播放信息等功能将无法正常工作。您可以稍后在设置中手动授予权限。")
            .setPositiveButton("前往设置") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("确定") { _, _ ->
                Log.d("NewMainActivity", "User acknowledged notification permission denial")
            }
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("NewMainActivity", "Failed to open app settings", e)
            Toast.makeText(this, "无法打开设置页面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMusicObserverIfNeeded() {
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val musicModeEnabled = prefs.getBoolean("musicModeEnabled", true)
        
        if (musicModeEnabled) {
            MusicObserver.checkAndRequestPermissions(
                activity = this,
                notificationPermissionLauncher = notificationPermissionLauncher,
                onPermissionsGranted = {
                    MusicObserver.start(this)
                    Log.d("NewMainActivity", "Music observer started successfully")
                }
            )
        }
    }

    // ====================== 【壁纸处理方法】 ======================
    
    private fun setupWallpaperStatusListener() {
        wallpaperCallback = object : AppContext.WallpaperStatusCallback {
            override fun onWallpaperStatusChanged(status: AppContext.WallpaperStatus) {
                when (status) {
                    AppContext.WallpaperStatus.NEEDS_SETUP -> {
                        if (!hasShownWallpaperDialog && !isFinishing && !isDestroyed) {
                            showWallpaperSetupDialog()
                        }
                    }
                    AppContext.WallpaperStatus.AVAILABLE -> {
                        Log.d("NewMainActivity", "Wallpaper is available")
                        AppContext.instance.preloadWallpaper()
                    }
                    AppContext.WallpaperStatus.SETTING_UP -> {
                        Log.d("NewMainActivity", "Wallpaper is being set up")
                    }
                    AppContext.WallpaperStatus.UNKNOWN -> {
                        Log.w("NewMainActivity", "Wallpaper status unknown")
                    }
                }
            }
        }
        
        AppContext.instance.addWallpaperStatusCallback(wallpaperCallback)
    }
    
    private fun showWallpaperSetupDialog() {
        if (hasShownWallpaperDialog) return
        hasShownWallpaperDialog = true
        
        // 延迟显示，避免启动时太突兀
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("需要设置壁纸")
                    .setMessage("由于系统权限限制，需要您手动选择一张图片作为应用壁纸。这只需要设置一次。")
                    .setPositiveButton("选择图片") { _, _ ->
                        launchWallpaperPickerInternal()
                    }
                    .setNegativeButton("稍后设置") { _, _ ->
                        // 允许用户稍后设置，但会在下次启动时再次提示
                        hasShownWallpaperDialog = false
                    }
                    .setCancelable(false)
                    .show()
            }
        }, 2000) // 延迟2秒显示
    }
    
    /**
     * 内部壁纸选择器启动方法
     */
    private fun launchWallpaperPickerInternal() {
        try {
            Log.d("NewMainActivity", "Launching wallpaper picker")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp", "image/bmp"))
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                putExtra("android.provider.extra.SHOW_ADVANCED", true)
            }
            wallpaperPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("NewMainActivity", "Failed to launch wallpaper picker", e)
            Toast.makeText(this, "无法启动图片选择器：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 公共方法：显示壁纸选择器
     * 可以从设置页面等地方调用
     */
    fun showWallpaperPicker() {
        if (isFinishing || isDestroyed) {
            Log.w("NewMainActivity", "Activity is finishing or destroyed, cannot show wallpaper picker")
            return
        }
        
        try {
            MaterialAlertDialogBuilder(this)
                .setTitle("选择壁纸")
                .setMessage("选择一张图片作为应用背景壁纸")
                .setPositiveButton("选择图片") { _, _ ->
                    launchWallpaperPickerInternal()
                }
                .setNeutralButton("清除壁纸") { _, _ ->
                    clearCurrentWallpaper()
                }
                .setNegativeButton("取消", null)
                .show()
        } catch (e: Exception) {
            Log.e("NewMainActivity", "Failed to show wallpaper picker dialog", e)
            // 如果对话框显示失败，直接启动选择器
            launchWallpaperPickerInternal()
        }
    }

    /**
     * 清除当前壁纸
     */
    private fun clearCurrentWallpaper() {
        try {
            WallpaperUtils.clearCustomWallpaper(this)
            Toast.makeText(this, "壁纸已清除", Toast.LENGTH_SHORT).show()
            Log.d("NewMainActivity", "Custom wallpaper cleared")
        } catch (e: Exception) {
            Log.e("NewMainActivity", "Failed to clear wallpaper", e)
            Toast.makeText(this, "清除壁纸失败", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        OverlayNotification.init(applicationContext)
        PacketNotificationOverlay.init(applicationContext)
        TargetHudOverlay.init(applicationContext)

        val prefs = getSharedPreferences("lumina_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("guide_done", false)) {
            startActivity(Intent(this, HelpActivity::class.java))
            finish()
            return
        }

        currentInstance = this
        ArrayListManager.initializeSounds(this)

        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE)
        Log.i("MainApplication", "Forced Netty to use JUL logger instead of Log4j2.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        enableEdgeToEdge()
        HashCat.getInstance().LintHashInit(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestStoragePermissions()
        requestOverlayPermission()
        
        requestNotificationPermission()
        
        setupWallpaperStatusListener()

        setContent {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                LuminaClientTheme {
                    Navigation()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ArrayListManager.releaseSounds()
        
        if (::wallpaperCallback.isInitialized) {
            AppContext.instance.removeWallpaperStatusCallback(wallpaperCallback)
        }
        
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}