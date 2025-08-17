package com.project.lumina.client.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
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
         * 调用此方法后，下一次按下的实体键将被绑定到该模块。
         * @param element 要绑定的模块。
         */
        fun requestBind(element: Element, context: Context) {
            BindRequest.element = element
            Toast.makeText(context, "请按下要绑定的实体按键...", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 用于暂存按键绑定请求的静态对象。
     */
    object BindRequest {
        var element: Element? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 创建前台服务通知，防止被系统杀死
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // 优先处理按键绑定请求
        BindRequest.element?.let { elementToBind ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                // 执行绑定
                KeyBindingManager.setBinding(elementToBind.name, event.keyCode)
                Toast.makeText(this, "${elementToBind.name} 已绑定", Toast.LENGTH_SHORT).show()

                // 清空请求并消费事件
                BindRequest.element = null
                return true
            }
        }

        // 如果没有绑定请求，则执行原有的按键触发逻辑
        if (event.action == KeyEvent.ACTION_DOWN) {
            KeyBindingManager.getElementByKeyCode(event.keyCode)?.let { elementName ->
                GameManager.elements.find { it.name == elementName }?.let { element ->
                    element.isEnabled = !element.isEnabled
                }
            }
        }

        // 广播事件（保留原有功能）
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
        // 服务被销毁时，PersistentService 会负责拉起，这里无需再自启
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