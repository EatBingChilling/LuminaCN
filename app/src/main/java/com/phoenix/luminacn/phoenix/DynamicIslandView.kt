@file:OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
package com.phoenix.luminacn.phoenix

import android.animation.TimeInterpolator
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.*
import kotlin.math.max

// --- Constants (Unchanged) ---
private const val VALUE_PROGRESS_TIMEOUT_MS = 3000L
private const val TIME_PROGRESS_GRACE_PERIOD_MS = 1000L
private const val MUSIC_VISIBILITY_TIMEOUT_MS = 5000L
private val COLLAPSED_HEIGHT = 36.dp
private val EXPANDED_CORNER_RADIUS = 28.dp
private val ITEM_HEIGHT = 52.dp
private val VIEW_PADDING = 12.dp
private val HORIZONTAL_ITEM_PADDING = 24.dp
private val ICON_SIZE = 40.dp
private val ICON_SPACING = 12.dp
private val PROGRESS_BAR_HEIGHT = 8.dp
private val PROGRESS_AREA_TOTAL_VERTICAL_PADDING = 12.dp
private val PROGRESS_AREA_EXTRA_HEIGHT = PROGRESS_BAR_HEIGHT + PROGRESS_AREA_TOTAL_VERTICAL_PADDING
private val GLOW_BLUR_RADIUS = 12.dp
private val GLOW_SPREAD_RADIUS = 4.dp
private val MUSIC_ART_CORNER_RADIUS = 15.dp

// --- Data Classes and State (With Animation Fixes) ---
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
    val displayProgress: Animatable<Float, AnimationVector1D> = Animatable(1.0f),
    val progressText: String? = null,
    val isVisuallyHidden: Boolean = false,
    val hideJob: Job? = null
) {
    public enum class Type { SWITCH, PROGRESS, MUSIC }
}

public fun TimeInterpolator.toEasing(): Easing = Easing { x -> getInterpolation(x) }

@Stable
public class DynamicIslandState(private var scope: CoroutineScope, private val textMeasurer: TextMeasurer, initialScale: Float, initialPersistentText: String) {
    var scale by mutableStateOf(initialScale)
    var persistentText by mutableStateOf(initialPersistentText)
    val tasks = mutableStateListOf<TaskItem>()
    val isExpanded by derivedStateOf { tasks.any { !it.removing && !it.isVisuallyHidden } }
    private var updateJob: Job? = null

    private val slowAnimationSpec: AnimationSpec<Float> = tween(800, easing = FastOutSlowInEasing)
    private val fastAnimationSpec: AnimationSpec<Float> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    init { startUpdateLoop() }

    fun updateScope(newScope: CoroutineScope) {
        if (scope != newScope) {
            updateJob?.cancel()
            scope = newScope
            startUpdateLoop()
        }
    }

    private fun startUpdateLoop() {
        updateJob = scope.launch {
            while (isActive) {
                updateTasks()
                delay(250)
            }
        }
    }

    fun updateConfig(newScale: Float, newText: String) {
        this.scale = newScale.coerceIn(0.5f, 2.0f)
        this.persistentText = newText
    }

    public fun addSwitch(identifier: String, text: String, state: Boolean) {
        val mainTitle = "功能开关"
        val subTitle = "$text|已被${if (state) "开启" else "关闭"}"
        val duration = 1500L
        val taskIndex = tasks.indexOfFirst { it.identifier == identifier }

        if (taskIndex != -1) {
            val updatedTask = tasks[taskIndex].copy(text = mainTitle, subtitle = subTitle, switchState = state, lastUpdateTime = System.currentTimeMillis(), duration = duration, removing = false, isTimeBased = true)
            tasks[taskIndex] = updatedTask
            startTimeBasedAnimation(updatedTask)
        } else {
            val newTask = TaskItem(type = TaskItem.Type.SWITCH, identifier = identifier, text = mainTitle, subtitle = subTitle, switchState = state, duration = duration, isTimeBased = true)
            startTimeBasedAnimation(newTask)
            tasks.add(0, newTask)
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

    public fun addOrUpdateMusic(
        identifier: String, text: String, subtitle: String, albumArt: Drawable?,
        progressText: String, progress: Float, isMajorUpdate: Boolean
    ) {
        val taskIndex = tasks.indexOfFirst { it.identifier == identifier }
        val existingTask = if (taskIndex != -1) tasks[taskIndex] else null

        if (isMajorUpdate) {
            existingTask?.hideJob?.cancel()
        }

        val progressAnimatable = existingTask?.displayProgress ?: Animatable(progress)
        val shouldBeHidden = if (isMajorUpdate) false else existingTask?.isVisuallyHidden ?: false
        val musicTask = (existingTask ?: TaskItem(type = TaskItem.Type.MUSIC, identifier = identifier, text = "", subtitle = null))
            .copy(
                text = text,
                subtitle = subtitle,
                icon = albumArt?.mutate() ?: existingTask?.icon,
                progressText = progressText,
                isVisuallyHidden = shouldBeHidden,
                lastUpdateTime = System.currentTimeMillis(),
                removing = false,
                displayProgress = progressAnimatable
            )

        var finalTask = musicTask
        if (isMajorUpdate) {
            val hideJob = scope.launch {
                delay(MUSIC_VISIBILITY_TIMEOUT_MS)
                val currentIndex = tasks.indexOfFirst { it.identifier == identifier }
                if (currentIndex != -1) {
                    tasks[currentIndex] = tasks[currentIndex].copy(isVisuallyHidden = true)
                }
            }
            finalTask = musicTask.copy(hideJob = hideJob)
        }

        val animSpec = if (isMajorUpdate) slowAnimationSpec else fastAnimationSpec
        animateProgressTo(finalTask, progress, animationSpec = animSpec, instant = existingTask == null)

        if (taskIndex != -1) {
            tasks[taskIndex] = finalTask
        } else {
            tasks.add(0, finalTask)
        }
    }

    public fun removeTask(identifier: String) {
        val taskIndex = tasks.indexOfFirst { it.identifier == identifier }
        if (taskIndex != -1) {
            tasks[taskIndex] = tasks[taskIndex].copy(removing = true)
        }
    }

    public fun hide() {
        tasks.forEach { it.progressJob?.cancel(); it.hideJob?.cancel() }
        tasks.clear()
    }

    fun cancelScope() {
        updateJob?.cancel()
        scope.cancel()
    }

    private fun updateTasks() {
        if (tasks.isEmpty()) return
        val currentTime = System.currentTimeMillis()
        val updates = mutableMapOf<Int, TaskItem>()
        tasks.toList().forEachIndexed { index, task ->
            if (task.type == TaskItem.Type.MUSIC) return@forEachIndexed

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
            updates.forEach { (index, updatedTask) -> if (index < tasks.size) { tasks[index] = updatedTask } }
        }
        tasks.removeAll { it.removing && !it.displayProgress.isRunning }
    }

    private fun addProgressInternal(identifier: String, text: String, subtitle: String?, icon: Drawable?, progressValue: Float?, duration: Long?) {
        val newTask = TaskItem(type = TaskItem.Type.PROGRESS, identifier = identifier, text = text, subtitle = subtitle, icon = icon?.mutate())
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
        var updatedTask = task.copy(text = text, subtitle = subtitle, lastUpdateTime = System.currentTimeMillis(), isAwaitingData = false, removing = false)
        if (progressValue != null) {
            updatedTask = updatedTask.copy(isTimeBased = false)
            animateProgressTo(updatedTask, progressValue)
        } else {
            updatedTask = updatedTask.copy(isTimeBased = true, duration = duration ?: 5000L)
            startTimeBasedAnimation(updatedTask)
        }
        tasks[index] = updatedTask
    }

    private fun animateProgressTo(
        task: TaskItem,
        newProgressValue: Float,
        animationSpec: AnimationSpec<Float> = slowAnimationSpec,
        instant: Boolean = false
    ) {
        task.progressJob?.cancel()
        val newTask = task.copy(
            progressJob = scope.launch {
                val coercedValue = newProgressValue.coerceIn(0f, 1f)
                if (instant || task.displayProgress.value == coercedValue) {
                    task.displayProgress.snapTo(coercedValue)
                } else {
                    task.displayProgress.animateTo(
                        targetValue = coercedValue,
                        animationSpec = animationSpec
                    )
                }
            }
        )
        val index = tasks.indexOfFirst { it.identifier == task.identifier }; if (index != -1) tasks[index] = newTask
    }

    private fun startTimeBasedAnimation(task: TaskItem) {
        task.progressJob?.cancel()
        val newTask = task.copy(progressJob = scope.launch {
            if (task.displayProgress.value < 1.0f) {
                task.displayProgress.animateTo(1.0f, tween(500, easing = DecelerateInterpolator().toEasing()))
            }; task.displayProgress.animateTo(0f, tween(task.duration.toInt(), easing = LinearEasing))
        })
        val index = tasks.indexOfFirst { it.identifier == task.identifier }; if (index != -1) tasks[index] = newTask
    }
}


// --- Composable UI ---

fun Modifier.softGlow(color: Color, borderRadius: Dp, blurRadius: Dp, spread: Dp) = this.drawBehind {
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    if (blurRadius > 0.dp) {
        frameworkPaint.maskFilter = (BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL))
    }
    frameworkPaint.color = color.toArgb()
    val spreadPx = spread.toPx()
    val left = -spreadPx
    val top = -spreadPx
    val right = size.width + spreadPx
    val bottom = size.height + spreadPx
    drawIntoCanvas {
        it.drawRoundRect(left, top, right, bottom, borderRadius.toPx(), borderRadius.toPx(), paint)
    }
}

@Composable
public fun DynamicIslandView(state: DynamicIslandState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val actualScale = state.scale.coerceAtLeast(0.6f)
    var currentFps by remember { mutableStateOf(0) }
    val musicTask by remember {
        derivedStateOf { state.tasks.find { it.type == TaskItem.Type.MUSIC && !it.removing } }
    }
    val visibleTasks by remember {
        derivedStateOf { state.tasks.filter { !it.removing && !it.isVisuallyHidden } }
    }

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
        onDispose { choreographer.removeFrameCallback(callback) }
    }

    val collapsedHeight = COLLAPSED_HEIGHT * actualScale
    val collapsedCornerRadius = collapsedHeight / 2
    val expandedCornerRadius = EXPANDED_CORNER_RADIUS * actualScale
    val itemHeight = ITEM_HEIGHT * actualScale
    val viewPadding = VIEW_PADDING * actualScale

    val collapsedWidth by derivedStateOf {
        with(density) {
            val textStyle = TextStyle(fontSize = (13.sp.value * actualScale).coerceAtLeast(9f).sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
            val iconSize = 16.dp * actualScale
            val iconSpacing = 4.dp * actualScale
            val separatorHorizontalPadding = 4.dp * actualScale
            val outerHorizontalPadding = 16.dp * actualScale

            fun measure(text: String) = textMeasurer.measure(AnnotatedString(text), style = textStyle).size.width.toDp()
            val separatorWidth = measure(" • ") + separatorHorizontalPadding * 2

            // 计算必须显示的固定部分宽度
            val kitasanWidth = iconSize + iconSpacing + measure("Kitasan")
            val persistentWidth = iconSize + iconSpacing + measure(state.persistentText)
            val fpsWidth = iconSize + iconSpacing + measure("999 FPS") // 使用最大可能的FPS宽度
            
            // 基础宽度：固定部分 + 分隔符 + 边距
            val baseWidth = kitasanWidth + separatorWidth + 
                           persistentWidth + separatorWidth + 
                           fpsWidth + outerHorizontalPadding * 2

            val totalWidth = if (musicTask != null) {
                // 音乐部分：图标 + 间距 + 最多12个字符的文本
                val musicIconWidth = iconSize + iconSpacing
                val maxMusicText = "A".repeat(12) // 假设最多12个字符
                val musicTextWidth = measure(maxMusicText)
                val musicWidth = musicIconWidth + musicTextWidth + separatorWidth
                
                baseWidth + musicWidth
            } else {
                baseWidth
            }
            
            // Add extra padding to prevent text cutoff
            totalWidth + (8.dp * actualScale)
        }
    }

    val expandedWidth by derivedStateOf {
        if (visibleTasks.isEmpty()) {
            280.dp * actualScale
        } else {
            with(density) {
                val requiredWidthInPixels = visibleTasks.maxOfOrNull { task ->
                    val iconWidthPx = (if (task.type == TaskItem.Type.SWITCH || task.icon != null) if (task.type == TaskItem.Type.SWITCH) 64.dp else ICON_SIZE else 0.dp).toPx() * actualScale
                    val spacingPx = (if (task.type == TaskItem.Type.SWITCH || task.icon != null) ICON_SPACING else 0.dp).toPx() * actualScale
                    val sidePaddingPx = (HORIZONTAL_ITEM_PADDING * 2).toPx() * actualScale
                    val mainTextWidth = textMeasurer.measure(AnnotatedString(task.text), style = TextStyle(fontSize = (15.sp.value * actualScale).sp, fontWeight = FontWeight.Bold)).size.width.toFloat()
                    val subtitleWidth = task.subtitle?.let { textMeasurer.measure(AnnotatedString(it.replace("|", " ")), style = TextStyle(fontSize = (12.sp.value * actualScale).sp)).size.width.toFloat() } ?: 0f
                    sidePaddingPx + iconWidthPx + spacingPx + max(mainTextWidth, subtitleWidth) + (16.dp * actualScale).toPx()
                } ?: (280.dp * actualScale).toPx()
                maxOf(requiredWidthInPixels.toDp(), 280.dp * actualScale)
            }
        }
    }
    val expandedHeight by derivedStateOf {
        var totalHeight = 0.dp
        visibleTasks.forEach { task ->
            totalHeight += itemHeight
            if (task.type == TaskItem.Type.PROGRESS) totalHeight += PROGRESS_AREA_EXTRA_HEIGHT * actualScale
            if (task.type == TaskItem.Type.MUSIC) totalHeight += (PROGRESS_AREA_EXTRA_HEIGHT + 10.dp) * actualScale
        }
        (totalHeight + viewPadding * 2).coerceAtMost(400.dp * actualScale)
    }

    val targetWidth = if (state.isExpanded) expandedWidth else collapsedWidth
    val targetHeight = if (state.isExpanded) expandedHeight else collapsedHeight
    val targetCorner = if (state.isExpanded) expandedCornerRadius else collapsedCornerRadius

    val springSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = springSpec, label = "width")
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = springSpec, label = "height")
    val animatedCorner by animateDpAsState(targetValue = targetCorner, animationSpec = springSpec, label = "corner")

    val infiniteTransition = rememberInfiniteTransition(label = "sheen")
    val sheenTranslate by infiniteTransition.animateFloat(initialValue = -1.0f, targetValue = 2.0f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing, delayMillis = 500), repeatMode = RepeatMode.Restart), label = "sheenTranslate")

    Box(
        modifier = modifier.layout { measurable, constraints ->
            val widthPx = animatedWidth.roundToPx()
            val heightPx = animatedHeight.roundToPx()
            val finalWidthPx = widthPx.coerceAtMost(constraints.maxWidth)
            // Fix 1: Scale the glow room calculation
            val glowRoomPx = ((GLOW_BLUR_RADIUS + GLOW_SPREAD_RADIUS) * actualScale).roundToPx()
            val placeable = measurable.measure(Constraints.fixed(finalWidthPx, heightPx))
            val x = (constraints.maxWidth - finalWidthPx) / 2
            val y = glowRoomPx
            layout(constraints.maxWidth, heightPx + glowRoomPx * 2) { placeable.placeRelative(x, y) }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .softGlow(color = Color.Black.copy(alpha = 0.4f), borderRadius = animatedCorner, blurRadius = GLOW_BLUR_RADIUS * actualScale, spread = GLOW_SPREAD_RADIUS * actualScale)
                .clip(RoundedCornerShape(animatedCorner))
                .background(Color.Black.copy(alpha = 0.75f))
                .drawWithContent {
                    drawContent()
                    val gradientWidth = size.width * 0.5f
                    val gradientStart = size.width * sheenTranslate
                    drawRect(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent), start = Offset(gradientStart - gradientWidth, 0f), end = Offset(gradientStart, size.height)))
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = state.isExpanded,
                transitionSpec = { fadeIn(animationSpec = tween(400, 100)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400, 100)) with fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200)) },
                label = "content"
            ) { isExpanded ->
                if (isExpanded) {
                    ExpandedContent(visibleTasks, actualScale)
                } else {
                    CollapsedContent(state.persistentText, currentFps, musicTask, actualScale)
                }
            }
        }
    }
}

@Composable private fun ExpandedContent(tasks: List<TaskItem>, scale: Float) {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = VIEW_PADDING * scale)) {
        tasks.forEachIndexed { index, task ->
            AnimatedVisibility(
                visible = true, // Already filtered, so always visible within this scope
                enter = fadeIn(animationSpec = tween(500, index * 120)) + expandVertically(animationSpec = tween(500, index * 120, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { -it / 2 }) + shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing))
            ) { TaskItemRow(task = task, scale = scale) }
        }
    }
}

@Composable private fun TaskItemRow(task: TaskItem, scale: Float) {
    when (task.type) {
        TaskItem.Type.MUSIC -> MusicItemRow(task, scale)
        TaskItem.Type.SWITCH -> SwitchItemRow(task, scale)
        TaskItem.Type.PROGRESS -> ProgressItemRow(task, scale)
    }
}

@Composable
private fun ScalableText(text: String, color: Color, baseSize: TextUnit, baseLineHeight: TextUnit, scale: Float, fontWeight: FontWeight? = null, maxLines: Int = 1, overflow: TextOverflow = TextOverflow.Ellipsis) {
    val finalSize = (baseSize.value * scale).coerceAtLeast(8f).sp
    val finalLineHeight = (baseLineHeight.value * scale).coerceAtLeast(10f).sp
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = finalSize,
            lineHeight = finalLineHeight,
            fontWeight = fontWeight
        ),
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
private fun SwitchItemRow(task: TaskItem, scale: Float) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = ITEM_HEIGHT * scale).padding(horizontal = HORIZONTAL_ITEM_PADDING * scale), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.sizeIn(minWidth = ICON_SIZE * scale), contentAlignment = Alignment.Center) {
            Switch(checked = task.switchState, onCheckedChange = null, modifier = Modifier.scale(scale * 1.1f))
        }
        Spacer(modifier = Modifier.width(ICON_SPACING * scale))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            ScalableText(text = task.text, color = Color.White, baseSize = 15.sp, baseLineHeight = 17.sp, scale = scale, fontWeight = FontWeight.Bold)
            task.subtitle?.let { subtitle ->
                if (subtitle.contains("|")) {
                    val parts = subtitle.split("|", limit = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScalableText(text = parts[0], color = MaterialTheme.colorScheme.primary, baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp * scale))
                        ScalableText(text = parts[1], color = Color.White.copy(alpha = 0.7f), baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale, fontWeight = FontWeight.Medium)
                    }
                } else {
                    ScalableText(text = subtitle, color = Color.White.copy(alpha = 0.7f), baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale)
                }
            }
        }
    }
}

@Composable
private fun ProgressItemRow(task: TaskItem, scale: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = ITEM_HEIGHT * scale).padding(horizontal = HORIZONTAL_ITEM_PADDING * scale), verticalAlignment = Alignment.CenterVertically) {
            task.icon?.let {
                Box(modifier = Modifier.size(ICON_SIZE * scale).clip(RoundedCornerShape(14.dp * scale)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Image(painter = rememberDrawablePainter(drawable = it), contentDescription = task.text, modifier = Modifier.size(24.dp * scale), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer))
                }
                Spacer(modifier = Modifier.width(ICON_SPACING * scale))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                ScalableText(text = task.text, color = Color.White, baseSize = 15.sp, baseLineHeight = 17.sp, scale = scale, fontWeight = FontWeight.Bold)
                task.subtitle?.let {
                    ScalableText(text = it, color = Color.White.copy(alpha = 0.7f), baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale)
                }
            }
        }
        val topPadding = PROGRESS_AREA_TOTAL_VERTICAL_PADDING / 2; val bottomPadding = PROGRESS_AREA_TOTAL_VERTICAL_PADDING - topPadding
        Spacer(modifier = Modifier.height(topPadding * scale))
        Box(modifier = Modifier.padding(horizontal = HORIZONTAL_ITEM_PADDING * scale)) { ProgressBarWithSheen(progress = task.displayProgress.value, scale = scale) }
        Spacer(modifier = Modifier.height(bottomPadding * scale))
    }
}

@Composable
private fun MusicItemRow(task: TaskItem, scale: Float) {
    val totalMinHeight = (ITEM_HEIGHT + PROGRESS_AREA_EXTRA_HEIGHT + 10.dp) * scale
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = totalMinHeight).padding(horizontal = HORIZONTAL_ITEM_PADDING / 2 * scale, vertical = VIEW_PADDING / 2 * scale), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.height(totalMinHeight).aspectRatio(1f).clip(RoundedCornerShape(MUSIC_ART_CORNER_RADIUS * scale)).background(Color.DarkGray)) {
            if (task.icon != null) {
                Image(painter = rememberDrawablePainter(drawable = task.icon), contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = "Music Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize(0.6f).align(Alignment.Center))
            }
        }
        Spacer(modifier = Modifier.width(ICON_SPACING * scale))
        Column(modifier = Modifier.weight(1f).height(totalMinHeight), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                ScalableText(text = task.text, color = Color.White, baseSize = 15.sp, baseLineHeight = 17.sp, scale = scale, fontWeight = FontWeight.Bold)
                task.subtitle?.let {
                    ScalableText(text = it, color = Color.White.copy(alpha = 0.7f), baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale)
                }
            }
            Column {
                task.progressText?.let {
                    val fontSize = (11.sp.value * scale).coerceAtLeast(8f).sp
                    Text(text = it, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize), maxLines = 1, modifier = Modifier.padding(bottom = 4.dp * scale))
                }
                ProgressBarWithSheen(progress = task.displayProgress.value, scale = scale)
            }
        }
    }
}

@Composable private fun ProgressBarWithSheen(progress: Float, scale: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "progressSheen")
    val sheenTranslate by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "progressSheen")
    val progressHeight = PROGRESS_BAR_HEIGHT * scale
    Box(modifier = Modifier.height(progressHeight).fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))) {
        Box(modifier = Modifier.height(progressHeight).fillMaxWidth(fraction = progress.coerceIn(0.001f, 1f)).clip(CircleShape).background(MaterialTheme.colorScheme.primary).drawWithContent {
            drawContent()
            val gradientWidth = size.width * 0.3f
            val gradientStart = size.width * (sheenTranslate - 1f)
            drawRect(brush = Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent), start = Offset(gradientStart, 0f), end = Offset(gradientStart + gradientWidth, 0f)))
        })
    }
}

@Composable 
private fun CollapsedContent(
    persistentText: String, 
    currentFps: Int, 
    musicTask: TaskItem?, 
    scale: Float
) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = (13.sp.value * scale).coerceAtLeast(9f).sp,
        lineHeight = (15.sp.value * scale).coerceAtLeast(11f).sp,
        fontWeight = FontWeight.Medium
    )
    val iconSize = 16.dp * scale

    // 简化的音乐文本截断逻辑
    val truncatedMusicText = remember(musicTask?.text) {
        musicTask?.let { task ->
            // 简单截断：最多12个字符
            if (task.text.length <= 12) {
                task.text
            } else {
                // 尝试在空格处截断
                val words = task.text.split(" ")
                var result = ""
                for (word in words) {
                    val potential = if (result.isEmpty()) word else "$result $word"
                    if (potential.length <= 11) { // 留1个字符给省略号
                        result = potential
                    } else {
                        break
                    }
                }
                
                if (result.isEmpty()) {
                    // 如果没有合适的单词边界，直接按字符截断
                    "${task.text.take(11)}…"
                } else if (result.length < task.text.length) {
                    "$result…"
                } else {
                    result
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp * scale), 
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = musicTask != null, 
            enter = fadeIn() + expandHorizontally(), 
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(4.dp * scale))
                Text(
                    text = truncatedMusicText ?: "", 
                    style = textStyle, 
                    color = MaterialTheme.colorScheme.primary, 
                    maxLines = 1, 
                    softWrap = false
                )
                Separator(scale)
            }
        }
        
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(4.dp * scale))
        Text("Kitasan", style = textStyle, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
        Separator(scale)
        
        Icon(Icons.Default.AccountCircle, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(4.dp * scale))
        Text(persistentText, style = textStyle, color = Color.White.copy(alpha = 0.7f), maxLines = 1, softWrap = false)
        Separator(scale)
        
        Icon(Icons.Default.Settings, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(4.dp * scale))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = currentFps, 
                transitionSpec = { 
                    (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } + fadeIn()) with 
                    (slideOutVertically(animationSpec = tween(200)) { -it } + fadeOut(animationSpec = tween(200))) 
                }, 
                label = "fps_animation"
            ) { fpsValue -> 
                Text(
                    text = fpsValue.toString(), 
                    style = textStyle, 
                    color = Color.White.copy(alpha = 0.7f), 
                    maxLines = 1, 
                    softWrap = false
                ) 
            }
            Text(
                text = " FPS", 
                style = textStyle, 
                color = Color.White.copy(alpha = 0.7f), 
                maxLines = 1, 
                softWrap = false
            )
        }
    }
}

@Composable private fun Separator(scale: Float) {
    Text(
        " • ", 
        style = MaterialTheme.typography.labelLarge.copy(fontSize = (13.sp.value * scale).sp), 
        color = Color.White.copy(alpha = 0.4f), 
        modifier = Modifier.padding(horizontal = 4.dp * scale)
    )
}




