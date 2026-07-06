package com.example.cgallery
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.*
import com.example.cgallery.ui.MediaGridItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavouritesScreen(
    favouriteImages: List<MediaItem>,
    onImageClick: (GalleryKey) -> Unit,
    onMediaSelected: (List<android.net.Uri>) -> Unit = {},
    isExternalPicker: Boolean = false,
    allowMultiple: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty() || isExternalPicker

    BackHandler(enabled = isSelectionMode) { selectedIds = emptySet() }

    val appSettingsRepository = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { 
                    if (isSelectionMode && !isExternalPicker) { Text("${selectedIds.size} selected") } 
                    else if (isExternalPicker) { Text(if (allowMultiple) "${selectedIds.size} selected" else "Select Item") }
                    else { Text("Favourites") }
                },
                navigationIcon = {
                    if (isSelectionMode && !isExternalPicker) { IconButton({ selectedIds = emptySet() }) { Icon(Icons.Default.Close, "clear") } } 
                    else if (isExternalPicker) { IconButton({ onMediaSelected(emptyList()) }) { Icon(Icons.Default.Close, "cancel") } }
                },
                actions = {
                    if (isExternalPicker && allowMultiple) {
                        IconButton({ onMediaSelected(selectedIds.mapNotNull { id -> favouriteImages.find { it.id == id }?.uri }) }, enabled = selectedIds.isNotEmpty()) { Icon(Icons.Default.Check, "ok") }
                    }
                }
            ) 
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { p ->
        if (favouriteImages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("nothing here yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val columns = if (appSettings.gridDensity == GridDensity.COMPACT) 5 else 3
            LazyVerticalGrid(GridCells.Fixed(columns), modifier = modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(2.dp)) {
                itemsIndexed(favouriteImages, key = { _, i -> i.id }) { index, img ->
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
}
