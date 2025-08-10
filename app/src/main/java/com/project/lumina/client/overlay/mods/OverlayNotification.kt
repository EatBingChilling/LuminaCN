package com.project.lumina.client.overlay.mods

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class OverlayNotification : OverlayWindow() {

    private val _layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.BOTTOM or Gravity.END
            x = 24 // Adjusted for better screen padding
            y = 24
            format = PixelFormat.TRANSLUCENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private val notificationState = NotificationState()
        private var overlayInstance: OverlayNotification? = null

        /* 旧接口：保持零改动兼容 */
        fun addNotification(moduleName: String) = onModuleEnabled(moduleName)

        /* 新增双参数接口 */
        fun addNotification(moduleName: String, enabled: Boolean) {
            val action = if (enabled) ModuleAction.ENABLE else ModuleAction.DISABLE
            notificationState.addNotification(moduleName, action)
            ensureOverlayVisible()
        }

        fun onModuleEnabled(moduleName: String) = addNotification(moduleName, true)
        fun onModuleDisabled(moduleName: String) = addNotification(moduleName, false)

        private fun ensureOverlayVisible() {
            if (overlayInstance?.composeView?.isAttachedToWindow == true) return
            try {
                val newInstance = OverlayNotification()
                overlayInstance = newInstance
                OverlayManager.showOverlayWindow(newInstance)
            } catch (_: Exception) {
            }
        }

        fun onOverlayDismissed() {
            overlayInstance = null
        }
    }

    @Composable
    override fun Content() {
        val notifications = notificationState.notifications
        // When the list becomes empty, schedule dismissal
        LaunchedEffect(notifications.isEmpty()) {
            if (notifications.isEmpty()) {
                delay(300) // Wait for exit animation to complete
                OverlayManager.dismissOverlayWindow(this@OverlayNotification)
                onOverlayDismissed()
            }
        }

        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                notifications.forEach { item ->
                    key(item.id) { NotificationCard(item, notificationState) }
                }
            }
        }
    }
}

/* -------------------- 卡片 -------------------- */
@Composable
private fun NotificationCard(
    item: NotificationItem,
    state: NotificationState
) {
    var visible by remember { mutableStateOf(false) }
    var exitState by remember { mutableStateOf(false) }

    // Lifecycle of the notification card
    LaunchedEffect(item.id) {
        delay(50)
        visible = true // Animate in
        delay(2500)
        exitState = true // Animate out
        delay(400)
        state.removeNotification(item.id) // Remove from state
    }

    // Animation states
    val springSpec = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
    val offsetX by animateFloatAsState(
        targetValue = if (exitState) 300f else if (visible) 0f else 300f,
        animationSpec = springSpec,
        label = "NotificationOffsetX"
    )
    val scale by animateFloatAsState(
        targetValue = if (exitState) 0.8f else 1f,
        animationSpec = springSpec,
        label = "NotificationScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible && !exitState) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "NotificationAlpha"
    )

    // Progress bar animation
    val progressAnimation = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progressAnimation.animateTo(0f, tween(durationMillis = 2500, easing = LinearEasing))
    }

    // --- MODIFIED: Use MaterialTheme colors for state indication ---
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = when (item.action) {
        ModuleAction.ENABLE -> colorScheme.primary
        ModuleAction.DISABLE -> colorScheme.error
    }
    val statusText = when (item.action) {
        ModuleAction.ENABLE -> "Enabled"
        ModuleAction.DISABLE -> "Disabled"
    }

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp)
            .alpha(alpha)
            .scale(scale)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .width(180.dp) // Slightly wider for better text fit
            .height(60.dp)
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)) // Use a semi-transparent surface color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Module name and status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.moduleName,
                    // --- MODIFIED: Use MaterialTheme typography and colors ---
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false) // Prevent text from pushing the dot
                )
                Spacer(Modifier.width(8.dp))
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
            }

            // Bottom row: Status text and progress bar
            Column {
                Text(
                    text = statusText,
                    // --- MODIFIED: Use MaterialTheme typography and colors ---
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimation.value)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
            }
        }
    }
}

/* -------------------- 状态 -------------------- */
private enum class ModuleAction { ENABLE, DISABLE }

private data class NotificationItem(
    val id: Int,
    val moduleName: String,
    val action: ModuleAction
)

private class NotificationState {
    private val _notifications = mutableStateListOf<NotificationItem>()
    val notifications: List<NotificationItem> get() = _notifications

    private var nextId = 0
    private val activeKeys = mutableSetOf<String>()

    fun addNotification(moduleName: String, action: ModuleAction) {
        val key = "$moduleName-${action.name}"
        if (key in activeKeys) return // Prevent duplicate notifications
        activeKeys.add(key)

        // Limit the number of visible notifications
        if (_notifications.size >= 3) {
            val oldest = _notifications.removeFirst()
            activeKeys.remove("${oldest.moduleName}-${oldest.action.name}")
        }
        _notifications.add(NotificationItem(nextId++, moduleName, action))
    }

    fun removeNotification(id: Int) {
        _notifications.find { it.id == id }?.let { item ->
            _notifications.remove(item)
            activeKeys.remove("${item.moduleName}-${item.action.name}")
        }
    }
}

@Preview
@Composable
private fun NotificationCardPreview() {
    val state = remember { NotificationState() }
    val item = NotificationItem(1, "Flight", ModuleAction.ENABLE)
    val item2 = NotificationItem(2, "Kill Aura", ModuleAction.DISABLE)
    
    LaunchedEffect(Unit) {
        state.addNotification("Flight", ModuleAction.ENABLE)
        state.addNotification("Kill Aura", ModuleAction.DISABLE)
    }

    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NotificationCard(item = item, state = state)
            NotificationCard(item = item2, state = state)
        }
    }
}