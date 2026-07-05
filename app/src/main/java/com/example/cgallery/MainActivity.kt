package com.example.cgallery
import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cgallery.data.*
import com.example.cgallery.ui.theme.CGalleryTheme

class MainActivity : ComponentActivity() {
    private var _backstackState = mutableStateOf<List<GalleryKey>>(listOf(GalleryKey.Gallery))
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") _backstackState.value = listOf(GalleryKey.Inbox(true)) }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); InboxDetectionService.start(this)
        if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") _backstackState.value = listOf(GalleryKey.Inbox(true))
        enableEdgeToEdge()
        setContent {
            CGalleryTheme(dynamicColor = false) {
                val nav = rememberListDetailPaneScaffoldNavigator<Any>()
                val perms = remember { if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) }
                val isGranted = remember(perms) { (if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true) && perms.all { ContextCompat.checkSelfPermission(this, it) == 0 } }
                val vm: MediaStoreViewModel = viewModel(); val items by vm.mediaItems.collectAsState(); val itemsMap by vm.mediaItemsMap.collectAsState()
                val byBuck by vm.mediaByBucket.collectAsState(); val favourites by vm.favouriteMedia.collectAsState(); val query by vm.searchQuery.collectAsState()
                val sRes by vm.searchResults.collectAsState(); val aRes by vm.albumResults.collectAsState(); val loading by vm.isLoading.collectAsState()
                val isPick = remember { intent.action in listOf(Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK, "android.provider.action.PICK_IMAGES") }
                val isView = remember { intent.action == Intent.ACTION_VIEW }; val pickMult = remember { intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) }
                var showAnim by remember { mutableStateOf(!isPick && !isView) }; var bStack by remember { _backstackState }

                LaunchedEffect(Unit) { if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") bStack = listOf(GalleryKey.Inbox(true)) else if (!isGranted) bStack = listOf(GalleryKey.Permission) }
                LaunchedEffect(isGranted, items) { if (isGranted && isView && items.isNotEmpty()) { intent.data?.let { uri -> val idx = items.indexOfFirst { it.uri == uri || it.fullPath == uri.path }; if (idx != -1) bStack = listOf(GalleryKey.Gallery, GalleryKey.Viewer(idx)) } } }
                val snack = remember { SnackbarHostState() }; LaunchedEffect(Unit) { vm.operationResult.collect { snack.showSnackbar(it) } }
                val win = currentWindowAdaptiveInfo(); val isComp = remember(win) { win.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }
                val curStack by rememberUpdatedState(bStack)

                val onNav: (GalleryKey) -> Unit = { k -> bStack = when (k) { is GalleryKey.Albums, is GalleryKey.Search -> listOf(k); is GalleryKey.Favourites -> if (curStack.lastOrNull() is GalleryKey.Albums) curStack + k else listOf(k); is GalleryKey.Gallery -> if (curStack.lastOrNull() is GalleryKey.Favourites) curStack + k else listOf(k); is GalleryKey.Viewer -> if (curStack.lastOrNull() is GalleryKey.Viewer) curStack.dropLast(1) + k else curStack + k; else -> curStack + k } }
                val onBack: () -> Unit = { if (bStack.size > 1) bStack = bStack.dropLast(1) else finish() }
                val onClearBS: () -> Unit = { val idx = bStack.indexOfFirst { it is GalleryKey.AlbumSelection || it is GalleryKey.InboxAlbumSelection }; bStack = if (idx != -1) bStack.take(idx) else if (bStack.size > 1) bStack.dropLast(1) else bStack }
                val showBar = isGranted && (!isComp || bStack.lastOrNull() !is GalleryKey.Viewer) && bStack.none { it is GalleryKey.Inbox && it.isEnforcementSession }

                Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snack) }, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
                    if (showBar && !showAnim) {
                        NavigationBar {
                            val base = bStack.firstOrNull { it is GalleryKey.Gallery || it is GalleryKey.Albums || it is GalleryKey.Favourites || it is GalleryKey.Search } ?: GalleryKey.Gallery
                            NavigationBarItem(base is GalleryKey.Gallery, { bStack = listOf(GalleryKey.Gallery) }, { Icon(Icons.Rounded.PhotoLibrary, "gal") }, label = { Text("Gallery") })
                            NavigationBarItem(base is GalleryKey.Albums, { bStack = listOf(GalleryKey.Albums) }, { Icon(Icons.Rounded.Collections, "alb") }, label = { Text("Albums") })
                            NavigationBarItem(base is GalleryKey.Favourites, { bStack = listOf(GalleryKey.Favourites) }, { Icon(Icons.Rounded.Favorite, "fav") }, label = { Text("Favourites") })
                            NavigationBarItem(base is GalleryKey.Search, { bStack = listOf(GalleryKey.Search) }, { Icon(Icons.Rounded.Search, "srch") }, label = { Text("Search") })
                        }
                    }
                }) { p ->
                    val alpha by animateFloatAsState(if (showAnim) 0f else 1f, tween(1000, easing = LinearOutSlowInEasing), label = "alpha")
                    val scale by animateFloatAsState(if (showAnim) 0.95f else 1f, tween(1000, easing = LinearOutSlowInEasing), label = "scale")
                    Box(Modifier.fillMaxSize().padding(p).graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale }) {
                        GalleryNavDisplay(bStack, items, itemsMap, byBuck, favourites, query, sRes, aRes, { vm.updateSearchQuery(it) }, { l, s -> vm.copyMediaToAlbum(l, s) }, { l, s -> vm.moveMediaToAlbum(l, s) }, { n, g -> vm.createFolder(n, g) }, { vm.loadMedia() }, onBack, onClearBS, onNav, { vm.toggleAlbumVisibility(it) }, { uris ->
                            if (uris.isNotEmpty()) { val res = Intent().apply { if (pickMult && uris.size > 1) { val clip = android.content.ClipData.newUri(contentResolver, "Media", uris[0]); (1 until uris.size).forEach { clip.addItem(android.content.ClipData.Item(uris[it])) }; this.clipData = clip } else data = uris[0]; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; setResult(-1, res) } else setResult(0); finish()
                        }, isPick, pickMult, nav)
                        if (loading && !showAnim) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(0.5f)), Alignment.Center) { CircularProgressIndicator() }
                    }
                    if (showAnim) StartupAnimation { showAnim = false }
                }
                BackHandler(true) { onBack() }
            }
        }
    }
}
