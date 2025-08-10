package com.project.lumina.client.overlay.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.lumina.client.application.AppContext
import kotlinx.coroutines.delay
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object ConnectionInfoOverlay {
    private val overlayInstance by lazy { ConnectionInfoWindow() }
    var localIp: String = "127.0.0.1"
        private set

    fun show(ip: String, durationMs: Long = 10000) {
        val context = AppContext.instance
        localIp = getLocalIpAddress(context)

        if (!overlayInstance.isShowing) {
             OverlayManager.showOverlayWindow(overlayInstance)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (overlayInstance.isShowing) {
                overlayInstance.dismissAnimated()
            }
        }, durationMs)
    }

    fun dismiss() {
        if (overlayInstance.isShowing) {
            OverlayManager.dismissOverlayWindow(overlayInstance)
        }
    }

    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                val ipAddress = wifiInfo.ipAddress
                if (ipAddress != 0) {
                    return String.format("%d.%d.%d.%d", (ipAddress and 0xff), (ipAddress shr 8 and 0xff), (ipAddress shr 16 and 0xff), (ipAddress shr 24 and 0xff))
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
            Log.e("ConnectionInfoOverlay", "Error getting IP address: ${e.message}")
        }
        return "127.0.0.1"
    }
}

class ConnectionInfoWindow : OverlayWindow() {
    private var isDismissing by mutableStateOf(false)
    var isShowing by mutableStateOf(false)
        private set

    fun dismissAnimated() { isDismissing = true }

    override val layoutParams by lazy {
        super.layoutParams.apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }
    }

    @Composable
    override fun Content() {
        var isVisible by remember { mutableStateOf(false) }
        val dismissTriggered = isDismissing

        val scale by animateFloatAsState(targetValue = if (dismissTriggered || !isVisible) 0.8f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "overlayScale")
        val alpha by animateFloatAsState(targetValue = if (dismissTriggered || !isVisible) 0f else 1f, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "overlayAlpha")
        val offsetY by animateFloatAsState(targetValue = if (dismissTriggered) 50f else if (isVisible) 0f else -50f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "overlayOffsetY")

        LaunchedEffect(dismissTriggered) {
            if (dismissTriggered) {
                delay(400)
                ConnectionInfoOverlay.dismiss()
            }
        }

        DisposableEffect(Unit) {
            isShowing = true
            isDismissing = false
            isVisible = true
            onDispose { isShowing = false }
        }

        Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
            MaterialTheme {
                ElevatedCard(
                    modifier = Modifier.width(300.dp).wrapContentHeight().scale(scale).alpha(alpha).offset(y = offsetY.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Lumina 连接信息", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = ::dismissAnimated, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow(icon = Icons.Default.Public, label = "IP 地址", value = ConnectionInfoOverlay.localIp)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.Dns, label = "端口", value = "19132")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "请在游戏的多人游戏设置中，添加一个新服务器，地址为 ${ConnectionInfoOverlay.localIp}，端口为 19132。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = "$label:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}