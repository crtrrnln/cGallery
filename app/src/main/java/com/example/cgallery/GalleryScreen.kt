package com.example.cgallery
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.cgallery.data.MediaItem
import com.example.cgallery.ui.MediaGridItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty() || isExternalPicker

    BackHandler(enabled = isSelectionMode) { selectedIds = emptySet() }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { selectedIds = emptySet(); onReloadMedia() }
    }

    Scaffold(topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isSelectionMode && !isExternalPicker) { Text("${selectedIds.size} selected") } 
                    else if (isExternalPicker) { Text(if (allowMultiple) "${selectedIds.size} selected" else "Select Item") } 
                    else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("cGallery")
                            Spacer(Modifier.width(4.dp))
                            Text("v0.72", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode && !isExternalPicker) { IconButton({ selectedIds = emptySet() }) { Icon(Icons.Default.Close, "clear") } } 
                    else if (isExternalPicker) { IconButton({ onMediaSelected(emptyList()) }) { Icon(Icons.Default.Close, "cancel") } }
                },
                actions = {
                    if (isExternalPicker && allowMultiple) {
                        IconButton({ onMediaSelected(selectedIds.mapNotNull { imagesMap[it]?.uri }) }, enabled = selectedIds.isNotEmpty()) { Icon(Icons.Default.Check, "ok") }
                    } else if (isSelectionMode && !isExternalPicker) {
                        IconButton({ onAddToAlbum(selectedIds, true); selectedIds = emptySet() }) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, "move") }
                        IconButton({ onAddToAlbum(selectedIds, false); selectedIds = emptySet() }) { Icon(Icons.Default.Add, "copy") }
                        IconButton({
                            val uris = selectedIds.mapNotNull { imagesMap[it]?.uri }
                            val i = Intent(Intent.ACTION_SEND_MULTIPLE).apply { putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris)); type = "*/*" }
                            context.startActivity(Intent.createChooser(i, "Share"))
                        }) { Icon(Icons.Default.Share, "share") }
                        IconButton({
                            val uris = selectedIds.mapNotNull { imagesMap[it]?.uri }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val p = MediaStore.createDeleteRequest(context.contentResolver, uris)
                                deleteLauncher.launch(IntentSenderRequest.Builder(p.intentSender).build())
                            } else {
                                scope.launch { uris.forEach { context.contentResolver.delete(it, null, null) }; selectedIds = emptySet() }
                            }
                        }) { Icon(Icons.Default.Delete, "delete") }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { p ->
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(2.dp)) {
            itemsIndexed(images, key = { _, i -> i.id }) { index, img ->
                val isSel = img.id in selectedIds
                MediaGridItem(image = img, index = index, isSelected = isSel, isSelectionMode = isSelectionMode,
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
