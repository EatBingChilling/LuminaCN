package com.phoenix.luminacn.overlay.clickgui

import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.phoenix.luminacn.WallpaperUtils
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow

class ClickGUI : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 8
            }
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            dimAmount = 0.7f
            windowAnimations = android.R.style.Animation_Dialog
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        var isVisible by remember { mutableStateOf(false) }
        val context = LocalContext.current
        var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }

        // 获取壁纸
        LaunchedEffect(Unit) {
            wallpaperBitmap = WallpaperUtils.getWallpaperBitmap(context)
            isVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    OverlayManager.dismissOverlayWindow(this@ClickGUI)
                }
        ) {
            // 壁纸背景层
            wallpaperBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(
                        color = Color.White.copy(alpha = 0.8f), // 80% 壁纸显示
                        blendMode = BlendMode.Modulate
                    )
                )
            }
            
            // 主题色叠加层 (20%) + 渐变效果
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            ),
                            startY = 0.66f * 1000f,
                            endY = 1000f
                        )
                    )
            )

            // UI内容层
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 150)
                ),
                exit = scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(durationMillis = 150)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top
                ) {
                    CheatCategory.entries
                        .filter { it != CheatCategory.Config && it != CheatCategory.Home }
                        .fastForEach { category ->
                            CategoryOverlay(category = category)
                        }
                }
            }
        }
    }

    @Composable
    private fun CategoryOverlay(category: CheatCategory) {
        var isPressed by remember { mutableStateOf(false) }

        // 按压动画
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = tween(durationMillis = 100),
            label = "press_scale"
        )

        Column(
            modifier = Modifier
                .width(140.dp)
                .wrapContentHeight()
                .scale(scale),
            horizontalAlignment = Alignment.Start
        ) {
            // 分类标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painterResource(category.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = stringResource(category.labelResId),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize * 0.9,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }

            // 模块卡片
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .width(140.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .drawWithContent {
                        drawContent()
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPressed = true
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            // 卡片背景使用半透明的surface颜色
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                        .padding(6.dp)
                ) {
                    ModuleContentA(category)
                }
            }
        }

        // 重置按压状态
        LaunchedEffect(isPressed) {
            if (isPressed) {
                kotlinx.coroutines.delay(100)
                isPressed = false
            }
        }
    }
}