package com.project.lumina.client.overlay.mods

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class ClientOverlay : OverlayWindow() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", false))
    // 字体范围扩大到5-300sp
    private var fontSize by mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300)) 
    private var rainbowEnabled by mutableStateOf(prefs.getBoolean("rainbow", false))

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private var overlayInstance: ClientOverlay? = null
        private var shouldShowOverlay = true

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
        // 字体大小范围扩大到5-300sp
        val seekSize = dialogView.findViewById<SeekBar>(R.id.seekSize).apply {
            max = 295 // 300 - 5 = 295
        }
        val switchRainbow = dialogView.findViewById<Switch>(R.id.switchRainbow)
        val colorPreview = dialogView.findViewById<TextView>(R.id.colorPreview)

        editText.setText(watermarkText)
        seekRed.progress = Color.red(textColor)
        seekGreen.progress = Color.green(textColor)
        seekBlue.progress = Color.blue(textColor)
        switchShadow.isChecked = shadowEnabled
        // 映射字体大小到控件范围
        seekSize.progress = fontSize - 5
        switchRainbow.isChecked = rainbowEnabled

        fun updateColorPreview() {
            val color = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
            colorPreview.setBackgroundColor(color)
        }

        updateColorPreview()

        seekRed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColorPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekGreen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColorPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColorPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
                // 反映射字体大小到实际值 (5-300sp)
                fontSize = seekSize.progress + 5
                rainbowEnabled = switchRainbow.isChecked

                prefs.edit()
                    .putString("text", watermarkText)
                    .putInt("color", textColor)
                    .putBoolean("shadow", shadowEnabled)
                    .putInt("size", fontSize)
                    .putBoolean("rainbow", rainbowEnabled)
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

        // 修复字体问题
        val unifontFamily = FontFamily(Font(R.font.unifont))

        val text = "L u m i n a C N${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

        var rainbowColor by remember { mutableStateOf(ComposeColor.White) }

        LaunchedEffect(rainbowEnabled) {
            if (rainbowEnabled) {
                while (true) {
                    val hue = (System.currentTimeMillis() % 3600L) / 10f
                    rainbowColor = ComposeColor.hsv(hue, 1f, 1f)
                    delay(50L)
                }
            }
        }

        // 统一透明度设置（25%透明度）
        val baseColor = if (rainbowEnabled) rainbowColor else ComposeColor(textColor)
        val finalColor = baseColor.copy(alpha = 0.25f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (shadowEnabled) {
                // 使用轻量阴影效果（避免偏移导致居中问题）
                Text(
                    text = text,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = unifontFamily,
                    color = ComposeColor.Black.copy(alpha = 0.15f),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.5).sp, // 设置1.5倍行距
                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                )
            }

            Text(
                text = text,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = unifontFamily,
                color = finalColor,
                textAlign = TextAlign.Center, // 确保文本居中
                lineHeight = (fontSize * 1.5).sp // 设置1.5倍行距
            )
        }
    }
}
