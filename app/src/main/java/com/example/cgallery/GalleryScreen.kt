package com.example.cgallery
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.*
import com.example.cgallery.ui.MediaGridItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    images: List<MediaItem>,
    imagesMap: Map<Long, MediaItem>,
    onAddToAlbum: (Set<Long>, Boolean) -> Unit = { _, _ -> },
    onImageClick: (GalleryKey) -> Unit,
    onMediaSelected: (List<android.net.Uri>) -> Unit = {},
    onReloadMedia: () -> Unit = {},
    isExternalPicker: Boolean = false,
    allowMultiple: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectionViewModel: SelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val vm: MediaStoreViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val selectedIds by selectionViewModel.selectedMediaIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty() || isExternalPicker
    BackHandler(enabled = isSelectionMode) { selectionViewModel.clearSelection() }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { selectionViewModel.clearSelection(); onReloadMedia() }
    }
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val starredIds by vm.combinedStarredIds.collectAsState()
    val useModern = appSettings.useModernUI

    Scaffold(topBar = {
            val titleText = if (isSelectionMode && !isExternalPicker) "${selectedIds.size} selected"
            else if (isExternalPicker) (if (allowMultiple) "${selectedIds.size} selected" else "Select Item")
            else "cGallery"

            if (useModern && !isSelectionMode) {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(titleText, style = MaterialTheme.typography.headlineMedium)
                            Text("v0.9/1.0rc3.2UI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSelectionMode || isExternalPicker) Text(titleText)
                        else Row(verticalAlignment = Alignment.Bottom) { Text("cGallery"); Spacer(Modifier.width(4.dp)); Text(if (useModern) "v0.9/1.0rc3.2UI" else "v0.9/1.0rc3.2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)) }
                    },
                    navigationIcon = {
                        if (isSelectionMode && !isExternalPicker) {
                            IconButton({ selectionViewModel.clearSelection() }) { Icon(Icons.Default.Close, "clear") }
                        } else if (isExternalPicker) {
                            IconButton({ onMediaSelected(emptyList()) }) { Icon(Icons.Default.Close, "cancel") }
                        }
                    },
                    actions = {
                        if (isExternalPicker && allowMultiple) {
                            IconButton(
                                { onMediaSelected(selectedIds.mapNotNull { imagesMap[it]?.uri }) },
                                enabled = selectedIds.isNotEmpty()
                            ) { Icon(Icons.Default.Check, "ok") }
                        } else if (isSelectionMode && !isExternalPicker) {
                            IconButton({ 
                                onAddToAlbum(selectedIds, true)
                                selectionViewModel.clearSelection()
                            }) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, "move") }
                            IconButton({ 
                                onAddToAlbum(selectedIds, false)
                                selectionViewModel.clearSelection()
                            }) { Icon(Icons.Default.Add, "copy") }
                            IconButton({
                                val uris = selectedIds.mapNotNull { imagesMap[it]?.uri }
                                val i = Intent(Intent.ACTION_SEND_MULTIPLE).apply { putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris)); type = "*/*" }
                                context.startActivity(Intent.createChooser(i, "Share"))
                            }) { Icon(Icons.Default.Share, "share") }
                            IconButton({
                                val items = selectedIds.mapNotNull { imagesMap[it] }
                                vm.moveToTrash(items)
                                selectionViewModel.clearSelection()
                            }) { Icon(Icons.Default.Delete, "delete") }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { p ->
        val columns = if (appSettings.gridDensity == GridDensity.COMPACT) 5 else 3
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(if (useModern) 4.dp else 2.dp)) {
            itemsIndexed(images, key = { _, i -> i.id }) { index, img ->
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
                    isStarred = img.id in starredIds,
                    onClick = {
                        if (isSelectionMode) {
                            if (isExternalPicker && !allowMultiple) {
                                onMediaSelected(listOf(img.uri))
                            } else {
                                selectionViewModel.toggleMediaId(img.id)
                            }
                        } else {
                            onImageClick(GalleryKey.Viewer(index))
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
