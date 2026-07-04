package com.example.cgallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaType
import com.example.cgallery.data.PhysicalAlbumEntity
import com.example.cgallery.ui.MediaGridItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    bucketName: String,
    images: List<MediaItem>,
    onAddToAlbum: (Set<Long>, Boolean) -> Unit = { _, _ -> },
    onChangeCover: () -> Unit = {},
    onImageClick: (GalleryKey) -> Unit,
    onBack: () -> Unit,
    albumImages: List<MediaItem>? = null,
    modifier: Modifier = Modifier
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showMenu by remember { mutableStateOf(false) }

    val currentSelectedIds by rememberUpdatedState(selectedIds)
    val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentOnImageClick by rememberUpdatedState(onImageClick)

    BackHandler(enabled = isSelectionMode) {
        selectedIds = emptySet()
    }

    val onToggleSelection = remember {
        { id: Long ->
            selectedIds = if (id in currentSelectedIds) {
                currentSelectedIds - id
            } else {
                currentSelectedIds + id
            }
        }
    }

    val onLongClickItem = remember {
        { id: Long ->
            if (currentSelectedIds.isEmpty()) {
                selectedIds = setOf(id)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedIds.size} Selected")
                    } else {
                        val albumName = remember(bucketName) { File(bucketName).name }
                        val imageCount = remember(images) { images.count { it.type == MediaType.IMAGE || it.type == MediaType.GIF } }
                        val videoCount = remember(images) { images.count { it.type == MediaType.VIDEO } }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = albumName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (images.isNotEmpty()) {
                                Text(
                                    text = "$imageCount images $videoCount videos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            onAddToAlbum(selectedIds, true)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to Album")
                        }
                        IconButton(onClick = {
                            onAddToAlbum(selectedIds, false)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Copy to Album")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Change Cover") },
                                    onClick = {
                                        showMenu = false
                                        onChangeCover()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(2.dp)
        ) {
            itemsIndexed(images, key = { _, it -> it.id }) { index, image ->
                val isSelected = image.id in selectedIds
                MediaGridItem(
                    image = image,
                    index = index,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (currentIsSelectionMode) {
                            onToggleSelection(image.id)
                        } else {
                            currentOnImageClick(GalleryKey.Viewer(index))
                        }
                    },
                    onLongClick = {
                        onLongClickItem(image.id)
                    }
                )
            }
        }
    }
}
