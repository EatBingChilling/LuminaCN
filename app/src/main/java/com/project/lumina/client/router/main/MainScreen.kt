package com.project.lumina.client.router.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        icon = { 
            Icon(
                Icons.TwoTone.Home, 
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            ) 
        },
        label = { 
            Text(
                stringResource(R.string.home),
                style = MaterialTheme.typography.labelLarge
            ) 
        },
        content = { NewHomePageContent() }
    )
}

@Composable
fun MainScreen() {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        NewHomePageContent()
        
        Box(modifier = Modifier.zIndex(10f)) {
            SimpleOverlayNotification.Content()
        }
    }
}

@Composable
fun NewHomePageContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: com.project.lumina.client.viewmodel.MainScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val captureModeModel = vm.captureModeModel.value

    NewHomeScreen(
        onStartToggle = {
            com.project.lumina.client.service.Services.toggle(context, captureModeModel)
        }
    )
}
