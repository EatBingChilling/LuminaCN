package com.project.lumina.client.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.lumina.client.R
import com.project.lumina.client.ui.theme.LuminaClientTheme
import com.project.lumina.client.util.UpdateCheck

class CrashHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UpdateCheck().initiateHandshake(this)

        val crashMessage = intent?.getStringExtra("message") ?: return finish()
        val stackTrace = crashMessage.lines()
            .dropWhile { !it.startsWith("Stack Trace:") }
            .joinToString("\n")
            .ifEmpty { crashMessage }          // fallback

        setContent {
            LuminaClientTheme {
                val context = LocalContext.current

                BackHandler { restartApp(context) }

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stack Trace
                        SelectionContainer {
                            Text(
                                text = stackTrace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Split Button Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 主按钮：复制日志
                            Button(
                                onClick = {
                                    copyToClipboard(context, crashMessage)
                                    Toast.makeText(
                                        context,
                                        getString(R.string.crash_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(text = "复制日志", textAlign = TextAlign.Center)
                            }

                            // 次级按钮：重启
                            OutlinedButton(
                                onClick = { restartApp(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("崩溃日志", text))
    }

    private fun restartApp(context: Context) {
        val intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.let {
                Intent.makeRestartActivityTask(it)
            }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
