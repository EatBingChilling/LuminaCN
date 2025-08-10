package com.project.lumina.client.router.main

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.project.lumina.client.R
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.util.AuthWebView
import com.project.lumina.client.util.XboxDeviceInfo
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(showNotification: (String, NotificationType) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
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
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountCard(
                    modifier = Modifier.width(380.dp),
                    showNotification = showNotification
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AccountCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    showNotification = showNotification
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AccountCard(
    modifier: Modifier = Modifier,
    showNotification: (String, NotificationType) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddAccountMenu by remember { mutableStateOf(false) }
    var deviceInfo: XboxDeviceInfo? by remember { mutableStateOf(null) }
    
    val isAccountLimitReached = AccountManager.accounts.size >= 2

    ElevatedCard(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                )
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.account),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                FilledIconButton(
                    onClick = { 
                        if (!isAccountLimitReached) {
                            showAddAccountMenu = !showAddAccountMenu
                        } else {
                            showNotification(
                                "只允许最多两个账户",
                                NotificationType.WARNING
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = !isAccountLimitReached,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isAccountLimitReached)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isAccountLimitReached)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    AnimatedContent(
                        targetState = showAddAccountMenu,
                        transitionSpec = {
                            scaleIn() with scaleOut()
                        },
                        label = "AddButtonIcon"
                    ) { isOpen ->
                        Icon(
                            imageVector = if (isOpen) Icons.Rounded.Close else Icons.Rounded.Add,
                            contentDescription = if (isOpen) "关闭" else "添加账户",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = isAccountLimitReached,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "只允许最多两个账户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = showAddAccountMenu && !isAccountLimitReached,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_account),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(XboxDeviceInfo.devices.values.toList()) { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            deviceInfo = device
                                            showAddAccountMenu = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(32.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Text(
                                            stringResource(R.string.login_in, device.deviceType),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (AccountManager.accounts.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 300.dp)
            ) {
                if (AccountManager.accounts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = stringResource(R.string.no_account_added_yet),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(AccountManager.accounts) { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (account == AccountManager.currentAccount) {
                                        AccountManager.selectAccount(null)
                                    } else {
                                        AccountManager.selectAccount(account)
                                    }
                                }
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = 400f
                                    )
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (account == AccountManager.currentAccount)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (account == AccountManager.currentAccount) 4.dp else 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = account.remark,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = account.platform.deviceType,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (account == AccountManager.currentAccount)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            if (account == AccountManager.currentAccount) {
                                                AssistChip(
                                                    onClick = { },
                                                    label = { Text(stringResource(R.string.has_been_selected)) },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                        labelColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                IconButton(
                                    onClick = {
                                        AccountManager.accounts.remove(account)
                                        if (account == AccountManager.currentAccount) {
                                            AccountManager.selectAccount(null)
                                        }
                                        AccountManager.save()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "删除",
                                        tint = if (account == AccountManager.currentAccount)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deviceInfo?.let {
        if (AccountManager.accounts.size < 2) {
            AccountDialog(it) { success ->
                deviceInfo = null
                if (success) {
                    coroutineScope.launch {
                        showNotification(
                            context.getString(R.string.fetch_account_successfully),
                            NotificationType.SUCCESS
                        )
                    }
                } else {
                    coroutineScope.launch {
                        showNotification(
                            context.getString(R.string.fetch_account_failed),
                            NotificationType.ERROR
                        )
                    }
                }
            }
        } else {
            deviceInfo = null
            coroutineScope.launch {
                showNotification(
                    "只允许最多两个账户",
                    NotificationType.WARNING
                )
            }
        }
    }
}
