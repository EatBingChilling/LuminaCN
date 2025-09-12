package com.phoenix.luminacn.phoenix

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.text.rememberTextMeasurer
import com.hud.test.modules.dynamicisland.DynamicIslandState
import kotlinx.coroutines.CoroutineScope

private const val PREFS_NAME = "dynamic_island_prefs"
private const val KEY_SCALE = "scale"
private const val KEY_Y_POSITION = "y_position"
private const val KEY_PERSISTENT_TEXT = "persistent_text"

class CompatDynamicIslandState(
    private val context: Context,
    scope: CoroutineScope,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 从SharedPreferences读取初始值
    private val initialScale = prefs.getFloat(KEY_SCALE, 1.0f)
    private val initialPersistentText = prefs.getString(KEY_PERSISTENT_TEXT, "LuminaCN") ?: "LuminaCN"
    private val initialYPosition = prefs.getFloat(KEY_Y_POSITION, 0f)
    
    // 创建底层的DynamicIslandState
    private val underlyingState = DynamicIslandState(scope, textMeasurer, initialScale, initialPersistentText)
    
    // 暴露所需的属性
    val scale: Float get() = underlyingState.scale
    val persistentText: String get() = underlyingState.persistentText
    val tasks: List<com.hud.test.modules.dynamicisland.TaskItem> get() = underlyingState.tasks
    val isExpanded: Boolean get() = underlyingState.isExpanded
    
    // Y位置状态（新API中的DynamicIslandState没有这个，所以我们自己管理）
    var yPosition by mutableStateOf(initialYPosition)
        private set
    
    fun updateScope(newScope: CoroutineScope) {
        underlyingState.updateScope(newScope)
    }
    
    fun updateScale(newScale: Float) {
        val clampedScale = newScale.coerceIn(0.5f, 2.0f)
        underlyingState.updateConfig(clampedScale, persistentText)
        prefs.edit().putFloat(KEY_SCALE, clampedScale).apply()
    }
    
    fun updatePersistentText(newText: String) {
        underlyingState.updateConfig(scale, newText)
        prefs.edit().putString(KEY_PERSISTENT_TEXT, newText).apply()
    }
    
    fun updateYPosition(newYPosition: Float) {
        yPosition = newYPosition
        prefs.edit().putFloat(KEY_Y_POSITION, newYPosition).apply()
    }
    
    // 代理方法到底层状态
    fun addSwitch(identifier: String, text: String, state: Boolean) {
        underlyingState.addSwitch(identifier, text, state)
    }
    
    fun addOrUpdateProgress(
        identifier: String,
        text: String,
        subtitle: String?,
        icon: android.graphics.drawable.Drawable?,
        progress: Float?,
        duration: Long?
    ) {
        underlyingState.addOrUpdateProgress(identifier, text, subtitle, icon, progress, duration)
    }
    
    fun addOrUpdateMusic(
        identifier: String,
        text: String,
        subtitle: String,
        albumArt: android.graphics.drawable.Drawable?,
        progressText: String,
        progress: Float,
        isMajorUpdate: Boolean
    ) {
        underlyingState.addOrUpdateMusic(identifier, text, subtitle, albumArt, progressText, progress, isMajorUpdate)
    }
    
    fun removeTask(identifier: String) {
        underlyingState.removeTask(identifier)
    }
    
    fun hide() {
        underlyingState.hide()
    }
    
    fun cancelScope() {
        underlyingState.cancelScope()
    }
    
    // 获取底层状态用于传递给DynamicIslandView
    fun getUnderlyingState(): DynamicIslandState = underlyingState
}

@Composable
fun rememberCompatDynamicIslandState(context: Context): CompatDynamicIslandState {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    
    return remember(context) {
        CompatDynamicIslandState(context, scope, textMeasurer)
    }
}