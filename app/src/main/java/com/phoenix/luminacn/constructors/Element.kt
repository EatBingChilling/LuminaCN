package com.phoenix.luminacn.constructors

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phoenix.luminacn.game.InterruptiblePacketHandler
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.mods.OverlayModuleList
import com.phoenix.luminacn.overlay.mods.OverlayNotification
import com.phoenix.luminacn.overlay.manager.OverlayShortcutButton
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.event.EventHook
import com.phoenix.luminacn.game.event.EventTick
import com.phoenix.luminacn.game.event.Handler
import com.phoenix.luminacn.game.event.EventManager
import com.phoenix.luminacn.game.event.GameEvent
import com.phoenix.luminacn.game.event.EventModuleToggle
import com.phoenix.luminacn.game.module.api.setting.ChoiceValue
import kotlinx.serialization.json.*
import kotlin.properties.Delegates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

abstract class Element(
    val name: String,
    val category: CheatCategory,
    val iconResId: Int = 0,
    defaultEnabled: Boolean = false,
    val private: Boolean = false,
    @StringRes open val displayNameResId: Int? = null,
    val canToggle: Boolean = true
) : InterruptiblePacketHandler, Configurable {

    open val state: Any
        get() = isEnabled

    open lateinit var session: NetBound
    private var _isEnabled by mutableStateOf(defaultEnabled)

    protected val handlers = mutableListOf<EventHook<in GameEvent>>()
    lateinit var moduleManager: GameManager

    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            if (_isEnabled == value) return

            if (this::session.isInitialized) {
                val event = EventModuleToggle(session, this, value)
                session.eventManager.emit(event)
                if (event.isCanceled()) {
                    return
                }
            }

            if (!canToggle) {
                onEnabled()
                return
            }

            _isEnabled = value
            if (value) {
                onEnabled()
            } else {
                onDisabled()
            }
        }

    val isSessionCreated: Boolean
        get() = ::session.isInitialized

    var isExpanded by mutableStateOf(false)
    var isShortcutDisplayed by mutableStateOf(false)
    var shortcutX = 0
    var shortcutY = 100

    val overlayShortcutButton by lazy { OverlayShortcutButton(this) }
    override val values: MutableList<Value<*>> = ArrayList()

    open fun getStatusInfo(): String {
        return ""
    }

    open fun onEnabled() {
        ArrayListManager.addModule(this)
        sendToggleMessage(true)
    }

    open fun onDisabled() {
        ArrayListManager.removeModule(this)
        sendToggleMessage(false)
    }

    open fun toggle() {
        this.isEnabled = !this.isEnabled
    }

    @Suppress("unchecked_cast")
    protected inline fun <reified T : GameEvent> handle(noinline handler: Handler<T>) {
        handlers.add(EventHook(T::class.java, handler) { this.isEnabled } as EventHook<in GameEvent>)
    }

    @Suppress("unchecked_cast")
    protected inline fun <reified T : GameEvent> handle(crossinline condition: () -> Boolean, noinline handler: Handler<T>) {
        handlers.add(EventHook(T::class.java, handler) { this.isEnabled && condition() } as EventHook<in GameEvent>)
    }

    @Suppress("unchecked_cast")
    protected inline fun <reified T : GameEvent> handleEvent(crossinline condition: (T) -> Boolean, noinline handler: Handler<T>) {
        handlers.add(EventHook(T::class.java, handler) { this.isEnabled && condition(it) } as EventHook<in GameEvent>)
    }

    @Suppress("unchecked_cast")
    protected inline fun <reified T : GameEvent> handleOneTime(crossinline condition: (T) -> Boolean, noinline handler: Handler<T>) {
        var trigger = false
        handlers.add(EventHook(T::class.java, handler) {
            val satisfied = condition(it)
            if (this.isEnabled) {
                if (satisfied &&!trigger) {
                    trigger = true
                    true
                } else {
                    if (!satisfied) {
                        trigger = false
                    }
                    false
                }
            } else {
                trigger = false
                false
            }  } as EventHook<in GameEvent>)
    }

    open fun toJson() = buildJsonObject {
        put("state", isEnabled)
        put("values", buildJsonObject {
            values.forEach { value ->
                val key = if (value.name.isNotEmpty()) value.name else value.nameResId.toString()
                put(key, value.toJson())
            }
        })
        if (isShortcutDisplayed) {
            put("shortcut", buildJsonObject {
                put("x", shortcutX)
                put("y", shortcutY)
            })
        }
    }

    open fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonObject) {
            isEnabled = (jsonElement["state"] as? JsonPrimitive)?.boolean ?: isEnabled
            (jsonElement["values"] as? JsonObject)?.let {
                it.forEach { jsonObject ->
                    val value = getValue(jsonObject.key)
                        ?: if (jsonObject.key.toIntOrNull() != null) {
                            values.find { it.nameResId == jsonObject.key.toInt() }
                        } else null

                    value?.let { v ->
                        try {
                            v.fromJson(jsonObject.value)
                        } catch (e: Throwable) {
                            v.reset()
                        }
                    }
                }
            }
            (jsonElement["shortcut"] as? JsonObject)?.let {
                shortcutX = (it["x"] as? JsonPrimitive)?.int ?: shortcutX
                shortcutY = (it["y"] as? JsonPrimitive)?.int ?: shortcutY
                isShortcutDisplayed = true
            }
        }
    }

    private fun sendToggleMessage(enabled: Boolean) {
        if (!isSessionCreated) return
        try {
            OverlayNotification.addNotification(name, enabled)
            if (enabled) OverlayModuleList.showText(name)
            else OverlayModuleList.removeText(name)
        } catch (e: Exception) {
            Log.w("AppCrashChan :3", "Failed to show module notification: ${e.message}")
        }
    }
}