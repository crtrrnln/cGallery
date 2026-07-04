package com.example.cgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.cgallery.data.MediaItem
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(
    images: List<MediaItem>,
    onCoverSelected: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    var selectedImage by remember { mutableStateOf<MediaItem?>(null) }
    var cropOffset by remember { mutableStateOf(Offset.Zero) }
    var cropScale by remember { mutableStateOf(1f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(Size.Zero) }

    // Reset crop when image changes
    LaunchedEffect(selectedImage) {
        cropOffset = Offset.Zero
        cropScale = 1f
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selectedImage == null) "Choose Cover" else "Adjust Crop") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedImage != null) {
                            selectedImage = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedImage != null) {
                        IconButton(onClick = {
                            if (containerSize.width > 0 && containerSize.height > 0) {
                                // Save as: scale, normalized_offset_x, normalized_offset_y
                                val normX = cropOffset.x / containerSize.width
                                val normY = cropOffset.y / containerSize.height
                                val cropData = "${cropScale},$normX,$normY"
                                onCoverSelected(selectedImage!!.uri.toString(), cropData)
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (selectedImage == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(images, key = { it.id }) { image ->
                    AsyncImage(
                        model = image.uri,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clickable { selectedImage = image },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Background dimmed image
                AsyncImage(
                    model = selectedImage!!.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f },
                    contentScale = ContentScale.Fit
                )

                // Cropping area (1:1 square)
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray)
                        .onGloballyPositioned { containerSize = it.size }
                        .pointerInput(imageIntrinsicSize, containerSize) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (cropScale * zoom).coerceIn(1f, 10f)
                                
                                // Default sizes if not loaded yet
                                val iWidth = if (imageIntrinsicSize.width > 0) imageIntrinsicSize.width else 1000f
                                val iHeight = if (imageIntrinsicSize.height > 0) imageIntrinsicSize.height else 1000f
                                val cWidth = if (containerSize.width > 0) containerSize.width.toFloat() else 900f
                                val cHeight = if (containerSize.height > 0) containerSize.height.toFloat() else 900f

                                // Calculate fitting
                                val containerAspectRatio = cWidth / cHeight
                                val imageAspectRatio = iWidth / iHeight
                                
                                val initialWidth: Float
                                val initialHeight: Float
                                
                                if (imageAspectRatio > containerAspectRatio) {
                                    initialWidth = cWidth
                                    initialHeight = cWidth / imageAspectRatio
                                } else {
                                    initialWidth = cHeight * imageAspectRatio
                                    initialHeight = cHeight
                                }

                                // Min scale to fill container
                                val minScaleToFill = max(cWidth / initialWidth, cHeight / initialHeight)
                                cropScale = max(newScale, minScaleToFill)
                                
                                val finalWidth = initialWidth * cropScale
                                val finalHeight = initialHeight * cropScale
                                
                                val maxOffsetX = (finalWidth - cWidth) / 2f
                                val maxOffsetY = (finalHeight - cHeight) / 2f
                                
                                val newOffset = cropOffset + pan
                                
                                // Snapping and clamping
                                val snapThreshold = 20f
                                var clampedX = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX)
                                var clampedY = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                
                                if (maxOffsetX > 0) {
                                    if (maxOffsetX - clampedX < snapThreshold) clampedX = maxOffsetX
                                    else if (clampedX - (-maxOffsetX) < snapThreshold) clampedX = -maxOffsetX
                                }
                                
                                if (maxOffsetY > 0) {
                                    if (maxOffsetY - clampedY < snapThreshold) clampedY = maxOffsetY
                                    else if (clampedY - (-maxOffsetY) < snapThreshold) clampedY = -maxOffsetY
                                }

                                cropOffset = Offset(clampedX, clampedY)
                            }
                        }
                        .clipToBounds()
                ) {
                    AsyncImage(
                        model = selectedImage!!.uri,
                        contentDescription = null,
                        onSuccess = { state ->
                            imageIntrinsicSize = Size(
                                state.painter.intrinsicSize.width,
                                state.painter.intrinsicSize.height
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = cropScale
                                scaleY = cropScale
                                translationX = cropOffset.x
                                translationY = cropOffset.y
                            },
                        contentScale = ContentScale.Fit
                    )
                    
                    // Simple guide lines
                    Box(Modifier.fillMaxSize().padding(1.dp).graphicsLayer { alpha = 0.2f }) {
                        HorizontalDivider(Modifier.align(Alignment.Center).fillMaxWidth())
                        VerticalDivider(Modifier.align(Alignment.Center).fillMaxHeight().width(1.dp))
                    }
                }
                
                Text(
                    text = "Pinch to zoom, drag to move. Edges snap to walls.",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
