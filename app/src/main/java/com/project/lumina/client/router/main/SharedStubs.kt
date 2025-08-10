package com.project.lumina.client.router.main

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.MessageDigest

// 已移除所有与 Packs 相关的定义和不再需要的 import

// --- Stub Data Models ---
data class CaptureModeModel(val serverHostName: String = "127.0.0.1", val serverPort: Int = 19132)
data class NoticeInfo(val title: String, val message: String, val rawJson: String)
data class UpdateInfo(val versionName: String, val changelog: String, val url: String)
enum class NotificationType { INFO, WARNING, ERROR, SUCCESS }

// --- Stub Services ---
object Services {
    var isActive by mutableStateOf(false)
    var isLaunchingMinecraft by mutableStateOf(false)
}

// --- Stub Utility Objects & Functions ---
suspend fun makeHttp(urlString: String): String = withContext(Dispatchers.IO) {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else { throw Exception("HTTP Error: ${connection.responseCode}") }
    } finally {
        connection.disconnect()
    }
}

fun getSHA(input: String): String {
    return try {
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { "" }
}

fun getLocalVersionCode(context: Context): Long {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        PackageInfoCompat.getLongVersionCode(packageInfo)
    } catch (e: Exception) { -1L }
}

object NetworkOptimizer {
    fun init(context: Context): Boolean = true
    fun openWriteSettingsPermissionPage(context: Context) {}
    fun optimizeSocket(socket: Socket) {}
    fun setThreadPriority() {}
    fun useFastDNS() {}
}

object ConnectionInfoOverlay {
    fun getLocalIpAddress(context: Context): String? = "192.168.1.100"
    fun show(ip: String?) {}
}

object SimpleOverlayNotification {
    fun show(message: String, type: NotificationType, duration: Long = 3000L) {}
}

object InjectNeko {
    fun injectNeko(context: Context, onComplete: () -> Unit) { onComplete() }
}

object ServerInit {
    fun addMinecraftServer(context: Context, ip: String?) {}
}

// --- Stub ViewModel ---
class MainScreenViewModel : ViewModel() {
    val captureModeModel = MutableStateFlow(CaptureModeModel())
    val selectedGame = MutableStateFlow<String?>("com.mojang.minecraftpe")
    fun selectGame(packageName: String?) {}
    fun selectCaptureModeModel(model: CaptureModeModel) {}
}