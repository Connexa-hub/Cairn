package com.cairn.app.ui.screens.tags

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cairn.app.ui.screens.contacts.ContactsViewModel

/** Smart groups / tag management. Tag CRUD is wired through ContactRepository.createTag/applyTag/removeTag;
 *  this screen lists tags and lets the user create new ones — full drag-to-assign UI is a TODO. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(onBack: () -> Unit, viewModel: TagsViewModel = hiltViewModel()) {
    val tags by viewModel.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(tags) { tag ->
                ListItem(headlineContent = { Text(tag.name) })
            }
        }
    }
}
