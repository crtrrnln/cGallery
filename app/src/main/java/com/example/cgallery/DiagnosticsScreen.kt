package com.example.cgallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.AppSettings
import com.example.cgallery.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    inboxViewModel: InboxViewModel,
    onBack: () -> Unit
) {
    val pendingItems by inboxViewModel.pendingItems.collectAsState()
    val stats by inboxViewModel.stats.collectAsState(null)
    val settings by inboxViewModel.enforcementSettings.collectAsState(AppSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { DebugHeader("Inbox State") }
            item { DebugRow("Pending", pendingItems.size.toString()) }
            item { DebugRow("Total Found", stats?.totalDetected?.toString() ?: "0") }
            item { DebugRow("Total Done", stats?.totalCompleted?.toString() ?: "0") }
            item { DebugRow("Total Failed", stats?.totalFailed?.toString() ?: "0") }
            
            item { DebugHeader("Enforcement") }
            item { DebugRow("Active", settings.isEnforcementEnabled.toString()) }
            item { DebugRow("Shizuku", settings.isShizukuEnabled.toString()) }
            item { DebugRow("Snooze End", settings.snoozeExpirationTime.toString()) }
            item { DebugRow("Snooze Count", settings.currentSnoozeCount.toString()) }

            item { DebugHeader("Performance") }
            stats?.let { s ->
                item { DebugRow("Processing Time", "${s.totalProcessingTimeMs}ms") }
            }
        }
    }
}

@Composable
private fun DebugHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DebugRow(label: String, value: String) {
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
