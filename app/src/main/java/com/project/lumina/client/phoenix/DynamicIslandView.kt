package com.project.lumina.client.phoenix

import android.animation.TimeInterpolator
import android.graphics.BlurMaskFilter
import android.graphics.drawable.Drawable
import android.view.Choreographer
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.*
import kotlin.math.max

// ====================================================================================
// == State Holder and Logic (无需修改)
// ====================================================================================
private const val VALUE_PROGRESS_TIMEOUT_MS = 3000L
private const val TIME_PROGRESS_GRACE_PERIOD_MS = 1000L
data class TaskItem(val type: Type, val identifier: String, var text: MutableState<String>, var subtitle: MutableState<String?>, var switchState: MutableState<Boolean> = mutableStateOf(false), val icon: Drawable? = null, var isTimeBased: Boolean = false, var lastUpdateTime: Long = System.currentTimeMillis(), var isAwaitingData: Boolean = false, var removing: MutableState<Boolean> = mutableStateOf(false), var duration: Long = 0, var progressJob: Job? = null, val displayProgress: Animatable<Float, AnimationVector1D> = Animatable(1.0f)) { enum class Type { SWITCH, PROGRESS } }
fun TimeInterpolator.toEasing(): Easing = Easing { x -> getInterpolation(x) }
@Stable
class DynamicIslandState(private val scope: CoroutineScope) {
    var persistentText by mutableStateOf("Phoen1x")
    val tasks = mutableStateListOf<TaskItem>()
    val isExpanded by derivedStateOf { tasks.isNotEmpty() }
    init { scope.launch { while (isActive) { updateTasks(); delay(250) } } }
    fun addSwitch(moduleName: String, state: Boolean) { val subtitle = if (state) "已开启" else "已关闭"; val duration = 2000L; val existingTask = tasks.find { it.identifier == moduleName }; if (existingTask != null) { existingTask.apply { text.value = moduleName; this.subtitle.value = subtitle; switchState.value = state; lastUpdateTime = System.currentTimeMillis(); this.duration = duration; if (removing.value) removing.value = false }; startTimeBasedAnimation(existingTask) } else { val task = TaskItem(type = TaskItem.Type.SWITCH, identifier = moduleName, text = mutableStateOf(moduleName), subtitle = mutableStateOf(subtitle), switchState = mutableStateOf(state), duration = duration, isTimeBased = true); startTimeBasedAnimation(task); tasks.add(0, task) } }
    fun addOrUpdateProgress(identifier: String, text: String, subtitle: String?, icon: Drawable?, progress: Float?, duration: Long?) { tasks.find { it.identifier == identifier }?.let { updateProgressInternal(it, text, subtitle, progress, duration) } ?: addProgressInternal(identifier, text, subtitle, icon, progress, duration) }
    fun hide() { tasks.forEach { it.progressJob?.cancel() }; tasks.clear() }
    private fun updateTasks() { if (tasks.isEmpty()) return; val currentTime = System.currentTimeMillis(); tasks.forEach { task -> if (!task.removing.value) { val shouldBeRemoved = when { task.isTimeBased -> { if (task.displayProgress.value <= 0.01f && !task.isAwaitingData) { task.isAwaitingData = true; task.lastUpdateTime = currentTime }; task.isAwaitingData && (currentTime - task.lastUpdateTime > TIME_PROGRESS_GRACE_PERIOD_MS) }; task.type == TaskItem.Type.PROGRESS -> currentTime - task.lastUpdateTime > VALUE_PROGRESS_TIMEOUT_MS; else -> false }; if (shouldBeRemoved) task.removing.value = true } }; tasks.removeAll { it.removing.value && !it.displayProgress.isRunning } }
    private fun addProgressInternal(identifier: String, text: String, subtitle: String?, icon: Drawable?, progressValue: Float?, duration: Long?) { val task = TaskItem(type = TaskItem.Type.PROGRESS, identifier = identifier, text = mutableStateOf(text), subtitle = mutableStateOf(subtitle), icon = icon?.mutate()); if (progressValue != null) { task.isTimeBased = false; scope.launch { task.displayProgress.snapTo(0f) }; animateProgressTo(task, progressValue) } else { task.isTimeBased = true; task.duration = duration ?: 5000L; startTimeBasedAnimation(task) }; tasks.add(0, task) }
    private fun updateProgressInternal(task: TaskItem, text: String, subtitle: String?, progressValue: Float?, duration: Long?) { task.text.value = text; task.subtitle.value = subtitle; task.lastUpdateTime = System.currentTimeMillis(); if (task.isAwaitingData || task.removing.value) { task.isAwaitingData = false; task.removing.value = false }; if (progressValue != null) { task.isTimeBased = false; animateProgressTo(task, progressValue) } else { task.isTimeBased = true; task.duration = duration ?: 5000L; startTimeBasedAnimation(task) } }
    private fun animateProgressTo(task: TaskItem, newProgressValue: Float) { task.progressJob?.cancel(); task.progressJob = scope.launch { task.displayProgress.animateTo(newProgressValue.coerceIn(0f, 1f), tween(500, easing = OvershootInterpolator(0.8f).toEasing())) } }
    private fun startTimeBasedAnimation(task: TaskItem) { task.progressJob?.cancel(); task.progressJob = scope.launch { if (task.displayProgress.value < 1.0f) { task.displayProgress.animateTo(1.0f, tween(300, easing = DecelerateInterpolator().toEasing())) }; task.displayProgress.animateTo(0f, tween(task.duration.toInt(), easing = LinearEasing)) } }
}

@Composable
fun rememberDynamicIslandState(): DynamicIslandState {
    val scope = rememberCoroutineScope()
    return remember { DynamicIslandState(scope) }
}

// ====================================================================================
// == UI Composables
// ====================================================================================
@OptIn(ExperimentalTextApi::class, ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView(
    state: DynamicIslandState,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val collapsedHeight = 32.dp * scale
    val collapsedCornerRadius = collapsedHeight / 2
    val expandedCornerRadius = 24.dp * scale
    val itemHeight = 56.dp * scale
    val viewPadding = 12.dp * scale

    // 实时计算常驻文本所需宽度 - 增加更多缓冲空间
    val collapsedWidth by remember(state.persistentText, density, scale) {
        derivedStateOf {
            val textStyle = TextStyle(fontSize = 13.sp * scale, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
            
            // 分别测量各部分文本宽度，使用更保守的估算
            val lumiWidth = textMeasurer.measure(AnnotatedString("LuminaCN B22"), style = textStyle).size.width
            val fpsWidth = textMeasurer.measure(AnnotatedString("9999 FPS"), style = textStyle.copy(fontFamily = FontFamily.Monospace)).size.width // 使用4位数FPS预估
            val persistentWidth = textMeasurer.measure(AnnotatedString(state.persistentText), style = textStyle).size.width
            val separatorWidth = textMeasurer.measure(AnnotatedString(" • "), style = textStyle).size.width * 2
            
            val totalContentWidth = lumiWidth + fpsWidth + persistentWidth + separatorWidth
            val extraBuffer = 64.dp * scale // 增加额外缓冲空间
            
            with(density) { 
                (totalContentWidth / this.density).dp + extraBuffer
            }
        }
    }

    // 实时计算展开内容所需宽度 - 增加更多缓冲空间
    val expandedWidth by remember(state.tasks, density, scale) {
        derivedStateOf {
            if (state.tasks.isEmpty()) {
                250.dp * scale // 增加最小宽度
            } else {
                val requiredWidthInPixels = state.tasks.maxOfOrNull { task ->
                    // Switch实际占用更多空间，增加缓冲
                    val iconWidthPx = with(density) { 
                        (if (task.type == TaskItem.Type.SWITCH) 72.dp else 48.dp).toPx() * scale // 增加空间
                    }
                    val spacingPx = with(density) { 16.dp.toPx() * scale } // 增加间距
                    val sidePaddingPx = with(density) { 32.dp.toPx() * scale } // 增加边距
                    
                    val mainTextWidth = textMeasurer.measure(
                        AnnotatedString(task.text.value), 
                        style = TextStyle(fontSize = 14.sp * scale, fontWeight = FontWeight.Bold)
                    ).size.width.toFloat()
                    
                    val subtitleWidth = task.subtitle.value?.let { subtitle ->
                        textMeasurer.measure(
                            AnnotatedString(subtitle), 
                            style = TextStyle(fontSize = 10.sp * scale)
                        ).size.width.toFloat()
                    } ?: 0f
                    
                    val maxTextWidth = max(mainTextWidth, subtitleWidth)
                    val extraBuffer = with(density) { 32.dp.toPx() * scale } // 额外缓冲
                    
                    sidePaddingPx + iconWidthPx + spacingPx + maxTextWidth + extraBuffer
                } ?: with(density) { (250.dp * scale).toPx() }
                
                with(density) { 
                    (requiredWidthInPixels / this.density).dp.coerceAtLeast(250.dp * scale)
                }
            }
        }
    }

    val expandedHeight by remember(state.tasks.size, scale) { 
        derivedStateOf { 
            (state.tasks.size * itemHeight.value + viewPadding.value).dp.coerceAtMost(400.dp * scale) 
        } 
    }

    // 目标尺寸和动画
    val targetWidth = if (state.isExpanded) expandedWidth else collapsedWidth
    val targetHeight = if (state.isExpanded) expandedHeight else collapsedHeight
    val targetCorner = if (state.isExpanded) expandedCornerRadius else collapsedCornerRadius

    val springSpec = spring<Dp>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = springSpec, label = "width")
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = springSpec, label = "height")
    val animatedCorner by animateDpAsState(targetValue = targetCorner, animationSpec = springSpec, label = "corner")

    val glowAlpha by animateFloatAsState(targetValue = if (state.isExpanded) 1.0f else 0.0f, animationSpec = tween(500), label = "glowAlpha")
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "glowRotation")

    Box(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
            .clip(RoundedCornerShape(animatedCorner))
            .glow(alpha = glowAlpha, rotation = glowRotation, cornerRadius = with(LocalDensity.current) { animatedCorner.toPx() }, scale = scale)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state.isExpanded, 
            transitionSpec = { fadeIn(tween(200, 100)) with fadeOut(tween(100)) }, 
            label = "content"
        ) { isExpanded ->
            if (isExpanded) {
                ExpandedContent(state, scale)
            } else {
                CollapsedContent(state.persistentText, scale)
            }
        }
    }
}

@Composable
private fun ExpandedContent(state: DynamicIslandState, scale: Float) {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 6.dp * scale)) {
        state.tasks.forEach { task ->
            AnimatedVisibility(
                visible = !task.removing.value, 
                enter = fadeIn() + expandVertically(), 
                exit = fadeOut() + shrinkVertically()
            ) {
                TaskItemRow(task = task, scale = scale)
            }
        }
    }
}

@Composable
private fun TaskItemRow(task: TaskItem, scale: Float) {
    val progress by task.displayProgress.asState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp * scale)
            .padding(horizontal = 16.dp * scale), // 增加内边距
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            task.type == TaskItem.Type.SWITCH -> { 
                Switch(
                    checked = task.switchState.value,
                    onCheckedChange = null,
                    modifier = Modifier.scale(scale)
                )
            }
            task.icon != null -> { 
                Box(
                    modifier = Modifier
                        .size(36.dp * scale)
                        .clip(RoundedCornerShape(12.dp * scale))
                        .background(MaterialTheme.colorScheme.primaryContainer), 
                    contentAlignment = Alignment.Center
                ) { 
                    Image(
                        painter = rememberDrawablePainter(drawable = task.icon), 
                        contentDescription = task.text.value, 
                        modifier = Modifier.size(22.dp * scale), 
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    ) 
                } 
            }
        }
        Spacer(modifier = Modifier.width(12.dp * scale)) // 增加间距
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = task.text.value, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp * scale, fontWeight = FontWeight.Bold), 
                maxLines = 1
            )
            task.subtitle.value?.let { 
                Text(
                    text = it, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp * scale), 
                    maxLines = 1
                ) 
            }
            if (task.type == TaskItem.Type.PROGRESS || task.type == TaskItem.Type.SWITCH) {
                Spacer(modifier = Modifier.height(4.dp * scale))
                Box(
                    modifier = Modifier
                        .height(3.5.dp * scale)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) { 
                    Box(
                        modifier = Modifier
                            .height(3.5.dp * scale)
                            .fillMaxWidth(fraction = progress)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) 
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CollapsedContent(persistentText: String, scale: Float) {
    var currentFps by remember { mutableStateOf(0) }
    
    // 使用AnimatedContent为FPS变化添加动画
    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastTime: Long = System.nanoTime()
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val elapsedTimeNanos = frameTimeNanos - lastTime
                if (elapsedTimeNanos >= 1_000_000_000) { 
                    currentFps = frameCount
                    frameCount = 0
                    lastTime = frameTimeNanos 
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp * scale) // 增加内边距
    ) {
        val textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp * scale)
        
        Text(
            text = "LuminaCN B22", 
            style = textStyle, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Separator(scale)
        
        // 为FPS添加AnimatedContent以实现切换动画
        AnimatedContent(
            targetState = currentFps,
            transitionSpec = {
                // 使用滑动+淡入淡出效果
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300)
                ) with slideOutVertically(
                    targetOffsetY = { -it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(300)
                )
            },
            label = "fps_animation"
        ) { fps ->
            Text(
                text = "$fps FPS", 
                style = textStyle.copy(fontFamily = FontFamily.Monospace), 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Separator(scale)
        Text(
            text = persistentText, 
            style = textStyle, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Separator(scale: Float) { 
    Text(
        text = " • ", 
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp * scale), 
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
        modifier = Modifier.padding(horizontal = 3.dp * scale) // 增加分隔符间距
    ) 
}

private fun Modifier.glow(alpha: Float, rotation: Float, cornerRadius: Float, scale: Float) = this.drawWithContent {
    drawContent()
    if (alpha > 0f) {
        val frameworkPaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = (3.dp * scale).toPx()
            maskFilter = BlurMaskFilter((5.dp * scale).toPx(), BlurMaskFilter.Blur.NORMAL)
        }
        drawIntoCanvas { canvas ->
            val drawSize = this.size
            val rect = Rect(Offset.Zero, drawSize)
            val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, CornerRadius(cornerRadius))) }
            val shader = android.graphics.SweepGradient(drawSize.width / 2f, drawSize.height / 2f, intArrayOf(Color.Cyan.toArgb(), Color.Magenta.toArgb(), Color.Yellow.toArgb(), Color.Cyan.toArgb()), null)
            val matrix = android.graphics.Matrix()
            matrix.setRotate(rotation, drawSize.width / 2f, drawSize.height / 2f)
            shader.setLocalMatrix(matrix)
            frameworkPaint.shader = shader
            frameworkPaint.alpha = (alpha * 255).toInt()
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), frameworkPaint)
        }
    }
}