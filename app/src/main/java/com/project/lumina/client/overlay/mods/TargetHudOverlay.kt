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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                targetData = targetData,
                onDismissed = {
                    OverlayManager.dismissOverlayWindow(this)
                    overlayInstance = null
                }
            )
        }
    }

    companion object {
        private var overlayInstance: TargetHudOverlay? = null
        private var targetData by mutableStateof(TargetData())
        private var dismissalJob: Job? = null

        // --- API RESTORED ---
        fun showTargetHud(
            username: String,
            image: Bitmap?,
            distance: Float,
            maxDistance: Float = 50f, // This param seems unused in original logic, but keeping for compatibility
            hurtTime: Float = 0f
        ) {
            // Internally, we map to our new data class
            targetData = TargetData(
                username = username,
                image = image,
                distance = distance,
                isHurt = hurtTime > 0f,
                isVisible = true
            )

            if (overlayInstance == null) {
                overlayInstance = TargetHudOverlay()
                OverlayManager.showOverlayWindow(overlayInstance!!)
            }

            dismissalJob?.cancel()
            dismissalJob = CoroutineScope(Dispatchers.Main).launch {
                delay(3000L)
                dismissTargetHud()
            }
        }

        fun dismissTargetHud() {
            targetData = targetData.copy(isVisible = false)
        }
        
        fun isTargetHudVisible(): Boolean = targetData.isVisible
    }
}

private data class TargetData(
    val username: String = "",
    val image: Bitmap? = null,
    val distance: Float = 0f,
    val isHurt: Boolean = false,
    val isVisible: Boolean = false,
    // Add health/absorption if they become available from the caller
    val health: Float = 20f,
    val maxHealth: Float = 20f,
    val absorption: Float = 0f
)

@Composable
private fun TargetHudContent(
    targetData: TargetData,
    onDismissed: () -> Unit
) {
    val (username, image, distance, isHurt, isVisible, health, maxHealth, absorption) = targetData

    val animatedHealth by animateFloatAsState(health / maxHealth, tween(600, easing = EaseOutCubic), label = "health")
    val animatedAbsorption by animateFloatAsState(absorption / maxHealth, tween(600, easing = EaseOutCubic), label = "absorption")
    val animatedDistance by animateFloatAsState(distance, tween(600, easing = EaseOutCubic), label = "distance")
    val hurtScale by animateFloatAsState(if (isHurt) 0.95f else 1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh), label = "hurt_scale")

    val baseHue = remember(username) { (username.hashCode() % 360 + 360) % 360f }
    val primaryColor = remember(baseHue) { Color.hsv(baseHue, 0.7f, 0.95f) }
    val secondaryColor = remember(baseHue) { Color.hsv((baseHue + 40) % 360, 0.8f, 0.9f) }
    val statusColor = remember(baseHue) { Color.hsv((baseHue + 90) % 360, 0.6f, 1f) }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(300)
            onDismissed()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300, easing = EaseOutCubic)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300))
    ) {
        ElevatedCard(
            modifier = Modifier.width(220.dp).wrapContentHeight().scale(hurtScale),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainer.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).border(2.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (image != null) {
                        Image(bitmap = image.asImageBitmap(), contentDescription = "$username's Avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Text(text = username.take(2).uppercase(), style = MaterialTheme.typography.headlineSmall, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    // The original didn't show health, so we use a distance bar instead.
                    DistanceBar(distancePercent = 1f - (animatedDistance / 50f).coerceIn(0f, 1f), color = primaryColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${"%.1f".format(animatedDistance)}m", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(text = getDistanceStatus(animatedDistance), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceBar(distancePercent: Float, color: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxWidth(distancePercent).fillMaxHeight().background(color))
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
                targetData = TargetData(
                    username = "ProGamer_1234", image = null, distance = 7.8f,
                    isHurt = false, isVisible = true
                ),
                onDismissed = {}
            )
        }
    }
}