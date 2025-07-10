package com.project.lumina.client.overlay.mods

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.ui.theme.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//叮咚鸡大狗叫


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
                verticalArrangement = Arrangement.spacedBy(12.dp), // 增加间距防止重叠
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
        delay(2000) // 通知显示时长
        exitState = true
        delay(300) // 退出动画时长
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
                elevation = 12.dp, // 增加阴影深度
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.8f),
                ambientColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(12.dp))
            .width(140.dp) // 固定宽度防止重叠
            .height(75.dp) // 固定高度防止重叠
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = baseColor.copy(alpha = 0.95f), // 增加不透明度
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp), // 增加内边距
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上部分：模块名称和指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.moduleName,
                    color = ONotifText,
                    fontSize = 12.sp, // 稍微增大字体
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(6.dp) // 增大指示器
                        .background(
                            color = accentColor,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            // 中间：状态文本
            Text(
                text = "Enabled",
                color = ONotifText.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 0.5f),
                        blurRadius = 1f
                    )
                )
            )

            // 下部分：进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp) // 增加进度条高度
                    .background(
                        color = ONotifProgressbar.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnimation.value)
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    ONotifProgressbar,
                                    ONotifProgressbar.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(1.5.dp)
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

        // 限制最多显示3个通知，防止过度重叠
        if (_notifications.size >= 3) {
            val oldest = _notifications.removeFirst()
            activeModules.remove(oldest.moduleName)
        }
        _notifications.add(NotificationItem(nextId++, moduleName))
    }

    fun removeNotification(id: Int) {
        val notification = _notifications.find { it.id == id }
        if (notification != null) {
            _notifications.remove(notification)
            activeModules.remove(notification.moduleName) 
        }
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