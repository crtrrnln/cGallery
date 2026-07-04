package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.MediaItem
import com.example.cgallery.ui.MediaGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: List<MediaItem>,
    albumResults: List<Pair<String, String>>,
    onUpdateSearchQuery: (String) -> Unit,
    onImageClick: (GalleryKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = onUpdateSearchQuery,
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
        val currentOnImageClick by rememberUpdatedState(onImageClick)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (albumResults.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Albums", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(albumResults, key = { "album_${it.second}" }, span = { GridItemSpan(maxLineSpan) }) { (albumName, albumPath) ->
                    ListItem(
                        headlineContent = { Text(albumName) },
                        modifier = Modifier.clickable { currentOnImageClick(GalleryKey.AlbumDetail(albumPath)) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (searchResults.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Photos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                itemsIndexed(searchResults, key = { _, it -> it.id }) { index, image ->
                    MediaGridItem(
                        image = image,
                        index = index,
                        onClick = {
                            currentOnImageClick(GalleryKey.Viewer(index))
                        }
                    )
                }
            }

            if (searchQuery.isNotBlank() && searchResults.isEmpty() && albumResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
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
