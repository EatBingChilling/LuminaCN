package com.project.lumina.client.activity

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.project.lumina.client.R

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 删除错误的 setTheme() 调用 - 这是问题根源
        // setTheme(com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight_NoActionBar)
        
        // 使用标准的无ActionBar主题（确保在AndroidManifest.xml中已配置）
        setTheme(R.style.Theme_Phoenix) 
        
        // 2. 开启 Edge-to-Edge
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // 3. 使用代码完全移除ActionBar
        supportActionBar?.hide() // 关键修复：确保隐藏ActionBar

        // 4. 沉浸式全屏（隐藏状态栏+导航栏）
        hideSystemUI()

        setContentView(R.layout.activity_loading_md3)

        // 5. 处理系统栏 padding（可选：如果布局需要避开系统栏区域保留）
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        verificationManager = AppVerificationManager(this) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(android.content.Intent(this, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }, 800)
        }
        verificationManager.startVerification()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::verificationManager.isInitialized) verificationManager.onDestroy()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 推荐写法
            window.setDecorFitsSystemWindows(false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 及以下兼容写法
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN           // 隐藏状态栏
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION   // 隐藏导航栏
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  // 手势滑出后自动再隐藏
            )
        }
    }
}
