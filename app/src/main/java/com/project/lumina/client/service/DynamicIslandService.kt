package com.project.lumina.client.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.project.lumina.client.R
import com.project.lumina.client.phoenix.DynamicIslandView
import kotlin.math.roundToInt

class DynamicIslandService : Service() {

    private lateinit var windowManager: WindowManager
    private var dynamicIslandView: DynamicIslandView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    companion object {
        // 用于设置的 Action 和 Extra
        const val ACTION_UPDATE_TEXT = "com.project.lumina.client.ACTION_UPDATE_TEXT"
        const val ACTION_UPDATE_Y_OFFSET = "com.project.lumina.client.ACTION_UPDATE_Y_OFFSET"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_Y_OFFSET_DP = "extra_y_offset_dp"
        
        // 用于显示模块开关通知的 Action 和 Extra
        const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.project.lumina.client.ACTION_SHOW_NOTIFICATION_SWITCH"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_STATE = "extra_module_state"
        
        // 用于显示进度条通知的 Action 和 Extra
        const val ACTION_SHOW_PROGRESS = "com.project.lumina.client.ACTION_SHOW_PROGRESS"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle" // 定义但未使用，保留原样
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingWindow()
    }

    private fun showFloatingWindow() {
        if (dynamicIslandView != null) return

        // 【修改】
        // 1. 创建一个 ContextThemeWrapper，将 Service 的基础上下文和您的 Material3 主题打包在一起。
        //    我们直接引用您在 themes.xml 中定义的主题，例如 R.style.Theme_LuminaClient。
        val themedContext = ContextThemeWrapper(this, R.style.Theme_LuminaClient)

        // 2. 使用这个被“主题化”的上下文来创建 DynamicIslandView，以确保它能访问到所有主题属性。
        dynamicIslandView = DynamicIslandView(themedContext)
        
        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val sp = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
            val yOffsetDp = sp.getFloat("dynamicIslandYOffset", 20f)
            y = dpToPx(yOffsetDp)
        }
        
        val sp = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val username = sp.getString("dynamicIslandUsername", "User") ?: "User"
        dynamicIslandView?.setPersistentText(username)

        windowManager.addView(dynamicIslandView, windowParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_UPDATE_TEXT -> {
                    val text = intent.getStringExtra(EXTRA_TEXT)
                    if (text != null) {
                        dynamicIslandView?.setPersistentText(text)
                    }
                }
                ACTION_UPDATE_Y_OFFSET -> {
                    val yOffsetDp = intent.getFloatExtra(EXTRA_Y_OFFSET_DP, 20f)
                    windowParams?.let { params ->
                        params.y = dpToPx(yOffsetDp)
                        windowManager.updateViewLayout(dynamicIslandView, params)
                    }
                }
                ACTION_SHOW_NOTIFICATION_SWITCH -> {
                    val moduleName = intent.getStringExtra(EXTRA_MODULE_NAME)
                    val moduleState = intent.getBooleanExtra(EXTRA_MODULE_STATE, false)
                    
                    if (moduleName != null) {
                        dynamicIslandView?.addSwitch(moduleName, moduleState)
                    }
                }
                ACTION_SHOW_PROGRESS -> {
                    val title = intent.getStringExtra(EXTRA_TITLE)
                    val duration = intent.getLongExtra(EXTRA_DURATION_MS, 1000L)
                    
                    val iconResId = intent.getIntExtra(EXTRA_ICON_RES_ID, -1)
                    val iconDrawable: Drawable? = if (iconResId != -1) {
                        try {
                            ContextCompat.getDrawable(this, iconResId)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    
                    if (title != null) {
                        // 修复：添加 identifier 参数为 null，以匹配 addProgress 方法的签名
                        dynamicIslandView?.addProgress(null, title, iconDrawable, duration)
                    }
                }
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (dynamicIslandView != null) {
            windowManager.removeView(dynamicIslandView)
            dynamicIslandView = null
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }
}