/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.lumina.client.R
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val scrollState = rememberScrollState()
    val showTutorialDialog = remember { mutableStateOf(false) }
    val tutorialText = remember { mutableStateOf("") }

    if (showTutorialDialog.value && tutorialText.value.isEmpty()) {
        try {
            val inputStream = context.resources.openRawResource(R.raw.t)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.use { it.readText() }
            tutorialText.value = text
        } catch (e: Exception) {
            tutorialText.value = "无法加载教程内容"
        }
    }

    if (showTutorialDialog.value) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog.value = false },
            title = { Text("使用教程") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(tutorialText.value)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog.value = false }) {
                    Text("确定")
                }
            }
        )
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
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                ToolsCard(
                    modifier = Modifier.weight(1f),
                    onShowTutorial = { showTutorialDialog.value = true },
                    context = context
                )
                
                AboutLuminaCard(
                    modifier = Modifier.weight(1f),
                    context = context
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ToolsCard(
                    modifier = Modifier.fillMaxWidth(),
                    onShowTutorial = { showTutorialDialog.value = true },
                    context = context
                )
                
                AboutLuminaCard(
                    modifier = Modifier.fillMaxWidth(),
                    context = context
                )
            }
        }
    }
}

@Composable
private fun ToolsCard(
    modifier: Modifier = Modifier,
    onShowTutorial: () -> Unit,
    context: Context
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "实用工具",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "推荐使用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToolButton(
                    icon = Icons.Filled.Download,
                    text = "下载客户端",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://mcapks.net")))
                    }
                )
                
                ToolButton(
                    icon = Icons.Filled.Group,
                    text = "加入群聊",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/dxqhrjC9Nu")))
                    }
                )
                
                ToolButton(
                    icon = Icons.Filled.Help,
                    text = "使用教程",
                    onClick = onShowTutorial
                )
            }
        }
    }
}

@Composable
private fun AboutLuminaCard(
    modifier: Modifier = Modifier,
    context: Context
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                stringResource(R.string.about_lumina),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.luminacn_dev),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    stringResource(R.string.lumina_introduction),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    stringResource(R.string.lumina_expectation),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    stringResource(R.string.lumina_compatibility),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.lumina_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    stringResource(R.string.lumina_team),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Connect with us",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialMediaIcon(
                        icon = painterResource(id = R.drawable.ic_github),
                        label = "GitHub",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EatBingChilling/LuminaCN")))
                        }
                    )
                    
                    SocialMediaIcon(
                        icon = painterResource(id = R.drawable.ic_discord),
                        label = "Discord",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/invite/6kz3dcndrN")))
                        }
                    )
                    
                    SocialMediaIcon(
                        icon = Icons.Filled.Public,
                        label = "QQ",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/fQ5wdjaeOc")))
                        }
                    )
                    
                    SocialMediaIcon(
                        icon = painterResource(id = R.drawable.ic_youtube),
                        label = "YouTube",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@prlumina")))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: Any,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (icon) {
                is androidx.compose.ui.graphics.painter.Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = text,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is androidx.compose.ui.graphics.vector.ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SocialMediaIcon(
    icon: Any,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        when (icon) {
            is androidx.compose.ui.graphics.painter.Painter -> {
                Icon(
                    painter = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            is androidx.compose.ui.graphics.vector.ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
