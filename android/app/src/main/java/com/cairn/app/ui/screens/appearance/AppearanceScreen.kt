package com.cairn.app.ui.screens.appearance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var dynamicColor by remember { mutableStateOf(true) }
    var amoled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ListItem(
                headlineContent = { Text("Dynamic color") },
                supportingContent = { Text("Match your wallpaper's palette (Android 12+)") },
                trailingContent = { Switch(checked = dynamicColor, onCheckedChange = { dynamicColor = it }) }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("True AMOLED black") },
                supportingContent = { Text("Pure black background to save battery on OLED screens") },
                trailingContent = { Switch(checked = amoled, onCheckedChange = { amoled = it }) }
            )
        }
    }
}
