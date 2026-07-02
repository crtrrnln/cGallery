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
import kotlinx.coroutines.launch

@Composable
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

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
                                "v0.52",
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
                            val selectedUris = images.filter { it.id in selectedIds }.map { it.uri }
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
                            val selectedUris = images.filter { it.id in selectedIds }.map { it.uri }
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
                val itemPadding by animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "padding")
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(itemPadding)
                        .clip(RoundedCornerShape(if (isSelected) 12.dp else 0.dp))
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) {
                                        selectedIds - image.id
                                    } else {
                                        selectedIds + image.id
                                    }
                                } else {
                                    onImageClick(GalleryKey.Viewer(index))
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedIds = setOf(image.id)
                                }
                            }
                        )
                ) {
                    AsyncImage(
                        model = image.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Video indicator (small play icon in bottom-right corner)
                    if (image.type == MediaType.VIDEO) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(24.dp)
                        )
                    }

                    // GIF indicator (small badge in top-left corner)
                    if (image.type == MediaType.GIF) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GIF",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAlbumSelection) {
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
