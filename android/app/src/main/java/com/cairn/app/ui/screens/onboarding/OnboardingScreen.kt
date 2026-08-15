package com.cairn.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    // TODO: replace with a HorizontalPager of 3 cards (Local-first / Encrypted / Your data never leaves)
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Your calls, kept forever.", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Cairn archives your contacts and call history locally and encrypted. " +
                    "No ads, no analytics, no cloud required.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
        }
    }
}
