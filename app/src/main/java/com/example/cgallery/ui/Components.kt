package com.example.cgallery.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cgallery.data.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    image: MediaItem, 
    index: Int, 
    isSelected: Boolean = false, 
    isSelectionMode: Boolean = false, 
    efficiencyMode: Boolean = false,
    onClick: () -> Unit, 
    onLongClick: () -> Unit = {}, 
    modifier: Modifier = Modifier
) {
    val pad = if (isSelectionMode) animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "pad").value else 2.dp
    val ctx = LocalContext.current
    
    val size = if (efficiencyMode) 100 else 180
    val req = remember(image.uri, efficiencyMode) { 
        ImageRequest.Builder(ctx)
            .data(image.uri)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .size(size)
            .crossfade(true)
            .build() 
    }

    Box(modifier = modifier.aspectRatio(1f).padding(pad).then(if (isSelected) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        AsyncImage(req, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (image.type == MediaType.VIDEO) Icon(Icons.Default.PlayCircle, "vid", Modifier.align(Alignment.BottomEnd).padding(6.dp).size(24.dp), Color.White.copy(0.8f))
        if (image.type == MediaType.GIF) Box(Modifier.align(Alignment.TopStart).padding(4.dp).background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("GIF", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        if (isSelected) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(0.2f)).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)))
            Icon(Icons.Default.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ZoomableImage(
    uri: android.net.Uri,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(androidx.compose.ui.graphics.RectangleShape)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        scale = 3f
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (scale > 1f || zoomChange != 1f) {
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += panChange
                                event.changes.forEach { it.consume() }
                            } else {
                                offset = androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}
