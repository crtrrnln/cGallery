package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.FavoritesManager
import com.example.cgallery.data.LocalImage
import com.example.cgallery.data.MediaStoreDataSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onImageClick: (GalleryKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataSource = remember { MediaStoreDataSource(context) }
    val favoritesManager = remember { FavoritesManager(context) }
    
    var allImages by remember { mutableStateOf(emptyList<LocalImage>()) }
    var favoriteImages by remember { mutableStateOf(emptyList<LocalImage>()) }
    val favoriteIds by favoritesManager.favoriteIds.collectAsState(initial = emptySet())

    LaunchedEffect(favoriteIds) {
        allImages = dataSource.fetchImages()
        favoriteImages = allImages.filter { it.id in favoriteIds }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Favorites") }
            )
        }
    ) { innerPadding ->
        if (favoriteImages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No favorites yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(favoriteImages, key = { it.id }) { image ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clickable {
                                onImageClick(GalleryKey.Viewer(allImages.indexOf(image)))
                            }
                    ) {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
