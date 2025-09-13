/*
 * © Project LuminaCN 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.phoenix.luminacn.router.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.content.res.Configuration
import android.provider.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phoenix.luminacn.R
import com.phoenix.luminacn.WallpaperUtils
import com.phoenix.luminacn.constructors.*
import com.phoenix.luminacn.overlay.manager.ConnectionInfoOverlay
import com.phoenix.luminacn.overlay.mods.NotificationType
import com.phoenix.luminacn.overlay.mods.SimpleOverlayNotification
import com.phoenix.luminacn.service.Services
import com.phoenix.luminacn.ui.component.ServerSelector
import com.phoenix.luminacn.util.*
import com.phoenix.luminacn.viewmodel.MainScreenViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/* -------------------- 数据类 & 常量 -------------------- */
// FIX: NoticeInfo's message field will be constructed from subtitle and content.
data class NoticeInfo(val title: String, val message: String, val rawJson: String)
// FIX: UpdateInfo fields updated to match server response from reference code.
data class UpdateInfo(val versionName: String, val changelog: String, val url: String)

private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
private const val PREFS_NAME = "app_verification_prefs"
// FIX: Updated SharedPreferences keys for clarity, matching reference code.
private const val KEY_NOTICE_HASH = "notice_hash"
private const val KEY_PRIVACY_HASH = "privacy_hash"
// MODIFIED: Added key for accessibility prompt preference
private const val KEY_DONT_SHOW_ACCESSIBILITY_PROMPT = "dont_show_accessibility_prompt"


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
    val model by vm.captureModeModel.collectAsState()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // [新增] 在父组件中收集设置状态，以便控制灵动岛
    val settingsState by vm.settingsState.collectAsState()

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
    // MODIFIED: State to control the accessibility dialog
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    
    // ================== [新增] 灵动岛控制逻辑 ==================
    // 将控制逻辑从 SettingsScreen 移动到这里，使其生命周期与主屏幕绑定
    val dynamicIslandController = remember { DynamicIslandController(context) }

    LaunchedEffect(settingsState.dynamicIslandUsername) {
        delay(300) // Debounce
        dynamicIslandController.setPersistentText(settingsState.dynamicIslandUsername)
    }

    LaunchedEffect(settingsState.dynamicIslandYOffset) {
        dynamicIslandController.updateYOffset(settingsState.dynamicIslandYOffset)
    }

    LaunchedEffect(settingsState.dynamicIslandScale) {
        dynamicIslandController.updateScale(settingsState.dynamicIslandScale)
    }

    LaunchedEffect(settingsState.musicModeEnabled) {
        dynamicIslandController.enableMusicMode(settingsState.musicModeEnabled)
    }
    // ================== [新增] 灵动岛控制逻辑结束 ==================

    // 获取壁纸
    LaunchedEffect(Unit) {
        wallpaperBitmap = WallpaperUtils.getWallpaperBitmap(context)
    }

    /* 验证流程 (FIX: Reworked verification logic to be more robust and correct JSON parsing) */
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var allStepsSuccess = true

            // Step 1: Check Server Status
            try {
                step = 1; msg = "步骤1: 连接服务器..."; progress = 0.2f; delay(500)
                makeHttp("$BASE_URL/appstatus/a.ini") // We just need a 200 OK
            } catch (e: Exception) {
                err = "服务器连接失败: ${e.message}"
                msg = "验证失败，将跳过检查..."
                allStepsSuccess = false
            }

            // Step 2: Fetch Notice
            if (allStepsSuccess) {
                try {
                    step = 2; msg = "步骤2: 获取公告..."; progress = 0.4f; delay(500)
                    val resp = makeHttp("$BASE_URL/title/a.json")
                    if (getSHA(resp) != prefs.getString(KEY_NOTICE_HASH, "")) {
                        val j = JSONObject(resp)
                        // FIX: Correctly parse subtitle and content, not "message"
                        val noticeMessage = "${j.optString("subtitle", "")}\n\n${j.getString("content")}"
                        notice = NoticeInfo(j.getString("title"), noticeMessage, resp)
                    }
                } catch (e: Exception) {
                    // Non-critical error, just log and continue
                    println("Failed to fetch notice: ${e.message}")
                }
            }


            // Step 3: Fetch Privacy Policy
            if (allStepsSuccess) {
                try {
                    step = 3; msg = "步骤3: 获取隐私协议..."; progress = 0.6f; delay(500)
                    val resp = makeHttp("$BASE_URL/privary/a.txt")
                    if (getSHA(resp) != prefs.getString(KEY_PRIVACY_HASH, "")) {
                        privacy = resp
                    }
                } catch (e: Exception) {
                    err = "无法获取隐私协议: ${e.message}"
                    msg = "验证失败，将跳过检查..."
                    allStepsSuccess = false
                }
            }


            // Step 4: Check for Updates
            if (allStepsSuccess) {
                try {
                    step = 4; msg = "步骤4: 检查更新..."; progress = 0.8f; delay(500)
                    val resp = makeHttp("$BASE_URL/update/a.json")
                    val j = JSONObject(resp)
                    val cloudVersion = j.getLong("version")
                    val localVersion = getLocalVersionCode(context)

                    if (cloudVersion > localVersion) {
                        update = UpdateInfo(
                            versionName = j.getString("name"),
                            changelog = j.getString("update_content"),
                            // The reference code hardcodes this URL, so we will too.
                            url = "https://www.123684.com/s/09SNjv-Zoxod"
                        )
                    }
                } catch (e: Exception) {
                    // Non-critical error, just log and continue
                    println("Failed to check for updates: ${e.message}")
                }
            }


            // Finalization
            progress = 1f
            msg = if (allStepsSuccess) "验证完成" else "验证流程已跳过"
            delay(800)
            isVerifying = false

            if (!allStepsSuccess && err != null) {
                SimpleOverlayNotification.show(err ?: "验证失败，应用可能工作不正常", NotificationType.ERROR, 3000)
            }
        }
    }


    // MODIFIED: Logic for starting the service, extracted for reuse.
    val startServiceAction = {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "服务正在启动...",
                actionLabel = "启动应用",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val settingsPrefs = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
                val selectedAppPackage = settingsPrefs.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe"
                val intent = context.packageManager.getLaunchIntentForPackage(selectedAppPackage)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    SimpleOverlayNotification.show(
                        "未安装选定的应用，请在设置中配置正确的应用包名",
                        NotificationType.ERROR,
                        3000
                    )
                }
            }
        }
        onStartToggle()
    }

    // MODIFIED: Centralized click handler for the FABs
    val fabOnClick = {
        if (Services.isActive) {
            // If service is running, just stop it.
            onStartToggle()
        } else {
            // If service is not running, check for accessibility permission before starting.
            val accessibilityEnabled = isAccessibilityEnabled(context)
            val dontAskAgain = prefs.getBoolean(KEY_DONT_SHOW_ACCESSIBILITY_PROMPT, false)

            if (!accessibilityEnabled && !dontAskAgain) {
                // Show the dialog if accessibility is off and the user hasn't opted out.
                showAccessibilityDialog = true
            } else {
                // Otherwise, start the service directly.
                startServiceAction()
            }
        }
    }

    val configuration = LocalConfiguration.current
    
    // Determine if we should use NavigationRail (landscape/wide screen) or NavigationBar (portrait)
    val useNavigationRail = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val navigationItems = listOf(
        "主仪表盘" to Icons.Filled.Dashboard,
        "账户" to Icons.Rounded.AccountCircle,
        "关于" to Icons.Filled.Info,
        "设置" to Icons.Filled.Settings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸背景层 (80%)
        wallpaperBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    color = Color.White.copy(alpha = 0.8f), // 80% 壁纸显示
                    blendMode = BlendMode.Modulate
                )
            )
        }
        
        // 主题色叠加层 (20%)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) // 20% 主题色
                )
        )

        // UI内容层
        if (useNavigationRail) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // NavigationRail for landscape
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = Color.Transparent
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    navigationItems.forEachIndexed { idx, (label, icon) ->
                        NavigationRailItem(
                            selected = tab == idx,
                            onClick = { tab = idx },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // Content area for landscape
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
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
                    
                    // FAB for landscape (positioned in bottom end)
                    if (tab == 0) {
                        ExtendedFloatingActionButton(
                            onClick = fabOnClick, // MODIFIED
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = if (Services.isActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (Services.isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                if (Services.isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (Services.isActive) "停止服务" else "启动服务"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (Services.isActive) "停止服务" else "启动服务",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait layout with NavigationBar at bottom
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = Color.Transparent, // 透明背景使用底层混合背景
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), // 85% 不透明度的表面色
                        tonalElevation = 3.dp // 添加一点高度感
                    ) {
                        navigationItems.forEachIndexed { idx, (label, icon) ->
                            NavigationBarItem(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                icon = { Icon(icon, label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (tab == 0) {
                        ExtendedFloatingActionButton(
                            onClick = fabOnClick, // MODIFIED
                            containerColor = if (Services.isActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (Services.isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                if (Services.isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (Services.isActive) "停止服务" else "启动服务"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (Services.isActive) "停止服务" else "启动服务",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            ) { inner ->
                Box(Modifier.padding(inner)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
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
                }
            }
        }
    }

    // <<< MODIFIED: 验证遮罩动画化
    // 1. 使用 `animateFloatAsState` 创建一个 `progress` 的动画版本
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "VerificationProgressAnimation"
    )

    // 2. 使用 `AnimatedVisibility` 包裹整个遮罩，实现优雅的淡出效果
    AnimatedVisibility(
        visible = isVerifying,
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxSize(),
            // 添加 clickable 以阻止下层UI的交互
            onClick = {}
        ) {
            Column(
                Modifier.fillMaxSize(),
                Arrangement.Center,
                Alignment.CenterHorizontally
            ) {
                // 3. 将 `LinearProgressIndicator` 的 progress 指向我们创建的 `animatedProgress`
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    Modifier.width(200.dp)
                )
                // 注意: 如果你想要的是那种无限循环的波浪线加载动画 (不显示具体进度),
                // 只需将上面的调用改为不带 progress 参数即可:
                // LinearProgressIndicator(Modifier.width(200.dp))

                Spacer(Modifier.height(16.dp))
                Text(msg, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    // >>> MODIFIED END

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
    
    // MODIFIED: Added AlertDialog for accessibility permission
    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { /* Disallow dismissing by clicking outside */ },
            title = { Text("启用实体按键绑定？") },
            text = { Text("LuminaCN 可以使用无障碍服务来启用实体按键绑定功能（如音量键控制）。\n\n如果不需要，服务仍可正常启动，但无法使用实体按键绑定。") },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showAccessibilityDialog = false
                            // User wants to enable it, go to settings.
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) { Text("需要") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showAccessibilityDialog = false
                            // User doesn't want to enable it, just start the service.
                            startServiceAction()
                        }
                    ) { Text("不需要") }

                    TextButton(
                        onClick = {
                            showAccessibilityDialog = false
                            // User doesn't want to be asked again, save preference and start service.
                            prefs.edit().putBoolean(KEY_DONT_SHOW_ACCESSIBILITY_PROMPT, true).apply()
                            startServiceAction()
                        }
                    ) { Text("不再提示") }
                }
            }
        )
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
        AnimatedVisibility(AccountManager.currentAccount != null) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface.copy(alpha = 0.9f) // 半透明卡片
                ),
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

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant.copy(alpha = 0.9f) // 半透明卡片
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
                Text("服务器配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ServerConfigSection(vm, model)
                AnimatedVisibility(model.serverHostName.isNotBlank()) {
                    Surface(
                        Modifier.fillMaxWidth(), 
                        RoundedCornerShape(8.dp), 
                        colors.surface.copy(alpha = 0.8f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("当前服务器", style = MaterialTheme.typography.bodySmall)
                            Text(model.serverHostName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("端口: ${model.serverPort}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // 服务器选择器卡片
        ServerSelectorCard()

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (Services.isActive) 
                    colors.tertiaryContainer.copy(alpha = 0.9f) 
                else 
                    colors.errorContainer.copy(alpha = 0.9f)
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

        // --- KEY BINDING SECTION REMOVED AS REQUESTED ---
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
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colors.surfaceContainerLow.copy(alpha = 0.9f) // 半透明卡片
            )
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

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colors.surfaceContainerLow.copy(alpha = 0.9f) // 半透明卡片
            )
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
private fun ServerConfigSection(vm: MainScreenViewModel, model: com.phoenix.luminacn.model.CaptureModeModel) {
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

// --- KeyBindingItem COMPOSABLE REMOVED ---

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
// MODIFIED: Added helper function to check for accessibility service status
private fun isAccessibilityEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabled.any { it.id.contains(context.packageName) }
}

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

// FIX: Added robust version code getter from reference code.
@Suppress("DEPRECATION")
private fun getLocalVersionCode(context: Context): Long {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
}

/* ======================================================
   服务器选择器卡片组件
   ====================================================== */
@Composable
private fun ServerSelectorCard() {
    val colors = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant.copy(alpha = 0.9f) // 半透明卡片
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            // 标题行，带展开/收起按钮
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        Modifier.size(40.dp),
                        CircleShape,
                        colors.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Filled.Storage,
                            contentDescription = null,
                            Modifier.padding(8.dp),
                            tint = colors.primary
                        )
                    }
                    Column {
                        Text(
                            "服务器列表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            if (isExpanded) "点击收起列表" else "点击展开选择服务器",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                
                // 展开/收起按钮
                Surface(
                    Modifier.size(32.dp),
                    CircleShape,
                    colors.secondaryContainer.copy(alpha = 0.8f)
                ) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300),
                        label = "chevron_rotation"
                    )
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        Modifier
                            .padding(4.dp)
                            .rotate(rotationAngle),
                        tint = colors.onSecondaryContainer
                    )
                }
            }

            // 可展开的服务器选择器内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut()
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colors.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 使用约束高度避免占用过多空间
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        ServerSelector()
                    }
                    
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}