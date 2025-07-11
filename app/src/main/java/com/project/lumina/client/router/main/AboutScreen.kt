/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don't bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */

package com.project.lumina.client.router.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    // 用于控制使用教程对话框的显示状态
    val showTutorialDialog = remember { mutableStateOf(false) }
    // 存储教程文本内容
    val tutorialText = remember { mutableStateOf("") }

    // 如果对话框需要显示，读取教程内容
    if (showTutorialDialog.value && tutorialText.value.isEmpty()) {
        try {
            // 从 raw 资源中读取 t.txt 文件内容
            val inputStream = context.resources.openRawResource(R.raw.t)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.use { it.readText() }
            tutorialText.value = text
        } catch (e: Exception) {
            tutorialText.value = "无法加载教程内容"
            e.printStackTrace()
        }
    }

    // 使用教程对话框
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                Text(
                    stringResource(R.string.about_lumina),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.weight(1f))
                
                
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
                
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.connect_with_us),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        SocialMediaIcon(
                            icon = painterResource(id = R.drawable.ic_github),
                            label = "GitHub",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EatBingChilling/LuminaCN"))
                                context.startActivity(intent)
                            }
                        )
                        
                        
                        SocialMediaIcon(
                            icon = painterResource(id = R.drawable.ic_discord),
                            label = "Discord",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/invite/6kz3dcndrN"))
                                context.startActivity(intent)
                            }
                        )
                        
                        
                        SocialMediaIcon(
                            icon = Icons.Filled.Public,
                            label = "QQ",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/fQ5wdjaeOc"))
                                context.startActivity(intent)
                            }
                        )
                        
                        
                        SocialMediaIcon(
                            icon = painterResource(id = R.drawable.ic_youtube),
                            label = "YouTube",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@prlumina"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // 添加实用工具卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // 主标题：实用工具
                Text(
                    "实用工具",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                // 副标题：推荐使用
                Text(
                    "推荐使用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.padding(8.dp))
                
                // 工具按钮区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 下载客户端按钮
                    ToolButton(
                        icon = Icons.Filled.Download,
                        text = "下载客户端",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://mcapks.net"))
                            context.startActivity(intent)
                        }
                    )
                    
                    // 加入群聊按钮
                    ToolButton(
                        icon = Icons.Filled.Group,
                        text = "加入群聊",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/dxqhrjC9Nu"))
                            context.startActivity(intent)
                        }
                    )
                    
                    // 使用教程按钮
                    ToolButton(
                        icon = Icons.Filled.Help,
                        text = "使用教程",
                        onClick = { showTutorialDialog.value = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            .clickable(onClick = onClick)
            .padding(8.dp)
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

// 新增的工具按钮组件
@Composable
private fun ToolButton(
    icon: Any,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
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
            color = MaterialTheme.colorScheme.primary
        )
    }
}
