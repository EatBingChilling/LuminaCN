package com.project.lumina.client.router.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.project.lumina.client.R
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.viewmodel.MainScreenViewModel

@Immutable
data class NavigationItem(
    val route: String,
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
    val content: @Composable () -> Unit
)

val navigationItems = listOf(
    NavigationItem(
        route = "home",
        icon = { Icon(Icons.TwoTone.Home, contentDescription = null) },
        label = { Text(stringResource(R.string.home)) },
        content = { NewHomePageContent() }
    ),
    NavigationItem(
        route = "account",
        icon = { Icon(Icons.TwoTone.AccountCircle, contentDescription = null) },
        label = { Text("账户") },
        content = { AccountScreenContent() }
    ),
    NavigationItem(
        route = "about",
        icon = { Icon(Icons.TwoTone.Info, contentDescription = null) },
        label = { Text("关于") },
        content = { AboutScreenContent() }
    ),
    NavigationItem(
        route = "settings",
        icon = { Icon(Icons.TwoTone.Settings, contentDescription = null) },
        label = { Text("设置") },
        content = { SettingsScreenContent() }
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    
    // Determine if we should use NavigationRail (landscape/wide screen) or NavigationBar (portrait)
    val useNavigationRail = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    if (useNavigationRail) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // NavigationRail for landscape
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                navigationItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = item.icon,
                        label = item.label,
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
            
            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                navigationItems[selectedTabIndex].content()
                
                Box(modifier = Modifier.zIndex(10f)) {
                    SimpleOverlayNotification.Content()
                }
            }
        }
    } else {
        // Portrait layout with NavigationBar at bottom
        Scaffold(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp
                ) {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = item.icon,
                            label = item.label,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                navigationItems[selectedTabIndex].content()
                
                Box(modifier = Modifier.zIndex(10f)) {
                    SimpleOverlayNotification.Content()
                }
            }
        }
    }
}

@Composable
fun NewHomePageContent() {
    val context = LocalContext.current
    val vm: MainScreenViewModel = viewModel()
    val captureModeModel by vm.captureModeModel.collectAsState()

    NewHomeScreen(
        onStartToggle = {
            com.project.lumina.client.service.Services.toggle(context, captureModeModel)
        }
    )
}

@Composable
fun AccountScreenContent() {
    AccountScreen { message, type ->
        SimpleOverlayNotification.show(message, type, 3000)
    }
}

@Composable
fun AboutScreenContent() {
    AboutScreen()
}

@Composable
fun SettingsScreenContent() {
    SettingsScreen()
}