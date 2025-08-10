/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.overlay.mods

import android.graphics.Bitmap
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TargetHudOverlay : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        MaterialTheme {
            TargetHudContent(
                username = targetData.username,
                image = targetData.image,
                distance = targetData.distance,
                maxDistance = targetData.maxDistance,
                health = targetData.health,
                maxHealth = targetData.maxHealth,
                absorption = targetData.absorption,
                isHurt = targetData.isHurt
            )
        }
    }

    companion object {
        private var overlayInstance: TargetHudOverlay? = null
        private var targetData by mutableStateOf(TargetData())
        private var dismissalJob: Job? = null

        fun showOrUpdateTarget(
            username: String, image: Bitmap? = null, distance: Float, maxDistance: Float = 50f,
            health: Float, maxHealth: Float = 20f, absorption: Float = 0f, isHurt: Boolean = false
        ) {
            targetData = TargetData(
                username, image, distance, maxDistance, health, maxHealth, absorption, isHurt, isVisible = true
            )

            if (overlayInstance == null) {
                overlayInstance = TargetHudOverlay()
                OverlayManager.showOverlayWindow(overlayInstance!!)
            }

            // Reset the dismissal timer every time the HUD is updated
            dismissalJob?.cancel()
            dismissalJob = CoroutineScope(Dispatchers.Main).launch {
                delay(3000L)
                dismissTargetHud()
            }
        }

        fun dismissTargetHud() {
            targetData = targetData.copy(isVisible = false)
        }
    }
}

private data class TargetData(
    val username: String = "",
    val image: Bitmap? = null,
    val distance: Float = 0f,
    val maxDistance: Float = 50f,
    val health: Float = 20f,
    val maxHealth: Float = 20f,
    val absorption: Float = 0f,
    val isHurt: Boolean = false,
    val isVisible: Boolean = false
)

@Composable
private fun TargetHudContent(
    username: String, image: Bitmap?, distance: Float, maxDistance: Float,
    health: Float, maxHealth: Float, absorption: Float, isHurt: Boolean
) {
    val animatedHealth by animateFloatAsState(health / maxHealth, tween(600, easing = EaseOutCubic), label = "health")
    val animatedAbsorption by animateFloatAsState(absorption / maxHealth, tween(600, easing = EaseOutCubic), label = "absorption")
    val animatedDistance by animateFloatAsState(distance, tween(600, easing = EaseOutCubic), label = "distance")

    val hurtScale by animateFloatAsState(
        if (isHurt) 0.95f else 1f,
        spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh),
        label = "hurt_scale"
    )

    // Generate a consistent color based on the username's hash code
    val baseHue = remember(username) { (username.hashCode() % 360 + 360) % 360f }
    val primaryColor = remember(baseHue) { Color.hsv(baseHue, 0.7f, 0.95f) }
    val secondaryColor = remember(baseHue) { Color.hsv((baseHue + 40) % 360, 0.8f, 0.9f) }
    val statusColor = remember(baseHue) { Color.hsv((baseHue + 90) % 360, 0.6f, 1f) }

    val colorScheme = MaterialTheme.colorScheme

    // Dismiss logic
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(targetData.isVisible) {
        isVisible = targetData.isVisible
        if (!isVisible) {
            delay(300)
            targetData.let {
                if (!it.isVisible) {
                    OverlayManager.dismissOverlayWindow(targetData.let {
                        // This assumes the calling context can handle dismissal.
                        // A more robust pattern would involve a callback or direct instance management.
                    } as OverlayWindow)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300, easing = EaseOutCubic)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300))
    ) {
        ElevatedCard(
            modifier = Modifier
                .width(220.dp)
                .wrapContentHeight()
                .scale(hurtScale),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainer.copy(alpha = 0.85f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player Avatar
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).border(2.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "$username's Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = username.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Health & Absorption Bar
                    HealthBar(
                        healthPercent = animatedHealth,
                        absorptionPercent = animatedAbsorption,
                        healthColor = primaryColor,
                        absorptionColor = secondaryColor
                    )

                    // Distance & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${"%.1f".format(animatedDistance)}m",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getDistanceStatus(animatedDistance),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthBar(healthPercent: Float, absorptionPercent: Float, healthColor: Color, absorptionColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth(healthPercent).fillMaxHeight().background(healthColor))
            Box(Modifier.fillMaxWidth(absorptionPercent / (1 - healthPercent)).fillMaxHeight().background(absorptionColor))
        }
    }
}


private fun getDistanceStatus(distance: Float): String = when {
    distance <= 5f -> "危险"
    distance <= 15f -> "近距离"
    distance <= 30f -> "中距离"
    else -> "远距离"
}


@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun TargetHudPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TargetHudContent(
                username = "ProGamer_1234", image = null, distance = 7.8f, maxDistance = 50f,
                health = 15f, maxHealth = 20f, absorption = 4f, isHurt = false
            )
            TargetHudContent(
                username = "NoobSlayer", image = null, distance = 25.1f, maxDistance = 50f,
                health = 8f, maxHealth = 20f, absorption = 0f, isHurt = true
            )
        }
    }
}