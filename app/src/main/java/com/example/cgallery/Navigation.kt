package com.example.cgallery

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
sealed interface GalleryKey : NavKey {
    @Serializable
    data object Permission : GalleryKey

    @Serializable
    data object Gallery : GalleryKey
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GalleryNavDisplay(
    backstack: List<GalleryKey>,
    onBack: () -> Unit,
    onNavigate: (GalleryKey) -> Unit,
    navigator: ThreePaneScaffoldNavigator<Any>
) {
    NavDisplay(
        backStack = backstack,
        onBack = onBack,
        entryProvider = { key ->
            when (key) {
                GalleryKey.Permission -> NavEntry(key) {
                    PermissionScreen(
                        onPermissionGranted = {
                            onNavigate(GalleryKey.Gallery)
                        }
                    )
                }

                GalleryKey.Gallery -> NavEntry(key) {
                    GalleryAdaptiveLayout(navigator)
                }
            }
        }
    )
}
