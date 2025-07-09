/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don't bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */

package com.project.lumina.client.overlay

import android.view.Gravity
import android.view.WindowManager
import android.graphics.PixelFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.ui.theme.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayNotification : OverlayWindow() {
    private val _layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 20
            format = PixelFormat.TRANSLUCENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private val notificationState = NotificationState()
        private var isOverlayShowing = false

        fun addNotification(moduleName: String) {
            notificationState.addNotification(moduleName)
            if (!isOverlayShowing) {
                try {
                    OverlayManager.showOverlayWindow(OverlayNotification())
                    isOverlayShowing = true
                } catch (e: Exception) {
                    
                }
            }
        }

        fun onOverlayDismissed() {
            isOverlayShowing = false
        }
    }

    @Composable
    override fun Content() {
        val notifications = notificationState.notifications

        LaunchedEffect(notifications.size) {
            if (notifications.isEmpty()) {
                delay(100)
                OverlayManager.dismissOverlayWindow(this@OverlayNotification)
                OverlayNotification.onOverlayDismissed()
            }
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(0.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                notifications.forEach { notification ->
                    key(notification.id) {
                        NotificationCard(notification, notificationState)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem, notificationState: NotificationState) {
    var visible by remember { mutableStateOf(false) }
    var exitState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = notification.id) {
        delay(50)
        visible = true
        delay(2500)
        exitState = true
        delay(400)
        notificationState.removeNotification(notification.id)
    }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = 0.001f
    )

    val offsetX by animateFloatAsState(
        targetValue = when {
            exitState -> 200f
            visible -> 0f
            else -> -200f
        },
        animationSpec = springSpec,
        label = "offsetX"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            exitState -> 0.8f
            visible -> 1f
            else -> 0.8f
        },
        animationSpec = springSpec,
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible && !exitState) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val progressAnimation = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scope.launch {
            progressAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(2500, easing = FastOutSlowInEasing)
            )
        }
    }

    val baseColor = ONotifBase 
    val accentColor = ONotifAccent 

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp)
            .alpha(alpha)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.6f),
                ambientColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(12.dp))
            .requiredWidthIn(max = 130.dp)
            .requiredHeightIn(max = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .width(130.dp)
                .background(
                    color = baseColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.moduleName,
                    color = ONotifText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    )
                )

                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = accentColor,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            Text(
                text = "Enabled",
                color = ONotifText.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        color = ONotifProgressbar.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(1.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnimation.value)
                        .height(2.dp)
                        .background(
                            color = ONotifProgressbar,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}


private fun cos(angle: Float): Float = kotlin.math.cos(angle)
private fun sin(angle: Float): Float = kotlin.math.sin(angle)

class NotificationState {
    private val _notifications = mutableStateListOf<NotificationItem>()
    val notifications: List<NotificationItem> get() = _notifications

    private var nextId = 0
    private val activeModules = mutableSetOf<String>()

    fun addNotification(moduleName: String) {
        if (moduleName in activeModules) {
            return
        }

        activeModules.add(moduleName)
        if (_notifications.size >= 3) _notifications.removeAt(0)
        _notifications.add(NotificationItem(nextId++, moduleName))
    }

    fun removeNotification(id: Int) {
        val notification = _notifications.find { it.id == id }
        notification?.let { activeModules.remove(it.moduleName) }
        _notifications.removeAll { it.id == id }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun NotificationCardPreview() {
    val sampleNotification = NotificationItem(id = 1, moduleName = "Sample Module")
    val notificationState = NotificationState().apply {
        addNotification(sampleNotification.moduleName)
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .background(Color.Black)
    ) {
        NotificationCard(
            notification = sampleNotification,
            notificationState = notificationState
        )
    }
}
data class NotificationItem(val id: Int, val moduleName: String)