package com.example.cgallery

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaType
import com.example.cgallery.ui.MediaGridItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    images: List<MediaItem>,
    onAddToAlbum: (Long, Set<Long>) -> Unit = { _, _ -> },
    onImageClick: (GalleryKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showAlbumSelection by remember { mutableStateOf(false) }

    val imagesMap = remember(images) { images.associateBy { it.id } }
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentOnImageClick by rememberUpdatedState(onImageClick)

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

    BackHandler(enabled = isSelectionMode) {
        selectedIds = emptySet()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedIds = emptySet()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            "${selectedIds.size} Selected",
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "cGallery",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "v0.53",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            showAlbumSelection = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add to Album")
                        }
                        IconButton(onClick = {
                            val selectedUris = selectedIds.mapNotNull { imagesMap[it]?.uri }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
                                type = "*/*"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = {
                            val selectedUris = selectedIds.mapNotNull { imagesMap[it]?.uri }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, selectedUris)
                                deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                            } else {
                                scope.launch {
                                    selectedUris.forEach { uri ->
                                        context.contentResolver.delete(uri, null, null)
                                    }
                                    selectedIds = emptySet()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(4.dp)
        ) {
            itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
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

    if (showAlbumSelection) {
// ...
        AlertDialog(
            onDismissRequest = { showAlbumSelection = false },
            title = { Text("Add to Album") },
            text = {
                Text(
                    "Virtual albums have been removed. This feature will be updated for physical folders.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showAlbumSelection = false }) {
                    Text("OK")
                }
            }
        )
    }
}
