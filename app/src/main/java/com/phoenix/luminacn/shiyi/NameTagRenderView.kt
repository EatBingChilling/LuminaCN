package com.phoenix.luminacn.shiyi

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.event.*

class NameTagRenderView(context: Context) : View(context), Listenable {

    private var session: NetBound? = null

    init {
        setWillNotDraw(false)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // 获取当前session
        session = GameManager.netBound
    }

    fun updateSession(newSession: NetBound?) {
        this.session = newSession
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        session?.let { currentSession ->
            val event = EventNameTagRender(currentSession, canvas, context)
            currentSession.eventManager.emit(event)
            if (event.needRefresh) {
                invalidate()
            }
        }
    }

    private val handleRefreshRender = handle<EventRefreshNameTagRender> {
        invalidate()
    }

    override val eventManager: EventManager?
        get() = session?.eventManager

    class EventNameTagRender(session: NetBound, val canvas: Canvas, val context: Context, var needRefresh: Boolean = false) : GameEvent(session, "nametag_render")
    class EventRefreshNameTagRender(session: NetBound) : GameEvent(session, "refresh_nametag_render")
}