package com.cairn.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cairn.app.data.local.entity.CallLogEntity
import com.cairn.app.data.local.entity.CallType
import com.cairn.app.ui.theme.CairnColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun iconFor(type: CallType): ImageVector = when (type) {
    CallType.INCOMING -> Icons.Default.CallReceived
    CallType.OUTGOING -> Icons.Default.CallMade
    CallType.MISSED -> Icons.Default.CallMissed
    CallType.REJECTED, CallType.BLOCKED -> Icons.Default.Block
    CallType.UNKNOWN -> Icons.Default.CallReceived
}

private fun colorFor(type: CallType) = when (type) {
    CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> CairnColors.danger
    else -> CairnColors.success
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

@Composable
fun CallRow(call: CallLogEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val time = remember_formatted(call.timestampEpoch)
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorFor(call.callType).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconFor(call.callType), contentDescription = call.callType.name, tint = colorFor(call.callType))
            }
        },
        headlineContent = {
            Text(
                call.contactNameSnapshot ?: call.rawNumber,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text("$time · ${call.simLabel ?: "SIM"} · ${formatDuration(call.durationSeconds)}")
        },
        trailingContent = {
            if (call.note != null) {
                Icon(androidx.compose.material.icons.Icons.Default.StickyNote2, contentDescription = "Has note")
            }
        }
    )
}

@Composable
private fun remember_formatted(epoch: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(formatter)
}
