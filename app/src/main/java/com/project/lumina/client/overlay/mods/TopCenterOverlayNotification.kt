/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.R // Assuming for preview
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TopCenterOverlayNotification : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100 // Position from the top
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        MaterialTheme {
            val notification = notificationState.currentNotification
            val onDismiss = { notificationState.clearNotification() }

            // Dismiss the overlay window when there's no notification
            LaunchedEffect(notification) {
                if (notification == null) {
                    delay(400) // Wait for exit animation
                    OverlayManager.dismissOverlayWindow(this@TopCenterOverlayNotification)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                notification?.let {
                    TopNotificationCard(it, onDismiss)
                }
            }
        }
    }

    companion object {
        private val notificationState = TopNotificationState()
        private var instance: TopCenterOverlayNotification? = null

        fun showNotification(
            title: String,
            subtitle: String,
            iconRes: Int? = null,
            duration: Long = 2500
        ) {
            notificationState.addNotification(title, subtitle, iconRes, duration)
            if (instance == null) {
                instance = TopCenterOverlayNotification()
                OverlayManager.showOverlayWindow(instance!!)
            }
        }
    }
}


@Composable
private fun TopNotificationCard(
    notification: TopNotificationItem,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val progressAnimatable = remember { Animatable(1f) }

    val colorScheme = MaterialTheme.colorScheme
    val gradientBrush = remember {
        Brush.linearGradient(colors = listOf(colorScheme.primary, colorScheme.tertiary))
    }

    LaunchedEffect(notification.id) {
        visible = true
        progressAnimatable.snapTo(1f)
        launch {
            progressAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = notification.progressDuration.toInt(), easing = LinearEasing)
            )
        }
        delay(notification.progressDuration)
        visible = false
        delay(400)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.8f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(tween(200)),
        exit = scaleOut(targetScale = 0.8f, animationSpec = tween(300)) +
               fadeOut(tween(200))
    ) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 360.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
            )
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(gradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        if (notification.iconRes != null) {
                            Icon(
                                painter = painterResource(id = notification.iconRes),
                                contentDescription = notification.title,
                                modifier = Modifier.size(24.dp),
                                tint = colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = notification.title.firstOrNull()?.uppercase() ?: "!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = notification.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progressAnimatable.value },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                )
            }
        }
    }
}

private class TopNotificationState {
    var currentNotification by mutableStateOf<TopNotificationItem?>(null)
        private set

    private var nextId = 0

    fun addNotification(title: String, subtitle: String, iconRes: Int?, progressDuration: Long) {
        currentNotification = TopNotificationItem(
            id = nextId++,
            title = title,
            subtitle = subtitle,
            iconRes = iconRes,
            progressDuration = progressDuration
        )
    }

    fun clearNotification() {
        currentNotification = null
    }
}

private data class TopNotificationItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val iconRes: Int?,
    val progressDuration: Long
)


@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun TopNotificationCardPreview() {
    MaterialTheme {
        TopNotificationCard(
            notification = TopNotificationItem(
                id = 0,
                title = "Aura Activated",
                subtitle = "Targeting nearest entity",
                iconRes = R.drawable.ic_discord,
                progressDuration = 5000L
            ),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun TopNotificationCardNoIconPreview() {
     MaterialTheme {
        TopNotificationCard(
            notification = TopNotificationItem(
                id = 1,
                title = "Profile Saved",
                subtitle = "Your settings have been updated.",
                iconRes = null,
                progressDuration = 5000L
            ),
            onDismiss = {}
        )
    }
}