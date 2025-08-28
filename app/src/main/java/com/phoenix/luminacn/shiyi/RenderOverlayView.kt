package com.phoenix.luminacn.shiyi

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.game.module.impl.visual.EspElement

class RenderOverlayView(context: Context) : View(context) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        GameManager.elements
            .filterIsInstance<EspElement>()
            .filter { it.isEnabled && it.isSessionCreated }
            .forEach { it.render(canvas) }

        if (GameManager.elements.any {
                it is EspElement&& it.isEnabled && it.isSessionCreated
            }) {
            postInvalidateOnAnimation()
        }
    }
}