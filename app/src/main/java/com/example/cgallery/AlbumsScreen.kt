package com.example.cgallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.AlbumWithMedia
import com.example.cgallery.data.SamsungImportManager
import com.example.cgallery.data.SamsungAlbumEntity
import com.example.cgallery.data.SamsungFolderEntity
import kotlinx.coroutines.launch

data class Album(
    val name: String,
    val count: Int,
    val coverImage: MediaItem
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    images: List<MediaItem>,
    virtualAlbums: List<AlbumWithMedia> = emptyList(),
    onCreateAlbum: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit,
    onVirtualAlbumClick: (AlbumWithMedia) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val samsungImportManager = remember { SamsungImportManager(context) }
    
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    
    val samsungAlbums by samsungImportManager.allAlbums.collectAsState(initial = emptyList())
    val samsungFolders by samsungImportManager.allFolders.collectAsState(initial = emptyList())
    
    // Group albums by folder
    val albumsByFolder = remember(samsungAlbums) {
        samsungAlbums.groupBy { it.folderId }
    }
    val importStatus by samsungImportManager.importStatus.collectAsState()
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                    if (content != null) {
                        samsungImportManager.importFromJson(content)
                    }
                } catch (e: Exception) {
                    // Error handled by importStatus
                }
            }
        }
    }

    val physicalAlbums = remember(images) {
        images.groupBy { it.bucketName }.map { (name, imagesInAlbum) ->
            Album(
                name = name,
                count = imagesInAlbum.size,
                coverImage = imagesInAlbum.first()
            )
        }.sortedBy { it.name }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Albums") },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Import Samsung Albums")
                    }
                    IconButton(onClick = { showCreateAlbumDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Virtual Album")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (samsungFolders.isNotEmpty() || samsungAlbums.any { it.folderId == null }) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Samsung Albums", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = {
                            scope.launch {
                                samsungImportManager.clearImportedData()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Samsung Albums")
                        }
                    }
                }
                
                // Display folders with their albums in grid layout (Samsung Gallery style)
                samsungFolders.forEach { folder ->
                    val albumsInFolder = albumsByFolder[folder.id] ?: emptyList()
                    if (albumsInFolder.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Column {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                // Display albums in a simple row for now (nested grids cause issues)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    albumsInFolder.take(4).forEach { album ->
                                        SamsungAlbumItem(
                                            album = album,
                                            onClick = { /* TODO: Navigate to album detail */ },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Display orphan albums (no folder) in grid
                val orphanAlbums = albumsByFolder[null] ?: emptyList()
                if (orphanAlbums.isNotEmpty()) {
                    items(orphanAlbums) { album ->
                        SamsungAlbumItem(
                            album = album,
                            onClick = { /* TODO: Navigate to album detail */ }
                        )
                    }
                }
            }

            if (virtualAlbums.isNotEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Text("Virtual Albums", style = MaterialTheme.typography.titleMedium)
                }
                items(virtualAlbums) { albumWithMedia ->
                    val coverMedia = images.find { it.id == albumWithMedia.mediaIds.firstOrNull() }
                    VirtualAlbumItem(
                        albumWithMedia = albumWithMedia,
                        coverMedia = coverMedia,
                        onClick = { onVirtualAlbumClick(albumWithMedia) }
                    )
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Text("Folders", style = MaterialTheme.typography.titleMedium)
            }
            items(physicalAlbums) { album ->
                AlbumItem(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }

    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("New Virtual Album") },
            text = {
                TextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    placeholder = { Text("Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAlbumName.isNotBlank()) {
                        onCreateAlbum(newAlbumName)
                        newAlbumName = ""
                        showCreateAlbumDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Samsung Albums") },
            text = {
                Column {
                    Text("Select a JSON file exported from Samsung Gallery")
                    Spacer(modifier = Modifier.height(8.dp))
                    when (importStatus) {
                        is SamsungImportManager.ImportStatus.Idle -> {
                            Button(onClick = { filePickerLauncher.launch("application/json") }) {
                                Text("Select File")
                            }
                        }
                        is SamsungImportManager.ImportStatus.Loading -> {
                            CircularProgressIndicator()
                        }
                        is SamsungImportManager.ImportStatus.Success -> {
                            val status = importStatus as SamsungImportManager.ImportStatus.Success
                            Text("Imported ${status.albumCount} albums and ${status.folderCount} folders")
                        }
                        is SamsungImportManager.ImportStatus.Error -> {
                            val status = importStatus as SamsungImportManager.ImportStatus.Error
                            Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                Column {
                    TextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            scope.launch {
                                samsungImportManager.createFolder(newFolderName)
                                newFolderName = ""
                                showCreateFolderDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VirtualAlbumItem(
    albumWithMedia: AlbumWithMedia,
    coverMedia: MediaItem?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        if (coverMedia != null) {
            AsyncImage(
                model = coverMedia.uri,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = albumWithMedia.album.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${albumWithMedia.mediaIds.size} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = album.coverImage.uri,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${album.count} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SamsungAlbumItem(
    album: SamsungAlbumEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = album.coverPath,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = album.path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
