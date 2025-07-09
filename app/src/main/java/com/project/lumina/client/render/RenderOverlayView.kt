package com.project.lumina.client.render

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.content.res.AssetManager
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.game.module.impl.misc.ESPElement

class RenderOverlayView(context: Context) : View(context) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate() // Initial render
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        GameManager.elements
            .filterIsInstance<ESPElement>()
            .filter { it.isEnabled && it.isSessionCreated } //isSessionCreated check
            .forEach { it.render(canvas) }

        if (GameManager.elements.any {
                it is ESPElement && it.isEnabled && it.isSessionCreated
            }) {
            postInvalidateOnAnimation()
        }
    }
}