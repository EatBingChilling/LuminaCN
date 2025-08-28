package com.phoenix.luminacn.application

import android.app.Application
import android.content.Context
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.shiyi.RenderOverlay
import com.phoenix.luminacn.shiyi.ArrayListOverlay

class AppContext : Application() {

    companion object {
        lateinit var instance: AppContext
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化游戏管理器
        GameManager.initialize(this)
        
        // 初始化悬浮窗管理器
        initializeOverlays()
        
        // 启动渲染悬浮窗
        startRenderOverlay()
    }

    private fun initializeOverlays() {
        // 显示悬浮窗
        OverlayManager.show(this)
        
        // 初始化渲染悬浮窗
        initializeRenderOverlay()
        
        // 初始化ArrayList悬浮窗
        initializeArrayListOverlay()
    }

    private fun initializeRenderOverlay() {
        // 设置当前session给RenderOverlay
        GameManager.currentSession?.let { session ->
            RenderOverlay.setSession(session)
        }
        
        // 启动渲染悬浮窗
        RenderOverlay.setOverlayEnabled(true)
    }

    private fun initializeArrayListOverlay() {
        // 启动ArrayList悬浮窗
        ArrayListOverlay.setOverlayEnabled(true)
        
        // 更新模块列表
        updateModuleList()
    }

    private fun startRenderOverlay() {
        // 确保渲染悬浮窗已启动
        if (!RenderOverlay.isOverlayEnabled()) {
            RenderOverlay.setOverlayEnabled(true)
        }
    }

    private fun updateModuleList() {
        // 从GameManager获取模块列表并更新到ArrayListOverlay
        val moduleList = GameManager.elements
            .filter { it.isEnabled }
            .map { element ->
                ArrayListOverlay.ModuleInfo(
                    name = element.name,
                    category = element.category?.name ?: "Unknown",
                    isEnabled = element.isEnabled,
                    priority = element.priority ?: 0
                )
            }
        
        ArrayListOverlay.setModules(moduleList)
    }

    /**
     * 更新session时调用
     */
    fun updateSession(session: com.phoenix.luminacn.constructors.NetBound?) {
        RenderOverlay.setSession(session)
    }

    /**
     * 更新模块状态时调用
     */
    fun onModuleStateChanged() {
        updateModuleList()
    }

    /**
     * 获取应用上下文
     */
    fun getContext(): Context {
        return this
    }

    override fun onTerminate() {
        super.onTerminate()
        // 清理资源
        OverlayManager.dismiss()
    }
}