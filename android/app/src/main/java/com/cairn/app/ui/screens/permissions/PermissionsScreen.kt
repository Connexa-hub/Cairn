package com.cairn.app.ui.screens.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Requests READ_CONTACTS, READ_CALL_LOG, READ_PHONE_STATE, POST_NOTIFICATIONS
 * via ActivityResultContracts.RequestMultiplePermissions (wired at the
 * Activity level — omitted here since it needs a real Activity context).
 * Each permission's purpose is explained inline before the system prompt,
 * per Play policy on sensitive permissions.
 */
@Composable
fun PermissionsScreen(onGranted: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Cairn needs a few permissions", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("• Contacts — to build your local archive")
            Text("• Call log — to preserve call history permanently")
            Text("• Phone state — to detect new calls as they happen")
            Text("• Notifications — to let you know when a backup finishes")
            Spacer(Modifier.height(32.dp))
            Button(onClick = onGranted, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        }
    }
}
