package com.phoenix.luminacn.overlay.kitsugui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoenix.luminacn.R
import com.phoenix.luminacn.WallpaperUtils
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.overlay.manager.KitsuSettingsOverlay
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow
import com.phoenix.luminacn.ui.component.ConfigCategoryContent
import com.phoenix.luminacn.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KitsuGUI : OverlayWindow() {

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 1001
    }

    val modernFont = FontFamily(
        Font(R.font.fredoka_light)
    )

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 20
            }

            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            dimAmount = 0.7f
            windowAnimations = 0
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var selectedCheatCategory by mutableStateOf(CheatCategory.Home)
    private var selectedModule by mutableStateOf<Element?>(null)

    @Composable
    override fun Content() {
        val configuration = LocalConfiguration.current
        val cardWidth = 660.dp
        val cardHeight = 500.dp
        val context = LocalContext.current
        var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }

        var shouldAnimate by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // 获取壁纸
        LaunchedEffect(Unit) {
            wallpaperBitmap = WallpaperUtils.getWallpaperBitmap(context)
            delay(50)
            shouldAnimate = true
        }

        val translateY by animateFloatAsState(
            targetValue = if (shouldAnimate) 0f else 100f,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            label = "slideAnimation"
        )

        val alpha by animateFloatAsState(
            targetValue = if (shouldAnimate) 1f else 0f,
            animationSpec = tween(
                durationMillis = 350,
                easing = LinearOutSlowInEasing
            ),
            label = "fadeAnimation"
        )

        val dismissWithAnimation = remember {
            {
                shouldAnimate = false
                scope.launch {
                    delay(200)
                    OverlayManager.dismissOverlayWindow(this@KitsuGUI)
                }
                Unit
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x70000000),
                            Color(0x90000000)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (selectedModule == null) {
                        dismissWithAnimation()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent // 设置卡片背景为透明
                ),
                modifier = Modifier
                    .widthIn(max = cardWidth)
                    .height(cardHeight)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .graphicsLayer {
                        translationY = translateY
                        this.alpha = alpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Prevent clicks from passing through */ },
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 壁纸背景层 (80%)
                    wallpaperBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) // 20% 主题色
                            )
                    )
                    
                    // UI内容层
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Sidebar()
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        MainUICard(dismissWithAnimation)
                    }
                }
            }

            selectedModule?.let { module ->
                KitsuSettingsOverlay(
                    element = module,
                    onDismiss = { selectedModule = null }
                )
            }
        }
    }

    @Composable
    private fun Sidebar() {
        Column(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight()
                .background(
                    color = Color.Transparent, // 透明背景使用底层混合背景
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary, 
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.moon_stars_24),
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "LUMINA",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = modernFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(23.dp))

            VerticalCategoryStack()
        }
    }

    @Composable
    private fun VerticalCategoryStack() {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            items(CheatCategory.entries) { category ->
                VerticalCategoryItem(
                    category = category,
                    isSelected = selectedCheatCategory == category,
                    onClick = {
                        selectedCheatCategory = category
                        selectedModule = null
                    }
                )
            }
        }
    }

    @Composable
    private fun VerticalCategoryItem(
        category: CheatCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        var isHovered by remember { mutableStateOf(false) }

        val backgroundColor by animateColorAsState(
            targetValue = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                isHovered -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 200),
            label = "categoryBackgroundColor"
        )

        val textColor by animateColorAsState(
            targetValue = if (isSelected) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 200),
            label = "textColor"
        )

        val iconColor by animateColorAsState(
            targetValue = if (isSelected) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 200),
            label = "iconColor"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = category.iconResId),
                contentDescription = stringResource(id = category.labelResId),
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = getCategoryTitle(category),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = modernFont,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun MainUICard(dismissWithAnimation: () -> Unit) {
        Card(
            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent // 透明背景使用底层混合背景
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    key(selectedCheatCategory) {
                        val titleAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 300),
                            label = "titleAlpha"
                        )

                        Text(
                            text = getCategoryTitle(selectedCheatCategory),
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontFamily = modernFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(titleAlpha)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current

                        ModernActionButton(
                            iconRes = R.drawable.discord_24,
                            onClick = {
                                openUrl("https://discord.gg/6kz3dcndrN", context)
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        ModernActionButton(
                            iconRes = R.drawable.browser_24,
                            onClick = {
                                openUrl("https://projectlumina.netlify.app/", context)
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        ModernActionButton(
                            iconRes = R.drawable.cross_circle_24,
                            onClick = dismissWithAnimation,
                            isClose = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                key(selectedCheatCategory) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(animationSpec = tween(300)) { it / 4 },
                        exit = fadeOut(animationSpec = tween(200)) +
                                slideOutHorizontally(animationSpec = tween(200)) { -it / 4 }
                    ) {
                        MainContentArea()
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContentArea() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    color = Color.Transparent, // 透明背景使用底层混合背景
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            if (selectedCheatCategory == CheatCategory.Config) {
                ConfigCategoryContent()
            }
            else if(selectedCheatCategory == CheatCategory.Home){
                HomeCategoryUi()
            }
            else {
                ModuleContent(
                    selectedCheatCategory,
                    onOpenSettings = { module ->
                        selectedModule = module
                    }
                )
            }
        }
    }

    @Composable
    private fun ModernActionButton(
        iconRes: Int,
        onClick: () -> Unit,
        isClose: Boolean = false
    ) {
        var isPressed by remember { mutableStateOf(false) }

        val backgroundColor by animateColorAsState(
            targetValue = when {
                isClose && isPressed -> MaterialTheme.colorScheme.error
                isClose -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                isPressed -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            animationSpec = tween(durationMillis = 150),
            label = "buttonBackgroundColor"
        )

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = tween(durationMillis = 100),
            label = "buttonScale"
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isPressed = true
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isClose) 
                    MaterialTheme.colorScheme.onError 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(100)
                isPressed = false
            }
        }
    }

    private fun openUrl(url: String, context: android.content.Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun getCategoryTitle(category: CheatCategory): String {
        return when (category) {
            CheatCategory.Combat -> "战斗"
            CheatCategory.Motion -> "移动"
            CheatCategory.World -> "世界"
            CheatCategory.Visual -> "渲染"
            CheatCategory.Misc -> "杂项"
            CheatCategory.Config -> "配置"
            CheatCategory.Home -> "首页"
            else -> "模块"
        }
    }
}