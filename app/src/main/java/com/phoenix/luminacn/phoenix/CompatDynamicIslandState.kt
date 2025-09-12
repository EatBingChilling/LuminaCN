package com.phoenix.luminacn.phoenix

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.CoroutineScope

private const val PREFS_NAME = "dynamic_island_prefs"
private const val KEY_SCALE = "scale"
private const val KEY_Y_POSITION = "y_position"
private const val KEY_PERSISTENT_TEXT = "persistent_text"

/**
 * 兼容的灵动岛状态管理器
 * 管理所有灵动岛相关的状态和配置
 */
class CompatDynamicIslandState(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 从SharedPreferences读取初始值
    var scale by mutableStateOf(prefs.getFloat(KEY_SCALE, 1.0f))
        private set
    
    var persistentText by mutableStateOf(prefs.getString(KEY_PERSISTENT_TEXT, "LuminaCN") ?: "LuminaCN")
        private set
    
    var yPosition by mutableStateOf(prefs.getFloat(KEY_Y_POSITION, 0f))
        private set
    
    // 任务列表
    private val _tasks = mutableStateListOf<DynamicIslandTask>()
    val tasks: List<DynamicIslandTask> get() = _tasks
    
    // 是否有任务正在显示
    val isExpanded: Boolean by derivedStateOf { 
        _tasks.any { !it.removing && !it.isVisuallyHidden } 
    }
    
    fun updateScale(newScale: Float) {
        val clampedScale = newScale.coerceIn(0.5f, 2.0f)
        scale = clampedScale
        prefs.edit().putFloat(KEY_SCALE, clampedScale).apply()
    }
    
    fun updatePersistentText(newText: String) {
        persistentText = newText
        prefs.edit().putString(KEY_PERSISTENT_TEXT, newText).apply()
    }
    
    fun updateYPosition(newYPosition: Float) {
        yPosition = newYPosition
        prefs.edit().putFloat(KEY_Y_POSITION, newYPosition).apply()
    }
    
    fun addSwitch(identifier: String, moduleName: String, state: Boolean) {
        val existingIndex = _tasks.indexOfFirst { it.identifier == identifier }
        val mainTitle = "功能开关"
        val subTitle = "$moduleName|已被${if (state) "开启" else "关闭"}"
        
        val task = DynamicIslandTask(
            type = DynamicIslandTask.Type.SWITCH,
            identifier = identifier,
            title = mainTitle,
            subtitle = subTitle,
            switchState = state,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        if (existingIndex != -1) {
            _tasks[existingIndex] = task
        } else {
            _tasks.add(0, task)
        }
    }
    
    fun addOrUpdateProgress(
        identifier: String,
        text: String,
        subtitle: String?,
        icon: android.graphics.drawable.Drawable?,
        progress: Float?,
        duration: Long?
    ) {
        val existingIndex = _tasks.indexOfFirst { it.identifier == identifier }
        
        val task = DynamicIslandTask(
            type = DynamicIslandTask.Type.PROGRESS,
            identifier = identifier,
            title = text,
            subtitle = subtitle,
            icon = icon,
            progress = progress ?: 0f,
            duration = duration ?: 5000L,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        if (existingIndex != -1) {
            _tasks[existingIndex] = task
        } else {
            _tasks.add(0, task)
        }
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
        val existingIndex = _tasks.indexOfFirst { it.identifier == identifier }
        
        val task = DynamicIslandTask(
            type = DynamicIslandTask.Type.MUSIC,
            identifier = identifier,
            title = text,
            subtitle = subtitle,
            icon = albumArt,
            progress = progress,
            progressText = progressText,
            isMajorUpdate = isMajorUpdate,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        if (existingIndex != -1) {
            _tasks[existingIndex] = task
        } else {
            _tasks.add(0, task)
        }
    }
    
    fun removeTask(identifier: String) {
        _tasks.removeAll { it.identifier == identifier }
    }
    
    fun hide() {
        _tasks.clear()
    }
}

/**
 * 灵动岛任务数据类
 */
data class DynamicIslandTask(
    val type: Type,
    val identifier: String,
    val title: String,
    val subtitle: String? = null,
    val switchState: Boolean = false,
    val icon: android.graphics.drawable.Drawable? = null,
    val progress: Float = 0f,
    val progressText: String? = null,
    val duration: Long = 0,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val removing: Boolean = false,
    val isVisuallyHidden: Boolean = false,
    val isMajorUpdate: Boolean = false
) {
    enum class Type { SWITCH, PROGRESS, MUSIC }
}

@Composable
fun rememberCompatDynamicIslandState(context: Context): CompatDynamicIslandState {
    return remember(context) {
        CompatDynamicIslandState(context)
    }
}