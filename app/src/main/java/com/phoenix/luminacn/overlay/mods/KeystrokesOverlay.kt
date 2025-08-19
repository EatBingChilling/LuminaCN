package com.phoenix.luminacn.overlay.mods

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow
import kotlin.math.min

class KeystrokesOverlay : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 100
            y = 100
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var keyStates by mutableStateOf(
        mapOf(
            "W" to false,
            "A" to false,
            "S" to false,
            "D" to false,
            "Space" to false
        )
    )

    companion object {
        val overlayInstance by lazy { KeystrokesOverlay() }
        private var shouldShowOverlay = false

        fun showOverlay() {
            if (shouldShowOverlay) {
                try {
                    if (!overlayInstance.composeView.isAttachedToWindow) {
                         OverlayManager.showOverlayWindow(overlayInstance)
                    }
                } catch (e: Exception) {
                    println("Error showing KeystrokesOverlay: ${e.message}")
                }
            }
        }

        fun dismissOverlay() {
            try {
                if (overlayInstance.composeView.isAttachedToWindow) {
                    OverlayManager.dismissOverlayWindow(overlayInstance)
                }
            } catch (e: Exception) {
                println("Error dismissing KeystrokesOverlay: ${e.message}")
            }
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (enabled) showOverlay() else dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        fun setKeyState(key: String, isPressed: Boolean) {
            overlayInstance.keyStates = overlayInstance.keyStates.toMutableMap().apply {
                if (containsKey(key)) {
                    this[key] = isPressed
                }
            }
        }
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Adjust position on configuration change to stay within screen bounds
        LaunchedEffect(isLandscape) {
            val width = context.resources.displayMetrics.widthPixels
            val height = context.resources.displayMetrics.heightPixels
            _layoutParams.x = _layoutParams.x.coerceIn(0, width - composeView.width)
            _layoutParams.y = _layoutParams.y.coerceIn(0, height - composeView.height)
            windowManager.updateViewLayout(composeView, _layoutParams)
        }

        KeystrokesContent(keyStates = keyStates) { dx, dy ->
            _layoutParams.x += dx.toInt()
            _layoutParams.y += dy.toInt()
            windowManager.updateViewLayout(composeView, _layoutParams)
        }
    }

    @Composable
    private fun KeystrokesContent(
        keyStates: Map<String, Boolean>,
        onDrag: (Float, Float) -> Unit
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        onDrag(drag.x, drag.y)
                    }
                }
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keySize = 48.dp // Define a standard key size
                KeyButton(
                    label = "W",
                    isPressed = keyStates["W"] ?: false,
                    modifier = Modifier.size(keySize)
                )

                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    KeyButton("A", keyStates["A"] ?: false, Modifier.size(keySize))
                    KeyButton("S", keyStates["S"] ?: false, Modifier.size(keySize))
                    KeyButton("D", keyStates["D"] ?: false, Modifier.size(keySize))
                }

                KeyButton(
                    label = " ", // Label is a space, but we'll show a visual indicator
                    isPressed = keyStates["Space"] ?: false,
                    modifier = Modifier.width((keySize * 3) + 8.dp).height(keySize)
                )
            }
        }
    }

    @Composable
    private fun KeyButton(
        label: String,
        isPressed: Boolean,
        modifier: Modifier = Modifier
    ) {
        // --- MODIFIED: Using MaterialTheme colors ---
        val colorScheme = MaterialTheme.colorScheme
        val baseAlpha = 0.8f // Opacity for the overlay
        
        // Define colors based on press state
        val targetBackgroundColor = if (isPressed) colorScheme.primary else colorScheme.surfaceContainer
        val targetContentColor = if (isPressed) colorScheme.onPrimary else colorScheme.onSurface
        val targetBorderColor = if (isPressed) colorScheme.primary else colorScheme.outline
        
        // Animate color changes for a smooth transition
        val backgroundColor by animateColorAsState(
            targetValue = targetBackgroundColor.copy(alpha = baseAlpha),
            animationSpec = tween(100),
            label = "KeyBackgroundColor"
        )
        val contentColor by animateColorAsState(
            targetValue = targetContentColor,
            animationSpec = tween(100),
            label = "KeyContentColor"
        )
        val borderColor by animateColorAsState(
            targetValue = targetBorderColor.copy(alpha = if (isPressed) 1f else 0.7f),
            animationSpec = tween(100),
            label = "KeyBorderColor"
        )

        // Animate scale for a tactile feel
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "KeyScale"
        )

        Box(
            modifier = modifier
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // For the space bar, show a visual line instead of text
            if (label.isBlank()) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(contentColor.copy(alpha = 0.8f))
                )
            } else {
                Text(
                    text = label,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    // --- MODIFIED: Using typography for consistent sizing ---
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    @Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
    @Composable
    private fun KeystrokesOverlayPreview() {
        MaterialTheme {
            KeystrokesContent(
                keyStates = mapOf(
                    "W" to true,
                    "A" to false,
                    "S" to true,
                    "D" to false,
                    "Space" to true
                ),
                onDrag = { _, _ -> }
            )
        }
    }
}