/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * ... (License header remains the same) ...
 */

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.lumina.client.R
import com.project.lumina.client.ui.theme.LuminaClientTheme

class CrashHandlerActivity : ComponentActivity() {

    private var fullCrashMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fullCrashMessage = intent?.getStringExtra("message")
        if (fullCrashMessage == null) {
            // If there's no message, just restart the app cleanly.
            restartApp(this)
            return
        }

        setContent {
            LuminaClientTheme {
                CrashScreen(
                    crashMessage = fullCrashMessage!!,
                    onCopy = ::copyToClipboard,
                    onRestart = ::restartApp
                )
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Lumina Crash Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.crash_copied), Toast.LENGTH_SHORT).show()
    }

    private fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        if (componentName != null) {
            val restartIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
        }
        Runtime.getRuntime().exit(0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashScreen(
    crashMessage: String,
    onCopy: (Context, String) -> Unit,
    onRestart: (Context) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Extract the stack trace for display, but keep the full message for copying
    val stackTrace = remember(crashMessage) {
        crashMessage.lines()
            .dropWhile { !it.contains("Exception", ignoreCase = true) && !it.contains("Error", ignoreCase = true) }
            .joinToString("\n")
            .ifEmpty { crashMessage }
    }

    BackHandler { onRestart(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.crash_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        },
        bottomBar = {
            ActionBottomBar(
                onCopy = { onCopy(context, crashMessage) },
                onRestart = { onRestart(context) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.crash_subtitle),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.crash_description),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Stack Trace Section
            Text(
                text = stringResource(R.string.crash_stack_trace_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = stackTrace,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBottomBar(
    onCopy: () -> Unit,
    onRestart: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main action: Restart App
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.crash_restart))
            }

            // Secondary action: Copy Log
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.crash_copy_log))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CrashScreenPreview() {
    LuminaClientTheme {
        CrashScreen(
            crashMessage = "java.lang.RuntimeException: An error occurred while rendering the overlay.\n" +
                    "\tat com.project.lumina.client.overlay.manager.OverlayManager.showOverlayWindow(OverlayManager.java:42)\n" +
                    "\tat com.project.lumina.client.mods.SomeMod.onEnable(SomeMod.java:23)\n" +
                    "Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.view.View.setVisibility(int)' on a null object reference\n" +
                    "\tat com.project.lumina.client.overlay.mods.SpeedometerOverlay.Content(SpeedometerOverlay.kt:120)",
            onCopy = { _, _ -> },
            onRestart = {}
        )
    }
}