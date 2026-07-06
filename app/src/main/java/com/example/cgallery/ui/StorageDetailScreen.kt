package com.example.cgallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.cgallery.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDetailScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onAlbumClick: (String) -> Unit) {
    val stats by viewModel.storageStats.collectAsState(); var showClearCacheConf by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.calculateStorage() }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Storage") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } })
    }) { p ->
        if (stats == null) Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else stats?.let { s ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(p)) {
                item { SectionHeader("Overview") }
                item { ListItem(headlineContent = { Text("Total Items") }, supportingContent = { Text("${s.iCount + s.vCount} files tracked") }, leadingContent = { Icon(Icons.Default.Storage, null) }) }
                item { ListItem(headlineContent = { Text("Media Breakdown") }, supportingContent = { Text("Images: ${s.iCount} (${formatSize(s.tISize)})\nVideos: ${s.vCount} (${formatSize(s.tVSize)})") }, leadingContent = { Icon(Icons.Default.PermMedia, null) }) }
                item { SectionHeader("Drives") }
                items(s.volumes) { vol ->
                    val name = if (vol.name == "external_primary") "Internal Storage" else "SD Card / External"
                    ListItem(headlineContent = { Text(name) }, supportingContent = { Text("Images: ${vol.iCount} (${formatSize(vol.imageSize)})\nVideos: ${vol.vCount} (${formatSize(vol.videoSize)})") }, leadingContent = { Icon(if (vol.name == "external_primary") Icons.Default.Storage else Icons.Default.SdCard, null) })
                }
                item { SectionHeader("Top Folders") }
                items(s.buckets.take(15)) { buck ->
                    val drive = if (buck.volumeName == "external_primary") "Internal" else "SD Card"
                    ListItem(
                        headlineContent = { Text(buck.name) }, 
                        supportingContent = { Text("$drive | ${buck.count} files (${formatSize(buck.imageSize + buck.videoSize)})") }, 
                        leadingContent = { Icon(Icons.Default.Folder, null) }, 
                        modifier = Modifier.clickable { onAlbumClick(buck.path) }
                    )
                }
                item { SectionHeader("Maintenance") }
                item { ListItem(headlineContent = { Text("Cache Management") }, supportingContent = { Text("Regenerate thumbnails if scrolling is slow") }, trailingContent = { Button({ showClearCacheConf = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Clear") } }) }
            }
        }
    }
    if (showClearCacheConf) {
        AlertDialog({ showClearCacheConf = false }, title = { Text("Clear Cache?") }, text = { Text("This will regenerate all thumbnails. This helps if scrolling feels slow on lower-RAM devices.") },
            confirmButton = { TextButton({ viewModel.clearCache(); showClearCacheConf = false }) { Text("Clear") } }, dismissButton = { TextButton({ showClearCacheConf = false }) { Text("Cancel") } })
    }
}
