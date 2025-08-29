package com.phoenix.luminacn.shiyi

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.event.*

class NameTagRenderView(context: Context) : View(context), Listenable {

    private var _session: NetBound? = null
    private var _eventManager: EventManager? = null

    init {
        setWillNotDraw(false)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // 不在构造函数中强制要求 session
        updateSession(GameManager.netBound)
    }

    fun updateSession(newSession: NetBound?) {
        this._session = newSession
        this._eventManager = newSession?.eventManager
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 只在有 session 时才绘制
        _session?.let { currentSession ->
            try {
                val event = EventNameTagRender(currentSession, canvas, context)
                currentSession.eventManager.emit(event)
                if (event.needRefresh) {
                    invalidate()
                }
            } catch (e: Exception) {
                android.util.Log.w("NameTagRenderView", "Error during rendering: ${e.message}")
            }
        }
    }

    // 延迟注册事件处理器，只在有 session 时注册
    private var refreshHandlerRegistered = false
    
    private fun ensureEventHandlerRegistered() {
        if (!refreshHandlerRegistered && _session != null) {
            try {
                handle<EventRefreshNameTagRender> {
                    invalidate()
                }
                refreshHandlerRegistered = true
            } catch (e: Exception) {
                android.util.Log.w("NameTagRenderView", "Failed to register event handler: ${e.message}")
            }
        }
    }

    // 提供一个安全的 EventManager，如果没有 session 就创建一个空的
    override val eventManager: EventManager
        get() {
            if (_eventManager == null) {
                // 创建一个临时的空 EventManager
                _eventManager = EventManager()
            }
            return _eventManager!!
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 窗口附加时尝试更新 session
        updateSession(GameManager.netBound)
        ensureEventHandlerRegistered()
    }

    class EventNameTagRender(session: NetBound, val canvas: Canvas, val context: Context, var needRefresh: Boolean = false) : GameEvent(session, "nametag_render")
    class EventRefreshNameTagRender(session: NetBound) : GameEvent(session, "refresh_nametag_render")
}