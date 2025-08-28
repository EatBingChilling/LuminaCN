package com.phoenix.luminacn.game.module.api.setting

import com.phoenix.luminacn.constructors.Configurable
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.ListItem
import com.phoenix.luminacn.constructors.Value
import com.phoenix.luminacn.game.event.EventHook
import com.phoenix.luminacn.game.event.GameEvent
import com.phoenix.luminacn.game.event.Handler
import com.phoenix.luminacn.constructors.ListValue

class ChoiceValue(name: String, values: Array<Choice>, value: Choice) : ListValue(name, value, values.toSet()) {

    private var isActive = false

    init {
        this.addChangeListener { newValue: ListItem ->
            if (isActive) {
                values.forEach { choice ->
                    if (newValue is Choice) {
                        choice.isActive = (newValue == choice)
                    }
                }
            }
        }
    }

    abstract class Choice(override val name: String) : ListItem, Configurable {

        open var isActive = false
            set(value) {
                if (value == field) return

                if (value) {
                    onEnable()
                } else {
                    onDisable()
                }
                field = value
            }

        val handlers = mutableListOf<EventHook<in GameEvent>>()

        override val values = mutableListOf<Value<*>>()

        open fun onEnable() {}

        open fun onDisable() {}

        @Suppress("unchecked_cast")
        protected inline fun <reified T : GameEvent> handle(noinline handler: Handler<T>) {
            handlers.add(
                EventHook(
                    T::class.java,
                    handler
                ) { this.isActive } as EventHook<in GameEvent>)
        }

        @Suppress("unchecked_cast")
        fun getHandlers(module: Element): List<EventHook<in GameEvent>> {
            return handlers.map {
                EventHook(it.eventClass, it.handler) {
                    module.isEnabled && this.isActive
                } as EventHook<in GameEvent>
            }
        }
    }
}