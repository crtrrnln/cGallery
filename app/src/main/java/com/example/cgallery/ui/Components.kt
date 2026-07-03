package com.example.cgallery.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    image: MediaItem,
    index: Int,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onItemClick: (Int, Long) -> Unit,
    onItemLongClick: (Long) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    // Optimization: Skip animation calculations when not in selection mode to save UI thread cycles
    val itemPadding = if (isSelectionMode) {
        animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "padding").value
    } else {
        2.dp
    }

    val context = LocalContext.current
    val imageRequest = remember(image.uri) {
        ImageRequest.Builder(context)
            .data(image.uri)
            .crossfade(false) // Optimization: Disable crossfade for faster list popping
            .bitmapConfig(Bitmap.Config.RGB_565) // Optimization: 50% memory saving (2 bytes per pixel instead of 4)
            .size(180)
            .build()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val checkIcon = rememberVectorPainter(Icons.Default.CheckCircle)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(itemPadding)
            .then(
                // Optimization: Use drawWithCache for overlay to reduce layout nodes and overdraw
                if (isSelected) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(primaryColor.copy(alpha = 0.2f))
                                val iconSize = 24.dp.toPx()
                                val padding = 4.dp.toPx()
                                translate(left = size.width - iconSize - padding, top = padding) {
                                    with(checkIcon) {
                                        draw(size = Size(iconSize, iconSize))
                                    }
                                }
                            }
                        }
                } else Modifier
            )
            .combinedClickable(
                onClick = { onItemClick(index, image.id) },
                onLongClick = { onItemLongClick(image.id) }
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Video indicator (small play icon in bottom-right corner)
        if (image.type == MediaType.VIDEO) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = "Video",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
            )
        }

        // GIF indicator (small badge in top-left corner)
        if (image.type == MediaType.GIF) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GIF",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
