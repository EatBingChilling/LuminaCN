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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

//================================================================================//
// FIX: PLACEHOLDER/STUB IMPLEMENTATIONS FOR MISSING CODE
// The following classes, objects, and functions were created to resolve errors.
// You should replace these with your actual project implementations.
//================================================================================//

// --- STUB: Utility Functions ---

suspend fun makeHttp(urlString: String): String = withContext(Dispatchers.IO) {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            throw Exception("HTTP Error: ${connection.responseCode} ${connection.responseMessage}")
        }
    } finally {
        connection.disconnect()
    }
}

fun getSHA(input: String): String {
    return try {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun getLocalVersionCode(context: Context): Long {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        PackageInfoCompat.getLongVersionCode(packageInfo)
    } catch (e: Exception) {
        e.printStackTrace()
        -1L
    }
}

// --- STUB: Domain-specific Logic & Data ---

enum class NotificationType { INFO, WARNING, ERROR, SUCCESS }

object Services {
    var isActive by mutableStateOf(false)
    var isLaunchingMinecraft by mutableStateOf(false)
}

data class GamePack(val name: String, val url: String)

object PackSelectionManager {
    var selectedPack: GamePack? = null
}

object ConnectionInfoOverlay {
    fun getLocalIpAddress(context: Context): String? = "192.168.1.100" // Placeholder
    fun show(ip: String?) { println("OVERLAY: Show connection info for $ip") }
}

object SimpleOverlayNotification {
    fun show(message: String, type: NotificationType, duration: Long = 3000L) {
        println("OVERLAY NOTIFICATION [${type.name}]: $message (for ${duration}ms)")
    }
}

object MCPackUtils {
    suspend fun downloadAndOpenPack(context: Context, pack: GamePack, onProgress: (Float) -> Unit) {
        println("MCPackUtils: Downloading ${pack.name}")
        withContext(Dispatchers.IO) { for (i in 1..10) { delay(200); onProgress(i / 10f) } }
        println("MCPackUtils: Download complete.")
    }
}

object InjectNeko {
    fun injectNeko(context: Context, onComplete: () -> Unit) {
        println("InjectNeko: Injecting...")
        onComplete()
    }
}

object ServerInit {
    fun addMinecraftServer(context: Context, ip: String?) {
        println("ServerInit: Adding Minecraft server with IP: $ip")
    }
}

// --- STUB: ViewModel ---

class MainScreenViewModel : ViewModel() {
    val captureModeModel = MutableStateFlow("") // Placeholder state
    val selectedGame = MutableStateFlow<String?>("com.mojang.minecraftpe") // Placeholder state
}

// --- STUB: UI Components ---

@Composable
fun MainDashboard() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("主面板") }
}

@Composable
fun AccountPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("账户页面") }
}

@Composable
fun AboutPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("关于页面") }
}

@Composable
fun SettingsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设置页面") }
}

@Composable
fun AppBottomNavigationBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = currentTab == 0, onClick = { onTabSelected(0) }, icon = { Icon(Icons.Default.Home, "主页") }, label = { Text("主页") })
        NavigationBarItem(selected = currentTab == 1, onClick = { onTabSelected(1) }, icon = { Icon(Icons.Rounded.AccountCircle, "账户") }, label = { Text("账户") })
        NavigationBarItem(selected = currentTab == 2, onClick = { onTabSelected(2) }, icon = { Icon(Icons.Default.Info, "关于") }, label = { Text("关于") })
        NavigationBarItem(selected = currentTab == 3, onClick = { onTabSelected(3) }, icon = { Icon(Icons.Default.Settings, "设置") }, label = { Text("设置") })
    }
}

@Composable
fun LaunchStopFAB(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        val icon = if (Services.isActive) Icons.Default.Stop else Icons.Default.PlayArrow
        val description = if (Services.isActive) "停止" else "启动"
        Icon(icon, contentDescription = description)
    }
}

@Composable
fun AppNavigationRail(currentTab: Int, onTabSelected: (Int) -> Unit, onStartToggle: () -> Unit) {
    NavigationRail(modifier = Modifier.padding(top = 16.dp)) {
        LaunchStopFAB(onClick = onStartToggle)
        Spacer(Modifier.height(24.dp))
        NavigationRailItem(selected = currentTab == 0, onClick = { onTabSelected(0) }, icon = { Icon(Icons.Default.Home, "主页") }, label = { Text("主页") })
        NavigationRailItem(selected = currentTab == 1, onClick = { onTabSelected(1) }, icon = { Icon(Icons.Rounded.AccountCircle, "账户") }, label = { Text("账户") })
        NavigationRailItem(selected = currentTab == 2, onClick = { onTabSelected(2) }, icon = { Icon(Icons.Default.Info, "关于") }, label = { Text("关于") })
        NavigationRailItem(selected = currentTab == 3, onClick = { onTabSelected(3) }, icon = { Icon(Icons.Default.Settings, "设置") }, label = { Text("设置") })
    }
}

@Composable
fun PrivacyDialog(content: String, onAgree: () -> Unit, onDisagree: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDisagree,
        title = { Text("隐私协议") },
        text = { Text(content, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { Button(onClick = onAgree) { Text("同意") } },
        dismissButton = { Button(onClick = onDisagree) { Text("不同意") } }
    )
}

@Composable
fun NoticeDialog(info: NoticeInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(info.title) },
        text = { Text(info.message, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { Button(onClick = onDismiss) { Text("好的") } }
    )
}

@Composable
fun UpdateDialog(info: UpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本: ${info.versionName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("更新日志:\n${info.changelog}")
            }
        },
        confirmButton = { Button(onClick = onUpdate) { Text("立即更新") } },
        dismissButton = { Button(onClick = onDismiss) { Text("稍后") } }
    )
}

//================================================================================//
// ORIGINAL USER CODE (NOW FIXED)
//================================================================================//

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
            try { msg = "步骤1: 连接服务器..."; progress = 0.2f; delay(500); makeHttp("$BASE_URL/appstatus/a.ini") } catch (e: Exception) { err = "服务器连接失败: ${e.message}"; msg = "验证失败，将跳过检查..."; allStepsSuccess = false }
            if (allStepsSuccess) try { msg = "步骤2: 获取公告..."; progress = 0.4f; delay(500); val resp = makeHttp("$BASE_URL/title/a.json"); if (getSHA(resp) != prefs.getString(KEY_NOTICE_HASH, "")) { val j = JSONObject(resp); notice = NoticeInfo(j.getString("title"), "${j.optString("subtitle", "")}\n\n${j.getString("content")}", resp) } } catch (e: Exception) { println("Failed to fetch notice: ${e.message}") }
            if (allStepsSuccess) try { msg = "步骤3: 获取隐私协议..."; progress = 0.6f; delay(500); val resp = makeHttp("$BASE_URL/privary/a.txt"); if (getSHA(resp) != prefs.getString(KEY_PRIVACY_HASH, "")) privacy = resp } catch (e: Exception) { err = "无法获取隐私协议: ${e.message}"; msg = "验证失败，将跳过检查..."; allStepsSuccess = false }
            if (allStepsSuccess) try { msg = "步骤4: 检查更新..."; progress = 0.8f; delay(500); val resp = makeHttp("$BASE_URL/update/a.json"); val j = JSONObject(resp); if (j.getLong("version") > getLocalVersionCode(context)) { update = UpdateInfo(j.getString("name"), j.getString("update_content"), "http://110.42.63.51:39078/apps/apks") } } catch (e: Exception) { println("Failed to check for updates: ${e.message}") }
            progress = 1f; msg = if (allStepsSuccess) "验证完成" else "验证流程已跳过"; delay(800); isVerifying = false
            if (!allStepsSuccess && err != null) SimpleOverlayNotification.show(err ?: "验证失败", NotificationType.ERROR, 3000)
        }
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key -> if (key == "injectNekoPackEnabled") injectNekoPack = sp.getBoolean(key, false) }
        settingsPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { settingsPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val launchStopAction = {
        if (Services.isActive) onStartToggle() else scope.launch {
            Services.isLaunchingMinecraft = true; onStartToggle(); delay(2500)
            if (!Services.isActive) { Services.isLaunchingMinecraft = false; return@launch }
            val selectedGame = vm.selectedGame.value
            if (selectedGame != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                if (intent != null) {
                    context.startActivity(intent); delay(3000)
                    if (Services.isActive && !settingsPrefs.getBoolean("disableConnectionInfoOverlay", false)) ConnectionInfoOverlay.show(localIp)
                    Services.isLaunchingMinecraft = false
                    try { when {
                        injectNekoPack && PackSelectionManager.selectedPack != null -> PackSelectionManager.selectedPack?.let { pack ->
                            currentPackName = pack.name; showProgressDialog = true; downloadProgress = 0f
                            try { MCPackUtils.downloadAndOpenPack(context, pack) { p -> downloadProgress = p }; showProgressDialog = false }
                            catch (e: Exception) { showProgressDialog = false; SimpleOverlayNotification.show("材质包下载失败: ${e.message}", NotificationType.ERROR) }
                        }
                        injectNekoPack -> try { InjectNeko.injectNeko(context) {} } catch (e: Exception) { SimpleOverlayNotification.show("Neko 注入失败: ${e.message}", NotificationType.ERROR) }
                        selectedGame == "com.mojang.minecraftpe" -> try { ServerInit.addMinecraftServer(context, localIp) } catch (e: Exception) { SimpleOverlayNotification.show("服务器初始化失败: ${e.message}", NotificationType.ERROR) }
                    } } catch (e: Exception) { SimpleOverlayNotification.show("一个未预料的错误发生: ${e.message}", NotificationType.ERROR) }
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
                MainContentArea(Modifier.weight(1f), tab, isVerifying, progress, msg, err, showProgressDialog, downloadProgress, currentPackName, privacy, notice, update, { p -> prefs.edit().putString(KEY_PRIVACY_HASH, getSHA(p)).apply(); privacy = null }, { (context as? Activity)?.finish() }, { n -> prefs.edit().putString(KEY_NOTICE_HASH, getSHA(n.rawJson)).apply(); notice = null }, { u -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))); update = null }, { update = null })
            }
        } else {
            MainContentArea(Modifier.padding(innerPadding), tab, isVerifying, progress, msg, err, showProgressDialog, downloadProgress, currentPackName, privacy, notice, update, { p -> prefs.edit().putString(KEY_PRIVACY_HASH, getSHA(p)).apply(); privacy = null }, { (context as? Activity)?.finish() }, { n -> prefs.edit().putString(KEY_NOTICE_HASH, getSHA(n.rawJson)).apply(); notice = null }, { u -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))); update = null }, { update = null })
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
        AnimatedContent(targetState = currentTab, transitionSpec = { if (targetState > initialState) slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut() else slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut() }, label = "tab-content") { t ->
            when (t) { 0 -> MainDashboard(); 1 -> AccountPage(); 2 -> AboutPage(); 3 -> SettingsScreen() }
        }
        val animatedProgress by animateFloatAsState(progress, tween(600, easing = FastOutSlowInEasing), label = "verificationProgress")
        AnimatedVisibility(visible = isVerifying, exit = fadeOut(tween(500))) {
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f), modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    LinearProgressIndicator(progress = { animatedProgress }, Modifier.width(200.dp)); Spacer(Modifier.height(16.dp))
                    Text(msg, style = MaterialTheme.typography.titleMedium); err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (showProgressDialog) Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.padding(16.dp).wrapContentSize()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "正在下载: $currentPackName", style = MaterialTheme.typography.titleMedium); Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(progress = { downloadProgress }, modifier = Modifier.size(48.dp))
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