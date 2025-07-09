package com.project.lumina.client.constructors

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlin.reflect.KProperty

interface Configurable {

    val values: MutableList<Value<*>>

    fun getValue(name: String) = values.find { it.name == name }

    fun boolValue(name: String, value: Boolean) = BoolValue(name, value).also { values.add(it) }

    fun boolValue(@StringRes nameResId: Int, value: Boolean) = BoolValue(nameResId, value).also { values.add(it) }

    fun floatValue(name: String, value: Float, range: ClosedFloatingPointRange<Float>) =
        FloatValue(name, value, range).also { values.add(it) }

    fun floatValue(@StringRes nameResId: Int, value: Float, range: ClosedFloatingPointRange<Float>) =
        FloatValue(nameResId, value, range).also { values.add(it) }

    fun intValue(name: String, value: Int, range: IntRange) =
        IntValue(name, value, range).also { values.add(it) }

    fun intValue(@StringRes nameResId: Int, value: Int, range: IntRange) =
        IntValue(nameResId, value, range).also { values.add(it) }

    fun listValue(name: String, value: ListItem, choices: Set<ListItem>) =
        ListValue(name, value, choices).also { values.add(it) }

    fun listValue(@StringRes nameResId: Int, value: ListItem, choices: Set<ListItem>) =
        ListValue(nameResId, value, choices).also { values.add(it) }

}

@Suppress("MemberVisibilityCanBePrivate")
sealed class Value<T>(val name: String, val defaultValue: T) {

    @StringRes
    var nameResId: Int = 0

    constructor(@StringRes nameResId: Int, defaultValue: T) : this("", defaultValue) {
        this.nameResId = nameResId
    }

    var value: T by mutableStateOf(defaultValue)

    open fun reset() {
        value = defaultValue
    }

    operator fun getValue(from: Any, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(from: Any, property: KProperty<*>, newValue: T) {
        value = newValue
    }

    abstract fun toJson(): JsonElement

    abstract fun fromJson(element: JsonElement)

}

class BoolValue : Value<Boolean> {
    constructor(name: String, defaultValue: Boolean) : super(name, defaultValue)
    constructor(@StringRes nameResId: Int, defaultValue: Boolean) : super(nameResId, defaultValue)

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element is JsonPrimitive) {
            value = element.boolean
        }
    }
}

class FloatValue : Value<Float> {
    val range: ClosedFloatingPointRange<Float>

    constructor(name: String, defaultValue: Float, range: ClosedFloatingPointRange<Float>) : super(name, defaultValue) {
        this.range = range
    }

    constructor(@StringRes nameResId: Int, defaultValue: Float, range: ClosedFloatingPointRange<Float>) : super(nameResId, defaultValue) {
        this.range = range
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element is JsonPrimitive) {
            value = element.float
        }
    }
}

class IntValue : Value<Int> {
    val range: IntRange

    constructor(name: String, defaultValue: Int, range: IntRange) : super(name, defaultValue) {
        this.range = range
    }

    constructor(@StringRes nameResId: Int, defaultValue: Int, range: IntRange) : super(nameResId, defaultValue) {
        this.range = range
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element is JsonPrimitive) {
            value = element.int
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class ListValue : Value<ListItem> {
    val listItems: Set<ListItem>

    constructor(name: String, defaultValue: ListItem, listItems: Set<ListItem>) : super(name, defaultValue) {
        this.listItems = listItems
    }

    constructor(@StringRes nameResId: Int, defaultValue: ListItem, listItems: Set<ListItem>) : super(nameResId, defaultValue) {
        this.listItems = listItems
    }

    override fun toJson() = JsonPrimitive(value.name)

    override fun fromJson(element: JsonElement) {
        if (element is JsonPrimitive) {
            val content = element.content
            value = listItems.find { it.name == content } ?: return
        }
    }
}

interface ListItem {
    val name: String
}