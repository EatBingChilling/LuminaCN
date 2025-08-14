package com.project.lumina.client.overlay.manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.project.lumina.client.service.DynamicIslandService
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration
import java.util.Locale

/**
 * 【重构 & 适配灵动岛】
 * 此类作为显示“连接信息”的中继点。
 * 它负责获取本地IP地址，并将其格式化后，通过灵动岛显示给用户。
 *
 * 它不再创建自己的悬浮窗或管理任何 Jetpack Compose UI。
 */
object ConnectionInfoOverlay {

    private var appContext: Context? = null
    private const val PORT = "19132" // 端口是固定的

    /**
     * 为连接信息通知定义一个固定的标识符。
     */
    private const val CONNECTION_INFO_IDENTIFIER = "connection_info"

    /**
     * 必须在应用启动时调用一次，以提供必要的上下文。
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * 在灵动岛上显示连接信息。
     *
     * @param durationMs 通知显示的毫秒时长。
     */
    fun show(durationMs: Long = 10000) {
        val context = appContext
        if (context == null) {
            Log.e("ConnectionInfoOverlay", "错误: Context 为空。请确保在应用启动时调用了 init()。")
            return
        }

        // 1. 获取本地IP地址
        val localIp = getLocalIpAddress(context)

        // 2. 将核心信息格式化为副标题
        val subtitle = "IP: $localIp:$PORT"
        val title = "LuminaCN 连接信息"

        // 3. 创建 Intent 并发送给服务
        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS

            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, CONNECTION_INFO_IDENTIFIER)
            putExtra(DynamicIslandService.EXTRA_TITLE, title)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, subtitle)

            // 使用一个网络相关的系统图标
            putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, android.R.drawable.stat_sys_wifi_signal_4_fully)

            // 这是一个基于时间的通知
            putExtra(DynamicIslandService.EXTRA_DURATION_MS, durationMs)
        }

        context.startService(intent)
    }

    /**
     * 【逻辑说明】隐藏连接信息。
     * 无需主动调用。灵动岛的任务有超时机制，
     * 时间到了会自动消失。
     */
    fun dismiss() {
        // No-op. The island view will automatically remove the task after its duration.
    }

    /**
     * 获取本地IP地址的工具函数。
     * (这是完整的代码)
     */
    private fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                val ipAddress = wifiInfo.ipAddress
                if (ipAddress != 0) {
                    return String.format(
                        Locale.getDefault(),
                        "%d.%d.%d.%d",
                        (ipAddress and 0xff),
                        (ipAddress shr 8 and 0xff),
                        (ipAddress shr 16 and 0xff),
                        (ipAddress shr 24 and 0xff)
                    )
                }
            }
            val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
            if (isConnected) {
                val networkInterfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface: NetworkInterface = networkInterfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    val addresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address: InetAddress = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.hostAddress.contains(".")) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectionInfoOverlay", "获取IP地址时出错: ${e.message}")
        }
        return "127.0.0.1"
    }
}