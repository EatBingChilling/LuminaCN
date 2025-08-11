/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.activity

import android.Manifest
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.project.lumina.client.constructors.ArrayListManager
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.game.module.api.config.ConfigManagerElement
import com.project.lumina.client.navigation.Navigation
import com.project.lumina.client.ui.theme.LuminaClientTheme
import com.project.lumina.client.util.HashCat
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.JdkLoggerFactory
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.constructors.RealmManager
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

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 首次启动检查：未引导则跳转 HelpActivity
        val prefs = getSharedPreferences("lumina_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("guide_done", false)) {
            startActivity(Intent(this, HelpActivity::class.java))
            finish()
            return
        }

        currentInstance = this
        
        ArrayListManager.initializeSounds(this)
        
        // 设置日志工厂
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE)
        Log.i("MainApplication", "Forced Netty to use JUL logger instead of Log4j2.")
        
        // 电池优化请求
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
        
        // EdgeToEdge 设置
        enableEdgeToEdge()
        
        // Hash验证
        val verifier = HashCat.getInstance()
        val isValid = verifier.LintHashInit(this)
        if (isValid) {
            // 如果验证成功，可以设置沉浸式模式（可选）
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // 验证失败时使用标准系统栏
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
        
        // 请求存储权限
        requestStoragePermissions()
        
        // 设置内容
        setContent {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                LuminaClientTheme {
                    Navigation()
                }
            }
        }
        
        // 初始化游戏管理器
        // lifecycleScope.launch {
        //     withContext(Dispatchers.IO) {
        //         try {
        //             GameManager.initialize(this@NewMainActivity)
        //             Log.d("NewMainActivity", "GameManager initialized successfully")
        //         } catch (e: Exception) {
        //             Log.e("NewMainActivity", "Failed to initialize GameManager", e)
        //         }
        //     }
        // }
        
        // 初始化账户管理器
        // lifecycleScope.launch {
        //     withContext(Dispatchers.IO) {
        //         try {
        //             AccountManager.initialize(this@NewMainActivity)
        //             Log.d("NewMainActivity", "AccountManager initialized successfully")
        //         } catch (e: Exception) {
        //             Log.e("NewMainActivity", "Failed to initialize AccountManager", e)
        //         }
        //     }
        // }
        
        // 初始化领域管理器
        // lifecycleScope.launch {
        //     withContext(Dispatchers.IO) {
        //         try {
        //             RealmManager.initialize(this@NewMainActivity)
        //             Log.d("NewMainActivity", "RealmManager initialized successfully")
        //         } catch (e: Exception) {
        //             Log.e("NewMainActivity", "Failed to initialize RealmManager", e)
        //         }
        //     }
        // }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ArrayListManager.releaseSounds()
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}
