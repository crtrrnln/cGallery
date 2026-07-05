package com.example.cgallery.ui
import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cgallery.data.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(image: MediaItem, index: Int, isSelected: Boolean = false, isSelectionMode: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val pad = if (isSelectionMode) animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "pad").value else 2.dp
    val ctx = LocalContext.current
    val req = remember(image.uri) { ImageRequest.Builder(ctx).data(image.uri).bitmapConfig(Bitmap.Config.RGB_565).size(180).build() }

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
