package com.example.cgallery
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.*
import com.example.cgallery.ui.MediaGridItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    bucketName: String, images: List<MediaItem>,
    onAddToAlbum: (Set<Long>, Boolean) -> Unit = { _, _ -> },
    onChangeCover: () -> Unit = {}, onImageClick: (GalleryKey) -> Unit,
    onBack: () -> Unit, albumImages: List<MediaItem>? = null,
    onMediaSelected: (List<android.net.Uri>) -> Unit = {},
    isExternalPicker: Boolean = false,
    allowMultiple: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty() || isExternalPicker; var showMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) { selectedIds = emptySet() }

    val appSettingsRepository = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    Scaffold(topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (isSelectionMode && !isExternalPicker) { Text("${selectedIds.size} selected") } 
                    else if (isExternalPicker) { Text(if (allowMultiple) "${selectedIds.size} selected" else "Select Item") }
                    else {
                        val albumName = remember(bucketName) { File(bucketName).name }
                        val iCount = remember(images) { images.count { it.type == MediaType.IMAGE || it.type == MediaType.GIF } }
                        val vCount = remember(images) { images.count { it.type == MediaType.VIDEO } }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(albumName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (images.isNotEmpty()) { Text("$iCount imgs, $vCount vids", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode && !isExternalPicker) { IconButton({ selectedIds = emptySet() }) { Icon(Icons.Default.Close, "clear") } } 
                    else if (isExternalPicker) { IconButton({ onMediaSelected(emptyList()) }) { Icon(Icons.Default.Close, "cancel") } }
                    else { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "back") } }
                },
                actions = {
                    if (isExternalPicker && allowMultiple) {
                        IconButton({ onMediaSelected(selectedIds.mapNotNull { id -> images.find { it.id == id }?.uri }) }, enabled = selectedIds.isNotEmpty()) { Icon(Icons.Default.Check, "ok") }
                    } else if (isSelectionMode && !isExternalPicker) {
                        IconButton({ onAddToAlbum(selectedIds, true); selectedIds = emptySet() }) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, "move") }
                        IconButton({ onAddToAlbum(selectedIds, false); selectedIds = emptySet() }) { Icon(Icons.Default.Add, "copy") }
                    } else if (!isExternalPicker) {
                        Box {
                            IconButton({ showMenu = true }) { Icon(Icons.Default.MoreVert, "menu") }
                            DropdownMenu(showMenu, { showMenu = false }) {
                                DropdownMenuItem({ Text("Change Cover") }, { showMenu = false; onChangeCover() }, leadingIcon = { Icon(Icons.Default.Image, null) })
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { p ->
        val columns = if (appSettings.gridDensity == GridDensity.COMPACT) 5 else 3
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(2.dp)) {
            itemsIndexed(images, key = { _, it -> it.id }) { index, img ->
                val isSel = img.id in selectedIds
                MediaGridItem(image = img, index = index, isSelected = isSel, isSelectionMode = isSelectionMode, efficiencyMode = appSettings.efficiencyMode,
                    onClick = {
                        if (isSelectionMode) {
                            if (isExternalPicker && !allowMultiple) onMediaSelected(listOf(img.uri))
                            else selectedIds = if (isSel) selectedIds - img.id else selectedIds + img.id
                        } else onImageClick(GalleryKey.Viewer(index))
                    },
                    onLongClick = { if (selectedIds.isEmpty()) selectedIds = setOf(img.id) }
                )
            }
        }
    }
}
