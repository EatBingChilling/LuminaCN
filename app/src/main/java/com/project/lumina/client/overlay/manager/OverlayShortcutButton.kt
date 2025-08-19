package com.project.lumina.client.overlay.manager

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.project.lumina.client.constructors.Element

class OverlayShortcutButton(
    private val element: Element
) : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
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

        val borderWidth by animateDpAsState(
            targetValue = if (element.isEnabled) 2.dp else 0.5.dp,
            animationSpec = tween(durationMillis = 300),
            label = "border_width_animation"
        )

        val buttonShape = RoundedCornerShape(32.dp)
        var buttonSize by remember { mutableStateOf(IntSize.Zero) }

        LaunchedEffect(isLandscape, buttonSize) {
            if (buttonSize != IntSize.Zero) {
                _layoutParams.x = _layoutParams.x.coerceIn(0, width - buttonSize.width)
                _layoutParams.y = _layoutParams.y.coerceIn(0, height - buttonSize.height)
                windowManager.updateViewLayout(composeView, _layoutParams)
                updateShortcut()
            }
        }

        // 为了防止按钮紧贴屏幕边缘，我们在最外层保留一个padding。
        // 这个padding现在是按钮与悬浮窗边界的间距，而不是边框与背景的间距。
        Box(
            modifier = Modifier.padding(5.dp)
        ) {
            ElevatedCard(
                onClick = { element.isEnabled = !element.isEnabled },
                shape = buttonShape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    // 将 border 修饰符直接应用到 Card 上
                    .border(
                        width = borderWidth,
                        color = Color.White,
                        shape = buttonShape
                    )
                    .onSizeChanged { newSize ->
                        buttonSize = newSize
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            _layoutParams.x = (_layoutParams.x + dragAmount.x.toInt())
                                .coerceIn(0, width - buttonSize.width)
                            _layoutParams.y = (_layoutParams.y + dragAmount.y.toInt())
                                .coerceIn(0, height - buttonSize.height)
                            windowManager.updateViewLayout(composeView, _layoutParams)
                            updateShortcut()
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    val displayName = element.displayNameResId?.let { resId ->
                        stringResource(id = resId)
                    } ?: element.name

                    Text(
                        text = displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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