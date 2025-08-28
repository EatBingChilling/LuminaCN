package com.phoenix.luminacn.application

import android.app.Application
import android.content.Context
import android.util.Log
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.shiyi.RenderOverlay
import com.phoenix.luminacn.shiyi.ArrayListOverlay
import com.phoenix.luminacn.ui.theme.ThemeManager

class AppContext : Application() {

    companion object {
        private const val TAG = "AppContext"
        
        lateinit var instance: AppContext
            private set
    }

    // 添加主题管理器
    lateinit var themeManager: ThemeManager
        private set

    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            initializeCore()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppContext", e)
        }
    }

    private fun initializeCore() {
        // 1. 首先初始化主题管理器
        initializeThemeManager()
        
        // 2. GameManager 是 object，不需要调用 initialize
        // 它会在第一次访问时自动初始化
        
        // 3. 延迟初始化悬浮窗
        postDelayedInitialization()
        
        isInitialized = true
        Log.d(TAG, "AppContext initialized successfully")
    }

    private fun initializeThemeManager() {
        try {
            themeManager = ThemeManager(this)
            Log.d(TAG, "ThemeManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ThemeManager", e)
            // 如果主题管理器初始化失败，创建一个基本的默认实例
            themeManager = createFallbackThemeManager()
        }
    }

    private fun createFallbackThemeManager(): ThemeManager {
        // 尝试创建一个基本的 ThemeManager，如果失败就抛出异常
        return try {
            ThemeManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "Even fallback ThemeManager failed, using mock", e)
            // 如果实在无法创建，返回一个模拟对象
            createMockThemeManager()
        }
    }

    private fun createMockThemeManager(): ThemeManager {
        // 创建一个应用上下文作为fallback，但这通常不会执行到
        return ThemeManager(applicationContext)
    }

    private fun postDelayedInitialization() {
        android.os.Handler(mainLooper).postDelayed({
            try {
                initializeOverlays()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize overlays", e)
            }
        }, 500)
    }

    private fun initializeOverlays() {
        try {
            OverlayManager.show(this)
            initializeRenderOverlay()
            initializeArrayListOverlay()
            Log.d(TAG, "Overlays initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize overlays", e)
        }
    }

    private fun initializeRenderOverlay() {
        try {
            // 使用 netBound 替代 currentSession
            GameManager.netBound?.let { session ->
                RenderOverlay.setSession(session)
                Log.d(TAG, "RenderOverlay session set")
            } ?: run {
                Log.w(TAG, "GameManager netBound not available yet")
            }
            
            RenderOverlay.setOverlayEnabled(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RenderOverlay", e)
        }
    }

    private fun initializeArrayListOverlay() {
        try {
            ArrayListOverlay.setOverlayEnabled(true)
            updateModuleListSafely()
            Log.d(TAG, "ArrayListOverlay initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ArrayListOverlay", e)
        }
    }

    private fun updateModuleListSafely() {
        try {
            val elements = GameManager.elements
            if (elements.isNotEmpty()) {
                val moduleList = elements
                    .filter { element -> element.isEnabled }
                    .map { element ->
                        ArrayListOverlay.ModuleInfo(
                            name = element.name,
                            category = element.category?.name ?: "Unknown",
                            isEnabled = element.isEnabled,
                            priority = 0  // 使用默认值，因为 Element 可能没有 priority 属性
                        )
                    }
                
                ArrayListOverlay.setModules(moduleList)
                Log.d(TAG, "Module list updated: ${moduleList.size} modules")
            } else {
                Log.w(TAG, "No modules available to update")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update module list", e)
        }
    }

    /**
     * 更新session时调用 - 使用 netBound 替代 currentSession
     */
    fun updateSession(session: com.phoenix.luminacn.constructors.NetBound?) {
        try {
            RenderOverlay.setSession(session)
            Log.d(TAG, "Session updated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update session", e)
        }
    }

    /**
     * 更新模块状态时调用
     */
    fun onModuleStateChanged() {
        if (isInitialized) {
            updateModuleListSafely()
        }
    }

    /**
     * 获取应用上下文
     */
    fun getContext(): Context = this

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * 清理资源的方法
     */
    fun cleanup() {
        try {
            OverlayManager.dismiss()
            Log.d(TAG, "Resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup resources", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "Memory trim requested: level $level")
    }
}