@file:OptIn(ExperimentalAnimationApi::class, ExperimentalTextApi::class)
package com.phoenix.luminacn.phoenix

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // <<< FIX: Added missing import for 'by' delegate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.phoenix.luminacn.R
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.roundToInt

// --- Constants ---
private const val VALUE_PROGRESS_TIMEOUT_MS = 3000L
private const val TIME_PROGRESS_GRACE_PERIOD_MS = 1000L
private const val MUSIC_VISIBILITY_TIMEOUT_MS = 5000L
private const val SWITCH_DISPLAY_DURATION_MS = 500L // 开关显示0.5秒
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

// --- Data Classes and State ---
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

    private var currentMusicIdentity: String? = null

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
        val duration = SWITCH_DISPLAY_DURATION_MS
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

    // =======================================================================================
    // =================================== BUG FIX HERE ======================================
    // =======================================================================================
    public fun addOrUpdateMusic(
        identifier: String, text: String, subtitle: String, albumArt: Drawable?,
        progressText: String, progress: Float,
        isMajorUpdate: Boolean,
        isSeek: Boolean = false
    ) {
        val taskIndex = tasks.indexOfFirst { it.identifier == identifier }
        val existingTask = if (taskIndex != -1) tasks[taskIndex] else null

        // --- 关键修复逻辑 Part 1: 确定最终要使用的数据 ---
        // 如果是次要更新 (isMajorUpdate = false)，则保留现有的标题、副标题和图标。
        // 只有在主要更新时，才使用新传入的值。这样可以防止进度更新时传入的空字符串覆盖掉正确的信息。
        val finalTitle = if (isMajorUpdate) text else existingTask?.text ?: text
        val finalSubtitle = if (isMajorUpdate) subtitle else existingTask?.subtitle ?: subtitle
        val finalAlbumArt = if (isMajorUpdate) albumArt?.mutate() else existingTask?.icon

        // --- 关键修复逻辑 Part 2: 使用最终确定的数据来判断是否是新歌 ---
        // 【最终修复】创建当前歌曲的稳定标识 (ID)。
        // 我们从 "0:15 / 3:45" 这样的字符串中提取出 "/ 3:45" 这部分，它代表了歌曲总长，是稳定的。
        // 然后结合歌手名(finalSubtitle)，生成一个在歌曲播放期间不会改变的唯一ID。
        val songDurationIdentifier = progressText.substringAfterLast('/', "").trim()
        val newMusicIdentity = "$finalSubtitle - $songDurationIdentifier"

        // 现在，只有当这个稳定的ID改变时，我们才认为这是一首新歌。
        // 并且，新歌的判断只应该在“主要更新”时发生，防止意外重置。
        val isNewSong = isMajorUpdate && (currentMusicIdentity != newMusicIdentity)

        if (isNewSong) {
            // 如果是新歌，更新我们内部存储的ID
            currentMusicIdentity = newMusicIdentity
        }

        var shouldBeHidden = existingTask?.isVisuallyHidden ?: false
        var hideJobToSet = existingTask?.hideJob

        // 只有主要更新才会重置“自动隐藏”的计时器
        if (isMajorUpdate) {
            existingTask?.hideJob?.cancel()
            shouldBeHidden = false
            hideJobToSet = scope.launch {
                delay(MUSIC_VISIBILITY_TIMEOUT_MS)
                val currentIndex = tasks.indexOfFirst { it.identifier == identifier }
                if (currentIndex != -1) {
                    tasks[currentIndex] = tasks[currentIndex].copy(isVisuallyHidden = true)
                }
            }
        }

        val progressAnimatable = existingTask?.displayProgress ?: Animatable(progress)

        val musicTask = (existingTask ?: TaskItem(type = TaskItem.Type.MUSIC, identifier = identifier, text = "", subtitle = null))
            .copy(
                text = finalTitle, // 使用修复后的 finalTitle
                subtitle = finalSubtitle, // 使用修复后的 finalSubtitle
                icon = finalAlbumArt ?: existingTask?.icon, // 使用修复后的 finalAlbumArt, 并提供回退
                progressText = progressText,
                isVisuallyHidden = shouldBeHidden,
                lastUpdateTime = System.currentTimeMillis(),
                removing = false,
                displayProgress = progressAnimatable,
                hideJob = hideJobToSet
            )

        // 只有当确认为新歌且不是用户拖动进度条时，才重置进度动画
        if (isNewSong && !isSeek) {
            scope.launch {
                progressAnimatable.snapTo(0f)
            }
        }

        val animSpec = if (isSeek) fastAnimationSpec else slowAnimationSpec
        animateProgressTo(musicTask, progress, animSpec)

        if (taskIndex != -1) {
            tasks[taskIndex] = musicTask
        } else {
            tasks.add(0, musicTask)
        }
    }
    // =======================================================================================
    // ================================ END OF BUG FIX =======================================
    // =======================================================================================


    public fun removeTask(identifier: String) {
        val taskIndex = tasks.indexOfFirst { it.identifier == identifier }
        if (taskIndex != -1) {
            // 如果移除的是音乐任务，清除音乐ID，以便下次能正确识别新歌
            if (tasks[taskIndex].type == TaskItem.Type.MUSIC) {
                currentMusicIdentity = null
            }
            tasks[taskIndex] = tasks[taskIndex].copy(removing = true)
        }
    }
    
    public fun hide() {
        tasks.forEach { it.progressJob?.cancel(); it.hideJob?.cancel() }
        tasks.clear()
        // 当灵动岛完全隐藏时，清除音乐ID
        currentMusicIdentity = null
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
            animateProgressTo(taskWithProgress, progressValue, slowAnimationSpec)
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
            animateProgressTo(updatedTask, progressValue, slowAnimationSpec)
        } else {
            updatedTask = updatedTask.copy(isTimeBased = true, duration = duration ?: 5000L)
            startTimeBasedAnimation(updatedTask)
        }
        tasks[index] = updatedTask
    }

    private fun animateProgressTo(
        task: TaskItem, newProgressValue: Float, animationSpec: AnimationSpec<Float>
    ) {
        task.progressJob?.cancel()
        val newTask = task.copy(
            progressJob = scope.launch {
                val coercedValue = newProgressValue.coerceIn(0f, 1f)
                if (task.displayProgress.value != coercedValue) {
                    task.displayProgress.animateTo(targetValue = coercedValue, animationSpec = animationSpec)
                }
            }
        )
        val index = tasks.indexOfFirst { it.identifier == task.identifier }
        if (index != -1) {
            tasks[index] = newTask
        }
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
// (The rest of the file remains unchanged from the previous version)

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

    val musicTaskForCollapsedState by derivedStateOf {
        state.tasks.find { it.type == TaskItem.Type.MUSIC && !it.removing }
    }

    val visibleTasks by remember(state.tasks) {
        derivedStateOf {
            val activeSwitchTasks = state.tasks.filter {
                it.type == TaskItem.Type.SWITCH && !it.removing && !it.isVisuallyHidden
            }
            if (activeSwitchTasks.isNotEmpty()) {
                activeSwitchTasks
            } else {
                state.tasks.filter {
                    it.type != TaskItem.Type.SWITCH && !it.removing && !it.isVisuallyHidden
                }
            }
        }
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

    val expandedWidth by derivedStateOf {
        if (visibleTasks.isEmpty()) {
            280.dp * actualScale
        } else {
            with(density) {
                val requiredWidthInPixels = visibleTasks.maxOfOrNull { task ->
                    val sidePaddingPx = (HORIZONTAL_ITEM_PADDING * 2 * actualScale).toPx()
                    val iconWidthPx = (if (task.icon != null || task.type == TaskItem.Type.SWITCH) {
                        if (task.type == TaskItem.Type.SWITCH) 64.dp else ICON_SIZE
                    } else 0.dp).times(actualScale).toPx()
                    val spacingPx = (if (task.icon != null || task.type == TaskItem.Type.SWITCH) ICON_SPACING else 0.dp).times(actualScale).toPx()

                    val titleStyle = TextStyle(fontSize = (15.sp.value * actualScale).sp, fontWeight = FontWeight.Bold)
                    val subtitleStyle = TextStyle(fontSize = (12.sp.value * actualScale).sp)
                    val mainTextWidth = textMeasurer.measure(AnnotatedString(task.text), style = titleStyle).size.width.toFloat()

                    val subtitleWidth = task.subtitle?.let {
                        if (it.contains("|") && task.type == TaskItem.Type.SWITCH) {
                            val parts = it.split("|", limit = 2)
                            val part1Width = textMeasurer.measure(AnnotatedString(parts[0]), style = subtitleStyle).size.width.toFloat()
                            val part2Width = textMeasurer.measure(AnnotatedString(parts[1]), style = subtitleStyle).size.width.toFloat()
                            val spacerWidth = (4.dp * actualScale).toPx()
                            part1Width + spacerWidth + part2Width
                        } else {
                            textMeasurer.measure(AnnotatedString(it), style = subtitleStyle).size.width.toFloat()
                        }
                    } ?: 0f

                    sidePaddingPx + iconWidthPx + spacingPx + max(mainTextWidth, subtitleWidth) + (16.dp * actualScale).toPx()
                } ?: (280.dp * actualScale).toPx()

                maxOf(requiredWidthInPixels.toDp(), 280.dp * actualScale)
            }
        }
    }

    val expandedHeight by derivedStateOf {
        var totalHeight = 0.dp
        visibleTasks.forEach { task ->
            totalHeight += when (task.type) {
                TaskItem.Type.MUSIC -> (ITEM_HEIGHT + PROGRESS_AREA_EXTRA_HEIGHT + 10.dp) * actualScale
                TaskItem.Type.PROGRESS -> (ITEM_HEIGHT + PROGRESS_AREA_EXTRA_HEIGHT) * actualScale
                TaskItem.Type.SWITCH -> ITEM_HEIGHT * actualScale
            }
        }
        (totalHeight + viewPadding * 2).coerceAtMost(400.dp * actualScale)
    }

    val targetHeight = if (state.isExpanded) expandedHeight else collapsedHeight
    val targetCorner = if (state.isExpanded) expandedCornerRadius else collapsedCornerRadius

    val springSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = springSpec, label = "height")
    val animatedCorner by animateDpAsState(targetValue = targetCorner, animationSpec = springSpec, label = "corner")
    val animatedWidth by animateDpAsState(targetValue = expandedWidth, animationSpec = springSpec, label = "width")

    val infiniteTransition = rememberInfiniteTransition(label = "sheen")
    val sheenTranslate by infiniteTransition.animateFloat(initialValue = -1.0f, targetValue = 2.0f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing, delayMillis = 500), repeatMode = RepeatMode.Restart), label = "sheenTranslate")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val glowRoomPx = ((GLOW_BLUR_RADIUS + GLOW_SPREAD_RADIUS) * actualScale).roundToPx()
                    val placeable = measurable.measure(constraints)
                    val widthWithGlow = placeable.width + glowRoomPx * 2
                    val heightWithGlow = placeable.height + glowRoomPx * 2
                    layout(widthWithGlow, heightWithGlow) {
                        placeable.placeRelative(glowRoomPx, glowRoomPx)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(animatedHeight)
                    .softGlow(
                        color = Color.Black.copy(alpha = 0.4f),
                        borderRadius = animatedCorner,
                        blurRadius = GLOW_BLUR_RADIUS * actualScale,
                        spread = GLOW_SPREAD_RADIUS * actualScale
                    )
                    .clip(RoundedCornerShape(animatedCorner))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .drawWithContent {
                        drawContent()
                        val gradientWidth = size.width * 0.5f
                        val gradientStart = size.width * sheenTranslate
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent
                                ),
                                start = Offset(gradientStart - gradientWidth, 0f),
                                end = Offset(gradientStart, size.height)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state.isExpanded,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, 100)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400, 100)) with
                                fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200)))
                            .using(SizeTransform(clip = true, sizeAnimationSpec = { _, _ ->
                                spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
                            }))
                    },
                    label = "content"
                ) { isExpanded ->
                    if (isExpanded) {
                        Box(modifier = Modifier.width(animatedWidth)) {
                            ExpandedContent(visibleTasks, actualScale)
                        }
                    } else {
                        CollapsedContent(state.persistentText, currentFps, musicTaskForCollapsedState, actualScale)
                    }
                }
            }
        }
    }
}

@Composable private fun ExpandedContent(tasks: List<TaskItem>, scale: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = VIEW_PADDING * scale)
    ) {
        tasks.forEach { task ->
            TaskItemRow(task = task, scale = scale)
        }
    }
}

@Composable private fun TaskItemRow(task: TaskItem, scale: Float) {
    val rowHeight = when (task.type) {
        TaskItem.Type.MUSIC -> (ITEM_HEIGHT + PROGRESS_AREA_EXTRA_HEIGHT + 10.dp) * scale
        TaskItem.Type.PROGRESS -> (ITEM_HEIGHT + PROGRESS_AREA_EXTRA_HEIGHT) * scale
        TaskItem.Type.SWITCH -> ITEM_HEIGHT * scale
    }

    Box(modifier = Modifier.height(rowHeight)) {
        when (task.type) {
            TaskItem.Type.MUSIC -> MusicItemRow(task, scale)
            TaskItem.Type.SWITCH -> SwitchItemRow(task, scale)
            TaskItem.Type.PROGRESS -> ProgressItemRow(task, scale)
        }
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
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HORIZONTAL_ITEM_PADDING * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                        ScalableText(text = parts[1], color = Color.White.copy(alpha = 0.7f), baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT * scale)
                .padding(horizontal = HORIZONTAL_ITEM_PADDING * scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        val topPadding = (PROGRESS_AREA_TOTAL_VERTICAL_PADDING / 2) * scale
        val bottomPadding = (PROGRESS_AREA_TOTAL_VERTICAL_PADDING - (PROGRESS_AREA_TOTAL_VERTICAL_PADDING / 2)) * scale

        Spacer(modifier = Modifier.height(topPadding))
        Box(modifier = Modifier.padding(horizontal = HORIZONTAL_ITEM_PADDING * scale)) { ProgressBarWithSheen(progress = task.displayProgress.value, scale = scale) }
        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
private fun AnimatedMusicContent(task: TaskItem, scale: Float) {
    val animationSpec: FiniteAnimationSpec<IntOffset> = spring(stiffness = Spring.StiffnessMedium)
    val transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec, initialOffsetY = { it / 4 }))
            .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec, targetOffsetY = { -it / 4 }))
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HORIZONTAL_ITEM_PADDING / 2 * scale, vertical = VIEW_PADDING / 2 * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = task.icon,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "albumArtAnimation",
            modifier = Modifier.fillMaxHeight().aspectRatio(1f)
        ) { icon ->
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(MUSIC_ART_CORNER_RADIUS * scale)).background(Color.DarkGray)
            ) {
                if (icon != null) {
                    Image(painter = rememberDrawablePainter(drawable = icon), contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, contentDescription = "Music Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize(0.6f).align(Alignment.Center))
                }
            }
        }

        Spacer(modifier = Modifier.width(ICON_SPACING * scale))

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                BoxWithConstraints {
                    val titleStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (15.sp.value * scale).coerceAtLeast(8f).sp,
                        lineHeight = (17.sp.value * scale).coerceAtLeast(10f).sp,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedContent(targetState = task.text, label = "titleAnimation", transitionSpec = transitionSpec) { text ->
                        MarqueeText(
                            text = text,
                            style = titleStyle,
                            color = Color.White,
                            maxWidth = maxWidth
                        )
                    }
                }
                AnimatedContent(targetState = task.subtitle, label = "subtitleAnimation", transitionSpec = transitionSpec) { subtitle ->
                    subtitle?.let {
                        ScalableText(text = it, color = Color.White.copy(alpha = 0.7f), baseSize = 12.sp, baseLineHeight = 14.sp, scale = scale)
                    }
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


@Composable
private fun MusicItemRow(task: TaskItem, scale: Float) {
    AnimatedMusicContent(task = task, scale = scale)
}

@Composable private fun ProgressBarWithSheen(progress: Float, scale: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "progressSheen")
    val sheenTranslate by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "progressSheen")
    val progressHeight = PROGRESS_BAR_HEIGHT * scale

    Box(
        modifier = Modifier
            .height(progressHeight)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .height(progressHeight)
                .fillMaxWidth(fraction = progress.coerceIn(0.001f, 1f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .drawWithContent {
                    drawContent()
                    val gradientWidth = size.width * 0.3f
                    val gradientStart = size.width * (sheenTranslate - 1f)
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent),
                            start = Offset(gradientStart, 0f),
                            end = Offset(gradientStart + gradientWidth, 0f)
                        )
                    )
                }
        )
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

    Row(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = (12 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        BrandModule(iconSize, scale, textStyle)

        if (musicTask != null) {
            Separator(scale)
            MusicModule(musicTask, iconSize, scale, textStyle)
        }

        Separator(scale)
        UserModule(persistentText, iconSize, scale, textStyle)

        Separator(scale)
        FpsModule(currentFps, iconSize, scale, textStyle)
    }
}

@Composable
private fun BrandModule(iconSize: Dp, scale: Float, textStyle: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.cnicon),
            contentDescription = "Brand Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(4.dp * scale))
        Text("LuminaCN", style = textStyle, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun MusicModule(musicTask: TaskItem, iconSize: Dp, scale: Float, textStyle: TextStyle) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = "Music",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(4.dp * scale))
        MarqueeText(
            text = musicTask.text.take(50),
            style = textStyle,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun UserModule(persistentText: String, iconSize: Dp, scale: Float, textStyle: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = "User",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(4.dp * scale))
        Text(
            persistentText,
            style = textStyle,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun FpsModule(currentFps: Int, iconSize: Dp, scale: Float, textStyle: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "FPS",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(iconSize)
        )
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

@Composable
private fun MarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 120.dp
) {
    val textMeasurer = rememberTextMeasurer()

    val textLayoutResult = remember(text, style) {
        textMeasurer.measure(text, style)
    }
    val textWidth = textLayoutResult.size.width

    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clipToBounds()
    ) {
        val containerWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val isOverflowing = textWidth > containerWidth

        val animatedXOffset by if (isOverflowing) {
            val gap = "      "
            val textWithGapLayoutResult = remember(text, style, gap) {
                textMeasurer.measure(text + gap, style)
            }
            val scrollDistancePx = textWithGapLayoutResult.size.width.toFloat()
            val targetValuePx = -scrollDistancePx
            val duration = (scrollDistancePx * 20).toInt().coerceAtLeast(3000)
            val delay = 2000

            val infiniteTransition = rememberInfiniteTransition(label = "marquee")
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = targetValuePx,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "marquee_offset"
            )
        } else {
            remember { mutableStateOf(0f) }
        }

        val textToDraw = if (isOverflowing) "$text      $text" else text

        Text(
            text = textToDraw,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                val layoutWidth = if (isOverflowing) containerWidth.toInt() else placeable.width

                layout(layoutWidth, placeable.height) {
                    placeable.placeRelative(x = animatedXOffset.roundToInt(), y = 0)
                }
            }
        )
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

@Composable
fun rememberDynamicIslandState(): DynamicIslandState {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE) }
    val initialScale = prefs.getFloat("dynamicIslandScale", 0.7f)
    val initialPersistentText = prefs.getString("dynamicIslandUsername", "User") ?: "User"

    return remember {
        DynamicIslandState(scope, textMeasurer, initialScale, initialPersistentText)
    }
}