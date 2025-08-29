package com.phoenix.luminacn.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.phoenix.luminacn.ui.theme.LuminaClientTheme

// --- Constants for page numbers to make logic clearer ---
private const val STORAGE_PERMISSION_PAGE = 5
private const val OVERLAY_PERMISSION_PAGE = 6
private const val TOTAL_PAGES = 8

class HelpActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("lumina_prefs", MODE_PRIVATE)
    }

    // 将页面状态提升到 Activity 中，以便在 onResume 中控制它
    private val currentPage = mutableStateOf(0)

    // Launcher 的具体回调逻辑现在统一在 onResume 中处理
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 结果在 onResume 中统一检查 */ }

    private val requestSpecialPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* 结果在 onResume 中统一检查 */ }

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
                    // 将状态和事件处理函数传递给 Composable
                    GuidePages(
                        currentPage = currentPage.value,
                        onPageChange = { newPage -> currentPage.value = newPage },
                        onFinish = {
                            prefs.edit().putBoolean("guide_done", true).apply()
                            startActivity(Intent(this, NewMainActivity::class.java))
                            finish()
                        },
                        onRequestStoragePermission = { requestStoragePermissions() },
                        onRequestOverlayPermission = { requestOverlayPermission() }
                    )
                }
            }
        }
    }

    // 实现 onResume 来处理从设置页返回的逻辑，实现自动翻页
    override fun onResume() {
        super.onResume()

        // 检查当前是否停留在权限请求页面
        when (currentPage.value) {
            STORAGE_PERMISSION_PAGE -> {
                // 检查存储权限是否已被授予
                val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
                // 如果已授予，自动翻到下一页
                if (storageGranted) {
                    currentPage.value++
                }
            }
            OVERLAY_PERMISSION_PAGE -> {
                // 检查悬浮窗权限是否已被授予
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    // 如果已授予，自动翻到下一页
                    currentPage.value++
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                requestSpecialPermissionLauncher.launch(intent)
            }
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (perms.isNotEmpty()) {
                requestStoragePermissionLauncher.launch(perms)
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                requestSpecialPermissionLauncher.launch(intent)
            }
        }
    }
}

// =================================================================
// ============== 以下是优化后的 Composable 函数 =====================
// =================================================================

@Composable
private fun GuidePages(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onFinish: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current

    val pages = listOf<@Composable () -> Unit>(
        // Page 0
        {
            GuidePage(
                title = "欢迎使用 Kitasan",
                subtitle = "一款基于 LuminaCN 的 Minecraft 辅助客户端，由 MITM 驱动 (Lunaris)。"
            )
        },
        // Page 1
        {
            GuidePage(
                title = "登录您的账号",
                subtitle = "您需要先在 Kitasan 的账号页中登录。请注意，不能在此应用内注册，即使只是注册 Xbox 游戏名。"
            )
        },
        // Page 2
        {
            GuidePage(
                title = "如何进入服务器？",
                subtitle = "如果进入游戏后没有发现“进服选我”或局域网联机，则需要您手动添加一个服务器，IP 地址为 127.0.0.1，端口为 19132。"
            )
        },
        // Page 3
        {
            GuidePage(
                title = "连接问题排查",
                subtitle = "无法连接服务器、登录失败或服务启动后崩溃？\n请检查您的网络连接并关闭所有 VPN 应用。我们对此类网络问题无法提供特定的解决方案。"
            )
        },
        // Page 4
        {
            GuidePage(
                title = "获取帮助与支持",
                subtitle = "遇到问题？欢迎加入我们的社区，与其他玩家交流或寻求帮助。"
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/Ny3wLbZwsi"))
                        )
                    }) { Text("加入 QQ 群聊") }
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EatBingChilling/LuminaCN"))
                        )
                    }) { Text("访问 Github 仓库") }
                }
            }
        },
        // Page 5 (STORAGE_PERMISSION_PAGE)
        {
            var requested by remember { mutableStateOf(false) }
            GuidePage(
                title = "请求存储权限",
                subtitle = "我们需要存储权限来读取和保存配置文件（如模块设置、好友列表等），确保客户端功能的正常运行。"
            ) {
                Button(
                    onClick = {
                        onRequestStoragePermission()
                        requested = true
                    }
                ) { Text(if (requested) "已请求，请在设置中授予" else "授予存储权限") }
            }
        },
        // Page 6 (OVERLAY_PERMISSION_PAGE)
        {
            var requested by remember { mutableStateOf(false) }
            GuidePage(
                title = "请求悬浮窗权限",
                subtitle = "为了在游戏界面上显示功能菜单（用于开启/关闭各种功能），我们需要悬浮窗权限。这是客户端核心功能之一。"
            ) {
                Button(
                    onClick = {
                        onRequestOverlayPermission()
                        requested = true
                    }
                ) { Text(if (requested) "已请求，请在设置中授予" else "授予悬浮窗权限") }
            }
        },
        // Page 7
        {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Kitasan", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text("您已完成所有引导，开启您的游玩之旅吧！", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onFinish) { Text("完成") }
            }
        }
    )

    Scaffold(
        topBar = {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { if (currentPage > 0) onPageChange(currentPage - 1) },
                    enabled = currentPage > 0
                ) {
                    Text("上一页")
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PageIndicator(totalPages = TOTAL_PAGES, currentPage = currentPage)
                }

                val isLastPage = currentPage == pages.size - 1
                TextButton(
                    onClick = {
                        if (!isLastPage) onPageChange(currentPage + 1) else onFinish()
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
            pages[currentPage]()
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