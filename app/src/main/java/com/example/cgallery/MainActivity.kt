package com.example.cgallery

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cgallery.data.*
import com.example.cgallery.ui.theme.CGalleryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var _backstackState = mutableStateOf<List<GalleryKey>>(listOf(GalleryKey.Gallery))

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") {
            _backstackState.value = listOf(GalleryKey.Inbox(isEnforcementSession = true))
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        InboxDetectionService.start(this)

        if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") {
            _backstackState.value = listOf(GalleryKey.Inbox(isEnforcementSession = true))
        }

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

                val isExternalPicker = remember {
                    intent.action == Intent.ACTION_GET_CONTENT || 
                    intent.action == Intent.ACTION_PICK ||
                    intent.action == "android.provider.action.PICK_IMAGES"
                }
                val isExternalView = remember {
                    intent.action == Intent.ACTION_VIEW
                }
                val pickerAllowMultiple = remember {
                    intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                }

                var showStartupAnimation by remember { mutableStateOf(!isExternalPicker && !isExternalView) }

                var backstack by remember { _backstackState }

                LaunchedEffect(Unit) {
                    if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") {
                        backstack = listOf(GalleryKey.Inbox(isEnforcementSession = true))
                    } else if (!isPermissionGranted) {
                        backstack = listOf(GalleryKey.Permission)
                    }
                }

                LaunchedEffect(isPermissionGranted, mediaItems) {
                    if (isPermissionGranted && isExternalView && mediaItems.isNotEmpty()) {
                        val viewUri = intent.data
                        if (viewUri != null) {
                            val index = mediaItems.indexOfFirst { it.uri == viewUri || it.fullPath == viewUri.path }
                            if (index != -1) {
                                backstack = listOf(GalleryKey.Gallery, GalleryKey.Viewer(index))
                            }
                        }
                    }
                }
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
                val onNavigate: (GalleryKey) -> Unit = { newKey ->
                    val updated = when (newKey) {
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
                        is GalleryKey.AlbumDetail, is GalleryKey.GroupDetail -> {
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
                    backstack = updated
                }

                val onBack: () -> Unit = {
                    if (backstack.size > 1) {
                        backstack = backstack.dropLast(1)
                    } else {
                        finish()
                    }
                }

                val onClearSelectionBackstack: () -> Unit = {
                    val firstSelectionIndex = backstack.indexOfFirst { 
                        it is GalleryKey.AlbumSelection || it is GalleryKey.InboxAlbumSelection 
                    }
                    if (firstSelectionIndex != -1) {
                        backstack = backstack.take(firstSelectionIndex)
                    } else if (backstack.size > 1) {
                        backstack = backstack.dropLast(1)
                    }
                }

                val showBottomBar = isPermissionGranted && 
                    (!isSinglePane || backstack.lastOrNull() !is GalleryKey.Viewer) &&
                    !isExternalPicker &&
                    backstack.none { it is GalleryKey.Inbox && it.isEnforcementSession }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar && !showStartupAnimation) {
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
                    val revealAlpha by animateFloatAsState(
                        targetValue = if (showStartupAnimation) 0f else 1f,
                        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
                        label = "galleryFadeIn"
                    )

                    val revealScale by animateFloatAsState(
                        targetValue = if (showStartupAnimation) 0.95f else 1f,
                        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
                        label = "galleryScaleIn"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .graphicsLayer {
                                alpha = revealAlpha
                                scaleX = revealScale
                                scaleY = revealScale
                            }
                    ) {
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
                            onCreateFolder = { folderName, groupId ->
                                mediaStoreViewModel.createFolder(folderName, groupId)
                            },
                            onClearSelectionBackstack = onClearSelectionBackstack,
                            isExternalPicker = isExternalPicker,
                            pickerAllowMultiple = pickerAllowMultiple,
                            onMediaSelected = { uris ->
                                if (uris.isNotEmpty()) {
                                    val resultIntent = Intent().apply {
                                        if (pickerAllowMultiple && uris.size > 1) {
                                            val clipData = android.content.ClipData.newUri(
                                                contentResolver,
                                                "Selected Media",
                                                uris[0]
                                            )
                                            for (i in 1 until uris.size) {
                                                clipData.addItem(android.content.ClipData.Item(uris[i]))
                                            }
                                            this.clipData = clipData
                                        } else {
                                            data = uris[0]
                                        }
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    setResult(RESULT_OK, resultIntent)
                                } else {
                                    setResult(RESULT_CANCELED)
                                }
                                finish()
                            },
                            navigator = navigator
                        )

                        if (isLoading && !showStartupAnimation) {
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

                    if (showStartupAnimation) {
                        StartupAnimation(onAnimationComplete = {
                            showStartupAnimation = false
                        })
                    }
                }

                BackHandler(enabled = true) {
                    onBack()
                }
            }
        }
    }
}
