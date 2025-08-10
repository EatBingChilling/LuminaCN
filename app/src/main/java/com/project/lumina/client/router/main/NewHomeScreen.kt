/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

package com.project.lumina.client.router.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.constructors.*
import com.project.lumina.client.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.Services
import com.project.lumina.client.util.*
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class NoticeInfo(val title: String, val message: String, val rawJson: String)
data class UpdateInfo(val versionName: String, val changelog: String, val url: String)

private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
private const val PREFS_NAME = "app_verification_prefs"
private const val KEY_NOTICE_HASH = "notice_hash"
private const val KEY_PRIVACY_HASH = "privacy_hash"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(onStartToggle: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val vm: MainScreenViewModel = viewModel()
    val model by vm.captureModeModel.collectAsState()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isVerifying by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf("正在连接服务器...") }
    var err by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }

    var notice by remember { mutableStateOf<NoticeInfo?>(null) }
    var privacy by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    var tab by remember { mutableIntStateOf(0) }

    val settingsPrefs = remember { context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE) }
    val localIp = remember { ConnectionInfoOverlay.getLocalIpAddress(context) }

    var injectNekoPack by remember { mutableStateOf(settingsPrefs.getBoolean("injectNekoPackEnabled", false)) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var currentPackName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var allStepsSuccess = true
            try {
                msg = "步骤1: 连接服务器..."; progress = 0.2f; delay(500)
                makeHttp("$BASE_URL/appstatus/a.ini")
            } catch (e: Exception) { err = "服务器连接失败: ${e.message}"; msg = "验证失败，将跳过检查..."; allStepsSuccess = false }
            if (allStepsSuccess) try {
                msg = "步骤2: 获取公告..."; progress = 0.4f; delay(500)
                val resp = makeHttp("$BASE_URL/title/a.json")
                if (getSHA(resp) != prefs.getString(KEY_NOTICE_HASH, "")) {
                    val j = JSONObject(resp); notice = NoticeInfo(j.getString("title"), "${j.optString("subtitle", "")}\n\n${j.getString("content")}", resp)
                }
            } catch (e: Exception) { println("Failed to fetch notice: ${e.message}") }
            if (allStepsSuccess) try {
                msg = "步骤3: 获取隐私协议..."; progress = 0.6f; delay(500)
                val resp = makeHttp("$BASE_URL/privary/a.txt")
                if (getSHA(resp) != prefs.getString(KEY_PRIVACY_HASH, "")) privacy = resp
            } catch (e: Exception) { err = "无法获取隐私协议: ${e.message}"; msg = "验证失败，将跳过检查..."; allStepsSuccess = false }
            if (allStepsSuccess) try {
                msg = "步骤4: 检查更新..."; progress = 0.8f; delay(500)
                val resp = makeHttp("$BASE_URL/update/a.json")
                val j = JSONObject(resp)
                if (j.getLong("version") > getLocalVersionCode(context)) {
                    update = UpdateInfo(j.getString("name"), j.getString("update_content"), "http://110.42.63.51:39078/apps/apks")
                }
            } catch (e: Exception) { println("Failed to check for updates: ${e.message}") }

            progress = 1f
            msg = if (allStepsSuccess) "验证完成" else "验证流程已跳过"
            delay(800)
            isVerifying = false
            if (!allStepsSuccess && err != null) SimpleOverlayNotification.show(err ?: "验证失败", NotificationType.ERROR, 3000)
        }
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key -> if (key == "injectNekoPackEnabled") injectNekoPack = sp.getBoolean(key, false) }
        settingsPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { settingsPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val launchStopAction = {
        if (Services.isActive) onStartToggle()
        else scope.launch {
            Services.isLaunchingMinecraft = true; onStartToggle(); delay(2500)
            if (!Services.isActive) { Services.isLaunchingMinecraft = false; return@launch }
            val selectedGame = vm.selectedGame.value
            if (selectedGame != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                if (intent != null) {
                    context.startActivity(intent); delay(3000)
                    if (Services.isActive && !settingsPrefs.getBoolean("disableConnectionInfoOverlay", false)) ConnectionInfoOverlay.show(localIp)
                    Services.isLaunchingMinecraft = false
                    try {
                        when {
                            injectNekoPack && PackSelectionManager.selectedPack != null -> PackSelectionManager.selectedPack?.let { pack ->
                                currentPackName = pack.name; showProgressDialog = true; downloadProgress = 0f
                                try { MCPackUtils.downloadAndOpenPack(context, pack) { p -> downloadProgress = p }; showProgressDialog = false }
                                catch (e: Exception) { showProgressDialog = false; SimpleOverlayNotification.show("材质包下载失败: ${e.message}", NotificationType.ERROR) }
                            }
                            injectNekoPack -> try { InjectNeko.injectNeko(context) {} } catch (e: Exception) { SimpleOverlayNotification.show("Neko 注入失败: ${e.message}", NotificationType.ERROR) }
                            selectedGame == "com.mojang.minecraftpe" -> try { ServerInit.addMinecraftServer(context, localIp) } catch (e: Exception) { SimpleOverlayNotification.show("服务器初始化失败: ${e.message}", NotificationType.ERROR) }
                        }
                    } catch (e: Exception) { SimpleOverlayNotification.show("一个未预料的错误发生: ${e.message}", NotificationType.ERROR) }
                } else SimpleOverlayNotification.show("游戏启动失败", NotificationType.ERROR)
            } else SimpleOverlayNotification.show("请在设置中选择一个游戏客户端", NotificationType.WARNING)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { if (!isLandscape) AppBottomNavigationBar(currentTab = tab, onTabSelected = { tab = it }) },
        floatingActionButton = { if (!isLandscape && tab == 0) LaunchStopFAB(onClick = launchStopAction) }
    ) { innerPadding ->
        if (isLandscape) {
            Row(Modifier.fillMaxSize().padding(innerPadding)) {
                AppNavigationRail(currentTab = tab, onTabSelected = { tab = it }, onStartToggle = launchStopAction)
                MainContentArea(Modifier.weight(1f), tab, isVerifying, progress, msg, err, showProgressDialog, downloadProgress, currentPackName, privacy, notice, update,
                    { p -> prefs.edit().putString(KEY_PRIVACY_HASH, getSHA(p)).apply(); privacy = null }, { (context as? Activity)?.finish() },
                    { n -> prefs.edit().putString(KEY_NOTICE_HASH, getSHA(n.rawJson)).apply(); notice = null },
                    { u -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))); update = null }, { update = null })
            }
        } else {
            MainContentArea(Modifier.padding(innerPadding), tab, isVerifying, progress, msg, err, showProgressDialog, downloadProgress, currentPackName, privacy, notice, update,
                { p -> prefs.edit().putString(KEY_PRIVACY_HASH, getSHA(p)).apply(); privacy = null }, { (context as? Activity)?.finish() },
                { n -> prefs.edit().putString(KEY_NOTICE_HASH, getSHA(n.rawJson)).apply(); notice = null },
                { u -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))); update = null }, { update = null })
        }
    }
}

@Composable
private fun LaunchStopFAB(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, containerColor = if (Services.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
        Icon(if (Services.isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (Services.isActive) "停止" else "开始")
    }
}

@Composable
private fun AppBottomNavigationBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        listOf("主页" to Icons.Filled.Dashboard, "账户" to Icons.Rounded.AccountCircle, "关于" to Icons.Filled.Info, "设置" to Icons.Filled.Settings).forEachIndexed { idx, (label, icon) ->
            NavigationBarItem(selected = currentTab == idx, onClick = { onTabSelected(idx) }, icon = { Icon(icon, label) }, label = { Text(label) })
        }
    }
}

@Composable
private fun AppNavigationRail(currentTab: Int, onTabSelected: (Int) -> Unit, onStartToggle: () -> Unit) {
    NavigationRail(modifier = Modifier.padding(horizontal = 8.dp), header = { if (currentTab == 0) LaunchStopFAB(onClick = onStartToggle) }) {
        Spacer(Modifier.height(32.dp))
        listOf("主页" to Icons.Filled.Dashboard, "账户" to Icons.Rounded.AccountCircle, "关于" to Icons.Filled.Info, "设置" to Icons.Filled.Settings).forEachIndexed { idx, (label, icon) ->
            NavigationRailItem(selected = currentTab == idx, onClick = { onTabSelected(idx) }, icon = { Icon(icon, label) }, label = { Text(label) }, alwaysShowLabel = false)
        }
    }
}

@Composable
private fun MainContentArea(
    modifier: Modifier, currentTab: Int, isVerifying: Boolean, progress: Float, msg: String, err: String?,
    showProgressDialog: Boolean, downloadProgress: Float, currentPackName: String, privacy: String?, notice: NoticeInfo?, update: UpdateInfo?,
    onPrivacyAgreed: (String) -> Unit, onPrivacyDisagreed: () -> Unit, onNoticeDismissed: (NoticeInfo) -> Unit,
    onUpdate: (UpdateInfo) -> Unit, onUpdateDismissed: () -> Unit
) {
    Box(modifier) {
        AnimatedContent(targetState = currentTab, transitionSpec = {
            if (targetState > initialState) slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            else slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
        }, label = "tab-content") { t ->
            when (t) {
                0 -> MainDashboard(); 1 -> AccountPage(); 2 -> AboutPage(); 3 -> SettingsScreen()
            }
        }

        val animatedProgress by animateFloatAsState(progress, tween(600, easing = FastOutSlowInEasing), label = "verificationProgress")
        AnimatedVisibility(visible = isVerifying, exit = fadeOut(tween(500))) {
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f), modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    LinearProgressIndicator({ animatedProgress }, Modifier.width(200.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(msg, style = MaterialTheme.typography.titleMedium); err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        if (showProgressDialog) Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.padding(16.dp).wrapContentSize()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "正在下载: $currentPackName", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator({ downloadProgress }, modifier = Modifier.size(48.dp))
                    Text(text = "${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Text(text = if (downloadProgress < 1f) "正在下载..." else "正在启动...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        privacy?.let { PrivacyDialog(it, { onPrivacyAgreed(it) }, onPrivacyDisagreed) }
        notice?.let { NoticeDialog(it) { onNoticeDismissed(it) } }
        update?.let { UpdateDialog(it, { onUpdate(it) }, onUpdateDismissed) }
    }
}

@Composable
private fun MainDashboard() {
    val vm: MainScreenViewModel = viewModel()
    val model by vm.captureModeModel.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(AccountManager.currentAccount != null) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
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
        Card(colors = CardDefaults.cardColors(containerColor = if (Services.isActive) colors.tertiaryContainer else colors.errorContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp).animateContentSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val iconScale by animateFloatAsState(if (Services.isActive) 1.2f else 1f, label = "")
                val iconColor by animateColorAsState(if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer, label = "")
                Icon(if (Services.isActive) Icons.Filled.Check else Icons.Filled.Stop, null, tint = iconColor, modifier = Modifier.scale(iconScale))
                Column {
                    Text("服务状态", style = MaterialTheme.typography.bodySmall)
                    Text(if (Services.isActive) "运行中" else "已停止", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable private fun AccountPage() { AccountScreen { m, t -> SimpleOverlayNotification.show(m, t, 3000) } }
@Composable private fun AboutPage() { /* AboutScreen() - Assuming this exists elsewhere */ }

@Composable
private fun ServerConfigSection(vm: MainScreenViewModel, model: com.project.lumina.client.model.CaptureModeModel) {
    var ip by remember(model.serverHostName) { mutableStateOf(model.serverHostName) }
    var port by remember(model.serverPort) { mutableStateOf(model.serverPort.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("服务器IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("服务器端口") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            try {
                vm.selectCaptureModeModel(model.copy(serverHostName = ip, serverPort = port.toInt()))
                SimpleOverlayNotification.show("配置已保存", NotificationType.SUCCESS, 2000)
            } catch (e: NumberFormatException) { SimpleOverlayNotification.show("端口格式错误", NotificationType.ERROR, 2000) }
        }, Modifier.fillMaxWidth()) { Text("保存配置") }
    }
}

@Composable
private fun PrivacyDialog(text: String, onAgree: () -> Unit, onDisagree: () -> Unit) {
    AlertDialog(onDismissRequest = {}, title = { Text("隐私协议更新") },
        text = {
            Column {
                Text("请阅读并同意更新后的隐私协议", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().heightIn(max = 300.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
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
    AlertDialog(onDismissRequest = onDismiss, title = { Text(info.title) },
        text = { Text(info.message, Modifier.verticalScroll(rememberScrollState()).heightIn(max = 300.dp)) },
        confirmButton = { Button(onDismiss) { Text("我已了解") } }
    )
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("发现新版本: ${info.versionName}") },
        text = { Text(info.changelog, Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)) },
        confirmButton = { Button(onUpdate) { Text("立即更新") } },
        dismissButton = { TextButton(onDismiss) { Text("稍后") } }
    )
}

private suspend fun makeHttp(url: String): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"; conn.connectTimeout = 15000; conn.readTimeout = 15000
    conn.setRequestProperty("User-Agent", "Lumina Android Client"); conn.connect()
    if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
    conn.inputStream.bufferedReader().use { it.readText() }
}

private fun getSHA(input: String): String = MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }

@Suppress("DEPRECATION")
private fun getLocalVersionCode(context: Context): Long {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
}