package com.phoenix.luminacn.phoenix

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 增强版灵动岛视图，支持无任务时隐藏功能
 */
@Composable
fun EnhancedDynamicIslandView(
    state: CompatDynamicIslandState,
    hideWhenNoTasks: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 计算是否应该显示灵动岛
    val shouldShow = remember(state.isExpanded, hideWhenNoTasks) {
        derivedStateOf {
            if (hideWhenNoTasks) {
                // 隐藏模式：只有当有任务时才显示
                state.isExpanded
            } else {
                // 正常模式：始终显示
                true
            }
        }
    }
    
    // 独立的显示/隐藏动画
    AnimatedVisibility(
        visible = shouldShow.value,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { -it }
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            initialScale = 0.8f
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutLinearInEasing
            )
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutLinearInEasing
            ),
            targetOffsetY = { -it }
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutLinearInEasing
            ),
            targetScale = 0.8f
        )
    ) {
        // 灵动岛主体视图
        DynamicIslandContent(
            state = state,
            modifier = modifier
        )
    }
}

@Composable
private fun DynamicIslandContent(
    state: CompatDynamicIslandState,
    modifier: Modifier = Modifier
) {
    val isExpanded = state.isExpanded
    val scale = state.scale
    
    // 动画尺寸
    val targetWidth by animateDpAsState(
        targetValue = if (isExpanded) 280.dp * scale else 120.dp * scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "width"
    )
    
    val targetHeight by animateDpAsState(
        targetValue = if (isExpanded) {
            (36.dp + (state.tasks.size * 60.dp)).coerceAtMost(300.dp) * scale
        } else {
            36.dp * scale
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "height"
    )
    
    val targetCornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 28.dp * scale else 18.dp * scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )
    
    Box(
        modifier = modifier
            .size(width = targetWidth, height = targetHeight)
            .clip(RoundedCornerShape(targetCornerRadius))
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(400, 100)) + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(400, 100)
                ) with fadeOut(animationSpec = tween(200)) + scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(200)
                )
            },
            label = "content"
        ) { expanded ->
            if (expanded) {
                ExpandedContent(state = state, scale = scale)
            } else {
                CollapsedContent(text = state.persistentText, scale = scale)
            }
        }
    }
}

@Composable
private fun ExpandedContent(
    state: CompatDynamicIslandState,
    scale: Float
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp * scale),
        verticalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        items(state.tasks.size) { index ->
            val task = state.tasks[index]
            AnimatedVisibility(
                visible = !task.removing && !task.isVisuallyHidden,
                enter = fadeIn(animationSpec = tween(500, index * 120)) + expandVertically(
                    animationSpec = tween(500, index * 120, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                TaskItemView(task = task, scale = scale)
            }
        }
    }
}

@Composable
private fun CollapsedContent(
    text: String,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = (13.sp.value * scale).coerceAtLeast(9f).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TaskItemView(
    task: DynamicIslandTask,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        // 任务图标区域
        Box(
            modifier = Modifier.size(24.dp * scale),
            contentAlignment = Alignment.Center
        ) {
            when (task.type) {
                DynamicIslandTask.Type.SWITCH -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp * scale)
                            .clip(RoundedCornerShape(2.dp * scale))
                            .background(
                                if (task.switchState) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Gray.copy(alpha = 0.5f)
                            )
                    )
                }
                DynamicIslandTask.Type.PROGRESS -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp * scale)
                            .clip(RoundedCornerShape(8.dp * scale))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                DynamicIslandTask.Type.MUSIC -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp * scale)
                            .clip(RoundedCornerShape(4.dp * scale))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    )
                }
            }
        }
        
        // 任务文本
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp * scale)
        ) {
            Text(
                text = task.title,
                color = Color.White,
                fontSize = (12.sp.value * scale).coerceAtLeast(8f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            
            task.subtitle?.let { subtitle ->
                Text(
                    text = subtitle.replace("|", " "),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = (10.sp.value * scale).coerceAtLeast(7f).sp,
                    maxLines = 1
                )
            }
        }
    }
}

// LazyColumn的简化替代
@Composable
private fun LazyColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement
    ) {
        val scope = object : LazyListScope {
            override fun items(count: Int, itemContent: @Composable (Int) -> Unit) {
                repeat(count) { index ->
                    itemContent(index)
                }
            }
        }
        scope.content()
    }
}

private interface LazyListScope {
    fun items(count: Int, itemContent: @Composable (Int) -> Unit)
}