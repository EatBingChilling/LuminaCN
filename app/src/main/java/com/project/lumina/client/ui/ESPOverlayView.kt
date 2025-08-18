package com.project.lumina.client.ui

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import com.project.lumina.client.game.module.ModuleManager
import com.project.lumina.client.game.module.impl.visual.EspElement

class ESPOverlayView(context: Context) : View(context) {

    companion object {
        // 单例模式：持有当前 View 的静态实例
        @Volatile
        var instance: ESPOverlayView? = null
            private set // 只允许内部设置
    }

    init {
        // 在创建时设置静态实例
        instance = this
        isFocusable = false
        isClickable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val espModule = ModuleManager.getModule(EspElement::class.java)

        if (espModule != null && espModule.isEnabled) {
            try {
                // 模块的 onRender2D 方法现在被动地被调用
                espModule.onRender2D(canvas)
            } catch (e: Exception) {
                Log.e("ESPOverlayView", "Error during ESP rendering", e)
            }
        }
        // 注意：我们移除了这里的 invalidate() 调用。重绘将由模块在需要时触发。
    }

    /**
     * 当 View 从窗口分离时（例如 Service 停止时），清理静态实例以防内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (instance == this) {
            instance = null
        }
    }
}