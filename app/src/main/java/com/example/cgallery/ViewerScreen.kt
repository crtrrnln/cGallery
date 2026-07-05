package com.example.cgallery
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(startIndex: Int, mediaItems: List<MediaItem>, onBack: () -> Unit, filteredMedia: List<MediaItem>? = null) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val favoritesManager = remember { FavoritesManager(context) }
    val favIds by favoritesManager.favoriteIds.collectAsState(initial = emptySet())
    var images by remember(mediaItems, filteredMedia) { mutableStateOf(filteredMedia ?: mediaItems) }
    var showInfo by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    if (images.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, images.size - 1), pageCount = { images.size })
    val cur = images.getOrNull(pagerState.currentPage)
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) { scope.launch { images = MediaStoreDataSource(context).fetchMedia(); if (images.isEmpty()) onBack() } }
    }
    val offY = remember { Animatable(0f) }; val scale = remember { Animatable(1f) }
    Scaffold(
        topBar = {
            TopAppBar(title = { }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back", tint = Color.White) } },
                actions = {
                    cur?.let { img ->
                        val isFav = img.id in favIds
                        IconButton({ scope.launch { if (isFav) favoritesManager.removeFavorite(img.id) else favoritesManager.addFavorite(img.id) } }) {
                            Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "fav", tint = Color.White)
                        }
                        IconButton({
                            val i = Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, img.uri); type = if (img.type == MediaType.VIDEO) "video/*" else "image/*"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            context.startActivity(Intent.createChooser(i, "share"))
                        }) { Icon(Icons.Rounded.Share, "share", tint = Color.White) }
                        if (img.type == MediaType.IMAGE) {
                            IconButton({
                                val i = Intent(Intent.ACTION_EDIT).apply { setDataAndType(img.uri, "image/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                context.startActivity(Intent.createChooser(i, "edit"))
                            }) { Icon(Icons.Rounded.Edit, "edit", tint = Color.White) }
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
            detectDragGestures(onDrag = { change, drag ->
                if (kotlin.math.abs(drag.y) > kotlin.math.abs(drag.x) || offY.value != 0f) {
                    change.consume(); scope.launch { offY.snapTo(offY.value + drag.y); if (offY.value > 0) scale.snapTo((1f - (offY.value / 1500f)).coerceIn(0.7f, 1f)) else scale.snapTo(1f) }
                }
            }, onDragEnd = {
                if (offY.value > 300f) onBack()
                else if (offY.value < -200f) { showInfo = true; scope.launch { launch { offY.animateTo(0f, tween(200)) }; launch { scale.animateTo(1f, tween(200)) } } }
                else { scope.launch { launch { offY.animateTo(0f, tween(200)) }; launch { scale.animateTo(1f, tween(200)) } } }
            })
        }.graphicsLayer { translationY = offY.value; scaleX = scale.value; scaleY = scale.value }) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), pageSpacing = 16.dp, beyondViewportPageCount = 2) { pg ->
                val img = images[pg]
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    if (img.type == MediaType.VIDEO) VideoPlayer(img.uri, pg == pagerState.currentPage, Modifier.fillMaxSize())
                    else AsyncImage(img.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
        }
    }
    if (showInfo && cur != null) {
        ModalBottomSheet({ showInfo = false }, sheetState = sheetState, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text("Details", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                InfoItem("Name", cur.displayName)
                val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                InfoItem("Date", if (cur.dateAdded > 0) df.format(Date(cur.dateAdded * 1000)) else "???")
                InfoItem("Album", File(cur.bucketName).name)
                InfoItem("Path", cur.fullPath)
                if (cur.duration > 0) { val mins = cur.duration / 60000; val secs = (cur.duration % 60000) / 1000; InfoItem("Duration", String.format("%02d:%02d", mins, secs)) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
