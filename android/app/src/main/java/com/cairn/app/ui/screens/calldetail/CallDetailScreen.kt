package com.cairn.app.ui.screens.calldetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cairn.app.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    callId: Long,
    onBack: () -> Unit,
    viewModel: CallDetailViewModel = hiltViewModel()
) {
    val call by viewModel.call.collectAsStateWithLifecycle(initialValue = null)
    var noteText by remember { mutableStateOf(call?.note ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call detail") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            call?.let { c ->
                Text(c.contactNameSnapshot ?: c.rawNumber, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Type: ${c.callType}")
                Text("Duration: ${formatDuration(c.durationSeconds)}")
                Text("SIM: ${c.simLabel ?: "—"}")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.saveNote(noteText) }) { Text("Save note") }
            }
        }
    }
}
