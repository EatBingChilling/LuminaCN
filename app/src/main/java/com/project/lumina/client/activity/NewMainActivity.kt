/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.activity

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.project.lumina.client.constructors.ArrayListManager
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.game.module.api.config.ConfigManagerElement
import com.project.lumina.client.navigation.Navigation
import com.project.lumina.client.overlay.mods.OverlayNotification
import com.project.lumina.client.overlay.mods.PacketNotificationOverlay
import com.project.lumina.client.overlay.mods.TargetHudOverlay
import com.project.lumina.client.service.DynamicIslandService
import com.project.lumina.client.service.ESPService // ======== 新增：ESP Service ======== 导入我们的新服务
import com.project.lumina.client.ui.theme.LuminaClientTheme
import com.project.lumina.client.util.HashCat
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.JdkLoggerFactory


class NewMainActivity : ComponentActivity() {

    // ======== 移除：ESP Overlay ======== 不再需要 View 的引用
    // private lateinit var espOverlayView: ESPOverlayView

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
        
        private const val ACCESSIBILITY_SERVICE_CLS = "com.project.lumina.client.service.KeyCaptureService"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabled.any { it.id.contains(packageName) }
    }

    private fun ensureAccessibilityPermission() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请开启 LuminaCN 的无障碍权限", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.i("NewMainActivity", "Overlay permission has been granted by user.")
                startDynamicIslandService()
                startEspService() // ======== 新增：ESP Service ======== 权限授予后也启动 ESP 服务
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

    private fun checkOverlayPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.i("NewMainActivity", "Overlay permission not found. Requesting...")
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } else {
                Log.d("NewMainActivity", "Overlay permission already granted. Starting services.")
                startDynamicIslandService()
                startEspService() // ======== 新增：ESP Service ======== 权限已有时，启动 ESP 服务
            }
        } else {
            Log.d("NewMainActivity", "Device is pre-Marshmallow. Starting services directly.")
            startDynamicIslandService()
            startEspService() // ======== 新增：ESP Service ======== 旧版本安卓直接启动
        }
    }

    private fun startDynamicIslandService() {
        Log.d("NewMainActivity", "Attempting to start DynamicIslandService.")
        val intent = Intent(this, DynamicIslandService::class.java)
        try {
            startService(intent)
            Log.i("NewMainActivity", "DynamicIslandService started successfully.")
        } catch (e: Exception) {
            Log.e("NewMainActivity", "Failed to start DynamicIslandService.", e)
        }
    }

    // ======== 新增：ESP Service ========
    // 启动 ESP 服务的函数
    private fun startEspService() {
        Log.d("NewMainActivity", "Attempting to start ESPService.")
        val intent = Intent(this, ESPService::class.java)
        try {
            startService(intent)
            Log.i("NewMainActivity", "ESPService started successfully.")
        } catch (e: Exception) {
            Log.e("NewMainActivity", "Failed to start ESPService.", e)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ensureAccessibilityPermission()

        OverlayNotification.init(applicationContext)
        PacketNotificationOverlay.init(applicationContext)
        TargetHudOverlay.init(applicationçContext)

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
        val verifier = HashCat.getInstance()
        val isValid = verifier.LintHashInit(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestStoragePermissions()
        checkOverlayPermissionAndStartService() // <-- 这里会负责启动我们的服务

        setContent {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                LuminaClientTheme {
                    Navigation()
                }
            }
        }

        // ======== 移除：ESP Overlay ========
        // addEspOverlay() 函数及其调用已被移除
    }
    
    override fun onResume() {
        super.onResume()
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "LuminaCN 需要无障碍权限才能正常工作", Toast.LENGTH_LONG).show()
            ensureAccessibilityPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ======== 新增：ESP Service ========
        // 在 Activity 销毁时停止 ESP 服务，以清理悬浮窗
        stopService(Intent(this, ESPService::class.java))
        Log.i("NewMainActivity", "ESPService stopped.")

        ArrayListManager.releaseSounds()
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}