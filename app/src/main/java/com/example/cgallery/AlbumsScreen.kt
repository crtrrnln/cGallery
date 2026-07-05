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
import com.example.cgallery.data.*
import kotlinx.coroutines.launch
import java.io.File

data class Album(val name: String, val displayName: String, val count: Int, val coverImage: MediaItem?)
enum class ReorderType { ROOT_ITEMS }
sealed class DisplayItem {
    data class SpecialAlbum(val name: String, val type: SpecialAlbumType) : DisplayItem()
    data class GroupItem(val group: AlbumGroupEntity) : DisplayItem()
    data class AlbumItem(val album: Album, val entity: PhysicalAlbumEntity) : DisplayItem()
}
enum class SpecialAlbumType { RECENTS, FAVOURITES }
data class RepresentativeImage(val uri: String, val crop: String? = null)

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
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val groupManager = remember { AlbumGroupManager(context) }; val physicalAlbumManager = remember { PhysicalAlbumManager(context) }
    var localSelectedAlbums by remember { mutableStateOf(setOf<String>()) }
    val selAlbums = externalSelectedAlbums ?: localSelectedAlbums
    var showCreateGroup by remember { mutableStateOf(false) }; var newGName by remember { mutableStateOf("") }
    var showCreateFolder by remember { mutableStateOf(false) }; var newFName by remember { mutableStateOf("") }
    var isHideShow by remember { mutableStateOf(false) }; var showMoveToG by remember { mutableStateOf(false) }
    var selAlbForG by remember { mutableStateOf<Album?>(null) }; var selGId by remember { mutableStateOf<Long?>(null) }
    var showMoveG by remember { mutableStateOf(false) }; var selGForMove by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    var selPGId by remember { mutableStateOf<Long?>(null) }; var showReorder by remember { mutableStateOf(false) }
    var rType by remember { mutableStateOf<ReorderType?>(null) }; var showDelGConf by remember { mutableStateOf<AlbumGroupEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val groups by groupManager.rootGroups.collectAsState(initial = emptyList())
    val allG by groupManager.allGroups.collectAsState(initial = emptyList())
    val physAlbs by physicalAlbumManager.allAlbums.collectAsState(initial = emptyList())
    val favsManager = remember { FavouritesManager(context) }
    val favIds by favsManager.favouriteIds.collectAsState(initial = emptySet())
    val visAlbs = remember(physAlbs, isHideShow) { if (isHideShow) physAlbs else physAlbs.filter { !it.isHidden } }
    val ungrouped = remember(visAlbs) { visAlbs.filter { it.groupId == null }.distinctBy { it.bucketName } }
    val albDetails = remember(ungrouped, mediaByBucket) {
        ungrouped.map { entity ->
            val imgs = mediaByBucket[entity.bucketName] ?: emptyList()
            Album(name = entity.bucketName, displayName = File(entity.bucketName).name, count = imgs.size, coverImage = imgs.firstOrNull())
        }.distinctBy { it.name }
    }
    val grouped = remember(allG, physAlbs) {
        val map = mutableMapOf<Long?, List<PhysicalAlbumEntity>>()
        map[null] = physAlbs.filter { it.groupId == null }
        allG.forEach { g -> map[g.id] = physAlbs.filter { it.groupId == g.id } }
        map
    }
    val displayItems = remember(groups, albDetails, physAlbs, selectionMode) {
        val items = mutableListOf<DisplayItem>()
        if (!selectionMode) {
            items.add(DisplayItem.SpecialAlbum("Recent", SpecialAlbumType.RECENTS))
            items.add(DisplayItem.SpecialAlbum("Favourites", SpecialAlbumType.FAVOURITES))
        }
        val roots = mutableListOf<DisplayItem>()
        groups.filter { it.parentId == null }.forEach { roots.add(DisplayItem.GroupItem(it)) }
        val relAlbs = if (selectionMode) albDetails else albDetails.filter { a -> physAlbs.find { it.bucketName == a.name }?.groupId == null }
        relAlbs.forEach { a -> physAlbs.find { it.bucketName == a.name }?.let { roots.add(DisplayItem.AlbumItem(a, it)) } }
        roots.sortedWith(compareBy({ when (it) { is DisplayItem.GroupItem -> it.group.sortOrder; is DisplayItem.AlbumItem -> it.entity.sortOrder; else -> Int.MAX_VALUE } }, { when (it) { is DisplayItem.GroupItem -> it.group.name; is DisplayItem.AlbumItem -> it.album.displayName; else -> "" } })).forEach { items.add(it) }
        items.distinctBy { when (it) { is DisplayItem.SpecialAlbum -> "special_${it.type}"; is DisplayItem.GroupItem -> "group_${it.group.id}"; is DisplayItem.AlbumItem -> "album_${it.album.name}" } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scope.launch { context.contentResolver.openInputStream(it)?.use { stream -> physicalAlbumManager.importStructure(stream.bufferedReader().readText()) } } }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val s = physicalAlbumManager.exportStructure()
                context.contentResolver.openOutputStream(it)?.use { out -> out.write(s.toByteArray()) }
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(title = { Text(if (selectionMode) "Select Destinations" else "Albums") }, navigationIcon = {
            if (selectionMode) IconButton(onClick = { onConfirmSelection(emptyList()) }) { Icon(Icons.Default.Close, "cancel") }
            else IconButton(onClick = onInboxClick) { Icon(Icons.Default.Email, "inbox") }
        }, actions = {
            if (selectionMode) {
                IconButton(onClick = { showCreateFolder = true }) { Icon(Icons.Default.Add, "new alb") }
                IconButton(onClick = { onConfirmSelection(selAlbums.toList()) }, enabled = selAlbums.isNotEmpty()) { Icon(Icons.Default.Check, "confirm") }
            } else {
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "menu") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Create Album") }, onClick = { showMenu = false; showCreateFolder = true }, leadingIcon = { Icon(Icons.Default.Add, null) })
                        DropdownMenuItem(text = { Text("Create Group") }, onClick = { showMenu = false; showCreateGroup = true }, leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) })
                        DropdownMenuItem(text = { Text(if (isHideShow) "Show All" else "Hide/Show") }, onClick = { showMenu = false; isHideShow = !isHideShow }, leadingIcon = { Icon(if (isHideShow) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) })
                        DropdownMenuItem(text = { Text("Reorder") }, onClick = { showMenu = false; showReorder = !showReorder }, leadingIcon = { Icon(Icons.Default.SwapVert, null) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Export Config") }, onClick = { showMenu = false; exportLauncher.launch("cgallery_export.json") }, leadingIcon = { Icon(Icons.Default.FileUpload, null) })
                        DropdownMenuItem(text = { Text("Import Config") }, onClick = { showMenu = false; importLauncher.launch("application/json") }, leadingIcon = { Icon(Icons.Default.FileDownload, null) })
                    }
                }
            }
        })
    }) { p ->
        Box(Modifier.fillMaxSize().padding(p)) {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 80.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(displayItems, key = { when (it) { is DisplayItem.SpecialAlbum -> "s_${it.type}"; is DisplayItem.GroupItem -> "g_${it.group.id}"; is DisplayItem.AlbumItem -> "a_${it.album.name}" } }) { item ->
                    val curIdx = displayItems.indexOf(item)
                    val canUp = curIdx > 0 && (displayItems[curIdx - 1] is DisplayItem.GroupItem || displayItems[curIdx - 1] is DisplayItem.AlbumItem)
                    val canDown = curIdx < displayItems.size - 1 && (displayItems[curIdx + 1] is DisplayItem.GroupItem || displayItems[curIdx + 1] is DisplayItem.AlbumItem)
                    
                    when (item) {
                        is DisplayItem.SpecialAlbum -> SpecialAlbumItem(name = item.name, type = item.type, images = images, favouriteIds = favIds, enabled = !selectionMode, onClick = { onSpecialAlbumClick(item.type) })
                        is DisplayItem.GroupItem -> GroupAlbumItem(group = item.group, physicalAlbums = physAlbs, mediaByBucket = mediaByBucket, allGroups = allG, albumsByGroup = { gid -> grouped[gid] ?: emptyList() }, onGroupClick = { onGroupClick(item.group.id) }, onLongClick = { if (!selectionMode) { selGForMove = item.group; showMoveG = true } }, onMoveUp = { if (canUp) scope.launch { performRootSwap(item, displayItems[curIdx - 1], physicalAlbumManager) } }, onMoveDown = { if (canDown) scope.launch { performRootSwap(item, displayItems[curIdx + 1], physicalAlbumManager) } }, showSortControls = showReorder)
                        is DisplayItem.AlbumItem -> AlbumItem(album = item.album, isHidden = item.entity.isHidden, isHideShowMode = isHideShow, selectionMode = selectionMode, isSelected = selAlbums.contains(item.album.name), entity = item.entity, onClick = { if (selectionMode) onToggleAlbumSelection(item.album.name) else if (isHideShow) onToggleAlbumVisibility(item.album.name) else onAlbumClick(item.album) }, onLongClick = { if (!selectionMode && !isHideShow) { selAlbForG = item.album; showMoveToG = true } }, onMoveUp = { if (canUp) scope.launch { performRootSwap(item, displayItems[curIdx - 1], physicalAlbumManager) } }, onMoveDown = { if (canDown) scope.launch { performRootSwap(item, displayItems[curIdx + 1], physicalAlbumManager) } }, showSortControls = showReorder)
                    }
                }
            }
            if (showReorder) {
                Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 4.dp) {
                    Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Reorder Mode", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(16.dp))
                        TextButton(onClick = { showReorder = false }) { Text("Done") }
                    }
                }
            }
        }
    }
    if (showCreateGroup) {
        AlertDialog(onDismissRequest = { showCreateGroup = false }, title = { Text("new group") }, text = { TextField(value = newGName, onValueChange = { newGName = it }, placeholder = { Text("name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (newGName.isNotBlank()) { scope.launch { groupManager.createGroup(newGName); newGName = ""; showCreateGroup = false } } }) { Text("create") } }, dismissButton = { TextButton(onClick = { showCreateGroup = false }) { Text("cancel") } })
    }
    if (showCreateFolder) {
        AlertDialog(onDismissRequest = { showCreateFolder = false }, title = { Text("new album") }, text = { TextField(value = newFName, onValueChange = { newFName = it }, placeholder = { Text("name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (newFName.isNotBlank()) { onCreateFolder(newFName); newFName = ""; showCreateFolder = false } }) { Text("create") } }, dismissButton = { TextButton(onClick = { showCreateFolder = false }) { Text("cancel") } })
    }
    if (showMoveToG && selAlbForG != null) {
        AlertDialog(onDismissRequest = { showMoveToG = false }, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Organise Album"); IconButton(onClick = { scope.launch { physicalAlbumManager.deleteAlbum(selAlbForG!!.name); showMoveToG = false } }) { Icon(Icons.Default.Delete, "delete", tint = MaterialTheme.colorScheme.error) } } }, text = { Column { Text("Move \"${selAlbForG?.displayName}\" to:"); Spacer(Modifier.height(8.dp)); Box(Modifier.heightIn(max = 400.dp)) { LazyColumn { item { Row(Modifier.fillMaxWidth().clickable { selGId = null }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selGId == null, { selGId = null }); Spacer(Modifier.width(8.dp)); Text("No Group (Root)") } }; items(allG.filter { it.parentId == null }) { g -> GroupSelectionRow(g, allG, selGId, { selGId = it }, 0) } } } } }, confirmButton = { TextButton(onClick = { scope.launch { physicalAlbumManager.moveAlbumToGroup(selAlbForG!!.name, selGId); showMoveToG = false } }) { Text("move") } })
    }
    if (showMoveG && selGForMove != null) {
        AlertDialog(onDismissRequest = { showMoveG = false }, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Move Group"); IconButton(onClick = { showDelGConf = selGForMove; showMoveG = false }) { Icon(Icons.Default.Delete, "del", tint = MaterialTheme.colorScheme.error) } } }, text = { Column { Text("Move \"${selGForMove?.name}\" into:"); Spacer(Modifier.height(8.dp)); Box(Modifier.heightIn(max = 400.dp)) { LazyColumn { item { Row(Modifier.fillMaxWidth().clickable { selPGId = null }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selPGId == null, { selPGId = null }); Spacer(Modifier.width(8.dp)); Text("Root") } }; items(allG.filter { it.id != selGForMove?.id && it.parentId == null && !isDescendant(it.id, selGForMove!!.id) }) { g -> GroupSelectionRow(g, allG, selPGId, { selPGId = it }, 0) } } } } }, confirmButton = { TextButton(onClick = { scope.launch { groupManager.moveGroup(selGForMove!!.id, selPGId); showMoveG = false } }) { Text("move") } })
    }
    if (showDelGConf != null) {
        AlertDialog(onDismissRequest = { showDelGConf = null }, title = { Text("delete?") }, text = { Text("Delete group \"${showDelGConf?.name}\"?") }, confirmButton = { TextButton(onClick = { scope.launch { groupManager.deleteGroup(showDelGConf!!); showDelGConf = null } }) { Text("delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { showDelGConf = null }) { Text("cancel") } })
    }
}
suspend fun performRootSwap(item1: DisplayItem, item2: DisplayItem, physicalAlbumManager: PhysicalAlbumManager) {
    val order1 = when (item1) { is DisplayItem.GroupItem -> item1.group.sortOrder; is DisplayItem.AlbumItem -> item1.entity.sortOrder; else -> return }
    val order2 = when (item2) { is DisplayItem.GroupItem -> item2.group.sortOrder; is DisplayItem.AlbumItem -> item2.entity.sortOrder; else -> return }
    when (item1) {
        is DisplayItem.GroupItem -> physicalAlbumManager.updateGroupSortOrder(item1.group.id, order2)
        is DisplayItem.AlbumItem -> physicalAlbumManager.updateAlbumSortOrder(item1.entity.id, order2)
        else -> {}
    }
    when (item2) {
        is DisplayItem.GroupItem -> physicalAlbumManager.updateGroupSortOrder(item2.group.id, order1)
        is DisplayItem.AlbumItem -> physicalAlbumManager.updateAlbumSortOrder(item2.entity.id, order1)
        else -> {}
    }
}
fun isDescendant(gid: Long, aid: Long) = false 
@Composable
fun GroupSelectionRow(group: AlbumGroupEntity, allG: List<AlbumGroupEntity>, selId: Long?, onSelect: (Long) -> Unit, depth: Int = 0) {
    Column { Row(Modifier.fillMaxWidth().clickable { onSelect(group.id) }.padding(start = (depth * 16 + 12).dp, top = 12.dp, bottom = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selId == group.id, { onSelect(group.id) }); Spacer(Modifier.width(8.dp)); Text(group.name) }; allG.filter { it.parentId == group.id }.forEach { GroupSelectionRow(it, allG, selId, onSelect, depth + 1) } }
}
@Composable
fun SpecialAlbumItem(name: String, type: SpecialAlbumType, images: List<MediaItem>, favouriteIds: Set<Long>, enabled: Boolean = true, onClick: () -> Unit) {
    val img = remember(type, images, favouriteIds) { if (type == SpecialAlbumType.RECENTS) images.firstOrNull() else images.find { it.id in favouriteIds } }
    val count = remember(type, images, favouriteIds) { if (type == SpecialAlbumType.RECENTS) images.size else favouriteIds.size }
    Column(modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.graphicsLayer { alpha = 0.38f })) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (img != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(img.uri).crossfade(true).size(300).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(if (type == SpecialAlbumType.RECENTS) Icons.Default.History else Icons.Default.Favorite, null, Modifier.size(48.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
        }
        Spacer(Modifier.height(8.dp)); Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun GroupAlbumItem(group: AlbumGroupEntity, physicalAlbums: List<PhysicalAlbumEntity>, mediaByBucket: Map<String, List<MediaItem>>, allGroups: List<AlbumGroupEntity>, albumsByGroup: (Long?) -> List<PhysicalAlbumEntity>, onGroupClick: () -> Unit, onLongClick: () -> Unit, onMoveUp: () -> Unit = {}, onMoveDown: () -> Unit = {}, showSortControls: Boolean = false) {
    val childAlbs = remember(group, physicalAlbums) { albumsByGroup(group.id) }
    val childGroups = remember(group, allGroups) { allGroups.filter { it.parentId == group.id } }
    fun getImg(alb: PhysicalAlbumEntity): RepresentativeImage? {
        val uri = alb.customCoverUri ?: mediaByBucket[alb.bucketName]?.firstOrNull()?.uri?.toString() ?: return null
        return RepresentativeImage(uri, alb.customCoverCrop)
    }
    fun getGImg(gid: Long, vis: MutableSet<Long> = mutableSetOf()): RepresentativeImage? {
        if (!vis.add(gid)) return null
        val g = allGroups.find { it.id == gid }
        if (g?.customCoverUri != null) return RepresentativeImage(g.customCoverUri!!, g.customCoverCrop)
        albumsByGroup(gid).sortedBy { it.sortOrder }.forEach { a -> getImg(a)?.let { return it } }
        allGroups.filter { it.parentId == gid }.sortedBy { it.sortOrder }.forEach { c -> getGImg(c.id, vis)?.let { return it } }
        return null
    }
    val repImgs = remember(group, childAlbs, childGroups, mediaByBucket, allGroups) {
        if (group.customCoverUri != null) {
            listOf(RepresentativeImage(group.customCoverUri!!, group.customCoverCrop))
        } else {
            val items = (childAlbs.map { it as Any } + childGroups.map { it as Any })
                .sortedWith(compareBy({ when(it) { is PhysicalAlbumEntity -> it.sortOrder; is AlbumGroupEntity -> it.sortOrder; else -> Int.MAX_VALUE } }, { when(it) { is PhysicalAlbumEntity -> File(it.bucketName).name; is AlbumGroupEntity -> it.name; else -> "" } }))
                .take(4)
            items.mapNotNull { item ->
                when(item) {
                    is PhysicalAlbumEntity -> getImg(item)
                    is AlbumGroupEntity -> getGImg(item.id)
                    else -> null
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onGroupClick, onLongClick = onLongClick)) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (repImgs.isNotEmpty()) {
                if (repImgs.size == 1) {
                    AsyncImage(repImgs[0].uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    when (repImgs.size) {
                        2 -> Row(Modifier.fillMaxSize()) { AsyncImage(repImgs[0].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); AsyncImage(repImgs[1].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop) }
                        3 -> Row(Modifier.fillMaxSize()) { AsyncImage(repImgs[0].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); Column(Modifier.weight(1f).fillMaxHeight()) { AsyncImage(repImgs[1].uri, null, Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop); Spacer(Modifier.height(1.dp)); AsyncImage(repImgs[2].uri, null, Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop) } }
                        else -> Column(Modifier.fillMaxSize()) { Row(Modifier.weight(1f)) { AsyncImage(repImgs[0].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); AsyncImage(repImgs[1].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop) }; Spacer(Modifier.height(1.dp)); Row(Modifier.weight(1f)) { AsyncImage(repImgs[2].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); AsyncImage(repImgs[3].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop) } }
                    }
                }
            } else Icon(Icons.Default.Folder, null, Modifier.size(48.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            if (showSortControls) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.5f)).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) { IconButton(onMoveUp, Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; IconButton(onMoveDown, Modifier.size(32.dp)) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) } }
        }
        Spacer(Modifier.height(8.dp)); Text(group.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${childAlbs.size} albums", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun AlbumItem(album: Album, isHidden: Boolean, isHideShowMode: Boolean, selectionMode: Boolean, isSelected: Boolean, entity: PhysicalAlbumEntity, onClick: () -> Unit, onLongClick: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, showSortControls: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).graphicsLayer { alpha = if (isHidden && !isHideShowMode) 0.5f else 1f }) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val model = entity.customCoverUri ?: album.coverImage?.uri
            if (model != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(model).crossfade(true).size(300).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.PhotoAlbum, null, Modifier.size(48.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            if (isHideShowMode) { Box(Modifier.fillMaxSize().background(if (isHidden) Color.Black.copy(0.4f) else Color.Transparent)); Icon(if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, Modifier.align(Alignment.Center).size(32.dp), tint = Color.White) }
            if (selectionMode) Checkbox(isSelected, { onClick() }, Modifier.align(Alignment.TopEnd))
            if (showSortControls) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.5f)).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) { IconButton(onMoveUp, Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; IconButton(onMoveDown, Modifier.size(32.dp)) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) } }
        }
        Spacer(Modifier.height(8.dp)); Text(album.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${album.count} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
