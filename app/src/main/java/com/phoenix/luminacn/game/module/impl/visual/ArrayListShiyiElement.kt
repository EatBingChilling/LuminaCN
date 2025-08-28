package com.phoenix.luminacn.game.module.impl.visual

import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.util.AssetManager
import com.phoenix.luminacn.shiyi.ArrayListOverlay
import com.phoenix.luminacn.game.module.api.setting.stringValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Paint
class ArrayListShiyiElement(iconResId: Int = AssetManager.getAsset("ic_alien")) : Element(
    name = "ArrayList2",
    category = CheatCategory.Visual,
    displayNameResId = AssetManager.getString("arraylist"),
    iconResId = iconResId
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val sortMode by stringValue("风格", "LENGTH", SortMode.values().map { it.name })
    private val animationSpeed by intValue("动画速度", 300, 100..1000)
    private val showBackground by boolValue("背景", true)
    private val showBorder by boolValue("边框", true)
    private val borderStyle by stringValue("边框风格", "LEFT", BorderStyle.values().map { it.name })
    private val colorMode by stringValue("颜色模式", "RAINBOW", ColorMode.values().map { it.name })
    private val rainbowSpeed by floatValue("彩虹速度", 1.0f, 0.1f..5.0f)
    private val fontSize by intValue("大小", 14, 8..24)
    private val spacing by intValue("间距", 2, 0..10)
    private val fadeAnimation by boolValue("开关动画", true)
    private val slideAnimation by boolValue("滑动动画", true)
    private val showModuleStatus by boolValue("显示状态", true)

    private var lastUpdateTime = 0L
    private val updateInterval = 50L

    override fun onEnabled() {
        super.onEnabled()
        try {
            if (isSessionCreated) {
                ArrayListOverlay.setOverlayEnabled(true)
                updateSettings()
                startUpdateLoop()
            }
        } catch (e: Exception) {
            println("Error enabling ArrayList: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            ArrayListOverlay.setOverlayEnabled(false)
        }
    }

    override fun onDisconnect(reason: String) {
        if (isSessionCreated) {
            ArrayListOverlay.setOverlayEnabled(false)
        }
    }

    private fun updateSettings() {
        ArrayListOverlay.setSortMode(SortMode.valueOf(sortMode))
        ArrayListOverlay.setAnimationSpeed(animationSpeed)
        ArrayListOverlay.setShowBackground(showBackground)
        ArrayListOverlay.setShowBorder(showBorder)
        ArrayListOverlay.setBorderStyle(BorderStyle.valueOf(borderStyle))
        ArrayListOverlay.setColorMode(ColorMode.valueOf(colorMode))
        ArrayListOverlay.setRainbowSpeed(rainbowSpeed)
        ArrayListOverlay.setFontSize(fontSize)
        ArrayListOverlay.setSpacing(spacing)
        ArrayListOverlay.setFadeAnimation(fadeAnimation)
        ArrayListOverlay.setSlideAnimation(slideAnimation)
    }

    private fun startUpdateLoop() {
        scope.launch {
            while (isEnabled && isSessionCreated) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= updateInterval) {
                    updateModuleList()
                    lastUpdateTime = currentTime
                }
                delay(updateInterval)
            }
        }
    }

    private fun updateModuleList() {
        val enabledModules = GameManager.elements
            .filter { module ->
                module.isEnabled &&
                        module.name.isNotEmpty() &&
                        module != this
            }
            .map { module ->
                val statusInfo = module.getStatusInfo()
                val displayName = if (showModuleStatus && statusInfo.isNotEmpty()) {
                    "${module.name} [$statusInfo]"
                } else {
                    module.name
                }

                ArrayListOverlay.ModuleInfo(
                    name = displayName,
                    category = module.category.name,
                    isEnabled = module.isEnabled,
                    priority = calculatePriority(module)
                )
            }

        ArrayListOverlay.setModules(enabledModules)
        updateSettings()
    }

    private fun calculatePriority(module: Element): Int {
        return when (SortMode.valueOf(sortMode)) {
            SortMode.LENGTH -> module.name.length
            SortMode.ALPHABETICAL -> -module.name.first().code
            SortMode.CATEGORY -> module.category.ordinal
            SortMode.CUSTOM -> when (module.category) {
                CheatCategory.Combat -> 1000
                CheatCategory.Visual -> 800
                CheatCategory.Misc -> 700
                else -> 500
            }
            SortMode.WIDTH -> {
                val paint = Paint().apply {
                    textSize = fontSize.toFloat()
                }
                paint.measureText(module.name).toInt()
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= updateInterval) {
            updateModuleList()
            lastUpdateTime = currentTime
        }
    }

    override fun getStatusInfo(): String {
        return when (sortMode) {
            "LENGTH" -> "长度排序"
            "ALPHABETICAL" -> "字母排序"
            "CATEGORY" -> "类别排序"
            "CUSTOM" -> "自定义排序"
            "WIDTH" -> "宽度排序"
            else -> ""
        }
    }

    enum class SortMode {
        LENGTH, ALPHABETICAL, CATEGORY, CUSTOM, WIDTH
    }

    enum class BorderStyle {
        LEFT, RIGHT, TOP, BOTTOM, FULL, NONE
    }

    enum class ColorMode {
        RAINBOW, GRADIENT, STATIC, CATEGORY_BASED, RANDOM
    }
}