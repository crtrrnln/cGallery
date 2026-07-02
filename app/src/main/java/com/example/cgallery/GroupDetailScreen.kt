package com.example.cgallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.AlbumGroupManager
import com.example.cgallery.data.AlbumGroupEntity
import com.example.cgallery.data.PhysicalAlbumEntity
import com.example.cgallery.data.MediaItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    images: List<MediaItem>,
    onAlbumClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }

    val group by groupManager.getGroupById(groupId).collectAsState(initial = null)
    val albumsInGroup by groupManager.getAlbumsByGroup(groupId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (albumsInGroup.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No folders in this group",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val albumsWithDetails = remember(albumsInGroup, images) {
                albumsInGroup.mapNotNull { albumEntity ->
                    val imagesInAlbum = images.filter { it.bucketName == albumEntity.bucketName }
                    if (imagesInAlbum.isNotEmpty()) {
                        Album(
                            name = albumEntity.bucketName,
                            count = imagesInAlbum.size,
                            coverImage = imagesInAlbum.first()
                        )
                    } else {
                        null
                    }
                }.sortedBy { it.name }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albumsWithDetails) { album ->
                    AlbumItem(
                        album = album,
                        onClick = { onAlbumClick(album.name) }
                    )
                }
            }
        }
    }
}
