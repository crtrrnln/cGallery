package com.example.cgallery

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaFolder
import com.example.cgallery.data.MonitoredFolderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSettingsScreen(
    viewModel: InboxViewModel,
    onBack: () -> Unit
) {
    val folders by viewModel.monitoredFolders.collectAsState()
    var showFolderPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Inbox Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.loadMediaFolders()
                showFolderPicker = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Folder")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val enforcementSettings by viewModel.enforcementSettings.collectAsState(com.example.cgallery.data.EnforcementSettings())

            Text(
                text = "Enforcement",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            ListItem(
                headlineContent = { Text("Enable Enforcement") },
                supportingContent = { Text("Newly detected media must be organised in the Inbox before appearing in the Gallery.") },
                trailingContent = {
                    Switch(
                        checked = enforcementSettings.isEnforcementEnabled,
                        onCheckedChange = { viewModel.updateEnforcementEnabled(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Shizuku Integration") },
                supportingContent = { Text("Allow cGallery to automatically launch the Inbox when new media is detected.") },
                trailingContent = {
                    Switch(
                        checked = enforcementSettings.isShizukuEnabled,
                        onCheckedChange = { viewModel.updateShizukuEnabled(it) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                text = "Monitored Folders",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "cGallery scans these folders for new media to organise. New content from these paths will appear in your Inbox.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(folders) { folder ->
                    MonitoredFolderItem(
                        folder = folder,
                        onToggle = { viewModel.toggleMonitoredFolder(folder) },
                        onDelete = { viewModel.removeMonitoredFolder(folder) },
                        onResetFilter = { viewModel.resetFolderFilter(folder) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (showFolderPicker) {
        val mediaFolders by viewModel.allMediaFolders.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Select Folder to Monitor") },
            text = {
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    if (mediaFolders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(mediaFolders, key = { it.path }) { folder ->
                                MediaFolderPickerItem(
                                    folder = folder,
                                    onClick = {
                                        viewModel.addMonitoredFolder(folder.path, folder.name)
                                        showFolderPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFolderPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MediaFolderPickerItem(
    folder: MediaFolder,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val request = remember(folder.coverUri) {
        coil.request.ImageRequest.Builder(context)
            .data(folder.coverUri)
            .crossfade(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .size(150)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${folder.itemCount} items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MonitoredFolderItem(
    folder: MonitoredFolderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onResetFilter: () -> Unit
) {
    ListItem(
        headlineContent = { Text(folder.displayName) },
        supportingContent = { 
            Column {
                Text(
                    text = folder.folderPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (folder.ignoreBeforeTimestamp > 0) {
                    Text(
                        text = "Ignoring items added before folder monitoring was enabled.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (folder.ignoreBeforeTimestamp > 0) {
                    IconButton(onClick = onResetFilter) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan all existing items")
                    }
                }
                Switch(checked = folder.isEnabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    )
}
