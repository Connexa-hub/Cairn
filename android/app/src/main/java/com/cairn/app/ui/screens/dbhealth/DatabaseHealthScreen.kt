package com.cairn.app.ui.screens.dbhealth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseHealthScreen(onBack: () -> Unit, viewModel: DatabaseHealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Health") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ListItem(headlineContent = { Text("Integrity check") }, supportingContent = { Text(state.integrityStatus) })
            HorizontalDivider()
            Button(onClick = viewModel::runIntegrityCheck, modifier = Modifier.fillMaxWidth()) { Text("Run integrity check") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::vacuum, modifier = Modifier.fillMaxWidth()) { Text("Optimize database (VACUUM)") }
            Spacer(Modifier.height(16.dp))
            Text(
                "If corruption is ever detected, Cairn will offer to restore automatically from your most recent encrypted local backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
