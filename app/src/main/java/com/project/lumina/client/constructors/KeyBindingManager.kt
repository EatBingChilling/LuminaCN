package com.project.lumina.client.constructors

import android.content.Context
import android.content.SharedPreferences

object KeyBindingManager {

    private const val PREFS_NAME = "key_bindings"
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setBinding(elementName: String, keyCode: Int) {
        sharedPreferences.edit().putInt(elementName, keyCode).apply()
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
    }
}
