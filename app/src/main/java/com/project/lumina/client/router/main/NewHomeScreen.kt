/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */
package com.project.lumina.client.router.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.util.*
import com.project.lumina.client.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.Services
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/* -------------------- 数据类 & 常量 -------------------- */
data class NoticeInfo(val title: String, val message: String, val rawJson: String)
data class UpdateInfo(val versionName: String, val changelog: String, val url: String)

private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
private const val PREFS_NAME = "app_verification_prefs"
private const val KEY_NOTICE_HASH = "notice_hash"
private const val KEY_PRIVACY_HASH = "privacy_hash"

/* ======================================================
   主入口：NewHomeScreen
   ====================================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(onStartToggle: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val vm: MainScreenViewModel = viewModel()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val settingsPrefs = remember { context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    /* 状态 */
    var isVerifying by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf("正在连接服务器...") }
    var err by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<NoticeInfo?>(null) }
    var privacy by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var tab by remember { mutableIntStateOf(0) }

    /* 当前选中的游戏包名 */
    var selectedGamePackage by remember {
        mutableStateOf(settingsPrefs.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe")
    }
    val localIp = remember { ConnectionInfoOverlay.getLocalIpAddress(context) }

    /* 验证流程 */
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var ok = true
            try { msg = "步骤1: 连接服务器..."; progress = 0.2f; makeHttp("$BASE_URL/appstatus/a.ini") }
            catch (e: Exception) { err = "服务器连接失败"; msg = "验证失败，将跳过检查..."; ok = false }
            if (ok) try { msg = "步骤2: 获取公告..."; progress = 0.4f
                val r = makeHttp("$BASE_URL/title/a.json")
                if (getSHA(r) != prefs.getString(KEY_NOTICE_HASH, "")) {
                    val j = JSONObject(r)
                    notice = NoticeInfo(j.getString("title"),
                        "${j.optString("subtitle","")}\n\n${j.getString("content")}", r)
                }
            } catch (_: Exception) {}
            if (ok) try { msg = "步骤3: 获取隐私协议..."; progress = 0.6f
                val r = makeHttp("$BASE_URL/privary/a.txt")
                if (getSHA(r) != prefs.getString(KEY_PRIVACY_HASH, "")) privacy = r
            } catch (e: Exception) { err = "无法获取隐私协议"; msg = "验证失败，将跳过检查..."; ok = false }
            if (ok) try { msg = "步骤4: 检查更新..."; progress = 0.8f
                val r = makeHttp("$BASE_URL/update/a.json")
                val j = JSONObject(r)
                if (j.getLong("version") > getLocalVersionCode(context)) {
                    update = UpdateInfo(j.getString("name"), j.getString("update_content"),
                        "http://110.42.63.51:39078/apps/apks")
                }
            } catch (_: Exception) {}
            progress = 1f; msg = if (ok) "验证完成" else "验证流程已跳过"
            delay(800); isVerifying = false
            if (!ok && err != null) SimpleOverlayNotification.show(err!!, NotificationType.ERROR, 3000)
        }
    }

    /* FAB 启动逻辑：自动拉起已选包名 */
    val launchStopAction: () -> Unit = {
        if (Services.isActive) {
            onStartToggle()
        } else {
            scope.launch {
                Services.isLaunchingMinecraft = true
                onStartToggle()
                delay(2500)
                if (!Services.isActive) { Services.isLaunchingMinecraft = false; return@launch }
                val intent = context.packageManager.getLaunchIntentForPackage(selectedGamePackage)
                if (intent != null) {
                    context.startActivity(intent)
                    delay(3000)
                    if (Services.isActive && !settingsPrefs.getBoolean("disableConnectionInfoOverlay", false)) {
                        ConnectionInfoOverlay.show(localIp)
                    }
                } else {
                    SimpleOverlayNotification.show("未安装指定游戏客户端", NotificationType.ERROR)
                }
                Services.isLaunchingMinecraft = false
            }
        }
    }

    /* -------------------- 布局 -------------------- */
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { if (!isLandscape) BottomBar(tab) { tab = it } },
        floatingActionButton = {
            if (!isLandscape && tab == 0) {
                FloatingActionButton(onClick = launchStopAction) {
                    Icon(
                        if (Services.isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
            }
        }
    ) { inner ->
        Row(Modifier.fillMaxSize().padding(inner)) {
            if (isLandscape) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    /* 让 NavigationRail 占满高度，FAB 放在最上方 */
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(onClick = launchStopAction) {
                            Icon(
                                if (Services.isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        listOf(Icons.Default.Home to 0,
                               Icons.Rounded.AccountCircle to 1,
                               Icons.Default.Info to 2,
                               Icons.Default.Settings to 3).forEach { (ic, idx) ->
                            NavigationRailItem(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                icon = { Icon(ic, null) },
                                label = { Text(listOf("主页","账户","关于","设置")[idx]) }
                            )
                        }
                    }
                }
            }

            MainContentArea(
                modifier = if (isLandscape) Modifier.weight(1f) else Modifier.fillMaxSize(),
                currentTab = tab,
                isVerifying = isVerifying,
                progress = progress,
                msg = msg,
                err = err,
                privacy = privacy,
                notice = notice,
                update = update,
                onPrivacyAgreed = { privacy = null; prefs.edit().putString(KEY_PRIVACY_HASH, getSHA(it)).apply() },
                onPrivacyDisagreed = { (context as? Activity)?.finish() },
                onNoticeDismissed = { notice = null; prefs.edit().putString(KEY_NOTICE_HASH, getSHA(it.rawJson)).apply() },
                onUpdate = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url))); update = null },
                onUpdateDismissed = { update = null }
            )
        }
    }
}

/* -------------------- 下面保持原样 -------------------- */
@Composable
private fun BottomBar(current: Int, onTab: (Int) -> Unit) {
    NavigationBar {
        listOf("主页" to Icons.Default.Home,
               "账户" to Icons.Rounded.AccountCircle,
               "关于" to Icons.Default.Info,
               "设置" to Icons.Default.Settings).forEachIndexed { idx, (label, icon) ->
            NavigationBarItem(
                selected = current == idx,
                onClick = { onTab(idx) },
                icon = { Icon(icon, label) },
                label = { Text(label) }
            )
        }
    }
}

/* ======================================================
   主内容区域
   ====================================================== */
@Composable
private fun MainContentArea(
    modifier: Modifier,
    currentTab: Int,
    isVerifying: Boolean,
    progress: Float,
    msg: String,
    err: String?,
    privacy: String?,
    notice: NoticeInfo?,
    update: UpdateInfo?,
    onPrivacyAgreed: (String) -> Unit,
    onPrivacyDisagreed: () -> Unit,
    onNoticeDismissed: (NoticeInfo) -> Unit,
    onUpdate: (UpdateInfo) -> Unit,
    onUpdateDismissed: () -> Unit
) {
    Box(modifier) {
        /* 主页面切换 */
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "tab-content"
        ) { t ->
            when (t) {
                0 -> MainDashboard()
                1 -> AccountPage()
                2 -> AboutPage()
                3 -> SettingsScreen()
            }
        }

        /* 验证遮罩 */
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "verificationProgress"
        )
        AnimatedVisibility(
            visible = isVerifying,
            exit = fadeOut(tween(500))
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    Arrangement.Center,
                    Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.width(200.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(msg, style = MaterialTheme.typography.titleMedium)
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        /* 弹窗 */
        privacy?.let { PrivacyDialog(it, onPrivacyAgreed, onPrivacyDisagreed) }
        notice?.let { NoticeDialog(it, onNoticeDismissed) }
        update?.let { UpdateDialog(it, onUpdate, onUpdateDismissed) }
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
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        /* 当前账户 */
        AnimatedVisibility(AccountManager.currentAccount != null) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(Modifier.size(48.dp), CircleShape, colors.primary.copy(alpha = 0.1f)) {
                        Icon(Icons.Rounded.AccountCircle, null, Modifier
                            .padding(8.dp)
                            .size(32.dp), tint = colors.primary)
                    }
                    Column {
                        Text("当前账户", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Text(AccountManager.currentAccount?.remark ?: "未选择账户", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        /* 服务器配置卡片（从 SettingsScreen 搬回） */
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
                Text("服务器配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ServerConfigSection(vm, model)
                AnimatedVisibility(model.serverHostName.isNotBlank()) {
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(8.dp), colors.surface) {
                        Column(Modifier.padding(12.dp)) {
                            Text("当前服务器", style = MaterialTheme.typography.bodySmall)
                            Text(model.serverHostName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("端口: ${model.serverPort}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        /* 服务状态 */
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (Services.isActive) colors.tertiaryContainer else colors.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconScale by animateFloatAsState(if (Services.isActive) 1.2f else 1f, label = "")
                val iconColor by animateColorAsState(
                    if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer,
                    label = ""
                )
                Icon(
                    if (Services.isActive) Icons.Default.Check else Icons.Default.Stop,
                    null,
                    tint = iconColor,
                    modifier = Modifier.scale(iconScale)
                )
                Column {
                    Text("服务状态", style = MaterialTheme.typography.bodySmall)
                    Text(if (Services.isActive) "运行中" else "已停止", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AccountPage() {
    AccountScreen { m, t -> SimpleOverlayNotification.show(m, t, 3000) }
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
            } catch (e: Exception) { "无法加载教程内容" }
        }
    }
    if (showTutorial.value) {
        AlertDialog(
            onDismissRequest = { showTutorial.value = false },
            title = { Text("使用教程") },
            text = { Text(tutorialText ?: "正在加载...") },
            confirmButton = { TextButton({ showTutorial.value = false }) { Text("确定") } }
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("实用工具", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
                Spacer(Modifier.height(8.dp))
                ToolButton(Icons.Default.Download, "下载客户端") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://mcapks.net")))
                }
                ToolButton(Icons.Default.Group, "加入群聊") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/dxqhrjC9Nu")))
                }
                ToolButton(Icons.Default.Help, "使用教程") { showTutorial.value = true }
            }
        }
        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow)
        ) {
            Column(Modifier.padding(24.dp), Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.about_lumina), style = MaterialTheme.typography.headlineMedium, color = colors.primary)
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
   子组件 & 工具
   ====================================================== */
@Composable
private fun ServerConfigSection(vm: MainScreenViewModel, model: com.project.lumina.client.model.CaptureModeModel) {
    val ctx = LocalContext.current
    var ip by remember(model.serverHostName) { mutableStateOf(model.serverHostName) }
    var port by remember(model.serverPort) { mutableStateOf(model.serverPort.toString()) }
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("服务器IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("服务器端口") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) { Text("保存配置") }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, text, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
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
                Text("请阅读并同意更新后的隐私协议", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(text, Modifier.verticalScroll(rememberScrollState()))
                }
            }
        },
        confirmButton = { Button(onClick = onAgree) { Text("同意并继续") } },
        dismissButton = { TextButton(onClick = onDisagree) { Text("不同意并退出") } }
    )
}

@Composable
private fun NoticeDialog(info: NoticeInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(info.title) },
        text = { Text(info.message, Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(max = 300.dp)) },
        confirmButton = { Button(onDismiss) { Text("我已了解") } }
    )
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本: ${info.versionName}") },
        text = { Text(info.changelog, Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(max = 200.dp)) },
        confirmButton = { Button(onUpdate) { Text("立即更新") } },
        dismissButton = { TextButton(onDismiss) { Text("稍后") } }
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
        connect()
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
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
}
