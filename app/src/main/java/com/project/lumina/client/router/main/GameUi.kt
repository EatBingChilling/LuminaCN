/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * Material Design 3 Expressive 风格适配
 */

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.service.Services
import com.project.lumina.client.ui.component.ServerSelector
import com.project.lumina.client.viewmodel.MainScreenViewModel
import io.lumina.luminaux.components.*
import kotlinx.coroutines.delay
import kotlin.text.toIntOrNull
import com.project.lumina.client.overlay.mods.NotificationType

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameUI() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var uiVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var interactionTime by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsStateWithLifecycle()
    
    val backgroundBlurRadius by animateFloatAsState(
        targetValue = if (uiVisible) 2f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "backgroundBlur"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "expressiveEffects")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rippleScale"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    var serverHostName by remember { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by remember { mutableStateOf(captureModeModel.serverPort.toString()) }

    val onPostPermissionResult: (Boolean) -> Unit = { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "请授权权限", Toast.LENGTH_SHORT).show()
            return@let
        }

        if (mainScreenViewModel.selectedGame.value == null) {
            Toast.makeText(context, "请选择一个游戏", Toast.LENGTH_SHORT).show()
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
            return@rememberLauncherForActivityResult
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@rememberLauncherForActivityResult
        }
        onPostPermissionResult(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    interactionTime = System.currentTimeMillis()
                }
            }
    ) {
        // Background with Expressive effects
        Box(modifier = Modifier.fillMaxSize()) {
            VideoBackground()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.05f)
                    .scale(rippleScale)
                    .blur(60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Panel - Server Config
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                            slideInHorizontally(animationSpec = tween(600, delayMillis = 200)),
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    GlassmorphicCard2(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                "服务器配置",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            
                            GlassmorphicOutlinedTextField(
                                value = serverHostName,
                                onValueChange = {
                                    serverHostName = it
                                    if (it.isNotEmpty()) {
                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverHostName = it)
                                        )
                                    }
                                },
                                label = "服务器地址",
                                placeholder = "例如 play.example.net",
                                singleLine = true,
                                enabled = !Services.isActive,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            GlassmorphicOutlinedTextField(
                                value = serverPort,
                                onValueChange = {
                                    serverPort = it
                                    val port = it.toIntOrNull()
                                    if (port != null && port in 0..65535) {
                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverPort = port)
                                        )
                                    }
                                },
                                label = "服务器端口",
                                placeholder = "例如 19132",
                                singleLine = true,
                                enabled = !Services.isActive,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Center Panel - Server Selector
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 400)),
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .weight(1f)
                ) {
                    GlassmorphicCard2(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ServerSelector()
                    }
                }

                // Right Panel - Navigation
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) +
                            slideInHorizontally(animationSpec = tween(600, delayMillis = 600)),
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .weight(0.5f)
                        .padding(start = 16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        VerticalNavButtons()
                        
                        GlassmorphicFloatingNavBar(
                            selectedIndex = 0,
                            onItemSelected = { }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Server Selector
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                            slideInVertically(animationSpec = tween(600, delayMillis = 200)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                ) {
                    GlassmorphicCard2(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ServerSelector()
                    }
                }

                // Middle Config
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 400)) +
                            slideInVertically(animationSpec = tween(600, delayMillis = 400)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.3f)
                ) {
                    GlassmorphicCard2(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GlassmorphicOutlinedTextField(
                                value = serverHostName,
                                onValueChange = {
                                    serverHostName = it
                                    if (it.isNotEmpty()) {
                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverHostName = it)
                                        )
                                    }
                                },
                                label = "服务器地址",
                                placeholder = "例如 play.example.net",
                                singleLine = true,
                                enabled = !Services.isActive,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            GlassmorphicOutlinedTextField(
                                value = serverPort,
                                onValueChange = {
                                    serverPort = it
                                    val port = it.toIntOrNull()
                                    if (port != null && port in 0..65535) {
                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverPort = port)
                                        )
                                    }
                                },
                                label = "服务器端口",
                                placeholder = "例如 19132",
                                singleLine = true,
                                enabled = !Services.isActive,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Controls
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) +
                            slideInVertically(animationSpec = tween(600, delayMillis = 600)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FlickeringStartButton(
                            modifier = Modifier.scale(1.2f),
                            onClick = {
                                if (!Settings.canDrawOverlays(context)) {
                                    Toast.makeText(context, R.string.request_overlay_permission, Toast.LENGTH_SHORT).show()
                                    overlayPermissionLauncher.launch(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                    )
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }

                                onPostPermissionResult(true)
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "© Project Lumina 2025 | v4.0.3",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            
                            Text(
                                text = "The Game Ends When You Give Up",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        uiVisible = true
    }
}
