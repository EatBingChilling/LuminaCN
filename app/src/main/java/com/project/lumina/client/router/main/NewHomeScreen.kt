/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * Material Design 3 Expressive 风格完整重写
 * 横竖屏自适应 · 响应式布局 · 不修改调用方法
 */

package com.project.lumina.client.router.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.constructors.*
import com.project.lumina.client.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.client.overlay.mods.*
import com.project.lumina.client.service.Services
import com.project.lumina.client.util.*
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/* ======================================================
   数据模型
   ====================================================== */
private data class NoticeInfo(val title: String, val message: String, val rawJson: String)
private data class UpdateInfo(val versionName: String, val changelog: String, val url: String)

private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
private const val PREFS_NAME = "app_verification_prefs"
private const val KEY_NOTICE_HASH = "notice_hash"
private const val KEY_PRIVACY_HASH = "privacy_hash"

/* ======================================================
   主入口
   ====================================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(onStartToggle: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val vm: MainScreenViewModel = viewModel()
    val model by vm.captureModeModel.collectAsState()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /* 状态 */
    var isVerifying by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(1) }
    var msg by remember { mutableStateOf("正在连接服务器…") }
    var err by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }

    var notice by remember { mutableStateOf<NoticeInfo?>(null) }
    var privacy by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    var tab by remember { mutableIntStateOf(0) }

    /* 验证流程 */
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var success = true
            try {
                step = 1; msg = "步骤1：连接服务器…"; progress = 0.2f; delay(600)
                makeHttp("$BASE_URL/appstatus/a.ini")
            } catch (e: Exception) {
                err = "服务器连接失败：${e.message}"
                msg = "验证失败，将跳过检查…"
                success = false
            }

            if (success) {
                try {
                    step = 2; msg = "步骤2：获取公告…"; progress = 0.4f; delay(600)
                    val resp = makeHttp("$BASE_URL/title/a.json")
                    if (getSHA(resp) != prefs.getString(KEY_NOTICE_HASH, "")) {
                        val j = JSONObject(resp)
                        val noticeMsg = "${j.optString("subtitle")}\n\n${j.getString("content")}"
                        notice = NoticeInfo(j.getString("title"), noticeMsg, resp)
                    }
                } catch (_: Exception) { }
            }

            if (success) {
                try {
                    step = 3; msg = "步骤3：获取隐私协议…"; progress = 0.6f; delay(600)
                    val resp = makeHttp("$BASE_URL/privary/a.txt")
                    if (getSHA(resp) != prefs.getString(KEY_PRIVACY_HASH, "")) privacy = resp
                } catch (e: Exception) {
                    err = "无法获取隐私协议：${e.message}"
                    msg = "验证失败，将跳过检查…"
                    success = false
                }
            }

            if (success) {
                try {
                    step = 4; msg = "步骤4：检查更新…"; progress = 0.8f; delay(600)
                    val resp = makeHttp("$BASE_URL/update/a.json")
                    val j = JSONObject(resp)
                    val cloud = j.getLong("version")
                    val local = getLocalVersionCode(context)
                    if (cloud > local) update = UpdateInfo(
                        j.getString("name"),
                        j.getString("update_content"),
                        "http://110.42.63.51:39078/apps/apks"
                    )
                } catch (_: Exception) { }
            }

            progress = 1f
            msg = if (success) "验证完成" else "验证流程已跳过"
            delay(800)
            isVerifying = false
            if (!success && err != null) SimpleOverlayNotification.show(
                err ?: "验证失败，应用可能工作不正常",
                NotificationType.ERROR,
                3000
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    listOf(
                        "主仪表盘" to Icons.Filled.Dashboard,
                        "账户" to Icons.Rounded.AccountCircle,
                        "关于" to Icons.Filled.Info,
                        "设置" to Icons.Filled.Settings
                    ).forEachIndexed { idx, (label, icon) ->
                        NavigationBarItem(
                            selected = tab == idx,
                            onClick = { tab = idx },
                            icon = { Icon(icon, label, Modifier.size(24.dp)) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == 0) {
                    val containerColor by animateColorAsState(
                        if (Services.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        tween(300)
                    )
                    ExtendedFloatingActionButton(
                        onClick = onStartToggle,
                        containerColor = containerColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp, pressedElevation = 12.dp
                        ),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            if (Services.isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (Services.isActive) "停止服务" else "启动服务",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { inner ->
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
            ) {
                /* 内容区域 */
                if (isLandscape) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(16.dp)
                        ) {
                            AnimatedContent(
                                targetState = tab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { it } + fadeIn() togetherWith
                                                slideOutHorizontally { -it } + fadeOut()
                                    } else {
                                        slideInHorizontally { -it } + fadeIn() togetherWith
                                                slideOutHorizontally { it } + fadeOut()
                                    }
                                }
                            ) { t ->
                                when (t) {
                                    0 -> MainDashboard()
                                    1 -> AccountPage()
                                    2 -> AboutPage()
                                    3 -> SettingsScreen()
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it } + fadeOut()
                                } else {
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }
                            }
                        ) { t ->
                            when (t) {
                                0 -> MainDashboard()
                                1 -> AccountPage()
                                2 -> AboutPage()
                                3 -> SettingsScreen()
                            }
                        }
                    }
                }

                /* 验证遮罩 */
                val animatedProgress by animateFloatAsState(
                    progress,
                    tween(600, easing = FastOutSlowInEasing)
                )
                AnimatedVisibility(
                    visible = isVerifying,
                    exit = fadeOut(tween(500))
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                        modifier = Modifier.fillMaxSize(),
                        onClick = {}
                    ) {
                        Column(
                            Modifier.fillMaxSize(),
                            Arrangement.Center,
                            Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(msg, style = MaterialTheme.typography.headlineSmall)
                            err?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                /* 弹窗 */
                privacy?.let {
                    PrivacyDialog(it, {
                        prefs.edit().putString(KEY_PRIVACY_HASH, getSHA(it)).apply()
                        privacy = null
                    }) { (context as? Activity)?.finish() }
                }
                notice?.let {
                    NoticeDialog(it) {
                        prefs.edit().putString(KEY_NOTICE_HASH, getSHA(it.rawJson)).apply()
                        notice = null
                    }
                }
                update?.let {
                    UpdateDialog(it, {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
                        update = null
                    }) { update = null }
                }
            }
        }
    }
}

/* ======================================================
   页面内容
   ====================================================== */
@Composable
private fun MainDashboard() {
    val vm: MainScreenViewModel = viewModel()
    val model by vm.captureModeModel.collectAsState()
    val scroll = rememberScrollState()
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = AccountManager.currentAccount != null,
            enter = fadeIn() + slideInVertically()
        ) {
            ElevatedCard(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerHigh),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        Modifier.size(48.dp),
                        CircleShape,
                        colors.primaryContainer
                    ) {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            null,
                            Modifier.padding(8.dp),
                            tint = colors.onPrimaryContainer
                        )
                    }
                    Column {
                        Text("当前账户", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Text(
                            AccountManager.currentAccount?.remark ?: "未选择",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainer),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                Arrangement.spacedBy(16.dp)
            ) {
                Text("服务器配置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                ServerConfigSection(vm, model)
                AnimatedVisibility(model.serverHostName.isNotBlank()) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(16.dp),
                        colors.surfaceContainerHigh
                    ) {
                        Column(
                            Modifier.padding(16.dp)
                        ) {
                            Text("当前服务器", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            Text(model.serverHostName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("端口：${model.serverPort}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (Services.isActive) colors.tertiaryContainer else colors.errorContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val iconScale by animateFloatAsState(if (Services.isActive) 1.2f else 1f)
                val iconColor by animateColorAsState(
                    if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer
                )
                Icon(
                    if (Services.isActive) Icons.Filled.Check else Icons.Filled.Stop,
                    null,
                    tint = iconColor,
                    modifier = Modifier.scale(iconScale)
                )
                Column {
                    Text("服务状态", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (Services.isActive) "运行中" else "已停止",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountPage() {
    val showNotification: (String, NotificationType) -> Unit = { m, t ->
        SimpleOverlayNotification.show(m, t, 3000)
    }
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AccountScreen(showNotification)
    }
}

@Composable
private fun AboutPage() {
    val scroll = rememberScrollState()
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val showTutorial = remember { mutableStateOf(false) }
    var tutorialText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showTutorial.value) {
        if (showTutorial.value && tutorialText == null) {
            tutorialText = try {
                ctx.resources.openRawResource(R.raw.t).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "无法加载教程内容"
            }
        }
    }

    if (showTutorial.value) {
        AlertDialog(
            onDismissRequest = { showTutorial.value = false },
            title = { Text("使用教程") },
            text = {
                Text(
                    tutorialText ?: "正在加载…",
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 400.dp)
                )
            },
            confirmButton = {
                TextButton({ showTutorial.value = false }) { Text("确定") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        Arrangement.spacedBy(16.dp),
        Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerHigh),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("实用工具", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                ToolButton(Icons.Filled.Download, "下载客户端") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://mcapks.net")))
                }
                ToolButton(Icons.Filled.Group, "加入群聊") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/dxqhrjC9Nu")))
                }
                ToolButton(Icons.Filled.Help, "使用教程") { showTutorial.value = true }
            }
        }

        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerHigh),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.about_lumina), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.luminacn_dev), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.lumina_introduction), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.lumina_expectation), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.lumina_compatibility), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.lumina_copyright), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(stringResource(R.string.lumina_team), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/* ======================================================
   子组件
   ====================================================== */
@Composable
private fun ServerConfigSection(vm: MainScreenViewModel, model: com.project.lumina.client.model.CaptureModeModel) {
    val ctx = LocalContext.current
    var ip by remember { mutableStateOf(model.serverHostName) }
    var port by remember { mutableStateOf(model.serverPort.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("服务器IP") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("服务器端口") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                try {
                    vm.selectCaptureModeModel(model.copy(serverHostName = ip, serverPort = port.toInt()))
                    SimpleOverlayNotification.show("服务器配置已保存", NotificationType.SUCCESS, 2000)
                } catch (e: NumberFormatException) {
                    SimpleOverlayNotification.show("端口格式错误", NotificationType.ERROR, 2000)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) { Text("保存配置", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, text, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/* ======================================================
   Dialog 组件
   ====================================================== */
@Composable
private fun PrivacyDialog(text: String, onAgree: () -> Unit, onDisagree: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("隐私协议更新") },
        text = {
            Column {
                Text("请阅读并同意更新后的隐私协议", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text,
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("同意并继续") }
        },
        dismissButton = {
            TextButton(onClick = onDisagree) { Text("不同意并退出") }
        }
    )
}

@Composable
private fun NoticeDialog(info: NoticeInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(info.title) },
        text = {
            Text(
                info.message,
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 300.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("我已了解") }
        }
    )
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本：${info.versionName}") },
        text = {
            Text(
                info.changelog,
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 200.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("立即更新") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("稍后") }
        }
    )
}

/* ======================================================
   网络/哈希工具
   ====================================================== */
private suspend fun makeHttp(url: String): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        setRequestProperty("User-Agent", "Lumina Android Client")
    }
    if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
    conn.inputStream.bufferedReader().use { it.readText() }
}

private fun getSHA(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }

@Suppress("DEPRECATION")
private fun getLocalVersionCode(context: Context): Long {
    val pi = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        pi.longVersionCode
    } else {
        pi.versionCode.toLong()
    }
}
