package com.phoenix.luminacn.router.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.delay  // 添加缺失的导入

@Composable
fun AnimatedLauncherScreen() {
    var showContent by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // 屏幕方向检测
    val windowMetrics = remember {
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    }
    val screenWidth = with(LocalDensity.current) { windowMetrics.bounds.width().toDp() }
    val screenHeight = with(LocalDensity.current) { windowMetrics.bounds.height().toDp() }
    val isLandscape = screenWidth > screenHeight

    // 直接显示内容动画
    LaunchedEffect(Unit) {
        // 极短延迟确保动画可以触发
        delay(10)
        showContent = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 修正：添加 false 参数
        AnimatedBackground(false)
        
        // 根据屏幕方向显示内容
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLandscape) {
                LandscapeLauncherContent()
            } else {
                PortraitLauncherContent()
            }
        }
        
        // 版权信息
        Text(
            text = "Translate: Phoen1x_ 丨 © Project Lumina 2025",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
    }
}
