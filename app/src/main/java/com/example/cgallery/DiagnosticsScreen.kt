package com.example.cgallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    inboxViewModel: InboxViewModel,
    onBack: () -> Unit
) {
    val pendingItems by inboxViewModel.pendingItems.collectAsState()
    val stats by inboxViewModel.stats.collectAsState(null)
    val settings by inboxViewModel.enforcementSettings.collectAsState(EnforcementSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { DiagnosticHeader("Inbox & Workflow") }
            item { DiagnosticRow("Pending Items", pendingItems.size.toString()) }
            item { DiagnosticRow("Total Detected", stats?.totalDetected?.toString() ?: "0") }
            item { DiagnosticRow("Total Completed", stats?.totalCompleted?.toString() ?: "0") }
            item { DiagnosticRow("Total Failed", stats?.totalFailed?.toString() ?: "0") }
            
            item { DiagnosticHeader("Enforcement Engine") }
            item { DiagnosticRow("Enabled", settings.isEnforcementEnabled.toString()) }
            item { DiagnosticRow("Shizuku Available", settings.isShizukuEnabled.toString()) }
            item { DiagnosticRow("Snooze Expiry", settings.snoozeExpirationTime.toString()) }
            item { DiagnosticRow("Snooze Count", settings.currentSnoozeCount.toString()) }

            item { DiagnosticHeader("Last Activity") }
            stats?.let { s ->
                item { DiagnosticRow("Total Processing Time", "${s.totalProcessingTimeMs} ms") }
            }
        }
    }
}

@Composable
private fun DiagnosticHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}
