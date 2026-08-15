package com.cairn.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cairn.app.ui.components.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (uiState.loading || uiState.stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val stats = uiState.stats!!
        val oldest = stats.oldestRecordEpoch?.let {
            DateTimeFormatter.ofPattern("MMM yyyy").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
        } ?: "—"

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(
                listOf(
                    "Total archived calls" to stats.totalCalls.toString(),
                    "Oldest record" to oldest,
                    "Most contacted" to (uiState.mostContactedName ?: "—"),
                    "Longest call" to (stats.longestCall?.let { formatDuration(it.durationSeconds) } ?: "—"),
                    "Average duration" to formatDuration(stats.averageDurationSeconds.toInt()),
                    "Total talk time" to formatDuration(stats.totalDurationSeconds.toInt())
                )
            ) { (label, value) ->
                StatCard(label, value)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
