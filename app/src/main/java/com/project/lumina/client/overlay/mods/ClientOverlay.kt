package com.project.lumina.client.overlay.mods

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow

class ClientOverlay : OverlayWindow() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", true))
    private var fontSize by mutableStateOf(prefs.getInt("size", 18))

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private var overlayInstance: ClientOverlay? = null
        private var shouldShowOverlay = true

        // 获取全局 Application Context
        private val appContext: Context by lazy {
            val appClass = Class.forName("android.app.ActivityThread")
            val method = appClass.getMethod("currentApplication")
            method.invoke(null) as Application
        }

        fun showOverlay() {
            if (shouldShowOverlay) {
                overlayInstance = ClientOverlay()
                try {
                    OverlayManager.showOverlayWindow(overlayInstance!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun dismissOverlay() {
            try {
                overlayInstance?.let { OverlayManager.dismissOverlayWindow(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) {
                dismissOverlay()
            }
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        fun showConfigDialog() {
            overlayInstance?.showConfigDialog()
        }
    }

    fun showConfigDialog() {
        val dialogView = LayoutInflater.from(appContext).inflate(R.layout.overlay_config_dialog, null)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        val seekRed = dialogView.findViewById<SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<SeekBar>(R.id.seekBlue)
        val switchShadow = dialogView.findViewById<Switch>(R.id.switchShadow)
        val seekSize = dialogView.findViewById<SeekBar>(R.id.seekSize)

        editText.setText(watermarkText)
        seekRed.progress = Color.red(textColor)
        seekGreen.progress = Color.green(textColor)
        seekBlue.progress = Color.blue(textColor)
        switchShadow.isChecked = shadowEnabled
        seekSize.progress = fontSize

        val dialog = AlertDialog.Builder(appContext)
            .setTitle("配置水印")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                watermarkText = editText.text.toString()
                val red = seekRed.progress
                val green = seekGreen.progress
                val blue = seekBlue.progress
                textColor = Color.rgb(red, green, blue)
                shadowEnabled = switchShadow.isChecked
                fontSize = seekSize.progress

                prefs.edit()
                    .putString("text", watermarkText)
                    .putInt("color", textColor)
                    .putBoolean("shadow", shadowEnabled)
                    .putInt("size", fontSize)
                    .apply()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val firaSansFamily = FontFamily.Default


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

            if (shadowEnabled) {
                for (i in 1..5) {
                    Text(
                        text = text,
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = firaSansFamily,
                        color = ComposeColor.Black.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(start = 8.dp, top = 13.dp, bottom = 8.dp)
                            .offset(x = (i * 0.5f).dp, y = (i * 0.5f).dp)
                    )
                }
            }

            Text(
                text = text,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = firaSansFamily,
                color = ComposeColor(textColor),
                modifier = Modifier
                    .padding(start = 8.dp, top = 13.dp, bottom = 8.dp)
            )
        }
    }
}
