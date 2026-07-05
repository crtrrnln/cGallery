package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
    onAlbumClick: (String) -> Unit,
    onGroupClick: (Long) -> Unit = {},
    onChangeCover: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    selectionMode: Boolean = false,
    selectedAlbums: Set<String> = emptySet(),
    onToggleAlbumSelection: (String) -> Unit = {},
    onConfirmSelection: (List<String>) -> Unit = {},
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }
    val physicalAlbumManager = remember { PhysicalAlbumManager(context) }

    var showMoveToGroupDialog by remember { mutableStateOf(value = false) }
    var selectedAlbumForGroup by remember { mutableStateOf<Album?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var isReorderMode by remember { mutableStateOf(value = false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showReorderDialog by remember { mutableStateOf(false) }
    var reorderType by remember { mutableStateOf<ReorderType?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedGroupForMove by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    var showMoveGroupDialog by remember { mutableStateOf(false) }
    var selectedParentGroupId by remember { mutableStateOf<Long?>(null) }
    var showDeleteGroupConfirmation by remember { mutableStateOf<AlbumGroupEntity?>(null) }

    val group by groupManager.getGroupById(groupId).collectAsState(initial = null)
    val albumsInGroup by groupManager.getAlbumsByGroup(groupId).collectAsState(initial = emptyList())
    val allGroups by groupManager.allGroups.collectAsState(initial = emptyList())
    val allPhysicalAlbums by physicalAlbumManager.allAlbums.collectAsState(initial = emptyList())

    val mediaByBucketInternal = remember(images) {
        images.groupBy { item ->
            try {
                java.io.File(item.fullPath).parent ?: "Unknown"
            } catch (_: Exception) {
                "Unknown"
            }
        }
    }

    // Get child groups of this group
    val childGroups = remember(allGroups, groupId) {
        allGroups.filter { it.parentId == groupId }
    }

    val displayItems = remember(childGroups, albumsInGroup, mediaByBucketInternal) {
        val items = mutableListOf<GroupDisplayItem>()
        val combined = mutableListOf<GroupDisplayItem>()
        
        childGroups.forEach { combined.add(GroupDisplayItem.GroupItem(it)) }
        
        albumsInGroup.forEach { albumEntity ->
            val imagesInAlbum = mediaByBucketInternal[albumEntity.bucketName] ?: emptyList()
            val album = Album(
                name = albumEntity.bucketName,
                displayName = java.io.File(albumEntity.bucketName).name,
                count = imagesInAlbum.size,
                coverImage = imagesInAlbum.firstOrNull(),
            )
            combined.add(GroupDisplayItem.AlbumItem(album, albumEntity))
        }
        
        combined.sortedWith(
            compareBy(
                { 
                    when (it) {
                        is GroupDisplayItem.GroupItem -> it.group.sortOrder
                        is GroupDisplayItem.AlbumItem -> it.entity.sortOrder
                    }
                },
                {
                    when (it) {
                        is GroupDisplayItem.GroupItem -> it.group.name
                        is GroupDisplayItem.AlbumItem -> it.album.displayName
                    }
                }
            )
        ).forEach { items.add(it) }
        
        items
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (selectionMode) {
                        Text("${selectedAlbums.size} Selected")
                    } else {
                        Text(
                            text = group?.name ?: "Group",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { 
                            showCreateFolderDialog = true 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Create Album")
                        }
                        IconButton(
                            onClick = { onConfirmSelection(selectedAlbums.toList()) },
                            enabled = selectedAlbums.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Confirm")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Change Cover") },
                                    onClick = {
                                        showMenu = false
                                        onChangeCover()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                text = { Text("Create Album") },
                                onClick = {
                                    showMenu = false
                                    showCreateFolderDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Create Group") },
                                    onClick = {
                                        showMenu = false
                                        showCreateGroupDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reorder") },
                                    onClick = {
                                        showMenu = false
                                        reorderType = ReorderType.ROOT_ITEMS
                                        showReorderDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) }
                                )
                            }
                        }
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayItems, key = { item ->
                    when (item) {
                        is GroupDisplayItem.GroupItem -> "group_${item.group.id}"
                        is GroupDisplayItem.AlbumItem -> "album_${item.album.name}"
                    }
                }) { item ->
                    when (item) {
                        is GroupDisplayItem.GroupItem -> {
                            GroupAlbumItem(
                                group = item.group,
                                physicalAlbums = allPhysicalAlbums,
                                mediaByBucket = mediaByBucketInternal,
                                allGroups = allGroups,
                                albumsByGroup = { gid -> allPhysicalAlbums.filter { it.groupId == gid } },
                                onGroupClick = { onGroupClick(item.group.id) },
                                onLongClick = {
                                    selectedGroupForMove = item.group
                                    showMoveGroupDialog = true
                                }
                            )
                        }
                        is GroupDisplayItem.AlbumItem -> {
                            val albumsWithIndex = remember(albumsInGroup) {
                                albumsInGroup.asSequence()
                                    .sortedBy { it.sortOrder }
                                    .mapIndexed { index, entity -> entity to index }
                                    .toList()
                            }
                            val currentIndex = albumsWithIndex.indexOfFirst { it.first.bucketName == item.album.name }
                            val canMoveUp = currentIndex > 0
                            val canMoveDown = currentIndex < (albumsWithIndex.size - 1)
                            val isSelected = item.album.name in selectedAlbums

                            AlbumItem(
                                album = item.album,
                                isHidden = item.entity.isHidden,
                                isHideShowMode = false,
                                selectionMode = selectionMode,
                                isSelected = isSelected,
                                entity = item.entity,
                                onClick = {
                                    if (selectionMode) {
                                        onToggleAlbumSelection(item.album.name)
                                    } else {
                                        onAlbumClick(item.album.name)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectedAlbumForGroup = item.album
                                        showMoveToGroupDialog = true
                                    }
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
                                showSortControls = isReorderMode
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Album") },
            text = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder(newFolderName)
                        newFolderName = ""
                        showCreateFolderDialog = false
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

    if (showMoveGroupDialog && (selectedGroupForMove != null)) {
        AlertDialog(
            onDismissRequest = {
                showMoveGroupDialog = false
                selectedGroupForMove = null
                selectedParentGroupId = null
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Move Group")
                    IconButton(onClick = {
                        showDeleteGroupConfirmation = selectedGroupForMove
                        showMoveGroupDialog = false
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
            text = {
                Column {
                    Text("Move \"${selectedGroupForMove?.name}\" into:")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            // Option to move to root (no parent)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedParentGroupId = null
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedParentGroupId == null,
                                        onClick = { selectedParentGroupId = null }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Root (No Parent)")
                                }
                            }
                            
                            // List of other groups with nested structure
                            val otherGroups = allGroups.filter { it.id != selectedGroupForMove?.id }
                            val rootOtherGroups = otherGroups.filter { it.parentId == null }
                            
                            items(rootOtherGroups, key = { it.id }) { group ->
                                GroupDetailSelectionRow(
                                    group = group,
                                    allGroups = otherGroups,
                                    selectedGroupId = selectedParentGroupId,
                                    onGroupSelected = { selectedParentGroupId = it },
                                    indent = 0
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            selectedGroupForMove?.let { group ->
                                if (selectedParentGroupId == group.id) return@launch
                                fun isDescendant(groupId: Long, potentialDescendantId: Long): Boolean {
                                    if (groupId == potentialDescendantId) return true
                                    val children = allGroups.filter { it.parentId == groupId }
                                    return children.any { isDescendant(it.id, potentialDescendantId) }
                                }
                                if (selectedParentGroupId != null && isDescendant(group.id, selectedParentGroupId!!)) return@launch
                                groupManager.moveGroup(group.id, selectedParentGroupId)
                            }
                            showMoveGroupDialog = false
                            selectedGroupForMove = null
                            selectedParentGroupId = null
                        }
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMoveGroupDialog = false
                    selectedGroupForMove = null
                    selectedParentGroupId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteGroupConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirmation = null },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete the group \"${showDeleteGroupConfirmation?.name}\"? All albums and sub-groups within will be moved to the root.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            showDeleteGroupConfirmation?.let { group ->
                                groupManager.deleteGroup(group.id)
                            }
                            showDeleteGroupConfirmation = null
                            selectedGroupForMove = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteGroupConfirmation = null
                    showMoveGroupDialog = true 
                }) {
                    Text("Cancel")
                }
            }
        )
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
            ReorderType.ROOT_ITEMS -> {
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
            else -> emptyList()
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
                                                val currentSort = when (item) {
                                                    is PhysicalAlbumEntity -> item.sortOrder
                                                    is AlbumGroupEntity -> item.sortOrder
                                                    else -> 0
                                                }
                                                val prevSort = when (prevItem) {
                                                    is PhysicalAlbumEntity -> prevItem.sortOrder
                                                    is AlbumGroupEntity -> prevItem.sortOrder
                                                    else -> 0
                                                }

                                                // Update item
                                                when (item) {
                                                    is PhysicalAlbumEntity -> physicalAlbumManager.updateAlbumSortOrder(item.id, prevSort)
                                                    is AlbumGroupEntity -> groupManager.updateGroupSortOrder(item.id, prevSort)
                                                }
                                                // Update prevItem
                                                when (prevItem) {
                                                    is PhysicalAlbumEntity -> physicalAlbumManager.updateAlbumSortOrder(prevItem.id, currentSort)
                                                    is AlbumGroupEntity -> groupManager.updateGroupSortOrder(prevItem.id, currentSort)
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
                                                val currentSort = when (item) {
                                                    is PhysicalAlbumEntity -> item.sortOrder
                                                    is AlbumGroupEntity -> item.sortOrder
                                                    else -> 0
                                                }
                                                val nextSort = when (nextItem) {
                                                    is PhysicalAlbumEntity -> nextItem.sortOrder
                                                    is AlbumGroupEntity -> nextItem.sortOrder
                                                    else -> 0
                                                }

                                                // Update item
                                                when (item) {
                                                    is PhysicalAlbumEntity -> physicalAlbumManager.updateAlbumSortOrder(item.id, nextSort)
                                                    is AlbumGroupEntity -> groupManager.updateGroupSortOrder(item.id, nextSort)
                                                }
                                                // Update nextItem
                                                when (nextItem) {
                                                    is PhysicalAlbumEntity -> physicalAlbumManager.updateAlbumSortOrder(nextItem.id, currentSort)
                                                    is AlbumGroupEntity -> groupManager.updateGroupSortOrder(nextItem.id, currentSort)
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
