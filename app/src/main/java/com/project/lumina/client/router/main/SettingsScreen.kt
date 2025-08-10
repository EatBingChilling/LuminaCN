/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * Material Design 3 Expressive 风格重写
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.util.NetworkOptimizer
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket

/* ======================================================
   主入口
   ====================================================== */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val scope = rememberCoroutineScope()
    val vm: MainScreenViewModel = viewModel()
    val captureModeModel by vm.captureModeModel.collectAsState()
    val sp = remember { context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE) }

    /* 状态 */
    var optimizeNetwork by remember { mutableStateOf(sp.getBoolean("optimizeNetworkEnabled", false)) }
    var priorityThreads by remember { mutableStateOf(sp.getBoolean("priorityThreadsEnabled", false)) }
    var fastDns by remember { mutableStateOf(sp.getBoolean("fastDnsEnabled", false)) }
    var injectNekoPack by remember { mutableStateOf(sp.getBoolean("injectNekoPackEnabled", false)) }
    var disableOverlay by remember { mutableStateOf(sp.getBoolean("disableConnectionInfoOverlay", false)) }
    var selectedGUI by remember { mutableStateOf(sp.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi") }
    var selectedApp by remember { mutableStateOf(sp.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe") }

    var serverIp by remember { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by remember { mutableStateOf(captureModeModel.serverPort.toString()) }

    var showPermission by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    /* 工具函数 */
    fun saveBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String) = sp.edit().putString(key, value).apply()

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

    /* 初始化 */
    LaunchedEffect(Unit) {
        vm.selectGame(selectedApp)
        scope.launch(Dispatchers.IO) {
            installedApps = context.packageManager.getInstalledPackages(0)
                .filter { pkg ->
                    pkg.applicationInfo?.let { ai ->
                        (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                                context.packageManager.getLaunchIntentForPackage(pkg.packageName) != null
                    } ?: false
                }
                .sortedBy { appName(it.packageName) }
        }
    }

    Box(
        modifier = Modifier
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
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left: Settings
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                ) {
                    SettingsContent(
                        optimizeNetwork = optimizeNetwork,
                        priorityThreads = priorityThreads,
                        fastDns = fastDns,
                        injectNekoPack = injectNekoPack,
                        disableOverlay = disableOverlay,
                        selectedGUI = selectedGUI,
                        onOptimizeChanged = { optimizeNetwork = it; saveBool("optimizeNetworkEnabled", it) },
                        onPriorityChanged = { priorityThreads = it; saveBool("priorityThreadsEnabled", it) },
                        onDnsChanged = { fastDns = it; saveBool("fastDnsEnabled", it) },
                        onNekoChanged = { injectNekoPack = it; saveBool("injectNekoPackEnabled", it) },
                        onOverlayChanged = { disableOverlay = it; saveBool("disableConnectionInfoOverlay", it) },
                        onGUIChanged = { selectedGUI = it; saveStr("selectedGUI", it) }
                    )
                }

                // Right: Server & App Cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ServerConfigCard(
                        ip = serverIp,
                        port = serverPort,
                        onClick = { showServerDialog = true }
                    )
                    AppManagerCard(
                        selectedPackage = selectedApp,
                        onClick = { showAppDialog = true },
                        appName = ::appName,
                        appVer = ::appVer,
                        appIcon = ::appIcon
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsContent(
                    optimizeNetwork = optimizeNetwork,
                    priorityThreads = priorityThreads,
                    fastDns = fastDns,
                    injectNekoPack = injectNekoPack,
                    disableOverlay = disableOverlay,
                    selectedGUI = selectedGUI,
                    onOptimizeChanged = { optimizeNetwork = it; saveBool("optimizeNetworkEnabled", it) },
                    onPriorityChanged = { priorityThreads = it; saveBool("priorityThreadsEnabled", it) },
                    onDnsChanged = { fastDns = it; saveBool("fastDnsEnabled", it) },
                    onNekoChanged = { injectNekoPack = it; saveBool("injectNekoPackEnabled", it) },
                    onOverlayChanged = { disableOverlay = it; saveBool("disableConnectionInfoOverlay", it) },
                    onGUIChanged = { selectedGUI = it; saveStr("selectedGUI", it) }
                )

                ServerConfigCard(
                    ip = serverIp,
                    port = serverPort,
                    onClick = { showServerDialog = true }
                )

                AppManagerCard(
                    selectedPackage = selectedApp,
                    onClick = { showAppDialog = true },
                    appName = ::appName,
                    appVer = ::appVer,
                    appIcon = ::appIcon
                )
            }
        }

        /* 对话框 */
        if (showPermission) {
            AlertDialog(
                onDismissRequest = { showPermission = false },
                title = { Text("需要权限") },
                text = { Text("网络优化需要系统写入设置权限，是否前往设置页面授予？") },
                confirmButton = {
                    Button(onClick = {
                        NetworkOptimizer.openWriteSettingsPermissionPage(context)
                        showPermission = false
                    }) { Text("前往") }
                },
                dismissButton = {
                    TextButton(onClick = { showPermission = false }) { Text("取消") }
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
                        vm.selectCaptureModeModel(
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
                selectedPkg = selectedApp,
                onDismiss = { showAppDialog = false },
                onSelect = {
                    selectedApp = it
                    saveStr("selectedAppPackage", it)
                    vm.selectGame(it)
                    showAppDialog = false
                    Toast.makeText(context, "已选择：${appName(it)}", Toast.LENGTH_SHORT).show()
                },
                appName = ::appName,
                appVer = ::appVer,
                appIcon = ::appIcon
            )
        }
    }
}

/* ======================================================
   内容组件
   ====================================================== */
@Composable
private fun SettingsContent(
    optimizeNetwork: Boolean,
    priorityThreads: Boolean,
    fastDns: Boolean,
    injectNekoPack: Boolean,
    disableOverlay: Boolean,
    selectedGUI: String,
    onOptimizeChanged: (Boolean) -> Unit,
    onPriorityChanged: (Boolean) -> Unit,
    onDnsChanged: (Boolean) -> Unit,
    onNekoChanged: (Boolean) -> Unit,
    onOverlayChanged: (Boolean) -> Unit,
    onGUIChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("界面设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            GUISetting(selectedGUI, onGUIChanged)
            
            Divider()
            
            Text("网络优化", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            SettingToggle(
                title = "增强网络",
                description = "提高网络性能",
                checked = optimizeNetwork,
                onChange = { checked ->
                    onOptimizeChanged(checked)
                    if (checked) {
                        scope.launch(Dispatchers.IO) {
                            if (NetworkOptimizer.init(context)) {
                                NetworkOptimizer.optimizeSocket(Socket())
                            } else {
                                scope.launch {
                                    // 权限请求逻辑
                                }
                            }
                        }
                    }
                }
            )
            
            SettingToggle(
                title = "高优先级线程",
                description = "提升线程优先级",
                checked = priorityThreads,
                onChange = { checked ->
                    onPriorityChanged(checked)
                    if (checked) scope.launch(Dispatchers.IO) { NetworkOptimizer.setThreadPriority() }
                }
            )
            
            SettingToggle(
                title = "使用最快的 DNS",
                description = "使用 Google DNS",
                checked = fastDns,
                onChange = { checked ->
                    onDnsChanged(checked)
                    if (checked) scope.launch(Dispatchers.IO) { NetworkOptimizer.useFastDNS() }
                }
            )
            
            Divider()
            
            Text("功能增强", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            SettingToggle(
                title = "注入 Neko Pack",
                description = "增强功能",
                checked = injectNekoPack,
                onChange = onNekoChanged
            )
            
            SettingToggle(
                title = "禁用连接信息覆盖",
                description = "启动时不显示连接信息",
                checked = disableOverlay,
                onChange = onOverlayChanged
            )
        }
    }
}

@Composable
private fun GUISetting(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("界面 GUI") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ServerConfigCard(ip: String, port: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("服务器配置", style = MaterialTheme.typography.titleLarge)
                    Text("IP：$ip", style = MaterialTheme.typography.bodyLarge)
                    Text("端口：$port", style = MaterialTheme.typography.bodyMedium)
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AppManagerCard(
    selectedPackage: String,
    onClick: () -> Unit,
    appName: (String) -> String,
    appVer: (String) -> String,
    appIcon: (String) -> Drawable?
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                appIcon(selectedPackage)?.let {
                    Image(
                        bitmap = it.toBitmap(64, 64).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Column {
                    Text("应用管理器", style = MaterialTheme.typography.titleLarge)
                    Text(appName(selectedPackage), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("v${appVer(selectedPackage)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "选择",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ServerConfigDialog(
    initialIp: String,
    initialPort: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器配置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP地址") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ip, port) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AppSelectionDialog(
    installedApps: List<PackageInfo>,
    selectedPkg: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    appName: (String) -> String,
    appVer: (String) -> String,
    appIcon: (String) -> Drawable?
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("选择客户端", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { pkgInfo ->
                        val pkg = pkgInfo.packageName
                        Surface(
                            selected = pkg == selectedPkg,
                            onClick = { onSelect(pkg) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (pkg == selectedPkg) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                appIcon(pkg)?.let {
                                    Image(
                                        bitmap = it.toBitmap(48, 48).asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } ?: Spacer(Modifier.size(40.dp))
                                
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        appName(pkg),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "v${appVer(pkg)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (pkg == selectedPkg) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("关闭") }
            }
        }
    }
}
