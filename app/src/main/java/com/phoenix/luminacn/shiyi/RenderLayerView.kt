package com.phoenix.luminacn.shiyi

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.View
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.event.*

class RenderLayerView(context: Context, private val session: NetBound) : View(context), Listenable {

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val event = EventRender(session, canvas, context)
        session.eventManager.emit(event)
        if (event.needRefresh) {
            invalidate()
        }
    }

    private val handleRefreshRender = handle<EventRefreshRender> {
        invalidate()
    }

    override val eventManager: EventManager
        get() = session.eventManager

    class EventRender(session: NetBound, val canvas: Canvas, val context: Context , var needRefresh: Boolean = false) : GameEvent(session, "render")
    class EventRefreshRender(session: NetBound) : GameEvent(session, "refresh_render")
}
