package com.phoenix.luminacn.phoenix

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hud.test.modules.dynamicisland.DynamicIslandState
import com.hud.test.modules.dynamicisland.DynamicIslandView

/**
 * 增强版灵动岛视图，支持无任务时隐藏功能
 * 保持原有样式，只是添加独立的显示/隐藏动画
 */
@Composable
fun EnhancedDynamicIslandView(
    state: DynamicIslandState,
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
    
    // 独立的显示/隐藏动画 - 与内部的展开/收缩动画分离
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
        // 使用原来的DynamicIslandView，保持原有的展开/收缩动画
        DynamicIslandView(
            state = state,
            modifier = modifier
        )
    }
}