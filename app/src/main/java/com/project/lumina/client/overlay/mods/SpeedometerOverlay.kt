package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.constructors.NetBound
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import org.cloudburstmc.math.vector.Vector3f
import java.util.ArrayDeque
import kotlin.math.sqrt

private data class LineData(val x: String, val y: Float)

@Composable
private fun MiniLineGraph(
    modifier: Modifier = Modifier,
    data: List<LineData>,
    lineColor: Color,
    gradientStartColor: Color,
    gradientEndColor: Color
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val minY = 0f // Start y-axis from 0 for consistency
        val maxY = data.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f
        val stepX = size.width / (data.size - 1)

        val path = Path().apply {
            moveTo(0f, size.height - ((data.first().y - minY) / (maxY - minY)) * size.height)
            data.drop(1).forEachIndexed { index, point ->
                val x = (index + 1) * stepX
                val y = size.height - ((point.y - minY) / (maxY - minY)) * size.height
                lineTo(x, y)
            }
        }

        val fillPath = Path(path.asAndroidPath()).apply {
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStartColor, gradientEndColor)
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}


class SpeedometerOverlay : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 100
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        MaterialTheme {
            if (shouldShowOverlay) {
                CompactSpeedometerDisplay(
                    currentSpeed = currentSpeed,
                    averageSpeed = averageSpeed,
                    graphData = graphData.toList()
                )
            }
        }
    }

    companion object {
        private var overlayInstance: SpeedometerOverlay? = null
        private var shouldShowOverlay = false

        private var lastPosition: Vector3f? = null
        private var lastUpdateTime: Long = 0L
        private var currentSpeed by mutableStateOf(0.0f)
        private val speedHistory = ArrayDeque<Double>(7) // Use a slightly larger window
        private val graphData = ArrayDeque<LineData>(20) // More data points for a smoother graph
        private var averageSpeed by mutableStateOf(0.0f)

        private const val MAX_SPEED = 70f // Higher cap for speed

        fun showOverlay() {
            if (shouldShowOverlay) {
                if (overlayInstance == null) {
                    overlayInstance = SpeedometerOverlay()
                    OverlayManager.showOverlayWindow(overlayInstance!!)
                }
            }
        }

        fun dismissOverlay() {
            overlayInstance?.let {
                OverlayManager.dismissOverlayWindow(it)
                overlayInstance = null
            }
        }

        fun setOverlayEnabled(enabled: Boolean, netBound: NetBound? = null) {
            shouldShowOverlay = enabled
            if (enabled && netBound != null) showOverlay() else dismissOverlay()
        }

        fun updatePosition(position: Vector3f) {
            val currentTime = System.currentTimeMillis()
            lastPosition?.let { lastPos ->
                if (lastUpdateTime > 0) {
                    val deltaTime = (currentTime - lastUpdateTime) / 1000f
                    if (deltaTime < 0.05f) return

                    val distance = position.distance(lastPos.x, position.y, lastPos.z)
                    if (distance.isInvalid()) return

                    val instantSpeed = (distance / deltaTime).coerceAtMost(MAX_SPEED)
                    if (instantSpeed.isInvalid()) return

                    // Smoothing using a moving average
                    speedHistory.addLast(instantSpeed.toDouble())
                    if (speedHistory.size > 7) speedHistory.removeFirst()
                    val smoothedSpeed = speedHistory.average().toFloat()
                    if (smoothedSpeed.isInvalid()) return

                    currentSpeed = smoothedSpeed

                    graphData.addLast(LineData(x = "", y = currentSpeed))
                    if (graphData.size > 20) graphData.removeFirst()

                    averageSpeed = graphData.map { it.y }.average().toFloat()
                }
            }
            lastPosition = position
            lastUpdateTime = currentTime
        }

        private fun Float.isInvalid() = this.isNaN() || this.isInfinite()
    }
}


@Composable
private fun CompactSpeedometerDisplay(
    currentSpeed: Float,
    averageSpeed: Float,
    graphData: List<LineData>
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 150f),
        label = "speedAnimation"
    )

    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary

    Row(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceContainer.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Speed Circle
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = accentColor,
                    style = Stroke(width = 3.dp.toPx()),
                    alpha = 0.3f
                )
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = (animatedSpeed / 50f).coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "%.1f".format(animatedSpeed),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        // Graph and Average Speed
        Column(
            modifier = Modifier.width(100.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val graphDataToShow = if (graphData.size < 2) {
                listOf(LineData("", 0f), LineData("", 0f))
            } else graphData

            MiniLineGraph(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                data = graphDataToShow,
                lineColor = accentColor,
                gradientStartColor = accentColor.copy(alpha = 0.3f),
                gradientEndColor = accentColor.copy(alpha = 0.0f)
            )

            Text(
                text = "平均: ${"%.1f".format(averageSpeed)} BPS",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SpeedometerPreview() {
    MaterialTheme {
        CompactSpeedometerDisplay(
            currentSpeed = 23.7f,
            averageSpeed = 18.2f,
            graphData = List(20) { LineData("", (10..30).random().toFloat()) }
        )
    }
}