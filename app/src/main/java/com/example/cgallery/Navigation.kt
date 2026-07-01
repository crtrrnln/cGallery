package com.example.cgallery

import android.net.Uri
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
sealed interface GalleryKey : NavKey {
    @Serializable
    data object Permission : GalleryKey

    @Serializable
    data object Gallery : GalleryKey

    @Serializable
    data class Viewer(val startIndex: Int) : GalleryKey
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GalleryNavDisplay(
    backstack: List<GalleryKey>,
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
                        HomeScreen(version = "v0.31")
                    }
                )
            ) {
                GalleryScreen(onImageClick = onNavigate)
            }

            entry<GalleryKey.Viewer>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                ViewerScreen(
                    startIndex = key.startIndex,
                    onBack = onBack
                )
            }
        }
    )
}
