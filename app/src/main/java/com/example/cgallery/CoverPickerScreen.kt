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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(
    images: List<MediaItem>,
    onCoverSelected: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    var selectedImage by remember { mutableStateOf<MediaItem?>(null) }
    var cropOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var cropScale by remember { mutableStateOf(1f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selectedImage == null) "Choose Cover" else "Adjust Crop") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedImage != null) {
                            selectedImage = null
                            cropOffset = androidx.compose.ui.geometry.Offset.Zero
                            cropScale = 1f
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
                            // Simplified crop storage: scale and offset as string
                            val cropData = "${cropScale},${cropOffset.x},${cropOffset.y}"
                            onCoverSelected(selectedImage!!.uri.toString(), cropData)
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
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.3f),
                    contentScale = ContentScale.Fit
                )

                // Cropping area (1:1 square)
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray)
                        .onGloballyPositioned { containerSize = it.size }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                cropScale *= zoom
                                cropOffset += pan
                            }
                        }
                        .clipToBounds()
                ) {
                    AsyncImage(
                        model = selectedImage!!.uri,
                        contentDescription = null,
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
                    Box(Modifier.fillMaxSize().padding(1.dp).graphicsLayer(alpha = 0.2f)) {
                        Divider(Modifier.align(Alignment.Center).fillMaxWidth())
                        Divider(Modifier.align(Alignment.Center).fillMaxHeight().width(1.dp))
                    }
                }
                
                Text(
                    text = "Pinch to zoom, drag to move",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
