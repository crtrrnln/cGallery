package com.example.cgallery

import android.net.Uri
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.cgallery.data.*
import kotlinx.serialization.Serializable

@Serializable
sealed interface GalleryKey : NavKey {
    @Serializable
    data object Permission : GalleryKey
    @Serializable
    data object Gallery : GalleryKey
    @Serializable
    data object Albums : GalleryKey
    @Serializable
    data object Favourites : GalleryKey
    @Serializable
    data object Search : GalleryKey
    @Serializable
    data class Inbox(val isEnforcementSession: Boolean = false) : GalleryKey
    @Serializable
    data class InboxProcessing(val startIndex: Int, val isEnforcementSession: Boolean = false) : GalleryKey
    @Serializable
    data class InboxAlbumSelection(val inboxIds: Set<Long>, val isMove: Boolean) : GalleryKey
    @Serializable
    data object InboxSettings : GalleryKey
    @Serializable
    data object AppSettings : GalleryKey
    @Serializable
    data object StorageDetail : GalleryKey
    @Serializable
    data object Diagnostics : GalleryKey
    @Serializable
    data class AlbumDetail(val id: String) : GalleryKey
    @Serializable
    data class CoverPicker(val bucketName: String?, val groupId: Long?) : GalleryKey
    @Serializable
    data class AlbumSelection(val mediaIds: Set<Long>, val isMove: Boolean) : GalleryKey
    @Serializable
    data class GroupDetail(
        val groupId: Long,
        val selectionMode: Boolean = false,
        val selectionMediaIds: Set<Long> = emptySet(),
        val selectionIsMove: Boolean = false,
        val isInbox: Boolean = false
    ) : GalleryKey
    @Serializable
    data class Viewer(val startIndex: Int) : GalleryKey
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GalleryNavDisplay(
    backstack: List<GalleryKey>,
    onAddToAlbum: (List<String>, Set<Long>) -> Unit,
    onMoveToAlbum: (List<String>, Set<Long>) -> Unit,
    onCreateFolder: (String, Long?) -> Unit = { _, _ -> },
    onReloadMedia: () -> Unit = {},
    onBack: () -> Unit,
    onClearSelectionBackstack: () -> Unit = {},
    onNavigate: (GalleryKey) -> Unit,
    onToggleAlbumVisibility: (String) -> Unit = {},
    onMediaSelected: (List<Uri>) -> Unit = {},
    isExternalPicker: Boolean = false,
    pickerAllowMultiple: Boolean = false,
    navigator: ThreePaneScaffoldNavigator<Any>
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<GalleryKey>()

    val vm: MediaStoreViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val inboxViewModel: InboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val selectionViewModel: SelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val selectedPaths by selectionViewModel.selectedPaths.collectAsState()

    NavDisplay(
        backStack = backstack,
        onBack = onBack,
        sceneStrategy = listDetailStrategy,
        entryProvider = entryProvider {
            entry<GalleryKey.Permission> {
                PermissionScreen(
                    onPermissionGranted = { onNavigate(GalleryKey.Gallery) },
                    onPermissionRequest = { onReloadMedia() }
                )
            }

            entry<GalleryKey.Gallery>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                val items by vm.mediaItems.collectAsState()
                val itemsMap by vm.mediaItemsMap.collectAsState()
                GalleryScreen(
                    images = items,
                    imagesMap = itemsMap,
                    onAddToAlbum = { ids, isMove ->
                        selectionViewModel.clearSelection()
                        onNavigate(GalleryKey.AlbumSelection(ids, isMove))
                    },
                    onImageClick = onNavigate,
                    onMediaSelected = onMediaSelected,
                    onReloadMedia = onReloadMedia,
                    isExternalPicker = isExternalPicker,
                    allowMultiple = pickerAllowMultiple
                )
            }

            entry<GalleryKey.Albums>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                val items by vm.mediaItems.collectAsState()
                val byBuck by vm.mediaByBucket.collectAsState()
                AlbumsScreen(
                    images = items,
                    mediaByBucket = byBuck,
                    onAlbumClick = { album -> onNavigate(GalleryKey.AlbumDetail(album.name)) },
                    onGroupClick = { groupId -> onNavigate(GalleryKey.GroupDetail(groupId)) },
                    onToggleAlbumVisibility = onToggleAlbumVisibility,
                    onCreateFolder = { onCreateFolder(it, null) },
                    onSpecialAlbumClick = { type ->
                        when (type) {
                            SpecialAlbumType.RECENTS -> onNavigate(GalleryKey.Gallery)
                            SpecialAlbumType.FAVOURITES -> onNavigate(GalleryKey.Favourites)
                        }
                    },
                    onInboxClick = { onNavigate(GalleryKey.Inbox(isEnforcementSession = false)) },
                    onSettingsClick = { onNavigate(GalleryKey.AppSettings) }
                )
            }

            entry<GalleryKey.Inbox>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) { key ->
                val needsRefresh by inboxViewModel.needsRefresh.collectAsState()
                InboxScreen(
                    viewModel = inboxViewModel,
                    isEnforcementSession = key.isEnforcementSession,
                    onItemClick = { index ->
                        onNavigate(GalleryKey.InboxProcessing(index, isEnforcementSession = key.isEnforcementSession))
                    },
                    onOrganise = { ids, isMove ->
                        selectionViewModel.clearSelection()
                        onNavigate(GalleryKey.InboxAlbumSelection(ids, isMove))
                    },
                    onSettingsClick = { onNavigate(GalleryKey.InboxSettings) },
                    onDiagnosticsClick = { onNavigate(GalleryKey.Diagnostics) },
                    onBack = {
                        if (needsRefresh) {
                            onReloadMedia()
                            inboxViewModel.clearRefreshFlag()
                        }
                        onBack()
                    }
                )
            }

            entry<GalleryKey.InboxProcessing>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) { key ->
                val items by inboxViewModel.pendingItems.collectAsState()
                if (items.isNotEmpty()) {
                    InboxProcessingScreen(
                        startIndex = key.startIndex,
                        viewModel = inboxViewModel,
                        isEnforcementSession = key.isEnforcementSession,
                        onOrganise = { ids, isMove ->
                            selectionViewModel.clearSelection()
                            onNavigate(GalleryKey.InboxAlbumSelection(ids, isMove))
                        },
                        onBack = onBack
                    )
                } else { onBack() }
            }

            entry<GalleryKey.InboxSettings>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                InboxSettingsScreen(viewModel = inboxViewModel, onBack = onBack)
            }

            entry<GalleryKey.AppSettings>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                com.example.cgallery.ui.SettingsScreen(
                    viewModel = settingsViewModel, 
                    onBack = onBack,
                    onNavigateToStorage = { onNavigate(GalleryKey.StorageDetail) }
                )
            }

            entry<GalleryKey.StorageDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                com.example.cgallery.ui.StorageDetailScreen(
                    viewModel = settingsViewModel,
                    onBack = onBack
                )
            }

            entry<GalleryKey.Diagnostics>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                DiagnosticsScreen(inboxViewModel = inboxViewModel, onBack = onBack)
            }

            entry<GalleryKey.InboxAlbumSelection>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) { key ->
                val items by vm.mediaItems.collectAsState()
                val byBuck by vm.mediaByBucket.collectAsState()
                AlbumsScreen(
                    images = items,
                    mediaByBucket = byBuck,
                    onAlbumClick = {},
                    onGroupClick = { groupId ->
                        onNavigate(GalleryKey.GroupDetail(groupId, selectionMode = true, selectionMediaIds = key.inboxIds, selectionIsMove = key.isMove, isInbox = true))
                    },
                    selectionMode = true,
                    externalSelectedAlbums = selectedPaths,
                    onToggleAlbumSelection = { selectionViewModel.togglePath(it) },
                    onCreateFolder = { onCreateFolder(it, null) },
                    onSettingsClick = { onNavigate(GalleryKey.AppSettings) },
                    onConfirmSelection = { paths ->
                        if (paths.isNotEmpty()) {
                            inboxViewModel.processItems(key.inboxIds, paths, key.isMove)
                            onReloadMedia()
                            onClearSelectionBackstack()
                        } else { onBack() }
                    }
                )
            }

            entry<GalleryKey.AlbumSelection>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) { key ->
                val items by vm.mediaItems.collectAsState()
                val byBuck by vm.mediaByBucket.collectAsState()
                AlbumsScreen(
                    images = items,
                    mediaByBucket = byBuck,
                    onAlbumClick = {},
                    onGroupClick = { groupId ->
                        onNavigate(GalleryKey.GroupDetail(groupId, selectionMode = true, selectionMediaIds = key.mediaIds, selectionIsMove = key.isMove, isInbox = false))
                    },
                    selectionMode = true,
                    externalSelectedAlbums = selectedPaths,
                    onToggleAlbumSelection = { selectionViewModel.togglePath(it) },
                    onCreateFolder = { onCreateFolder(it, null) },
                    onSettingsClick = { onNavigate(GalleryKey.AppSettings) },
                    onConfirmSelection = { paths ->
                        if (paths.isNotEmpty()) {
                            if (key.isMove) onMoveToAlbum(paths, key.mediaIds) else onAddToAlbum(paths, key.mediaIds)
                            onClearSelectionBackstack()
                        } else { onBack() }
                    }
                )
            }

            entry<GalleryKey.AlbumDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) { key ->
                val byBuck by vm.mediaByBucket.collectAsState()
                val albumImages = remember(key, byBuck) { byBuck[key.id] ?: emptyList() }
                AlbumDetailScreen(
                    bucketName = key.id,
                    images = albumImages,
                    onAddToAlbum = { ids, isMove ->
                        selectionViewModel.clearSelection()
                        onNavigate(GalleryKey.AlbumSelection(ids, isMove))
                    },
                    onChangeCover = { onNavigate(GalleryKey.CoverPicker(bucketName = key.id, groupId = null)) },
                    onImageClick = onNavigate,
                    onBack = onBack,
                    onMediaSelected = onMediaSelected,
                    isExternalPicker = isExternalPicker,
                    allowMultiple = pickerAllowMultiple
                )
            }

            entry<GalleryKey.GroupDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) { key ->
                val items by vm.mediaItems.collectAsState()
                GroupDetailScreen(
                    groupId = key.groupId,
                    images = items,
                    onAlbumClick = { albumName -> onNavigate(GalleryKey.AlbumDetail(albumName)) },
                    onGroupClick = { childGroupId ->
                        onNavigate(GalleryKey.GroupDetail(childGroupId, selectionMode = key.selectionMode, selectionMediaIds = key.selectionMediaIds, selectionIsMove = key.selectionIsMove, isInbox = key.isInbox))
                    },
                    onChangeCover = { onNavigate(GalleryKey.CoverPicker(bucketName = null, groupId = key.groupId)) },
                    onCreateFolder = { onCreateFolder(it, key.groupId) },
                    selectionMode = key.selectionMode,
                    selectedAlbums = selectedPaths,
                    onToggleAlbumSelection = { selectionViewModel.togglePath(it) },
                    onConfirmSelection = { paths ->
                        if (paths.isNotEmpty()) {
                            if (key.isInbox) {
                                inboxViewModel.processItems(key.selectionMediaIds, paths, key.selectionIsMove)
                                onReloadMedia()
                            } else {
                                if (key.selectionIsMove) onMoveToAlbum(paths, key.selectionMediaIds) else onAddToAlbum(paths, key.selectionMediaIds)
                            }
                            onClearSelectionBackstack()
                        } else { onBack() }
                    },
                    onBack = onBack,
                    onMediaSelected = onMediaSelected,
                    isExternalPicker = isExternalPicker,
                    pickerAllowMultiple = pickerAllowMultiple
                )
            }

            entry<GalleryKey.CoverPicker>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                val items by vm.mediaItems.collectAsState()
                val byBuck by vm.mediaByBucket.collectAsState()
                val albumImages = remember(key, byBuck, items) {
                    if (key.bucketName != null) byBuck[key.bucketName] ?: emptyList() else items
                }
                CoverPickerScreen(
                    images = albumImages,
                    onCoverSelected = { uri, crop ->
                        if (key.bucketName != null) inboxViewModel.updateAlbumCover(key.bucketName, uri, crop)
                        else if (key.groupId != null) inboxViewModel.updateGroupCover(key.groupId, uri, crop)
                        onBack()
                    },
                    onBack = onBack
                )
            }

            entry<GalleryKey.Favourites>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                val favs by vm.favouriteMedia.collectAsState()
                FavouritesScreen(
                    favouriteImages = favs,
                    onImageClick = onNavigate,
                    onMediaSelected = onMediaSelected,
                    isExternalPicker = isExternalPicker,
                    allowMultiple = pickerAllowMultiple
                )
            }

            entry<GalleryKey.Search>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { HomeScreen(version = "v0.8") }
                )
            ) {
                val query by vm.searchQuery.collectAsState()
                val sRes by vm.searchResults.collectAsState()
                val aRes by vm.albumResults.collectAsState()
                SearchScreen(
                    searchQuery = query,
                    searchResults = sRes,
                    albumResults = aRes,
                    onUpdateSearchQuery = { vm.updateSearchQuery(it) },
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Viewer>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                val items by vm.mediaItems.collectAsState()
                val favs by vm.favouriteMedia.collectAsState()
                val byBuck by vm.mediaByBucket.collectAsState()
                
                val previousKey = backstack.getOrNull(backstack.size - 2)
                val filteredMedia = when (previousKey) {
                    is GalleryKey.AlbumDetail -> byBuck[previousKey.id]
                    is GalleryKey.Favourites -> favs
                    else -> null
                }
                ViewerScreen(
                    startIndex = key.startIndex,
                    mediaItems = items,
                    onBack = onBack,
                    onReloadMedia = onReloadMedia,
                    filteredMedia = filteredMedia
                )
            }
        }
    )
}
