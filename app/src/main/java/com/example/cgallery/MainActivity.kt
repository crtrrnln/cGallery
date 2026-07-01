package com.example.cgallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.cgallery.ui.theme.CGalleryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CGalleryTheme {
                val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val isPermissionGranted = ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED

                var backstack by remember {
                    mutableStateOf(
                        if (isPermissionGranted) listOf(GalleryKey.Gallery)
                        else listOf(GalleryKey.Permission)
                    )
                }
                val scope = rememberCoroutineScope()
                val windowAdaptiveInfo = currentWindowAdaptiveInfo()
                val isSinglePane = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

                val showBottomBar = isPermissionGranted && 
                    (!isSinglePane || backstack.lastOrNull() !is GalleryKey.Viewer)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val currentBase = backstack.firstOrNull { 
                                    it is GalleryKey.Gallery || it is GalleryKey.Albums || 
                                    it is GalleryKey.Favorites || it is GalleryKey.Search 
                                } ?: GalleryKey.Gallery

                                NavigationBarItem(
                                    selected = currentBase is GalleryKey.Gallery,
                                    onClick = { backstack = listOf(GalleryKey.Gallery) },
                                    icon = { Icon(Icons.Rounded.PhotoLibrary, "Gallery") },
                                    label = { Text("Gallery") }
                                )
                                NavigationBarItem(
                                    selected = currentBase is GalleryKey.Albums,
                                    onClick = { backstack = listOf(GalleryKey.Albums) },
                                    icon = { Icon(Icons.Rounded.Collections, "Albums") },
                                    label = { Text("Albums") }
                                )
                                NavigationBarItem(
                                    selected = currentBase is GalleryKey.Favorites,
                                    onClick = { backstack = listOf(GalleryKey.Favorites) },
                                    icon = { Icon(Icons.Rounded.Favorite, "Favorites") },
                                    label = { Text("Favorites") }
                                )
                                NavigationBarItem(
                                    selected = currentBase is GalleryKey.Search,
                                    onClick = { backstack = listOf(GalleryKey.Search) },
                                    icon = { Icon(Icons.Rounded.Search, "Search") },
                                    label = { Text("Search") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        GalleryNavDisplay(
                            backstack = backstack,
                            onBack = {
                                if (backstack.size > 1) {
                                    backstack = backstack.dropLast(1)
                                } else {
                                    finish()
                                }
                            },
                            onNavigate = { newKey ->
                                backstack = when (newKey) {
                                    is GalleryKey.Gallery, is GalleryKey.Albums, 
                                    is GalleryKey.Favorites, is GalleryKey.Search -> listOf(newKey)
                                    is GalleryKey.AlbumDetail -> {
                                        // Allow pushing album detail onto albums list
                                        backstack + newKey
                                    }
                                    is GalleryKey.Viewer -> {
                                        if (backstack.lastOrNull() is GalleryKey.Viewer) {
                                            backstack.dropLast(1) + newKey
                                        } else {
                                            backstack + newKey
                                        }
                                    }
                                    else -> backstack + newKey
                                }
                            },
                            navigator = navigator
                        )
                    }
                }

                // Handle back button for the adaptive scaffold
                BackHandler(enabled = navigator.canNavigateBack()) {
                    scope.launch {
                        navigator.navigateBack()
                    }
                }
            }
        }
    }
}
