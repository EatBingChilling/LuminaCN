package com.project.lumina.client.overlay.mods

import android.graphics.BlurMaskFilter
import android.graphics.Color as AndroidColor
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlin.math.max

class OverlayModuleList : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 3
            y = 2
        }
    }

    override val layoutParams: WindowManager.LayoutParams get() = _layoutParams

    companion object {
        private val moduleState = ModuleState()
        private val overlayInstance by lazy { OverlayModuleList() }
        private var shouldShowOverlay = false

        /* 主业务方法 */
        fun showText(name: String) {
            if (shouldShowOverlay) {
                moduleState.addModule(name)
                kotlin.runCatching { OverlayManager.showOverlayWindow(overlayInstance) }
            }
        }

        fun removeText(name: String) {
            moduleState.markForRemoval(name)
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) kotlin.runCatching { OverlayManager.dismissOverlayWindow(overlayInstance) }
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        /* 向下兼容空壳，防止外部旧代码报错 */
        @Suppress("unused")
        fun setCapitalizeAndMerge(enabled: Boolean) = Unit

        @Suppress("unused")
        fun setDisplayMode(mode: String) = Unit
    }

    /* --------------------------------------------------------------------- */

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val infinite = rememberInfiniteTransition()
        val phase by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(5000, easing = LinearEasing))
        )

        val density = LocalDensity.current
        val style = TextStyle(fontSize = 13.sp)
        val tm = rememberTextMeasurer()

        /* 先测宽度再排序，确保"最长在上" */
        val sorted = remember(moduleState.modules) {
            moduleState.modules
                .map { it to tm.measure(it.name, style).size.width }
                .sortedByDescending { it.second }
                .map { it.first }
        }

        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 4.dp, end = 8.dp), // 👈 距离屏幕最右侧 4dp
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            sorted.forEachIndexed { idx, item ->
                key(item.id) {
                    GlowOnlyRow(
                        text = item.name,
                        phase = phase,
                        index = idx,
                        total = sorted.size,
                        isRemoving = moduleState.modulesToRemove.contains(item.name),
                        onRemove = { moduleState.removeModule(item.name) }
                    )
                }
            }
        }
    }

    /* -------------------- 仅发光阴影 -------------------- */

    @Composable
    private fun GlowOnlyRow(
        text: String,
        phase: Float,
        index: Int,
        total: Int,
        isRemoving: Boolean,
        onRemove: () -> Unit
    ) {
        val density = LocalDensity.current
        val glowPx = with(density) { 8.dp.toPx() }
        val paddingH = with(density) { 1.dp.toPx() }
        val paddingV = with(density) { 1.dp.toPx() }

        val tm = rememberTextMeasurer()
        val style = TextStyle(fontSize = 13.sp)
        val layout = remember(text) { tm.measure(text, style) }

        var enterDone by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(index * 50L); enterDone = true }

        val alpha by animateFloatAsState(
            targetValue = if (isRemoving || !enterDone) 0f else 1f,
            animationSpec = tween(300)
        )
        val offsetX by animateFloatAsState(
            targetValue = if (isRemoving) 200f else 0f,
            animationSpec = tween(300)
        )
        LaunchedEffect(alpha) { if (alpha == 0f && isRemoving) onRemove() }

        val yRatio = index.toFloat() / max(total - 1, 1)
        val colorPos = (phase + yRatio * 0.7f) % 1f
        val color = smoothColor(colorPos)

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp)
                .alpha(alpha)
                .padding(horizontal = 1.dp, vertical = 1.dp)
        ) {
            Canvas(
                Modifier.size(
                    width = with(density) { (layout.size.width + paddingH * 2).toDp() },
                    height = with(density) { (layout.size.height + paddingV * 2).toDp() }
                )
            ) {
                val x = paddingH
                val y = paddingV + layout.firstBaseline
                repeat(3) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            this.color = color.toArgb()
                            textSize = style.fontSize.toPx()
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            isAntiAlias = true
                            maskFilter = BlurMaskFilter(glowPx, BlurMaskFilter.Blur.SOLID)
                        }
                        canvas.nativeCanvas.drawText(text, x, y, paint)
                    }
                }
            }
        }
    }

    private fun smoothColor(position: Float): Color {
        val hsvSteps = listOf(
            floatArrayOf(0f, 1f, 1f),
            floatArrayOf(15f, 1f, 1f),
            floatArrayOf(30f, 1f, 1f),
            floatArrayOf(45f, 1f, 1f),
            floatArrayOf(60f, 1f, 1f),
            floatArrayOf(75f, 1f, 1f),
            floatArrayOf(90f, 1f, 1f),
            floatArrayOf(105f, 1f, 1f),
            floatArrayOf(120f, 1f, 1f),
            floatArrayOf(135f, 1f, 1f),
            floatArrayOf(150f, 1f, 1f),
            floatArrayOf(165f, 1f, 1f),
            floatArrayOf(180f, 1f, 1f),
            floatArrayOf(195f, 1f, 1f),
            floatArrayOf(210f, 1f, 1f),
            floatArrayOf(225f, 1f, 1f),
            floatArrayOf(240f, 1f, 1f),
            floatArrayOf(255f, 1f, 1f),
            floatArrayOf(270f, 1f, 1f),
            floatArrayOf(285f, 1f, 1f),
            floatArrayOf(300f, 1f, 1f),
            floatArrayOf(315f, 1f, 1f),
            floatArrayOf(330f, 1f, 1f),
            floatArrayOf(345f, 1f, 1f)
        )
        val len = hsvSteps.size
        val pos = (position % 1f).coerceIn(0f, 1f)
        val scaled = pos * (len - 1)
        val idx = scaled.toInt()
        val fract = scaled - idx

        val hsv1 = hsvSteps[idx % len]
        val hsv2 = hsvSteps[(idx + 1) % len]

        val hue1 = hsv1[0]
        val hue2 = hsv2[0]
        val hue = when {
            hue2 - hue1 > 180 -> hue1 + fract * (hue2 - 360 - hue1)
            hue2 - hue1 < -180 -> hue1 + fract * (hue2 + 360 - hue1)
            else -> hue1 + fract * (hue2 - hue1)
        }.let { (it + 360) % 360 }

        val sat = hsv1[1] + fract * (hsv2[1] - hsv1[1])
        val value = hsv1[2] + fract * (hsv2[2] - hsv1[2])

        return Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, value)))
    }
}

/* -------------------- 状态 -------------------- */

class ModuleState {
    private val _modules = mutableStateListOf<ModuleItem>()
    val modules: List<ModuleItem> get() = _modules.toList()

    private var nextId = 0
    private val _modulesToRemove = mutableStateListOf<String>()
    val modulesToRemove: List<String> get() = _modulesToRemove.toList()

    fun addModule(name: String) {
        if (_modules.none { it.name == name }) {
            _modules.add(ModuleItem(nextId++, name))
            _modulesToRemove.remove(name)
        }
    }

    fun markForRemoval(name: String) {
        if (!_modulesToRemove.contains(name)) _modulesToRemove.add(name)
    }

    fun removeModule(name: String) {
        _modules.removeAll { it.name == name }
        _modulesToRemove.remove(name)
    }
}

data class ModuleItem(val id: Int, val name: String)
