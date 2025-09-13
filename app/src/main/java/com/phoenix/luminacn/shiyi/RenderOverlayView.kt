package com.phoenix.luminacn.shiyi

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.game.module.impl.visual.EspElement

class RenderOverlayView(context: Context) : View(context) {

    // 找到 ESP 模块的实例
    private val espElement: EspElement? = GameManager.elements.filterIsInstance<EspElement>().firstOrNull()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 当 View 被添加到窗口时，设置 ESP 模块对它的引用
        espElement?.let { EspElement.setRenderView(this) }
        // 首次附加时立即请求重绘
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 当 View 从窗口移除时，清理引用，防止内存泄漏
        espElement?.let { EspElement.setRenderView(null) }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 直接调用 ESP 模块的 render 方法
        espElement?.render(canvas)

        // 如果 ESP 模块是启用的，则持续请求下一帧动画
        if (espElement?.isEnabled == true && espElement.isSessionCreated) {
            postInvalidateOnAnimation()
        }
    }
}