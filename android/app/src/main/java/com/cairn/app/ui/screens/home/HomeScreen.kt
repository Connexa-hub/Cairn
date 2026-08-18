package com.cairn.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.cairn.app.ui.components.CallRow
import com.cairn.app.ui.components.DotMatrix
import com.cairn.app.ui.screens.timeline.TimelineViewModel
import com.cairn.app.ui.theme.CairnColors

/**
 * Search-first home. Two deliberate departures from stock Material here,
 * both aimed at not reading as "generic Google app": a quiet dot-matrix
 * texture behind the header (Nothing OS's signature decorative language,
 * used sparingly — never the focal point), and quick-access tiles that use
 * Cash-App-style confident flat color blocking (one distinct color per
 * tile) instead of identical small icon bubbles in a single repeated tint.
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

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HomeHeader() }
            item { SearchEntryField(onClick = onOpenSearch) }
            item {
                QuickAccessRow(
                    onOpenContacts = onOpenContacts,
                    onOpenTimeline = onOpenTimeline,
                    onOpenDashboard = onOpenDashboard,
                    onOpenFavorites = onOpenFavorites,
                    onOpenBackup = onOpenBackup
                )
            }
            item { SectionLabel("RECENT ACTIVITY", modifier = Modifier.padding(top = 8.dp)) }
            items(recentCalls.itemCount) { index ->
                recentCalls[index]?.let { call ->
                    CallRow(call = call, onClick = { onOpenCall(call.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        // Quiet dot-matrix texture, confined to the right edge behind the
        // wordmark — a nod to Nothing OS without literally copying their UI.
        DotMatrix(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(120.dp, 88.dp)
        )
        Text(
            "Cairn",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun SearchEntryField(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CairnColors.hairline),
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

private data class QuickTile(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun QuickAccessRow(
    onOpenContacts: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBackup: () -> Unit
) {
    val tiles = listOf(
        QuickTile("Contacts", Icons.Default.Contacts, CairnColors.tileContacts, onOpenContacts),
        QuickTile("Timeline", Icons.Default.Timeline, CairnColors.tileTimeline, onOpenTimeline),
        QuickTile("Stats", Icons.Default.BarChart, CairnColors.tileStats, onOpenDashboard),
        QuickTile("Favorites", Icons.Default.Star, CairnColors.tileFavorites, onOpenFavorites),
        QuickTile("Backup", Icons.Default.Lock, CairnColors.tileBackup, onOpenBackup)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        tiles.forEach { tile ->
            Surface(
                onClick = tile.onClick,
                shape = MaterialTheme.shapes.large,
                color = tile.color,
                modifier = Modifier.weight(1f).height(80.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tile.icon, contentDescription = tile.label, tint = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tile.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
