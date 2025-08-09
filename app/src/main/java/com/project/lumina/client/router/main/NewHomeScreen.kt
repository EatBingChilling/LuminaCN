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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.project.lumina.client.R
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.constructors.KeyBindingManager
import com.project.lumina.client.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.KeyCaptureService
import com.project.lumina.client.service.Services
import com.project.lumina.client.util.InjectNeko
import com.project.lumina.client.util.MCPackUtils
import com.project.lumina.client.util.ServerInit
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// --- 数据类定义 ---
data class NoticeInfo(
    val title: String,
    val message: String,
    val rawJson: String
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val url: String
)
// --------------------

private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
private const val PREFS_NAME_VERIFICATION = "app_verification_prefs"
private const val KEY_NOTICE_HASH = "notice_content_hash"
private const val KEY_PRIVACY_HASH = "privacy_content_hash"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(
    onStartToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    val colors = MaterialTheme.colorScheme
    val prefs = remember { context.getSharedPreferences(PREFS_NAME_VERIFICATION, Context.MODE_PRIVATE) }

    // 验证流程状态
    var isVerifying by remember { mutableStateOf(true) }
    var verificationStep by remember { mutableIntStateOf(1) }
    var verificationMessage by remember { mutableStateOf("正在连接服务器...") }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var verificationProgress by remember { mutableFloatStateOf(0f) }

    // 交互式内容状态 (用于控制对话框)
    var noticeToShow by remember { mutableStateOf<NoticeInfo?>(null) }
    var privacyToShow by remember { mutableStateOf<String?>(null) }
    var updateToShow by remember { mutableStateOf<UpdateInfo?>(null) }

    // 页面状态
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLaunchingMinecraft by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var currentPackName by remember { mutableStateOf("") }

    // --- 真实的验证流程 ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // 第1步：连接服务器
                verificationStep = 1
                verificationMessage = "正在连接服务器..."
                verificationProgress = 0.2f
                delay(500) // 视觉停留
                try {
                    makeHttpRequest("$BASE_URL/appstatus/a.ini")
                } catch (e: Exception) {
                    println("Server connection failed, proceeding...: ${e.message}")
                }
                
                // 第2步：获取公告
                verificationStep = 2
                verificationMessage = "正在获取公告..."
                verificationProgress = 0.4f
                delay(500)
                try {
                    val noticeResp = makeHttpRequest("$BASE_URL/title/a.json")
                    val hash = getSHA256Hash(noticeResp)
                    val storedHash = prefs.getString(KEY_NOTICE_HASH, "")
                    if (hash != storedHash) {
                        val json = JSONObject(noticeResp)
                        noticeToShow = NoticeInfo(
                            title = json.getString("title"),
                            message = json.getString("message"),
                            rawJson = noticeResp
                        )
                    }
                } catch (e: Exception) {
                    println("Failed to get notice: ${e.message}")
                }

                // 第3步：获取隐私协议
                verificationStep = 3
                verificationMessage = "正在检查隐私协议..."
                verificationProgress = 0.6f
                delay(500)
                try {
                    val privacyResp = makeHttpRequest("$BASE_URL/privary/a.txt")
                    val hash = getSHA256Hash(privacyResp)
                    val storedHash = prefs.getString(KEY_PRIVACY_HASH, "")
                    if (hash != storedHash) {
                        privacyToShow = privacyResp
                    }
                } catch (e: Exception) {
                    println("Failed to get privacy policy: ${e.message}")
                }
                
                // 第4步：检查版本更新
                verificationStep = 4
                verificationMessage = "正在检查版本更新..."
                verificationProgress = 0.8f
                delay(500)
                try {
                    val updateResp = makeHttpRequest("$BASE_URL/update/a.json")
                    val json = JSONObject(updateResp)
                    @Suppress("DEPRECATION") val currentVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                    val remoteVersionCode = json.getInt("versionCode")

                    if (remoteVersionCode > currentVersionCode) {
                        updateToShow = UpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = json.getString("versionName"),
                            changelog = json.getString("changelog"),
                            url = json.getString("url")
                        )
                    }
                } catch (e: Exception) {
                    println("Failed to check for updates: ${e.message}")
                }
                
                // 验证完成
                verificationMessage = "验证完成"
                verificationProgress = 1.0f
                delay(800)
                isVerifying = false
            } catch (e: Exception) {
                // 全局错误处理
                verificationError = "验证过程出现错误: ${e.message}"
                verificationMessage = "验证失败，正在跳过..."
                delay(1500)
                isVerifying = false
                withContext(Dispatchers.Main) {
                    SimpleOverlayNotification.show(
                        message = "验证失败，应用可能工作不正常",
                        type = NotificationType.ERROR,
                        durationMs = 3000
                    )
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                contentColor = colors.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Dashboard, "主仪表盘") },
                    label = { Text("主仪表盘") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primaryContainer)
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.AccountCircle, "账户") },
                    label = { Text("账户") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primaryContainer)
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Info, "关于") },
                    label = { Text("关于") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primaryContainer)
                )
                NavigationBarItem(
                    selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Settings, "设置") },
                    label = { Text("设置") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primaryContainer)
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (Services.isActive) {
                            isLaunchingMinecraft = false
                            onStartToggle()
                        } else {
                            scope.launch {
                                try {
                                    isLaunchingMinecraft = true
                                    Services.isLaunchingMinecraft = true
                                    onStartToggle()
                                    
                                    delay(2500)
                                    if (!Services.isActive) {
                                        isLaunchingMinecraft = false
                                        Services.isLaunchingMinecraft = false
                                        return@launch
                                    }
                                    
                                    val selectedGame = mainScreenViewModel.selectedGame.value ?: "com.mojang.minecraftpe"
                                    val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                                    if (intent != null && Services.isActive) {
                                        context.startActivity(intent)
                                        delay(3000)
                                        if (Services.isActive) {
                                            val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
                                            if (!sharedPreferences.getBoolean("disableConnectionInfoOverlay", false)) {
                                                ConnectionInfoOverlay.show(ConnectionInfoOverlay.getLocalIpAddress(context))
                                            }
                                        }
                                        isLaunchingMinecraft = false
                                        Services.isLaunchingMinecraft = false
                                        
                                        val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
                                        val injectNekoPackEnabled = sharedPreferences.getBoolean("injectNekoPackEnabled", false)
                                        when {
                                            injectNekoPackEnabled && PackSelectionManager.selectedPack != null -> {
                                                PackSelectionManager.selectedPack?.let { pack ->
                                                    currentPackName = pack.name
                                                    showProgressDialog = true
                                                    downloadProgress = 0f
                                                    try {
                                                        MCPackUtils.downloadAndOpenPack(context, pack) { progress -> downloadProgress = progress }
                                                        showProgressDialog = false
                                                    } catch (e: Exception) {
                                                        showProgressDialog = false
                                                        SimpleOverlayNotification.show("材质包下载失败: ${e.message}", NotificationType.ERROR, 3000)
                                                    }
                                                }
                                            }
                                            injectNekoPackEnabled -> {
                                                try {
                                                    InjectNeko.injectNeko(context) {}
                                                } catch (e: Exception) {
                                                    SimpleOverlayNotification.show("Neko 注入失败: ${e.message}", NotificationType.ERROR, 3000)
                                                }
                                            }
                                            else -> {
                                                if (selectedGame == "com.mojang.minecraftpe") {
                                                    try {
                                                        ServerInit.addMinecraftServer(context, ConnectionInfoOverlay.getLocalIpAddress(context))
                                                    } catch (e: Exception) {
                                                        SimpleOverlayNotification.show("服务器初始化失败: ${e.message}", NotificationType.ERROR, 3000)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        isLaunchingMinecraft = false
                                        Services.isLaunchingMinecraft = false
                                        SimpleOverlayNotification.show("游戏启动失败，请检查是否安装或正确添加客户端", NotificationType.ERROR, 3000)
                                    }
                                } catch (e: Exception) {
                                    isLaunchingMinecraft = false
                                    Services.isLaunchingMinecraft = false
                                    SimpleOverlayNotification.show("启动失败: ${e.message}", NotificationType.ERROR, 3000)
                                }
                            }
                        }
                    },
                    containerColor = if (Services.isActive) colors.error else colors.primary,
                    contentColor = if (Services.isActive) colors.onError else colors.onPrimary,
                    modifier = Modifier.scale(if (isLaunchingMinecraft) 0.9f else 1f)
                ) {
                    AnimatedContent(
                        targetState = when {
                            isLaunchingMinecraft -> Icons.Filled.Stop
                            Services.isActive -> Icons.Filled.Stop
                            else -> Icons.Filled.PlayArrow
                        },
                        transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                        label = "fabIconAnimation"
                    ) { icon ->
                        Icon(icon, contentDescription = if (Services.isActive) "停止" else "开始")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) AnimatedContentScope.SlideDirection.Left else AnimatedContentScope.SlideDirection.Right
                    slideIntoContainer(slideDirection) + fadeIn() togetherWith slideOutOfContainer(slideDirection) + fadeOut()
                },
                label = "tabAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> MainDashboard(isLaunchingMinecraft, showProgressDialog, downloadProgress, currentPackName)
                    1 -> AccountPage()
                    2 -> AboutPage()
                    3 -> SettingsScreen()
                }
            }
            
            if (isVerifying) {
                Surface(color = colors.background.copy(alpha = 0.95f), modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { verificationProgress },
                                modifier = Modifier.width(200.dp),
                                color = colors.primary,
                                trackColor = colors.surfaceVariant
                            )
                            Text(verificationMessage, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                            verificationError?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VerificationStep(1, "连接服务器", verificationStep >= 1, verificationStep == 1)
                            VerificationStep(2, "获取公告", verificationStep >= 2, verificationStep == 2)
                            VerificationStep(3, "隐私协议", verificationStep >= 3, verificationStep == 3)
                            VerificationStep(4, "版本检查", verificationStep >= 4, verificationStep == 4)
                        }
                    }
                }
            }

            when {
                privacyToShow != null -> {
                    PrivacyDialog(
                        policyText = privacyToShow!!,
                        onAgree = {
                            prefs.edit().putString(KEY_PRIVACY_HASH, getSHA256Hash(privacyToShow!!)).apply()
                            privacyToShow = null
                        },
                        onDisagree = { (context as? Activity)?.finish() }
                    )
                }
                noticeToShow != null -> {
                    NoticeDialog(
                        notice = noticeToShow!!,
                        onDismiss = {
                            prefs.edit().putString(KEY_NOTICE_HASH, getSHA256Hash(noticeToShow!!.rawJson)).apply()
                            noticeToShow = null
                        }
                    )
                }
                updateToShow != null -> {
                    UpdateDialog(
                        updateInfo = updateToShow!!,
                        onUpdate = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateToShow!!.url)))
                            updateToShow = null
                        },
                        onDismiss = { updateToShow = null }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyDialog(policyText: String, onAgree: () -> Unit, onDisagree: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("隐私协议更新") },
        text = {
            Column {
                Text("为了继续使用，请阅读并同意更新后的隐私协议。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(policyText, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            }
        },
        confirmButton = { Button(onClick = onAgree) { Text("同意并继续") } },
        dismissButton = { TextButton(onClick = onDisagree) { Text("不同意并退出") } }
    )
}

@Composable
fun NoticeDialog(notice: NoticeInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(notice.title) },
        text = { Text(notice.message, modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { Button(onClick = onDismiss) { Text("我已了解") } }
    )
}

@Composable
fun UpdateDialog(updateInfo: UpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本: ${updateInfo.versionName}") },
        text = {
            Column {
                Text("建议您立即更新以获得最佳体验。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("更新日志:", fontWeight = FontWeight.Bold)
                Text(updateInfo.changelog, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()))
            }
        },
        confirmButton = { Button(onClick = onUpdate) { Text("立即更新") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("稍后") } }
    )
}

@Composable
fun VerificationStep(step: Int, title: String, isCompleted: Boolean, isCurrent: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp).background(
                color = when {
                    isCompleted -> colors.primary
                    isCurrent -> colors.primaryContainer
                    else -> colors.surfaceVariant
                },
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Filled.Check, null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Text(step.toString(), style = MaterialTheme.typography.labelMedium, color = if (isCurrent) colors.onPrimaryContainer else colors.onSurfaceVariant)
            }
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, color = when {
            isCompleted -> colors.primary
            isCurrent -> colors.onSurface
            else -> colors.onSurfaceVariant
        })
    }
}

@Composable
fun MainDashboard(isLaunchingMinecraft: Boolean, showProgressDialog: Boolean, downloadProgress: Float, currentPackName: String) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = AccountManager.currentAccount != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = colors.primary.copy(alpha = 0.1f)) {
                        Icon(Icons.Rounded.AccountCircle, null, modifier = Modifier.padding(8.dp).size(32.dp), tint = colors.primary)
                    }
                    Column {
                        Text("当前账户", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Text(AccountManager.currentAccount?.remark ?: "未选择账户", style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("服务器配置", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                ServerConfigSection(mainScreenViewModel, captureModeModel)
                AnimatedVisibility(
                    visible = captureModeModel.serverHostName.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = colors.surface) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("当前服务器", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            Text(captureModeModel.serverHostName, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, fontWeight = FontWeight.Medium)
                            Text("端口: ${captureModeModel.serverPort}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (Services.isActive) colors.tertiaryContainer else colors.errorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconScale by animateFloatAsState(if (Services.isActive) 1.2f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "")
                val iconColor by animateColorAsState(if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer, tween(300), label = "")
                Icon(if (Services.isActive) Icons.Filled.Check else Icons.Filled.Stop, null, tint = iconColor, modifier = Modifier.scale(iconScale))
                Column {
                    Text("服务状态", style = MaterialTheme.typography.bodySmall, color = (if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer).copy(alpha = 0.8f))
                    Text(if (Services.isActive) "运行中" else "已停止", style = MaterialTheme.typography.titleMedium, color = if (Services.isActive) colors.onTertiaryContainer else colors.onErrorContainer, fontWeight = FontWeight.Medium)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("物理按键绑定", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                try {
                    val boolValuesExist = GameManager.elements.any { it.values.any { v -> v is com.project.lumina.client.constructors.BoolValue } }
                    if (!boolValuesExist) {
                        Text("暂无可绑定的功能", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    } else {
                        GameManager.elements.forEach { element ->
                            element.values.filterIsInstance<com.project.lumina.client.constructors.BoolValue>().forEach { boolValue ->
                                KeyBindingItem(boolValue, element)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Text("加载功能列表时出错", style = MaterialTheme.typography.bodyMedium, color = colors.error)
                }
            }
        }

        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在下载: $currentPackName", color = colors.onSurface, style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(progress = { downloadProgress }, color = colors.primary, modifier = Modifier.size(48.dp))
                        Text("${(downloadProgress * 100).toInt()}%", color = colors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (downloadProgress < 1f) "正在下载..." else "正在启动 Minecraft ...", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {},
                containerColor = colors.surface,
                titleContentColor = colors.onSurface,
                textContentColor = colors.onSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun KeyBindingItem(boolValue: com.project.lumina.client.constructors.BoolValue, element: com.project.lumina.client.constructors.Element) {
    val context = LocalContext.current
    var isBinding by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val keyEventReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == KeyCaptureService.ACTION_KEY_EVENT) {
                    val event = intent.getParcelableExtra<KeyEvent>(KeyCaptureService.EXTRA_KEY_EVENT)
                    if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                        KeyBindingManager.setBinding(boolValue.name, event.keyCode)
                        isBinding = false
                    }
                }
            }
        }
    }

    DisposableEffect(isBinding) {
        if (isBinding) {
            LocalBroadcastManager.getInstance(context).registerReceiver(keyEventReceiver, IntentFilter(KeyCaptureService.ACTION_KEY_EVENT))
        }
        onDispose {
            if (isBinding) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(keyEventReceiver)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${element.name} - ${boolValue.name}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
            Text(KeyBindingManager.getBinding(boolValue.name)?.let { "按键: ${KeyEvent.keyCodeToString(it)}" } ?: "未绑定", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { isBinding = !isBinding }) {
                Icon(if (isBinding) Icons.Default.Check else Icons.Default.Keyboard, "Bind Key", tint = if (isBinding) colors.primary else colors.onSurfaceVariant)
            }
            Switch(checked = boolValue.value, onCheckedChange = { boolValue.value = it }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, uncheckedThumbColor = colors.onSurfaceVariant, checkedTrackColor = colors.primaryContainer, uncheckedTrackColor = colors.surfaceVariant))
        }
    }
}

@Composable
fun ServerConfigSection(mainScreenViewModel: MainScreenViewModel, captureModeModel: com.project.lumina.client.model.CaptureModeModel) {
    var serverIp by remember(captureModeModel.serverHostName) { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by remember(captureModeModel.serverPort) { mutableStateOf(captureModeModel.serverPort.toString()) }
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = serverIp, onValueChange = { serverIp = it }, label = { Text("服务器IP") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, unfocusedBorderColor = colors.outline))
        OutlinedTextField(value = serverPort, onValueChange = { serverPort = it }, label = { Text("服务器端口") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, unfocusedBorderColor = colors.outline))
        Button(
            onClick = {
                try {
                    mainScreenViewModel.selectCaptureModeModel(captureModeModel.copy(serverHostName = serverIp, serverPort = serverPort.toInt()))
                    SimpleOverlayNotification.show("服务器配置已保存", NotificationType.SUCCESS, 2000)
                } catch (e: NumberFormatException) {
                    SimpleOverlayNotification.show("端口格式错误", NotificationType.ERROR, 2000)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Text("保存配置")
        }
    }
}

@Composable
fun AccountPage() {
    AccountScreen { message, type -> SimpleOverlayNotification.show(message, type, 3000) }
}

@Composable
fun AboutPage() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val showTutorialDialog = remember { mutableStateOf(false) }
    val tutorialText = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showTutorialDialog.value) {
        if (showTutorialDialog.value && tutorialText.value == null) {
            tutorialText.value = try {
                context.resources.openRawResource(R.raw.t).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "无法加载教程内容"
            }
        }
    }

    if (showTutorialDialog.value) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog.value = false },
            title = { Text("使用教程") },
            text = { Text(tutorialText.value ?: "正在加载...") },
            confirmButton = { TextButton(onClick = { showTutorialDialog.value = false }) { Text("确定") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("实用工具", style = MaterialTheme.typography.headlineMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                Text("推荐使用", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.padding(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolButton(Icons.Filled.Download, "下载客户端") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://mcapks.net"))) }
                    ToolButton(Icons.Filled.Group, "加入群聊") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/dxqhrjC9Nu"))) }
                    ToolButton(Icons.Filled.Help, "使用教程") { showTutorialDialog.value = true }
                }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.about_lumina), style = MaterialTheme.typography.headlineMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.luminacn_dev), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    Text(stringResource(R.string.lumina_introduction), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    Text(stringResource(R.string.lumina_expectation), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    Text(stringResource(R.string.lumina_compatibility), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.lumina_copyright), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(stringResource(R.string.lumina_team), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.connect_with_us), style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.padding(top = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        SocialMediaIcon(painterResource(R.drawable.ic_github), "GitHub") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EatBingChilling/LuminaCN"))) }
                        SocialMediaIcon(painterResource(R.drawable.ic_discord), "Discord") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/invite/6kz3dcndrN"))) }
                        SocialMediaIcon(Icons.Filled.Public, "QQ") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/fQ5wdjaeOc"))) }
                        SocialMediaIcon(painterResource(R.drawable.ic_youtube), "YouTube") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@prlumina"))) }
                    }
                }
            }
        }
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

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("设置页面待实现", style = MaterialTheme.typography.headlineSmall)
    }
}

private suspend fun makeHttpRequest(url: String): String {
    return withContext(Dispatchers.IO) {
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
}

private fun getSHA256Hash(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}