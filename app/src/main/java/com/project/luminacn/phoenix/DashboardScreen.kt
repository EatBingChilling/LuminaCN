package com.project.luminacn.phoenix

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.luminacn.constructors.Account
import com.project.luminacn.constructors.AccountManager
import com.project.luminacn.overlay.manager.ConnectionInfoOverlay
import com.project.luminacn.overlay.mods.NotificationType
import com.project.luminacn.overlay.mods.SimpleOverlayNotification
import com.project.luminacn.pack.PackSelectionManager
import com.project.luminacn.service.Services
import com.project.luminacn.util.InjectNeko
import com.project.luminacn.util.MCPackUtils
import com.project.luminacn.util.ServerInit
import com.project.luminacn.viewmodel.MainScreenViewModel
import kotlinx.coroutines.*

@Composable
internal fun DashboardScreen(viewModel: MainScreenViewModel) {
    val context = LocalContext.current
    var isLaunching by remember { mutableStateOf(false) }

    val serverModel by viewModel.captureModeModel.collectAsStateWithLifecycle()
    val accounts by AccountManager.accounts.collectAsStateWithLifecycle()
    val currentAccount = AccountManager.currentAccount
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 启动/停止按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (Services.isActive) {
                Button(onClick = { Services.stop() }) { Text("停止服务") }
            } else {
                Button(
                    enabled = !isLaunching,
                    onClick = {
                        Services.start()
                        if (!isLaunching) {
                            isLaunching = true
                            launchMinecraft(context, viewModel) { isLaunching = false }
                        }
                    }
                ) { Text("启动服务 & 进入游戏") }
            }
        }

        // 2. 服务器信息
        if (serverModel.serverHostName.isNotBlank()) {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("服务器: ${serverModel.serverHostName}")
                    Text("端口: ${serverModel.serverPort}")
                }
            }
        }

        // 3. 当前账号 or 列表
        if (currentAccount != null) {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("当前账号: ${currentAccount.remark}")
                    Button(onClick = { AccountManager.selectAccount(null) }) {
                        Text("切换账号")
                    }
                }
            }
        } else if (accounts.isNotEmpty()) {
            Card {
                LazyColumn(Modifier.padding(8.dp)) {
                    items(accounts) { account ->
                        TextButton(onClick = { AccountManager.selectAccount(account) }) {
                            Text(account.remark)
                        }
                    }
                }
            }
        } else {
            Text("暂无账号")
        }
    }
}

private fun launchMinecraft(
    context: Context,
    viewModel: MainScreenViewModel,
    onFinish: () -> Unit
) {
    val prefs = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    val injectNekoPack = prefs.getBoolean("injectNekoPackEnabled", false)

    CoroutineScope(Dispatchers.IO).launch {
        delay(2500)
        if (!Services.isActive) {
            onFinish()
            return@launch
        }

        val pkg = viewModel.selectedGame.value
        if (pkg != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                withContext(Dispatchers.Main) { context.startActivity(intent) }

                delay(3000)
                if (Services.isActive) {
                    if (!prefs.getBoolean("disableConnectionInfoOverlay", false)) {
                        val ip = ConnectionInfoOverlay.getLocalIpAddress(context)
                        ConnectionInfoOverlay.show(ip)
                    }
                }

                try {
                    when {
                        injectNekoPack && PackSelectionManager.selectedPack != null -> {
                            val pack = PackSelectionManager.selectedPack!!
                            val progress = MCPackUtils.downloadAndOpenPack(context, pack)
                            // 如需弹进度，可在此回调
                        }
                        injectNekoPack -> InjectNeko.injectNeko(context) {}
                        pkg == "com.mojang.minecraftpe" -> {
                            val ip = ConnectionInfoOverlay.getLocalIpAddress(context)
                            ServerInit.addMinecraftServer(context, ip)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        SimpleOverlayNotification.show("错误: ${e.message}", NotificationType.ERROR, 5000)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    SimpleOverlayNotification.show("游戏启动失败", NotificationType.ERROR, 5000)
                }
            }
        }
        onFinish()
    }
}
