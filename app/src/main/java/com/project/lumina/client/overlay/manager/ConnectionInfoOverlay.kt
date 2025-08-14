// ConnectionInfoOverlay.kt (已修复图标问题)
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

object ConnectionInfoOverlay {
    private var appContext: Context? = null
    private const val PORT = "19132"
    private const val CONNECTION_INFO_IDENTIFIER = "connection_info"

    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    fun show(durationMs: Long = 10000) {
        val context = appContext ?: return
        val localIp = getLocalIpAddress(context)
        val subtitle = "IP: $localIp:$PORT"
        val title = "Lumina 连接信息"

        val intent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS
            putExtra(DynamicIslandService.EXTRA_IDENTIFIER, CONNECTION_INFO_IDENTIFIER)
            putExtra(DynamicIslandService.EXTRA_TITLE, title)
            putExtra(DynamicIslandService.EXTRA_SUBTITLE, subtitle)
            // 【修复】使用一个绝对安全的公共图标
            putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, android.R.drawable.ic_dialog_info)
            putExtra(DynamicIslandService.EXTRA_DURATION_MS, durationMs)
        }
        context.startService(intent)
    }

    fun dismiss() { /* No-op */ }

    // 【修复】将此方法改为 public，以便外部（如HomeScreen）可以调用
    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                val ipAddress = wifiInfo.ipAddress
                if (ipAddress != 0) {
                    return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ipAddress and 0xff), (ipAddress shr 8 and 0xff), (ipAddress shr 16 and 0xff), (ipAddress shr 24 and 0xff))
                }
            }
            val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
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