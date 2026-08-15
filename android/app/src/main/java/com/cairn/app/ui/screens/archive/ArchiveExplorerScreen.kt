package com.cairn.app.ui.screens.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.cairn.app.ui.components.CallRow
import com.cairn.app.ui.screens.timeline.TimelineViewModel

/**
 * Raw, filterable table view over the entire archive — the "browse
 * everything" counterpart to Search and Timeline. Reuses TimelineViewModel's
 * unfiltered feed; a full column-filter UI (by SIM, type, tag) is a TODO
 * on top of the same CallLogRepository query surface used by Search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveExplorerScreen(onBack: () -> Unit, viewModel: TimelineViewModel = hiltViewModel()) {
    val calls = viewModel.calls.collectAsLazyPagingItems()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive Explorer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(calls.itemCount) { index ->
                calls[index]?.let { call -> CallRow(call = call, onClick = {}) }
            }
        }
    }
}
