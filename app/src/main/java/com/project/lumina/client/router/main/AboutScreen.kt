/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 * ... (rest of the license header) ...
 * ─────────────────────────────────────────────────────────────────────────────
 */

package com.project.lumina.client.router.main

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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


// ======================================================
//  主 Composable: AboutScreen
// ======================================================
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // --- State Management ---
    var showTutorialDialog by remember { mutableStateOf(false) }
    var tutorialText by remember { mutableStateOf<String?>(null) }

    // --- Logic ---
    // Load tutorial text only when the dialog is about to be shown
    LaunchedEffect(showTutorialDialog) {
        if (showTutorialDialog && tutorialText == null) {
            tutorialText = try {
                context.resources.openRawResource(R.raw.t)
                    .bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                "无法加载教程内容"
            }
        }
    }

    // --- Dialogs ---
    if (showTutorialDialog) {
        TutorialDialog(
            tutorialText = tutorialText ?: "正在加载...",
            onDismiss = { showTutorialDialog = false }
        )
    }

    // --- Layout ---
    // Use a two-pane layout for landscape, and a single column for portrait
    if (isLandscape) {
        LandscapeLayout { showTutorialDialog = true }
    } else {
        PortraitLayout { showTutorialDialog = true }
    }
}


// ======================================================
//  布局 Composable
// ======================================================

@Composable
private fun PortraitLayout(onShowTutorial: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Utilities
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            UtilitiesCardContent(onShowTutorial)
        }

        // Card 2: About Lumina
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            AboutLuminaCardContent()
        }
    }
}

@Composable
private fun LandscapeLayout(onShowTutorial: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Left Pane: Utilities (40% width) ---
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                UtilitiesCardContent(onShowTutorial)
            }
        }

        // --- Right Pane: About Lumina (60% width) ---
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                AboutLuminaCardContent()
            }
        }
    }
}


// ======================================================
//  内容 Composable (可重用)
// ======================================================

/**
 * The content for the "Utilities" card. Reusable in both layouts.
 */
@Composable
private fun UtilitiesCardContent(onShowTutorial: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "实用工具",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "推荐使用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // Tool buttons area
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

/**
 * The content for the "About Lumina" card. Reusable in both layouts.
 */
@Composable
private fun AboutLuminaCardContent() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            stringResource(R.string.about_lumina),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.luminacn_dev), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.lumina_introduction), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.lumina_expectation), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.lumina_compatibility), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.weight(1f, fill = false)) // Pushes content below down, but only if there's space

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

        Spacer(Modifier.height(16.dp))

        // Social media section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.connect_with_us),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialMediaIcon(
                    icon = painterResource(id = R.drawable.ic_github),
                    label = "GitHub",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EatBingChilling/LuminaCN"))) }
                )
                SocialMediaIcon(
                    icon = painterResource(id = R.drawable.ic_discord),
                    label = "Discord",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/invite/6kz3dcndrN"))) }
                )
                SocialMediaIcon(
                    icon = Icons.Filled.Public, // Using a generic icon for QQ as an example
                    label = "QQ",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/fQ5wdjaeOc"))) }
                )
                SocialMediaIcon(
                    icon = painterResource(id = R.drawable.ic_youtube),
                    label = "YouTube",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@prlumina"))) }
                )
            }
        }
    }
}


// ======================================================
//  UI 组件
// ======================================================

@Composable
private fun ToolButton(icon: Any, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp), // Reduced padding for a tighter look
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val iconModifier = Modifier.size(24.dp)
        when (icon) {
            is androidx.compose.ui.graphics.painter.Painter -> {
                Icon(painter = icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = iconModifier)
            }
            is androidx.compose.ui.graphics.vector.ImageVector -> {
                Icon(imageVector = icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = iconModifier)
            }
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SocialMediaIcon(icon: Any, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        val iconModifier = Modifier.size(28.dp)
        when (icon) {
            is androidx.compose.ui.graphics.painter.Painter -> {
                Icon(painter = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = iconModifier)
            }
            is androidx.compose.ui.graphics.vector.ImageVector -> {
                Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = iconModifier)
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TutorialDialog(tutorialText: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用教程") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(tutorialText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}