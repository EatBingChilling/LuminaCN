package com.project.lumina.client.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.project.lumina.client.R

import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.constructors.KeyBindingManager

class KeyCaptureService : AccessibilityService() {

    companion object {
        const val ACTION_KEY_EVENT = "com.project.lumina.client.ACTION_KEY_EVENT"
        const val EXTRA_KEY_EVENT = "com.project.lumina.client.EXTRA_KEY_EVENT"
        private const val NOTIFICATION_CHANNEL_ID = "key_capture_service_channel"
        private const val NOTIFICATION_ID = 1002
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 创建前台服务通知，防止被系统杀死
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            KeyBindingManager.getElementByKeyCode(event.keyCode)?.let { elementName ->
                GameManager.elements.find { it.name == elementName }?.let { element ->
                    element.isEnabled = !element.isEnabled
                }
            }
        }

        val intent = Intent(ACTION_KEY_EVENT).apply {
            putExtra(EXTRA_KEY_EVENT, event)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for key capture
    }

    override fun onInterrupt() {
        // Not needed
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务被销毁时尝试重启
        val intent = Intent(this, KeyCaptureService::class.java)
        startService(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Key Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lumina Key Capture Service"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Lumina Key Capture")
        .setContentText("无障碍服务正在运行")
        .setSmallIcon(R.drawable.img)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}
