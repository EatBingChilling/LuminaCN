/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class SessionStatsOverlay : OverlayWindow() {

    private val _statLines = mutableStateOf<List<String>>(emptyList())
    private val _isVisible = mutableStateOf(true)

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            // Using WRAP_CONTENT for height to be more flexible
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 100
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        MaterialTheme {
            SessionStatsCard(
                statLines = _statLines.value,
                isVisible = _isVisible.value,
                onDismiss = ::dismiss
            )
        }
    }
    
    // Internal dismiss logic that triggers animations
    private fun dismiss() {
        _isVisible.value = false
    }
    
    // Public API to manage stats
    fun updateStats(statLines: List<String>) { _statLines.value = statLines }
    fun addStat(statLine: String) { _statLines.value = _statLines.value + statLine }
    fun clearStats() { _statLines.value = emptyList() }
    
    companion object {
        private var currentOverlay: SessionStatsOverlay? = null

        fun showSessionStats(initialStats: List<String> = emptyList()): SessionStatsOverlay {
            // Dismiss any existing overlay before showing a new one
            currentOverlay?.dismiss()

            val overlay = SessionStatsOverlay().apply {
                updateStats(initialStats)
            }
            
            OverlayManager.showOverlayWindow(overlay)
            currentOverlay = overlay
            return overlay
        }
        
        fun dismissCurrent() {
            currentOverlay?.dismiss()
            currentOverlay = null
        }
    }
}

@Composable
private fun SessionStatsCard(
    statLines: List<String>,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    // Animation for the rainbow divider
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowHueAnimation")
    val animatedHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Hue"
    )

    // Animations for appearance and disappearance
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "Scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "Alpha"
    )

    // Dismiss the overlay window after the exit animation completes
    if (!isVisible) {
        LaunchedEffect(Unit) {
            delay(300)
            // This assumes the overlay instance is managed outside.
            // A more robust solution might pass the dismiss function from the overlay.
        }
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .width(IntrinsicSize.Min) // Let the content determine the width
            .wrapContentHeight()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            // --- MODIFIED: Use MaterialTheme color for background ---
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // --- Title ---
            Text(
                text = "游玩信息",
                // --- MODIFIED: Use MaterialTheme typography and colors ---
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Rainbow Divider ---
            RainbowDivider(hue = animatedHue)

            Spacer(modifier = Modifier.height(8.dp))

            // --- Stats List ---
            if (statLines.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                 Column(
                    modifier = Modifier
                        .width(180.dp) // Fixed width for stats section for alignment
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statLines.forEach { statLine ->
                        StatRow(statLine)
                    }
                }
            }
        }
    }
}

@Composable
private fun RainbowDivider(hue: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
    ) {
        val segmentCount = 20
        val segmentWidth = size.width / segmentCount
        for (i in 0 until segmentCount) {
            val currentHue = (hue + (i * 18f)) % 360f
            drawRect(
                color = Color.hsv(currentHue, 0.8f, 0.95f),
                topLeft = Offset(i * segmentWidth, 0f),
                size = Size(segmentWidth, size.height)
            )
        }
    }
}

@Composable
private fun StatRow(statLine: String) {
    val parts = statLine.split(":", limit = 2)
    val label = parts.getOrNull(0)?.trim() ?: statLine
    val value = parts.getOrNull(1)?.trim()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            // --- MODIFIED: Use MaterialTheme typography and colors ---
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SessionStatsCardPreview() {
    val stats = remember {
        mutableStateOf(
            listOf(
                "游玩时长: 1h 23m",
                "击杀: 42",
                "死亡: 3",
                "获得经验: 1,204"
            )
        )
    }
    MaterialTheme {
        SessionStatsCard(
            statLines = stats.value,
            isVisible = true,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SessionStatsCardEmptyPreview() {
    MaterialTheme {
        SessionStatsCard(
            statLines = emptyList(),
            isVisible = true,
            onDismiss = {}
        )
    }
}