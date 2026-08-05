package com.example.cgallery
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.*
import com.example.cgallery.ui.MediaGridItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FavouritesScreen(
    favouriteImages: List<MediaItem>, 
    onImageClick: (GalleryKey) -> Unit, 
    onMediaSelected: (List<android.net.Uri>) -> Unit = {}, 
    isExternalPicker: Boolean = false, 
    allowMultiple: Boolean = false, 
    onChangeCover: () -> Unit = {}, 
    onBack: () -> Unit = {}, 
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectionViewModel: SelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val selectedIds by selectionViewModel.selectedMediaIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty() || isExternalPicker
    var showMenu by remember { mutableStateOf(false) }
    BackHandler(enabled = isSelectionMode) { selectionViewModel.clearSelection() }
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val useModern = appSettings.useModernUI

    Scaffold(topBar = { 
        if (useModern && !isSelectionMode) {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Favourites")
                        Text("v0.9/1.0rc3.2UI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                    }
                },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
                actions = {
                    IconButton({ showMenu = true }) { Icon(Icons.Default.MoreVert, "menu") }
                    DropdownMenu(showMenu, { showMenu = false }) { 
                        DropdownMenuItem({ Text("Change Cover") }, { showMenu = false; onChangeCover() }, leadingIcon = { Icon(Icons.Default.Image, null) }) 
                    }
                }
            )
        } else {
            CenterAlignedTopAppBar(
                title = { if (isSelectionMode && !isExternalPicker) Text("${selectedIds.size} selected") else if (isExternalPicker) Text(if (allowMultiple) "${selectedIds.size} selected" else "Select Item") else Text("Favourites") },
                navigationIcon = { 
                    if (isSelectionMode && !isExternalPicker) {
                        IconButton({ selectionViewModel.clearSelection() }) { Icon(Icons.Default.Close, "clear") }
                    } else if (isExternalPicker) {
                        IconButton({ onMediaSelected(emptyList()) }) { Icon(Icons.Default.Close, "cancel") }
                    } else {
                        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") }
                    }
                },
                actions = { 
                    if (isExternalPicker && allowMultiple) IconButton({ onMediaSelected(selectedIds.mapNotNull { id -> favouriteImages.find { it.id == id }?.uri }) }, enabled = selectedIds.isNotEmpty()) { Icon(Icons.Default.Check, "ok") }
                    else if (!isSelectionMode) Box { IconButton({ showMenu = true }) { Icon(Icons.Default.MoreVert, "menu") }; DropdownMenu(showMenu, { showMenu = false }) { DropdownMenuItem({ Text("Change Cover") }, { showMenu = false; onChangeCover() }, leadingIcon = { Icon(Icons.Default.Image, null) }) } }
                }
            )
        }
    }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { p ->
        if (favouriteImages.isEmpty()) Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { Text("nothing here yet", style = MaterialTheme.typography.bodyLarge) }
        else LazyVerticalGrid(columns = GridCells.Fixed(if (appSettings.gridDensity == GridDensity.COMPACT) 5 else 3), modifier = modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(if (useModern) 4.dp else 2.dp)) {
            itemsIndexed(favouriteImages, key = { _, i -> i.id }) { index, img ->
                val isSel = img.id in selectedIds
                
                val itemModifier = if (useModern && sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = "image_${img.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else Modifier

                MediaGridItem(
                    image = img, 
                    index = index, 
                    isSelected = isSel, 
                    isSelectionMode = isSelectionMode, 
                    efficiencyMode = appSettings.efficiencyMode,
                    useModernUI = useModern,
                    onClick = { 
                        if (isSelectionMode) {
                            if (isExternalPicker && !allowMultiple) {
                                onMediaSelected(listOf(img.uri))
                            } else {
                                selectionViewModel.toggleMediaId(img.id)
                            }
                        } else {
                            onImageClick(GalleryKey.Viewer(index, isFavourites = true))
                        }
                    }, 
                    onLongClick = { 
                        if (selectedIds.isEmpty()) {
                            selectionViewModel.toggleMediaId(img.id)
                        }
                    },
                    modifier = itemModifier
                )
            }
        }
    }
}
