/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.activity

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo // <--- 在这里添加导入
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
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.constructors.ArrayListManager
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.constructors.RealmManager
import com.project.lumina.client.game.module.api.config.ConfigManagerElement
import com.project.lumina.client.navigation.Navigation
import com.project.lumina.client.overlay.mods.OverlayNotification
import com.project.lumina.client.overlay.mods.PacketNotificationOverlay
import com.project.lumina.client.overlay.mods.TargetHudOverlay
import com.project.lumina.client.service.DynamicIslandService
import com.project.lumina.client.ui.theme.LuminaClientTheme
import com.project.lumina.client.util.HashCat
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.JdkLoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewMainActivity : ComponentActivity() {

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

        /* ======== 新增：无障碍 Service 全类名 ======== */
        /* 把这一行改成你声明的 Service 的完整类名即可 */
        private const val ACCESSIBILITY_SERVICE_CLS =
            "com.project.lumina.client.service.KeyCaptureService"

    }

    /* ======== 新增：判断是否已启用我们的无障碍服务 ======== */
    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        // <--- 这里是修复点：使用导入的 AccessibilityServiceInfo.FEEDBACK_ALL
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL)
        return enabled.any { it.id.contains(packageName) }
    }

    /* ======== 新增：确保无障碍权限 ======== */
    private fun ensureAccessibilityPermission() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请开启 LuminaCN 的无障碍权限", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    /* ======== 原有代码保持不变 ======== */
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.i("NewMainActivity", "Overlay permission has been granted by user.")
                startDynamicIslandService()
            } else {
                Log.w("NewMainActivity", "Overlay permission was not granted by user.")
                Toast.makeText(this, "悬浮窗权限未授予，灵动岛无法显示", Toast.LENGTH_LONG).show()
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
                Log.d("NewMainActivity", "Overlay permission already granted. Starting service.")
                startDynamicIslandService()
            }
        } else {
            Log.d("NewMainActivity", "Device is pre-Marshmallow. Starting service directly.")
            startDynamicIslandService()
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

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* ======== 新增：检查并引导开启无障碍 ======== */
        ensureAccessibilityPermission()

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
        val verifier = HashCat.getInstance()
        val isValid = verifier.LintHashInit(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestStoragePermissions()
        checkOverlayPermissionAndStartService()

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

    /* ======== 新增：每次回到前台再检查一次 ======== */
    override fun onResume() {
        super.onResume()
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "LuminaCN 需要无障碍权限才能正常工作", Toast.LENGTH_LONG).show()
            ensureAccessibilityPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ArrayListManager.releaseSounds()
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}