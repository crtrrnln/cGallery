package com.example.cgallery.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cgallery.data.*

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(com.example.cgallery.ui.theme.GlassSurface)
            .border(0.5.dp, com.example.cgallery.ui.theme.GlassBorder, shape)
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    image: MediaItem, 
    index: Int, 
    isSelected: Boolean = false, 
    isSelectionMode: Boolean = false, 
    efficiencyMode: Boolean = false,
    useModernUI: Boolean = true,
    isStarred: Boolean = false,
    onClick: () -> Unit, 
    onLongClick: () -> Unit = {}, 
    modifier: Modifier = Modifier
) {
    val pad = if (isSelectionMode) animateDpAsState(if (isSelected) 10.dp else 2.dp, label = "pad").value else 2.dp
    val ctx = LocalContext.current
    
    val size = if (efficiencyMode) 100 else 300
    val req = remember(image.uri, efficiencyMode) { 
        ImageRequest.Builder(ctx)
            .data(image.uri)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .size(size)
            .crossfade(true)
            .build() 
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isPressed) {
        if (isPressed && useModernUI) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val shape = if (useModernUI) {
        if (isSelected) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)
    } else {
        if (isSelected) RoundedCornerShape(12.dp) else androidx.compose.ui.graphics.RectangleShape
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(pad)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (useModernUI && !isSelected) Modifier.border(
                    0.5.dp, 
                    MaterialTheme.colorScheme.outlineVariant.copy(0.2f), 
                    shape
                ) else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                onClick = onClick, 
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(req, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        
        if (useModernUI) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.3f)),
                            startY = 300f
                        )
                    )
            )
        }

        if (image.type == MediaType.VIDEO) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.PlayCircle, "vid", Modifier.size(14.dp), Color.White)
            }
        }
        
        if (image.type == MediaType.GIF) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("GIF", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        if (isStarred) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Star, 
                    null, 
                    Modifier.size(if (useModernUI) 18.dp else 16.dp), 
                    if (useModernUI) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (isSelected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(0.25f))
            )
            Icon(
                Icons.Default.CheckCircle, 
                null, 
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp), 
                MaterialTheme.colorScheme.primary
            )
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
