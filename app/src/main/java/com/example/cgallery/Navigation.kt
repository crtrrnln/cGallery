package com.example.cgallery

import android.net.Uri
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
import com.example.cgallery.data.AlbumWithMedia
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
    data object Favorites : GalleryKey

    @Serializable
    data object Search : GalleryKey

    @Serializable
    data class AlbumDetail(val id: String, val isVirtual: Boolean = false) : GalleryKey

    @Serializable
    data class Viewer(val startIndex: Int) : GalleryKey
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GalleryNavDisplay(
    backstack: List<GalleryKey>,
    mediaItems: List<MediaItem>,
    virtualAlbums: List<AlbumWithMedia>,
    onCreateAlbum: (String) -> Unit,
    onAddToAlbum: (Long, Set<Long>) -> Unit,
    onBack: () -> Unit,
    onNavigate: (GalleryKey) -> Unit,
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
                    }
                )
            }

            entry<GalleryKey.Gallery>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.41")
                    }
                )
            ) {
                GalleryScreen(
                    images = mediaItems,
                    virtualAlbums = virtualAlbums.map { it.album },
                    onAddToAlbum = onAddToAlbum,
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Albums>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.41")
                    }
                )
            ) {
                AlbumsScreen(
                    images = mediaItems,
                    virtualAlbums = virtualAlbums,
                    onCreateAlbum = onCreateAlbum,
                    onAlbumClick = { album ->
                        onNavigate(GalleryKey.AlbumDetail(album.name, isVirtual = false))
                    },
                    onVirtualAlbumClick = { albumWithMedia ->
                        onNavigate(GalleryKey.AlbumDetail(albumWithMedia.album.id.toString(), isVirtual = true))
                    }
                )
            }

            entry<GalleryKey.AlbumDetail>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.41")
                    }
                )
            ) { key ->
                val albumImages = remember(key, mediaItems, virtualAlbums) {
                    if (key.isVirtual) {
                        val albumId = key.id.toLongOrNull() ?: -1L
                        val mediaIds = virtualAlbums.find { it.album.id == albumId }?.mediaIds ?: emptyList()
                        mediaItems.filter { it.id in mediaIds }
                    } else {
                        mediaItems.filter { it.bucketName == key.id }
                    }
                }

                AlbumDetailScreen(
                    bucketName = if (key.isVirtual) {
                        virtualAlbums.find { it.album.id == (key.id.toLongOrNull() ?: -1L) }?.album?.name ?: "Album"
                    } else key.id,
                    images = albumImages,
                    onImageClick = onNavigate,
                    onBack = onBack
                )
            }

            entry<GalleryKey.Favorites>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.41")
                    }
                )
            ) {
                FavoritesScreen(
                    images = mediaItems,
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Search>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeScreen(version = "v0.41")
                    }
                )
            ) {
                SearchScreen(
                    images = mediaItems,
                    onImageClick = onNavigate
                )
            }

            entry<GalleryKey.Viewer>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                ViewerScreen(
                    startIndex = key.startIndex,
                    mediaItems = mediaItems,
                    onBack = onBack
                )
            }
        }
    )
}
