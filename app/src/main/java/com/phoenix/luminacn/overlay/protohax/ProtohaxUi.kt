package com.phoenix.luminacn.overlay.protohax

import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.overlay.kitsugui.HomeCategoryUi
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow
import com.phoenix.luminacn.ui.component.ConfigCategoryContent
import com.phoenix.luminacn.ui.component.NavigationRailX
import com.phoenix.luminacn.WallpaperUtils

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
        val context = LocalContext.current
        var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
        
        // 获取壁纸
        LaunchedEffect(Unit) {
            wallpaperBitmap = WallpaperUtils.getWallpaperBitmap(context)
        }

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
                    ) {},
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Transparent // 卡片背景透明，使用自定义背景
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 壁纸背景层 (80%)
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
                    
                    // 主题色叠加层 (20%)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) // 20% 主题色
                            )
                    )
                    
                    // UI内容层 - 保持正常不透明度，确保文本清晰
                    Row(Modifier.fillMaxSize()) {
                        NavigationRailX(
                            windowInsets = WindowInsets(8, 8, 8, 8)
                        ) {
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
                                .background(Color.Transparent) // 内容区域背景透明，使用底层混合背景
                        ) { targetCategory ->
                            Box(Modifier.fillMaxSize()) {
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
}