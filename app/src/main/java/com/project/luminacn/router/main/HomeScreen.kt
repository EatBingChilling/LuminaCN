/**
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.luminacn.router.main

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.luminacn.R
import com.project.luminacn.constructors.AccountManager
import com.project.luminacn.util.InjectNeko
import com.project.luminacn.util.MCPackUtils
import com.project.luminacn.util.ServerInit
import com.project.luminacn.overlay.mods.NotificationType
import com.project.luminacn.overlay.mods.SimpleOverlayNotification
import com.project.luminacn.service.Services
import com.project.luminacn.ui.component.ServerSelector
import com.project.luminacn.viewmodel.MainScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberModalBottomSheetState
import com.project.luminacn.overlay.manager.ConnectionInfoOverlay
import com.project.luminacn.ui.component.SubServerInfo

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartToggle: () -> Unit
) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()
    var selectedView by remember { mutableStateOf("ServerSelector") }
    var previousView by remember { mutableStateOf("ServerSelector") }

    var showCustomNotification by remember { mutableStateOf(false) }
    var customNotificationMessage by remember { mutableStateOf("") }
    var customNotificationType by remember { mutableStateOf<NotificationType>(NotificationType.INFO) }
    var lastCustomNotificationTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }

    var isLaunchingMinecraft by remember { mutableStateOf(false) }

    LaunchedEffect(Services.isActive) {
        if (Services.isActive) {
            delay(600)
            showBottomSheet = false
        } else {
            showBottomSheet = false
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var currentPackName by remember { mutableStateOf("") }

    var showZeqaBottomSheet by remember { mutableStateOf(false) }

    val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    var InjectNekoPack by remember {
        mutableStateOf(sharedPreferences.getBoolean("injectNekoPackEnabled", false))
    }

    val isCompactScreen = screenWidth < 600.dp

    val leftColumnWidth = if (isCompactScreen) 0.4f else 0.5f
    val localIp = remember { ConnectionInfoOverlay.getLocalIpAddress(context) }
    val showNotification: (String, NotificationType) -> Unit = { message, type ->
        SimpleOverlayNotification.show(
            message = message,
            type = type,
            durationMs = 3000
        )
    }

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

    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(leftColumnWidth)
                .padding(
                    start = if (isCompactScreen) 12.dp else 24.dp,
                    end = if (isCompactScreen) 8.dp else 24.dp,
                    top = if (isCompactScreen) 16.dp else 24.dp,
                    bottom = if (isCompactScreen) 16.dp else 24.dp
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            if (isCompactScreen) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val tabs = listOf("ServerSelector", "View2", "View3")
                    val tabNames = listOf(R.string.servers, R.string.accounts, R.string.packs)

                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedView == tab
                        val interactionSource = remember { MutableInteractionSource() }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                previousView = selectedView
                                selectedView = tab
                            },
                            label = {
                                Text(
                                    stringResource(tabNames[index]),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            interactionSource = interactionSource
                        )
                    }
                }
            } else {

                Row(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val tabs = listOf("ServerSelector", "View2", "View3")
                    val tabNames = listOf(R.string.servers, R.string.accounts, R.string.packs)

                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedView == tab
                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                        ) {
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    previousView = selectedView
                                    selectedView = tab
                                },
                                label = {
                                    Text(
                                        stringResource(tabNames[index]),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                interactionSource = interactionSource
                            )
                        }
                    }
                }
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                    },
                    label = "tabContentAnimation"
                ) { targetView ->
                    when (targetView) {
                        "ServerSelector" -> ServerSelector(
                            onShowZeqaBottomSheet = { showZeqaBottomSheet = true }
                        )
                        "View2" -> AccountScreen(showNotification)
                        "View3" -> PacksScreen()
                    }
                }
            }
        }


        if (!isCompactScreen) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight(0.97f)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }


        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(
                    start = if (isCompactScreen) 8.dp else 16.dp,
                    end = if (isCompactScreen) 12.dp else 16.dp,
                    top = if (isCompactScreen) 16.dp else 16.dp,
                    bottom = if (isCompactScreen) 16.dp else 16.dp
                )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    AnimatedContent(
                        targetState = AccountManager.currentAccount?.remark,
                        transitionSpec = {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                        },
                        label = "accountAnimation"
                    ) { accountRemark ->
                        if (accountRemark != null) {

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(if (isCompactScreen) 0.2f else 0.25f)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 400f
                                        )
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 2.dp,
                                shadowElevation = 1.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {

                                    if (isCompactScreen) {

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {

                                            Surface(
                                                modifier = Modifier.size(40.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AccountCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .padding(2.dp)
                                                        .size(36.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }


                                            Text(
                                                text = "你好!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.7f
                                                )
                                            )

                                            Text(
                                                text = accountRemark,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {

                                            Surface(
                                                modifier = Modifier.size(56.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AccountCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .padding(4.dp)
                                                        .size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }


                                            Row(
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "你好!",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.7f
                                                    )
                                                )

                                                Text(
                                                    modifier = Modifier.padding(start = 4.dp),
                                                    text = accountRemark,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Thin,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (AccountManager.accounts.isNotEmpty()) {

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(if (isCompactScreen) 0.35f else 0.4f)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 400f
                                        )
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (isCompactScreen) 12.dp else 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(if (isCompactScreen) 18.dp else 24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "选择一个账号",
                                            style = if (isCompactScreen)
                                                MaterialTheme.typography.bodyLarge else
                                                MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Divider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.5f
                                        )
                                    )


                                    LazyColumn(
                                        modifier = Modifier.heightIn(
                                            max = min(screenHeight.times(0.3f), 200.dp)
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 6.dp else 8.dp)
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
                                                        .padding(
                                                            horizontal = if (isCompactScreen) 8.dp else 12.dp,
                                                            vertical = if (isCompactScreen) 8.dp else 10.dp
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AccountCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(if (isCompactScreen) 16.dp else 20.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = account.remark,
                                                        style = if (isCompactScreen)
                                                            MaterialTheme.typography.bodyMedium else
                                                            MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(if (isCompactScreen) 8.dp else 16.dp))


                    AnimatedVisibility(
                        visible = showCustomNotification,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .width(if (isCompactScreen) 240.dp else 320.dp)
                                .wrapContentHeight(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = when (customNotificationType) {
                                    NotificationType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                                    NotificationType.ERROR -> MaterialTheme.colorScheme.errorContainer
                                    NotificationType.INFO -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    NotificationType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = if (isCompactScreen) 12.dp else 16.dp,
                                        vertical = if (isCompactScreen) 8.dp else 12.dp
                                    ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = customNotificationMessage,
                                    style = if (isCompactScreen)
                                        MaterialTheme.typography.bodySmall else
                                        MaterialTheme.typography.bodyMedium,
                                    color = when (customNotificationType) {
                                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                                        NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                                        NotificationType.INFO -> MaterialTheme.colorScheme.onSurface
                                        NotificationType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                    }
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(if (isCompactScreen) 8.dp else 16.dp))
                    }


                    AnimatedVisibility(
                        visible = captureModeModel.serverHostName.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = if (isCompactScreen) 70.dp else 90.dp)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(if (isCompactScreen) 12.dp else 16.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 4.dp else 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 4.dp else 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isCompactScreen) 16.dp else 20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.selected_server),
                                        style = if (isCompactScreen)
                                            MaterialTheme.typography.bodyLarge else
                                            MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = captureModeModel.serverHostName,
                                    style = if (isCompactScreen)
                                        MaterialTheme.typography.bodyLarge else
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.port, captureModeModel.serverPort),
                                    style = if (isCompactScreen)
                                        MaterialTheme.typography.bodySmall else
                                        MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.weight(1f))


                Box(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = if (isCompactScreen) 12.dp else 20.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AnimatedContent(
                        targetState = Services.isActive,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        },
                        label = "buttonLayoutAnimation"
                    ) { isActive ->

                        val scaleAnimation by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0.95f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "buttonScaleAnimation"
                        )

                        if (isActive) {
                            Button(
                                onClick = {
                                    isLaunchingMinecraft = false
                                    onStartToggle()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isCompactScreen) 48.dp else 56.dp),
                                shape = RoundedCornerShape(if (isCompactScreen) 12.dp else 16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Pause,
                                        contentDescription = "停止",
                                        modifier = Modifier.size(if (isCompactScreen) 20.dp else 24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.stop),
                                        style = if (isCompactScreen)
                                            MaterialTheme.typography.bodyLarge else
                                            MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (isCompactScreen) 8.dp else 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ExtendedFloatingActionButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isCompactScreen) 48.dp else 56.dp)
                                        .scale(scaleAnimation)
                                        .animateContentSize(),
                                    onClick = {
                                        scope.launch {
                                            delay(100)
                                            isLaunchingMinecraft = true
                                            Services.isLaunchingMinecraft = true
                                            onStartToggle()
                                            
                                            delay(2500)
                                            if (!Services.isActive) {
                                                isLaunchingMinecraft = false
                                                Services.isLaunchingMinecraft = false
                                                return@launch
                                            }
                                            
                                            val selectedGame = mainScreenViewModel.selectedGame.value
                                            if (selectedGame != null) {
                                                val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                                                if (intent != null && Services.isActive) {
                                                    context.startActivity(intent)
                                                    
                                                    delay(3000)
                                                    if (Services.isActive) {
                                                        val disableConnectionInfoOverlay = sharedPreferences.getBoolean("disableConnectionInfoOverlay", false)
                                                        if (!disableConnectionInfoOverlay) {
                                                            ConnectionInfoOverlay.show(localIp)
                                                        }
                                                    }
                                                    isLaunchingMinecraft = false
                                                    Services.isLaunchingMinecraft = false
                                                    
                                                    try {
                                                        when {
                                                            InjectNekoPack == true && PackSelectionManager.selectedPack != null -> {
                                                                PackSelectionManager.selectedPack?.let { selectedPack ->
                                                                    currentPackName = selectedPack.name
                                                                    showProgressDialog = true
                                                                    downloadProgress = 0f

                                                                    try {
                                                                        MCPackUtils.downloadAndOpenPack(
                                                                            context,
                                                                            selectedPack
                                                                        ) { progress ->
                                                                            downloadProgress = progress
                                                                        }
                                                                        showProgressDialog = false
                                                                    } catch (e: Exception) {
                                                                        showProgressDialog = false
                                                                        showNotification(
                                                                            "材质包下载失败: ${e.message}",
                                                                            NotificationType.ERROR
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            InjectNekoPack == true -> {
                                                                try {
                                                                    InjectNeko.injectNeko(
                                                                        context = context,
                                                                        onProgress = {
                                                                            progress = it
                                                                        }
                                                                    )
                                                                } catch (e: Exception) {
                                                                    showNotification(
                                                                        "Neko 注入失败: ${e.message}",
                                                                        NotificationType.ERROR
                                                                    )
                                                                }
                                                            }

                                                            else -> {
                                                                if (selectedGame == "com.mojang.minecraftpe") {
                                                                    try {
                                                                        ServerInit.addMinecraftServer(
                                                                            context,
                                                                            localIp
                                                                        )
                                                                    } catch (e: Exception) {
                                                                        showNotification(
                                                                            "服务器初始化失败: ${e.message}",
                                                                            NotificationType.ERROR
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        showNotification(
                                                            "一个未预料的错误发生: ${e.message}",
                                                            NotificationType.ERROR
                                                        )
                                                    }
                                                } else {
                                                    showNotification(
                                                        "游戏启动失败，请检查是否安装 Minecraft 或在 App 管理器中正确添加了客户端",
                                                        NotificationType.ERROR
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(if (isCompactScreen) 12.dp else 16.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(if (isCompactScreen) 20.dp else 24.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = stringResource(R.string.start),
                                            style = if (isCompactScreen)
                                                MaterialTheme.typography.bodyLarge else
                                                MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )

                                if (showProgressDialog) {
                                    Dialog(onDismissRequest = { /* Prevent dismissal during download */ }) {
                                        Card(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .wrapContentSize()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "正在下载: $currentPackName",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                CircularProgressIndicator(
                                                    progress = { downloadProgress },
                                                    modifier = Modifier.size(48.dp),
                                                    trackColor = ProgressIndicatorDefaults.circularTrackColor,
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "${(downloadProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (downloadProgress < 1f) "正在下载..." else "正在启动 Minecraft ...",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showZeqaBottomSheet) {
        ZeqaSubServerBottomSheet(
            onDismiss = { showZeqaBottomSheet = false },
            onSelect = { subServer ->
                mainScreenViewModel.selectCaptureModeModel(
                    captureModeModel.copy(serverHostName = subServer.serverAddress, serverPort = subServer.serverPort)
                )
                showZeqaBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeqaSubServerBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (SubServerInfo) -> Unit
) {
    val subServers = listOf(
        SubServerInfo("AS1", "亚洲", "104.234.6.50", 10001),
        SubServerInfo("AS2", "亚洲", "104.234.6.50", 10002),
        SubServerInfo("AS3", "亚洲", "104.234.6.50", 10003),
        SubServerInfo("AS4", "亚洲", "104.234.6.50", 10004),
        SubServerInfo("AS5", "亚洲", "104.234.6.50", 10005),
        SubServerInfo("EU1", "欧洲", "178.32.145.167", 10001),
        SubServerInfo("EU2", "欧洲", "178.32.145.167", 10002),
        SubServerInfo("EU3", "欧洲", "178.32.145.167", 10003),
        SubServerInfo("EU4", "欧洲", "178.32.145.167", 10004),
        SubServerInfo("EU5", "欧洲", "178.32.145.167", 10005),
        SubServerInfo("NA1", "北美", "51.79.62.8", 10001),
        SubServerInfo("NA2", "北美", "51.79.62.8", 10002),
        SubServerInfo("NA3", "北美", "51.79.62.8", 10003),
        SubServerInfo("NA4", "北美", "51.79.62.8", 10004),
        SubServerInfo("NA5", "北美", "51.79.62.8", 10005),
        SubServerInfo("SA1", "南非", "38.54.63.126", 10001),
        SubServerInfo("SA2", "南非", "38.54.63.126", 10002),
        SubServerInfo("SA3", "南非", "38.54.63.126", 10003),
        SubServerInfo("SA4", "南非", "38.54.63.126", 10004),
        SubServerInfo("SA5", "南非", "38.54.63.126", 10005)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "选择 Zeqa 分服",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onDismiss) {
                    Text(
                        "取消",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "基于你的地理位置选择一个分服",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subServers) { subServer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onSelect(subServer)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = subServer.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subServer.region,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = subServer.serverAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "端口: ${subServer.serverPort}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



