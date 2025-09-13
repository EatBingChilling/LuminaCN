// 文件路径: com/phoenix/luminacn/ui/theme/ThemeManager.kt

package com.phoenix.luminacn.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.phoenix.luminacn.R
import org.json.JSONObject
import java.io.InputStream

class ThemeManager(context: Context) {
    private val json: JSONObject

    init {
        json = try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.theme)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            JSONObject(jsonString)
        } catch (e: Exception) {
            // 如果 theme.json 文件不存在或解析失败，则创建一个空的JSONObject。
            // 这样系统就会完全依赖我们在 Color.kt 中定义的后备颜色。
            JSONObject()
        }
    }

    private fun hexToColor(hex: String?): Color {
        if (hex.isNullOrEmpty()) return Color.Unspecified
        val cleanHex = hex.removePrefix("#").trim()
        return try {
            when (cleanHex.length) {
                6 -> Color(android.graphics.Color.parseColor("#FF$cleanHex"))
                8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                else -> Color.Unspecified
            }
        } catch (e: IllegalArgumentException) {
            Color.Unspecified
        }
    }

    // **修改**: 这个函数现在接受一个 defaultValue。
    private fun getColor(vararg keys: String, defaultValue: Color): Color {
        var current: JSONObject? = json
        for (i in 0 until keys.size - 1) {
            current = current?.optJSONObject(keys[i])
            if (current == null) return defaultValue // 如果路径不存在，返回默认值
        }
        val hex = current?.optString(keys.last())
        val color = hexToColor(hex)
        // 如果颜色是无效的或未指定的，返回默认值
        return if (color == Color.Unspecified) defaultValue else color
    }

    // --- Material Design 颜色 ---
    private fun getMaterialColor(scheme: String, key: String, defaultValue: Color): Color {
        return getColor("material", scheme, key, defaultValue = defaultValue)
    }

    fun getMaterialColorScheme(scheme: String): ColorScheme {
        return when (scheme) {
            "light" -> lightColorScheme(
                primary = getMaterialColor("light", "primary", primaryLight),
                onPrimary = getMaterialColor("light", "onPrimary", onPrimaryLight),
                primaryContainer = getMaterialColor("light", "primaryContainer", primaryContainerLight),
                onPrimaryContainer = getMaterialColor("light", "onPrimaryContainer", onPrimaryContainerLight),
                secondary = getMaterialColor("light", "secondary", secondaryLight),
                onSecondary = getMaterialColor("light", "onSecondary", onSecondaryLight),
                secondaryContainer = getMaterialColor("light", "secondaryContainer", secondaryContainerLight),
                onSecondaryContainer = getMaterialColor("light", "onSecondaryContainer", onSecondaryContainerLight),
                tertiary = getMaterialColor("light", "tertiary", tertiaryLight),
                onTertiary = getMaterialColor("light", "onTertiary", onTertiaryLight),
                tertiaryContainer = getMaterialColor("light", "tertiaryContainer", tertiaryContainerLight),
                onTertiaryContainer = getMaterialColor("light", "onTertiaryContainer", onTertiaryContainerLight),
                error = getMaterialColor("light", "error", errorLight),
                onError = getMaterialColor("light", "onError", onErrorLight),
                errorContainer = getMaterialColor("light", "errorContainer", errorContainerLight),
                onErrorContainer = getMaterialColor("light", "onErrorContainer", onErrorContainerLight),
                background = getMaterialColor("light", "background", backgroundLight),
                onBackground = getMaterialColor("light", "onBackground", onBackgroundLight),
                surface = getMaterialColor("light", "surface", surfaceLight),
                onSurface = getMaterialColor("light", "onSurface", onSurfaceLight),
                surfaceVariant = getMaterialColor("light", "surfaceVariant", surfaceVariantLight),
                onSurfaceVariant = getMaterialColor("light", "onSurfaceVariant", onSurfaceVariantLight),
                outline = getMaterialColor("light", "outline", outlineLight),
                outlineVariant = getMaterialColor("light", "outlineVariant", outlineVariantLight),
                scrim = getMaterialColor("light", "scrim", scrimLight),
                inverseSurface = getMaterialColor("light", "inverseSurface", inverseSurfaceLight),
                inverseOnSurface = getMaterialColor("light", "inverseOnSurface", inverseOnSurfaceLight),
                inversePrimary = getMaterialColor("light", "inversePrimary", inversePrimaryLight),
                surfaceDim = getMaterialColor("light", "surfaceDim", surfaceDimLight),
                surfaceBright = getMaterialColor("light", "surfaceBright", surfaceBrightLight),
                surfaceContainerLowest = getMaterialColor("light", "surfaceContainerLowest", surfaceContainerLowestLight),
                surfaceContainerLow = getMaterialColor("light", "surfaceContainerLow", surfaceContainerLowLight),
                surfaceContainer = getMaterialColor("light", "surfaceContainer", surfaceContainerLight),
                surfaceContainerHigh = getMaterialColor("light", "surfaceContainerHigh", surfaceContainerHighLight),
                surfaceContainerHighest = getMaterialColor("light", "surfaceContainerHighest", surfaceContainerHighestLight)
            )
            "dark" -> darkColorScheme(
                primary = getMaterialColor("dark", "primary", primaryDark),
                onPrimary = getMaterialColor("dark", "onPrimary", onPrimaryDark),
                primaryContainer = getMaterialColor("dark", "primaryContainer", primaryContainerDark),
                onPrimaryContainer = getMaterialColor("dark", "onPrimaryContainer", onPrimaryContainerDark),
                secondary = getMaterialColor("dark", "secondary", secondaryDark),
                onSecondary = getMaterialColor("dark", "onSecondary", onSecondaryDark),
                secondaryContainer = getMaterialColor("dark", "secondaryContainer", secondaryContainerDark),
                onSecondaryContainer = getMaterialColor("dark", "onSecondaryContainer", onSecondaryContainerDark),
                tertiary = getMaterialColor("dark", "tertiary", tertiaryDark),
                onTertiary = getMaterialColor("dark", "onTertiary", onTertiaryDark),
                tertiaryContainer = getMaterialColor("dark", "tertiaryContainer", tertiaryContainerDark),
                onTertiaryContainer = getMaterialColor("dark", "onTertiaryContainer", onTertiaryContainerDark),
                error = getMaterialColor("dark", "error", errorDark),
                onError = getMaterialColor("dark", "onError", onErrorDark),
                errorContainer = getMaterialColor("dark", "errorContainer", errorContainerDark),
                onErrorContainer = getMaterialColor("dark", "onErrorContainer", onErrorContainerDark),
                background = getMaterialColor("dark", "background", backgroundDark),
                onBackground = getMaterialColor("dark", "onBackground", onBackgroundDark), // **关键修复**
                surface = getMaterialColor("dark", "surface", surfaceDark),
                onSurface = getMaterialColor("dark", "onSurface", onSurfaceDark),          // **关键修复**
                surfaceVariant = getMaterialColor("dark", "surfaceVariant", surfaceVariantDark),
                onSurfaceVariant = getMaterialColor("dark", "onSurfaceVariant", onSurfaceVariantDark),
                outline = getMaterialColor("dark", "outline", outlineDark),
                outlineVariant = getMaterialColor("dark", "outlineVariant", outlineVariantDark),
                scrim = getMaterialColor("dark", "scrim", scrimDark),
                inverseSurface = getMaterialColor("dark", "inverseSurface", inverseSurfaceDark),
                inverseOnSurface = getMaterialColor("dark", "inverseOnSurface", inverseOnSurfaceDark),
                inversePrimary = getMaterialColor("dark", "inversePrimary", inversePrimaryDark),
                surfaceDim = getMaterialColor("dark", "surfaceDim", surfaceDimDark),
                surfaceBright = getMaterialColor("dark", "surfaceBright", surfaceBrightDark),
                surfaceContainerLowest = getMaterialColor("dark", "surfaceContainerLowest", surfaceContainerLowestDark),
                surfaceContainerLow = getMaterialColor("dark", "surfaceContainerLow", surfaceContainerLowDark),
                surfaceContainer = getMaterialColor("dark", "surfaceContainer", surfaceContainerDark),
                surfaceContainerHigh = getMaterialColor("dark", "surfaceContainerHigh", surfaceContainerHighDark),
                surfaceContainerHighest = getMaterialColor("dark", "surfaceContainerHighest", surfaceContainerHighestDark)
            )
            else -> getMaterialColorScheme("dark") // 默认回退到暗色主题
        }
    }

    // --- 自定义颜色属性 ---
    // 所有自定义颜色现在也都使用后备值，使其更加健壮
    val primary: Color get() = getColor("main", "primary", defaultValue = primaryDark)
    val background: Color get() = getColor("main", "background", defaultValue = backgroundDark)
    val surface: Color get() = getColor("main", "surface", defaultValue = surfaceDark)
    val onBackground: Color get() = getColor("main", "onBackground", defaultValue = onBackgroundDark)
    val onSurface: Color get() = getColor("main", "onSurface", defaultValue = onSurfaceDark)
    val backgroundOverlayUi: Color get() = getColor("main", "backgroundOverlayUi", defaultValue = Color(0x80000000))
    val backgroundOverlayUi2: Color get() = getColor("main", "backgroundOverlayUi2", defaultValue = Color(0x99000000))
    val notBackgroundOverlayUi: Color get() = getColor("main", "notBackgroundOverlayUi", defaultValue = Color.Transparent)
    val textModules: Color get() = getColor("main", "textModules", defaultValue = Color.White)

    val launcherRadial: Color get() = getColor("launchActivity", "launcherRadial", defaultValue = primaryDark)
    val lAnimation: Color get() = getColor("launchActivity", "lAnimation", defaultValue = Color.White)
    val lText: Color get() = getColor("launchActivity", "lText", defaultValue = Color.White)
    val lBlob1: Color get() = getColor("launchActivity", "lBlob1", defaultValue = secondaryDark)
    val lBlob2: Color get() = getColor("launchActivity", "lBlob2", defaultValue = tertiaryDark)
    val lBg1: Color get() = getColor("launchActivity", "lBg1", defaultValue = backgroundDark)
    val lBg2: Color get() = getColor("launchActivity", "lBg2", defaultValue = surfaceDark)

    val mBg: Color get() = getColor("miniMap", "mBg", defaultValue = Color(0x80000000))
    val mGrid: Color get() = getColor("miniMap", "mGrid", defaultValue = Color(0x80FFFFFF))
    val mCrosshair: Color get() = getColor("miniMap", "mCrosshair", defaultValue = Color.White)
    val mPlayerMarker: Color get() = getColor("miniMap", "mPlayerMarker", defaultValue = Color.Red)
    val mNorth: Color get() = getColor("miniMap", "mNorth", defaultValue = Color.White)
    val mEntityClose: Color get() = getColor("miniMap", "mEntityClose", defaultValue = Color.Yellow)
    val mEntityFar: Color get() = getColor("miniMap", "mEntityFar", defaultValue = Color.Green)

    val oArrayList1: Color get() = getColor("arrayList", "oArrayList1", defaultValue = primaryDark)
    val oArrayList2: Color get() = getColor("arrayList", "oArrayList2", defaultValue = secondaryDark)
    val oArrayBase: Color get() = getColor("arrayList", "oArrayBase", defaultValue = surfaceDark.copy(alpha = 0.5f))

    val oNotifAccent: Color get() = getColor("overlayNotification", "oNotifAccent", defaultValue = primaryDark)
    val oNotifBase: Color get() = getColor("overlayNotification", "oNotifBase", defaultValue = surfaceDark)
    val oNotifText: Color get() = getColor("overlayNotification", "oNotifText", defaultValue = onSurfaceDark)
    val oNotifProgressbar: Color get() = getColor("overlayNotification", "oNotifProgressbar", defaultValue = primaryDark)

    val pColorGradient1: Color get() = getColor("packetNotification", "pColorGradient1", defaultValue = primaryDark)
    val pColorGradient2: Color get() = getColor("packetNotification", "pColorGradient2", defaultValue = secondaryDark)
    val pBackground: Color get() = getColor("packetNotification", "pBackground", defaultValue = surfaceDark)

    val sBase: Color get() = getColor("sessionStats", "sBase", defaultValue = onSurfaceDark)
    val sAccent: Color get() = getColor("sessionStats", "sAccent", defaultValue = primaryDark)
    val sBackgroundGradient1: Color get() = getColor("sessionStats", "sBackgroundGradient1", defaultValue = surfaceDark)
    val sBackgroundGradient2: Color get() = getColor("sessionStats", "sBackgroundGradient2", defaultValue = surfaceDark.copy(alpha = 0.5f))

    val sMiniLineGraph: Color get() = getColor("speedoMeter", "sMiniLineGraph", defaultValue = primaryDark)
    val sMeterBg: Color get() = getColor("speedoMeter", "sMeterBg", defaultValue = surfaceDark.copy(alpha = 0.5f))
    val sMeterAccent: Color get() = getColor("speedoMeter", "sMeterAccent", defaultValue = primaryDark)
    val sMeterBase: Color get() = getColor("speedoMeter", "sMeterBase", defaultValue = onSurfaceDark)

    val tcoGradient1: Color get() = getColor("topCenterOverlay", "tcoGradient1", defaultValue = primaryDark)
    val tcoGradient2: Color get() = getColor("topCenterOverlay", "tcoGradient2", defaultValue = secondaryDark)
    val tcoBackground: Color get() = getColor("topCenterOverlay", "tcoBackground", defaultValue = surfaceDark.copy(alpha = 0.5f))

    val eColorCard1: Color get() = getColor("graceUi", "elevatedCard", "eColorCard1", defaultValue = surfaceContainerLowDark)
    val eColorCard2: Color get() = getColor("graceUi", "elevatedCard", "eColorCard2", defaultValue = surfaceContainerDark)
    val eColorCard3: Color get() = getColor("graceUi", "elevatedCard", "eColorCard3", defaultValue = surfaceContainerHighDark)
    val mColorCard1: Color get() = getColor("graceUi", "elevatedCard", "mColorCard1", defaultValue = primaryDark)
    val mColorCard2: Color get() = getColor("graceUi", "elevatedCard", "mColorCard2", defaultValue = secondaryDark)
    val mColorCard3: Color get() = getColor("graceUi", "elevatedCard", "mColorCard3", defaultValue = tertiaryDark)
    val mColorScreen1: Color get() = getColor("graceUi", "moduleSettingScreen", "mColorScreen1", defaultValue = backgroundDark)
    val mColorScreen2: Color get() = getColor("graceUi", "moduleSettingScreen", "mColorScreen2", defaultValue = surfaceDimDark)
    val nColorItem1: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem1", defaultValue = primaryDark)
    val nColorItem2: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem2", defaultValue = secondaryDark)
    val nColorItem3: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem3", defaultValue = tertiaryDark)
    val nColorItem4: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem4", defaultValue = primaryContainerDark)
    val nColorItem5: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem5", defaultValue = secondaryContainerDark)
    val nColorItem6: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem6", defaultValue = tertiaryContainerDark)
    val nColorItem7: Color get() = getColor("graceUi", "navigationRailItem", "nColorItem7", defaultValue = errorDark)
    val pColorItem1: Color get() = getColor("graceUi", "packItem", "pColorItem1", defaultValue = primaryDark)

    val enabledBackground: Color get() = getColor("clickGui", "interfaceElement", "enabledBackground", defaultValue = primaryDark)
    val disabledBackground: Color get() = getColor("clickGui", "interfaceElement", "disabledBackground", defaultValue = surfaceVariantDark)
    val enabledGlow: Color get() = getColor("clickGui", "interfaceElement", "enabledGlow", defaultValue = primaryDark.copy(alpha = 0.5f))
    val enabledText: Color get() = getColor("clickGui", "interfaceElement", "enabledText", defaultValue = onPrimaryDark)
    val disabledText: Color get() = getColor("clickGui", "interfaceElement", "disabledText", defaultValue = onSurfaceVariantDark)
    val enabledIcon: Color get() = getColor("clickGui", "interfaceElement", "enabledIcon", defaultValue = onPrimaryDark)
    val disabledIcon: Color get() = getColor("clickGui", "interfaceElement", "disabledIcon", defaultValue = onSurfaceVariantDark)
    val progressIndicator: Color get() = getColor("clickGui", "interfaceElement", "progressIndicator", defaultValue = primaryDark)
    val sliderTrack: Color get() = getColor("clickGui", "interfaceElement", "sliderTrack", defaultValue = surfaceVariantDark)
    val sliderActiveTrack: Color get() = getColor("clickGui", "interfaceElement", "sliderActiveTrack", defaultValue = primaryDark)
    val sliderThumb: Color get() = getColor("clickGui", "interfaceElement", "sliderThumb", defaultValue = onPrimaryDark)
    val checkboxUnchecked: Color get() = getColor("clickGui", "interfaceElement", "checkboxUnchecked", defaultValue = onSurfaceVariantDark)
    val checkboxChecked: Color get() = getColor("clickGui", "interfaceElement", "checkboxChecked", defaultValue = primaryDark)
    val checkboxCheckmark: Color get() = getColor("clickGui", "interfaceElement", "checkboxCheckmark", defaultValue = onPrimaryDark)
    val choiceSelected: Color get() = getColor("clickGui", "interfaceElement", "choiceSelected", defaultValue = primaryDark)
    val choiceUnselected: Color get() = getColor("clickGui", "interfaceElement", "choiceUnselected", defaultValue = surfaceVariantDark)

    val kitsuPrimary: Color get() = getColor("kitsu", "interfaceElement", "kitsuPrimary", defaultValue = primaryDark)
    val kitsuSecondary: Color get() = getColor("kitsu", "interfaceElement", "kitsuSecondary", defaultValue = secondaryDark)
    val kitsuSurface: Color get() = getColor("kitsu", "interfaceElement", "kitsuSurface", defaultValue = surfaceDark)
    val kitsuSurfaceVariant: Color get() = getColor("kitsu", "interfaceElement", "kitsuSurfaceVariant", defaultValue = surfaceVariantDark)
    val kitsuOnSurface: Color get() = getColor("kitsu", "interfaceElement", "kitsuOnSurface", defaultValue = onSurfaceDark)
    val kitsuOnSurfaceVariant: Color get() = getColor("kitsu", "interfaceElement", "kitsuOnSurfaceVariant", defaultValue = onSurfaceVariantDark)
    val kitsuBackground: Color get() = getColor("kitsu", "interfaceElement", "kitsuBackground", defaultValue = backgroundDark)
    val kitsuSelected: Color get() = getColor("kitsu", "interfaceElement", "kitsuSelected", defaultValue = primaryContainerDark)
    val kitsuUnselected: Color get() = getColor("kitsu", "interfaceElement", "kitsuUnselected", defaultValue = Color.Transparent)
    val kitsuHover: Color get() = getColor("kitsu", "interfaceElement", "kitsuHover", defaultValue = primaryDark.copy(alpha = 0.1f))

    val baseColor: Color get() = getColor("keystroke", "interfaceElement", "baseColor", defaultValue = surfaceDark.copy(alpha = 0.7f))
    val borderColor: Color get() = getColor("keystrokes", "interfaceElement", "borderColor", defaultValue = outlineDark)
    val pressedColor: Color get() = getColor("keystrokes", "interfaceElement", "pressedColor", defaultValue = primaryDark)
    val textColor: Color get() = getColor("keystrokes", "interfaceElement", "textColor", defaultValue = onSurfaceDark)
}