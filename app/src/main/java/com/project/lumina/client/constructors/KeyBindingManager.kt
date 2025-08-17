package com.project.lumina.client.constructors

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object KeyBindingManager {

    private const val PREFS_NAME = "key_bindings"
    private lateinit var sharedPreferences: SharedPreferences
    
    // 添加响应式的 bindings StateFlow
    private val _bindings = MutableStateFlow<Map<String, Int>>(emptyMap())
    val bindings: StateFlow<Map<String, Int>> = _bindings.asStateFlow()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 初始化时加载所有绑定
        loadAllBindings()
    }

    private fun loadAllBindings() {
        val allBindings = mutableMapOf<String, Int>()
        for ((key, value) in sharedPreferences.all) {
            if (value is Int) {
                allBindings[key] = value
            }
        }
        _bindings.value = allBindings
    }

    fun setBinding(elementName: String, keyCode: Int) {
        sharedPreferences.edit().putInt(elementName, keyCode).apply()
        // 更新 StateFlow
        val currentBindings = _bindings.value.toMutableMap()
        currentBindings[elementName] = keyCode
        _bindings.value = currentBindings
    }

    fun getBinding(elementName: String): Int? {
        return if (sharedPreferences.contains(elementName)) {
            sharedPreferences.getInt(elementName, -1)
        } else {
            null
        }
    }

    fun getElementByKeyCode(keyCode: Int): String? {
        for ((elementName, storedKeyCode) in sharedPreferences.all) {
            if (storedKeyCode as? Int == keyCode) {
                return elementName
            }
        }
        return null
    }

    fun removeBinding(elementName: String) {
        sharedPreferences.edit().remove(elementName).apply()
        // 更新 StateFlow
        val currentBindings = _bindings.value.toMutableMap()
        currentBindings.remove(elementName)
        _bindings.value = currentBindings
    }
}