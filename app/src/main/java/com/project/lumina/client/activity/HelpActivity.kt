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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.project.lumina.client.ui.theme.LuminaClientTheme

// Activity 部分保持不变
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                // 建议使用 ActivityResultLauncher 来处理返回结果
                startActivity(intent)
            } else {
                 // 如果已经有 MANAGE_EXTERNAL_STORAGE 权限，则直接认为是完成
                // 您可以在 onResume 中检查权限并自动跳转
            }
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (perms.isNotEmpty()) {
                requestPermissionLauncher.launch(perms)
            }
        }
    }
}


// =================================================================
// ============== 以下是优化后的 Composable 函数 =====================
// =================================================================

@Composable
private fun GuidePages(
    onFinish: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }

    // 总页数
    val totalPages = 7

    val pages = listOf<@Composable () -> Unit>(
        {
            GuidePage(
                title = "欢迎使用 LuminaCN",
                subtitle = "由 MITM 原理驱动的 Minecraft 作弊客户端。"
            )
        },
        {
            GuidePage(
                title = "登录您的账号",
                subtitle = "您需要先在 LuminaCN 的账号页中登录。请注意，不能在此应用内注册，即使只是注册 Xbox 游戏名。"
            )
        },
        {
            GuidePage(
                title = "如何进入服务器？",
                subtitle = "如果进入游戏后没有发现“进服选我”或局域网联机，则需要您手动添加一个服务器，IP 地址为 127.0.0.1，端口为 19132。"
            )
        },
        {
            GuidePage(
                title = "连接问题排查",
                subtitle = "无法连接服务器、登录失败或服务启动后崩溃？\n请检查您的网络连接并关闭所有 VPN 应用。我们对此类网络问题无法提供特定的解决方案。"
            )
        },
        {
            GuidePage(
                title = "获取帮助与支持",
                subtitle = "欢迎加入我们的社区，与其他玩家交流或寻求帮助。"
            ) {
                // 这个 Column 确保按钮组本身是左对齐的
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://qm.qq.com/q/Ny3wLbZwsi"))
                        )
                    }) { Text("加入 QQ 群聊") }
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/EatBingChilling/LuminaCN"))
                        )
                    }) { Text("访问 Github 仓库") }
                }
            }
        },
        {
            var requested by remember { mutableStateOf(false) }
            GuidePage(
                title = "请求权限",
                subtitle = "我们需要存储权限来读取和保存配置文件，确保客户端功能的正常运行。"
            ) {
                Button(
                    onClick = {
                        onRequestPermission()
                        requested = true
                    }
                ) { Text(if (requested) "已请求权限" else "授予存储权限") }
            }
        },
        {
            // 最后一页保持居中，作为结束页
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

    Scaffold(
        topBar = {
            // 顶部提示条保持居中，作为视觉焦点
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "请仔细阅读后再进行社区提问",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        bottomBar = {
            // 底部导航栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(), // 适配全面屏手势导航
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 上一页按钮
                TextButton(
                    onClick = { if (page > 0) page-- },
                    enabled = page > 0
                ) {
                    Text("上一页")
                }

                // 页面指示器（居中）
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PageIndicator(totalPages = totalPages, currentPage = page)
                }

                // 下一页/完成按钮
                val isLastPage = page == pages.size - 1
                TextButton(
                    onClick = {
                        if (!isLastPage) page++ else onFinish()
                    }
                ) {
                    Text(if (isLastPage) "完成" else "下一页")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 内容区
            pages[page]()
        }
    }
}

/**
 * Pixel 风格的页面指示器
 * @param totalPages 总页数
 * @param currentPage 当前页码 (0-indexed)
 */
@Composable
fun PageIndicator(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isSelected = index == currentPage
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 300)
            )
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 300)
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape) // 使用 CircleShape 变成胶囊状
                    .background(color)
            )
        }
    }
}


/**
 * 优化后的引导页基础布局
 * @param title 大标题
 * @param subtitle 副标题/描述
 * @param content 可选的额外内容，如按钮等
 */
@Composable
private fun GuidePage(
    title: String,
    subtitle: String,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    Column(
        // 垂直居中整个内容块
        verticalArrangement = Arrangement.Center,
        // 水平方向靠左对齐
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp) // 给内容区添加左右边距
    ) {
        // 使用更大的字体样式
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp)) // 增加间距
        Text(
            subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // 使用柔和一些的颜色
        )
        // 如果有额外内容，则显示
        if (content != null) {
            Spacer(Modifier.height(32.dp))
            content()
        }
    }
}