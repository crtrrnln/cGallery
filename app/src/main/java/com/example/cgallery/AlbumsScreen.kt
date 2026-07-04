package com.example.cgallery

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.AlbumGroupManager
import com.example.cgallery.data.AlbumGroupEntity
import com.example.cgallery.data.PhysicalAlbumManager
import com.example.cgallery.data.PhysicalAlbumEntity
import com.example.cgallery.data.FavoritesManager
import kotlinx.coroutines.launch

import java.io.File

data class Album(
    val name: String, // This is now the full path
    val displayName: String, // This is the short name for UI
    val count: Int,
    val coverImage: MediaItem?
)

enum class ReorderType {
    ROOT_ITEMS
}

// Mix groups and albums into a single list for display
sealed class DisplayItem {
    data class SpecialAlbum(val name: String, val type: SpecialAlbumType) : DisplayItem()
    data class GroupItem(val group: AlbumGroupEntity) : DisplayItem()
    data class AlbumItem(val album: Album, val entity: PhysicalAlbumEntity) : DisplayItem()
}

enum class SpecialAlbumType {
    RECENTS,
    FAVOURITES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    images: List<MediaItem>,
    mediaByBucket: Map<String, List<MediaItem>>,
    onAlbumClick: (Album) -> Unit,
    onConfirmSelection: (List<String>) -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    onToggleAlbumVisibility: (String) -> Unit = {},
    onSpecialAlbumClick: (SpecialAlbumType) -> Unit = {},
    onInboxClick: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    selectionMode: Boolean = false,
    externalSelectedAlbums: Set<String>? = null,
    onToggleAlbumSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }
    val physicalAlbumManager = remember { PhysicalAlbumManager(context) }

    var localSelectedAlbumsForAction by remember { mutableStateOf(setOf<String>()) }
    val selectedAlbumsForAction = externalSelectedAlbums ?: localSelectedAlbumsForAction
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isHideShowMode by remember { mutableStateOf(false) }
    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var selectedAlbumForGroup by remember { mutableStateOf<Album?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var showMoveGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupForMove by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    var selectedParentGroupId by remember { mutableStateOf<Long?>(null) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var reorderType by remember { mutableStateOf<ReorderType?>(null) }
    var showDeleteGroupConfirmation by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val groups by groupManager.rootGroups.collectAsState(initial = emptyList())
    val allGroups by groupManager.allGroups.collectAsState(initial = emptyList())
    val physicalAlbums by physicalAlbumManager.allAlbums.collectAsState(initial = emptyList())
    val favoritesManager = remember { FavoritesManager(context) }
    val favoriteIds by favoritesManager.favoriteIds.collectAsState(initial = emptySet())

    val visibleAlbums = remember(physicalAlbums, isHideShowMode) {
        if (isHideShowMode) {
            physicalAlbums
        } else {
            physicalAlbums.filter { !it.isHidden }
        }
    }

    // Only show ungrouped albums in the main list
    val ungroupedAlbums = remember(visibleAlbums) {
        visibleAlbums.filter { it.groupId == null }.distinctBy { it.bucketName }
    }

    val albumsWithDetails = remember(ungroupedAlbums, mediaByBucket) {
        ungroupedAlbums.map { albumEntity ->
            val imagesInAlbum = mediaByBucket[albumEntity.bucketName] ?: emptyList()
            Album(
                name = albumEntity.bucketName,
                displayName = File(albumEntity.bucketName).name,
                count = imagesInAlbum.size,
                coverImage = imagesInAlbum.firstOrNull()
            )
        }.distinctBy { it.name }
    }

    val groupedAlbums = remember(allGroups, physicalAlbums) {
        val albumsByGroup = mutableMapOf<Long?, List<PhysicalAlbumEntity>>()
        val ungrouped = physicalAlbums.filter { it.groupId == null }
        albumsByGroup[null] = ungrouped
        allGroups.forEach { group ->
            albumsByGroup[group.id] = physicalAlbums.filter { it.groupId == group.id }
        }
        albumsByGroup
    }

    val displayItems = remember(groups, albumsWithDetails, physicalAlbums, selectionMode) {
        val items = mutableListOf<DisplayItem>()
        // Add special albums only when not in selection mode
        if (!selectionMode) {
            items.add(DisplayItem.SpecialAlbum("Recent", SpecialAlbumType.RECENTS))
            items.add(DisplayItem.SpecialAlbum("Favourites", SpecialAlbumType.FAVOURITES))
        }

        val rootItems = mutableListOf<DisplayItem>()
        
        // Add root groups
        groups.filter { it.parentId == null }.forEach { group ->
            rootItems.add(DisplayItem.GroupItem(group))
        }

        // Add ungrouped albums
        val relevantAlbums = if (selectionMode) albumsWithDetails else albumsWithDetails.filter { album ->
            physicalAlbums.find { it.bucketName == album.name }?.groupId == null
        }
        
        relevantAlbums.forEach { album ->
            val entity = physicalAlbums.find { it.bucketName == album.name }
            if (entity != null) {
                rootItems.add(DisplayItem.AlbumItem(album, entity))
            }
        }

        // Sort combined list by sortOrder, then by name to mix them if sortOrder is identical
        rootItems.sortedWith(compareBy({ 
            when (it) {
                is DisplayItem.GroupItem -> it.group.sortOrder
                is DisplayItem.AlbumItem -> it.entity.sortOrder
                else -> Int.MAX_VALUE
            }
        }, {
            when (it) {
                is DisplayItem.GroupItem -> it.group.name
                is DisplayItem.AlbumItem -> it.album.displayName
                else -> ""
            }
        })).forEach { items.add(it) }

        items.distinctBy { item ->
            when (item) {
                is DisplayItem.SpecialAlbum -> "special_${item.type}"
                is DisplayItem.GroupItem -> "group_${item.group.id}"
                is DisplayItem.AlbumItem -> "album_${item.album.name}"
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val structure = physicalAlbumManager.exportStructure()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(structure.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val structure = stream.bufferedReader().readText()
                    physicalAlbumManager.importStructure(structure)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selectionMode) "Select Destinations" else "Albums") },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { onConfirmSelection(emptyList()) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = onInboxClick) {
                            Icon(Icons.Default.Email, contentDescription = "Inbox")
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = { onConfirmSelection(selectedAlbumsForAction.toList()) },
                            enabled = selectedAlbumsForAction.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Create Album") },
                                    onClick = {
                                        showMenu = false
                                        showCreateFolderDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Add, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Create Group") },
                                    onClick = {
                                        showMenu = false
                                        showCreateGroupDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isHideShowMode) "Exit Hide/Show Mode" else "Hide/Show Albums") },
                                    onClick = {
                                        showMenu = false
                                        isHideShowMode = !isHideShowMode
                                    },
                                    leadingIcon = { 
                                        Icon(if (isHideShowMode) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Structure") },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("application/json"))
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Structure") },
                                    onClick = {
                                        showMenu = false
                                        exportLauncher.launch("cgallery_structure.json")
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reorder") },
                                    onClick = {
                                        showMenu = false
                                        reorderType = ReorderType.ROOT_ITEMS
                                        showReorderDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.SwapVert, null) }
                                )
                            }
                        }
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(displayItems, key = { item ->
                when (item) {
                    is DisplayItem.SpecialAlbum -> "special_${item.type}"
                    is DisplayItem.GroupItem -> "group_${item.group.id}"
                    is DisplayItem.AlbumItem -> "album_${item.album.name}"
                }
            }) { item ->
                when (item) {
                    is DisplayItem.SpecialAlbum -> {
                        SpecialAlbumItem(
                            name = item.name,
                            type = item.type,
                            images = images,
                            favoriteIds = favoriteIds,
                            onClick = { onSpecialAlbumClick(item.type) }
                        )
                    }
                    is DisplayItem.GroupItem -> {
                        val albumsInGroup = groupedAlbums[item.group.id] ?: emptyList()
                        GroupAlbumItem(
                            group = item.group,
                            albumsInGroup = albumsInGroup,
                            mediaByBucket = mediaByBucket,
                            allGroups = allGroups,
                            getAlbumsByGroup = { groupId -> groupedAlbums[groupId] ?: emptyList() },
                            onClick = { onGroupClick(item.group.id) },
                            onLongClick = {
                                selectedGroupForMove = item.group
                                showMoveGroupDialog = true
                            }
                        )
                    }
                    is DisplayItem.AlbumItem -> {
                        val isSelected = item.album.name in selectedAlbumsForAction
                        AlbumItem(
                            album = item.album,
                            isHidden = item.entity.isHidden,
                            isHideShowMode = isHideShowMode,
                            isSelected = isSelected,
                            selectionMode = selectionMode,
                            entity = item.entity,
                            onClick = {
                                if (selectionMode) {
                                    if (externalSelectedAlbums != null) {
                                        onToggleAlbumSelection(item.album.name)
                                    } else {
                                        localSelectedAlbumsForAction = if (isSelected) {
                                            localSelectedAlbumsForAction - item.album.name
                                        } else {
                                            localSelectedAlbumsForAction + item.album.name
                                        }
                                    }
                                } else if (isHideShowMode) {
                                    onToggleAlbumVisibility(item.album.name)
                                } else {
                                    onAlbumClick(item.album)
                                }
                            },
                            onLongClick = {
                                if (!isHideShowMode && !selectionMode) {
                                    selectedAlbumForGroup = item.album
                                    showMoveToGroupDialog = true
                                }
                            }
                        )
                    }
                }
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
                    
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            // Option to remove from group
                            item {
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
                            }
                            // List of groups with nested structure
                            val rootGroups = allGroups.filter { it.parentId == null }
                            items(rootGroups, key = { it.id }) { group ->
                                GroupSelectionRow(
                                    group = group,
                                    allGroups = allGroups,
                                    selectedGroupId = selectedGroupId,
                                    onGroupSelected = { selectedGroupId = it },
                                    indent = 0
                                )
                            }
                        }
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

    if (showMoveGroupDialog && selectedGroupForMove != null) {
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
                                GroupSelectionRow(
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
                                // Prevent moving a group into itself or its descendants
                                if (selectedParentGroupId == group.id) {
                                    return@launch
                                }
                                
                                // Check if the target parent is a descendant of the group being moved
                                fun isDescendant(groupId: Long, potentialDescendantId: Long): Boolean {
                                    if (groupId == potentialDescendantId) return true
                                    val children = allGroups.filter { it.parentId == groupId }
                                    return children.any { isDescendant(it.id, potentialDescendantId) }
                                }
                                
                                if (selectedParentGroupId != null && isDescendant(group.id, selectedParentGroupId!!)) {
                                    return@launch
                                }
                                
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
                    showMoveGroupDialog = true // Re-open move dialog
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReorderDialog && reorderType != null) {
        val itemsToReorder = when (reorderType) {
            ReorderType.ROOT_ITEMS -> {
                // Create a unified list of groups and albums with their sort orders
                val rootGroups = groups.filter { it.parentId == null }.sortedBy { it.sortOrder }
                val ungroupedAlbumsSorted = ungroupedAlbums.sortedBy { it.sortOrder }
                val unifiedList = mutableListOf<Any>()
                // Interleave groups and albums based on sortOrder
                var groupIndex = 0
                var albumIndex = 0
                while (groupIndex < rootGroups.size || albumIndex < ungroupedAlbumsSorted.size) {
                    val groupSort = if (groupIndex < rootGroups.size) rootGroups[groupIndex].sortOrder else Int.MAX_VALUE
                    val albumSort = if (albumIndex < ungroupedAlbumsSorted.size) ungroupedAlbumsSorted[albumIndex].sortOrder else Int.MAX_VALUE
                    if (groupSort <= albumSort && groupIndex < rootGroups.size) {
                        unifiedList.add(rootGroups[groupIndex])
                        groupIndex++
                    } else if (albumIndex < ungroupedAlbumsSorted.size) {
                        unifiedList.add(ungroupedAlbumsSorted[albumIndex])
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
fun GroupSelectionRow(
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
        GroupSelectionRow(
            group = child,
            allGroups = allGroups,
            selectedGroupId = selectedGroupId,
            onGroupSelected = onGroupSelected,
            indent = indent + 1
        )
    }
}

@Composable
fun SpecialAlbumItem(
    name: String,
    type: SpecialAlbumType,
    images: List<MediaItem>,
    favoriteIds: Set<Long>,
    onClick: () -> Unit
) {
    val coverImage = remember(type, images, favoriteIds) {
        when (type) {
            SpecialAlbumType.RECENTS -> images.firstOrNull()
            SpecialAlbumType.FAVOURITES -> images.find { it.id in favoriteIds }
        }
    }

    val itemCount = remember(type, images, favoriteIds) {
        when (type) {
            SpecialAlbumType.RECENTS -> images.size
            SpecialAlbumType.FAVOURITES -> favoriteIds.size // Much faster than images.count { it.id in favoriteIds }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverImage != null) {
                val context = LocalContext.current
                val request = remember(coverImage.uri) {
                    ImageRequest.Builder(context)
                        .data(coverImage.uri)
                        .crossfade(false)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .size(180)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$itemCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GroupAlbumItem(
    group: AlbumGroupEntity,
    albumsInGroup: List<PhysicalAlbumEntity>,
    mediaByBucket: Map<String, List<MediaItem>>,
    allGroups: List<AlbumGroupEntity>,
    getAlbumsByGroup: (Long?) -> List<PhysicalAlbumEntity>,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    // Correctly get representative images for the collage, respecting sort order and nested groups
    val representativeImages = remember(group.id, albumsInGroup, allGroups, mediaByBucket, getAlbumsByGroup) {
        val childGroups = allGroups.filter { it.parentId == group.id }
        
        // Direct children (albums and groups) sorted by sortOrder
        val directChildren = (albumsInGroup + childGroups).sortedWith(compareBy({
            when (it) {
                is PhysicalAlbumEntity -> it.sortOrder
                is AlbumGroupEntity -> it.sortOrder
                else -> Int.MAX_VALUE
            }
        }, {
            when (it) {
                is PhysicalAlbumEntity -> it.bucketName
                is AlbumGroupEntity -> it.name
                else -> ""
            }
        }))

        val images = mutableListOf<String>()

        fun getFirstImageOfAlbum(album: PhysicalAlbumEntity): String? {
            return album.customCoverUri ?: mediaByBucket[album.bucketName]?.firstOrNull()?.uri?.toString()
        }

        fun getFirstImageOfGroup(groupId: Long, visited: MutableSet<Long> = mutableSetOf()): String? {
            if (groupId in visited) return null
            visited.add(groupId)

            val currentGroup = allGroups.find { it.id == groupId }
            if (currentGroup?.customCoverUri != null) return currentGroup.customCoverUri

            val albums = getAlbumsByGroup(groupId).sortedBy { it.sortOrder }
            val children = allGroups.filter { it.parentId == groupId }.sortedBy { it.sortOrder }
            
            val nestedChildren = (albums + children).sortedWith(compareBy({
                when (it) {
                    is PhysicalAlbumEntity -> it.sortOrder
                    is AlbumGroupEntity -> it.sortOrder
                    else -> Int.MAX_VALUE
                }
            }, {
                when (it) {
                    is PhysicalAlbumEntity -> it.bucketName
                    is AlbumGroupEntity -> it.name
                    else -> ""
                }
            }))

            for (item in nestedChildren) {
                when (item) {
                    is PhysicalAlbumEntity -> {
                        val img = getFirstImageOfAlbum(item)
                        if (img != null) return img
                    }
                    is AlbumGroupEntity -> {
                        val img = getFirstImageOfGroup(item.id, visited)
                        if (img != null) return img
                    }
                }
            }
            return null
        }

        for (child in directChildren) {
            if (images.size >= 4) break
            when (child) {
                is PhysicalAlbumEntity -> {
                    getFirstImageOfAlbum(child)?.let { images.add(it) }
                }
                is AlbumGroupEntity -> {
                    getFirstImageOfGroup(child.id)?.let { images.add(it) }
                }
            }
        }
        images
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            if (group.customCoverUri != null) {
                val context = LocalContext.current
                val cropData = group.customCoverCrop?.split(",")
                val scale = cropData?.get(0)?.toFloatOrNull() ?: 1f
                val offX = cropData?.get(1)?.toFloatOrNull() ?: 0f
                val offY = cropData?.get(2)?.toFloatOrNull() ?: 0f
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(group.customCoverUri)
                        .crossfade(false)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .size(180)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offX / 300f * size.width // Proportional adjustment
                        translationY = offY / 300f * size.height
                    },
                    contentScale = ContentScale.Crop
                )
            } else {
                when (representativeImages.size) {
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
                        val coverItem = representativeImages.first()
                        val context = LocalContext.current
                        val request = remember(coverItem) {
                            ImageRequest.Builder(context)
                                .data(coverItem)
                                .crossfade(false)
                                .bitmapConfig(Bitmap.Config.RGB_565)
                                .size(180)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    2 -> {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            representativeImages.take(2).forEach { coverItem ->
                                val context = LocalContext.current
                                val request = remember(coverItem) {
                                    ImageRequest.Builder(context)
                                        .data(coverItem)
                                        .crossfade(false)
                                        .bitmapConfig(Bitmap.Config.RGB_565)
                                        .size(180)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    3 -> {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Column(Modifier.weight(0.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                representativeImages.take(2).forEach { coverItem ->
                                    val context = LocalContext.current
                                    val request = remember(coverItem) {
                                        ImageRequest.Builder(context)
                                            .data(coverItem)
                                            .crossfade(false)
                                            .bitmapConfig(Bitmap.Config.RGB_565)
                                            .size(180)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = request,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            val coverItem3 = representativeImages[2]
                            val context = LocalContext.current
                            val request = remember(coverItem3) {
                                ImageRequest.Builder(context)
                                    .data(coverItem3)
                                    .crossfade(false)
                                    .bitmapConfig(Bitmap.Config.RGB_565)
                                    .size(180)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    else -> {
                        // 4+ albums: 2x2 grid
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                representativeImages.take(2).forEach { coverItem ->
                                    val context = LocalContext.current
                                    val request = remember(coverItem) {
                                        ImageRequest.Builder(context)
                                            .data(coverItem)
                                            .crossfade(false)
                                            .bitmapConfig(Bitmap.Config.RGB_565)
                                            .size(180)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = request,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                representativeImages.drop(2).take(2).forEach { coverItem ->
                                    val context = LocalContext.current
                                    val request = remember(coverItem) {
                                        ImageRequest.Builder(context)
                                            .data(coverItem)
                                            .crossfade(false)
                                            .bitmapConfig(Bitmap.Config.RGB_565)
                                            .size(180)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = request,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumItem(
    album: Album,
    isHidden: Boolean = false,
    isHideShowMode: Boolean = false,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    entity: PhysicalAlbumEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    showReorderButtons: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box {
            val context = LocalContext.current
            val coverUri = entity.customCoverUri ?: album.coverImage?.uri?.toString()
            val request = remember(coverUri) {
                if (coverUri != null) {
                    ImageRequest.Builder(context)
                        .data(coverUri)
                        .crossfade(false)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .size(180)
                        .build()
                } else null
            }
            
            val cropData = entity.customCoverCrop?.split(",")
            val scale = cropData?.get(0)?.toFloatOrNull() ?: 1f
            val offX = cropData?.get(1)?.toFloatOrNull() ?: 0f
            val offY = cropData?.get(2)?.toFloatOrNull() ?: 0f

            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .graphicsLayer {
                            if (entity.customCoverUri != null) {
                                scaleX = scale
                                scaleY = scale
                                translationX = offX / 300f * size.width
                                translationY = offY / 300f * size.height
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (selectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Reorder buttons
            if (showReorderButtons) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                             onMoveDown()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // GIF badge on cover (video icon removed)
            if (album.coverImage?.type == com.example.cgallery.data.MediaType.GIF) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GIF",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            if (isHideShowMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp))
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
            text = album.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.count}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
