package com.project.lumina.client.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.project.lumina.client.R
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class AppVerificationManager(
    private val activity: AppCompatActivity,
    private val onVerificationComplete: () -> Unit
) {
    companion object {
        private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
        private const val PREFS_NAME = "app_verification_prefs"
        private const val KEY_NOTICE_HASH = "notice_content_hash"
        private const val KEY_PRIVACY_HASH = "privacy_content_hash"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs: SharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var statusText: TextView
    private lateinit var step1Text: TextView
    private lateinit var step2Text: TextView
    private lateinit var step3Text: TextView
    private lateinit var step4Text: TextView
    private lateinit var step1Progress: CircularProgressIndicator
    private lateinit var step2Progress: CircularProgressIndicator
    private lateinit var step3Progress: CircularProgressIndicator
    private lateinit var step4Progress: CircularProgressIndicator

    private lateinit var noticeContainer: LinearLayout
    private lateinit var privacyContainer: LinearLayout
    private lateinit var updateContainer: LinearLayout

    private var step1Passed = false
    private var step2Passed = false
    private var step3Passed = false
    private var step4Passed = false

    fun startVerification() {
        bindViews()
        initializeStepUI()
        startStep1()
    }

    private fun bindViews() {
        progressIndicator = activity.findViewById(R.id.progress_indicator)
        statusText = activity.findViewById(R.id.status_text)

        step1Text = activity.findViewById(R.id.step1_text)
        step2Text = activity.findViewById(R.id.step2_text)
        step3Text = activity.findViewById(R.id.step3_text)
        step4Text = activity.findViewById(R.id.step4_text)

        step1Progress = activity.findViewById(R.id.step1_progress)
        step2Progress = activity.findViewById(R.id.step2_progress)
        step3Progress = activity.findViewById(R.id.step3_progress)
        step4Progress = activity.findViewById(R.id.step4_progress)

        noticeContainer = activity.findViewById(R.id.notice_container)
        privacyContainer = activity.findViewById(R.id.privacy_container)
        updateContainer = activity.findViewById(R.id.update_container)
    }

    private fun initializeStepUI() {
        progressIndicator.progress = 0
        statusText.text = "应用验证中…"
        setStepStatus(1, StepStatus.IN_PROGRESS, "正在连接服务器")
        setStepStatus(2, StepStatus.WAITING, "等待公告")
        setStepStatus(3, StepStatus.WAITING, "等待隐私协议")
        setStepStatus(4, StepStatus.WAITING, "检查版本")
    }

    private enum class StepStatus { WAITING, IN_PROGRESS, SUCCESS, ERROR }

    private fun setStepStatus(step: Int, status: StepStatus, text: String) {
        val txt = when (step) {
            1 -> step1Text
            2 -> step2Text
            3 -> step3Text
            4 -> step4Text
            else -> return
        }
        val progress = when (step) {
            1 -> step1Progress
            2 -> step2Progress
            3 -> step3Progress
            4 -> step4Progress
            else -> return
        }

        handler.post {
            txt.text = text
            when (status) {
                StepStatus.WAITING -> {
                    progress.visibility = View.GONE
                    txt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                StepStatus.IN_PROGRESS -> {
                    progress.visibility = View.VISIBLE
                    progress.isIndeterminate = true
                    txt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                StepStatus.SUCCESS -> {
                    progress.visibility = View.GONE
                    txt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_24, 0, 0, 0)
                }
                StepStatus.ERROR -> {
                    progress.visibility = View.GONE
                    txt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_24, 0, 0, 0)
                }
            }
        }
    }

    private fun updateProgress() {
        val done = listOf(step1Passed, step2Passed, step3Passed, step4Passed).count { it }
        handler.post {
            progressIndicator.setProgress(done * 100 / 4, true)
            if (done == 4) statusText.text = "验证完成，启动中…"
        }
    }

    private fun startStep1() {
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/appstatus/a.ini")
                handler.postDelayed({
                    if (parseIniStatus(resp)) {
                        step1Passed = true
                        setStepStatus(1, StepStatus.SUCCESS, "服务器连接成功")
                        updateProgress()
                        startStep2()
                    } else {
                        setStepStatus(1, StepStatus.ERROR, "应用状态验证失败")
                        showRetryContent("状态验证失败", "应用当前不可用，请联系开发者", ::startStep1)
                    }
                }, 1000)
            } catch (e: IOException) {
                handler.postDelayed({
                    setStepStatus(1, StepStatus.ERROR, "网络连接失败")
                    showRetryContent("网络错误", "无法连接服务器，请检查网络", ::startStep1)
                }, 1000)
            }
        }
    }

    private fun startStep2() {
        setStepStatus(2, StepStatus.IN_PROGRESS, "获取公告…")
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/title/a.json")
                handler.postDelayed({
                    try {
                        val json = JSONObject(resp)
                        val title = json.getString("title")
                        val subtitle = json.getString("subtitle")
                        val content = json.getString("content")
                        val hash = getSHA256Hash(resp)
                        if (prefs.getString(KEY_NOTICE_HASH, "") != hash) {
                            showNoticeContent(title, subtitle, content, hash)
                        } else {
                            step2Passed = true
                            setStepStatus(2, StepStatus.SUCCESS, "公告已读")
                            updateProgress()
                            startStep3()
                        }
                    } catch (e: Exception) {
                        step2Passed = true
                        setStepStatus(2, StepStatus.ERROR, "公告解析失败，跳过")
                        updateProgress()
                        startStep3()
                    }
                }, 1000)
            } catch (e: IOException) {
                handler.postDelayed({
                    step2Passed = true
                    setStepStatus(2, StepStatus.ERROR, "获取公告失败，跳过")
                    updateProgress()
                    startStep3()
                }, 1000)
            }
        }
    }

    private fun startStep3() {
        setStepStatus(3, StepStatus.IN_PROGRESS, "获取隐私协议…")
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/privary/a.txt")
                handler.postDelayed({
                    val hash = getSHA256Hash(resp)
                    if (prefs.getString(KEY_PRIVACY_HASH, "") != hash) {
                        showPrivacyContent(resp, hash)
                    } else {
                        step3Passed = true
                        setStepStatus(3, StepStatus.SUCCESS, "隐私协议已同意")
                        updateProgress()
                        startStep4()
                    }
                }, 1000)
            } catch (e: IOException) {
                handler.postDelayed({
                    setStepStatus(3, StepStatus.ERROR, "获取协议失败")
                    showRetryContent("隐私协议获取失败", "无法获取隐私协议，这是必需的步骤", ::startStep3)
                }, 1000)
            }
        }
    }

    private fun startStep4() {
        setStepStatus(4, StepStatus.IN_PROGRESS, "检查版本…")
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/update/a.json")
                handler.postDelayed({
                    try {
                        val json = JSONObject(resp)
                        val cloud = json.getLong("version")
                        val local = getLocalVersionCode()
                        if (cloud > local) {
                            showUpdateContent(
                                json.getString("name"),
                                cloud.toString(),
                                json.getString("update_content"),
                                local,
                                cloud
                            )
                        } else {
                            step4Passed = true
                            setStepStatus(4, StepStatus.SUCCESS, "已是最新版本")
                            updateProgress()
                            checkAllStepsComplete()
                        }
                    } catch (e: Exception) {
                        step4Passed = true
                        setStepStatus(4, StepStatus.ERROR, "版本检查失败，跳过")
                        updateProgress()
                        checkAllStepsComplete()
                    }
                }, 1000)
            } catch (e: IOException) {
                handler.postDelayed({
                    step4Passed = true
                    setStepStatus(4, StepStatus.ERROR, "无法获取版本信息，跳过")
                    updateProgress()
                    checkAllStepsComplete()
                }, 1000)
            }
        }
    }

    private fun checkAllStepsComplete() {
        if (step1Passed && step2Passed && step3Passed && step4Passed) {
            handler.postDelayed({
                onVerificationComplete()
            }, 800)
        }
    }

    /* ---------- 左侧内容展示 ---------- */

    private fun showNoticeContent(title: String, subtitle: String, content: String, hash: String) {
        handler.post {
            noticeContainer.removeAllViews()

            val titleView = TextView(activity).apply {
                text = title
                textSize = 18f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, 8)
            }
            val subtitleView = TextView(activity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, 0, 0, 8)
            }
            val contentView = TextView(activity).apply {
                text = content
                textSize = 14f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, 0, 0, 16)
            }
            val agreeBtn = MaterialButton(activity).apply {
                text = "我已阅读"
                setOnClickListener {
                    prefs.edit().putString(KEY_NOTICE_HASH, hash).apply()
                    step2Passed = true
                    setStepStatus(2, StepStatus.SUCCESS, "公告已读")
                    updateProgress()
                    animateOut(noticeContainer) { startStep3() }
                }
            }

            noticeContainer.addView(titleView)
            noticeContainer.addView(subtitleView)
            noticeContainer.addView(contentView)
            noticeContainer.addView(agreeBtn)

            animateIn(noticeContainer)
        }
    }

    private fun showPrivacyContent(content: String, hash: String) {
        handler.post {
            privacyContainer.removeAllViews()

            val titleView = TextView(activity).apply {
                text = "隐私协议"
                textSize = 18f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, 8)
            }
            val contentView = TextView(activity).apply {
                text = content
                textSize = 14f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, 0, 0, 16)
            }
            val agreeBtn = MaterialButton(activity).apply {
                text = "同意"
                setOnClickListener {
                    prefs.edit().putString(KEY_PRIVACY_HASH, hash).apply()
                    step3Passed = true
                    setStepStatus(3, StepStatus.SUCCESS, "隐私协议已同意")
                    updateProgress()
                    animateOut(privacyContainer) { startStep4() }
                }
            }
            val rejectBtn = MaterialButton(activity).apply {
                text = "拒绝"
                setOnClickListener { activity.finish() }
            }

            privacyContainer.addView(titleView)
            privacyContainer.addView(contentView)
            privacyContainer.addView(agreeBtn)
            privacyContainer.addView(rejectBtn)

            animateIn(privacyContainer)
        }
    }

    private fun showUpdateContent(name: String, ver: String, content: String, local: Long, cloud: Long) {
        handler.post {
            updateContainer.removeAllViews()

            val titleView = TextView(activity).apply {
                text = "发现新版本"
                textSize = 18f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, 8)
            }
            val infoView = TextView(activity).apply {
                text = "$name v$ver\n当前版本: $local\n最新版本: $cloud"
                textSize = 14f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, 0, 0, 8)
            }
            val contentView = TextView(activity).apply {
                text = "更新内容：\n$content"
                textSize = 14f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, 0, 0, 16)
            }
            val updateBtn = MaterialButton(activity).apply {
                text = "立即更新"
                setOnClickListener {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://110.42.63.51:39078/apps/apks")))
                    activity.finish()
                }
            }
            val skipBtn = MaterialButton(activity).apply {
                text = "跳过更新"
                setOnClickListener {
                    step4Passed = true
                    setStepStatus(4, StepStatus.SUCCESS, "跳过更新")
                    updateProgress()
                    animateOut(updateContainer) { checkAllStepsComplete() }
                }
            }

            updateContainer.addView(titleView)
            updateContainer.addView(infoView)
            updateContainer.addView(contentView)
            updateContainer.addView(updateBtn)
            updateContainer.addView(skipBtn)

            animateIn(updateContainer)
        }
    }

    private fun showRetryContent(title: String, msg: String, action: () -> Unit) {
        handler.post {
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            val titleView = TextView(activity).apply {
                text = title
                textSize = 16f
                setTextColor(resolveColor(com.google.android.material.R.attr.colorError))
            }
            val msgView = TextView(activity).apply {
                text = msg
                textSize = 14f
                setPadding(0, 8, 0, 16)
            }
            val retryBtn = MaterialButton(activity).apply {
                text = "重试"
                setOnClickListener { action() }
            }
            val exitBtn = MaterialButton(activity).apply {
                text = "退出"
                setOnClickListener { activity.finish() }
            }

            container.addView(titleView)
            container.addView(msgView)
            container.addView(retryBtn)
            container.addView(exitBtn)

            activity.findViewById<LinearLayout>(R.id.left_panel).addView(container)
            animateIn(container)
        }
    }

    /* ---------- 动画 ---------- */

    private fun animateIn(view: View) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate().alpha(1f).setDuration(400).start()
    }

    private fun animateOut(view: View, onEnd: () -> Unit) {
        view.animate().alpha(0f).setDuration(300).withEndAction {
            view.visibility = View.GONE
            onEnd()
        }.start()
    }

    /* ---------- 工具 ---------- */

    private fun makeHttpRequest(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Lumina Android Client")
        conn.connect()
        if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseIniStatus(s: String) = s.contains("status=true", ignoreCase = true)

    private fun getSHA256Hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02X".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun getLocalVersionCode(): Long {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    /* ---------- 主题颜色解析工具 ---------- */

    private fun resolveColor(attr: Int): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        return ContextCompat.getColor(activity, typedValue.resourceId)
    }

    fun onDestroy() {
        executor.shutdownNow()
    }
}
