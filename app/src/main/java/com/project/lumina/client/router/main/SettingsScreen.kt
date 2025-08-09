/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    
    val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    
    var optimizeNetworkEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("optimizeNetworkEnabled", false))
    }
    var priorityThreadsEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("priorityThreadsEnabled", false))
    }
    var fastDnsEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("fastDnsEnabled", false))
    }
    var injectNekoPackEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("injectNekoPackEnabled", false))
    }
    var disableConnectionInfoOverlay by remember {
        mutableStateOf(sharedPreferences.getBoolean("disableConnectionInfoOverlay", false))
    }
    var selectedGUI by remember {
        mutableStateOf(sharedPreferences.getString("selectedGUI", "KitsuGUI") ?: "KitsuGUI")
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showAppSelectionDialog by remember { mutableStateOf(false) }

    var selectedAppPackage by remember {
        mutableStateOf(sharedPreferences.getString("selectedAppPackage", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe")
    }
    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    
    fun saveToggleState(key: String, value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    fun saveGUISelection(value: String) {
        with(sharedPreferences.edit()) {
            putString("selectedGUI", value)
            apply()
        }
    }

    fun saveSelectedApp(packageName: String) {
        with(sharedPreferences.edit()) {
            putString("selectedAppPackage", packageName)
            apply()
        }
        mainScreenViewModel.selectGame(packageName)
    }

    fun getAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getAppVersion(packageName: String): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    LaunchedEffect(Unit) {
        mainScreenViewModel.selectGame(selectedAppPackage)

        scope.launch(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                val packages = packageManager.getInstalledPackages(0)
                    .filter { packageInfo ->
                        val appInfo = packageInfo.applicationInfo
                        appInfo != null &&
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) &&
                        packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null
                    }
                    .sortedBy { packageInfo ->
                        val appInfo = packageInfo.applicationInfo
                        if (appInfo != null) {
                            packageManager.getApplicationLabel(appInfo).toString()
                        } else {
                            packageInfo.packageName
                        }
                    }
                installedApps = packages
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    
    val isPortrait = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // FIX: Changed parameters to items, selected, onSelect
                        DropdownMenu(
                            modifier = Modifier.fillMaxWidth(),
                            items = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI"),
                            selected = selectedGUI,
                            onSelect = { newSelection ->
                                selectedGUI = newSelection
                                saveGUISelection(newSelection)
                            }
                        )
                        Divider()
                        // FIX: Now resolves because SettingToggle is correctly defined
                        SettingToggle(
                            title = "增强网络",
                            description = "初始化网络优化并配置套接字以提高性能",
                            checked = optimizeNetworkEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    scope.launch(Dispatchers.IO) {
                                        val success = NetworkOptimizer.init(context)
                                        if (success) {
                                            optimizeNetworkEnabled = true
                                            saveToggleState("optimizeNetworkEnabled", true)
                                            val socket = Socket()
                                            NetworkOptimizer.optimizeSocket(socket)
                                        } else {
                                            scope.launch(Dispatchers.Main) {
                                                showPermissionDialog = true
                                            }
                                        }
                                    }
                                } else {
                                    optimizeNetworkEnabled = false
                                    saveToggleState("optimizeNetworkEnabled", false)
                                }
                            }
                        )
                        Divider()
                        SettingToggle(
                            title = "高优先级线程",
                            description = "将应用程序线程设置为前台优先级以获得更好的性能",
                            checked = priorityThreadsEnabled,
                            onCheckedChange = { isEnabled ->
                                priorityThreadsEnabled = isEnabled
                                saveToggleState("priorityThreadsEnabled", isEnabled)
                                scope.launch(Dispatchers.IO) {
                                    if (isEnabled) {
                                        NetworkOptimizer.setThreadPriority()
                                    }
                                }
                            }
                        )
                        Divider()
                        SettingToggle(
                            title = "使用最快的 DNS",
                            description = "使用谷歌的DNS服务器可以更快地解析名称",
                            checked = fastDnsEnabled,
                            onCheckedChange = { isEnabled ->
                                fastDnsEnabled = isEnabled
                                saveToggleState("fastDnsEnabled", isEnabled)
                                scope.launch(Dispatchers.IO) {
                                    if (isEnabled) {
                                        NetworkOptimizer.useFastDNS()
                                    }
                                }
                            }
                        )
                        Divider()
                        SettingToggle(
                            title = "注入 Neko Pack",
                            description = "启用注入 Neko Pack 以增强功能",
                            checked = injectNekoPackEnabled,
                            onCheckedChange = { isEnabled ->
                                injectNekoPackEnabled = isEnabled
                                saveToggleState("injectNekoPackEnabled", isEnabled)
                            }
                        )
                        Divider()
                        SettingToggle(
                            title = "禁用 ConnectionInfo 覆盖",
                            description = "禁用启动服务时显示的连接信息覆盖",
                            checked = disableConnectionInfoOverlay,
                            onCheckedChange = { isEnabled ->
                                disableConnectionInfoOverlay = isEnabled
                                saveToggleState("disableConnectionInfoOverlay", isEnabled)
                            }
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "App 管理器", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "选择启动服务时将自动运行的应用程序",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val appIcon = getAppIcon(selectedAppPackage)
                                if (appIcon != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = appIcon.toBitmap(48, 48).asImageBitmap(),
                                        contentDescription = "App Icon",
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Icon(imageVector = Icons.Filled.Apps, contentDescription = null)
                                }
                                Text(
                                    text = getAppName(selectedAppPackage),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(onClick = { showAppSelectionDialog = true }) { Text("选择应用") }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        
                        // FIX: Changed parameters to items, selected, onSelect
                        DropdownMenu(
                            modifier = Modifier.fillMaxWidth(),
                            items = listOf("GraceGUI", "KitsuGUI", "ProtohaxUi", "ClickGUI"),
                            selected = selectedGUI,
                            onSelect = { newSelection ->
                                selectedGUI = newSelection
                                saveGUISelection(newSelection)
                            }
                        )

                        Divider()

                        
                        SettingToggle(
                            title = "增强网络",
                            description = "初始化网络优化并配置套接字以提高性能",
                            checked = optimizeNetworkEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    scope.launch(Dispatchers.IO) {
                                        val success = NetworkOptimizer.init(context)
                                        if (success) {
                                            optimizeNetworkEnabled = true
                                            saveToggleState("optimizeNetworkEnabled", true)
                                            val socket = Socket()
                                            NetworkOptimizer.optimizeSocket(socket)
                                        } else {
                                            
                                            scope.launch(Dispatchers.Main) {
                                                showPermissionDialog = true
                                            }
                                        }
                                    }
                                } else {
                                    optimizeNetworkEnabled = false
                                    saveToggleState("optimizeNetworkEnabled", false)
                                }
                            }
                        )

                        Divider()

                        
                        SettingToggle(
                            title = "高优先级线程",
                            description = "将应用程序线程设置为前台优先级以获得更好的性能",
                            checked = priorityThreadsEnabled,
                            onCheckedChange = { isEnabled ->
                                priorityThreadsEnabled = isEnabled
                                saveToggleState("priorityThreadsEnabled", isEnabled)
                                scope.launch(Dispatchers.IO) {
                                    if (isEnabled) {
                                        NetworkOptimizer.setThreadPriority()
                                    }
                                }
                            }
                        )

                        Divider()

                        
                        SettingToggle(
                            title = "使用最快的 DNS",
                            description = "使用谷歌的DNS服务器可以更快地解析名称",
                            checked = fastDnsEnabled,
                            onCheckedChange = { isEnabled ->
                                fastDnsEnabled = isEnabled
                                saveToggleState("fastDnsEnabled", isEnabled)
                                scope.launch(Dispatchers.IO) {
                                    if (isEnabled) {
                                        NetworkOptimizer.useFastDNS()
                                    }
                                }
                            }
                        )

                        Divider()


                        SettingToggle(
                            title = "注入 Neko Pack",
                            description = "启用注入 Neko Pack 以增强功能",
                            checked = injectNekoPackEnabled,
                            onCheckedChange = { isEnabled ->
                                injectNekoPackEnabled = isEnabled
                                saveToggleState("injectNekoPackEnabled", isEnabled)
                            }
                        )

                        Divider()


                        SettingToggle(
                            title = "禁用 ConnectionInfo 覆盖",
                            description = "禁用启动服务时显示的连接信息覆盖",
                            checked = disableConnectionInfoOverlay,
                            onCheckedChange = { isEnabled ->
                                disableConnectionInfoOverlay = isEnabled
                                saveToggleState("disableConnectionInfoOverlay", isEnabled)
                            }
                        )
                    }
                }
            }

            
            Column(
                modifier = Modifier
                    .weight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.CenterHorizontally),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "App 管理器",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "选择启动服务时将自动运行的应用程序",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val appIcon = getAppIcon(selectedAppPackage)
                                if (appIcon != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = appIcon.toBitmap(48, 48).asImageBitmap(),
                                        contentDescription = "App Icon",
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = "Default App Icon",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column {
                                    Text(
                                        text = getAppName(selectedAppPackage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "选择的版本: ${getAppVersion(selectedAppPackage)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { showAppSelectionDialog = true },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("选择")
                            }
                        }
                    }
                }

            }
        }
    }

    if (showPermissionDialog) {
        // FIX: Now resolves because PermissionDialog is correctly defined
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onRequestPermission = {
                NetworkOptimizer.openWriteSettingsPermissionPage(context)
                showPermissionDialog = false
            }
        )
    }

    if (showAppSelectionDialog) {
        // FIX: Now resolves because AppSelectionDialog is correctly defined
        AppSelectionDialog(
            installedApps = installedApps,
            selectedAppPackage = selectedAppPackage,
            onDismiss = { showAppSelectionDialog = false },
            onAppSelected = { packageName ->
                selectedAppPackage = packageName
                saveSelectedApp(packageName)
                showAppSelectionDialog = false
                Toast.makeText(context, "客户端选择: ${getAppName(packageName)}", Toast.LENGTH_SHORT).show()
            },
            getAppName = ::getAppName,
            getAppVersion = ::getAppVersion,
            getAppIcon = ::getAppIcon
        )
    }
} // <-- FIX: ADDED MISSING CLOSING BRACE FOR SettingsScreen()

@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要权限") },
        text = { Text("网络优化需要特殊权限，是否要打开设置来授予这些权限？") },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("打开设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// FIX: Renamed parameters to items, selected, onSelect
@Composable
fun DropdownMenu(
    modifier: Modifier = Modifier,
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "rotation"
    )
    
    
    val elevationState by animateFloatAsState(
        targetValue = if (expanded) 8f else 2f,
        label = "elevation"
    )

    Column(modifier = modifier) {
        Text(
            text = "界面 GUI",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = elevationState.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    // FIX: Use 'selected' parameter
                                    when (selected) {
                                        "GraceGUI" -> MaterialTheme.colorScheme.primary
                                        "KitsuGUI" -> MaterialTheme.colorScheme.tertiary
                                        "ProtohaxUi" -> MaterialTheme.colorScheme.error
                                        "ClickGUI" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                        
                        Text(
                            // FIX: Use 'selected' parameter
                            text = selected,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .rotate(rotationState)
                            .size(20.dp)
                    )
                }
            }

            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(220.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // FIX: Use 'items' parameter
                items.forEach { option ->
                    // FIX: Use 'selected' parameter
                    val isSelected = option == selected
                    
                    androidx.compose.material3.DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            when (option) {
                                                "GraceGUI" -> MaterialTheme.colorScheme.primary
                                                "KitsuGUI" -> MaterialTheme.colorScheme.tertiary
                                                "ProtohaxUi" -> MaterialTheme.colorScheme.error
                                                "ClickGUI" -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                                
                                Text(
                                    text = option,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            }
                        },
                        onClick = {
                            // FIX: Use 'onSelect' parameter
                            onSelect(option)
                            expanded = false
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .background(
                                if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                else 
                                    Color.Transparent
                            )
                    )
                }
            }
        }
    }
}


@Composable
fun AppSelectionDialog(
    installedApps: List<PackageInfo>,
    selectedAppPackage: String,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit,
    getAppName: (String) -> String,
    getAppVersion: (String) -> String,
    getAppIcon: (String) -> Drawable?
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialogScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "dialogOffsetY"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (0.4f * alpha.coerceIn(0f, 1f))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    isVisible = false
                    delay(300)
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 320.dp, max = min(screenWidth * 0.9f, 500.dp))
                .heightIn(max = screenHeight * 0.8f)
                .scale(scale)
                .alpha(alpha)
                .offset(y = offsetY.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume click to prevent propagation */ },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "选择客户端",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "选择启动服务时将自动运行的应用程序",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps) { packageInfo ->
                        val packageName = packageInfo.packageName
                        val isSelected = packageName == selectedAppPackage

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        isVisible = false
                                        delay(300)
                                        onAppSelected(packageName)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val appIcon = getAppIcon(packageName)
                                if (appIcon != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = appIcon.toBitmap(48, 48).asImageBitmap(),
                                        contentDescription = "App 图标",
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = "默认 App 图标",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = getAppName(packageName),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "v${getAppVersion(packageName)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isVisible = false
                                delay(300)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消"
                            )
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}