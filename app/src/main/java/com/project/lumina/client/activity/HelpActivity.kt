package com.project.lumina.client.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.project.lumina.client.ui.theme.LuminaClientTheme

class HelpActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("lumina_prefs", MODE_PRIVATE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap ->
        val allGranted = grantedMap.values.all { it }
        if (allGranted || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    Environment.isExternalStorageManager())
        ) {
            prefs.edit().putBoolean("guide_done", true).apply()
            startActivity(Intent(this, NewMainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.getBoolean("guide_done", false)) {
            startActivity(Intent(this, NewMainActivity::class.java))
            finish()
            return
        }

        setContent {
            LuminaClientTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    GuidePages(
                        onFinish = {
                            prefs.edit().putBoolean("guide_done", true).apply()
                            startActivity(Intent(this, NewMainActivity::class.java))
                            finish()
                        },
                        onRequestPermission = { requestStoragePermissions() }
                    )
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (perms.isNotEmpty()) {
            requestPermissionLauncher.launch(perms)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}

@Composable
private fun GuidePages(
    onFinish: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }

    // 总页数（含新增 3 页）
    val totalPages = 7

    // 动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = (page + 1) / totalPages.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    val pages = listOf<@Composable () -> Unit>(
        {
            GuidePage(
                title = "欢迎使用 LuminaCN！",
                subtitle = "由 MITM 原理驱动的 Minecraft 作弊客户端"
            )
        },
        {
            GuidePage(
                title = "您需要先在 LuminaCN 的账号页中登录！",
                subtitle = "注意，不能在这里注册！即使只是注册 Xbox 游戏名！"
            )
        },
        {
            GuidePage(
                title = "如果进入游戏后没有发现“进服选我”或局域网联机?",
                subtitle = "则你需要手动新建一个服务器！其 IP 为 127.0.0.1，端口为 19132。"
            )
        },
        {
            GuidePage(
                title = "为什么我无法连接服务器?",
                subtitle = "或者无法登录账户? 启动服务后不过一会就崩溃?\n" +
                        "请你检查网络连接 (我们没有任何解决方案)，并关闭一切 VPN 连接。"
            )
        },
        {
            GuidePage(
                title = "您可以通过以下途径联系我们",
                subtitle = "欢迎加入我们的社区以获取帮助"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://qm.qq.com/q/Ny3wLbZwsi"))
                        )
                    }) { Text("加入 QQ 群聊") }
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/EatBingChilling/LuminaCN"))
                        )
                    }) { Text("访问我们的 Github 仓库") }
                }
            }
        },
        {
            var granted by remember { mutableStateOf(false) }
            GuidePage(
                title = "我们所需要的权限: 存储权限",
                subtitle = "获取存储权限以加载配置文件"
            ) {
                Button(
                    onClick = {
                        onRequestPermission()
                        granted = true
                    },
                    enabled = !granted
                ) { Text(if (granted) "已申请" else "申请权限") }
            }
        },
        {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("LuminaCN", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text("您已完成所有引导，开启您的游玩之旅吧！", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onFinish) { Text("完成") }
            }
        }
    )

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 顶部提示条：24dp 圆角、wrap 内容、整体居中
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = "请仔细阅读后再进行社区提问",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 内容区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                pages[page]()
            }

            // 底部一行：上一页 | 进度条 | 下一页
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { if (page > 0) page-- }) {
                    Text("上一页")
                }

                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )

                if (page != 5) {
                    TextButton(
                        onClick = {
                            if (page < pages.size - 1) page++ else onFinish()
                        }
                    ) {
                        Text(if (page == pages.size - 1) "完成" else "下一页")
                    }
                } else {
                    // 占位，保持左右对称
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    }
}

@Composable
private fun GuidePage(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        content()
    }
}
