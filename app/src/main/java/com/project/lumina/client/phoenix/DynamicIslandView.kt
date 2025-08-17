@file:OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)

package com.project.lumina.client.phoenix

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.drawable.Drawable
import android.view.Choreographer
import android.view.animation.DecelerateInterpolator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.*
import kotlin.math.max

// region Constants
private const val VALUE_PROGRESS_TIMEOUT_MS = 3000L
private const val TIME_PROGRESS_GRACE_PERIOD_MS = 1000L
private val COLLAPSED_HEIGHT = 34.dp
private val EXPANDED_CORNER_RADIUS = 28.dp
private val ITEM_HEIGHT = 60.dp
private val VIEW_PADDING = 12.dp
private val HORIZONTAL_ITEM_PADDING = 20.dp
private val ICON_SIZE = 40.dp
private val ICON_SPACING = 16.dp

// SharedPreferences keys
private const val PREFS_NAME = "dynamic_island_prefs"
private const val KEY_Y_POSITION = "y_position"
private const val KEY_SCALE = "scale"
private const val KEY_PERSISTENT_TEXT = "persistent_text"
// endregion

// region Data Class and Helpers
public data class TaskItem(
    val type: Type,
    val identifier: String,
    val text: String,
    val subtitle: String?,
    val switchState: Boolean = false,
    val icon: Drawable? = null,
    val isTimeBased: Boolean = false,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val isAwaitingData: Boolean = false,
    val removing: Boolean = false,
    val duration: Long = 0,
    val progressJob: Job? = null,
    val displayProgress: Animatable<Float, AnimationVector1D> = Animatable(1.0f)
) {
    public enum class Type { SWITCH, PROGRESS }
}

public fun TimeInterpolator.toEasing(): Easing = Easing { x -> getInterpolation(x) }
// endregion

// region State Holder: DynamicIslandState
@Stable
public class DynamicIslandState(
    private val scope: CoroutineScope,
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // ✨ 持久化的状态
    public var yPosition by mutableStateOf(prefs.getFloat(KEY_Y_POSITION, 100f))
        private set
    
    public var scale by mutableStateOf(prefs.getFloat(KEY_SCALE, 1.0f))
        private set
        
    public var persistentText by mutableStateOf(prefs.getString(KEY_PERSISTENT_TEXT, "Phoen1x") ?: "Phoen1x")
        private set
    
    public val tasks = mutableStateListOf<TaskItem>()
    public val isExpanded by derivedStateOf { tasks.any { !it.removing } }

    init {
        scope.launch {
            while (isActive) {
                updateTasks()
                delay(250)
            }
        }
    }

    // ✨ 公开的更新方法，自动保存到SharedPreferences
    public fun updateYPosition(newY: Float) {
        yPosition = newY
        prefs.edit().putFloat(KEY_Y_POSITION, newY).apply()
    }
    
    public fun updateScale(newScale: Float) {
        scale = newScale.coerceIn(0.5f, 2.0f) // 限制缩放范围
        prefs.edit().putFloat(KEY_SCALE, scale).apply()
    }
    
    public fun updatePersistentText(newText: String) {
        persistentText = newText
        prefs.edit().putString(KEY_PERSISTENT_TEXT, newText).apply()
    }

    public fun addSwitch(moduleName: String, state: Boolean) {
        val subtitle = if (state) "已开启" else "已关闭"
        val duration = 2000L
        val taskIndex = tasks.indexOfFirst { it.identifier == moduleName }

        if (taskIndex != -1) {
            val existingTask = tasks[taskIndex]
            val updatedTask = existingTask.copy(
                text = moduleName,
                subtitle = subtitle,
                switchState = state,
                lastUpdateTime = System.currentTimeMillis(),
                duration = duration,
                removing = false
            )
            tasks[taskIndex] = updatedTask
            startTimeBasedAnimation(updatedTask)
        } else {
            val newTask = TaskItem(
                type = TaskItem.Type.SWITCH,
                identifier = moduleName,
                text = moduleName,
                subtitle = subtitle,
                switchState = state,
                duration = duration,
                isTimeBased = true
            )
            tasks.add(0, newTask)
            startTimeBasedAnimation(newTask)
        }
    }

    public fun addOrUpdateProgress(identifier: String, text: String, subtitle: String?, icon: Drawable?, progress: Float?, duration: Long?) {
        val taskIndex = tasks.indexOfFirst { it.identifier == identifier }
        if (taskIndex != -1) {
            updateProgressInternal(tasks[taskIndex], taskIndex, text, subtitle, progress, duration)
        } else {
            addProgressInternal(identifier, text, subtitle, icon, progress, duration)
        }
    }

    public fun hide() {
        tasks.forEach { it.progressJob?.cancel() }
        tasks.clear()
    }

    private fun updateTasks() {
        if (tasks.isEmpty()) return

        val currentTime = System.currentTimeMillis()
        val updates = mutableMapOf<Int, TaskItem>()

        tasks.toList().forEachIndexed { index, task ->
            if (!task.removing) {
                var updatedTask = task
                val shouldBeRemoved = when {
                    task.isTimeBased -> {
                        if (task.displayProgress.value <= 0.01f && !task.isAwaitingData) {
                            updatedTask = task.copy(isAwaitingData = true, lastUpdateTime = currentTime)
                            updates[index] = updatedTask
                        }
                        updatedTask.isAwaitingData && (currentTime - updatedTask.lastUpdateTime > TIME_PROGRESS_GRACE_PERIOD_MS)
                    }
                    task.type == TaskItem.Type.PROGRESS -> currentTime - task.lastUpdateTime > VALUE_PROGRESS_TIMEOUT_MS
                    else -> false
                }

                if (shouldBeRemoved) {
                    updates[index] = updatedTask.copy(removing = true)
                }
            }
        }

        if (updates.isNotEmpty()) {
            updates.forEach { (index, updatedTask) ->
                if (index < tasks.size) {
                    tasks[index] = updatedTask
                }
            }
        }

        tasks.removeAll { it.removing && !it.displayProgress.isRunning }
    }

    private fun addProgressInternal(identifier: String, text: String, subtitle: String?, icon: Drawable?, progressValue: Float?, duration: Long?) {
        val newTask = TaskItem(
            type = TaskItem.Type.PROGRESS,
            identifier = identifier,
            text = text,
            subtitle = subtitle,
            icon = icon?.mutate()
        )
        if (progressValue != null) {
            val taskWithProgress = newTask.copy(isTimeBased = false)
            scope.launch { taskWithProgress.displayProgress.snapTo(0f) }
            animateProgressTo(taskWithProgress, progressValue)
            tasks.add(0, taskWithProgress)
        } else {
            val timeBasedTask = newTask.copy(isTimeBased = true, duration = duration ?: 5000L)
            startTimeBasedAnimation(timeBasedTask)
            tasks.add(0, timeBasedTask)
        }
    }

    private fun updateProgressInternal(task: TaskItem, index: Int, text: String, subtitle: String?, progressValue: Float?, duration: Long?) {
        var updatedTask = task.copy(
            text = text,
            subtitle = subtitle,
            lastUpdateTime = System.currentTimeMillis(),
            isAwaitingData = false,
            removing = false
        )
        
        if (progressValue != null) {
            updatedTask = updatedTask.copy(isTimeBased = false)
            animateProgressTo(updatedTask, progressValue)
        } else {
            updatedTask = updatedTask.copy(isTimeBased = true, duration = duration ?: 5000L)
            startTimeBasedAnimation(updatedTask)
        }
        tasks[index] = updatedTask
    }

    private fun animateProgressTo(task: TaskItem, newProgressValue: Float) {
        task.progressJob?.cancel()
        val newTask = task.copy(
            progressJob = scope.launch {
                task.displayProgress.animateTo(
                    targetValue = newProgressValue.coerceIn(0f, 1f),
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                )
            }
        )
        val index = tasks.indexOfFirst { it.identifier == task.identifier }
        if (index != -1) tasks[index] = newTask
    }

    private fun startTimeBasedAnimation(task: TaskItem) {
        task.progressJob?.cancel()
        val newTask = task.copy(
            progressJob = scope.launch {
                if (task.displayProgress.value < 1.0f) {
                    task.displayProgress.animateTo(1.0f, tween(500, easing = DecelerateInterpolator().toEasing()))
                }
                task.displayProgress.animateTo(0f, tween(task.duration.toInt(), easing = LinearEasing))
            }
        )
        val index = tasks.indexOfFirst { it.identifier == task.identifier }
        if (index != -1) tasks[index] = newTask
    }
}

@Composable
public fun rememberDynamicIslandState(): DynamicIslandState {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val context = LocalContext.current
    return remember { DynamicIslandState(scope, textMeasurer, density, context) }
}
// endregion

// region Composables
@Composable
public fun DynamicIslandView(
    state: DynamicIslandState,
    modifier: Modifier = Modifier,
    useStoredScale: Boolean = true // 是否使用存储的缩放值
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val actualScale = if (useStoredScale) state.scale else 1.0f
    
    val collapsedHeight = COLLAPSED_HEIGHT * actualScale
    val collapsedCornerRadius = collapsedHeight / 2
    val expandedCornerRadius = EXPANDED_CORNER_RADIUS * actualScale
    val itemHeight = ITEM_HEIGHT * actualScale
    val viewPadding = VIEW_PADDING * actualScale

    // ✨ 实时计算收起状态所需宽度 - 使用存储的persistentText
    val collapsedWidth by remember(state.persistentText, density, actualScale) {
        derivedStateOf {
            val textStyle = TextStyle(fontSize = 13.sp * actualScale, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)

            val lumiWidth = textMeasurer.measure(AnnotatedString("LuminaCN B23"), style = textStyle).size.width
            val fpsWidth = textMeasurer.measure(AnnotatedString("999 FPS"), style = textStyle.copy(fontFamily = FontFamily.Monospace)).size.width
            val persistentWidth = textMeasurer.measure(AnnotatedString(state.persistentText), style = textStyle).size.width
            val separatorWidth = textMeasurer.measure(AnnotatedString(" • "), style = textStyle).size.width * 2
            val paddingWidth = with(density) { (16.dp * 2 * actualScale).toPx() }

            val totalContentWidth = lumiWidth + fpsWidth + persistentWidth + separatorWidth + paddingWidth
            val extraBuffer = with(density) { (32.dp * actualScale).toPx() }

            with(density) { 
                (totalContentWidth + extraBuffer).toDp()
            }
        }
    }

    // ✨ 实时计算展开状态所需宽度
    val expandedWidth by remember(state.tasks, density, actualScale) {
        derivedStateOf {
            if (state.tasks.isEmpty()) {
                280.dp * actualScale
            } else {
                val requiredWidthInPixels = state.tasks.maxOfOrNull { task ->
                    val iconWidthPx = with(density) { 
                        (if (task.type == TaskItem.Type.SWITCH) 64.dp else ICON_SIZE).toPx() * actualScale
                    }
                    val spacingPx = with(density) { ICON_SPACING.toPx() * actualScale }
                    val sidePaddingPx = with(density) { (HORIZONTAL_ITEM_PADDING * 2).toPx() * actualScale }

                    val mainTextWidth = textMeasurer.measure(
                        AnnotatedString(task.text), 
                        style = TextStyle(fontSize = 15.sp * actualScale, fontWeight = FontWeight.Bold)
                    ).size.width.toFloat()

                    val subtitleWidth = task.subtitle?.let { subtitle ->
                        textMeasurer.measure(
                            AnnotatedString(subtitle), 
                            style = TextStyle(fontSize = 12.sp * actualScale)
                        ).size.width.toFloat()
                    } ?: 0f

                    val maxTextWidth = max(mainTextWidth, subtitleWidth)
                    val extraBuffer = with(density) { (24.dp * actualScale).toPx() }

                    sidePaddingPx + iconWidthPx + spacingPx + maxTextWidth + extraBuffer
                } ?: with(density) { (280.dp * actualScale).toPx() }

                with(density) { 
                    requiredWidthInPixels.toDp().coerceAtLeast(280.dp * actualScale)
                }
            }
        }
    }

    val expandedHeight by remember(state.tasks.size, actualScale) { 
        derivedStateOf { 
            (state.tasks.count { !it.removing } * itemHeight.value + viewPadding.value * 2).dp.coerceAtMost(400.dp * actualScale) 
        } 
    }

    val targetWidth = if (state.isExpanded) expandedWidth else collapsedWidth
    val targetHeight = if (state.isExpanded) expandedHeight else collapsedHeight
    val targetCorner = if (state.isExpanded) expandedCornerRadius else collapsedCornerRadius

    val springSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = springSpec, label = "width")
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = springSpec, label = "height")
    val animatedCorner by animateDpAsState(targetValue = targetCorner, animationSpec = springSpec, label = "corner")

    val glowPulse = remember { Animatable(0f) }
    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            glowPulse.snapTo(1f)
            glowPulse.animateTo(0f, animationSpec = tween(1000, easing = LinearEasing))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sheen")
    val sheenTranslate by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing, delayMillis = 500), RepeatMode.Restart),
        label = "sheenTranslate"
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
            .glow(pulse = glowPulse.value, cornerRadius = with(LocalDensity.current) { animatedCorner.toPx() }, scale = actualScale),
        // ✨ 修复：始终居中对齐
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
                ExpandedContent(state, actualScale)
            } else {
                CollapsedContent(state.persistentText, actualScale)
            }
        }
    }
}

@Composable
private fun ExpandedContent(state: DynamicIslandState, scale: Float) {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = (VIEW_PADDING / 2) * scale)) {
        state.tasks.forEachIndexed { index, task ->
            val isVisible = !task.removing
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = index * 120)) +
                        expandVertically(animationSpec = tween(durationMillis = 500, delayMillis = index * 120, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                        slideOutVertically(targetOffsetY = { -it / 2 }) +
                        shrinkVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
            ) {
                TaskItemRow(task = task, scale = scale)
            }
        }
    }
}

@Composable
private fun TaskItemRow(task: TaskItem, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT * scale)
            .padding(horizontal = HORIZONTAL_ITEM_PADDING * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(ICON_SIZE * scale),
            contentAlignment = Alignment.Center
        ) {
            when {
                task.type == TaskItem.Type.SWITCH -> {
                    Switch(checked = task.switchState, onCheckedChange = null, modifier = Modifier.scale(scale * 0.8f))
                }
                task.icon != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp * scale))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberDrawablePainter(drawable = task.icon),
                            contentDescription = task.text,
                            modifier = Modifier.size(24.dp * scale),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(ICON_SPACING * scale))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = task.text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp * scale, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            task.subtitle?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp * scale),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (task.type == TaskItem.Type.PROGRESS || task.type == TaskItem.Type.SWITCH) {
                Spacer(modifier = Modifier.height(5.dp * scale))
                ProgressBarWithSheen(progress = task.displayProgress.value, scale = scale)
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

@Composable
private fun CollapsedContent(persistentText: String, scale: Float) {
    var currentFps by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
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
        val choreographer = Choreographer.getInstance()
        choreographer.postFrameCallback(callback)

        onDispose {
            choreographer.removeFrameCallback(callback)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 16.dp * scale)
    ) {
        val textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp * scale, fontWeight = FontWeight.Medium)

        Text(text = "LuminaCN B23", style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Separator(scale)

        AnimatedContent(
            targetState = currentFps,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) with (slideOutVertically { -it / 2 } + fadeOut())
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
    if (pulseAlpha <= 0) return@drawWithContent

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
// endregion