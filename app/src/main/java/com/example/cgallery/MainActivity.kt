package com.example.cgallery
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
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
import androidx.core.content.ContextCompat
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.example.cgallery.data.*
import com.example.cgallery.ui.theme.CGalleryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var _backstackState = mutableStateOf<List<GalleryKey>>(listOf(GalleryKey.Gallery))
    private var _isLocked = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) { 
        super.onNewIntent(intent); setIntent(intent)
        if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") {
            if (_backstackState.value.none { it is GalleryKey.Inbox && it.isEnforcementSession }) _backstackState.value = listOf(GalleryKey.Inbox(true)) 
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); InboxDetectionService.start(this); val settingsRepo = AppSettingsRepository(this); val bioHelper = BiometricHelper(this); val db = VirtualAlbumDatabase.getDatabase(this)
        lifecycleScope.launch {
            val settings = settingsRepo.settingsFlow.first()
            if (settings.isBiometricEnabled && bioHelper.canAuthenticate()) { _isLocked.value = true; bioHelper.authenticate(this@MainActivity) { if (it) _isLocked.value = false else finish() } }
            if (intent.getStringExtra("TARGET_SCREEN") != "INBOX" && settings.isEnforcementEnabled && settings.requireInboxBeforeGallery) {
                val pending = db.inboxDao().getPendingItems().first()
                if (pending.isNotEmpty() && !(settings.snoozeExpirationTime > System.currentTimeMillis() || (settings.snoozeItemThreshold > 0 && pending.size < settings.snoozeItemThreshold))) _backstackState.value = listOf(GalleryKey.Inbox(true))
            }
        }
        if (intent.getStringExtra("TARGET_SCREEN") == "INBOX" && _backstackState.value.none { it is GalleryKey.Inbox && it.isEnforcementSession }) _backstackState.value = listOf(GalleryKey.Inbox(true))
        enableEdgeToEdge()
        setContent {
            CGalleryTheme {
                val vm: MediaStoreViewModel = viewModel(); val perms = remember { if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) }
                val isGranted = remember(perms) { (if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true) && perms.all { ContextCompat.checkSelfPermission(this, it) == 0 } }
                val isPick = remember { intent.action in listOf(Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK, "android.provider.action.PICK_IMAGES") }
                val isView = remember { intent.action == Intent.ACTION_VIEW }; val pickMult = remember { intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) }
                var showAnim by remember { mutableStateOf(!isPick && !isView) }; var bStack by remember { _backstackState }
                val isLocked by _isLocked
                if (isLocked) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) { Text("Locked", style = MaterialTheme.typography.headlineMedium) }
                else {
                    LaunchedEffect(Unit) { if (intent.getStringExtra("TARGET_SCREEN") == "INBOX") bStack = listOf(GalleryKey.Inbox(true)) else if (!isGranted) bStack = listOf(GalleryKey.Permission) }
                    val snack = remember { SnackbarHostState() }; LaunchedEffect(Unit) { vm.operationResult.collect { snack.showSnackbar(it) } }
                    val win = currentWindowAdaptiveInfo(); val isComp = remember(win) { win.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT }; val curStack by rememberUpdatedState(bStack)
                    val onNav: (GalleryKey) -> Unit = { k -> bStack = when (k) { is GalleryKey.Albums, is GalleryKey.Search -> listOf(k); is GalleryKey.Favourites -> if (curStack.lastOrNull() is GalleryKey.Albums) curStack + k else listOf(k); is GalleryKey.Gallery -> if (curStack.lastOrNull() is GalleryKey.Favourites) curStack + k else listOf(k); is GalleryKey.Viewer -> if (curStack.lastOrNull() is GalleryKey.Viewer) curStack.dropLast(1) + k else curStack + k; else -> curStack + k } }
                    val onBack: () -> Unit = { if (bStack.size > 1) bStack = bStack.dropLast(1) else finish() }
                    val onClearBS: () -> Unit = { val idx = bStack.indexOfFirst { it is GalleryKey.AlbumSelection || it is GalleryKey.InboxAlbumSelection }; bStack = if (idx != -1) bStack.take(idx) else if (bStack.size > 1) bStack.dropLast(1) else bStack }
                    val showBar = isGranted && (!isComp || bStack.lastOrNull() !is GalleryKey.Viewer) && bStack.none { it is GalleryKey.Inbox && it.isEnforcementSession }
                    Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snack) }, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
                        if (showBar && !showAnim) NavigationBar {
                            val base = bStack.firstOrNull { it is GalleryKey.Gallery || it is GalleryKey.Albums || it is GalleryKey.Favourites || it is GalleryKey.Search } ?: GalleryKey.Gallery
                            NavigationBarItem(base is GalleryKey.Gallery, { bStack = listOf(GalleryKey.Gallery) }, { Icon(Icons.Rounded.PhotoLibrary, null) }, label = { Text("Gallery") })
                            NavigationBarItem(base is GalleryKey.Albums, { bStack = listOf(GalleryKey.Albums) }, { Icon(Icons.Rounded.Collections, null) }, label = { Text("Albums") })
                            NavigationBarItem(base is GalleryKey.Favourites, { bStack = listOf(GalleryKey.Favourites) }, { Icon(Icons.Rounded.Favorite, null) }, label = { Text("Favourites") })
                            NavigationBarItem(base is GalleryKey.Search, { bStack = listOf(GalleryKey.Search) }, { Icon(Icons.Rounded.Search, null) }, label = { Text("Search") })
                        }
                    }) { p ->
                        val alpha by animateFloatAsState(if (showAnim) 0f else 1f, tween(1000, easing = LinearOutSlowInEasing), label = "a"); val scale by animateFloatAsState(if (showAnim) 0.95f else 1f, tween(1000, easing = LinearOutSlowInEasing), label = "s")
                        Box(Modifier.fillMaxSize().padding(p).graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale }) {
                            val nav = rememberListDetailPaneScaffoldNavigator<Any>()
                            GalleryNavDisplay(bStack, { l, s -> vm.copyMediaToAlbum(l, s) }, { l, s -> vm.moveMediaToAlbum(l, s) }, { n, g -> vm.createFolder(n, g) }, { vm.loadMedia() }, onBack, onClearBS, onNav, { vm.toggleAlbumVisibility(it) }, { uris ->
                                if (uris.isNotEmpty()) { val res = Intent().apply { if (pickMult && uris.size > 1) { val clip = android.content.ClipData.newUri(contentResolver, "Media", uris[0]); (1 until uris.size).forEach { clip.addItem(android.content.ClipData.Item(uris[it])) }; this.clipData = clip } else data = uris[0]; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; setResult(-1, res) } else setResult(0); finish()
                            }, isPick, pickMult, nav)
                            val isLoading by vm.isLoading.collectAsState()
                            if (isLoading && !showAnim) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(0.5f)), Alignment.Center) { CircularProgressIndicator() }
                        }
                        if (showAnim) StartupAnimation { showAnim = false }
                    }
                    BackHandler(true) { onBack() }
                }
            }
        }
    }
}
