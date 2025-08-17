package com.phoen1x.bar

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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.*

private const val VALUE_PROGRESS_TIMEOUT_MS = 3000L
private const val TIME_PROGRESS_GRACE_PERIOD_MS = 1000L
data class TaskItem(val type: Type, val identifier: String, var text: MutableState<String>, var subtitle: MutableState<String?>, var switchState: MutableState<Boolean> = mutableStateOf(false), val icon: Drawable? = null, var isTimeBased: Boolean = false, var lastUpdateTime: Long = System.currentTimeMillis(), var isAwaitingData: Boolean = false, var removing: MutableState<Boolean> = mutableStateOf(false), var duration: Long = 0, var progressJob: Job? = null, val displayProgress: Animatable<Float, AnimationVector1D> = Animatable(1.0f)) { enum class Type { SWITCH, PROGRESS } }
fun TimeInterpolator.toEasing(): Easing = Easing { x -> getInterpolation(x) }
@Stable
class DynamicIslandState(private val scope: CoroutineScope) {
    var persistentText by mutableStateOf("Phoen1x")
    val tasks = mutableStateListOf<TaskItem>()
    val isExpanded by derivedStateOf { tasks.any { !it.removing.value } }
    init { scope.launch { while (isActive) { updateTasks(); delay(250) } } }
    fun addSwitch(moduleName: String, state: Boolean) { val subtitle = if (state) "已开启" else "已关闭"; val duration = 2000L; val existingTask = tasks.find { it.identifier == moduleName }; if (existingTask != null) { existingTask.apply { text.value = moduleName; this.subtitle.value = subtitle; switchState.value = state; lastUpdateTime = System.currentTimeMillis(); this.duration = duration; if (removing.value) removing.value = false }; startTimeBasedAnimation(existingTask) } else { val task = TaskItem(type = TaskItem.Type.SWITCH, identifier = moduleName, text = mutableStateOf(moduleName), subtitle = mutableStateOf(subtitle), switchState = mutableStateOf(state), duration = duration, isTimeBased = true); startTimeBasedAnimation(task); tasks.add(0, task) } }
    fun addOrUpdateProgress(identifier: String, text: String, subtitle: String?, icon: Drawable?, progress: Float?, duration: Long?) { tasks.find { it.identifier == identifier }?.let { updateProgressInternal(it, text, subtitle, progress, duration) } ?: addProgressInternal(identifier, text, subtitle, icon, progress, duration) }
    fun hide() { tasks.forEach { it.progressJob?.cancel() }; tasks.clear() }
    private fun updateTasks() { if (tasks.isEmpty()) return; val currentTime = System.currentTimeMillis(); tasks.forEach { task -> if (!task.removing.value) { val shouldBeRemoved = when { task.isTimeBased -> { if (task.displayProgress.value <= 0.01f && !task.isAwaitingData) { task.isAwaitingData = true; task.lastUpdateTime = currentTime }; task.isAwaitingData && (currentTime - task.lastUpdateTime > TIME_PROGRESS_GRACE_PERIOD_MS) }; task.type == TaskItem.Type.PROGRESS -> currentTime - task.lastUpdateTime > VALUE_PROGRESS_TIMEOUT_MS; else -> false }; if (shouldBeRemoved) task.removing.value = true } }; tasks.removeAll { it.removing.value && !it.displayProgress.isRunning } }
    private fun addProgressInternal(identifier: String, text: String, subtitle: String?, icon: Drawable?, progressValue: Float?, duration: Long?) { val task = TaskItem(type = TaskItem.Type.PROGRESS, identifier = identifier, text = mutableStateOf(text), subtitle = mutableStateOf(subtitle), icon = icon?.mutate()); if (progressValue != null) { task.isTimeBased = false; scope.launch { task.displayProgress.snapTo(0f) }; animateProgressTo(task, progressValue) } else { task.isTimeBased = true; task.duration = duration ?: 5000L; startTimeBasedAnimation(task) }; tasks.add(0, task) }
    private fun updateProgressInternal(task: TaskItem, text: String, subtitle: String?, progressValue: Float?, duration: Long?) { task.text.value = text; task.subtitle.value = subtitle; task.lastUpdateTime = System.currentTimeMillis(); if (task.isAwaitingData || task.removing.value) { task.isAwaitingData = false; task.removing.value = false }; if (progressValue != null) { task.isTimeBased = false; animateProgressTo(task, progressValue) } else { task.isTimeBased = true; task.duration = duration ?: 5000L; startTimeBasedAnimation(task) } }
    private fun animateProgressTo(task: TaskItem, newProgressValue: Float) { task.progressJob?.cancel(); task.progressJob = scope.launch { task.displayProgress.animateTo(newProgressValue.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing)) } }
    private fun startTimeBasedAnimation(task: TaskItem) { task.progressJob?.cancel(); task.progressJob = scope.launch { if (task.displayProgress.value < 1.0f) { task.displayProgress.animateTo(1.0f, tween(500, easing = DecelerateInterpolator().toEasing())) }; task.displayProgress.animateTo(0f, tween(task.duration.toInt(), easing = LinearEasing)) } }
}

@Composable
fun rememberDynamicIslandState(): DynamicIslandState {
    val scope = rememberCoroutineScope()
    return remember { DynamicIslandState(scope) }
}

@OptIn(ExperimentalTextApi::class, ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView(
    state: DynamicIslandState,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val collapsedHeight = 34.dp * scale
    val collapsedCornerRadius = collapsedHeight / 2
    val expandedCornerRadius = 28.dp * scale
    val itemHeight = 60.dp * scale
    val viewPadding = 12.dp * scale

    val collapsedWidth by remember { derivedStateOf { 250.dp * scale } }
    val expandedWidth by remember { derivedStateOf { 280.dp * scale } }
    val expandedHeight by remember(state.tasks.size, scale) {
        derivedStateOf {
            (state.tasks.filterNot { it.removing.value }.size * itemHeight.value + viewPadding.value * 2).dp.coerceAtMost(400.dp * scale)
        }
    }

    val targetWidth = if (state.isExpanded) expandedWidth else collapsedWidth
    val targetHeight = if (state.isExpanded) expandedHeight else collapsedHeight
    val targetCorner = if (state.isExpanded) expandedCornerRadius else (targetHeight / 2)

    val springSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = springSpec, label = "width")
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = springSpec, label = "height")
    val animatedCorner by animateDpAsState(targetValue = targetCorner, animationSpec = springSpec, label = "corner")

    val glowPulse = remember { Animatable(0f) }
    LaunchedEffect(state.tasks.size) {
        if (state.tasks.any { !it.removing.value }) {
            glowPulse.snapTo(1f)
            glowPulse.animateTo(0f, animationSpec = tween(1000, easing = LinearEasing))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sheen")
    val sheenTranslate by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing, delayMillis = 500), RepeatMode.Restart), label = "sheenTranslate"
    )

    Box(
        modifier = modifier
         
            .size(width = animatedWidth, height = animatedHeight)
            .clip(RoundedCornerShape(animatedCorner))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            )
            .drawWithContent {
                drawContent()
                val gradientWidth = size.width * 0.5f
                val gradientStart = size.width * sheenTranslate
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent),
                        start = Offset(gradientStart - gradientWidth, 0f),
                        end = Offset(gradientStart, size.height)
                    )
                )
            }
            .glow(pulse = glowPulse.value, cornerRadius = with(LocalDensity.current) { animatedCorner.toPx() }, scale = scale),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state.isExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400, 100)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400, 100))) with
                        (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f))
            },
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
        state.tasks.forEachIndexed { index, task ->
            val isVisible = !task.removing.value
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = index * 120)) +
                        expandVertically(animationSpec = tween(durationMillis = 500, delayMillis = index * 120, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                        slideOutVertically(targetOffsetY = { -it / 2 }) +
                        shrinkVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
            ) {
                TaskItemRow(task = task, scale = scale, isVisible = isVisible)
            }
        }
    }
}

@Composable
private fun TaskItemRow(task: TaskItem, scale: Float, isVisible: Boolean) {
    val progress by task.displayProgress.asState()

    val contentOffsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "contentOffsetY"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 150),
        label = "contentAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp * scale)
            .padding(horizontal = 20.dp * scale)
            .graphicsLayer {
                translationY = contentOffsetY.toPx()
                alpha = contentAlpha
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconScale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.5f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "iconScale"
        )

        Box(
            modifier = Modifier.size(40.dp * scale).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            contentAlignment = Alignment.Center
        ) {
            when {
                task.type == TaskItem.Type.SWITCH -> {
                    Switch(checked = task.switchState.value, onCheckedChange = null, modifier = Modifier.scale(scale * 0.8f))
                }
                task.icon != null -> {
                    Box(
                        modifier = Modifier
                            .size(40.dp * scale)
                            .clip(RoundedCornerShape(14.dp * scale))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberDrawablePainter(drawable = task.icon),
                            contentDescription = task.text.value,
                            modifier = Modifier.size(24.dp * scale),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp * scale))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(text = task.text.value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp * scale, fontWeight = FontWeight.Bold), maxLines = 1)
            task.subtitle.value?.let {
                Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp * scale), maxLines = 1)
            }
            if (task.type == TaskItem.Type.PROGRESS || task.type == TaskItem.Type.SWITCH) {
                Spacer(modifier = Modifier.height(5.dp * scale))
                ProgressBarWithSheen(progress = progress, scale = scale)
            }
        }
    }
}

@Composable
private fun ProgressBarWithSheen(progress: Float, scale: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "progressSheen")
    val sheenTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "progressSheen"
    )

    Box(
        modifier = Modifier
            .height(4.dp * scale)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .height(4.dp * scale)
                .fillMaxWidth(fraction = progress)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .drawWithContent {
                    drawContent()
                    val gradientWidth = size.width * 0.3f
                    val gradientStart = size.width * (sheenTranslate - 1f)
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent),
                            start = Offset(gradientStart, 0f),
                            end = Offset(gradientStart + gradientWidth, 0f)
                        )
                    )
                }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CollapsedContent(persistentText: String, scale: Float) {
    var currentFps by remember { mutableStateOf(0) }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp * scale)
    ) {
        val textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp * scale, fontWeight = FontWeight.Medium)

        Text(
            text = "LuminaCN B23",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Separator(scale)

        AnimatedContent(
            targetState = currentFps,
            transitionSpec = {
                val easing = FastOutSlowInEasing
                slideInVertically(
                    animationSpec = tween(500, easing = easing),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(
                    animationSpec = tween(500, easing = easing)
                ) with slideOutVertically(
                    animationSpec = tween(500, easing = easing),
                    targetOffsetY = { -it / 2 }
                ) + fadeOut(
                    animationSpec = tween(500, easing = easing)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Separator(scale: Float) {
    Text(
        text = " • ",
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp * scale),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 4.dp * scale)
    )
}

private fun Modifier.glow(pulse: Float, cornerRadius: Float, scale: Float) = this.drawWithContent {
    drawContent()
    val pulseAlpha = (pulse * 255).toInt()
    if (pulseAlpha > 0) {
        val frameworkPaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = (2.dp * scale).toPx() + (pulse * (4.dp * scale).toPx())
            maskFilter = BlurMaskFilter((6.dp * scale).toPx() + (pulse * (8.dp * scale).toPx()), BlurMaskFilter.Blur.NORMAL)
        }
        drawIntoCanvas { canvas ->
            val drawSize = this.size
            val rect = Rect(Offset.Zero, drawSize)
            val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, CornerRadius(cornerRadius))) }
            val colors = listOf(
                Color(0xFF80DEEA),
                Color(0xFFF48FB1),
                Color(0xFFFFF59D),
                Color(0xFF80DEEA)
            ).map { it.toArgb() }.toIntArray()

            val shader = android.graphics.LinearGradient(0f, 0f, drawSize.width, drawSize.height, colors, null, android.graphics.Shader.TileMode.CLAMP)
            frameworkPaint.shader = shader
            frameworkPaint.alpha = pulseAlpha
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), frameworkPaint)
        }
    }
}