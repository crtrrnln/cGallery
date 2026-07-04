package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
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
import com.example.cgallery.data.PhysicalAlbumManager
import com.example.cgallery.data.MediaItem
import kotlinx.coroutines.launch

// Mix child groups and albums into a single list for display
sealed class GroupDisplayItem {
    data class GroupItem(val group: AlbumGroupEntity) : GroupDisplayItem()
    data class AlbumItem(val album: Album, val entity: PhysicalAlbumEntity) : GroupDisplayItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    images: List<MediaItem>,
    mediaByBucket: Map<String, List<MediaItem>>,
    onAlbumClick: (String) -> Unit,
    onGroupClick: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }
    val physicalAlbumManager = remember { PhysicalAlbumManager(context) }

    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var selectedAlbumForGroup by remember { mutableStateOf<Album?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showReorderDialog by remember { mutableStateOf(false) }
    var reorderType by remember { mutableStateOf<com.example.cgallery.ReorderType?>(null) }

    val group by groupManager.getGroupById(groupId).collectAsState(initial = null)
    val albumsInGroup by groupManager.getAlbumsByGroup(groupId).collectAsState(initial = emptyList())
    val allGroups by groupManager.allGroups.collectAsState(initial = emptyList())

    // Get child groups of this group
    val childGroups = remember(allGroups, groupId) {
        allGroups.filter { it.parentId == groupId }
    }

    val displayItems = remember(childGroups, albumsInGroup, mediaByBucket) {
        val items = mutableListOf<GroupDisplayItem>()
        // Add child groups
        childGroups.forEach { childGroup ->
            items.add(GroupDisplayItem.GroupItem(childGroup))
        }
        // Add albums in this group
        albumsInGroup.forEach { albumEntity ->
            val imagesInAlbum = mediaByBucket[albumEntity.bucketName] ?: emptyList()
            if (imagesInAlbum.isNotEmpty()) {
                val album = Album(
                    name = albumEntity.bucketName,
                    displayName = java.io.File(albumEntity.bucketName).name,
                    count = imagesInAlbum.size,
                    coverImage = imagesInAlbum.first()
                )
                items.add(GroupDisplayItem.AlbumItem(album, albumEntity))
            }
        }
        items
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Group")
                    }
                    IconButton(onClick = {
                        reorderType = com.example.cgallery.ReorderType.ROOT_ITEMS
                        showReorderDialog = true
                    }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Reorder")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (displayItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No folders or groups in this group",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayItems, key = { item ->
                    when (item) {
                        is GroupDisplayItem.GroupItem -> "group_${item.group.id}"
                        is GroupDisplayItem.AlbumItem -> "album_${item.album.name}"
                    }
                }) { item ->
                    when (item) {
                        is GroupDisplayItem.GroupItem -> {
                            val albumsInChildGroup by groupManager.getAlbumsByGroup(item.group.id).collectAsState(initial = emptyList())
                            GroupAlbumItem(
                                group = item.group,
                                albumsInGroup = albumsInChildGroup,
                                mediaByBucket = mediaByBucket,
                                allGroups = emptyList(),
                                getAlbumsByGroup = { emptyList() },
                                onClick = { onGroupClick(item.group.id) }
                            )
                        }
                        is GroupDisplayItem.AlbumItem -> {
                            val albumsWithIndex = remember(albumsInGroup) {
                                albumsInGroup.sortedBy { it.sortOrder }.mapIndexed { index, entity -> entity to index }
                            }
                            val currentIndex = albumsWithIndex.indexOfFirst { it.first.bucketName == item.album.name }
                            val canMoveUp = currentIndex > 0
                            val canMoveDown = currentIndex < albumsWithIndex.size - 1

                            AlbumItem(
                                album = item.album,
                                onClick = { onAlbumClick(item.album.name) },
                                onLongClick = {
                                    selectedAlbumForGroup = item.album
                                    showMoveToGroupDialog = true
                                },
                                onMoveUp = {
                                    if (canMoveUp) {
                                        scope.launch {
                                            val currentAlbum = albumsWithIndex[currentIndex].first
                                            val previousAlbum = albumsWithIndex[currentIndex - 1].first
                                            physicalAlbumManager.updateAlbumSortOrder(currentAlbum.id, previousAlbum.sortOrder)
                                            physicalAlbumManager.updateAlbumSortOrder(previousAlbum.id, currentAlbum.sortOrder)
                                        }
                                    }
                                },
                                onMoveDown = {
                                    if (canMoveDown) {
                                        scope.launch {
                                            val currentAlbum = albumsWithIndex[currentIndex].first
                                            val nextAlbum = albumsWithIndex[currentIndex + 1].first
                                            physicalAlbumManager.updateAlbumSortOrder(currentAlbum.id, nextAlbum.sortOrder)
                                            physicalAlbumManager.updateAlbumSortOrder(nextAlbum.id, currentAlbum.sortOrder)
                                        }
                                    }
                                },
                                showReorderButtons = isReorderMode
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMoveToGroupDialog && selectedAlbumForGroup != null) {
        AlertDialog(
            onDismissRequest = {
                showMoveToGroupDialog = false
                selectedAlbumForGroup = null
                selectedGroupId = null
            },
            title = { Text("Move to Group") },
            text = {
                Column {
                    Text("Move \"${selectedAlbumForGroup?.name}\" to:")
                    Spacer(modifier = Modifier.height(8.dp))
                    // Option to remove from group (move to root)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedGroupId = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGroupId == null,
                            onClick = { selectedGroupId = null }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Root (No Group)")
                    }
                    // List of groups with nested structure (only show root groups to start)
                    allGroups.filter { it.parentId == null }.forEach { group ->
                        GroupDetailSelectionRow(
                            group = group,
                            allGroups = allGroups,
                            selectedGroupId = selectedGroupId,
                            onGroupSelected = { groupId -> selectedGroupId = groupId },
                            indent = 0
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        selectedAlbumForGroup?.let { album ->
                            physicalAlbumManager.moveAlbumToGroup(album.name, selectedGroupId)
                        }
                        showMoveToGroupDialog = false
                        selectedAlbumForGroup = null
                        selectedGroupId = null
                    }
                }) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMoveToGroupDialog = false
                    selectedAlbumForGroup = null
                    selectedGroupId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Group") },
            text = {
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (newGroupName.isNotBlank()) {
                                val result = groupManager.createGroup(newGroupName, groupId)
                                if (result > 0) {
                                    newGroupName = ""
                                    showCreateGroupDialog = false
                                }
                            }
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
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

    if (showReorderDialog && reorderType != null) {
        val itemsToReorder = when (reorderType) {
            com.example.cgallery.ReorderType.ROOT_ITEMS -> {
                // Create a unified list of groups and albums with their sort orders
                val childGroupsSorted = childGroups.sortedBy { it.sortOrder }
                val albumsInGroupSorted = albumsInGroup.sortedBy { it.sortOrder }
                val unifiedList = mutableListOf<Any>()
                // Interleave groups and albums based on sortOrder
                var groupIndex = 0
                var albumIndex = 0
                while (groupIndex < childGroupsSorted.size || albumIndex < albumsInGroupSorted.size) {
                    val groupSort = if (groupIndex < childGroupsSorted.size) childGroupsSorted[groupIndex].sortOrder else Int.MAX_VALUE
                    val albumSort = if (albumIndex < albumsInGroupSorted.size) albumsInGroupSorted[albumIndex].sortOrder else Int.MAX_VALUE
                    if (groupSort <= albumSort && groupIndex < childGroupsSorted.size) {
                        unifiedList.add(childGroupsSorted[groupIndex])
                        groupIndex++
                    } else if (albumIndex < albumsInGroupSorted.size) {
                        unifiedList.add(albumsInGroupSorted[albumIndex])
                        albumIndex++
                    }
                }
                unifiedList
            }
            null -> emptyList()
        }

        AlertDialog(
            onDismissRequest = {
                showReorderDialog = false
                reorderType = null
            },
            title = { Text("Reorder Items") },
            text = {
                LazyColumn {
                    items(itemsToReorder.size) { index ->
                        val item = itemsToReorder[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when (item) {
                                    is PhysicalAlbumEntity -> item.bucketName
                                    is AlbumGroupEntity -> item.name
                                    else -> ""
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (index > 0) {
                                                val prevItem = itemsToReorder[index - 1]
                                                // Only swap if both items are the same type
                                                when {
                                                    item is PhysicalAlbumEntity && prevItem is PhysicalAlbumEntity -> {
                                                        physicalAlbumManager.updateAlbumSortOrder(item.id, prevItem.sortOrder)
                                                        physicalAlbumManager.updateAlbumSortOrder(prevItem.id, item.sortOrder)
                                                    }
                                                    item is AlbumGroupEntity && prevItem is AlbumGroupEntity -> {
                                                        groupManager.updateGroupSortOrder(item.id, prevItem.sortOrder)
                                                        groupManager.updateGroupSortOrder(prevItem.id, item.sortOrder)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (index < itemsToReorder.size - 1) {
                                                val nextItem = itemsToReorder[index + 1]
                                                // Only swap if both items are the same type
                                                when {
                                                    item is PhysicalAlbumEntity && nextItem is PhysicalAlbumEntity -> {
                                                        physicalAlbumManager.updateAlbumSortOrder(item.id, nextItem.sortOrder)
                                                        physicalAlbumManager.updateAlbumSortOrder(nextItem.id, item.sortOrder)
                                                    }
                                                    item is AlbumGroupEntity && nextItem is AlbumGroupEntity -> {
                                                        groupManager.updateGroupSortOrder(item.id, nextItem.sortOrder)
                                                        groupManager.updateGroupSortOrder(nextItem.id, item.sortOrder)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = index < itemsToReorder.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showReorderDialog = false
                    reorderType = null
                }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun GroupDetailSelectionRow(
    group: AlbumGroupEntity,
    allGroups: List<AlbumGroupEntity>,
    selectedGroupId: Long?,
    onGroupSelected: (Long) -> Unit,
    indent: Int
) {
    val childGroups = remember(allGroups, group.id) {
        allGroups.filter { it.parentId == group.id }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onGroupSelected(group.id)
            }
            .padding(start = (8 + indent * 24).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selectedGroupId == group.id,
            onClick = { onGroupSelected(group.id) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(group.name)
    }

    // Recursively show child groups
    childGroups.forEach { child ->
        GroupDetailSelectionRow(
            group = child,
            allGroups = allGroups,
            selectedGroupId = selectedGroupId,
            onGroupSelected = onGroupSelected,
            indent = indent + 1
        )
    }
}
