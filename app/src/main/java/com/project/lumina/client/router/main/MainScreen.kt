package com.project.lumina.client.router.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.viewmodel.MainScreenViewModel

@Immutable
enum class MainScreenPages(
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
    val content: @Composable () -> Unit
) {
    HomePage(
        icon = { Icon(Icons.TwoTone.Home, contentDescription = null) },
        label = { Text(stringResource(R.string.home)) },
        content = { NewHomePageContent() }
    )
}

@Composable
fun MainScreen() {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {

        NewHomePageContent()

        // 【已修复】下面这个Box和其中的Content()调用已被删除。
        // 灵动岛的UI由后台服务管理，不再需要在这里手动渲染。
        /*
        Box(modifier = Modifier.zIndex(10f)) {
            SimpleOverlayNotification.Content()
        }
        */
    }
}

@Composable
fun NewHomePageContent() {
    val context = LocalContext.current
    val vm: MainScreenViewModel = viewModel()
    val captureModeModel = vm.captureModeModel.value

    NewHomeScreen(
        onStartToggle = {
            com.project.lumina.client.service.Services.toggle(context, captureModeModel)
        }
    )
}