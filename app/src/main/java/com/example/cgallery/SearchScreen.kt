package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
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
fun SearchScreen(
    onImageClick: (GalleryKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataSource = remember { MediaStoreDataSource(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    var allImages by remember { mutableStateOf(emptyList<LocalImage>()) }
    var allAlbums by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        allImages = dataSource.fetchImages()
        allAlbums = allImages.map { it.bucketName }.distinct()
    }

    val filteredImages = remember(searchQuery, allImages) {
        if (searchQuery.isBlank()) emptyList()
        else allImages.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    val filteredAlbums = remember(searchQuery, allAlbums) {
        if (searchQuery.isBlank()) emptyList()
        else allAlbums.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search photos or albums...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredAlbums.isNotEmpty()) {
                item {
                    Text("Albums", style = MaterialTheme.typography.titleMedium)
                }
                items(filteredAlbums) { albumName ->
                    ListItem(
                        headlineContent = { Text(albumName) },
                        modifier = Modifier.clickable { onImageClick(GalleryKey.AlbumDetail(albumName)) }
                    )
                }
            }

            if (filteredImages.isNotEmpty()) {
                item {
                    Text("Photos", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 2000.dp), // Height adjustment for nesting
                        contentPadding = PaddingValues(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        userScrollEnabled = false
                    ) {
                        items(filteredImages) { image ->
                            AsyncImage(
                                model = image.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { onImageClick(GalleryKey.Viewer(allImages.indexOf(image))) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            if (searchQuery.isNotBlank() && filteredImages.isEmpty() && filteredAlbums.isEmpty()) {
                item {
                    Text(
                        "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
