package com.phoenix.luminacn.overlay.manager

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.phoenix.luminacn.constructors.Element

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

        var isDragging by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isDragging) 1.1f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale_animation"
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

        // To prevent the button from sticking to the screen edge, we use an outer padding.
        Box(
            modifier = Modifier.padding(5.dp)
        ) {
            Button(
                onClick = { element.isEnabled = !element.isEnabled },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = (if (element.isEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.85f),
                    contentColor = if (element.isEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .onSizeChanged { newSize ->
                        buttonSize = newSize
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                updateShortcut()
                            },
                            onDragCancel = { isDragging = false }
                        ) { _, dragAmount ->
                            _layoutParams.x = (_layoutParams.x + dragAmount.x.toInt())
                                .coerceIn(0, width - buttonSize.width)
                            _layoutParams.y = (_layoutParams.y + dragAmount.y.toInt())
                                .coerceIn(0, height - buttonSize.height)
                            windowManager.updateViewLayout(composeView, _layoutParams)
                        }
                    }
            ) {
                val displayName = element.displayNameResId?.let { resId ->
                    stringResource(id = resId)
                } ?: element.name

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    private fun updateShortcut() {
        element.shortcutX = _layoutParams.x
        element.shortcutY = _layoutParams.y
    }
}