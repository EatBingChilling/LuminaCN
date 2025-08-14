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
import kotlin.math.roundToInt

import com.project.lumina.client.phoenix.DynamicIslandView


class DynamicIslandService : Service() {

    private lateinit var windowManager: WindowManager
    private var dynamicIslandView: DynamicIslandView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    companion object {
        const val ACTION_UPDATE_TEXT = "com.phoen1x.bar.ACTION_UPDATE_TEXT"
        const val ACTION_UPDATE_Y_OFFSET = "com.phoen1x.bar.ACTION_UPDATE_Y_OFFSET"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_Y_OFFSET_DP = "extra_y_offset_dp"

        const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.phoen1x.bar.ACTION_SHOW_NOTIFICATION_SWITCH"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_STATE = "extra_module_state"

        const val ACTION_SHOW_OR_UPDATE_PROGRESS = "com.phoen1x.bar.ACTION_SHOW_OR_UPDATE_PROGRESS"
        const val EXTRA_IDENTIFIER = "extra_identifier"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_PROGRESS_VALUE = "extra_progress_value"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingWindow()
    }

    private fun showFloatingWindow() {
    if (dynamicIslandView != null) return

    val themedContext = ContextThemeWrapper(
        this,
        com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight
    )
    dynamicIslandView = DynamicIslandView(themedContext)

    windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = 0 // 直接覆盖状态栏
    }

    windowManager.addView(dynamicIslandView, windowParams)
}


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_STICKY
        when (intent.action) {
            ACTION_UPDATE_TEXT -> intent.getStringExtra(EXTRA_TEXT)?.let {
                dynamicIslandView?.setPersistentText(it)
            }

            ACTION_UPDATE_Y_OFFSET -> {
                val yOffsetDp = intent.getFloatExtra(EXTRA_Y_OFFSET_DP, 20f)
                windowParams?.let {
                    it.y = dpToPx(yOffsetDp)
                    windowManager.updateViewLayout(dynamicIslandView, it)
                }
            }

            ACTION_SHOW_NOTIFICATION_SWITCH -> intent.getStringExtra(EXTRA_MODULE_NAME)?.let { name ->
                dynamicIslandView?.addSwitch(name, intent.getBooleanExtra(EXTRA_MODULE_STATE, false))
            }

            ACTION_SHOW_OR_UPDATE_PROGRESS -> handleShowOrUpdateProgress(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        dynamicIslandView?.let { windowManager.removeView(it) }
        dynamicIslandView = null
    }

    /* ---------- 私有工具 ---------- */

    private fun dpToPx(dp: Float): Int =
        (dp * resources.displayMetrics.density).roundToInt()

    /* ---------- Progress 处理 ---------- */

    private fun handleShowOrUpdateProgress(intent: Intent) {
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return

        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)

        // 1. progress
        val progress = intent.takeIf { it.hasExtra(EXTRA_PROGRESS_VALUE) }
            ?.getFloatExtra(EXTRA_PROGRESS_VALUE, 0f)

        // 2. duration：仅在 progress 为 null 且存在 EXTRA_DURATION_MS 时取
        val duration = if (progress == null && intent.hasExtra(EXTRA_DURATION_MS)) {
            intent.getLongExtra(EXTRA_DURATION_MS, 5000L)
        } else {
            null
        }

        // 3. icon
        val iconDrawable = intent.getIntExtra(EXTRA_ICON_RES_ID, -1)
            .takeIf { it != -1 }
            ?.let { runCatching { ContextCompat.getDrawable(this, it) }.getOrNull() }

        dynamicIslandView?.addOrUpdateProgress(
            identifier, title, subtitle, iconDrawable, progress, duration
        )
    }
}
