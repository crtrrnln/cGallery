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
import androidx.compose.ui.draw.clipToBounds
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
    val name: String, 
    val displayName: String,
    val count: Int,
    val coverImage: MediaItem?
)

enum class ReorderType {
    ROOT_ITEMS
}

sealed class DisplayItem {
    data class SpecialAlbum(val name: String, val type: SpecialAlbumType) : DisplayItem()
    data class GroupItem(val group: AlbumGroupEntity) : DisplayItem()
    data class AlbumItem(val album: Album, val entity: PhysicalAlbumEntity) : DisplayItem()
}

enum class SpecialAlbumType {
    RECENTS,
    FAVOURITES
}

data class RepresentativeImage(
    val uri: String,
    val crop: String? = null
)

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
        if (!selectionMode) {
            items.add(DisplayItem.SpecialAlbum("Recent", SpecialAlbumType.RECENTS))
            items.add(DisplayItem.SpecialAlbum("Favourites", SpecialAlbumType.FAVOURITES))
        }

        val rootItems = mutableListOf<DisplayItem>()
        
        groups.filter { it.parentId == null }.forEach { group ->
            rootItems.add(DisplayItem.GroupItem(group))
        }

        val relevantAlbums = if (selectionMode) albumsWithDetails else albumsWithDetails.filter { album ->
            physicalAlbums.find { it.bucketName == album.name }?.groupId == null
        }
        
        relevantAlbums.forEach { album ->
            val entity = physicalAlbums.find { it.bucketName == album.name }
            if (entity != null) {
                rootItems.add(DisplayItem.AlbumItem(album, entity))
            }
        }

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

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
                        IconButton(onClick = { 
                            showCreateFolderDialog = true 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Create Album")
                        }
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
                                    leadingIcon = { Icon(if (isHideShowMode) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
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
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Export Config") },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            val structure = physicalAlbumManager.exportStructure()
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(android.content.Intent.EXTRA_TEXT, structure)
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "cGallery Structure Export")
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Save Export"))
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Config") },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch("application/json")
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
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
                            enabled = !selectionMode,
                            onClick = { onSpecialAlbumClick(item.type) }
                        )
                    }
                    is DisplayItem.GroupItem -> {
                        GroupAlbumItem(
                            group = item.group,
                            physicalAlbums = physicalAlbums,
                            mediaByBucket = mediaByBucket,
                            allGroups = allGroups,
                            albumsByGroup = { gid -> groupedAlbums[gid] ?: emptyList() },
                            onGroupClick = { onGroupClick(item.group.id) },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectedGroupForMove = item.group
                                    showMoveGroupDialog = true
                                }
                            }
                        )
                    }
                    is DisplayItem.AlbumItem -> {
                        val isSelected = selectedAlbumsForAction.contains(item.album.name)
                        AlbumItem(
                            album = item.album,
                            isHidden = item.entity.isHidden,
                            isHideShowMode = isHideShowMode,
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            entity = item.entity,
                            onClick = {
                                if (selectionMode) {
                                    onToggleAlbumSelection(item.album.name)
                                } else if (isHideShowMode) {
                                    onToggleAlbumVisibility(item.album.name)
                                } else {
                                    onAlbumClick(item.album)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode && !isHideShowMode) {
                                    selectedAlbumForGroup = item.album
                                    showMoveToGroupDialog = true
                                }
                            },
                            onMoveUp = {
                                scope.launch {
                                    val index = physicalAlbums.indexOf(item.entity)
                                    if (index > 0) {
                                        val prev = physicalAlbums[index - 1]
                                        physicalAlbumManager.updateAlbumSortOrder(item.entity.id, prev.sortOrder)
                                        physicalAlbumManager.updateAlbumSortOrder(prev.id, item.entity.sortOrder)
                                    }
                                }
                            },
                            onMoveDown = {
                                scope.launch {
                                    val index = physicalAlbums.indexOf(item.entity)
                                    if (index < physicalAlbums.size - 1) {
                                        val next = physicalAlbums[index + 1]
                                        physicalAlbumManager.updateAlbumSortOrder(item.entity.id, next.sortOrder)
                                        physicalAlbumManager.updateAlbumSortOrder(next.id, item.entity.sortOrder)
                                    }
                                }
                            },
                            showSortControls = showReorderDialog && reorderType == ReorderType.ROOT_ITEMS
                        )
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("New Group") },
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
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to Group")
                    IconButton(onClick = {
                        scope.launch {
                            physicalAlbumManager.moveAlbumToGroup(selectedAlbumForGroup!!.name, null)
                            showMoveToGroupDialog = false
                            selectedAlbumForGroup = null
                        }
                    }) {
                        Icon(Icons.Default.Unarchive, contentDescription = "Remove from Group")
                    }
                }
            },
            text = {
                Column {
                    Text("Move \"${selectedAlbumForGroup?.displayName}\" into:")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            items(allGroups) { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGroupId = group.id
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedGroupId == group.id,
                                        onClick = { selectedGroupId = group.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(group.name)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedGroupId != null) {
                            scope.launch {
                                physicalAlbumManager.moveAlbumToGroup(selectedAlbumForGroup!!.name, selectedGroupId)
                                showMoveToGroupDialog = false
                                selectedAlbumForGroup = null
                                selectedGroupId = null
                            }
                        }
                    },
                    enabled = selectedGroupId != null
                ) {
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
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedParentGroupId = null
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedParentGroupId == null,
                                        onClick = { selectedParentGroupId = null }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Main List (Root)")
                                }
                            }
                            
                            items(allGroups.filter { it.id != selectedGroupForMove?.id && !isDescendant(it.id, selectedGroupForMove!!.id) }) { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedParentGroupId = group.id
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedParentGroupId == group.id,
                                        onClick = { selectedParentGroupId = group.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(group.name)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            groupManager.moveGroup(selectedGroupForMove!!.id, selectedParentGroupId)
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
            title = { Text("Delete Group?") },
            text = { Text("This will delete the group \"${showDeleteGroupConfirmation?.name}\". Albums inside will be moved to the main list. Actual photos won't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        groupManager.deleteGroup(showDeleteGroupConfirmation!!)
                        showDeleteGroupConfirmation = null
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReorderDialog) {
        AlertDialog(
            onDismissRequest = { 
                showReorderDialog = false 
                reorderType = null
            },
            title = { Text("Reorder Items") },
            text = { Text("Use the arrows to reorder. Changes are saved automatically.") },
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

fun isDescendant(groupId: Long, possibleAncestorId: Long): Boolean {
    // Basic check for cycles, this would need group entities to properly implement
    return false 
}

@Composable
fun GroupSelectionRow(
    group: AlbumGroupEntity,
    allGroups: List<AlbumGroupEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    depth: Int = 0
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(group.id) }
                .padding(start = (depth * 16 + 12).dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedId == group.id,
                onClick = { onSelect(group.id) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(group.name)
        }
        
        allGroups.filter { it.parentId == group.id }.forEach { child ->
            GroupSelectionRow(child, allGroups, selectedId, onSelect, depth + 1)
        }
    }
}

@Composable
fun SpecialAlbumItem(
    name: String,
    type: SpecialAlbumType,
    images: List<MediaItem>,
    favoriteIds: Set<Long>,
    enabled: Boolean = true,
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
            SpecialAlbumType.FAVOURITES -> favoriteIds.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.graphicsLayer { alpha = 0.38f })
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
                        .crossfade(true)
                        .size(300)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = if (type == SpecialAlbumType.RECENTS) Icons.Default.History else Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$itemCount items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GroupAlbumItem(
    group: AlbumGroupEntity,
    physicalAlbums: List<PhysicalAlbumEntity>,
    mediaByBucket: Map<String, List<MediaItem>>,
    allGroups: List<AlbumGroupEntity>,
    albumsByGroup: (Long?) -> List<PhysicalAlbumEntity>,
    onGroupClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val childAlbums = remember(group, physicalAlbums) { albumsByGroup(group.id) }
    
    fun getFirstImageOfAlbum(album: PhysicalAlbumEntity): RepresentativeImage? {
        val items = mediaByBucket[album.bucketName]
        return items?.firstOrNull()?.let { RepresentativeImage(it.uri.toString(), album.customCoverCrop) }
    }

    fun getFirstImageOfGroup(groupId: Long, visited: MutableSet<Long> = mutableSetOf()): RepresentativeImage? {
        if (!visited.add(groupId)) return null
        
        val albums = albumsByGroup(groupId)
        albums.forEach { album ->
            val img = getFirstImageOfAlbum(album)
            if (img != null) return img
        }
        
        val children = allGroups.filter { it.parentId == groupId }
        children.forEach { child ->
            val img = getFirstImageOfGroup(child.id, visited)
            if (img != null) return img
        }
        
        return null
    }

    val representativeImages = remember(group, physicalAlbums, mediaByBucket, allGroups) {
        val images = mutableListOf<RepresentativeImage>()
        
        childAlbums.forEach { album ->
            if (images.size >= 4) return@forEach
            val img = getFirstImageOfAlbum(album)
            if (img != null) images.add(img)
        }
        
        if (images.size < 4) {
            val children = allGroups.filter { it.parentId == group.id }
            children.forEach { child ->
                if (images.size >= 4) return@forEach
                val img = getFirstImageOfGroup(child.id)
                if (img != null) images.add(img)
            }
        }
        images
    }

    val totalItems = remember(group, physicalAlbums, mediaByBucket) {
        var count = 0
        childAlbums.forEach { count += mediaByBucket[it.bucketName]?.size ?: 0 }
        count
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onGroupClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (group.customCoverUri != null) {
                AsyncImage(
                    model = group.customCoverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (representativeImages.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (representativeImages.size) {
                        1 -> {
                            AsyncImage(
                                model = representativeImages[0].uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        2 -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = representativeImages[0].uri,
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(1.dp))
                                AsyncImage(
                                    model = representativeImages[1].uri,
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        3 -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = representativeImages[0].uri,
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(1.dp))
                                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AsyncImage(
                                        model = representativeImages[1].uri,
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    AsyncImage(
                                        model = representativeImages[2].uri,
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = representativeImages[0].uri,
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    AsyncImage(
                                        model = representativeImages[1].uri,
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Row(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = representativeImages[2].uri,
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    AsyncImage(
                                        model = representativeImages[3].uri,
                                        contentDescription = null,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${childAlbums.size} albums",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlbumItem(
    album: Album,
    isHidden: Boolean,
    isHideShowMode: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    entity: PhysicalAlbumEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    showSortControls: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .graphicsLayer {
                alpha = if (isHidden && !isHideShowMode) 0.5f else 1f
            }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (entity.customCoverUri != null) {
                AsyncImage(
                    model = entity.customCoverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (album.coverImage != null) {
                val context = LocalContext.current
                val request = remember(album.coverImage.uri) {
                    ImageRequest.Builder(context)
                        .data(album.coverImage.uri)
                        .crossfade(true)
                        .size(300)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoAlbum,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (isHideShowMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isHidden) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                )
                Icon(
                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                    tint = Color.White
                )
            }

            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            if (showSortControls) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.displayName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.count} items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
