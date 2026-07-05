package com.example.cgallery
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.MediaItem
import com.example.cgallery.ui.MediaGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    favouriteImages: List<MediaItem>,
    onImageClick: (GalleryKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Favourites") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { p ->
        if (favouriteImages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("nothing here yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(3), modifier = modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(2.dp)) {
                itemsIndexed(favouriteImages, key = { _, i -> i.id }) { index, img ->
                    MediaGridItem(image = img, index = index, onClick = { onImageClick(GalleryKey.Viewer(index)) })
                }
            }
        }
    }
}
