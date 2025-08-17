package com.project.lumina.client.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt
import com.project.lumina.client.phoenix.DynamicIslandView
import com.project.lumina.client.phoenix.DynamicIslandState
import com.project.lumina.client.phoenix.rememberDynamicIslandState

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    fun performRestore(savedState: Bundle?) { savedStateRegistryController.performRestore(savedState) }
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}

class DynamicIslandService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private var dynamicIslandState: DynamicIslandState? = null
    private lateinit var windowParams: WindowManager.LayoutParams
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lifecycleOwner = ServiceLifecycleOwner()
    
    // ✨ 移除了 _scale，现在由 DynamicIslandState 管理

    //用于控制首次显示的透明度，实现预热
    private var isWarmedUp = mutableStateOf(false)

    companion object {
        const val ACTION_UPDATE_TEXT = "com.project.lumina.client.ACTION_UPDATE_TEXT"
        const val ACTION_UPDATE_Y_OFFSET = "com.project.lumina.client.ACTION_UPDATE_Y_OFFSET"
        const val ACTION_UPDATE_SCALE = "com.project.lumina.client.ACTION_UPDATE_SCALE"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_Y_OFFSET_DP = "extra_y_offset_dp"
        const val EXTRA_SCALE = "extra_scale"
        const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.project.lumina.client.ACTION_SHOW_NOTIFICATION_SWITCH"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_STATE = "extra_module_state"
        const val ACTION_SHOW_OR_UPDATE_PROGRESS = "com.project.lumina.client.ACTION_SHOW_OR_UPDATE_PROGRESS"
        const val EXTRA_IDENTIFIER = "extra_identifier"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_PROGRESS_VALUE = "extra_progress_value"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val isDarkTheme = isSystemInDarkTheme()
                val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { 
                    if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) 
                } else { 
                    if (isDarkTheme) darkColorScheme() else lightColorScheme() 
                }
                
                //动画化透明度，用于预热
                val alpha by animateFloatAsState(targetValue = if (isWarmedUp.value) 1.0f else 0.0f)

                MaterialTheme(colorScheme = colorScheme) {
                    val state = rememberDynamicIslandState()
                    
                    // ✨ 监听状态变化，同步Y位置到WindowManager
                    LaunchedEffect(state.yPosition) {
                        windowParams.y = dpToPx(state.yPosition)
                        windowManager.updateViewLayout(composeView, windowParams)
                    }
                    
                    LaunchedEffect(state) { 
                        this@DynamicIslandService.dynamicIslandState = state 
                    }
                    
                    // ✨ 修复：使用新的API
                    DynamicIslandView(
                        state = state, 
                        useStoredScale = true, // 使用存储的缩放值
                        modifier = Modifier.alpha(alpha) //应用透明度
                    )
                }
            }
        }
        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, 
            PixelFormat.TRANSLUCENT
        ).apply { 
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0 
        }
        windowManager.addView(composeView, windowParams)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //接收到任何指令后，将isWarmedUp设为true，让灵动岛可见
        if (!isWarmedUp.value) {
            isWarmedUp.value = true
        }

        intent ?: return START_STICKY
        when (intent.action) {
            // ✨ 修复：使用新的更新方法
            ACTION_UPDATE_TEXT -> intent.getStringExtra(EXTRA_TEXT)?.let { text -> 
                dynamicIslandState?.updatePersistentText(text) 
            }
            
            // ✨ 修复：使用新的更新方法，自动保存到SharedPreferences
            ACTION_UPDATE_Y_OFFSET -> { 
                val yOffsetDp = intent.getFloatExtra(EXTRA_Y_OFFSET_DP, 0f)
                dynamicIslandState?.updateYPosition(yOffsetDp)
                // 注意：WindowManager的更新现在在LaunchedEffect中自动处理
            }
            
            // ✨ 修复：使用新的更新方法
            ACTION_UPDATE_SCALE -> { 
                val newScale = intent.getFloatExtra(EXTRA_SCALE, 1.0f)
                dynamicIslandState?.updateScale(newScale)
            }
            
            ACTION_SHOW_NOTIFICATION_SWITCH -> intent.getStringExtra(EXTRA_MODULE_NAME)?.let { name -> 
                val state = intent.getBooleanExtra(EXTRA_MODULE_STATE, false)
                dynamicIslandState?.addSwitch(name, state) 
            }
            
            ACTION_SHOW_OR_UPDATE_PROGRESS -> handleShowOrUpdateProgress(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() { 
        super.onDestroy()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        windowManager.removeView(composeView)
        serviceScope.cancel() 
    }
    
    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).roundToInt()
    
    private fun handleShowOrUpdateProgress(intent: Intent) { 
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
        val progress = intent.takeIf { it.hasExtra(EXTRA_PROGRESS_VALUE) }?.getFloatExtra(EXTRA_PROGRESS_VALUE, 0f)
        val duration = if (progress == null && intent.hasExtra(EXTRA_DURATION_MS)) { 
            intent.getLongExtra(EXTRA_DURATION_MS, 5000L) 
        } else { 
            null 
        }
        val iconDrawable = intent.getIntExtra(EXTRA_ICON_RES_ID, -1).takeIf { it != -1 }?.let { resId -> 
            runCatching { ContextCompat.getDrawable(this, resId) }.getOrNull() 
        }
        dynamicIslandState?.addOrUpdateProgress(identifier, title, subtitle, iconDrawable, progress, duration) 
    }
}