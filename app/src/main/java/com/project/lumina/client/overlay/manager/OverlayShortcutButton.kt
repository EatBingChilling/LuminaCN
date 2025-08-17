package com.project.lumina.client.overlay.manager

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.constructors.Element

class OverlayShortcutButton(
    private val element: Element
) : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            windowAnimations = android.R.style.Animation_Toast
            x = element.shortcutX
            y = element.shortcutY
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        // 使用固定的高度，宽度由内容决定
        val buttonHeightPx = with(LocalDensity.current) { 56.dp.roundToPx() }

        // --- 新的追光动画 ---
        val chasingLightTransition = rememberInfiniteTransition(label = "chasing_light_transition")
        val offset by chasingLightTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1500f, // 一个足够大的值，确保渐变扫过整个按钮
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "chasing_light_offset"
        )

        // 定义追光效果的颜色和画刷
        val chasingLightColors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White,
            Color.White.copy(alpha = 0.1f)
        )
        val chasingLightBrush = Brush.linearGradient(
            colors = chasingLightColors,
            start = Offset(offset, offset),
            end = Offset(offset + 500f, offset + 500f) // 渐变条的宽度
        )

        val density = LocalDensity.current

        LaunchedEffect(isLandscape) {
            // 注意：这里的宽度限制可能不完美，因为按钮宽度是动态的。
            // 一个更精确的实现需要使用 onSizeChanged 来获取实际宽度。
            // 但对于大多数情况，这个基本限制已经足够。
            val roughButtonWidthPx = with(density) { 150.dp.roundToPx() } // 估算一个宽度
            _layoutParams.x = _layoutParams.x.coerceIn(0, width - roughButtonWidthPx)
            _layoutParams.y = _layoutParams.y.coerceIn(0, height - buttonHeightPx)
            windowManager.updateViewLayout(composeView, _layoutParams)
            updateShortcut()
        }

        // 定义按钮的圆角形状
        val buttonShape = RoundedCornerShape(32.dp)

        Box(
            modifier = Modifier
                .padding(5.dp)
                .height(56.dp) // 将 size 改为固定的 height
                .then(
                    // 当激活时，应用追光动画边框
                    if (element.isEnabled) Modifier.border(2.dp, chasingLightBrush, buttonShape)
                    else Modifier
                )
        ) {
            ElevatedCard(
                onClick = { element.isEnabled = !element.isEnabled },
                shape = buttonShape, // 应用长条形圆角
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            // 拖动逻辑的边界限制也需要注意宽度是动态的
                            val currentButtonWidth = this.size.width
                            _layoutParams.x = (_layoutParams.x + dragAmount.x.toInt())
                                .coerceIn(0, width - currentButtonWidth)
                            _layoutParams.y = (_layoutParams.y + dragAmount.y.toInt())
                                .coerceIn(0, height - buttonHeightPx)
                            windowManager.updateViewLayout(composeView, _layoutParams)
                            updateShortcut()
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = element.name,
                        color = Color.White, // 字体改为白色
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp, // 可以适当调整字体大小
                        textAlign = TextAlign.Center,
                        maxLines = 1, // 不允许换行
                        overflow = TextOverflow.Ellipsis, // 超出部分显示省略号
                        modifier = Modifier.padding(horizontal = 24.dp) // 增加水平内边距
                    )
                }
            }
        }
    }

    private fun updateShortcut() {
        element.shortcutX = _layoutParams.x
        element.shortcutY = _layoutParams.y
    }
}