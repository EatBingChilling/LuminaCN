package com.project.lumina.client.overlay.mods

import android.graphics.Paint.Align
import android.graphics.Typeface
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- Visibility FIXED: Changed from private-in-file to public ---
data class Position(val x: Float, val y: Float)

private const val minimapZoom = 1.0f
private const val minimapDotSize = 3.5f

class MiniMapOverlay : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 50
            y = 50
        }
    }
    override val layoutParams: WindowManager.LayoutParams get() = _layoutParams

    private var centerPosition by mutableStateOf(Position(0f, 0f))
    private var playerRotation by mutableStateOf(0f)
    private var targets by mutableStateOf(listOf<Position>())
    private var minimapSize by mutableStateOf(120f)
    private var targetRotation by mutableStateOf(0f)
    private var rotationSmoothStep = 0.15f

    companion object {
        val overlayInstance by lazy { MiniMapOverlay() }
        private var shouldShowOverlay = false

        fun showOverlay() {
            if (shouldShowOverlay && !overlayInstance.composeView.isAttachedToWindow) {
                try { OverlayManager.showOverlayWindow(overlayInstance) } catch (e: Exception) {}
            }
        }
        fun dismissOverlay() {
            if (overlayInstance.composeView.isAttachedToWindow) {
                try { OverlayManager.dismissOverlayWindow(overlayInstance) } catch (e: Exception) {}
            }
        }
        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (enabled) showOverlay() else dismissOverlay()
        }
        fun isOverlayEnabled(): Boolean = shouldShowOverlay
        fun setCenter(x: Float, y: Float) { overlayInstance.centerPosition = Position(x, y) }
        fun setPlayerRotation(rotation: Float) { overlayInstance.targetRotation = rotation }
        fun setTargets(targetList: List<Position>) { overlayInstance.targets = targetList }
        fun setMinimapSize(size: Float) { overlayInstance.minimapSize = size }
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return
        LaunchedEffect(targetRotation) {
            while (kotlin.math.abs(playerRotation - targetRotation) > 0.001f) {
                var delta = (targetRotation - playerRotation) % (2 * Math.PI).toFloat()
                if (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
                if (delta < -Math.PI) delta += (2 * Math.PI).toFloat()
                playerRotation += delta * rotationSmoothStep
                delay(16L)
            }
        }
        MaterialTheme {
            Minimap(centerPosition, playerRotation, targets, minimapSize)
        }
    }

    @Composable
    private fun Minimap(center: Position, rotation: Float, targets: List<Position>, size: Float) {
        val dpSize = size.dp
        val rawRadius = size / 2
        val radius = rawRadius * minimapZoom
        val scale = 2f * minimapZoom

        val colorScheme = MaterialTheme.colorScheme
        val bgColor = colorScheme.surfaceContainer.copy(alpha = 0.85f)
        val gridColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
        val crosshairColor = colorScheme.outline
        val playerMarkerColor = colorScheme.primary
        val entityCloseColor = colorScheme.tertiary
        val entityFarColor = entityCloseColor.copy(alpha = 0.6f)
        val northMarkerColor = colorScheme.error.toArgb()

        val density = LocalDensity.current
        val northTextSize = with(density) { (size * 0.12f).toDp().toPx() }
        val northPaint = remember {
            android.graphics.Paint().apply {
                color = northMarkerColor
                textSize = northTextSize
                textAlign = Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
        }

        Box(
            modifier = Modifier.size(dpSize).clip(RoundedCornerShape(24.dp)).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(dpSize)) {
                val centerX = this.size.width / 2
                val centerY = this.size.height / 2
                val gridSpacing = this.size.width / 10
                for (i in 1 until 10) {
                    val pos = i * gridSpacing
                    drawLine(gridColor, Offset(pos, 0f), Offset(pos, this.size.height), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, pos), Offset(this.size.width, pos), strokeWidth = 1f)
                }
                drawLine(crosshairColor, Offset(centerX, 0f), Offset(centerX, this.size.height), strokeWidth = 1.5f)
                drawLine(crosshairColor, Offset(0f, centerY), Offset(this.size.width, centerY), strokeWidth = 1.5f)
                drawCircle(playerMarkerColor, radius = minimapDotSize * minimapZoom, center = Offset(centerX, centerY))

                val northAngle = -rotation
                val northDistance = rawRadius * 0.9f
                val northX = centerX + northDistance * sin(northAngle)
                val northY = centerY - northDistance * cos(northAngle)
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText("N", northX, northY + northTextSize * 0.4f, northPaint) }

                targets.forEach { target ->
                    val relX = target.x - center.x
                    val relY = target.y - center.y
                    val distance = sqrt(relX * relX + relY * relY) * scale
                    val dotRadius = minimapDotSize * minimapZoom
                    val angle = atan2(relY, relX) - rotation
                    val clampedDistance = kotlin.math.min(distance, radius * 0.9f)
                    val entityX = centerX + clampedDistance * sin(angle)
                    val entityY = centerY - clampedDistance * cos(angle)
                    drawCircle(color = if (distance < radius * 0.9f) entityCloseColor else entityFarColor, radius = dotRadius, center = Offset(entityX, entityY))
                }
            }
        }
    }
}