/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.util.DynamicIslandController
import com.project.lumina.client.util.NetworkOptimizer
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.Socket
import kotlin.math.roundToInt

/* =========================  唯一入口  ========================= */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    val dynamicIslandController = remember { DynamicIslandController(context) }
    val sp = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    /* ---------- 状态 ---------- */
    var optimizeNetworkEnabled   by remember { mutableStateOf(sp.getBoolean("optimizeNetworkEnabled", false)) }
    var priorityThreadsEnabled   by remember { mutableStateOf(sp.getBoolean("priorityThreadsEnabled", false)) }
    var fastDnsEnabled           by remember { mutableStateOf(sp.getBoolean("fastDnsEnabled", false)) }
    var injectNekoPackEnabled    by remember { mutableStateOf(sp.getBoolean("injectNekoPackEnabled", false)) }
    var disableOverlay           by remember { mutableStateOf(sp.getBoolean("disableConnectionInfoOverlay", false)) }
    var selectedGUI              by remember { mutableStateOf(sp.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi") }
    var selectedAppPackage       by remember { mutableStateOf(sp.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe") }

    var serverIp                 by remember { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort               by remember { mutableStateOf(captureModeModel.serverPort.toString()) }

    // 灵动岛状态
    var dynamicIslandUsername    by remember { mutableStateOf(sp.getString("dynamicIslandUsername", "User") ?: "User") }
    var dynamicIslandYOffset     by remember { mutableStateOf(sp.getFloat("dynamicIslandYOffset", 20f)) }
    // [新增] 灵动岛缩放状态，默认值为 1.0f
    var dynamicIslandScale       by remember { mutableStateOf(sp.getFloat("dynamicIslandScale", 0.7f)) }


    var showPermission           by remember { mutableStateOf(false) }
    var showServerDialog         by remember { mutableStateOf(false) }
    var showAppDialog            by remember { mutableStateOf(false) }

    var installedApps            by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    /* ---------- 工具函数 ---------- */
    fun saveBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String)   = sp.edit().putString(key, value).apply()
    fun saveFloat(key: String, value: Float)  = sp.edit().putFloat(key, value).apply()

    fun appName(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    fun appVer(pkg: String): String = try {
        context.packageManager.getPackageInfo(pkg, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    fun appIcon(pkg: String): Drawable? = try {
        context.packageManager.getApplicationIcon(pkg)
    } catch (e: Exception) { null }

    /* ---------- 初始化 & 效果 ---------- */
    LaunchedEffect(dynamicIslandUsername) {
        delay(300) // 防抖
        dynamicIslandController.setPersistentText(dynamicIslandUsername)
    }

    LaunchedEffect(dynamicIslandYOffset) {
        dynamicIslandController.updateYOffset(dynamicIslandYOffset)
    }

    // [新增] 监听缩放值变化，并调用控制器更新
    LaunchedEffect(dynamicIslandScale) {
        dynamicIslandController.updateScale(dynamicIslandScale)
    }

    LaunchedEffect(Unit) {
        mainScreenViewModel.selectGame(selectedAppPackage)
        scope.launch(Dispatchers.IO) {
            installedApps = context.packageManager.getInstalledPackages(0)
                .filter { packageInfo ->
                    packageInfo.applicationInfo?.let { appInfo ->
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                                context.packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null
                    } ?: false
                }
                .sortedBy { appName(it.packageName) }
        }
    }

    /* ---------- 内部组合 ---------- */
    @Composable
    fun Content() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "界面选择",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                DropdownMenu(
                    options = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI"),
                    selected = selectedGUI,
                    onSelect = {
                        selectedGUI = it
                        saveStr("selectedGUI", it)
                    }
                )
                
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                Text(
                    text = "网络优化",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                SettingToggle("增强网络", "提高网络性能", optimizeNetworkEnabled) {
                    optimizeNetworkEnabled = it
                    if (it) {
                        scope.launch(Dispatchers.IO) {
                            if (NetworkOptimizer.init(context)) {
                                saveBool("optimizeNetworkEnabled", true)
                                NetworkOptimizer.optimizeSocket(Socket())
                            } else {
                                scope.launch(Dispatchers.Main) {
                                    showPermission = true
                                    optimizeNetworkEnabled = false
                                }
                            }
                        }
                    } else saveBool("optimizeNetworkEnabled", false)
                }
                
                SettingToggle("高优先级线程", "提升线程优先级", priorityThreadsEnabled) {
                    priorityThreadsEnabled = it
                    saveBool("priorityThreadsEnabled", it)
                    if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.setThreadPriority() }
                }
                
                SettingToggle("使用最快的 DNS", "使用 Google DNS", fastDnsEnabled) {
                    fastDnsEnabled = it
                    saveBool("fastDnsEnabled", it)
                    if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.useFastDNS() }
                }
                
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                Text(
                    text = "功能设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                SettingToggle("注入 Neko Pack", "增强功能", injectNekoPackEnabled) {
                    injectNekoPackEnabled = it
                    saveBool("injectNekoPackEnabled", it)
                }
                
                SettingToggle("禁用连接信息覆盖", "启动时不显示连接信息", disableOverlay) {
                    disableOverlay = it
                    saveBool("disableConnectionInfoOverlay", it)
                }
                
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Text(
                    text = "灵动岛设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = dynamicIslandUsername,
                    onValueChange = {
                        dynamicIslandUsername = it
                        saveStr("dynamicIslandUsername", it)
                    },
                    label = { Text("用户名") },
                    placeholder = { Text("在灵动岛上显示的文本") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Y 轴调整",
                             style = MaterialTheme.typography.bodyMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dynamicIslandYOffset.roundToInt()} dp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = dynamicIslandYOffset,
                        onValueChange = { dynamicIslandYOffset = it },
                        valueRange = -100f..100f,
                        steps = 199,
                        onValueChangeFinished = {
                            saveFloat("dynamicIslandYOffset", dynamicIslandYOffset)
                        }
                    )
                }

                // [新增] 缩放控制 Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "缩放调整",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "x${"%.1f".format(dynamicIslandScale)}", // 格式化为一位小数
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = dynamicIslandScale,
                        onValueChange = { dynamicIslandScale = it },
                        valueRange = 0.5f..2.0f, // 允许从 50% 缩放到 200%
                        steps = 14, // (2.0-0.5)/0.1 = 15 段, 即 14 个步进点
                        onValueChangeFinished = {
                            saveFloat("dynamicIslandScale", dynamicIslandScale)
                        }
                    )
                }
            }
        }
    }

    /* ---------- MD3 Enhanced UI ---------- */
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val screenWidthDp = configuration.screenWidthDp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isPortrait || screenWidthDp < 800) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item { Content() }
                item {
                    Text(
                        text = "连接配置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                item { ServerConfigCard(serverIp, serverPort) { showServerDialog = true } }
                item { AppManagerCard(selectedAppPackage, { showAppDialog = true }, ::appName, ::appVer, ::appIcon) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Content()
                }

                Column(
                    modifier = Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "连接配置",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ServerConfigCard(serverIp, serverPort) { showServerDialog = true }
                    AppManagerCard(selectedAppPackage, { showAppDialog = true }, ::appName, ::appVer, ::appIcon)
                }
            }
        }
    }

    /* ---------- 弹窗 ---------- */
    if (showPermission) {
        PermissionDialog(
            onDismiss = { showPermission = false },
            onRequest = {
                NetworkOptimizer.openWriteSettingsPermissionPage(context)
                showPermission = false
            }
        )
    }

    if (showServerDialog) {
        ServerConfigDialog(
            initialIp = serverIp,
            initialPort = serverPort,
            onDismiss = { showServerDialog = false },
            onSave = { ip, port ->
                serverIp = ip
                serverPort = port
                showServerDialog = false
                try {
                    val portInt = port.toInt()
                    mainScreenViewModel.selectCaptureModeModel(
                        captureModeModel.copy(serverHostName = ip, serverPort = portInt)
                    )
                    Toast.makeText(context, "服务器配置更新", Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "非法端口", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAppDialog) {
        AppSelectionDialog(
            installedApps = installedApps,
            selectedPkg = selectedAppPackage,
            onDismiss = { showAppDialog = false },
            onSelect = {
                selectedAppPackage = it
                saveStr("selectedAppPackage", it)
                mainScreenViewModel.selectGame(it)
                showAppDialog = false
                Toast.makeText(context, "已选择：${appName(it)}", Toast.LENGTH_SHORT).show()
            },
            ::appName,
            ::appVer,
            ::appIcon
        )
    }
}

/* =========================  内嵌组件  ========================= */

@Composable
private fun SettingToggle(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenu(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("界面 GUI") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ServerConfigCard(ip: String, port: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Filled.Storage,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("服务器配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("IP: $ip", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("端口: $port", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, "配置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppManagerCard(pkg: String, onClick: () -> Unit, name: (String) -> String, ver: (String) -> String, icon: (String) -> Drawable?) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Filled.Apps, null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text("应用管理器", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                icon(pkg)?.let {
                    Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                        Image(it.toBitmap(112, 112).asImageBitmap(), null, modifier = Modifier.fillMaxSize())
                    }
                } ?: Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Filled.Android, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name(pkg), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("版本 ${ver(pkg)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, "选择应用", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ServerConfigDialog(initialIp: String, initialPort: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(ip, { ip = it }, label = { Text("IP地址") }, singleLine = true)
                OutlinedTextField(port, { port = it }, label = { Text("端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(ip, port) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun PermissionDialog(onDismiss: () -> Unit, onRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要权限") },
        text = { Text("网络优化需要系统写入设置权限，是否前往设置页面授予？") },
        confirmButton = { Button(onClick = onRequest) { Text("前往") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AppSelectionDialog(installedApps: List<PackageInfo>, selectedPkg: String, onDismiss: () -> Unit, onSelect: (String) -> Unit, name: (String) -> String, ver: (String) -> String, icon: (String) -> Drawable?) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择客户端") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(installedApps, key = { it.packageName }) { pkgInfo ->
                    val p = pkgInfo.packageName
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon(p)?.let {
                            Image(it.toBitmap(64, 64).asImageBitmap(), null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                        } ?: Spacer(Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name(p), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("v${ver(p)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (p == selectedPkg) {
                            Icon(Icons.Default.Check, "已选择", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}