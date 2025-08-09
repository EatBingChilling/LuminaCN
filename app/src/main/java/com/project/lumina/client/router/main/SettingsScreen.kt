/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.util.NetworkOptimizer
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.Socket

/* =========================  唯一入口  ========================= */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    val sp = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    /* ---------- 状态 ---------- */
    var optimizeNetworkEnabled   by remember { mutableStateOf(sp.getBoolean("optimizeNetworkEnabled", false)) }
    var priorityThreadsEnabled   by remember { mutableStateOf(sp.getBoolean("priorityThreadsEnabled", false)) }
    var fastDnsEnabled           by remember { mutableStateOf(sp.getBoolean("fastDnsEnabled", false)) }
    var injectNekoPackEnabled    by remember { mutableStateOf(sp.getBoolean("injectNekoPackEnabled", false)) }
    var disableOverlay           by remember { mutableStateOf(sp.getBoolean("disableConnectionInfoOverlay", false)) }
    var selectedGUI              by remember { mutableStateOf(sp.getString("selectedGUI", "KitsuGUI") ?: "KitsuGUI") }
    var selectedAppPackage       by remember { mutableStateOf(sp.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe") }

    var serverIp   by remember { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by remember { mutableStateOf(captureModeModel.serverPort.toString()) }

    var showPermission      by remember { mutableStateOf(false) }
    var showServerDialog    by remember { mutableStateOf(false) }
    var showAppDialog       by remember { mutableStateOf(false) }

    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    /* ---------- 工具函数 ---------- */
    fun saveBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String)   = sp.edit().putString(key, value).apply()

    fun appName(pkg: String) = try {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(pkg, 0)
        ).toString()
    } catch (e: Exception) { pkg }

    fun appVer(pkg: String) = try {
        context.packageManager.getPackageInfo(pkg, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    fun appIcon(pkg: String) = try {
        context.packageManager.getApplicationIcon(pkg)
    } catch (e: Exception) { null }

    /* ---------- 初始化 ---------- */
    LaunchedEffect(Unit) {
        mainScreenViewModel.selectGame(selectedAppPackage)
        scope.launch(Dispatchers.IO) {
            installedApps = context.packageManager.getInstalledPackages(0)
                .filter {
                    val ai = it.applicationInfo
                    ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 &&
                            context.packageManager.getLaunchIntentForPackage(it.packageName) != null
                }
                .sortedBy { appName(it.packageName) }
        }
    }

    /* ---------- UI ---------- */
    val isPortrait = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    Box(Modifier.fillMaxSize()) {
        if (isPortrait) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { Content() }
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) { Content() }

                Column(
                    Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

    /* ---------- 内部组合 ---------- */
    @Composable
    fun Content() {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DropdownMenu(
                    options = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI"),
                    selected = selectedGUI,
                    onSelect = {
                        selectedGUI = it
                        saveStr("selectedGUI", it)
                    }
                )
                Divider()
                SettingToggle("增强网络", "提高网络性能", optimizeNetworkEnabled) {
                    if (it) {
                        scope.launch(Dispatchers.IO) {
                            if (NetworkOptimizer.init(context)) {
                                saveBool("optimizeNetworkEnabled", true)
                                NetworkOptimizer.optimizeSocket(Socket())
                            } else scope.launch(Dispatchers.Main) { showPermission = true }
                        }
                    } else saveBool("optimizeNetworkEnabled", false)
                }
                Divider()
                SettingToggle("高优先级线程", "提升线程优先级", priorityThreadsEnabled) {
                    saveBool("priorityThreadsEnabled", it)
                    if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.setThreadPriority() }
                }
                Divider()
                SettingToggle("使用最快的 DNS", "使用 Google DNS", fastDnsEnabled) {
                    saveBool("fastDnsEnabled", it)
                    if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.useFastDNS() }
                }
                Divider()
                SettingToggle("注入 Neko Pack", "增强功能", injectNekoPackEnabled) { saveBool("injectNekoPackEnabled", it) }
                Divider()
                SettingToggle("禁用连接信息覆盖", "启动时不显示连接信息", disableOverlay) { saveBool("disableConnectionInfoOverlay", it) }
            }
        }
    }
}

/* =========================  内嵌组件  ========================= */

/* ---- SettingToggle ---- */
@Composable
private fun SettingToggle(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) = Row(
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

/* ---- DropdownMenu ---- */
@Composable
private fun DropdownMenu(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("界面 GUI", style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            Card(
                onClick = { expanded = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selected)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(it) },
                        onClick = { onSelect(it); expanded = false }
                    )
                }
            }
        }
    }
}

/* ---- ServerConfigCard ---- */
@Composable
private fun ServerConfigCard(ip: String, port: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("服务器配置", style = MaterialTheme.typography.titleMedium)
            Text("IP: $ip  端口: $port", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick) { Text("配置") }
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
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("App 管理器", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon(pkg)?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it.toBitmap(32, 32).asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(name(pkg), style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = onClick) { Text("选择") }
            }
        }
    }
}

/* ---- ServerConfigDialog ---- */
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP地址") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ip, port) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/* ---- PermissionDialog ---- */
@Composable
private fun PermissionDialog(onDismiss: () -> Unit, onRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要权限") },
        text = { Text("网络优化需要特殊权限，是否前往设置？") },
        confirmButton = {
            Button(onClick = onRequest) { Text("前往") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/* ---- AppSelectionDialog ---- */
@Composable
private fun AppSelectionDialog(
    installedApps: List<PackageInfo>,
    selectedPkg: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    name: (String) -> String,
    ver: (String) -> String,
    icon: (String) -> Drawable?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择客户端") },
        text = {
            LazyColumn(Modifier.height(300.dp)) {
                items(installedApps) { pkg ->
                    val p = pkg.packageName
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(p) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon(p)?.let {
                            androidx.compose.foundation.Image(
                                bitmap = it.toBitmap(40, 40).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            Text(name(p), style = MaterialTheme.typography.bodyMedium)
                            Text("v${ver(p)}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (p == selectedPkg) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
