/**
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * Material Design 3 Expressive 风格适配
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.util.*
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.Services
import com.project.lumina.client.ui.component.ServerSelector
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartToggle: () -> Unit) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var selectedView by remember { mutableStateOf("ServerSelector") }
    var previousView by remember { mutableStateOf("ServerSelector") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }

    val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    var InjectNekoPack by remember {
        mutableStateOf(sharedPreferences.getBoolean("injectNekoPackEnabled", false))
    }

    val isCompactScreen = configuration.screenWidthDp.dp < 600.dp

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "injectNekoPackEnabled") {
                InjectNekoPack = prefs.getBoolean("injectNekoPackEnabled", false)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        if (isLandscape) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Panel
                Column(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    TabSelector(
                        selectedView = selectedView,
                        onViewSelected = { newView ->
                            previousView = selectedView
                            selectedView = newView
                        },
                        isLandscape = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TabContent(
                        selectedView = selectedView,
                        previousView = previousView,
                        onStartToggle = onStartToggle,
                        isLandscape = true
                    )
                }

                // Right Panel
                Column(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    AccountAndServerPanel(
                        context = context,
                        captureModeModel = captureModeModel,
                        isCompactScreen = isCompactScreen
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TabSelector(
                    selectedView = selectedView,
                    onViewSelected = { newView ->
                        previousView = selectedView
                        selectedView = newView
                    },
                    isLandscape = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                TabContent(
                    selectedView = selectedView,
                    previousView = previousView,
                    onStartToggle = onStartToggle,
                    isLandscape = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                AccountAndServerPanel(
                    context = context,
                    captureModeModel = captureModeModel,
                    isCompactScreen = isCompactScreen
                )
            }
        }
    }
}

@Composable
private fun TabSelector(
    selectedView: String,
    onViewSelected: (String) -> Unit,
    isLandscape: Boolean
) {
    val tabs = listOf("ServerSelector", "View2", "View3")
    val tabNames = listOf(R.string.servers, R.string.accounts, R.string.packs)

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedView == tab
                
                Surface(
                    selected = isSelected,
                    onClick = { onViewSelected(tab) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(tabNames[index]),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedView == tab
                
                Surface(
                    selected = isSelected,
                    onClick = { onViewSelected(tab) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(tabNames[index]),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedView: String,
    previousView: String,
    onStartToggle: () -> Unit,
    isLandscape: Boolean
) {
    val currentTabIndex = when (selectedView) {
        "ServerSelector" -> 0
        "View2" -> 1
        "View3" -> 2
        else -> 0
    }

    val previousTabIndex = when (previousView) {
        "ServerSelector" -> 0
        "View2" -> 1
        "View3" -> 2
        else -> 0
    }

    val enterTransition = if (currentTabIndex > previousTabIndex) {
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    } else {
        slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
    }

    val exitTransition = if (currentTabIndex > previousTabIndex) {
        slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    } else {
        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    }

    AnimatedContent(
        targetState = selectedView,
        transitionSpec = {
            enterTransition togetherWith exitTransition using SizeTransform(clip = false)
        }
    ) { targetView ->
        when (targetView) {
            "ServerSelector" -> ServerSelector()
            "View2" -> AccountScreen { m, t -> SimpleOverlayNotification.show(m, t, 3000) }
            "View3" -> PacksScreen()
        }
    }
}

@Composable
private fun AccountAndServerPanel(
    context: Context,
    captureModeModel: com.project.lumina.client.model.CaptureModeModel,
    isCompactScreen: Boolean
) {
    val showNotification: (String, NotificationType) -> Unit = { message, type ->
        SimpleOverlayNotification.show(
            message = message,
            type = type,
            durationMs = 3000
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AnimatedContent(
            targetState = AccountManager.currentAccount?.remark,
            transitionSpec = {
                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> height } + fadeOut())
            }
        ) { accountRemark ->
            if (accountRemark != null) {
                AccountCard(
                    accountName = accountRemark,
                    isCompactScreen = isCompactScreen
                )
            } else if (AccountManager.accounts.isNotEmpty()) {
                AccountSelectionCard(
                    isCompactScreen = isCompactScreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ServerInfoCard(
            serverName = captureModeModel.serverHostName,
            serverPort = captureModeModel.serverPort,
            isCompactScreen = isCompactScreen
        )
    }
}

@Composable
private fun AccountCard(
    accountName: String,
    isCompactScreen: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(if (isCompactScreen) 0.2f else 0.25f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(if (isCompactScreen) 48.dp else 56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(if (isCompactScreen) 8.dp else 12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        "当前账户",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        accountName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSelectionCard(
    isCompactScreen: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(if (isCompactScreen) 0.35f else 0.4f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "选择一个账号",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AccountManager.accounts) { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (account == AccountManager.currentAccount) {
                                    AccountManager.selectAccount(null)
                                } else {
                                    AccountManager.selectAccount(account)
                                }
                            },
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = account.remark,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerInfoCard(
    serverName: String,
    serverPort: Int,
    isCompactScreen: Boolean
) {
    AnimatedVisibility(
        visible = serverName.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isCompactScreen) 80.dp else 100.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(if (isCompactScreen) 16.dp else 20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.selected_server),
                        style = if (isCompactScreen)
                            MaterialTheme.typography.bodyLarge else
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = serverName,
                    style = if (isCompactScreen)
                        MaterialTheme.typography.bodyLarge else
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.port, serverPort),
                    style = if (isCompactScreen)
                        MaterialTheme.typography.bodySmall else
                        MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
