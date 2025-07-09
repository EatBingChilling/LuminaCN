package com.project.lumina.client.util

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import java.net.InetAddress
import java.net.Socket

object NetworkOptimizer {
    private lateinit var connectivityManager: ConnectivityManager
    private const val TAG = "NetworkOptimizer"

    fun init(context: Context): Boolean {
        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        return try {
            if (hasRequiredPermissions(context)) {
                requestBestNetwork()
                true
            } else {
                Log.e(TAG, "Missing required permissions for network optimization")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing network optimizer: ${e.message}")
            false
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        
        val hasWriteSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true 
        }
        
        return hasWriteSettings
    }
    
    fun openWriteSettingsPermissionPage(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun requestBestNetwork() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Wi-Fi connected, binding process to Wi-Fi")
                    try {
                        connectivityManager.bindProcessToNetwork(network)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                    }
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Wi-Fi lost, switching to mobile data")
                    switchToMobileData()
                }
            })
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when requesting network: ${e.message}")
        }
    }

    private fun switchToMobileData() {
        try {
            val mobileRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            connectivityManager.requestNetwork(mobileRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Mobile data connected, binding process to mobile network")
                    try {
                        connectivityManager.bindProcessToNetwork(network)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                    }
                }
            })
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when switching to mobile data: ${e.message}")
        }
    }

    fun optimizeSocket(socket: Socket) {
        socket.keepAlive = true
        socket.tcpNoDelay = true
    }

    fun setThreadPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
    }

    fun useFastDNS(): InetAddress {
        return InetAddress.getByName("8.8.8.8")
    }
}
