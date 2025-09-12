@file:OptIn(ExperimentalAnimationApi::class)
package com.phoenix.luminacn.phoenix

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.sin

/**
 * 增强版灵动岛视图 - 高保真iPhone样式
 */
@Composable
fun EnhancedDynamicIslandView(
    state: CompatDynamicIslandState,
    hideWhenNoTasks: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shouldShow = remember(state.isExpanded, hideWhenNoTasks) {
        derivedStateOf {
            if (hideWhenNoTasks) {
                state.isExpanded
            } else {
                true
            }
        }
    }
    
    AnimatedVisibility(
        visible = shouldShow.value,
        enter = fadeIn(
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            initialOffsetY = { -it }
        ) + scaleIn(
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            initialScale = 0.7f
        ),
        exit = fadeOut(
            animationSpec = tween(400, easing = FastOutLinearInEasing)
        ) + slideOutVertically(
            animationSpec = tween(400, easing = FastOutLinearInEasing),
            targetOffsetY = { -it }
        ) + scaleOut(
            animationSpec = tween(400, easing = FastOutLinearInEasing),
            targetScale = 0.7f
        )
    ) {
        PremiumDynamicIsland(
            state = state,
            modifier = modifier
        )
    }
}

@Composable
private fun PremiumDynamicIsland(
    state: CompatDynamicIslandState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isExpanded = state.isExpanded
    val scale = state.scale
    val musicTask = state.tasks.find { it.type == DynamicIslandTask.Type.MUSIC && !it.removing }
    val visibleTasks = state.tasks.filter { !it.removing && !it.isVisuallyHidden }
    
    // 计算尺寸
    val collapsedWidth = with(density) { (200 * scale).dp }
    val collapsedHeight = with(density) { (36 * scale).dp }
    val expandedWidth = with(density) { 
        if (visibleTasks.isEmpty()) (280 * scale).dp 
        else (320 * scale).dp
    }
    val expandedHeight = with(density) {
        val baseHeight = 80
        val taskHeight = visibleTasks.size * 70
        val maxHeight = 400
        ((baseHeight + taskHeight).coerceAtMost(maxHeight) * scale).dp
    }
    
    // 动画值
    val targetWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "width"
    )
    
    val targetHeight by animateDpAsState(
        targetValue = if (isExpanded) expandedHeight else collapsedHeight,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "height"
    )
    
    val targetCornerRadius by animateDpAsState(
        targetValue = if (isExpanded) (28 * scale).dp else (18 * scale).dp,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "corner"
    )
    
    // 光泽动画
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .size(width = targetWidth, height = targetHeight)
            .softGlow(
                color = Color.Black.copy(alpha = 0.6f),
                borderRadius = targetCornerRadius,
                blurRadius = (16 * scale).dp,
                spread = (6 * scale).dp
            )
            .clip(RoundedCornerShape(targetCornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.95f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
            .drawWithContent {
                drawContent()
                // 光泽效果
                val gradientWidth = size.width * 0.4f
                val gradientStart = size.width * shimmerTranslate
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        start = Offset(gradientStart - gradientWidth, 0f),
                        end = Offset(gradientStart, size.height)
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(500, 150)
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(500, 150)
                ) with fadeOut(
                    animationSpec = tween(250)
                ) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(250)
                )
            },
            label = "content"
        ) { expanded ->
            if (expanded) {
                PremiumExpandedContent(
                    state = state,
                    scale = scale,
                    musicTask = musicTask,
                    visibleTasks = visibleTasks
                )
            } else {
                PremiumCollapsedContent(
                    persistentText = state.persistentText,
                    musicTask = musicTask,
                    scale = scale
                )
            }
        }
    }
}

@Composable
private fun PremiumCollapsedContent(
    persistentText: String,
    musicTask: DynamicIslandTask?,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (16 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 音乐图标动画
        AnimatedVisibility(
            visible = musicTask != null,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 脉动的音乐图标
                val pulseAnimation by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        tween(1000, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size((16 * scale * pulseAnimation).dp)
                )
                Spacer(Modifier.width((4 * scale).dp))
                
                musicTask?.title?.let { title ->
                    val truncatedTitle = if (title.length > 12) "${title.take(11)}…" else title
                    Text(
                        text = truncatedTitle,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = (13.sp.value * scale).coerceAtLeast(9f).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = " • ",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = (13.sp.value * scale).sp
                )
            }
        }
        
        // 主要信息
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((16 * scale).dp)
            )
            Spacer(Modifier.width((4 * scale).dp))
            Text(
                "Kitasan",
                color = MaterialTheme.colorScheme.primary,
                fontSize = (13.sp.value * scale).coerceAtLeast(9f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            
            Text(
                text = " • ",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = (13.sp.value * scale).sp
            )
            
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size((16 * scale).dp)
            )
            Spacer(Modifier.width((4 * scale).dp))
            Text(
                persistentText,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = (13.sp.value * scale).coerceAtLeast(9f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PremiumExpandedContent(
    state: CompatDynamicIslandState,
    scale: Float,
    musicTask: DynamicIslandTask?,
    visibleTasks: List<DynamicIslandTask>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((20 * scale).dp),
        verticalArrangement = Arrangement.spacedBy((16 * scale).dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
        ) {
            itemsIndexed(visibleTasks) { index, task ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(600, index * 100)
                    ) + expandVertically(
                        animationSpec = tween(600, index * 100, easing = FastOutSlowInEasing)
                    ) + slideInHorizontally(
                        animationSpec = tween(600, index * 100, easing = FastOutSlowInEasing),
                        initialOffsetX = { it / 2 }
                    ),
                    exit = fadeOut(
                        animationSpec = tween(400)
                    ) + slideOutVertically(
                        targetOffsetY = { -it / 2 }
                    ) + shrinkVertically(
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    )
                ) {
                    when (task.type) {
                        DynamicIslandTask.Type.MUSIC -> PremiumMusicItem(task, scale)
                        DynamicIslandTask.Type.SWITCH -> PremiumSwitchItem(task, scale)
                        DynamicIslandTask.Type.PROGRESS -> PremiumProgressItem(task, scale)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumMusicItem(task: DynamicIslandTask, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape((16 * scale).dp)
            )
            .padding((16 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((16 * scale).dp)
    ) {
        // 专辑封面或图标
        Box(
            modifier = Modifier
                .size((48 * scale).dp)
                .clip(RoundedCornerShape((12 * scale).dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            task.icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap().asImageBitmap(),
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Icon(
                Icons.Default.MusicNote,
                contentDescription = "音乐",
                tint = Color.White,
                modifier = Modifier.size((24 * scale).dp)
            )
        }
        
        // 音乐信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((4 * scale).dp)
        ) {
            Text(
                text = task.title,
                color = Color.White,
                fontSize = (15.sp.value * scale).coerceAtLeast(10f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            task.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = (12.sp.value * scale).coerceAtLeast(8f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 进度条
            Column(verticalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                task.progressText?.let { progressText ->
                    Text(
                        text = progressText,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = (10.sp.value * scale).coerceAtLeast(7f).sp,
                        maxLines = 1
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((6 * scale).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = task.progress.coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSwitchItem(task: DynamicIslandTask, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape((16 * scale).dp)
            )
            .padding((16 * scale).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 开关图标
            Surface(
                modifier = Modifier.size((40 * scale).dp),
                shape = CircleShape,
                color = if (task.switchState) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else 
                    Color.Gray.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = task.switchState,
                        transitionSpec = {
                            scaleIn() + fadeIn() with scaleOut() + fadeOut()
                        },
                        label = "switch_icon"
                    ) { isOn ->
                        Icon(
                            if (isOn) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isOn) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size((20 * scale).dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width((12 * scale).dp))
            
            Column {
                Text(
                    text = task.title,
                    color = Color.White,
                    fontSize = (15.sp.value * scale).coerceAtLeast(10f).sp,
                    fontWeight = FontWeight.Bold
                )
                
                task.subtitle?.let { subtitle ->
                    val parts = subtitle.split("|")
                    if (parts.size >= 2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = parts[0],
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = (12.sp.value * scale).coerceAtLeast(8f).sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width((8 * scale).dp))
                            Text(
                                text = parts[1],
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = (12.sp.value * scale).coerceAtLeast(8f).sp
                            )
                        }
                    } else {
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = (12.sp.value * scale).coerceAtLeast(8f).sp
                        )
                    }
                }
            }
        }
        
        // 动画开关
        Switch(
            checked = task.switchState,
            onCheckedChange = null,
            modifier = Modifier.scale(scale * 1.2f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun PremiumProgressItem(task: DynamicIslandTask, scale: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape((16 * scale).dp)
            )
            .padding((16 * scale).dp),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
        ) {
            task.icon?.let { drawable ->
                Surface(
                    modifier = Modifier.size((40 * scale).dp),
                    shape = RoundedCornerShape((12 * scale).dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = task.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding((8 * scale).dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = Color.White,
                    fontSize = (15.sp.value * scale).coerceAtLeast(10f).sp,
                    fontWeight = FontWeight.Bold
                )
                
                task.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = (12.sp.value * scale).coerceAtLeast(8f).sp
                    )
                }
            }
        }
        
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((8 * scale).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = task.progress.coerceIn(0.001f, 1f))
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .shimmerEffect()
            )
        }
    }
}

// 辅助函数
fun Modifier.softGlow(
    color: Color,
    borderRadius: Dp,
    blurRadius: Dp,
    spread: Dp
) = this.drawBehind {
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    if (blurRadius > 0.dp) {
        frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
            blurRadius.toPx(),
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
    }
    frameworkPaint.color = color.toArgb()
    val spreadPx = spread.toPx()
    val left = -spreadPx
    val top = -spreadPx
    val right = size.width + spreadPx
    val bottom = size.height + spreadPx
    drawIntoCanvas {
        it.drawRoundRect(left, top, right, bottom, borderRadius.toPx(), borderRadius.toPx(), paint)
    }
}

fun Modifier.shimmerEffect() = this.drawWithContent {
    drawContent()
    val shimmerWidth = size.width * 0.3f
    val shimmerOffset = (System.currentTimeMillis() % 2000) / 2000f * size.width
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.4f),
                Color.Transparent
            ),
            start = Offset(shimmerOffset - shimmerWidth, 0f),
            end = Offset(shimmerOffset, size.height)
        )
    )
}