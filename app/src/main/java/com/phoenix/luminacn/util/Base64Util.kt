package com.phoenix.luminacn.util

object Base64Util {
    fun decode(encoded: String): String {
        return String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
    }
}