package com.cairn.app.ui.screens.contactdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.cairn.app.ui.components.CallRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onOpenCall: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel()
) {
    val contact by viewModel.contact.collectAsStateWithLifecycle(initialValue = null)
    val numbers by viewModel.numbers.collectAsStateWithLifecycle(initialValue = emptyList())
    val calls = viewModel.calls.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact?.displayName ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    val isFav = contact?.isFavorite ?: false
                    IconButton(onClick = { viewModel.toggleFavorite(isFav) }) {
                        Icon(if (isFav) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Favorite")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            contact?.displayName?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(contact?.displayName ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    numbers.firstOrNull()?.let {
                        Text(it.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Text(
                    "Call history",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }
            items(calls.itemCount) { index ->
                calls[index]?.let { call -> CallRow(call = call, onClick = { onOpenCall(call.id) }) }
            }
        }
    }
}
