package com.cairn.app.ui.screens.backup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Local encrypted backup is always available (no network needed). Cloud
 * backup to the user's own Render-hosted backend is opt-in and clearly
 * labeled with the zero-knowledge guarantee.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    var passphrase by remember { mutableStateOf("") }
    val status by viewModel.status.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Backups are encrypted on this device before anything leaves it. " +
                    "If you forget your backup passphrase, nobody — including us — can recover it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Backup passphrase") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.createLocalBackup(passphrase) }, modifier = Modifier.fillMaxWidth()) {
                Text("Create encrypted local backup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.uploadToCloud(passphrase) }, modifier = Modifier.fillMaxWidth()) {
                Text("Upload to my backend (optional)")
            }
            Spacer(Modifier.height(16.dp))
            Text(status)
        }
    }
}
