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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
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
    private val backstackState = mutableStateOf<List<GalleryKey>>(listOf(GalleryKey.Gallery))
    private val isLocked = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InboxDetectionService.start(this)
        enableEdgeToEdge()
        
        // Initialize biometric and settings
        initializeAppLogic()
        
        // Handle initial intent
        handleIntent(intent)
        
        setContent {
            CGalleryTheme {
                MainContent(
                    backstackState = backstackState,
                    isLocked = isLocked,
                    intent = intent,
                    onBackstackChange = { backstackState.value = it },
                    onUnlock = { isLocked.value = false },
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        val target = intent.getStringExtra("TARGET_SCREEN")
        if (target == "INBOX") {
            backstackState.value = listOf(GalleryKey.Inbox(true))
        }
    }

    private fun initializeAppLogic() {
        val settingsRepo = AppSettingsRepository(this)
        val bioHelper = BiometricHelper(this)
        val db = VirtualAlbumDatabase.getDatabase(this)
        
        lifecycleScope.launch {
            val settings = settingsRepo.settingsFlow.first()
            
            // Handle biometric authentication
            if (settings.isBiometricEnabled && bioHelper.canAuthenticate()) {
                isLocked.value = true
                bioHelper.authenticate(this@MainActivity) { success ->
                    if (success) isLocked.value = false else finish()
                }
            }
            
            // Handle enforcement mode
            if (intent.getStringExtra("TARGET_SCREEN") != "INBOX" && 
                settings.isEnforcementEnabled && 
                settings.requireInboxBeforeGallery) {
                val pending = db.inboxDao().getPendingItems().first()
                val shouldEnforce = pending.isNotEmpty() && 
                    !(settings.snoozeExpirationTime > System.currentTimeMillis() || 
                      (settings.snoozeItemThreshold > 0 && pending.size < settings.snoozeItemThreshold))
                
                if (shouldEnforce) {
                    backstackState.value = listOf(GalleryKey.Inbox(true))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun MainContent(
    backstackState: MutableState<List<GalleryKey>>,
    isLocked: State<Boolean>,
    intent: Intent,
    onBackstackChange: (List<GalleryKey>) -> Unit,
    onUnlock: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { AppSettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val vm: MediaStoreViewModel = viewModel()
    val perms = remember { 
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    val isGranted = (if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else true) && perms.all { ContextCompat.checkSelfPermission(
        (LocalContext.current as FragmentActivity), it
    ) == 0 }
    
    val isPick = remember { 
        intent.action in listOf(Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK, "android.provider.action.PICK_IMAGES") 
    }
    val isView = remember { intent.action == Intent.ACTION_VIEW }
    val pickMult = remember { intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) }
    
    var showAnim by remember { mutableStateOf(!isPick && !isView) }
    var bStack by remember { backstackState }
    val locked by isLocked
    
    if (locked) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            Alignment.Center
        ) {
            Text("Locked", style = MaterialTheme.typography.headlineMedium)
        }
    } else {
        PermissionAndContentHandler(
            isGranted = isGranted,
            bStack = bStack,
            onBackstackChange = onBackstackChange,
            vm = vm,
            settings = settings,
            showAnim = showAnim,
            onShowAnimChange = { showAnim = it },
            isPick = isPick,
            pickMult = pickMult,
            intent = intent,
            onFinish = onFinish
        )
    }
}

private fun computeNavigationStack(currentStack: List<GalleryKey>, newKey: GalleryKey): List<GalleryKey> {
    return when (newKey) {
        is GalleryKey.Albums, is GalleryKey.Search -> listOf(newKey)
        is GalleryKey.Favourites -> if (currentStack.lastOrNull() is GalleryKey.Albums) {
            currentStack + newKey
        } else {
            listOf(newKey)
        }
        is GalleryKey.Gallery -> if (currentStack.lastOrNull() is GalleryKey.Favourites) {
            currentStack + newKey
        } else {
            listOf(newKey)
        }
        is GalleryKey.Viewer -> if (currentStack.lastOrNull() is GalleryKey.Viewer) {
            currentStack.dropLast(1) + newKey
        } else {
            currentStack + newKey
        }
        else -> currentStack + newKey
    }
}

private fun clearSelectionBackstack(backstack: List<GalleryKey>): List<GalleryKey> {
    val idx = backstack.indexOfFirst { 
        it is GalleryKey.AlbumSelection || it is GalleryKey.InboxAlbumSelection 
    }
    return when {
        idx != -1 -> backstack.take(idx)
        backstack.size > 1 -> backstack.dropLast(1)
        else -> backstack
    }
}

private fun shouldShowBottomBar(
    isGranted: Boolean,
    showAnim: Boolean,
    isCompact: Boolean,
    backstack: List<GalleryKey>
): Boolean {
    return isGranted && 
           !showAnim && 
           (!isCompact || backstack.lastOrNull() !is GalleryKey.Viewer) && 
           backstack.none { it is GalleryKey.Inbox && it.isEnforcementSession } && 
           backstack.lastOrNull() !is GalleryKey.Permission
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun PermissionAndContentHandler(
    isGranted: Boolean,
    bStack: List<GalleryKey>,
    onBackstackChange: (List<GalleryKey>) -> Unit,
    vm: MediaStoreViewModel,
    settings: AppSettings,
    showAnim: Boolean,
    onShowAnimChange: (Boolean) -> Unit,
    isPick: Boolean,
    pickMult: Boolean,
    intent: Intent,
    onFinish: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity
    
    LaunchedEffect(isGranted) {
        if (isGranted && bStack.lastOrNull() is GalleryKey.Permission) {
            onBackstackChange(listOf(GalleryKey.Gallery))
        } else if (!isGranted && bStack.lastOrNull() !is GalleryKey.Permission) {
            onBackstackChange(listOf(GalleryKey.Permission))
        }
    }
    
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.operationResult.collect { snack.showSnackbar(it) }
    }
    
    val win = currentWindowAdaptiveInfo()
    val isComp = remember(win) { 
        win.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT 
    }
    val curStack by rememberUpdatedState(bStack)
    
    val onNav: (GalleryKey) -> Unit = { key ->
        onBackstackChange(computeNavigationStack(curStack, key))
    }
    
    val onBack: () -> Unit = {
        if (bStack.size > 1) {
            onBackstackChange(bStack.dropLast(1))
        } else {
            onFinish()
        }
    }
    
    val onClearBS: () -> Unit = {
        onBackstackChange(clearSelectionBackstack(bStack))
    }
    
    val showBar = shouldShowBottomBar(isGranted, showAnim, isComp, bStack)
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                if (settings.useModernUI) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .navigationBarsPadding()
                    ) {
                        com.example.cgallery.ui.GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val base = bStack.firstOrNull { 
                                    it is GalleryKey.Gallery || it is GalleryKey.Albums || 
                                    it is GalleryKey.Favourites || it is GalleryKey.Search 
                                } ?: GalleryKey.Gallery
                                
                                ModernNavItem(
                                    selected = base is GalleryKey.Gallery,
                                    onClick = { onBackstackChange(listOf(GalleryKey.Gallery)) },
                                    icon = Icons.Rounded.PhotoLibrary,
                                    label = "Gallery"
                                )
                                ModernNavItem(
                                    selected = base is GalleryKey.Albums,
                                    onClick = { onBackstackChange(listOf(GalleryKey.Albums)) },
                                    icon = Icons.Rounded.Collections,
                                    label = "Albums"
                                )
                                ModernNavItem(
                                    selected = base is GalleryKey.Favourites,
                                    onClick = { onBackstackChange(listOf(GalleryKey.Favourites)) },
                                    icon = Icons.Rounded.Favorite,
                                    label = "Favourites"
                                )
                                ModernNavItem(
                                    selected = base is GalleryKey.Search,
                                    onClick = { onBackstackChange(listOf(GalleryKey.Search)) },
                                    icon = Icons.Rounded.Search,
                                    label = "Search"
                                )
                            }
                        }
                    }
                } else {
                    NavigationBar {
                        val base = bStack.firstOrNull { 
                            it is GalleryKey.Gallery || it is GalleryKey.Albums || 
                            it is GalleryKey.Favourites || it is GalleryKey.Search 
                        } ?: GalleryKey.Gallery
                        
                        NavigationBarItem(
                            selected = base is GalleryKey.Gallery,
                            onClick = { onBackstackChange(listOf(GalleryKey.Gallery)) },
                            icon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                            label = { Text("Gallery") }
                        )
                        NavigationBarItem(
                            selected = base is GalleryKey.Albums,
                            onClick = { onBackstackChange(listOf(GalleryKey.Albums)) },
                            icon = { Icon(Icons.Rounded.Collections, null) },
                            label = { Text("Albums") }
                        )
                        NavigationBarItem(
                            selected = base is GalleryKey.Favourites,
                            onClick = { onBackstackChange(listOf(GalleryKey.Favourites)) },
                            icon = { Icon(Icons.Rounded.Favorite, null) },
                            label = { Text("Favourites") }
                        )
                        NavigationBarItem(
                            selected = base is GalleryKey.Search,
                            onClick = { onBackstackChange(listOf(GalleryKey.Search)) },
                            icon = { Icon(Icons.Rounded.Search, null) },
                            label = { Text("Search") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val alpha by animateFloatAsState(
            if (showAnim) 0f else 1f, 
            tween(1000, easing = LinearOutSlowInEasing), 
            label = "alpha"
        )
        val scale by animateFloatAsState(
            if (showAnim) 0.95f else 1f, 
            tween(1000, easing = LinearOutSlowInEasing), 
            label = "scale"
        )
        
        Box(
            Modifier.fillMaxSize().padding(padding).graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
        ) {
            val nav = rememberListDetailPaneScaffoldNavigator<Any>()
            
            GalleryNavDisplay(
                backstack = bStack,
                onAddToAlbum = { paths, ids -> vm.copyMediaToAlbum(paths, ids) },
                onMoveToAlbum = { paths, ids -> vm.moveMediaToAlbum(paths, ids) },
                onCreateFolder = { name, groupId -> vm.createFolder(name, groupId) },
                onReloadMedia = { vm.loadMedia() },
                onBack = onBack,
                onClearSelectionBackstack = onClearBS,
                onNavigate = onNav,
                onToggleAlbumVisibility = { vm.toggleAlbumVisibility(it) },
                onMediaSelected = { uris ->
                    if (uris.isNotEmpty()) {
                        val res = Intent().apply {
                            if (pickMult && uris.size > 1) {
                                val clip = android.content.ClipData.newUri(
                                    context.contentResolver, "Media", uris[0]
                                )
                                (1 until uris.size).forEach { 
                                    clip.addItem(android.content.ClipData.Item(uris[it])) 
                                }
                                this.clipData = clip
                            } else {
                                data = uris[0]
                            }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.setResult(-1, res)
                    } else {
                        context.setResult(0)
                    }
                    context.finish()
                },
                isExternalPicker = isPick,
                pickerAllowMultiple = pickMult,
                navigator = nav
            )
            
            val isLoading by vm.isLoading.collectAsState()
            if (isLoading && !showAnim) {
                Box(
                    Modifier.fillMaxSize().background(
                        MaterialTheme.colorScheme.surface.copy(0.5f)
                    ),
                    Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        if (showAnim) {
            StartupAnimation { onShowAnimChange(false) }
        }
    }
    
    BackHandler(true) { onBack() }
}

@Composable
private fun ModernNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    val scale by animateFloatAsState(if (selected) 1.2f else 1f, label = "iconScale")
    val alpha by animateFloatAsState(if (selected) 1f else 0.6f, label = "iconAlpha")
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            })
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        if (selected) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
