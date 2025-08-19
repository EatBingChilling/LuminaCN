package com.phoenix.luminacn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.phoenix.luminacn.R

class PersistentService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "persistent_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        // 在这里启动或绑定 KeyCaptureService
        val keyCaptureIntent = Intent(this, KeyCaptureService::class.java)
        startService(keyCaptureIntent)

        return START_STICKY // 系统杀死服务后，会尝试重启
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 非绑定服务
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Lumina Persistent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "确保Lumina核心服务持续运行"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Lumina 核心服务")
            .setContentText("服务正在后台运行以支持按键绑定")
            .setSmallIcon(R.drawable.img)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}