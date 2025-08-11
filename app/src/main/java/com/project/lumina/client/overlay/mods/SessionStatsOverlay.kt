/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 60
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
                onAnimatedOut = {
                    OverlayManager.dismissOverlayWindow(this)
                    currentOverlay = null
                }
            )
        }
    }

    fun updateStats(statLines: List<String>) {
        _statLines.value = statLines
    }

    fun addStat(statLine: String) {
        _statLines.value = _statLines.value + statLine
    }

    fun clearStats() {
        _statLines.value = emptyList()
    }

    // --- API RESTORED to public ---
    fun dismiss() {
        _isVisible.value = false
    }

    companion object {
        private var currentOverlay: SessionStatsOverlay? = null

        fun showSessionStats(initialStats: List<String> = emptyList()): SessionStatsOverlay {
            currentOverlay?.dismiss()
            val overlay = SessionStatsOverlay().apply {
                updateStats(initialStats)
            }
            OverlayManager.showOverlayWindow(overlay)
            currentOverlay = overlay
            return overlay
        }
    }
}

@Composable
private fun SessionStatsCard(
    statLines: List<String>,
    isVisible: Boolean,
    onAnimatedOut: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "Scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "Alpha"
    )

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(300)
            onAnimatedOut()
        }
    }

    Surface(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .wrapContentWidth()
            .height(28.dp), // 缩小高度
        shape = RoundedCornerShape(24.dp), // 缩小圆角
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp) // 缩小内边距
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 缩小间距
        ) {
            // Fixed brand text
            Text(
                text = "LuminaCN B21",
                style = MaterialTheme.typography.labelSmall, // 缩小字体
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (statLines.isNotEmpty()) {
                // Divider
                VerticalDivider()

                // Stats - 过滤攻击相关统计
                val filteredStats = statLines.filter { statLine ->
                    val label = statLine.split(":", limit = 2).getOrNull(0)?.trim()?.lowercase() ?: ""
                    // 隐藏攻击、击杀、死亡等相关统计
                    !label.contains("攻击") && 
                    !label.contains("击杀") && 
                    !label.contains("死亡") &&
                    !label.contains("kill") &&
                    !label.contains("death") &&
                    !label.contains("attack") &&
                    !label.contains("状态") &&
                    !label.contains("status")
                }
                
                filteredStats.forEachIndexed { index, statLine ->
                    StatItem(statLine)
                    
                    // Add divider between stats (but not after the last one)
                    if (index < filteredStats.lastIndex) {
                        VerticalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(12.dp) // 缩小分隔线高度
            .background(
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                RoundedCornerShape(0.5.dp)
            )
    )
}

@Composable
private fun StatItem(statLine: String) {
    val parts = statLine.split(":", limit = 2)
    val label = parts.getOrNull(0)?.trim() ?: statLine
    val value = parts.getOrNull(1)?.trim()

    if (value != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp) // 缩小间距
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall, // 更小的字体
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall, // 缩小字体
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Text(
            text = statLine,
            style = MaterialTheme.typography.labelSmall, // 缩小字体
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SessionStatsCardPreview() {
    val stats = listOf(
        "时长: 1h 23m",
        "经验: 1,204",
        "等级: 12"
    )
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SessionStatsCard(
                statLines = stats,
                isVisible = true,
                onAnimatedOut = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SessionStatsCardEmptyPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SessionStatsCard(
                statLines = emptyList(),
                isVisible = true,
                onAnimatedOut = {}
            )
        }
    }
}