package com.project.lumina.client.application

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import com.project.lumina.client.activity.CrashHandlerActivity
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

    // 用于跟踪重试次数的映射
    private val retryCountMap = ConcurrentHashMap<String, AtomicInteger>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread.setDefaultUncaughtExceptionHandler(this)
        themeManager = ThemeManager(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG, "未捕获的异常在线程 ${t.name}", e)
        
        // 检查是否为非致命错误
        if (isNonFatalError(e)) {
            handleNonFatalError(t, e)
            return
        }
        
        // 致命错误，显示崩溃界面
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

                    append(name)
                    append(": ")
                    appendLine(value)
                } catch (_: Throwable) { }
            }
        }

        startActivity(Intent(this, CrashHandlerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
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

    /**
     * 判断是否为非致命错误
     */
    private fun isNonFatalError(e: Throwable): Boolean {
        return when (e) {
            // 网络相关错误
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is java.io.IOException -> true
            
            // 数据解析错误
            is org.json.JSONException,
            is NumberFormatException,
            is IllegalArgumentException -> true
            
            // 空指针但在特定安全场景下
            is NullPointerException -> {
                val stackTrace = e.stackTraceToString()
                // 检查是否在UI组件或网络请求中
                stackTrace.contains("com.project.lumina.client.overlay") ||
                stackTrace.contains("network") ||
                stackTrace.contains("http") ||
                stackTrace.contains("json")
            }
            
            // 并发相关错误
            is java.util.concurrent.RejectedExecutionException,
            is java.util.concurrent.CancellationException -> true
            
            // 其他可恢复错误
            is IllegalStateException -> {
                val message = e.message?.lowercase() ?: ""
                message.contains("network") || 
                message.contains("connection") ||
                message.contains("timeout")
            }
            
            else -> false
        }
    }

    /**
     * 处理非致命错误
     */
    private fun handleNonFatalError(t: Thread, e: Throwable) {
        val errorKey = "${e.javaClass.simpleName}:${e.message?.take(50) ?: "unknown"}"
        val retryCount = retryCountMap.computeIfAbsent(errorKey) { AtomicInteger(0) }
        
        val currentRetry = retryCount.incrementAndGet()
        
        Log.w(TAG, "处理非致命错误 (重试 $currentRetry/$MAX_RETRY_COUNT): $errorKey", e)
        
        if (currentRetry <= MAX_RETRY_COUNT) {
            // 延迟重试
            handler.postDelayed({
                try {
                    Log.i(TAG, "重试执行，错误: $errorKey")
                    // 这里可以根据错误类型执行特定的恢复操作
                    performRecoveryAction(e)
                } catch (retryException: Throwable) {
                    Log.e(TAG, "重试失败: $errorKey", retryException)
                    // 如果重试也失败，递归调用处理
                    uncaughtException(t, retryException)
                }
            }, RETRY_DELAY_MS * currentRetry) // 指数退避
        } else {
            Log.e(TAG, "重试次数已达上限，将作为致命错误处理: $errorKey")
            // 重试次数用完，作为致命错误处理
            showCrashDialog(t, e)
        }
    }

    /**
     * 执行恢复操作
     */
    private fun performRecoveryAction(e: Throwable) {
        when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException -> {
                Log.i(TAG, "网络错误恢复：重置网络连接状态")
                // 这里可以重置网络连接状态
            }
            
            is org.json.JSONException -> {
                Log.i(TAG, "JSON解析错误恢复：跳过当前数据")
                // 跳过当前数据解析
            }
            
            is NullPointerException -> {
                Log.i(TAG, "空指针错误恢复：重新初始化相关组件")
                // 重新初始化相关组件
            }
            
            else -> {
                Log.i(TAG, "通用错误恢复：执行垃圾回收")
                System.gc() // 执行垃圾回收
            }
        }
    }

    /**
     * 显示崩溃对话框
     */
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

                    append(name)
                    append(": ")
                    appendLine(value)
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