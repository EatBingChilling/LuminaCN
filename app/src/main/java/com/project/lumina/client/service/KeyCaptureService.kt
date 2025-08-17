package com.project.lumina.client.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.project.lumina.client.R
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.constructors.KeyBindingManager

class KeyCaptureService : AccessibilityService() {

    companion object {
        const val ACTION_KEY_EVENT = "com.project.lumina.client.ACTION_KEY_EVENT"
        const val EXTRA_KEY_EVENT = "com.project.lumina.client.EXTRA_KEY_EVENT"
        private const val NOTIFICATION_CHANNEL_ID = "key_capture_service_channel"
        private const val NOTIFICATION_ID = 1002

        /**
         * 请求为一个模块绑定实体按键。
         * @param context 调用方的 Context，用于发送 Intent。
         * @param element 要绑定的模块。
         */
        fun requestBind(context: Context, element: Element) {
            BindRequest.element = element
            // 【修改 1】直接构建并发送 Intent 来调用灵动岛服务
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS
                putExtra(DynamicIslandService.EXTRA_IDENTIFIER, "key_bind_request")
                putExtra(DynamicIslandService.EXTRA_TITLE, "请按下要绑定的实体按键...")
                putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, R.drawable.ic_key_variant_black_24dp)
                putExtra(DynamicIslandService.EXTRA_DURATION_MS, 4000L) // 延长等待时间
            }
            context.startService(intent)
        }
    }

    object BindRequest {
        var element: Element? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        BindRequest.element?.let { elementToBind ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                KeyBindingManager.setBinding(elementToBind.name, event.keyCode)
                // 【修改 2】绑定成功后，直接从此服务发送 Intent
                val intent = Intent(this, DynamicIslandService::class.java).apply {
                    action = DynamicIslandService.ACTION_SHOW_OR_UPDATE_PROGRESS
                    // 使用时间戳作为唯一ID，确保每次都是新通知
                    putExtra(DynamicIslandService.EXTRA_IDENTIFIER, System.currentTimeMillis().toString())
                    putExtra(DynamicIslandService.EXTRA_TITLE, "${elementToBind.name} 已绑定")
                    putExtra(DynamicIslandService.EXTRA_ICON_RES_ID, R.drawable.ic_check_circle_24)
                    putExtra(DynamicIslandService.EXTRA_DURATION_MS, 3000L)
                }
                startService(intent)

                BindRequest.element = null
                return true
            }
        }

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
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