package com.example.cgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.AlbumWithMedia
import com.example.cgallery.data.AlbumGroupManager
import com.example.cgallery.data.AlbumGroupEntity
import com.example.cgallery.data.AlbumEntity
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
    virtualAlbums: List<AlbumWithMedia> = emptyList(),
    onCreateAlbum: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit,
    onVirtualAlbumClick: (AlbumWithMedia) -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var selectedAlbumForMove by remember { mutableStateOf<AlbumEntity?>(null) }

    val groups by groupManager.rootGroups.collectAsState(initial = emptyList())
    val virtualAlbumEntities by remember(virtualAlbums) {
        derivedStateOf { virtualAlbums.map { it.album } }
    }
    val groupedVirtualAlbums = remember(groups, virtualAlbumEntities) {
        val albumsByGroup = mutableMapOf<Long?, List<AlbumEntity>>()
        val ungrouped = virtualAlbumEntities.filter { it.groupId == null }
        albumsByGroup[null] = ungrouped
        groups.forEach { group ->
            albumsByGroup[group.id] = virtualAlbumEntities.filter { it.groupId == group.id }
        }
        albumsByGroup
    }

    val physicalAlbums = remember(images) {
        images.groupBy { it.bucketName }.map { (name, imagesInAlbum) ->
            Album(
                name = name,
                count = imagesInAlbum.size,
                coverImage = imagesInAlbum.first()
            )
        }.sortedBy { it.name }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Albums") },
                actions = {
                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Group")
                    }
                    IconButton(onClick = { showCreateAlbumDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Virtual Album")
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
                    val albumsInGroup = groupedVirtualAlbums[group.id] ?: emptyList()
                    GroupAlbumItem(
                        group = group,
                        albumsInGroup = albumsInGroup,
                        images = images,
                        onClick = { onGroupClick(group.id) }
                    )
                }
            }

            // Display ungrouped virtual albums
            val ungroupedAlbums = groupedVirtualAlbums[null] ?: emptyList()
            if (ungroupedAlbums.isNotEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text("Virtual Albums", style = MaterialTheme.typography.titleMedium)
                }
                items(ungroupedAlbums) { album ->
                    val albumWithMedia = virtualAlbums.find { it.album.id == album.id }
                    if (albumWithMedia != null) {
                        val coverMedia = images.find { it.id == albumWithMedia.mediaIds.firstOrNull() }
                        VirtualAlbumItem(
                            albumWithMedia = albumWithMedia,
                            coverMedia = coverMedia,
                            onClick = { onVirtualAlbumClick(albumWithMedia) },
                            onLongPress = {
                                selectedAlbumForMove = album
                                showMoveToGroupDialog = true
                            }
                        )
                    }
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Text("Folders", style = MaterialTheme.typography.titleMedium)
            }
            items(physicalAlbums) { album ->
                AlbumItem(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }

    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("New Virtual Album") },
            text = {
                TextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    placeholder = { Text("Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAlbumName.isNotBlank()) {
                        onCreateAlbum(newAlbumName)
                        newAlbumName = ""
                        showCreateAlbumDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

    if (showMoveToGroupDialog && selectedAlbumForMove != null) {
        Dialog(onDismissRequest = {
            showMoveToGroupDialog = false
            selectedAlbumForMove = null
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Move to Group",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select a group to move '${selectedAlbumForMove!!.name}' to:")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        item {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        groupManager.moveAlbumToGroup(selectedAlbumForMove!!.id, null)
                                        showMoveToGroupDialog = false
                                        selectedAlbumForMove = null
                                    }
                                }
                            ) {
                                Text("No Group (Ungrouped)")
                            }
                        }
                        items(groups.size) { index ->
                            val group = groups[index]
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        groupManager.moveAlbumToGroup(selectedAlbumForMove!!.id, group.id)
                                        showMoveToGroupDialog = false
                                        selectedAlbumForMove = null
                                    }
                                }
                            ) {
                                Text(group.name)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showMoveToGroupDialog = false
                            selectedAlbumForMove = null
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupAlbumItem(
    group: AlbumGroupEntity,
    albumsInGroup: List<AlbumEntity>,
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
                    val albumWithMedia = albumsInGroup.first()
                    val coverImage = images.firstOrNull()
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
                            val coverImage = images.firstOrNull()
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
                                val coverImage = images.firstOrNull()
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
                        val coverImage = images.firstOrNull()
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
                                val coverImage = images.firstOrNull()
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
                                val coverImage = images.firstOrNull()
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
            text = "${albumsInGroup.size} albums",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VirtualAlbumItem(
    albumWithMedia: AlbumWithMedia,
    coverMedia: MediaItem?,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        if (coverMedia != null) {
            AsyncImage(
                model = coverMedia.uri,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = albumWithMedia.album.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = "${albumWithMedia.mediaIds.size} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = album.coverImage.uri,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
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
