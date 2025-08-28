package com.phoenix.luminacn.application

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.phoenix.luminacn.activity.CrashHandlerActivity
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.KeyBindingManager
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.shiyi.RenderOverlay
import com.phoenix.luminacn.shiyi.ArrayListOverlay
import com.phoenix.luminacn.ui.theme.ThemeManager
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
    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化按键绑定管理器
        KeyBindingManager.init(this)

        // 设置崩溃处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        try {
            initializeCore()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppContext", e)
        }
    }

    private fun initializeCore() {
        // 1. 首先初始化主题管理器
        initializeThemeManager()
        
        // 2. 检查权限并启动服务
        checkOverlayPermissionAndStartServices()
        
        // 3. 延迟初始化悬浮窗
        postDelayedInitialization()
        
        isInitialized = true
        Log.d(TAG, "AppContext initialized successfully")
    }

    private fun initializeThemeManager() {
        try {
            themeManager = ThemeManager(this)
            Log.d(TAG, "ThemeManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ThemeManager", e)
            try {
                themeManager = ThemeManager(applicationContext)
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Even fallback ThemeManager failed", fallbackException)
                throw fallbackException
            }
        }
    }

    // ====================== 【权限和服务管理】 ======================
    fun checkOverlayPermissionAndStartServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.d("AppContext", "Overlay permission already granted. Starting services.")
                startServices()
            } else {
                Log.w("AppContext", "Overlay permission not granted. Services requiring it will not start.")
            }
        } else {
            Log.d("AppContext", "Device is pre-Marshmallow. Starting services directly.")
            startServices()
        }
    }

    private fun startServices() {
        Log.d("AppContext", "Attempting to start services.")
        try {
            // 这里可以启动其他需要的服务
            // 例如：startService(Intent(this, SomeOtherService::class.java))
            Log.i("AppContext", "Services started successfully.")
        } catch (e: Exception) {
            Log.e("AppContext", "Failed to start services.", e)
        }
    }

    private fun postDelayedInitialization() {
        handler.postDelayed({
            try {
                initializeOverlays()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize overlays", e)
            }
        }, 500)
    }

    private fun initializeOverlays() {
        try {
            OverlayManager.show(this)
            initializeRenderOverlay()
            initializeArrayListOverlay()
            Log.d(TAG, "Overlays initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize overlays", e)
        }
    }

    private fun initializeRenderOverlay() {
        try {
            GameManager.netBound?.let { session ->
                RenderOverlay.setSession(session)
                Log.d(TAG, "RenderOverlay session set")
            } ?: run {
                Log.w(TAG, "GameManager netBound not available yet")
            }
            
            RenderOverlay.setOverlayEnabled(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RenderOverlay", e)
        }
    }

    private fun initializeArrayListOverlay() {
        try {
            ArrayListOverlay.setOverlayEnabled(true)
            updateModuleListSafely()
            Log.d(TAG, "ArrayListOverlay initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ArrayListOverlay", e)
        }
    }

    private fun updateModuleListSafely() {
        try {
            val elements = GameManager.elements
            if (elements.isNotEmpty()) {
                val moduleList = elements
                    .filter { element -> element.isEnabled }
                    .map { element ->
                        ArrayListOverlay.ModuleInfo(
                            name = element.name,
                            category = element.category?.name ?: "Unknown",
                            isEnabled = element.isEnabled,
                            priority = 0
                        )
                    }
                
                ArrayListOverlay.setModules(moduleList)
                Log.d(TAG, "Module list updated: ${moduleList.size} modules")
            } else {
                Log.w(TAG, "No modules available to update")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update module list", e)
        }
    }

    // ====================== 【崩溃处理逻辑】 ======================
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
                stackTrace.contains("com.phoenix.luminacn.overlay") ||
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

    // ====================== 【公共接口】 ======================
    fun updateSession(session: com.phoenix.luminacn.constructors.NetBound?) {
        try {
            RenderOverlay.setSession(session)
            Log.d(TAG, "Session updated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update session", e)
        }
    }

    fun onModuleStateChanged() {
        if (isInitialized) {
            updateModuleListSafely()
        }
    }

    fun getContext(): Context = this

    fun isInitialized(): Boolean = isInitialized

    fun cleanup() {
        try {
            OverlayManager.dismiss()
            Log.d(TAG, "Resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup resources", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "Memory trim requested: level $level")
    }
}