package com.cairn.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.cairn.app.ui.components.CallRow
import com.cairn.app.ui.screens.timeline.TimelineViewModel

/**
 * Search-first home: a tappable search field up top (opens the full Search
 * overlay), quick-access tiles for the other pillars, then a live recent-
 * activity feed so the archive feels alive even before the user searches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenContact: (Long) -> Unit,
    onOpenCall: (Long) -> Unit,
    timelineViewModel: TimelineViewModel = hiltViewModel()
) {
    val recentCalls = timelineViewModel.calls.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            LargeTopAppBar(title = { Text("Cairn", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SearchEntryField(onClick = onOpenSearch)
            }
            item {
                QuickAccessRow(
                    onOpenContacts = onOpenContacts,
                    onOpenTimeline = onOpenTimeline,
                    onOpenDashboard = onOpenDashboard,
                    onOpenFavorites = onOpenFavorites,
                    onOpenBackup = onOpenBackup
                )
            }
            item {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(recentCalls.itemCount) { index ->
                recentCalls[index]?.let { call ->
                    CallRow(call = call, onClick = { onOpenCall(call.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchEntryField(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                "Search names, numbers, dates…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAccessRow(
    onOpenContacts: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBackup: () -> Unit
) {
    val tiles = listOf(
        Triple("Contacts", Icons.Default.Contacts, onOpenContacts),
        Triple("Timeline", Icons.Default.Timeline, onOpenTimeline),
        Triple("Stats", Icons.Default.BarChart, onOpenDashboard),
        Triple("Favorites", Icons.Default.Star, onOpenFavorites),
        Triple("Backup", Icons.Default.Lock, onOpenBackup)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        tiles.forEach { (label, icon, action) ->
            Surface(
                onClick = action,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f).height(76.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
