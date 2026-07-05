package com.example.cgallery
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.*
import kotlinx.coroutines.launch
import java.io.File

sealed class GroupDisplayItem {
    data class GroupItem(val group: AlbumGroupEntity) : GroupDisplayItem()
    data class AlbumItem(val album: Album, val entity: PhysicalAlbumEntity) : GroupDisplayItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long, images: List<MediaItem>,
    onAlbumClick: (String) -> Unit, onGroupClick: (Long) -> Unit = {},
    onChangeCover: () -> Unit = {}, onCreateFolder: (String) -> Unit = {},
    selectionMode: Boolean = false, selectedAlbums: Set<String> = emptySet(),
    onToggleAlbumSelection: (String) -> Unit = {}, onConfirmSelection: (List<String>) -> Unit = {},
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current; val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }; val physicalAlbumManager = remember { PhysicalAlbumManager(context) }
    var showMoveToG by remember { mutableStateOf(false) }; var selAlbForG by remember { mutableStateOf<Album?>(null) }
    var selGId by remember { mutableStateOf<Long?>(null) }; var isReorder by remember { mutableStateOf(false) }
    var showCreateG by remember { mutableStateOf(false) }; var newGName by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }; var showCreateF by remember { mutableStateOf(false) }
    var newFName by remember { mutableStateOf("") }; var selGForMove by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    var showMoveG by remember { mutableStateOf(false) }; var selPGId by remember { mutableStateOf<Long?>(null) }
    var showDelGConf by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    val group by groupManager.getGroupById(groupId).collectAsState(null)
    val albumsInG by groupManager.getAlbumsByGroup(groupId).collectAsState(emptyList())
    val allG by groupManager.allGroups.collectAsState(emptyList())
    val physAlbs by physicalAlbumManager.allAlbums.collectAsState(emptyList())
    val bucketMap = remember(images) { images.groupBy { i -> try { File(i.fullPath).parent ?: "unknown" } catch (e: Exception) { "unknown" } } }
    val childG = remember(allG, groupId) { allG.filter { it.parentId == groupId } }
    val displayItems = remember(childG, albumsInG, bucketMap) {
        val list = mutableListOf<GroupDisplayItem>()
        childG.forEach { list.add(GroupDisplayItem.GroupItem(it)) }
        albumsInG.forEach { entity ->
            val imgs = bucketMap[entity.bucketName] ?: emptyList()
            list.add(GroupDisplayItem.AlbumItem(Album(entity.bucketName, File(entity.bucketName).name, imgs.size, imgs.firstOrNull()), entity))
        }
        list.sortedWith(compareBy({ when (it) { is GroupDisplayItem.GroupItem -> it.group.sortOrder; is GroupDisplayItem.AlbumItem -> it.entity.sortOrder } }, { when (it) { is GroupDisplayItem.GroupItem -> it.group.name; is GroupDisplayItem.AlbumItem -> it.album.displayName } }))
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        CenterAlignedTopAppBar(title = { if (selectionMode) Text("${selectedAlbums.size} Selected") else Text(group?.name ?: "Group", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onBack) { Icon(if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, "back") } },
            actions = {
                if (selectionMode) {
                    IconButton({ showCreateF = true }) { Icon(Icons.Default.Add, "new") }
                    IconButton({ onConfirmSelection(selectedAlbums.toList()) }, enabled = selectedAlbums.isNotEmpty()) { Icon(Icons.Default.Done, "ok") }
                } else {
                    Box {
                        IconButton({ showMenu = true }) { Icon(Icons.Default.MoreVert, "more") }
                        DropdownMenu(showMenu, { showMenu = false }) {
                            DropdownMenuItem({ Text("Change Cover") }, { showMenu = false; onChangeCover() }, leadingIcon = { Icon(Icons.Default.Image, null) })
                            DropdownMenuItem({ Text("Create Album") }, { showMenu = false; showCreateF = true }, leadingIcon = { Icon(Icons.Default.Add, null) })
                            DropdownMenuItem({ Text("Create Group") }, { showMenu = false; showCreateG = true }, leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) })
                            DropdownMenuItem({ Text("Reorder") }, { showMenu = false; isReorder = !isReorder }, leadingIcon = { Icon(Icons.Default.SwapVert, null) })
                        }
                    }
                }
            })
    }) { p ->
        Box(Modifier.fillMaxSize().padding(p)) {
            if (displayItems.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("empty group", style = MaterialTheme.typography.bodyLarge) } } else {
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 80.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(displayItems, key = { when (it) { is GroupDisplayItem.GroupItem -> "g_${it.group.id}"; is GroupDisplayItem.AlbumItem -> "a_${it.album.name}" } }) { item ->
                        val curIdx = displayItems.indexOf(item)
                        val canUp = curIdx > 0; val canDown = curIdx < (displayItems.size - 1)
                        when (item) {
                            is GroupDisplayItem.GroupItem -> GroupAlbumItem(group = item.group, physicalAlbums = physAlbs, mediaByBucket = bucketMap, allGroups = allG, albumsByGroup = { gid -> physAlbs.filter { it.groupId == gid } }, onGroupClick = { onGroupClick(item.group.id) }, onLongClick = { selGForMove = item.group; showMoveG = true }, onMoveUp = { if (canUp) scope.launch { performSwap(item, displayItems[curIdx - 1], physicalAlbumManager) } }, onMoveDown = { if (canDown) scope.launch { performSwap(item, displayItems[curIdx + 1], physicalAlbumManager) } }, showSortControls = isReorder)
                            is GroupDisplayItem.AlbumItem -> AlbumItem(album = item.album, isHidden = item.entity.isHidden, isHideShowMode = false, selectionMode = selectionMode, isSelected = item.album.name in selectedAlbums, entity = item.entity, onClick = { if (selectionMode) onToggleAlbumSelection(item.album.name) else onAlbumClick(item.album.name) }, onLongClick = { if (!selectionMode) { selAlbForG = item.album; showMoveToG = true } }, onMoveUp = { if (canUp) scope.launch { performSwap(item, displayItems[curIdx - 1], physicalAlbumManager) } }, onMoveDown = { if (canDown) scope.launch { performSwap(item, displayItems[curIdx + 1], physicalAlbumManager) } }, showSortControls = isReorder)
                        }
                    }
                }
            }
            if (isReorder) {
                Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 4.dp) {
                    Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Reorder Mode", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.width(16.dp)); TextButton({ isReorder = false }) { Text("Done") }
                    }
                }
            }
        }
    }
    if (showCreateF) { AlertDialog({ showCreateF = false }, title = { Text("new album") }, text = { TextField(newFName, { newFName = it }, placeholder = { Text("name") }, singleLine = true) }, confirmButton = { TextButton({ if (newFName.isNotBlank()) { onCreateFolder(newFName); newFName = ""; showCreateF = false } }) { Text("create") } }, dismissButton = { TextButton({ showCreateF = false }) { Text("cancel") } }) }
    if (showMoveG && selGForMove != null) { AlertDialog({ showMoveG = false; selGForMove = null }, title = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Move Group"); IconButton({ showDelGConf = selGForMove; showMoveG = false }) { Icon(Icons.Default.Delete, "del", tint = MaterialTheme.colorScheme.error) } } }, text = { Column { Text("Move \"${selGForMove?.name}\" into:"); Spacer(Modifier.height(8.dp)); Box(Modifier.heightIn(max = 400.dp)) { LazyColumn { item { Row(Modifier.fillMaxWidth().clickable { selPGId = null }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selPGId == null, { selPGId = null }); Spacer(Modifier.width(8.dp)); Text("Root") } }; val otherG = allG.filter { it.id != selGForMove?.id && it.parentId == null }; items(otherG) { g -> GroupDetailSelectionRow(g, allG, selPGId, { selPGId = it }, 0) } } } } }, confirmButton = { TextButton({ scope.launch { selGForMove?.let { g -> if (selPGId != g.id) groupManager.moveGroup(g.id, selPGId) }; showMoveG = false; selGForMove = null; selPGId = null } }) { Text("move") } }) }
    if (showDelGConf != null) { AlertDialog({ showDelGConf = null }, title = { Text("delete?") }, text = { Text("Sure? albums and sub-groups will go to root.") }, confirmButton = { TextButton({ scope.launch { showDelGConf?.let { g -> groupManager.deleteGroup(g.id) }; showDelGConf = null; selGForMove = null } }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("delete") } }) }
    if (showMoveToG && selAlbForG != null) { AlertDialog({ showMoveToG = false; selAlbForG = null }, title = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Organise Album"); IconButton(onClick = { scope.launch { physicalAlbumManager.deleteAlbum(selAlbForG!!.name); showMoveToG = false } }) { Icon(Icons.Default.Delete, "delete", tint = MaterialTheme.colorScheme.error) } } }, text = { Column { Text("Move \"${selAlbForG?.displayName}\" to:"); Spacer(Modifier.height(8.dp)); Box(Modifier.heightIn(max = 400.dp)) { LazyColumn { item { Row(Modifier.fillMaxWidth().clickable { selGId = null }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selGId == null, { selGId = null }); Spacer(Modifier.width(8.dp)); Text("No Group (Root)") } }; val rootG = allG.filter { it.parentId == null }; items(rootG) { g -> GroupDetailSelectionRow(g, allG, selGId, { selGId = it }, 0) } } } } }, confirmButton = { TextButton({ scope.launch { selAlbForG?.let { a -> physicalAlbumManager.moveAlbumToGroup(a.name, selGId) }; showMoveToG = false; selAlbForG = null; selGId = null } }) { Text("move") } }) }
    if (showCreateG) { AlertDialog({ showCreateG = false }, title = { Text("new group") }, text = { TextField(newGName, { newGName = it }, label = { Text("name") }, singleLine = true) }, confirmButton = { TextButton({ scope.launch { if (newGName.isNotBlank()) { val res = groupManager.createGroup(newGName, groupId); if (res > 0) { newGName = ""; showCreateG = false } } } }, enabled = newGName.isNotBlank()) { Text("create") } }) }
}
@Composable
fun GroupDetailSelectionRow(group: AlbumGroupEntity, allG: List<AlbumGroupEntity>, selId: Long?, onSelect: (Long) -> Unit, indent: Int) {
    val children = remember(allG, group.id) { allG.filter { it.parentId == group.id } }
    Row(Modifier.fillMaxWidth().clickable { onSelect(group.id) }.padding(start = (8 + indent * 24).dp, top = 4.dp, bottom = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selId == group.id, { onSelect(group.id) }); Spacer(Modifier.width(8.dp)); Text(group.name) }
    children.forEach { c -> GroupDetailSelectionRow(c, allG, selId, onSelect, indent + 1) }
}
suspend fun performSwap(item1: GroupDisplayItem, item2: GroupDisplayItem, physicalAlbumManager: PhysicalAlbumManager) {
    val order1 = when (item1) { is GroupDisplayItem.GroupItem -> item1.group.sortOrder; is GroupDisplayItem.AlbumItem -> item1.entity.sortOrder }
    val order2 = when (item2) { is GroupDisplayItem.GroupItem -> item2.group.sortOrder; is GroupDisplayItem.AlbumItem -> item2.entity.sortOrder }
    when (item1) {
        is GroupDisplayItem.GroupItem -> physicalAlbumManager.updateGroupSortOrder(item1.group.id, order2)
        is GroupDisplayItem.AlbumItem -> physicalAlbumManager.updateAlbumSortOrder(item1.entity.id, order2)
    }
    when (item2) {
        is GroupDisplayItem.GroupItem -> physicalAlbumManager.updateGroupSortOrder(item2.group.id, order1)
        is GroupDisplayItem.AlbumItem -> physicalAlbumManager.updateAlbumSortOrder(item2.entity.id, order1)
    }
}
