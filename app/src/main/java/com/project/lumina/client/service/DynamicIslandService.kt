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
        // ... (之前的 Action 和 Extra)
        const val ACTION_UPDATE_TEXT = "com.project.lumina.client.ACTION_UPDATE_TEXT"
        const val ACTION_UPDATE_Y_OFFSET = "com.project.lumina.client.ACTION_UPDATE_Y_OFFSET"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_Y_OFFSET_DP = "extra_y_offset_dp"
        
        const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.project.lumina.client.ACTION_SHOW_NOTIFICATION_SWITCH"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_STATE = "extra_module_state"
        
        const val ACTION_SHOW_PROGRESS = "com.project.lumina.client.ACTION_SHOW_PROGRESS"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"

        // 【新增】为 TargetHUD 创建专用的 Action 和 Extra，以更好地区分和去重
        const val ACTION_UPDATE_TARGET_HUD = "com.project.lumina.client.ACTION_UPDATE_TARGET_HUD"
        const val EXTRA_TARGET_USERNAME = "extra_target_username"
        const val EXTRA_TARGET_DISTANCE = "extra_target_distance"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingWindow()
    }

    private fun showFloatingWindow() {
        if (dynamicIslandView != null) return

        val themedContext = ContextThemeWrapper(this, R.style.Theme_LuminaClient)
        dynamicIslandView = DynamicIslandView(themedContext)
        
        windowParams = WindowManager.LayoutParams(
            // 【修改】让悬浮窗宽度占满屏幕，这是实现真·居中的关键
            WindowManager.LayoutParams.MATCH_PARENT, 
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START // 改为 START，因为我们手动处理水平居中
            x = 0 // 从屏幕最左边开始
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
                // ... (其他 action case 保持不变)
                ACTION_UPDATE_TEXT, ACTION_UPDATE_Y_OFFSET, ACTION_SHOW_NOTIFICATION_SWITCH, ACTION_SHOW_PROGRESS -> {
                     handleStandardActions(intent)
                }
                
                // 【新增】处理 TargetHUD 的更新
                ACTION_UPDATE_TARGET_HUD -> {
                    val username = intent.getStringExtra(EXTRA_TARGET_USERNAME)
                    val distance = intent.getFloatExtra(EXTRA_TARGET_DISTANCE, 0f)
                    
                    if (username != null) {
                        val title = "$username (${"%.1f".format(distance)}m)"
                        // 调用 addProgress，并使用 username 作为去重的 key
                        dynamicIslandView?.addProgress(title, null, 3000L, username)
                    }
                }
            }
        }
        return START_STICKY
    }

    // 将标准 Action 的处理逻辑提取到一个函数中，保持 onStartCommand 的整洁
    private fun handleStandardActions(intent: Intent) {
        when (intent.action) {
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
                    try { ContextCompat.getDrawable(this, iconResId) } catch (e: Exception) { null }
                } else {
                    null
                }
                if (title != null) {
                    // 对于通用进度条，我们使用 title 作为 key 来去重
                    dynamicIslandView?.addProgress(title, iconDrawable, duration, title)
                }
            }
        }
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