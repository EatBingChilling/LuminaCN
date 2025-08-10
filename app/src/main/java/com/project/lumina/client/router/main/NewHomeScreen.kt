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

// ... Data classes and constants remain the same ...
// ... I will paste the full file to be safe.

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
                    LinearProgressIndicator({ animatedProgress }, Modifier.width(200.dp)); Spacer(Modifier.height(16.dp))
                    Text(msg, style = MaterialTheme.typography.titleMedium); err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (showProgressDialog) Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.padding(16.dp).wrapContentSize()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "正在下载: $currentPackName", style = MaterialTheme.typography.titleMedium); Spacer(modifier = Modifier.height(8.dp))
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

// All other private Composables like MainDashboard, Dialogs, etc. remain the same.
// Omitting them here as they were not part of the error log.