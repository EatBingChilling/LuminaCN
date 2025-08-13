package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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

// ... (The rest of the file is the same as the previous correct one)
// I'll paste the full class to be safe.

private data class SpeedLineData(val x: String, val y: Float)

@Composable
private fun MiniLineGraph(
    modifier: Modifier = Modifier,
    data: List<SpeedLineData>,
    lineColor: Color,
    gradientStartColor: Color,
    gradientEndColor: Color
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val minY = 0f
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
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(size.width, size.height)
        fillPath.lineTo(0f, size.height)
        fillPath.close()
        drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(gradientStartColor, gradientEndColor)))
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

class SpeedometerOverlay : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 100
        }
    }
    override val layoutParams: WindowManager.LayoutParams get() = _layoutParams

    @Composable
    override fun Content() {
        MaterialTheme {
            if (shouldShowOverlay) {
                CompactSpeedometerDisplay(currentSpeed = currentSpeed, averageSpeed = averageSpeed, graphData = graphData.toList())
            }
        }
    }

    companion object {
        private var overlayInstance: SpeedometerOverlay? = null
        private var shouldShowOverlay = false
        private var lastPosition: Vector3f? = null
        private var lastUpdateTime: Long = 0L
        private var currentSpeed by mutableStateOf(0.0f)
        private val speedHistory = ArrayDeque<Double>(7)
        private val graphData = ArrayDeque<SpeedLineData>(20)
        private var averageSpeed by mutableStateOf(0.0f)
        private const val MAX_SPEED = 70f

        fun showOverlay() {
            if (shouldShowOverlay && overlayInstance == null) {
                overlayInstance = SpeedometerOverlay()
                OverlayManager.showOverlayWindow(overlayInstance!!)
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
                    speedHistory.addLast(instantSpeed.toDouble())
                    if (speedHistory.size > 7) speedHistory.removeFirst()
                    val smoothedSpeed = speedHistory.average().toFloat()
                    if (smoothedSpeed.isInvalid()) return
                    currentSpeed = smoothedSpeed
                    graphData.addLast(SpeedLineData(x = "", y = currentSpeed))
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
private fun CompactSpeedometerDisplay(currentSpeed: Float, averageSpeed: Float, graphData: List<SpeedLineData>) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.8f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "scale")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300), label = "alpha")
    val animatedSpeed by animateFloatAsState(targetValue = currentSpeed, animationSpec = spring(dampingRatio = 0.6f, stiffness = 150f), label = "speedAnimation")

    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary

    Row(
        modifier = Modifier.scale(scale).alpha(alpha)
            .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.medium).clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceContainer.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = accentColor, style = Stroke(width = 3.dp.toPx()), alpha = 0.3f)
                drawArc(color = accentColor, startAngle = -90f, sweepAngle = (animatedSpeed / 50f).coerceIn(0f, 1f) * 360f, useCenter = false, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(text = "%.1f".format(animatedSpeed), style = MaterialTheme.typography.titleSmall, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.width(100.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val graphDataToShow = if (graphData.size < 2) listOf(SpeedLineData("", 0f), SpeedLineData("", 0f)) else graphData
            MiniLineGraph(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                data = graphDataToShow,
                lineColor = accentColor,
                gradientStartColor = accentColor.copy(alpha = 0.3f),
                gradientEndColor = accentColor.copy(alpha = 0.0f)
            )
            Text(text = "Avg: ${"%.1f".format(averageSpeed)} BPS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
        }
    }
}

// Preview remains the same...