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
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
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
            .bitmapConfig(Bitmap.Config.RGB_565) // Optimization: 50% memory saving
            .size(180)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(itemPadding)
            .then(
                if (isSelected) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Video indicator
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

        // GIF indicator
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

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            )
        }
    }
}
