/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.util.Enumeration

object ConnectionInfoOverlay {
    private val overlayInstance by lazy { ConnectionInfoWindow() }
    var localIp: String = "127.0.0.1"
        private set

    fun show(ip: String, durationMs: Long = 10000) {
        val context = AppContext.instance
        // Update the localIp with the latest value
        localIp = getLocalIpAddress(context)
        
        OverlayManager.showOverlayWindow(overlayInstance)
        
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if the window is still shown before dismissing
            if (overlayInstance.isShowing) {
                overlayInstance.dismissAnimated()
            }
        }, durationMs)
    }
    
    fun dismiss() {
        OverlayManager.dismissOverlayWindow(overlayInstance)
    }
    
    /**
     * get local ip address of the phone
     */
    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                val ipAddress = wifiInfo.ipAddress
                if (ipAddress != 0) {
                    return String.format(
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
            
            val isConnected = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            
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
    
    // The state for dismissing must be internal to the window instance
    private var isDismissing by mutableStateOf(false)
    var isShowing by mutableStateOf(false)
        private set
    
    fun dismissAnimated() {
        isDismissing = true
    }
    
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
        
        // Use the instance's isDismissing state
        val dismissTriggered = isDismissing
        
        val scale by animateFloatAsState(
            targetValue = if (dismissTriggered || !isVisible) 0.8f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "overlayScale"
        )
        
        val alpha by animateFloatAsState(
            targetValue = if (dismissTriggered || !isVisible) 0f else 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "overlayAlpha"
        )
        
        val offsetY by animateFloatAsState(
            targetValue = if (dismissTriggered) 50f else if (isVisible) 0f else -50f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "overlayOffsetY"
        )
        
        LaunchedEffect(dismissTriggered) {
            if (dismissTriggered) {
                delay(400) // Wait for animation to finish
                ConnectionInfoOverlay.dismiss()
            }
        }
        
        // Reset state when the composable enters composition
        LaunchedEffect(Unit) {
            isShowing = true
            isDismissing = false
            isVisible = true
        }

        DisposableEffect(Unit) {
            onDispose {
                isShowing = false
            }
        }
        
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            // <<< MODIFIED: Using ElevatedCard for a modern MD3 look with tonal elevation
            ElevatedCard(
                modifier = Modifier
                    .width(300.dp) // Slightly wider for better spacing
                    .wrapContentHeight()
                    .scale(scale)
                    .alpha(alpha)
                    .offset(y = offsetY.dp),
                shape = RoundedCornerShape(16.dp),
                // <<< MODIFIED: Using theme colors for container and elevation
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Title and Close Button ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lumina 连接信息",
                            // <<< MODIFIED: Using MaterialTheme typography and colors
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { dismissAnimated() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                // <<< MODIFIED: Using theme color for icon
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        // <<< MODIFIED: Using standard MD3 divider color
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // --- IP Address Info ---
                    InfoRow(
                        icon = Icons.Default.Public,
                        label = "IP 地址",
                        // <<< FIXED: Display the actual local IP address
                        value = ConnectionInfoOverlay.localIp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // --- Port Info ---
                    InfoRow(
                        icon = Icons.Default.Dns,
                        label = "端口",
                        value = "19132" // Minecraft PE default port
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- Instruction Text ---
                    Text(
                        // <<< FIXED: Dynamically insert IP and Port into instructions
                        text = "请在游戏的服务器设置中，添加一个新服务器，" +
                                "地址为 ${ConnectionInfoOverlay.localIp}，端口为 19132。",
                        // <<< MODIFIED: Using theme typography and colors for secondary text
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * A reusable Composable for displaying an information row with an icon, label, and value.
 * This improves code readability and consistency.
 */
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            // <<< MODIFIED: Use primary theme color for icons
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}