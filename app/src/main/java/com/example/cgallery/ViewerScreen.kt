package com.example.cgallery
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.*
import com.example.cgallery.ui.ZoomableImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ViewerScreen(
    startIndex: Int, 
    mediaItems: List<MediaItem>, 
    onBack: () -> Unit, 
    onReloadMedia: () -> Unit = {}, 
    onNavigate: (GalleryKey) -> Unit = {}, 
    filteredMedia: List<MediaItem>? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val favouritesManager = remember { FavouritesManager(context) }
    val vm: MediaStoreViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val favIds by favouritesManager.favouriteIds.collectAsState(initial = emptySet())
    val starredIds by vm.starredMediaIds.collectAsState()
    
    var images by remember(mediaItems, filteredMedia) { mutableStateOf(filteredMedia ?: mediaItems) }
    var showInfo by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(true) }
    
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val useModern = appSettings.useModernUI
    
    if (images.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, images.size - 1), pageCount = { images.size })
    val cur = images.getOrNull(pagerState.currentPage)
    
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) { onReloadMedia() }
    }
    val offY = remember { Animatable(0f) }; val scale = remember { Animatable(1f) }
    
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = uiVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                TopAppBar(
                    title = { }, 
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back", tint = Color.White) } },
                    actions = {
                        cur?.let { img ->
                            val isFav = img.id in favIds
                            val isStarred = img.id in starredIds || isFav
                            
                            // Star button (Pinterest style localized favorite)
                            IconButton(
                                onClick = { if (!isFav) vm.toggleStar(img.id) },
                                enabled = !isFav
                            ) {
                                Icon(
                                    imageVector = if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "star",
                                    tint = if (isFav) Color.Gray else Color.White
                                )
                            }

                            IconButton({ scope.launch { if (isFav) favouritesManager.removeFavourite(img.id) else favouritesManager.addFavourite(img.id) } }) {
                                Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "fav", tint = Color.White)
                            }
                            IconButton({
                                val i = Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, img.uri); type = if (img.type == MediaType.VIDEO) "video/*" else "image/*"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                context.startActivity(Intent.createChooser(i, "share"))
                            }) { Icon(Icons.Rounded.Share, "share", tint = Color.White) }
                            
                            if (img.type == MediaType.VIDEO) {
                                IconButton({ onNavigate(GalleryKey.VideoTrimmer(img.id)) }) { Icon(Icons.Rounded.ContentCut, "trim", tint = Color.White) }
                            }
                            IconButton({
                                vm.moveToTrash(listOf(img))
                                onBack()
                            }) { Icon(Icons.Rounded.Delete, "del", tint = Color.White) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f), navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
                )
            }
        },
        containerColor = Color.Black.copy(alpha = scale.value.coerceIn(0f, 1f))
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p).pointerInput(Unit) {
            val velocityTracker = VelocityTracker()
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragStarted = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val dragAmount = change.position - change.previousPosition
                        
                        if (!dragStarted) {
                            if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x) * 2f && kotlin.math.abs(dragAmount.y) > 10f) {
                                dragStarted = true
                                uiVisible = false
                                change.consume()
                            } else if (kotlin.math.abs(dragAmount.x) > 10f) {
                                break 
                            }
                        } else {
                            change.consume()
                            scope.launch { 
                                offY.snapTo(offY.value + dragAmount.y)
                                scale.snapTo((1f - (kotlin.math.abs(offY.value) / 2000f)).coerceIn(0.6f, 1f))
                            }
                        }
                    }
                    if (dragStarted) {
                        val velocity = velocityTracker.calculateVelocity().y
                        if (offY.value > 400f || velocity > 2000f) {
                            if (showInfo) { 
                                showInfo = false
                                scope.launch { offY.animateTo(0f, spring()); scale.animateTo(1f, spring()); uiVisible = true } 
                            } else onBack()
                        } else if (offY.value < -200f || velocity < -1500f) {
                            if (!showInfo) {
                                showInfo = true
                                scope.launch { offY.animateTo(0f, spring()); scale.animateTo(1f, spring()); uiVisible = false }
                            } else {
                                scope.launch { offY.animateTo(0f, spring()); scale.animateTo(1f, spring()) }
                            }
                        } else {
                            scope.launch { 
                                launch { offY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                if (!showInfo) uiVisible = true
                            }
                        }
                    } else {
                        // Handle tap to hide UI
                        uiVisible = !uiVisible
                    }
                }
            }
        }.graphicsLayer { 
            translationY = offY.value
            scaleX = scale.value
            scaleY = scale.value
            alpha = (1f - (kotlin.math.abs(offY.value) / 1000f)).coerceIn(0.5f, 1f)
        }) {
            HorizontalPager(
                state = pagerState, 
                modifier = Modifier.fillMaxSize(), 
                pageSpacing = 16.dp, 
                beyondViewportPageCount = 2, 
                userScrollEnabled = !showInfo 
            ) { pg ->
                val img = images[pg]
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures(onTap = { uiVisible = !uiVisible })
                    }
                ) {
                    val contentModifier = if (useModern && sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "image_${img.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier

                    Box(contentModifier.fillMaxSize()) {
                        if (img.type == MediaType.VIDEO) VideoPlayer(img.uri, pg == pagerState.currentPage, Modifier.fillMaxSize())
                        else ZoomableImage(img.uri, Modifier.fillMaxSize())
                    }
                    
                    AnimatedVisibility(
                        visible = showInfo,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp, max = 600.dp)
                                .padding(if (useModern) 12.dp else 16.dp),
                            shape = RoundedCornerShape(if (useModern) 32.dp else 24.dp),
                            color = if (useModern) com.example.cgallery.ui.theme.GlassSurface else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = if (useModern) 0.dp else 8.dp,
                            border = if (useModern) androidx.compose.foundation.BorderStroke(0.5.dp, com.example.cgallery.ui.theme.GlassBorder) else null
                        ) {
                            Column {
                                if (useModern) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        Alignment.Center
                                    ) {
                                        Surface(
                                            Modifier.size(40.dp, 4.dp),
                                            shape = RoundedCornerShape(2.dp),
                                            color = Color.White.copy(0.3f)
                                        ) {}
                                    }
                                }
                                MediaDetails(img, vm, useModern) { showInfo = false }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetails(item: MediaItem, vm: MediaStoreViewModel, useModern: Boolean = true, onClose: () -> Unit = {}) {
    Column(Modifier.padding(if (useModern) 24.dp else 16.dp).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Details", 
                style = if (useModern) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = if (useModern) Color.White else MaterialTheme.colorScheme.onSurface
            )
            if (useModern) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.6f))
                }
            }
        }
        Spacer(Modifier.height(if (useModern) 16.dp else 16.dp))
        InfoItem("Name", item.displayName, useModern)
        
        val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
        var showDatePicker by remember { mutableStateOf(false) }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoItem("Date", if (item.dateAdded > 0) df.format(Date(item.dateAdded * 1000)) else "???", useModern)
            IconButton({ showDatePicker = true }) { Icon(Icons.Rounded.Edit, "edit date", modifier = Modifier.size(16.dp), tint = if (useModern) Color.White.copy(0.7f) else MaterialTheme.colorScheme.primary) }
        }
        
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (item.dateAdded > 0) item.dateAdded * 1000 else System.currentTimeMillis())
            DatePickerDialog(onDismissRequest = { showDatePicker = false }, 
                confirmButton = { 
                    TextButton({ 
                        datePickerState.selectedDateMillis?.let { vm.updateMediaDate(item.id, item.type, it / 1000) }
                        showDatePicker = false
                    }) { Text("Update") } 
                }) {
                DatePicker(state = datePickerState)
            }
        }

        InfoItem("Album", File(item.bucketName).name, useModern)
        InfoItem("Path", item.fullPath, useModern)
        if (item.duration > 0) { 
            val mins = item.duration / 60000
            val secs = (item.duration % 60000) / 1000
            InfoItem("Duration", String.format("%02d:%02d", mins, secs), useModern) 
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun InfoItem(label: String, value: String, useModern: Boolean = true) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (useModern) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = if (useModern) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}
