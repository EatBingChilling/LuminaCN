package com.project.lumina.client.router.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.util.LocalSnackbarHostState
import com.project.lumina.client.util.SnackbarHostStateScope
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomePageContent() {
    SnackbarHostStateScope {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val mainScreenViewModel: MainScreenViewModel = viewModel()
        val pages = listOf(R.string.home, R.string.about, R.string.settings)
        var currentPage by rememberSaveable { mutableStateOf(R.string.home) }

        var uiAnimationState by remember { mutableStateOf(0) }
        var uiVisible by remember { mutableStateOf(false) }

        var showProgressDialog by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableStateOf(0f) }
        var currentPackName by remember { mutableStateOf("") }

        val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        var InjectNekoPack by remember {
            mutableStateOf(sharedPreferences.getBoolean("injectNekoPackEnabled", false))
        }
        var isConnected by remember { mutableStateOf(false) }

        val blurRadius by animateFloatAsState(
            targetValue = if (uiAnimationState > 0) 0f else 15f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing),
            label = "blurAnimation"
        )

        val logoScale by animateFloatAsState(
            targetValue = if (uiAnimationState > 1) 1f else 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "logoScale"
        )

        val contentAlpha by animateFloatAsState(
            targetValue = if (uiAnimationState > 2) 1f else 0f,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "contentAlpha"
        )

        val parallaxOffset by animateFloatAsState(
            targetValue = if (uiAnimationState > 0) 0f else -30f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
            label = "parallaxOffset"
        )

        LaunchedEffect(Unit) {
            uiAnimationState = 0
            uiVisible = false
            delay(100)
            uiAnimationState = 1
            delay(300)
            uiAnimationState = 2
            delay(200)
            uiAnimationState = 3
            uiVisible = true
            delay(200)
            uiAnimationState = 4
        }

        val showNotification: (String, NotificationType) -> Unit = { message, type ->
            SimpleOverlayNotification.show(
                message = message,
                type = type,
                durationMs = 3000
            )
        }

        val onPostPermissionResult: (Boolean) -> Unit = { isGranted ->
            if (!isGranted) {
                showNotification(
                    context.getString(R.string.notification_permission_denied),
                    NotificationType.ERROR
                )
                return@let
            }

            if (mainScreenViewModel.selectedGame.value == null) {
                showNotification(
                    context.getString(R.string.select_game_first),
                    NotificationType.WARNING
                )
                return@let
            }

            val captureModeModel = mainScreenViewModel.captureModeModel.value
            Services.toggle(context, captureModeModel)
        }

        val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted -> onPostPermissionResult(isGranted) }

        val overlayPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!Settings.canDrawOverlays(context)) {
                showNotification(
                    context.getString(R.string.overlay_permission_denied),
                    NotificationType.ERROR
                )
                return@rememberLauncherForActivityResult
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@rememberLauncherForActivityResult
            }
            onPostPermissionResult(true)
        }

        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(500)) +
                            slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = tween(500)
                            ),
                    modifier = Modifier.graphicsLayer {
                        translationY = parallaxOffset * 0.3f
                    }
                ) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Lumina",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.graphicsLayer {
                                        val pulseFactor = 1f + (sin(System.currentTimeMillis() / 3000f) * 0.05f)
                                        scaleX = pulseFactor * logoScale
                                        scaleY = pulseFactor * logoScale
                                    }
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    pages.forEachIndexed { index, page ->
                                        AnimatedVisibility(
                                            visible = uiAnimationState >= 3,
                                            enter = fadeIn(animationSpec = tween(300, delayMillis = 100 * index)) +
                                                    slideInHorizontally(
                                                        initialOffsetX = { it / 4 },
                                                        animationSpec = tween(300)
                                                    ),
                                        ) {
                                            val selected = currentPage == page
                                            Surface(
                                                selected = selected,
                                                onClick = { currentPage = page },
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) else Color.Transparent,
                                                modifier = Modifier.animateContentSize()
                                            ) {
                                                Text(
                                                    text = stringResource(page),
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            ),
                                            startX = 0f + (System.currentTimeMillis() % 4000) / 4000f * 1000f,
                                            endX = 1000f + (System.currentTimeMillis() % 4000) / 4000f * 1000f
                                        )
                                    )
                            )
                        }
                    }
                }
            },
            bottomBar = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier
                        .animateContentSize()
                        .padding(horizontal = if (isLandscape) 200.dp else 16.dp, bottom = 16.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = uiAnimationState >= 3,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400)
                            ),
                    modifier = Modifier.graphicsLayer {
                        translationY = parallaxOffset * 0.5f
                        alpha = contentAlpha
                    }
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                AnimatedContent(
                                    targetState = currentPage,
                                    transitionSpec = {
                                        val direction = if (targetState > initialState) 1 else -1
                                        if (direction > 0) {
                                            slideInHorizontally { it } + fadeIn() togetherWith
                                                    slideOutHorizontally { -it } + fadeOut()
                                        } else {
                                            slideInHorizontally { -it } + fadeIn() togetherWith
                                                    slideOutHorizontally { it } + fadeOut()
                                        }
                                    }
                                ) { page ->
                                    when (page) {
                                        R.string.home -> HomeScreen(onStartToggle = {
                                            if (!Settings.canDrawOverlays(context)) {
                                                Toast.makeText(context, context.getString(R.string.request_overlay_permission), Toast.LENGTH_SHORT).show()
                                                overlayPermissionLauncher.launch(
                                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                                )
                                                return@HomeScreen
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                return@HomeScreen
                                            }

                                            onPostPermissionResult(true)
                                        })
                                        R.string.about -> AboutScreen()
                                        R.string.settings -> SettingsScreen()
                                        else -> {}
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            AnimatedContent(
                                targetState = currentPage,
                                transitionSpec = {
                                    val direction = if (targetState > initialState) 1 else -1
                                    if (direction > 0) {
                                        slideInHorizontally { it } + fadeIn() togetherWith
                                                slideOutHorizontally { -it } + fadeOut()
                                    } else {
                                        slideInHorizontally { -it } + fadeIn() togetherWith
                                                slideOutHorizontally { it } + fadeOut()
                                    }
                                }
                            ) { page ->
                                when (page) {
                                    R.string.home -> HomeScreen(onStartToggle = {
                                        if (!Settings.canDrawOverlays(context)) {
                                            Toast.makeText(context, context.getString(R.string.request_overlay_permission), Toast.LENGTH_SHORT).show()
                                            overlayPermissionLauncher.launch(
                                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                            )
                                            return@HomeScreen
                                        }

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@HomeScreen
                                        }

                                        onPostPermissionResult(true)
                                    })
                                    R.string.about -> AboutScreen()
                                    R.string.settings -> SettingsScreen()
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                if (showProgressDialog) {
                    Dialog(onDismissRequest = { }) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 6.dp,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "下载中: $currentPackName",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (downloadProgress < 1f) "下载中..." else "启动 Minecraft...",
                                    style = MaterialTheme.typography.bodyMedium,
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
