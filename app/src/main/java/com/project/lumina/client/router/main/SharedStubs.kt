package com.project.lumina.client.router.main

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.MessageDigest

//================================================================================//
// STUBS: 占位符/共享代码
// 这个文件包含了所有其他文件都需要的共享数据类、对象和函数。
//================================================================================//

// --- 共享数据类和模型 ---
data class MCPack(val name: String, val url: String)
data class CaptureModeModel(val serverHostName: String = "127.0.0.1", val serverPort: Int = 19132)
data class NoticeInfo(val title: String, val message: String, val rawJson: String)
data class UpdateInfo(val versionName: String, val changelog: String, val url: String)
enum class NotificationType { INFO, WARNING, ERROR, SUCCESS }

// --- 共享的管理器和服务 ---
object PackSelectionManager {
    var selectedPack: MCPack? = null
}

object Services {
    var isActive by mutableStateOf(false)
    var isLaunchingMinecraft by mutableStateOf(false)
}

// --- 共享的工具对象和函数 ---
suspend fun makeHttp(urlString: String): String = withContext(Dispatchers.IO) {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            throw Exception("HTTP Error: ${connection.responseCode}")
        }
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
    fun init(context: Context): Boolean {
        println("NetworkOptimizer: Initializing...")
        return true // Return true if permission is granted, false otherwise
    }
    fun openWriteSettingsPermissionPage(context: Context) { println("NetworkOptimizer: Opening settings page.") }
    fun optimizeSocket(socket: Socket) { println("NetworkOptimizer: Optimizing socket.") }
    fun setThreadPriority() { println("NetworkOptimizer: Setting thread priority.") }
    fun useFastDNS() { println("NetworkOptimizer: Using fast DNS.") }
}

// --- 共享的浮窗和通知组件 ---
object ConnectionInfoOverlay {
    fun getLocalIpAddress(context: Context): String? = "192.168.1.100"
    fun show(ip: String?) { println("OVERLAY: Show connection info for $ip") }
}

object SimpleOverlayNotification {
    fun show(message: String, type: NotificationType, duration: Long = 3000L) {
        println("OVERLAY NOTIFICATION [${type.name}]: $message (for ${duration}ms)")
    }
}

// --- 共享的特定领域工具 ---
object MCPackUtils {
    suspend fun downloadAndOpenPack(context: Context, pack: MCPack, onProgress: (Float) -> Unit) {
        println("MCPackUtils: Downloading ${pack.name}")
        withContext(Dispatchers.IO) { for (i in 1..10) { delay(200); onProgress(i / 10f) } }
        println("MCPackUtils: Download complete.")
    }
}

object InjectNeko {
    fun injectNeko(context: Context, onComplete: () -> Unit) {
        println("InjectNeko: Injecting..."); onComplete()
    }
}

object ServerInit {
    fun addMinecraftServer(context: Context, ip: String?) {
        println("ServerInit: Adding Minecraft server with IP: $ip")
    }
}

// --- 共享的 ViewModel ---
class MainScreenViewModel : ViewModel() {
    private val _captureModeModel = MutableStateFlow(CaptureModeModel())
    val captureModeModel = _captureModeModel.asStateFlow()

    private val _selectedGame = MutableStateFlow<String?>("com.mojang.minecraftpe")
    val selectedGame = _selectedGame.asStateFlow()

    fun selectCaptureModeModel(model: CaptureModeModel) {
        _captureModeModel.value = model
        // Save to preferences logic would go here
    }

    fun selectGame(packageName: String?) {
        _selectedGame.value = packageName
    }
}