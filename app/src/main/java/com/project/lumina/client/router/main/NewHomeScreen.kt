/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.view.KeyEvent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.project.lumina.client.R
import com.project.lumina.client.constructors.*
import com.project.lumina.client.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.KeyCaptureService
import com.project.lumina.client.service.Services
import com.project.lumina.client.util.*
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/* -------------------- 数据类 & 常量 -------------------- */
data class NoticeInfo(val title: String, val message: String, val rawJson: String)
data class UpdateInfo(val versionCode: Int, val versionName: String, val changelog: String, val url: String)

private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
private const val PREFS_NAME = "app_verification_prefs"
private const val KEY_NOTICE = "notice_hash"
private const val KEY_PRIVACY = "privacy_hash"

/* ======================================================
   主入口：NewHomeScreen
   ====================================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(onStartToggle: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: MainScreenViewModel = viewModel()
    val model by vm.captureModeModel.collectAsState()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /* 状态 */
    var isVerifying by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(1) }
    var msg by remember { mutableStateOf("正在连接服务器...") }
    var err by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }

    var notice by remember { mutableStateOf<NoticeInfo?>(null) }
    var privacy by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    var tab by remember { mutableIntStateOf(0) }

    /* 验证流程 */
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                step = 1; progress = 0.2f; delay(500); makeHttp("$BASE_URL/appstatus/a.ini")
                step = 2; progress = 0.4f; delay(500)
                makeHttp("$BASE_URL/title/a.json").let { resp ->
                    if (getSHA(resp) != prefs.getString(KEY_NOTICE, "")) {
                        val j = JSONObject(resp)
                        notice = NoticeInfo(j.getString("title"), j.getString("message"), resp)
                    }
                }
                step = 3; progress = 0.6f; delay(500)
                makeHttp("$BASE_URL/privary/a.txt").let { resp ->
                    if (getSHA(resp) != prefs.getString(KEY_PRIVACY, "")) privacy = resp
                }
                step = 4; progress = 0.8f; delay(500)
                makeHttp("$BASE_URL/update/a.json").let { resp ->
                    val j = JSONObject(resp)
                    @Suppress("DEPRECATION")
                    val cur = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                    if (j.getInt("versionCode") > cur) {
                        update = UpdateInfo(
                            j.getInt("versionCode"),
                            j.getString("versionName"),
                            j.getString("changelog"),
                            j.getString("url")
                        )
                    }
                }
                progress = 1f; msg = "验证完成"; delay(800); isVerifying = false
            } catch (e: Exception) {
                err = e.message; msg = "验证失败，跳过..."; delay(1500); isVerifying = false
                SimpleOverlayNotification.show("验证失败，应用可能工作不正常", NotificationType.ERROR, 3000)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("主仪表盘" to Icons.Filled.Dashboard,
                        "账户"   to Icons.Rounded.AccountCircle,
                        "关于"   to Icons.Filled.Info,
                        "设置"   to Icons.Filled.Settings).forEachIndexed { idx, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == idx,
                        onClick = { tab = idx },
                        icon = { Icon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(
                onClick = onStartToggle,
                containerColor = if (Services.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (Services.isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (Services.isActive) "停止" else "开始"
                )
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    // FIX 1: Replaced deprecated SlideDirection with modern slide transitions.
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "tab"
            ) { t ->
                when (t) {
                    0 -> MainDashboard()
                    1 -> AccountPage()
                    2 -> AboutPage()
                    3 -> SettingsScreen()
                }
            }

            /* 验证遮罩 */
            if (isVerifying) Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    Arrangement.Center,
                    Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(progress = { progress }, Modifier.width(200.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(msg, style = MaterialTheme.typography.titleMedium)
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }

            /* 弹窗 */
            privacy?.let {
                PrivacyDialog(it, {
                    prefs.edit().putString(KEY_PRIVACY, getSHA(it)).apply()
                    privacy = null
                }) { (context as? Activity)?.finish() }
            }
            notice?.let {
                NoticeDialog(it) {
                    prefs.edit().putString(KEY_NOTICE, getSHA(it.rawJson)).apply()
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

/* ======================================================
   页面内容（完整保留）
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
        AnimatedVisibility(AccountManager.currentAccount != null) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(Modifier.size(48.dp), CircleShape, colors.primary.copy(alpha = 0.1f)) {
                        Icon(Icons.Rounded.AccountCircle, null, Modifier.padding(8.dp).size(32.dp), tint = colors.primary)
                    }
                    Column {
                        Text("当前账户", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Text(AccountManager.currentAccount?.remark ?: "未选择账户", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

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

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (Services.isActive) colors.tertiaryContainer else colors.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconScale by animateFloatAsState(if (Services.isActive) 1.2f else 1f, label = "")
                val iconColor by animateColorAsState(if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer, label = "")
                Icon(
                    if (Services.isActive) Icons.Filled.Check else Icons.Filled.Stop,
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

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
                Text("物理按键绑定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val elements = GameManager.elements
                if (elements.none { it.values.any { v -> v is BoolValue } }) {
                    Text("暂无可绑定的功能", style = MaterialTheme.typography.bodyMedium)
                } else {
                    elements.forEach { el ->
                        el.values.filterIsInstance<BoolValue>().forEach { bool ->
                            KeyBindingItem(bool, el)
                        }
                    }
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
        // FIX 2: Used named arguments for `modifier` and `colors` to resolve ambiguity.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("实用工具", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
                Spacer(Modifier.height(8.dp))
                ToolButton(Icons.Filled.Download, "下载客户端") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://mcapks.net")))
                }
                ToolButton(Icons.Filled.Group, "加入群聊") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/dxqhrjC9Nu")))
                }
                ToolButton(Icons.Filled.Help, "使用教程") { showTutorial.value = true }
            }
        }

        // FIX 3: Used named arguments for `modifier` and `colors` here as well.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
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
private fun KeyBindingItem(bool: BoolValue, element: Element) {
    val ctx = LocalContext.current
    var binding by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.getParcelableExtra<KeyEvent>(KeyCaptureService.EXTRA_KEY_EVENT)?.let { ev ->
                    if (ev.action == KeyEvent.ACTION_DOWN) {
                        KeyBindingManager.setBinding(bool.name, ev.keyCode)
                        binding = false
                    }
                }
            }
        }
    }

    DisposableEffect(binding) {
        if (binding) LocalBroadcastManager.getInstance(ctx).registerReceiver(
            receiver,
            IntentFilter(KeyCaptureService.ACTION_KEY_EVENT)
        )
        onDispose { if (binding) LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver) }
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("${element.name} - ${bool.name}", style = MaterialTheme.typography.bodyMedium)
            Text(
                KeyBindingManager.getBinding(bool.name)?.let { "按键: ${KeyEvent.keyCodeToString(it)}" } ?: "未绑定",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton({ binding = !binding }) {
                Icon(if (binding) Icons.Filled.Check else Icons.Filled.Keyboard, null)
            }
            Switch(
                checked = bool.value,
                onCheckedChange = { bool.value = it },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
            )
        }
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

@Composable
private fun SocialMediaIcon(icon: Any, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        when (icon) {
            is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            is androidx.compose.ui.graphics.vector.ImageVector -> Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
        text = { Text(info.message, Modifier.verticalScroll(rememberScrollState()).heightIn(max = 300.dp)) },
        confirmButton = { Button(onDismiss) { Text("我已了解") } }
    )
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本: ${info.versionName}") },
        text = { Text(info.changelog, Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)) },
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

