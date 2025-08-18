package com.project.lumina.client.application

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.project.lumina.client.activity.CrashHandlerActivity
import com.project.lumina.client.constructors.KeyBindingManager
import com.project.lumina.client.service.ESPService
import com.project.lumina.client.ui.theme.ThemeManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class AppContext : Application(), Thread.UncaughtExceptionHandler {
    companion object {
        lateinit var instance: AppContext
        lateinit var themeManager: ThemeManager
            private set
        
        private const val TAG = "CrashHandler"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private val retryCountMap = ConcurrentHashMap<String, AtomicInteger>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this

        KeyBindingManager.init(this)

        Thread.setDefaultUncaughtExceptionHandler(this)
        themeManager = ThemeManager(this)

        checkOverlayPermissionAndStartServices()
    }

    // ====================== 【关键修改】 ======================
    // 移除了 "private" 关键字，使此函数可以从 NewMainActivity 调用
    // =========================================================
    fun checkOverlayPermissionAndStartServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.d("AppContext", "Overlay permission already granted. Starting services.")
                startEspService()
            } else {
                Log.w("AppContext", "Overlay permission not granted. Services requiring it will not start.")
            }
        } else {
            Log.d("AppContext", "Device is pre-Marshmallow. Starting services directly.")
            startEspService()
        }
    }

    private fun startEspService() {
        Log.d("AppContext", "Attempting to start ESPService.")
        val intent = Intent(this, ESPService::class.java)
        try {
            startService(intent)
            Log.i("AppContext", "ESPService started successfully.")
        } catch (e: Exception) {
            Log.e("AppContext", "Failed to start ESPService.", e)
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG, "未捕获的异常在线程 ${t.name}", e)
        
        if (isNonFatalError(e)) {
            handleNonFatalError(t, e)
            return
        }
        
        showCrashDialog(t, e)
    }

    private fun isNonFatalError(e: Throwable): Boolean {
        return when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is java.io.IOException -> true
            
            is org.json.JSONException,
            is NumberFormatException,
            is IllegalArgumentException -> true
            
            is NullPointerException -> {
                val stackTrace = e.stackTraceToString()
                stackTrace.contains("com.project.lumina.client.overlay") ||
                stackTrace.contains("network") ||
                stackTrace.contains("http") ||
                stackTrace.contains("json")
            }
            
            is java.util.concurrent.RejectedExecutionException,
            is java.util.concurrent.CancellationException -> true
            
            is IllegalStateException -> {
                val message = e.message?.lowercase() ?: ""
                message.contains("network") || 
                message.contains("connection") ||
                message.contains("timeout")
            }
            
            else -> false
        }
    }

    private fun handleNonFatalError(t: Thread, e: Throwable) {
        val errorKey = "${e.javaClass.simpleName}:${e.message?.take(50) ?: "unknown"}"
        val retryCount = retryCountMap.computeIfAbsent(errorKey) { AtomicInteger(0) }
        
        val currentRetry = retryCount.incrementAndGet()
        
        Log.w(TAG, "处理非致命错误 (重试 $currentRetry/$MAX_RETRY_COUNT): $errorKey", e)
        
        if (currentRetry <= MAX_RETRY_COUNT) {
            handler.postDelayed({
                try {
                    Log.i(TAG, "重试执行，错误: $errorKey")
                    performRecoveryAction(e)
                } catch (retryException: Throwable) {
                    Log.e(TAG, "重试失败: $errorKey", retryException)
                    uncaughtException(t, retryException)
                }
            }, RETRY_DELAY_MS * currentRetry)
        } else {
            Log.e(TAG, "重试次数已达上限，将作为致命错误处理: $errorKey")
            showCrashDialog(t, e)
        }
    }

    private fun performRecoveryAction(e: Throwable) {
        when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException -> {
                Log.i(TAG, "网络错误恢复：重置网络连接状态")
            }
            is org.json.JSONException -> {
                Log.i(TAG, "JSON解析错误恢复：跳过当前数据")
            }
            is NullPointerException -> {
                Log.i(TAG, "空指针错误恢复：重新初始化相关组件")
            }
            else -> {
                Log.i(TAG, "通用错误恢复：执行垃圾回收")
                System.gc()
            }
        }
    }

    private fun showCrashDialog(t: Thread, e: Throwable) {
        val stackTrace = e.stackTraceToString()
        val deviceInfo = buildString {
            val declaredFields = Build::class.java.declaredFields
            for (field in declaredFields) {
                field.isAccessible = true
                try {
                    val name = field.name
                    var value = field.get(null)
                    if (value == null) {
                        value = "null"
                    } else if (value.javaClass.isArray) {
                        value = (value as Array<out Any?>).contentDeepToString()
                    }
                    append(name); append(": "); appendLine(value)
                } catch (_: Throwable) { }
            }
        }
        startActivity(Intent(this, CrashHandlerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", buildString {
                appendLine("一个未预料的错误或崩溃发生")
                appendLine("你可以联系开发者并报告该问题（使用英语）")
                appendLine()
                appendLine(deviceInfo)
                appendLine("Thread: ${t.name}")
                appendLine("Thread Group: ${t.threadGroup?.name}")
                appendLine()
                appendLine("Stack Trace: $stackTrace")
            })
        })
        Process.killProcess(Process.myPid())
    }
}