package com.cairn.app.ui.screens.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cairn.app.worker.CallArchiveWorker

/**
 * Actually requests READ_CONTACTS, READ_CALL_LOG, READ_PHONE_STATE, and (on
 * Android 13+) POST_NOTIFICATIONS via the system permission dialog — this is
 * the piece that was previously missing entirely, meaning the app never had
 * real access to import contacts/call history even though the rest of the
 * archive pipeline assumed it did.
 *
 * The user can still proceed even if they deny some/all permissions — Cairn
 * degrades gracefully (an empty archive, filled in only from what's granted)
 * rather than blocking access to the app.
 */
@Composable
fun PermissionsScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    var requested by remember { mutableStateOf(false) }

    val permissions = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Kick off the initial archive import with whatever was granted —
        // CallArchiveWorker itself already handles SecurityException for
        // anything that wasn't (see worker/CallArchiveWorker.kt), so this
        // is safe to enqueue unconditionally.
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<CallArchiveWorker>().build())
        onGranted()
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Cairn needs a few permissions", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("• Contacts — to build your local archive")
            Text("• Call log — to preserve call history permanently")
            Text("• Phone state — to detect new calls as they happen")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text("• Notifications — to let you know when a backup finishes")
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    requested = true
                    launcher.launch(permissions)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue") }
        }
    }

    // Safety net: if this screen is somehow re-entered after permissions were
    // already granted in a prior session, don't force the dialog again.
    LaunchedEffect(Unit) {
        if (!requested && permissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        ) {
            onGranted()
        }
    }
}
