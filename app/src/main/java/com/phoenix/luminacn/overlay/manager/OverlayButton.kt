package com.phoenix.luminacn.overlay.manager

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.phoenix.luminacn.R
import com.phoenix.luminacn.overlay.grace.GraceMenuUi
import com.phoenix.luminacn.overlay.protohax.ProtohaxUi
import com.phoenix.luminacn.overlay.clickgui.ClickGUI
import com.phoenix.luminacn.overlay.kitsugui.KitsuGUI
import kotlin.math.*
import kotlin.random.Random

class OverlayButton : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            windowAnimations = android.R.style.Animation_Toast
            x = 0
            y = 100
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private val kitsuGUI by lazy { KitsuGUI() }
    private val graceGUI by lazy { GraceMenuUi() }
    private val protohaxUi by lazy { ProtohaxUi() }
    private val clickGUI by lazy { ClickGUI() }

    // 枚举来定义图标类型
    enum class IconType {
        VECTOR, BITMAP
    }

    // 数据类来存储图标信息
    data class IconResource(
        val resourceId: Int,
        val type: IconType
    )

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        val configuration = LocalConfiguration.current
        val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        LaunchedEffect(isLandScape) {
            _layoutParams.x = min(width, _layoutParams.x)
            _layoutParams.y = min(height, _layoutParams.y)
            windowManager.updateViewLayout(composeView, _layoutParams)
        }

        // 使用位图 img 作为图标
        val iconResource = IconResource(
            resourceId = R.drawable.img,
            type = IconType.BITMAP
        )

        val prefs = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        // 默认改为 ProtohaxUi
        var selectedGUIName by remember { mutableStateOf(prefs.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi") }

        DisposableEffect(Unit) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "selectedGUI") {
                    selectedGUIName = prefs.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi"
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        val selectedGUI = when (selectedGUIName) {
            "KitsuGUI" -> kitsuGUI
            "ProtohaxUi" -> protohaxUi
            "ClickGUI" -> clickGUI
            else -> graceGUI
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        _layoutParams.x += drag.x.toInt()
                        _layoutParams.y += drag.y.toInt()
                        windowManager.updateViewLayout(composeView, _layoutParams)
                    }
                }
                .clickable { OverlayManager.showOverlayWindow(selectedGUI) }
        ) {
            // 背景雾化动画
            FogAnimation()

            // 图标显示 - 使用完整的圆形容器
            IconDisplay(
                iconResource = iconResource,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    private fun IconDisplay(
        iconResource: IconResource,
        modifier: Modifier = Modifier
    ) {
        when (iconResource.type) {
            IconType.VECTOR -> {
                // 对于Vector资源使用Icon组件
                Icon(
                    imageVector = ImageVector.vectorResource(id = iconResource.resourceId),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = modifier
                        .clip(CircleShape)
                        .padding(4.dp)
                )
            }
            IconType.BITMAP -> {
                // 对于Bitmap资源(PNG/JPG)使用Image组件
                Image(
                    painter = painterResource(id = iconResource.resourceId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // 使用Crop来填满整个容器
                    modifier = modifier
                        .clip(CircleShape) // 圆形裁剪
                        .clipToBounds()
                )
            }
        }
    }
}

@Composable
private fun FogAnimation() {
    data class LayerParams(
        val speedMs: Int,
        val sizeFactor: Float,
        val blurFactor: Float,
        val alphaMin: Float,
        val alphaMax: Float
    )

    val baseLayers = listOf(
        LayerParams(18000, 0.5f, 0.4f, 0.05f, 0.15f),
        LayerParams(20000, 0.7f, 0.5f, 0.04f, 0.12f),
        LayerParams(22000, 0.9f, 0.6f, 0.03f, 0.10f),
        LayerParams(25000, 1.1f, 0.7f, 0.02f, 0.08f)
    )

    data class LayerConfig(
        val speed: Int,
        val sizeFactor: Float,
        val blurFactor: Float,
        val alphaMin: Float,
        val alphaMax: Float,
        val angleDeg: Float
    )

    val configs = baseLayers.map { base ->
        remember {
            val speed = (base.speedMs * (0.8f + Random.nextFloat() * 0.4f)).toInt()
            val sizeF = base.sizeFactor * (0.9f + Random.nextFloat() * 0.2f)
            val blurF = base.blurFactor * (0.9f + Random.nextFloat() * 0.2f)
            val aMin = base.alphaMin * (0.8f + Random.nextFloat() * 0.4f)
            val aMax = base.alphaMax * (0.8f + Random.nextFloat() * 0.4f)
            val angle = Random.nextFloat() * 360f
            LayerConfig(speed, sizeF, blurF, aMin, aMax, angle)
        }
    }

    val transition = rememberInfiniteTransition()
    data class AnimatedLayer(
        val offset: State<Float>,
        val alpha: State<Float>,
        val size: Float,
        val blur: Float,
        val angleDeg: Float
    )

    val animatedLayers = configs.map { cfg ->
        val offsetSt = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(cfg.speed, easing = LinearEasing),
                RepeatMode.Restart
            )
        )
        val alphaSt = transition.animateFloat(
            initialValue = cfg.alphaMin,
            targetValue = cfg.alphaMax,
            animationSpec = infiniteRepeatable(
                tween(cfg.speed / 2, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            )
        )
        AnimatedLayer(offsetSt, alphaSt, cfg.sizeFactor, cfg.blurFactor, cfg.angleDeg)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape) // 确保雾化动画也是圆形的
    ) {
        val w = size.width
        val h = size.height

        animatedLayers.forEach { layer ->
            val t = layer.offset.value
            val alpha = layer.alpha.value
            val radius = min(w, h) * layer.size
            val maxOffset = max(w, h) / 2f + radius

            val angleRad = Math.toRadians(layer.angleDeg.toDouble())
            val dx = cos(angleRad).toFloat()
            val dy = sin(angleRad).toFloat()

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(radius * layer.blur, BlurMaskFilter.Blur.NORMAL)
                color = Color.White.copy(alpha = alpha).toArgb()
            }

            listOf(-1f, 1f).forEach { dir ->
                val cx = w / 2f + dir * t * dx * maxOffset
                val cy = h / 2f + dir * t * dy * maxOffset
                drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius, paint)
            }
        }
    }
}