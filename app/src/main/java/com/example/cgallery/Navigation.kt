package com.example.cgallery

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.PhysicalAlbumEntity
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
    data object Inbox : GalleryKey

    @Serializable
    data class InboxProcessing(val startIndex: Int) : GalleryKey

    @Serializable
    data class InboxAlbumSelection(val inboxIds: Set<Long>, val isMove: Boolean) : GalleryKey

    @Serializable
    data object InboxSettings : GalleryKey

    @Serializable
    data class AlbumDetail(val id: String) : GalleryKey

    @Serializable
    data class CoverPicker(val bucketName: String?, val groupId: Long?) : GalleryKey

    @Serializable
    data class AlbumSelection(val mediaIds: Set<Long>, val isMove: Boolean) : GalleryKey

    @Serializable
    data class GroupDetail(val groupId: Long) : GalleryKey

    @Serializable
    data class Viewer(val startIndex: Int) : GalleryKey
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GalleryNavDisplay(
    backstack: List<GalleryKey>,
    mediaItems: List<MediaItem>,
    mediaItemsMap: Map<Long, MediaItem>,
    mediaByBucket: Map<String, List<MediaItem>>,
    favoriteMedia: List<MediaItem>,
    searchQuery: String,
    searchResults: List<MediaItem>,
    albumResults: List<Pair<String, String>>,
    onUpdateSearchQuery: (String) -> Unit,
    onAddToAlbum: (List<String>, Set<Long>) -> Unit,
    onMoveToAlbum: (List<String>, Set<Long>) -> Unit,
    onReloadMedia: () -> Unit = {},
    onBack: () -> Unit,
    onNavigate: (GalleryKey) -> Unit,
    onToggleAlbumVisibility: (String) -> Unit = {},
    navigator: ThreePaneScaffoldNavigator<Any>
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<GalleryKey>()

    val inboxViewModel: InboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    NavDisplay(
        backStack = backstack,
        onBack = onBack,
        sceneStrategy = listDetailStrategy,
        entryProvider = entryProvider {
            entry<GalleryKey.Permission> {
                PermissionScreen(
                    onPermissionGranted = {
                        onNavigate(GalleryKey.Gallery)
                    },
                    onPermissionRequest = {
                        onReloadMedia()
                    }
                )
            }

            entry<GalleryKey.Gallery>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) {
                GalleryScreen(
                    images = mediaItems,
                    imagesMap = mediaItemsMap,
                    onAddToAlbum = { ids, isMove ->
                        onNavigate(GalleryKey.AlbumSelection(ids, isMove))
                    },
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Albums>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) {
                AlbumsScreen(
                    images = mediaItems,
                    mediaByBucket = mediaByBucket,
                    onAlbumClick = { album ->
                        onNavigate(GalleryKey.AlbumDetail(album.name))
                    },
                    onGroupClick = { groupId ->
                        onNavigate(GalleryKey.GroupDetail(groupId))
                    },
                    onToggleAlbumVisibility = onToggleAlbumVisibility,
                    onSpecialAlbumClick = { type ->
                        when (type) {
                            SpecialAlbumType.RECENTS -> onNavigate(GalleryKey.Gallery)
                            SpecialAlbumType.FAVOURITES -> onNavigate(GalleryKey.Favourites)
                        }
                    },
                    onInboxClick = { onNavigate(GalleryKey.Inbox) }
                )
            }

            entry<GalleryKey.Inbox>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) {
                InboxScreen(
                    viewModel = inboxViewModel,
                    onItemClick = { index ->
                        onNavigate(GalleryKey.InboxProcessing(index))
                    },
                    onOrganise = { ids, isMove ->
                        onNavigate(GalleryKey.InboxAlbumSelection(ids, isMove))
                    },
                    onSettingsClick = {
                        onNavigate(GalleryKey.InboxSettings)
                    }
                )
            }

            entry<GalleryKey.InboxProcessing>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) { key ->
                val items by inboxViewModel.pendingItems.collectAsState()
                if (items.isNotEmpty()) {
                    InboxProcessingScreen(
                        startIndex = key.startIndex,
                        viewModel = inboxViewModel,
                        onOrganise = { ids, isMove ->
                            onNavigate(GalleryKey.InboxAlbumSelection(ids, isMove))
                        },
                        onBack = onBack
                    )
                } else {
                    onBack()
                }
            }

            entry<GalleryKey.InboxSettings>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) {
                InboxSettingsScreen(
                    viewModel = inboxViewModel,
                    onBack = onBack
                )
            }

            entry<GalleryKey.InboxAlbumSelection>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) { key ->
                AlbumsScreen(
                    images = mediaItems,
                    mediaByBucket = mediaByBucket,
                    onAlbumClick = {},
                    selectionMode = true,
                    onConfirmSelection = { paths ->
                        if (paths.isNotEmpty()) {
                            inboxViewModel.processItems(key.inboxIds, paths, key.isMove)
                        }
                        onBack()
                    }
                )
            }

            entry<GalleryKey.AlbumSelection>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) { key ->
                AlbumsScreen(
                    images = mediaItems,
                    mediaByBucket = mediaByBucket,
                    onAlbumClick = {},
                    selectionMode = true,
                    onConfirmSelection = { paths ->
                        if (paths.isNotEmpty()) {
                            if (key.isMove) {
                                onMoveToAlbum(paths, key.mediaIds)
                            } else {
                                onAddToAlbum(paths, key.mediaIds)
                            }
                        }
                        onBack()
                    }
                )
            }

            entry<GalleryKey.AlbumDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) { key ->
                val albumImages = remember(key, mediaByBucket) {
                    mediaByBucket[key.id] ?: emptyList()
                }

                AlbumDetailScreen(
                    bucketName = key.id,
                    images = albumImages,
                    onAddToAlbum = { ids, isMove ->
                        onNavigate(GalleryKey.AlbumSelection(ids, isMove))
                    },
                    onChangeCover = {
                        onNavigate(GalleryKey.CoverPicker(bucketName = key.id, groupId = null))
                    },
                    onImageClick = onNavigate,
                    onBack = onBack
                )
            }

            entry<GalleryKey.GroupDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) { key ->
                GroupDetailScreen(
                    groupId = key.groupId,
                    images = mediaItems,
                    mediaByBucket = mediaByBucket,
                    onAlbumClick = { albumName ->
                        onNavigate(GalleryKey.AlbumDetail(albumName))
                    },
                    onGroupClick = { childGroupId ->
                        onNavigate(GalleryKey.GroupDetail(childGroupId))
                    },
                    onChangeCover = {
                        onNavigate(GalleryKey.CoverPicker(bucketName = null, groupId = key.groupId))
                    },
                    onBack = onBack
                )
            }

            entry<GalleryKey.CoverPicker>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                val albumImages = remember(key, mediaByBucket, mediaItems) {
                    if (key.bucketName != null) {
                        mediaByBucket[key.bucketName] ?: emptyList()
                    } else {
                        mediaItems
                    }
                }

                CoverPickerScreen(
                    images = albumImages,
                    onCoverSelected = { uri, crop ->
                        if (key.bucketName != null) {
                            inboxViewModel.updateAlbumCover(key.bucketName, uri, crop)
                        } else if (key.groupId != null) {
                            inboxViewModel.updateGroupCover(key.groupId, uri, crop)
                        }
                        onBack()
                    },
                    onBack = onBack
                )
            }

            entry<GalleryKey.Favourites>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) {
                FavouritesScreen(
                    favoriteImages = favoriteMedia,
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Search>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.63")
                    }
                )
            ) {
                SearchScreen(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    albumResults = albumResults,
                    onUpdateSearchQuery = onUpdateSearchQuery,
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Viewer>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                // Check if we're coming from a list screen by looking at the backstack
                val previousKey = backstack.getOrNull(backstack.size - 2)
                val filteredMedia = when (previousKey) {
                    is GalleryKey.AlbumDetail -> mediaByBucket[previousKey.id]
                    is GalleryKey.Favourites -> favoriteMedia
                    is GalleryKey.Search -> searchResults
                    else -> null
                }

                ViewerScreen(
                    startIndex = key.startIndex,
                    mediaItems = mediaItems,
                    onBack = onBack,
                    filteredMedia = filteredMedia
                )
            }
        }
    )
}
