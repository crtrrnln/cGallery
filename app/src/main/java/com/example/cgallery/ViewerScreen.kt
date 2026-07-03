package com.example.cgallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaStoreDataSource
import com.example.cgallery.data.MediaType
import com.example.cgallery.data.FavoritesManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    startIndex: Int,
    mediaItems: List<MediaItem>,
    onBack: () -> Unit,
    filteredMedia: List<MediaItem>? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataSource = remember { MediaStoreDataSource(context) }
    val favoritesManager = remember { FavoritesManager(context) }
    val favoriteIds by favoritesManager.favoriteIds.collectAsState(initial = emptySet())
    var images by remember(mediaItems, filteredMedia) {
        mutableStateOf(filteredMedia ?: mediaItems)
    }

    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, images.size - 1),
        pageCount = { images.size }
    )
    
    val currentImage = images.getOrNull(pagerState.currentPage)

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                images = dataSource.fetchMedia()
                if (images.isEmpty()) {
                    onBack()
                }
            }
        }
    }

    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    currentImage?.let { image ->
                        val isFavorite = image.id in favoriteIds
                        IconButton(onClick = {
                            scope.launch {
                                if (isFavorite) {
                                    favoritesManager.removeFavorite(image.id)
                                } else {
                                    favoritesManager.addFavorite(image.id)
                                }
                            }
                        }) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from Favourites" else "Add to Favourites",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, image.uri)
                                type = if (image.type == MediaType.VIDEO) "video/*" else "image/*"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                        }
                        if (image.type == MediaType.IMAGE) {
                            IconButton(onClick = {
                                val editIntent = Intent().apply {
                                    action = Intent.ACTION_EDIT
                                    setDataAndType(image.uri, "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(editIntent, "Edit Image"))
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = Color.White)
                            }
                        }
                        IconButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    listOf(image.uri)
                                )
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            } else {
                                context.contentResolver.delete(image.uri, null, null)
                                scope.launch {
                                    images = dataSource.fetchMedia()
                                    if (images.isEmpty()) {
                                        onBack()
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = (0.5f * scale.value).coerceIn(0f, 0.5f)),
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black.copy(alpha = scale.value.coerceIn(0f, 1f))
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            // Only consume vertical drag if we're not dragging horizontally much
                            if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x) || offsetY.value != 0f) {
                                change.consume()
                                scope.launch {
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                    val newScale = (1f - (kotlin.math.abs(offsetY.value) / 1500f)).coerceIn(0.7f, 1f)
                                    scale.snapTo(newScale)
                                }
                            }
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(offsetY.value) > 300f) {
                                onBack()
                            } else {
                                scope.launch {
                                    launch { offsetY.animateTo(0f, tween(200)) }
                                    launch { scale.animateTo(1f, tween(200)) }
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    translationY = offsetY.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 2
            ) { page ->
                val image = images[page]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (image.type == MediaType.VIDEO) {
                        VideoPlayer(
                            uri = image.uri,
                            isActive = pagerState.currentPage == page,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
