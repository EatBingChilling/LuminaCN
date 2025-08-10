/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * Material Design 3 Expressive 风格重写
 */

package com.project.lumina.client.router.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.ui.component.PackItem
import com.project.lumina.client.util.*
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.overlay.mods.NotificationType

object PackSelectionManager {
    var selectedPack: MCPack? by mutableStateOf(null)
}

@Composable
fun PacksScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var packs by remember { mutableStateOf<List<MCPack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showProgress by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        try {
            packs = MCPackUtils.fetchPacksFromJson(API.FILES_SERVICE_PACK_INDEX_JSON)
        } catch (e: Exception) {
            SimpleOverlayNotification.show("加载失败：${e.message}", NotificationType.ERROR, 3000)
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left: Pack List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                ) {
                    PackListContent(
                        packs = packs,
                        isLoading = isLoading,
                        onPackSelected = { PackSelectionManager.selectedPack = it },
                        onDownloadClick = { pack ->
                            showProgress = true
                            downloadProgress = 0f
                            scope.launch {
                                try {
                                    MCPackUtils.downloadAndOpenPack(context, pack) {
                                        downloadProgress = it
                                    }
                                } catch (e: Exception) {
                                    SimpleOverlayNotification.show(
                                        "下载失败：${e.message}",
                                        NotificationType.ERROR,
                                        3000
                                    )
                                } finally {
                                    showProgress = false
                                }
                            }
                        }
                    )
                }

                // Right: Selected Pack Details
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                ) {
                    PackSelectionManager.selectedPack?.let { pack ->
                        PackDetailContent(pack = pack)
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "选择材质包查看详情",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                PackListContent(
                    packs = packs,
                    isLoading = isLoading,
                    onPackSelected = { PackSelectionManager.selectedPack = it },
                    onDownloadClick = { pack ->
                        showProgress = true
                        downloadProgress = 0f
                        scope.launch {
                            try {
                                MCPackUtils.downloadAndOpenPack(context, pack) {
                                    downloadProgress = it
                                }
                            } catch (e: Exception) {
                                SimpleOverlayNotification.show(
                                    "下载失败：${e.message}",
                                    NotificationType.ERROR,
                                    3000
                                )
                            } finally {
                                showProgress = false
                            }
                        }
                    }
                )
            }
        }

        /* 下载进度对话框 */
        if (showProgress) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("下载材质包") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = { }
            )
        }
    }
}

@Composable
private fun PackListContent(
    packs: List<MCPack>,
    isLoading: Boolean,
    onPackSelected: (MCPack) -> Unit,
    onDownloadClick: (MCPack) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "材质包管理",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
            packs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "暂无可用材质包",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(packs) { pack ->
                        EnhancedPackCard(
                            pack = pack,
                            isSelected = PackSelectionManager.selectedPack == pack,
                            onClick = { onPackSelected(pack) },
                            onDownloadClick = { onDownloadClick(pack) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackDetailContent(pack: MCPack) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            pack.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "描述",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    pack.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "版本：${pack.version}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "作者：${pack.author}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "已选择：${pack.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun EnhancedPackCard(
    pack: MCPack,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDownloadClick: (MCPack) -> Unit
) {
    val elevation by animateDpAsState(
        if (isSelected) 8.dp else 4.dp,
        label = "packCardElevation"
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    pack.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    pack.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text("v${pack.version}") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text(pack.author) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            FilledTonalIconButton(
                onClick = { onDownloadClick(pack) },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "下载",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
