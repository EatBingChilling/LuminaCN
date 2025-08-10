/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.R // Assuming R file is accessible for preview
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// A notification for events like KillAura activation
class PacketNotificationOverlay(
    private val title: String,
    private val subtitle: String,
    private val iconRes: Int? = null,
    private val duration: Long = 1000L
) : OverlayWindow() {

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
            y = 100 // Position from the top
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        // Wrap with MaterialTheme to ensure colors are available
        MaterialTheme {
             PacketNotificationCard(title, subtitle, iconRes, duration, this)
        }
    }

    companion object {
        fun showNotification(
            title: String,
            subtitle: String,
            iconRes: Int? = null,
            duration: Long = 1000L
        ) {
            val overlay = PacketNotificationOverlay(title, subtitle, iconRes, duration)
            OverlayManager.showOverlayWindow(overlay)
        }
    }
}

@Composable
private fun PacketNotificationCard(
    title: String,
    subtitle: String,
    iconRes: Int?,
    duration: Long,
    overlay: OverlayWindow
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    val progressAnimatable = remember { Animatable(1f) }

    // --- MODIFIED: Use MaterialTheme colors for the gradient ---
    val colorScheme = MaterialTheme.colorScheme
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(colorScheme.primary, colorScheme.tertiary),
            start = Offset.Zero,
            end = Offset(200f, 200f)
        )
    }

    // Lifecycle: Animate in, wait, animate out, then dismiss
    LaunchedEffect(Unit) {
        visible = true
        scope.launch {
            progressAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = duration.toInt(), easing = LinearEasing)
            )
        }
        delay(duration)
        visible = false
        delay(300) // Wait for exit animation
        OverlayManager.dismissOverlayWindow(overlay)
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.8f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(tween(200)),
        exit = scaleOut(targetScale = 0.8f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
               fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .width(280.dp) // A bit wider for better text spacing
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(gradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        if (iconRes != null) {
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = title.firstOrNull()?.uppercase() ?: "!",
                                // --- MODIFIED: Use onPrimary for text on a primary-colored background ---
                                color = colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title and Subtitle Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            // --- MODIFIED: Use MaterialTheme typography and colors ---
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(colorScheme.surfaceVariant) // Background for the progress track
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimatable.value)
                            .height(4.dp)
                            .background(gradientBrush)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PacketNotificationPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PacketNotificationCard(
                title = "Kill Aura",
                subtitle = "Target locked: Notch",
                iconRes = R.drawable.ic_discord, // Example icon
                duration = 3000L,
                overlay = OverlayWindow() // Dummy overlay for preview
            )
            PacketNotificationCard(
                title = "Flight",
                subtitle = "Packet fly enabled",
                iconRes = null, // Preview without icon
                duration = 3000L,
                overlay = OverlayWindow()
            )
        }
    }
}