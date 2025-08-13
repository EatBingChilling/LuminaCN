package com.project.lumina.client.overlay.mods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ... (The rest of the file is the same as the previous correct one)
// I'll paste the full class to be safe.

enum class NotificationType {
    SUCCESS, WARNING, ERROR, INFO
}

object SimpleOverlayNotification {
    private val notificationState = SimpleNotificationState()

    fun show(message: String, type: NotificationType = NotificationType.INFO, durationMs: Long = 3000) {
        notificationState.setNotification(SimpleNotificationItem(message, type, durationMs))
    }

    @Composable
    fun Content() {
        val notification = notificationState.currentNotification
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            notification?.let {
                SimpleNotificationCard(it, onDismiss = notificationState::clearNotification)
            }
        }
    }
}

private data class SimpleNotificationItem(
    val message: String,
    val type: NotificationType,
    val duration: Long
)

private class SimpleNotificationState {
    var currentNotification by mutableStateOf<SimpleNotificationItem?>(null)
        private set

    fun setNotification(notification: SimpleNotificationItem) { currentNotification = notification }
    fun clearNotification() { currentNotification = null }
}

@Composable
private fun SimpleNotificationCard(
    notification: SimpleNotificationItem,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = notification.duration.toInt(), easing = LinearEasing),
        label = "NotificationProgress"
    )

    val (icon, backgroundColor, contentColor, accentColor) = when (notification.type) {
        NotificationType.SUCCESS -> Quadruple(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
        NotificationType.WARNING -> Quadruple(Icons.Filled.Warning, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiary)
        NotificationType.ERROR -> Quadruple(Icons.Filled.Error, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.error)
        NotificationType.INFO -> Quadruple(Icons.Filled.Info, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondary)
    }

    LaunchedEffect(notification) {
        visible = true
        delay(notification.duration)
        visible = false
        delay(500)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { -it * 2 }, animationSpec = tween(500, easing = EaseOutCubic)),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { -it * 2 }, animationSpec = tween(400, easing = EaseInCubic))
    ) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor, contentColor = contentColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(imageVector = icon, contentDescription = notification.type.name, tint = accentColor, modifier = Modifier.size(28.dp))
                    Text(text = notification.message, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                LinearProgressIndicator(
                    progress = { 1f - progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Butt
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
