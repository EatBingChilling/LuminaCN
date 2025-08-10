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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()
    val sp = remember { context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE) }

    var optimizeNetworkEnabled by remember { mutableStateOf(sp.getBoolean("optimizeNetworkEnabled", false)) }
    var priorityThreadsEnabled by remember { mutableStateOf(sp.getBoolean("priorityThreadsEnabled", false)) }
    var fastDnsEnabled by remember { mutableStateOf(sp.getBoolean("fastDnsEnabled", false)) }
    var injectNekoPackEnabled by remember { mutableStateOf(sp.getBoolean("injectNekoPackEnabled", false)) }
    var disableOverlay by remember { mutableStateOf(sp.getBoolean("disableConnectionInfoOverlay", false)) }
    var selectedGUI by remember { mutableStateOf(sp.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi") }
    var selectedAppPackage by remember { mutableStateOf(sp.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe") }

    var serverIp by remember(captureModeModel) { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by remember(captureModeModel) { mutableStateOf(captureModeModel.serverPort.toString()) }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }

    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    fun saveBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun saveStr(key: String, value: String) = sp.edit().putString(key, value).apply()
    fun getAppName(pkg: String): String = try { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
    fun getAppVersion(pkg: String): String = try { context.packageManager.getPackageInfo(pkg, 0).versionName ?: "?" } catch (e: Exception) { "?" }
    fun getAppIcon(pkg: String): Drawable? = try { context.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }

    LaunchedEffect(Unit) {
        mainScreenViewModel.selectGame(selectedAppPackage)
        scope.launch(Dispatchers.IO) {
            installedApps = context.packageManager.getInstalledPackages(0)
                .filter { it.applicationInfo?.let { app -> (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && context.packageManager.getLaunchIntentForPackage(it.packageName) != null } ?: false }
                .sortedBy { getAppName(it.packageName).lowercase() }
        }
    }

    val isPortrait = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    if (isPortrait) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsContent(scope, context, selectedGUI, { selectedGUI = it; saveStr("selectedGUI", it) }, optimizeNetworkEnabled, { optimizeNetworkEnabled = it; if (it) showPermissionDialog = !NetworkOptimizer.init(context); saveBool("optimizeNetworkEnabled", it) }, priorityThreadsEnabled, { priorityThreadsEnabled = it; saveBool("priorityThreadsEnabled", it) }, fastDnsEnabled, { fastDnsEnabled = it; saveBool("fastDnsEnabled", it) }, injectNekoPackEnabled, { injectNekoPackEnabled = it; saveBool("injectNekoPackEnabled", it) }, disableOverlay, { disableOverlay = it; saveBool("disableConnectionInfoOverlay", it) },
                serverIp, serverPort, { showServerDialog = true }, selectedAppPackage, { showAppDialog = true }, ::getAppName, ::getAppVersion, ::getAppIcon)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GeneralSettingsCard(scope, context, selectedGUI, { selectedGUI = it; saveStr("selectedGUI", it) }, optimizeNetworkEnabled, { optimizeNetworkEnabled = it; if (it) showPermissionDialog = !NetworkOptimizer.init(context); saveBool("optimizeNetworkEnabled", it) }, priorityThreadsEnabled, { priorityThreadsEnabled = it; saveBool("priorityThreadsEnabled", it) }, fastDnsEnabled, { fastDnsEnabled = it; saveBool("fastDnsEnabled", it) }, injectNekoPackEnabled, { injectNekoPackEnabled = it; saveBool("injectNekoPackEnabled", it) }, disableOverlay, { disableOverlay = it; saveBool("disableConnectionInfoOverlay", it) })
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ManagementCards(serverIp, serverPort, { showServerDialog = true }, selectedAppPackage, { showAppDialog = true }, ::getAppName, ::getAppVersion, ::getAppIcon)
            }
        }
    }

    if (showPermissionDialog) {
        PermissionDialog(onDismiss = { showPermissionDialog = false }) { NetworkOptimizer.openWriteSettingsPermissionPage(context); showPermissionDialog = false }
    }
    if (showServerDialog) {
        ServerConfigDialog(initialIp = serverIp, initialPort = serverPort,
            onDismiss = { showServerDialog = false },
            onSave = { ip, port ->
                serverIp = ip; serverPort = port; showServerDialog = false
                try {
                    mainScreenViewModel.selectCaptureModeModel(captureModeModel.copy(serverHostName = ip, serverPort = port.toInt()))
                    Toast.makeText(context, "服务器配置已更新", Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) { Toast.makeText(context, "端口号无效", Toast.LENGTH_SHORT).show() }
            }
        )
    }
    if (showAppDialog) {
        AppSelectionDialog(installedApps = installedApps, selectedPkg = selectedAppPackage,
            onDismiss = { showAppDialog = false },
            onSelect = { pkg ->
                selectedAppPackage = pkg; saveStr("selectedAppPackage", pkg); mainScreenViewModel.selectGame(pkg); showAppDialog = false
                Toast.makeText(context, "已选择: ${getAppName(pkg)}", Toast.LENGTH_SHORT).show()
            }, ::getAppName, ::getAppVersion, ::getAppIcon
        )
    }
}

@Composable
private fun SettingsContent(
    scope: CoroutineScope, context: Context,
    selectedGUI: String, onGuiSelect: (String) -> Unit,
    optimizeNetworkEnabled: Boolean, onOptimizeNetworkChange: (Boolean) -> Unit,
    priorityThreadsEnabled: Boolean, onPriorityThreadsChange: (Boolean) -> Unit,
    fastDnsEnabled: Boolean, onFastDnsChange: (Boolean) -> Unit,
    injectNekoPackEnabled: Boolean, onInjectNekoPackChange: (Boolean) -> Unit,
    disableOverlay: Boolean, onDisableOverlayChange: (Boolean) -> Unit,
    serverIp: String, serverPort: String, onServerClick: () -> Unit,
    selectedAppPackage: String, onAppClick: () -> Unit,
    getAppName: (String) -> String, getAppVersion: (String) -> String, getAppIcon: (String) -> Drawable?
) {
    GeneralSettingsCard(scope, context, selectedGUI, onGuiSelect, optimizeNetworkEnabled, onOptimizeNetworkChange, priorityThreadsEnabled, onPriorityThreadsChange, fastDnsEnabled, onFastDnsChange, injectNekoPackEnabled, onInjectNekoPackChange, disableOverlay, onDisableOverlayChange)
    ManagementCards(serverIp, serverPort, onServerClick, selectedAppPackage, onAppClick, getAppName, getAppVersion, getAppIcon)
}

@Composable
private fun GeneralSettingsCard(
    scope: CoroutineScope, context: Context, selectedGUI: String, onGuiSelect: (String) -> Unit, optimizeNetworkEnabled: Boolean, onOptimizeNetworkChange: (Boolean) -> Unit,
    priorityThreadsEnabled: Boolean, onPriorityThreadsChange: (Boolean) -> Unit, fastDnsEnabled: Boolean, onFastDnsChange: (Boolean) -> Unit,
    injectNekoPackEnabled: Boolean, onInjectNekoPackChange: (Boolean) -> Unit, disableOverlay: Boolean, onDisableOverlayChange: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text("通用设置", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            DropdownSetting(options = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI"), selected = selectedGUI, onSelect = onGuiSelect)
            Divider(Modifier.padding(horizontal = 16.dp))
            SettingToggle("网络优化", "提高网络性能和稳定性", optimizeNetworkEnabled) {
                onOptimizeNetworkChange(it)
                if (it) scope.launch(Dispatchers.IO) { if (!NetworkOptimizer.init(context)) { scope.launch { onOptimizeNetworkChange(false) } } else NetworkOptimizer.optimizeSocket(Socket()) }
            }
            Divider(Modifier.padding(horizontal = 16.dp))
            SettingToggle("高优先级线程", "为游戏和应用分配更高线程优先级", priorityThreadsEnabled) { onPriorityThreadsChange(it); if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.setThreadPriority() } }
            Divider(Modifier.padding(horizontal = 16.dp))
            SettingToggle("快速 DNS", "使用 Google DNS (8.8.8.8)", fastDnsEnabled) { onFastDnsChange(it); if (it) scope.launch(Dispatchers.IO) { NetworkOptimizer.useFastDNS() } }
            Divider(Modifier.padding(horizontal = 16.dp))
            SettingToggle("注入 Neko Pack", "启动时自动应用 Neko 材质包", injectNekoPackEnabled, onInjectNekoPackChange)
            Divider(Modifier.padding(horizontal = 16.dp))
            SettingToggle("禁用连接浮窗", "启动游戏时不显示IP连接信息", disableOverlay, onDisableOverlayChange)
        }
    }
}

@Composable
private fun ManagementCards(
    serverIp: String, serverPort: String, onServerClick: () -> Unit, selectedAppPackage: String, onAppClick: () -> Unit,
    appName: (String) -> String, appVer: (String) -> String, appIcon: (String) -> Drawable?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text("管理", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            ServerConfigCard(serverIp, serverPort, onServerClick)
            Divider(Modifier.padding(horizontal = 16.dp))
            AppManagerCard(selectedAppPackage, onAppClick, appName, appVer, appIcon)
        }
    }
}

@Composable
private fun SettingToggle(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected, onValueChange = {}, readOnly = true, label = { Text("界面 GUI") },
                leadingIcon = { Icon(Icons.Filled.Dashboard, contentDescription = "GUI 选择") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun ServerConfigCard(ip: String, port: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Filled.Dns, contentDescription = "服务器配置", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text("服务器配置", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("当前: $ip:$port", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "编辑")
    }
}

@Composable
private fun AppManagerCard(pkg: String, onClick: () -> Unit, name: (String) -> String, ver: (String) -> String, icon: (String) -> Drawable?) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon(pkg)?.let {
            Image(bitmap = it.toBitmap(96, 96).asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
        } ?: Icon(Icons.Filled.Apps, contentDescription = "应用管理器", modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text("游戏客户端", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(name(pkg), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "选择应用")
    }
}

@Composable
private fun ServerConfigDialog(initialIp: String, initialPort: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑服务器配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP地址") }, singleLine = true)
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(ip, port) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun PermissionDialog(onDismiss: () -> Unit, onRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
        title = { Text("需要权限") }, text = { Text("网络优化需要系统写入设置权限，是否前往设置页面授予？") },
        confirmButton = { Button(onClick = onRequest) { Text("前往") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AppSelectionDialog(
    installedApps: List<PackageInfo>, selectedPkg: String, onDismiss: () -> Unit, onSelect: (String) -> Unit,
    name: (String) -> String, ver: (String) -> String, icon: (String) -> Drawable?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择游戏客户端") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(installedApps, key = { it.packageName }) { pkgInfo ->
                    val p = pkgInfo.packageName
                    ListItem(
                        headlineContent = { Text(name(p), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("v${ver(p)}") },
                        leadingContent = {
                            icon(p)?.let { Image(bitmap = it.toBitmap(96, 96).asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))) }
                                ?: Box(Modifier.size(40.dp))
                        },
                        trailingContent = { if (p == selectedPkg) Icon(Icons.Default.Check, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { onSelect(p) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Preview
@Composable
private fun SettingsPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}