package com.cairn.app.ui.screens.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.cairn.app.ui.components.CallRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onOpenCall: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val calls = viewModel.calls.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Quick range shortcuts
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = uiState.granularity == TimelineGranularity.DAY, onClick = viewModel::jumpToToday, label = { Text("Today") })
                FilterChip(selected = uiState.granularity == TimelineGranularity.WEEK, onClick = viewModel::jumpToThisWeek, label = { Text("This week") })
                FilterChip(selected = uiState.granularity == TimelineGranularity.MONTH, onClick = viewModel::jumpToThisMonth, label = { Text("This month") })
                FilterChip(selected = uiState.granularity == TimelineGranularity.ALL, onClick = viewModel::clearRange, label = { Text("All") })
            }

            // Year rail — jump directly to 2020, 2021, ... as specified.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.availableYears) { year ->
                    FilterChip(
                        selected = uiState.selectedYear == year,
                        onClick = { viewModel.jumpToYear(year) },
                        label = { Text(year) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            LazyColumn {
                items(calls.itemCount) { index ->
                    calls[index]?.let { call -> CallRow(call = call, onClick = { onOpenCall(call.id) }) }
                }
            }
        }
    }
}
