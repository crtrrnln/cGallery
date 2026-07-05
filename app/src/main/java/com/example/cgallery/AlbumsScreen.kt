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
    val favsManager = remember { FavoritesManager(context) }
    val favIds by favsManager.favoriteIds.collectAsState(initial = emptySet())
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
                        DropdownMenuItem(text = { Text("Reorder") }, onClick = { showMenu = false; rType = ReorderType.ROOT_ITEMS; showReorder = true }, leadingIcon = { Icon(Icons.Default.SwapVert, null) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Export Config") }, onClick = { showMenu = false; scope.launch {
                            val s = physicalAlbumManager.exportStructure(); val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "application/json"; putExtra(android.content.Intent.EXTRA_TEXT, s); putExtra(android.content.Intent.EXTRA_SUBJECT, "cGallery Export") }
                            context.startActivity(android.content.Intent.createChooser(i, "save it"))
                        } }, leadingIcon = { Icon(Icons.Default.FileUpload, null) })
                        DropdownMenuItem(text = { Text("Import Config") }, onClick = { showMenu = false; importLauncher.launch("application/json") }, leadingIcon = { Icon(Icons.Default.FileDownload, null) })
                    }
                }
            }
        })
    }) { p ->
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(displayItems, key = { when (it) { is DisplayItem.SpecialAlbum -> "s_${it.type}"; is DisplayItem.GroupItem -> "g_${it.group.id}"; is DisplayItem.AlbumItem -> "a_${it.album.name}" } }) { item ->
                when (item) {
                    is DisplayItem.SpecialAlbum -> SpecialAlbumItem(name = item.name, type = item.type, images = images, favoriteIds = favIds, enabled = !selectionMode, onClick = { onSpecialAlbumClick(item.type) })
                    is DisplayItem.GroupItem -> GroupAlbumItem(group = item.group, physicalAlbums = physAlbs, mediaByBucket = mediaByBucket, allGroups = allG, albumsByGroup = { gid -> grouped[gid] ?: emptyList() }, onGroupClick = { onGroupClick(item.group.id) }, onLongClick = { if (!selectionMode) { selGForMove = item.group; showMoveG = true } })
                    is DisplayItem.AlbumItem -> AlbumItem(album = item.album, isHidden = item.entity.isHidden, isHideShowMode = isHideShow, selectionMode = selectionMode, isSelected = selAlbums.contains(item.album.name), entity = item.entity, onClick = { if (selectionMode) onToggleAlbumSelection(item.album.name) else if (isHideShow) onToggleAlbumVisibility(item.album.name) else onAlbumClick(item.album) }, onLongClick = { if (!selectionMode && !isHideShow) { selAlbForG = item.album; showMoveToG = true } }, onMoveUp = { scope.launch { val idx = physAlbs.indexOf(item.entity); if (idx > 0) { val prev = physAlbs[idx - 1]; physicalAlbumManager.updateAlbumSortOrder(item.entity.id, prev.sortOrder); physicalAlbumManager.updateAlbumSortOrder(prev.id, item.entity.sortOrder) } } }, onMoveDown = { scope.launch { val idx = physAlbs.indexOf(item.entity); if (idx < physAlbs.size - 1) { val next = physAlbs[idx + 1]; physicalAlbumManager.updateAlbumSortOrder(item.entity.id, next.sortOrder); physicalAlbumManager.updateAlbumSortOrder(next.id, item.entity.sortOrder) } } }, showSortControls = showReorder && rType == ReorderType.ROOT_ITEMS)
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
        AlertDialog(onDismissRequest = { showMoveToG = false }, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Add to Group"); IconButton(onClick = { scope.launch { physicalAlbumManager.moveAlbumToGroup(selAlbForG!!.name, null); showMoveToG = false } }) { Icon(Icons.Default.Unarchive, "remove") } } }, text = { Column { Text("move to:"); Spacer(Modifier.height(8.dp)); Box(Modifier.heightIn(max = 400.dp)) { LazyColumn { items(allG) { g -> Row(Modifier.fillMaxWidth().clickable { selGId = g.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selGId == g.id, { selGId = g.id }); Spacer(Modifier.width(8.dp)); Text(g.name) } } } } } }, confirmButton = { TextButton(onClick = { if (selGId != null) scope.launch { physicalAlbumManager.moveAlbumToGroup(selAlbForG!!.name, selGId); showMoveToG = false } }, enabled = selGId != null) { Text("move") } })
    }
    if (showMoveG && selGForMove != null) {
        AlertDialog(onDismissRequest = { showMoveG = false }, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Move Group"); IconButton(onClick = { showDelGConf = selGForMove; showMoveG = false }) { Icon(Icons.Default.Delete, "del", tint = MaterialTheme.colorScheme.error) } } }, text = { Column { Text("move into:"); Spacer(Modifier.height(8.dp)); Box(Modifier.heightIn(max = 400.dp)) { LazyColumn { item { Row(Modifier.fillMaxWidth().clickable { selPGId = null }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selPGId == null, { selPGId = null }); Spacer(Modifier.width(8.dp)); Text("root") } }; items(allG.filter { it.id != selGForMove?.id && !isDescendant(it.id, selGForMove!!.id) }) { g -> Row(Modifier.fillMaxWidth().clickable { selPGId = g.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selPGId == g.id, { selPGId = g.id }); Spacer(Modifier.width(8.dp)); Text(g.name) } } } } } }, confirmButton = { TextButton(onClick = { scope.launch { groupManager.moveGroup(selGForMove!!.id, selPGId); showMoveG = false } }) { Text("move") } })
    }
    if (showDelGConf != null) {
        AlertDialog(onDismissRequest = { showDelGConf = null }, title = { Text("delete?") }, text = { Text("Delete group \"${showDelGConf?.name}\"?") }, confirmButton = { TextButton(onClick = { scope.launch { groupManager.deleteGroup(showDelGConf!!); showDelGConf = null } }) { Text("delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { showDelGConf = null }) { Text("cancel") } })
    }
    if (showReorder) {
        AlertDialog(onDismissRequest = { showReorder = false }, title = { Text("reorder") }, text = { Text("Use arrows to reorder root items.") }, confirmButton = { TextButton(onClick = { showReorder = false }) { Text("done") } })
    }
}
fun isDescendant(gid: Long, aid: Long) = false 
@Composable
fun GroupSelectionRow(group: AlbumGroupEntity, allG: List<AlbumGroupEntity>, selId: Long?, onSelect: (Long) -> Unit, depth: Int = 0) {
    Column { Row(Modifier.fillMaxWidth().clickable { onSelect(group.id) }.padding(start = (depth * 16 + 12).dp, top = 12.dp, bottom = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selId == group.id, { onSelect(group.id) }); Spacer(Modifier.width(8.dp)); Text(group.name) }; allG.filter { it.parentId == group.id }.forEach { GroupSelectionRow(it, allG, selId, onSelect, depth + 1) } }
}
@Composable
fun SpecialAlbumItem(name: String, type: SpecialAlbumType, images: List<MediaItem>, favoriteIds: Set<Long>, enabled: Boolean = true, onClick: () -> Unit) {
    val img = remember(type, images, favoriteIds) { if (type == SpecialAlbumType.RECENTS) images.firstOrNull() else images.find { it.id in favoriteIds } }
    val count = remember(type, images, favoriteIds) { if (type == SpecialAlbumType.RECENTS) images.size else favoriteIds.size }
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
fun GroupAlbumItem(group: AlbumGroupEntity, physicalAlbums: List<PhysicalAlbumEntity>, mediaByBucket: Map<String, List<MediaItem>>, allGroups: List<AlbumGroupEntity>, albumsByGroup: (Long?) -> List<PhysicalAlbumEntity>, onGroupClick: () -> Unit, onLongClick: () -> Unit) {
    val childAlbs = remember(group, physicalAlbums) { albumsByGroup(group.id) }
    fun getImg(alb: PhysicalAlbumEntity) = mediaByBucket[alb.bucketName]?.firstOrNull()?.let { RepresentativeImage(it.uri.toString(), alb.customCoverCrop) }
    fun getGImg(gid: Long, vis: MutableSet<Long> = mutableSetOf()): RepresentativeImage? {
        if (!vis.add(gid)) return null; albumsByGroup(gid).forEach { a -> getImg(a)?.let { return it } }
        allGroups.filter { it.parentId == gid }.forEach { c -> getGImg(c.id, vis)?.let { return it } }
        return null
    }
    val repImgs = remember(group, physicalAlbums, mediaByBucket, allGroups) {
        val list = mutableListOf<RepresentativeImage>(); childAlbs.forEach { a -> if (list.size < 4) getImg(a)?.let { list.add(it) } }
        if (list.size < 4) allGroups.filter { it.parentId == group.id }.forEach { c -> if (list.size < 4) getGImg(c.id)?.let { list.add(it) } }
        list
    }
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onGroupClick, onLongClick = onLongClick)) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (group.customCoverUri != null) AsyncImage(model = group.customCoverUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (repImgs.isNotEmpty()) {
                when (repImgs.size) {
                    1 -> AsyncImage(repImgs[0].uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    2 -> Row(Modifier.fillMaxSize()) { AsyncImage(repImgs[0].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); AsyncImage(repImgs[1].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop) }
                    3 -> Row(Modifier.fillMaxSize()) { AsyncImage(repImgs[0].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); Column(Modifier.weight(1f).fillMaxHeight()) { AsyncImage(repImgs[1].uri, null, Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop); Spacer(Modifier.height(1.dp)); AsyncImage(repImgs[2].uri, null, Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop) } }
                    else -> Column(Modifier.fillMaxSize()) { Row(Modifier.weight(1f)) { AsyncImage(repImgs[0].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); AsyncImage(repImgs[1].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop) }; Spacer(Modifier.height(1.dp)); Row(Modifier.weight(1f)) { AsyncImage(repImgs[2].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop); Spacer(Modifier.width(1.dp)); AsyncImage(repImgs[3].uri, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop) } }
                }
            } else Icon(Icons.Default.Folder, null, Modifier.size(48.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
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
