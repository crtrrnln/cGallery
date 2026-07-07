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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(startIndex: Int, mediaItems: List<MediaItem>, onBack: () -> Unit, onReloadMedia: () -> Unit = {}, onNavigate: (GalleryKey) -> Unit = {}, filteredMedia: List<MediaItem>? = null) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val favouritesManager = remember { FavouritesManager(context) }
    val favIds by favouritesManager.favouriteIds.collectAsState(initial = emptySet())
    var images by remember(mediaItems, filteredMedia) { mutableStateOf(filteredMedia ?: mediaItems) }
    var showInfo by remember { mutableStateOf(false) }
    
    if (images.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, images.size - 1), pageCount = { images.size })
    val cur = images.getOrNull(pagerState.currentPage)
    
    val vm: MediaStoreViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) { 
            onReloadMedia()
            scope.launch { 
                images = MediaStoreDataSource(context).fetchMedia(since = 0)
                if (images.isEmpty()) onBack() 
            } 
        }
    }
    val offY = remember { Animatable(0f) }; val scale = remember { Animatable(1f) }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back", tint = Color.White) } },
                actions = {
                    cur?.let { img ->
                        val isFav = img.id in favIds
                        IconButton({ scope.launch { if (isFav) favouritesManager.removeFavourite(img.id) else favouritesManager.addFavourite(img.id) } }) {
                            Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "fav", tint = Color.White)
                        }
                        IconButton({
                            val i = Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, img.uri); type = if (img.type == MediaType.VIDEO) "video/*" else "image/*"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            context.startActivity(Intent.createChooser(i, "share"))
                        }) { Icon(Icons.Rounded.Share, "share", tint = Color.White) }
                        if (img.type == MediaType.IMAGE) {
                            IconButton({
                                onNavigate(GalleryKey.ImageEditor(img.id))
                            }) { Icon(Icons.Rounded.Edit, "edit", tint = Color.White) }
                        }
                        if (img.type == MediaType.VIDEO) {
                            IconButton({
                                onNavigate(GalleryKey.VideoTrimmer(img.id))
                            }) { Icon(Icons.Rounded.ContentCut, "trim", tint = Color.White) }
                        }
                        IconButton({
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val p = MediaStore.createDeleteRequest(context.contentResolver, listOf(img.uri))
                                deleteLauncher.launch(IntentSenderRequest.Builder(p.intentSender).build())
                            } else {
                                context.contentResolver.delete(img.uri, null, null)
                                scope.launch { images = MediaStoreDataSource(context).fetchMedia(); if (images.isEmpty()) onBack() }
                            }
                        }) { Icon(Icons.Rounded.Delete, "del", tint = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = (0.5f * scale.value).coerceIn(0f, 0.5f)), navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            )
        },
        containerColor = Color.Black.copy(alpha = scale.value.coerceIn(0f, 1f))
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragStarted = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dragAmount = change.position - change.previousPosition
                        if (!dragStarted) {
                            if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x) * 2f && kotlin.math.abs(dragAmount.y) > 10f) {
                                dragStarted = true
                                change.consume()
                            } else if (kotlin.math.abs(dragAmount.x) > 10f) {
                                break // Let it be horizontal
                            }
                        } else {
                            change.consume()
                            scope.launch { 
                                offY.snapTo(offY.value + dragAmount.y)
                                if (offY.value > 0) scale.snapTo((1f - (offY.value / 1500f)).coerceIn(0.7f, 1f)) else scale.snapTo(1f) 
                            }
                        }
                    }
                    if (dragStarted) {
                        if (offY.value > 300f) {
                            if (showInfo) { showInfo = false; scope.launch { offY.animateTo(0f); scale.animateTo(1f) } }
                            else onBack()
                        }
                        else if (offY.value < -200f) { showInfo = true; scope.launch { offY.animateTo(0f); scale.animateTo(1f) } }
                        else { scope.launch { launch { offY.animateTo(0f, tween(200)) }; launch { scale.animateTo(1f, tween(200)) } } }
                    }
                }
            }
        }.graphicsLayer { translationY = offY.value; scaleX = scale.value; scaleY = scale.value }) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), pageSpacing = 16.dp, beyondViewportPageCount = 2, userScrollEnabled = true) { pg ->
                val img = images[pg]
                Box(Modifier.fillMaxSize()) {
                    if (img.type == MediaType.VIDEO) VideoPlayer(img.uri, pg == pagerState.currentPage, Modifier.fillMaxSize())
                    else ZoomableImage(img.uri, Modifier.fillMaxSize())
                    
                    AnimatedVisibility(
                        visible = showInfo,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 8.dp
                        ) {
                            MediaDetails(img, vm)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetails(item: MediaItem, vm: MediaStoreViewModel) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        InfoItem("Name", item.displayName)
        
        val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
        var showDatePicker by remember { mutableStateOf(false) }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoItem("Date", if (item.dateAdded > 0) df.format(Date(item.dateAdded * 1000)) else "???")
            IconButton({ showDatePicker = true }) { Icon(Icons.Rounded.Edit, "edit date", modifier = Modifier.size(16.dp)) }
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

        InfoItem("Album", File(item.bucketName).name)
        InfoItem("Path", item.fullPath)
        if (item.duration > 0) { 
            val mins = item.duration / 60000
            val secs = (item.duration % 60000) / 1000
            InfoItem("Duration", String.format("%02d:%02d", mins, secs)) 
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
