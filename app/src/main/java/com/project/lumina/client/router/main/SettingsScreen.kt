/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.util.NetworkOptimizer
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket

/* =========================  唯一入口  ========================= */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    val sp = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    /* ---------- 状态 ---------- */
    var optimizeNetworkEnabled by remember { mutableStateOf(sp.getBoolean("optimizeNetworkEnabled", false)) }
    var priorityThreadsEnabled by remember { mutableStateOf(sp.getBoolean("priorityThreadsEnabled", false)) }
    var fastDnsEnabled by remember { mutableStateOf(sp.getBoolean("fastDnsEnabled", false)) }
    var injectNekoPackEnabled by remember { mutableStateOf(sp.getBoolean("injectNekoPackEnabled", false)) }
    var disableOverlay by remember { mutableStateOf(sp.getBoolean("disableConnectionInfoOverlay", false)) }
    var selectedGUI by remember { mutableStateOf(sp.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi") }
    var selectedAppPackage by remember { mutableStateOf(sp.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe") }

    var serverIp by remember { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by remember { mutableStateOf(captureModeModel.serverPort.toString()) }

    var showPermission by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }

    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    /* ---------- 工具函数 ---------- */
    fun saveBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String) = sp.edit().putString(key, value).apply()

    fun appName(pkg: String): String = try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    fun appVer(pkg: String): String = try {
        context.packageManager.getPackageInfo(pkg, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    fun appIcon(pkg: String): Drawable? = try {
        context.packageManager.getApplicationIcon(pkg)
    } catch (e: Exception) { null }

    /* ---------- 初始化 ---------- */
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
                .sortedBy { appName(it.packageName).lowercase() }
        }
    }

    /* ---------- UI 布局 ---------- */
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    Box(Modifier.fillMaxSize()) {
        if (isPortrait) {
            // 竖屏布局：单列滚动
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GeneralSettingsCard(
                    scope, context,
                    selectedGUI, { selectedGUI = it; saveStr("selectedGUI", it) },
                    optimizeNetworkEnabled, { optimizeNetworkEnabled = it; if (it) showPermission = !NetworkOptimizer.init(context); saveBool("optimizeNetworkEnabled", it) },
                    priorityThreadsEnabled, { priorityThreadsEnabled = it; saveBool("priorityThreadsEnabled", it) },
                    fastDnsEnabled, { fastDnsEnabled = it; saveBool("fastDnsEnabled", it) },
                    injectNekoPackEnabled, { injectNekoPackEnabled = it; saveBool("injectNekoPackEnabled", it) },
                    disableOverlay, { disableOverlay = it; saveBool("disableConnectionInfoOverlay", it) }
                )
                ManagementCards(
                    serverIp, serverPort, { showServerDialog = true },
                    selectedAppPackage, { showAppDialog = true },
                    ::appName, ::appVer, ::appIcon
                )
            }
        } else {
            // 横屏布局：双列滚动
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val scrollState1 = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState1),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GeneralSettingsCard(
                        scope, context,
                        selectedGUI, { selectedGUI = it; saveStr("selectedGUI", it) },
                        optimizeNetworkEnabled, { optimizeNetworkEnabled = it; if (it) showPermission = !NetworkOptimizer.init(context); saveBool("optimizeNetworkEnabled", it) },
                        priorityThreadsEnabled, { priorityThreadsEnabled = it; saveBool("priorityThreadsEnabled", it) },
                        fastDnsEnabled, { fastDnsEnabled = it; saveBool("fastDnsEnabled", it) },
                        injectNekoPackEnabled, { injectNekoPackEnabled = it; saveBool("injectNekoPackEnabled", it) },
                        disableOverlay, { disableOverlay = it; saveBool("disableConnectionInfoOverlay", it) }
                    )
                }
                val scrollState2 = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState2),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ManagementCards(
                        serverIp, serverPort, { showServerDialog = true },
                        selectedAppPackage, { showAppDialog = true },
                        ::appName, ::appVer, ::appIcon
                    )
                }
            }
        }
    }

    /* ---------- 弹窗 ---------- */
    if (showPermission) {
        PermissionDialog(onDismiss = { showPermission = false }) {
            NetworkOptimizer.openWriteSettingsPermissionPage(context)
            showPermission = false
        }
    }

    if (showServerDialog) {
        ServerConfigDialog(
            initialIp = serverIp, initialPort = serverPort,
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
                    Toast.makeText(context, "服务器配置已更新", Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "端口号无效", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAppDialog) {
        AppSelectionDialog(
            installedApps = installedApps, selectedPkg = selectedAppPackage,
            onDismiss = { showAppDialog = false },
            onSelect = { pkg ->
                selectedAppPackage = pkg
                saveStr("selectedAppPackage", pkg)
                mainScreenViewModel.selectGame(pkg)
                showAppDialog = false
                Toast.makeText(context, "已选择: ${appName(pkg)}", Toast.LENGTH_SHORT).show()
            },
            ::appName, ::appVer, ::appIcon
        )
    }
}

/* =========================  内嵌组件  ========================= */

@Composable
private fun GeneralSettingsCard(
    scope: CoroutineScope, context: Context,
    selectedGUI: String, onGuiSelect: (String) -> Unit,
    optimizeNetworkEnabled: Boolean, onOptimizeNetworkChange: (Boolean) -> Unit,
    priorityThreadsEnabled: Boolean, onPriorityThreadsChange: (Boolean) -> Unit,
    fastDnsEnabled: Boolean, onFastDnsChange: (Boolean) -> Unit,
    injectNekoPackEnabled: Boolean, onInjectNekoPackChange: (Boolean) -> Unit,
    disableOverlay: Boolean, onDisableOverlayChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                "通用设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            DropdownMenu(
                options = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI"),
                selected = selectedGUI,
                onSelect = onGuiSelect
            )
            Spacer(Modifier.height(8.dp))
            Divider()
            
            SettingToggle("增强网络", "提高网络性能", optimizeNetworkEnabled) {
                onOptimizeNetworkChange(it)
                if (it) {
                    scope.launch(Dispatchers.IO) {
                        if (NetworkOptimizer.init(context)) {
                            NetworkOptimizer.optimizeSocket(Socket())
                        } else {
                            scope.launch(Dispatchers.Main) {
                                onOptimizeNetworkChange(false) // Revert state if permission is denied/needed
                            }
                        }
                    }
                }
            }
            Divider()
            SettingToggle("高优先级线程", "提升线程优先级", priorityThreadsEnabled) {
                onPriorityThreadsChange(it)
                if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.setThreadPriority() }
            }
            Divider()
            SettingToggle("使用最快的 DNS", "使用 Google DNS", fastDnsEnabled) {
                onFastDnsChange(it)
                if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.useFastDNS() }
            }
            Divider()
            SettingToggle("注入 Neko Pack", "增强功能", injectNekoPackEnabled, onInjectNekoPackChange)
            Divider()
            SettingToggle("禁用连接信息覆盖", "启动时不显示连接信息", disableOverlay, onDisableOverlayChange)
        }
    }
}

@Composable
private fun ManagementCards(
    serverIp: String, serverPort: String, onServerClick: () -> Unit,
    selectedAppPackage: String, onAppClick: () -> Unit,
    appName: (String) -> String, appVer: (String) -> String, appIcon: (String) -> Drawable?
) {
    ServerConfigCard(serverIp, serverPort, onServerClick)
    Spacer(Modifier.height(16.dp))
    AppManagerCard(selectedAppPackage, onAppClick, appName, appVer, appIcon)
}


/* ---- SettingToggle ---- */
@Composable
private fun SettingToggle(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null) // Pass null as onChange is handled by the Row
    }
}

/* ---- DropdownMenu ---- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenu(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("界面 GUI") },
            leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = "GUI 选择") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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


/* ---- ServerConfigCard ---- */
@Composable
private fun ServerConfigCard(ip: String, port: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text("服务器配置", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("IP: $ip", style = MaterialTheme.typography.bodyMedium)
            Text("端口: $port", style = MaterialTheme.typography.bodyMedium)
        }
    }
}


/* ---- AppManagerCard ---- */
@Composable
private fun AppManagerCard(
    pkg: String,
    onClick: () -> Unit,
    name: (String) -> String,
    ver: (String) -> String,
    icon: (String) -> Drawable?
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text("应用管理器", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                icon(pkg)?.let {
                    Image(
                        bitmap = it.toBitmap(64, 64).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(name(pkg), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("v${ver(pkg)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "选择应用")
            }
        }
    }
}


/* ---- ServerConfigDialog ---- */
@Composable
private fun ServerConfigDialog(
    initialIp: String, initialPort: String,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit
) {
    var ip by remember { mutableStateof(initialIp) }
    var port by remember { mutableStateof(initialPort) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ip, onValueChange = { ip = it },
                    label = { Text("IP地址") }, singleLine = true
                )
                OutlinedTextField(
                    value = port, onValueChange = { port = it },
                    label = { Text("端口") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(ip, port) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/* ---- PermissionDialog ---- */
@Composable
private fun PermissionDialog(onDismiss: () -> Unit, onRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
        title = { Text("需要权限") },
        text = { Text("网络优化需要系统写入设置权限，是否前往设置页面授予？") },
        confirmButton = { Button(onClick = onRequest) { Text("前往") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/* ---- AppSelectionDialog ---- */
@Composable
private fun AppSelectionDialog(
    installedApps: List<PackageInfo>, selectedPkg: String,
    onDismiss: () -> Unit, onSelect: (String) -> Unit,
    name: (String) -> String, ver: (String) -> String, icon: (String) -> Drawable?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择客户端") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(installedApps, key = { it.packageName }) { pkgInfo ->
                    val p = pkgInfo.packageName
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(p) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon(p)?.let {
                            Image(
                                bitmap = it.toBitmap(64, 64).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } ?: Spacer(Modifier.size(40.dp))

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(name(p), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("v${ver(p)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (p == selectedPkg) {
                            Icon(Icons.Default.Check, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = null
    )
}