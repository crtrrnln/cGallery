package com.example.cgallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cgallery.ui.theme.CGalleryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CGalleryTheme(dynamicColor = false) {
                val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
                val permissions = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                val isPermissionGranted = remember(permissions) {
                    val isAllFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Environment.isExternalStorageManager()
                    } else {
                        true
                    }
                    permissions.all { permission ->
                        ContextCompat.checkSelfPermission(
                            this,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    } && isAllFilesGranted
                }

                val mediaStoreViewModel: MediaStoreViewModel = viewModel()
                val mediaItems by mediaStoreViewModel.mediaItems.collectAsState()
                val mediaItemsMap by mediaStoreViewModel.mediaItemsMap.collectAsState()
                val mediaByBucket by mediaStoreViewModel.mediaByBucket.collectAsState()
                val favoriteMedia by mediaStoreViewModel.favoriteMedia.collectAsState()
                val searchQuery by mediaStoreViewModel.searchQuery.collectAsState()
                val searchResults by mediaStoreViewModel.searchResults.collectAsState()
                val albumResults by mediaStoreViewModel.albumResults.collectAsState()
                val isLoading by mediaStoreViewModel.isLoading.collectAsState()

                var backstack by remember {
                    mutableStateOf(
                        if (isPermissionGranted) listOf(GalleryKey.Gallery)
                        else listOf(GalleryKey.Permission)
                    )
                }
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    mediaStoreViewModel.operationResult.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                val windowAdaptiveInfo = currentWindowAdaptiveInfo()
                val isSinglePane = remember(windowAdaptiveInfo) {
                    windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
                }

                val currentBackstack by rememberUpdatedState(backstack)
                val onNavigate = remember {
                    { newKey: GalleryKey ->
                        backstack = when (newKey) {
                            is GalleryKey.Albums, is GalleryKey.Search -> listOf(newKey)
                            is GalleryKey.Favourites -> {
                                if (currentBackstack.lastOrNull() is GalleryKey.Albums) {
                                    currentBackstack + newKey
                                } else {
                                    listOf(newKey)
                                }
                            }
                            is GalleryKey.Gallery -> {
                                if (currentBackstack.lastOrNull() is GalleryKey.Favourites) {
                                    currentBackstack + newKey
                                } else {
                                    listOf(newKey)
                                }
                            }
                            is GalleryKey.AlbumDetail -> {
                                currentBackstack + newKey
                            }
                            is GalleryKey.Viewer -> {
                                if (currentBackstack.lastOrNull() is GalleryKey.Viewer) {
                                    currentBackstack.dropLast(1) + newKey
                                } else {
                                    currentBackstack + newKey
                                }
                            }
                            else -> currentBackstack + newKey
                        }
                    }
                }

                val onBack = remember {
                    {
                        if (currentBackstack.size > 1) {
                            backstack = currentBackstack.dropLast(1)
                        } else {
                            finish()
                        }
                    }
                }

                val showBottomBar = isPermissionGranted && 
                    (!isSinglePane || backstack.lastOrNull() !is GalleryKey.Viewer)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val currentBase = backstack.firstOrNull {
                                    it is GalleryKey.Gallery || it is GalleryKey.Albums ||
                                    it is GalleryKey.Favourites || it is GalleryKey.Search
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
                                    selected = currentBase is GalleryKey.Favourites,
                                    onClick = { backstack = listOf(GalleryKey.Favourites) },
                                    icon = { Icon(Icons.Rounded.Favorite, "Favourites") },
                                    label = { Text("Favourites") }
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
                            mediaItems = mediaItems,
                            mediaItemsMap = mediaItemsMap,
                            mediaByBucket = mediaByBucket,
                            favoriteMedia = favoriteMedia,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            albumResults = albumResults,
                            onUpdateSearchQuery = { mediaStoreViewModel.updateSearchQuery(it) },
                            onAddToAlbum = { albumPaths, mediaIds ->
                                mediaStoreViewModel.copyMediaToAlbum(albumPaths, mediaIds)
                            },
                            onMoveToAlbum = { albumPaths, mediaIds ->
                                mediaStoreViewModel.moveMediaToAlbum(albumPaths, mediaIds)
                            },
                            onReloadMedia = { mediaStoreViewModel.loadMedia() },
                            onBack = onBack,
                            onNavigate = onNavigate,
                            onToggleAlbumVisibility = { bucketName ->
                                mediaStoreViewModel.toggleAlbumVisibility(bucketName)
                            },
                            navigator = navigator
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
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
