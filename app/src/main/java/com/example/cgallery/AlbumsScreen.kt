package com.example.cgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.AlbumGroupManager
import com.example.cgallery.data.AlbumGroupEntity
import com.example.cgallery.data.PhysicalAlbumManager
import com.example.cgallery.data.PhysicalAlbumEntity
import kotlinx.coroutines.launch

data class Album(
    val name: String,
    val count: Int,
    val coverImage: MediaItem
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    images: List<MediaItem>,
    onAlbumClick: (Album) -> Unit,
    onGroupClick: (Long) -> Unit = {},
    onToggleAlbumVisibility: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }
    val physicalAlbumManager = remember { PhysicalAlbumManager(context) }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isHideShowMode by remember { mutableStateOf(false) }

    val groups by groupManager.rootGroups.collectAsState(initial = emptyList())
    val physicalAlbums by physicalAlbumManager.allAlbums.collectAsState(initial = emptyList())

    val visibleAlbums = remember(physicalAlbums, isHideShowMode) {
        if (isHideShowMode) {
            physicalAlbums
        } else {
            physicalAlbums.filter { !it.isHidden }
        }
    }

    val albumsWithDetails = remember(visibleAlbums, images) {
        visibleAlbums.mapNotNull { albumEntity ->
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

    val groupedAlbums = remember(groups, physicalAlbums) {
        val albumsByGroup = mutableMapOf<Long?, List<PhysicalAlbumEntity>>()
        val ungrouped = physicalAlbums.filter { it.groupId == null }
        albumsByGroup[null] = ungrouped
        groups.forEach { group ->
            albumsByGroup[group.id] = physicalAlbums.filter { it.groupId == group.id }
        }
        albumsByGroup
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Albums") },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Folder")
                    }
                    IconButton(onClick = { isHideShowMode = !isHideShowMode }) {
                        Icon(
                            if (isHideShowMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isHideShowMode) "Exit Hide/Show Mode" else "Enter Hide/Show Mode"
                        )
                    }
                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Group")
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
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display album groups
            if (groups.isNotEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text("Album Groups", style = MaterialTheme.typography.titleMedium)
                }
                items(groups) { group ->
                    val albumsInGroup = groupedAlbums[group.id] ?: emptyList()
                    GroupAlbumItem(
                        group = group,
                        albumsInGroup = albumsInGroup,
                        images = images,
                        onClick = { onGroupClick(group.id) }
                    )
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Text("Folders", style = MaterialTheme.typography.titleMedium)
            }
            items(albumsWithDetails) { album ->
                val albumEntity = physicalAlbums.find { it.bucketName == album.name }
                AlbumItem(
                    album = album,
                    isHidden = albumEntity?.isHidden == true,
                    isHideShowMode = isHideShowMode,
                    onClick = {
                        if (isHideShowMode) {
                            onToggleAlbumVisibility(album.name)
                        } else {
                            onAlbumClick(album)
                        }
                    }
                )
            }
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("New Album Group") },
            text = {
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder = { Text("Group Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newGroupName.isNotBlank()) {
                        scope.launch {
                            groupManager.createGroup(newGroupName)
                            newGroupName = ""
                            showCreateGroupDialog = false
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        scope.launch {
                            val result = physicalAlbumManager.createFolder(newFolderName)
                            if (result.isSuccess) {
                                newFolderName = ""
                                showCreateFolderDialog = false
                            }
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GroupAlbumItem(
    group: AlbumGroupEntity,
    albumsInGroup: List<PhysicalAlbumEntity>,
    images: List<MediaItem>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when (albumsInGroup.size) {
                0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                1 -> {
                    val album = albumsInGroup.first()
                    val coverImage = images.find { it.bucketName == album.bucketName }
                    if (coverImage != null) {
                        AsyncImage(
                            model = coverImage.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                2 -> {
                    Row(Modifier.fillMaxSize()) {
                        albumsInGroup.take(2).forEach { album ->
                            val coverImage = images.find { it.bucketName == album.bucketName }
                            if (coverImage != null) {
                                AsyncImage(
                                    model = coverImage.uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                3 -> {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(0.5f)) {
                            albumsInGroup.take(2).forEach { album ->
                                val coverImage = images.find { it.bucketName == album.bucketName }
                                if (coverImage != null) {
                                    AsyncImage(
                                        model = coverImage.uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        val coverImage = images.find { it.bucketName == albumsInGroup[2].bucketName }
                        if (coverImage != null) {
                            AsyncImage(
                                model = coverImage.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                else -> {
                    // 4+ albums: 2x2 grid
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.weight(1f)) {
                            albumsInGroup.take(2).forEach { album ->
                                val coverImage = images.find { it.bucketName == album.bucketName }
                                if (coverImage != null) {
                                    AsyncImage(
                                        model = coverImage.uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        Row(Modifier.weight(1f)) {
                            albumsInGroup.drop(2).take(2).forEach { album ->
                                val coverImage = images.find { it.bucketName == album.bucketName }
                                if (coverImage != null) {
                                    AsyncImage(
                                        model = coverImage.uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${albumsInGroup.size} folders",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlbumItem(
    album: Album,
    isHidden: Boolean = false,
    isHideShowMode: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = album.coverImage.uri,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            if (isHideShowMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isHidden)
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${album.count} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
