package com.phoenix.luminacn.ui

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import com.phoenix.luminacn.constructors.GameManager // FIX: Correct import from ModuleManager
import com.phoenix.luminacn.game.module.impl.visual.EspElement

class ESPOverlayView(context: Context) : View(context) {

    companion object {
        @Volatile
        var instance: ESPOverlayView? = null
            private set
    }

    init {
        instance = this
        isFocusable = false
        isClickable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // FIX: Used GameManager.elements.find to get the module instance, which is the correct way for this client.
        val espModule = GameManager.elements.find { it is EspElement } as? EspElement

        // FIX: The errors for 'isEnabled' and 'onRender2D' are resolved now that we correctly find and cast the module.
        if (espModule != null && espModule.isEnabled) {
            try {
                espModule.onRender2D(canvas)
            } catch (e: Exception) {
                Log.e("ESPOverlayView", "Error during ESP rendering", e)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (instance == this) {
            instance = null
        }
    }
}