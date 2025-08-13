// ClientOverlay.kt  最终修复版
package com.project.lumina.client.overlay.mods

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class ClientOverlay : OverlayWindow() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", false))
    private var fontSize by mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300))
    private var rainbowEnabled by mutableStateOf(prefs.getBoolean("rainbow", false))
    private var opacity by mutableStateOf(prefs.getInt("opacity", 100).coerceIn(0, 100))

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
            if (!enabled) dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        fun showConfigDialog() {
            overlayInstance?.showConfigDialog()
        }
    }

    fun showConfigDialog() {
        val inflater = LayoutInflater.from(appContext)
        val dialogView = inflater.inflate(R.layout.dialog_watermark_config, null)
        
        // 初始化视图
        val textInput = dialogView.findViewById<EditText>(R.id.watermark_text)
        val colorPreview = dialogView.findViewById<View>(R.id.color_preview)
        val redSlider = dialogView.findViewById<SeekBar>(R.id.red_slider)
        val greenSlider = dialogView.findViewById<SeekBar>(R.id.green_slider)
        val blueSlider = dialogView.findViewById<SeekBar>(R.id.blue_slider)
        val sizeSlider = dialogView.findViewById<SeekBar>(R.id.size_slider)
        val opacitySlider = dialogView.findViewById<SeekBar>(R.id.opacity_slider)
        val shadowSwitch = dialogView.findViewById<Switch>(R.id.shadow_switch)
        val rainbowSwitch = dialogView.findViewById<Switch>(R.id.rainbow_switch)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)
        
        // 设置当前值
        textInput.setText(watermarkText)
        
        val red = Color.red(textColor)
        val green = Color.green(textColor)
        val blue = Color.blue(textColor)
        
        redSlider.progress = red
        greenSlider.progress = green
        blueSlider.progress = blue
        sizeSlider.progress = fontSize - 5
        opacitySlider.progress = opacity
        shadowSwitch.isChecked = shadowEnabled
        rainbowSwitch.isChecked = rainbowEnabled
        
        // 更新颜色预览
        updateColorPreview(colorPreview, red, green, blue)
        
        // 颜色滑块监听器
        val colorChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColorPreview(
                    colorPreview,
                    redSlider.progress,
                    greenSlider.progress,
                    blueSlider.progress
                )
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        
        redSlider.setOnSeekBarChangeListener(colorChangeListener)
        greenSlider.setOnSeekBarChangeListener(colorChangeListener)
        blueSlider.setOnSeekBarChangeListener(colorChangeListener)
        
        // 创建对话框
        val dialog = android.app.Dialog(appContext) // 明确指定android.app.Dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        
        // 设置对话框窗口参数
        val window = dialog.window
        if (window != null) {
            val params = window.attributes
            params.width = (appContext.resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.CENTER
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            window.attributes = params
        }
        
        // 按钮监听器
        cancelButton.setOnClickListener { dialog.dismiss() }
        
        saveButton.setOnClickListener {
            watermarkText = textInput.text.toString()
            textColor = Color.rgb(redSlider.progress, greenSlider.progress, blueSlider.progress)
            shadowEnabled = shadowSwitch.isChecked
            fontSize = sizeSlider.progress + 5
            rainbowEnabled = rainbowSwitch.isChecked
            opacity = opacitySlider.progress
            
            prefs.edit()
                .putString("text", watermarkText)
                .putInt("color", textColor)
                .putBoolean("shadow", shadowEnabled)
                .putInt("size", fontSize)
                .putBoolean("rainbow", rainbowEnabled)
                .putInt("opacity", opacity)
                .apply()
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateColorPreview(view: View, red: Int, green: Int, blue: Int) {
        view.setBackgroundColor(Color.rgb(red, green, blue))
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val unifontFamily = FontFamily(Font(R.font.unifont))
        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

        var rainbowColor by remember { mutableStateOf(ComposeColor.White) }

        LaunchedEffect(rainbowEnabled) {
            try {
                if (rainbowEnabled) {
                    while (isActive) {
                        val hue = (System.currentTimeMillis() % 3600L) / 10f
                        rainbowColor = ComposeColor.hsv(hue, 1f, 1f)
                        delay(50L)
                    }
                }
            } catch (e: CancellationException) {
                // 正常取消，无需处理
            }
        }

        val baseColor = if (rainbowEnabled) rainbowColor else ComposeColor(textColor)
        val finalColor = baseColor.copy(alpha = opacity / 100f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (shadowEnabled) {
                Text(
                    text = text,
                    fontSize = fontSize.sp,
                    fontFamily = unifontFamily,
                    color = ComposeColor.Black.copy(alpha = 0.15f),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.5).sp,
                    letterSpacing = (fontSize * 0.1).sp,
                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                )
            }

            Text(
                text = text,
                fontSize = fontSize.sp,
                fontFamily = unifontFamily,
                color = finalColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize * 1.2).sp,
                letterSpacing = (fontSize * 0.1).sp
            )
        }
    }
}
