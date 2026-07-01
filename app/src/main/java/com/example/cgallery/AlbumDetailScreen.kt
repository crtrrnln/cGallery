package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.LocalImage
import com.example.cgallery.data.MediaStoreDataSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    bucketName: String,
    onImageClick: (GalleryKey) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataSource = remember { MediaStoreDataSource(context) }
    var allImages by remember { mutableStateOf(emptyList<LocalImage>()) }
    var albumImages by remember { mutableStateOf(emptyList<LocalImage>()) }

    LaunchedEffect(bucketName) {
        allImages = dataSource.fetchImages()
        albumImages = allImages.filter { it.bucketName == bucketName }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(bucketName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(albumImages, key = { it.id }) { image ->
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
