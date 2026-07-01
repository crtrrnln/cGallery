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
    data object Home : GalleryKey
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GalleryNavDisplay(
    backstack: List<GalleryKey>,
    onBack: () -> Unit,
    navigator: ThreePaneScaffoldNavigator<Any>
) {
    NavDisplay(
        backStack = backstack,
        onBack = onBack,
        entryProvider = { key ->
            when (key) {
                GalleryKey.Home -> NavEntry(key) {
                    GalleryAdaptiveLayout(navigator)
                }
            }
        }
    )
}
