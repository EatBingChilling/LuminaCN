package com.project.lumina.client.overlay.mods

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
            x = 24
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
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val maxNotifications = (screenWidth / 220.dp).toInt().coerceAtLeast(2).coerceAtMost(5)
        
        // When the list becomes empty, schedule dismissal
        LaunchedEffect(notifications.isEmpty()) {
            if (notifications.isEmpty()) {
                delay(300)
                OverlayManager.dismissOverlayWindow(this@OverlayNotification)
                onOverlayDismissed()
            }
        }

        // Update notification state with screen-based limit
        LaunchedEffect(maxNotifications) {
            notificationState.updateMaxNotifications(maxNotifications)
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
        visible = true
        delay(2500)
        exitState = true
        delay(400)
        state.removeNotification(item.id)
    }

    // Animation states
    val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
    val offsetX by animateFloatAsState(
        targetValue = if (exitState) 300f else if (visible) 0f else 300f,
        animationSpec = springSpec,
        label = "NotificationOffsetX"
    )
    val scale by animateFloatAsState(
        targetValue = if (exitState) 0.9f else 1f,
        animationSpec = springSpec,
        label = "NotificationScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible && !exitState) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "NotificationAlpha"
    )

    // Switch animation state
    var switchChecked by remember { mutableStateOf(item.action == ModuleAction.DISABLE) }
    LaunchedEffect(item.action) {
        delay(200) // Slight delay for visual effect
        switchChecked = item.action == ModuleAction.ENABLE
    }

    // Progress bar animation
    val progressAnimation = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progressAnimation.animateTo(0f, tween(durationMillis = 2500, easing = LinearEasing))
    }

    // Material Design 3 colors
    val colorScheme = MaterialTheme.colorScheme
    val isEnabled = item.action == ModuleAction.ENABLE
    val statusText = if (isEnabled) "Enabled" else "Disabled"

    // Card with Material Design 3 styling
    Card(
        modifier = Modifier
            .offset(x = offsetX.dp)
            .alpha(alpha)
            .scale(scale)
            .width(200.dp)
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Material Switch on the left
            Switch(
                checked = switchChecked,
                onCheckedChange = { }, // Read-only for display
                enabled = false, // Disable interaction
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = colorScheme.primary,
                    disabledCheckedTrackColor = colorScheme.primaryContainer,
                    disabledUncheckedThumbColor = colorScheme.outline,
                    disabledUncheckedTrackColor = colorScheme.surfaceVariant
                ),
                modifier = Modifier.scale(0.8f) // Slightly smaller for compact design
            )

            // Content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Module name and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.moduleName,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Status indicator badge
                    Surface(
                        shape = CircleShape,
                        color = if (isEnabled) colorScheme.primaryContainer else colorScheme.errorContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Material Design 3 Progress Indicator
                LinearProgressIndicator(
                    progress = { progressAnimation.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (isEnabled) colorScheme.primary else colorScheme.error,
                    trackColor = colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
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
    private var maxNotifications = 3

    fun updateMaxNotifications(max: Int) {
        maxNotifications = max
        // Trim existing notifications if needed
        while (_notifications.size > maxNotifications) {
            val oldest = _notifications.removeFirst()
            activeKeys.remove("${oldest.moduleName}-${oldest.action.name}")
        }
    }

    fun addNotification(moduleName: String, action: ModuleAction) {
        val key = "$moduleName-${action.name}"
        if (key in activeKeys) return
        activeKeys.add(key)

        // Use dynamic max notifications based on screen size
        if (_notifications.size >= maxNotifications) {
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
        Surface(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotificationCard(item = item, state = state)
                NotificationCard(item = item2, state = state)
            }
        }
    }
}