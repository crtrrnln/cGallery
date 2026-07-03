package com.example.cgallery

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.cgallery.data.MediaItem
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
    data class AlbumDetail(val id: String) : GalleryKey

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
    albumResults: List<String>,
    onUpdateSearchQuery: (String) -> Unit,
    onAddToAlbum: (Long, Set<Long>) -> Unit,
    onReloadMedia: () -> Unit = {},
    onBack: () -> Unit,
    onNavigate: (GalleryKey) -> Unit,
    onToggleAlbumVisibility: (String) -> Unit = {},
    navigator: ThreePaneScaffoldNavigator<Any>
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<GalleryKey>()

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
                        HomeScreen(version = "v0.54")
                    }
                )
            ) {
                GalleryScreen(
                    images = mediaItems,
                    imagesMap = mediaItemsMap,
                    onAddToAlbum = onAddToAlbum,
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Albums>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.54")
                    }
                )
            ) {
                AlbumsScreen(
                    images = mediaItems,
                    onAlbumClick = { album ->
                        onNavigate(GalleryKey.AlbumDetail(album.name))
                    },
                    onGroupClick = { groupId ->
                        onNavigate(GalleryKey.GroupDetail(groupId))
                    },
                    onToggleAlbumVisibility = onToggleAlbumVisibility,
                    onSpecialAlbumClick = { type ->
                        when (type) {
                            com.example.cgallery.SpecialAlbumType.RECENTS -> onNavigate(GalleryKey.Gallery)
                            com.example.cgallery.SpecialAlbumType.FAVOURITES -> onNavigate(GalleryKey.Favourites)
                        }
                    }
                )
            }

            entry<GalleryKey.AlbumDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.54")
                    }
                )
            ) { key ->
                val albumImages = remember(key, mediaByBucket) {
                    mediaByBucket[key.id] ?: emptyList()
                }

                AlbumDetailScreen(
                    bucketName = key.id,
                    images = albumImages,
                    onImageClick = onNavigate,
                    onBack = onBack
                )
            }

            entry<GalleryKey.GroupDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.54")
                    }
                )
            ) { key ->
                GroupDetailScreen(
                    groupId = key.groupId,
                    images = mediaItems,
                    onAlbumClick = { albumName ->
                        onNavigate(GalleryKey.AlbumDetail(albumName))
                    },
                    onGroupClick = { childGroupId ->
                        onNavigate(GalleryKey.GroupDetail(childGroupId))
                    },
                    onBack = onBack
                )
            }

            entry<GalleryKey.Favourites>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.54")
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
                        HomeScreen(version = "v0.54")
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
