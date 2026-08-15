package com.cairn.app.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    var biometricEnabled by remember { mutableStateOf(true) }
    var autoLockMinutes by remember { mutableStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ListItem(
                headlineContent = { Text("Biometric unlock") },
                supportingContent = { Text("Use fingerprint or face to open Cairn") },
                trailingContent = { Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it }) }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("PIN fallback") },
                supportingContent = { Text("Uses your device screen-lock PIN/pattern — Cairn never stores its own PIN") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Auto-lock") },
                supportingContent = { Text("Lock Cairn after $autoLockMinutes minute(s) in background") }
            )
            Text(
                "The database is encrypted at rest with SQLCipher using a key held in the Android Keystore. " +
                    "Nothing here is ever uploaded — biometric data never leaves your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
