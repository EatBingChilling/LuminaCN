package com.project.lumina.client.overlay.protohax

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.overlay.kitsugui.HomeCategoryUi
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import com.project.lumina.client.ui.component.ConfigCategoryContent // <-- 1. 导入 ConfigCategoryContent
import com.project.lumina.client.ui.component.NavigationRailX

class ProtohaxUi : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags =
                flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 15
            }
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            dimAmount = 0.4f
            windowAnimations = android.R.style.Animation_Dialog
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var selectedModuleCategory by mutableStateOf(CheatCategory.Home)

    @Composable
    override fun Content() {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    OverlayManager.dismissOverlayWindow(this)
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(vertical = 30.dp, horizontal = 100.dp)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRailX(
                        windowInsets = WindowInsets(8, 8, 8, 8)
                    ) {
                        // 2. 移除 filter，让所有类别都显示在导航栏中
                        CheatCategory.entries.fastForEach { cheatCategory ->
                            NavigationRailItem(
                                selected = selectedModuleCategory === cheatCategory,
                                onClick = {
                                    if (selectedModuleCategory !== cheatCategory) {
                                        selectedModuleCategory = cheatCategory
                                    }
                                },
                                icon = {
                                    Icon(
                                        painterResource(cheatCategory.iconResId),
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(stringResource(cheatCategory.labelResId))
                                },
                                alwaysShowLabel = true
                            )
                        }
                    }
                    VerticalDivider()

                    AnimatedContent(
                        targetState = selectedModuleCategory,
                        label = "animatedPage",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) { targetCategory ->
                        Box(Modifier.fillMaxSize()) {
                            // 3. 在 when 语句中添加 Config 分支
                            when (targetCategory) {
                                CheatCategory.Home -> HomeCategoryUi()
                                CheatCategory.Config -> ConfigCategoryContent()
                                else -> ModuleContentY(targetCategory)
                            }
                        }
                    }
                }
            }
        }
    }
}