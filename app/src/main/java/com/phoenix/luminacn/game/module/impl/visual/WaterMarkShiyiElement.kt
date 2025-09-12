package com.phoenix.luminacn.game.module.impl.visual

import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.util.AssetManager
import com.phoenix.luminacn.shiyi.WaterMarkOverlay
import com.phoenix.luminacn.game.module.api.setting.stringValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WaterMarkShiyiElement(
    iconResId: Int = AssetManager.getAsset("ic_waterpolo")
) : Element(
    name = "WaterMark2",
    category = CheatCategory.Visual,
    displayNameResId = AssetManager.getString("module_watermark_display_name"),
    iconResId = iconResId
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val customText by stringeValue("显示文字", "LuminaCN", listOf())
    private val showVersion by boolValue("版本", true)
    private val showTime by boolValue("时间", false)
    private val position by stringValue("位置", "TOP_LEFT", Position.values().map { it.name })
    private val fontSize by intValue("字体大小", 18, 10..32)
    private val colorMode by stringValue("颜色风格", "RAINBOW", ColorMode.values().map { it.name })
    private val rainbowSpeed by floatValue("彩虹速度", 1.0f, 0.1f..5.0f)
    private val showBackground by boolValue("背景", true)
    private val backgroundOpacity by floatValue("背景模糊", 0.7f, 0.0f..1.0f)
    private val showShadow by boolValue("阴影", true)
    private val shadowOffset by intValue("阴影程度", 2, 0..10)
    private val animateText by boolValue("跳动字体", false)
    private val glowEffect by boolValue("发光效果", false)
    private val borderStyle by stringValue("包边", "NONE", BorderStyle.values().map { it.name })

    private var lastUpdateTime = 0L
    private val updateInterval = 1000L

    override fun onEnabled() {
        super.onEnabled()
        try {
            if (isSessionCreated) {
                WaterMarkOverlay.setOverlayEnabled(true)
                updateSettings()
                startUpdateLoop()
            }
        } catch (e: Exception) {
            println("Error enabling WaterMark: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            WaterMarkOverlay.setOverlayEnabled(false)
        }
    }

    override fun onDisconnect(reason: String) {
        if (isSessionCreated) {
            WaterMarkOverlay.setOverlayEnabled(false)
        }
    }

    private fun updateSettings() {
        WaterMarkOverlay.setCustomText(customText)
        WaterMarkOverlay.setShowVersion(showVersion)
        WaterMarkOverlay.setShowTime(showTime)
        WaterMarkOverlay.setPosition(Position.valueOf(position))
        WaterMarkOverlay.setFontSize(fontSize)
        WaterMarkOverlay.setColorMode(ColorMode.valueOf(colorMode))
        WaterMarkOverlay.setRainbowSpeed(rainbowSpeed)
        WaterMarkOverlay.setShowBackground(showBackground)
        WaterMarkOverlay.setBackgroundOpacity(backgroundOpacity)
        WaterMarkOverlay.setShowShadow(showShadow)
        WaterMarkOverlay.setShadowOffset(shadowOffset)
        WaterMarkOverlay.setAnimateText(animateText)
        WaterMarkOverlay.setGlowEffect(glowEffect)
        WaterMarkOverlay.setBorderStyle(BorderStyle.valueOf(borderStyle))
    }

    private fun startUpdateLoop() {
        scope.launch {
            while (isEnabled && isSessionCreated) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= updateInterval) {
                    updateSettings()
                    lastUpdateTime = currentTime
                }
                delay(100L)
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= updateInterval) {
            updateSettings()
            lastUpdateTime = currentTime
        }
    }

    enum class Position {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    enum class ColorMode {
        RAINBOW, GRADIENT, STATIC, PULSING, WAVE
    }

    enum class BorderStyle {
        NONE, SOLID, DASHED, DOTTED, GLOW
    }
}