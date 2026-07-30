package com.example.cgallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.InboxItemEntity
import com.example.cgallery.data.MediaItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onItemClick: (Int) -> Unit,
    onOrganise: (Set<Long>, Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onDiagnosticsClick: () -> Unit = {},
    onDebugBypass: () -> Unit = {},
    onBack: () -> Unit,
    isEnforcementSession: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items by viewModel.pendingItems.collectAsState()
    val unmonitored by viewModel.unmonitoredItems.collectAsState()
    val showUnmonitored by viewModel.showUnmonitored.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectionViewModel: SelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val selectedIds by selectionViewModel.selectedMediaIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    var previewUri by remember { mutableStateOf<String?>(null) }
    var titleTapCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val displayItems = if (showUnmonitored) {
        unmonitored.map { InboxItemEntity(mediaStoreId = it.id, mediaUri = it.uri.toString(), filename = it.displayName, sourcePath = it.fullPath, detectedTimestamp = 0L) }
    } else {
        items
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isSelectionMode) {
                            Text("${selectedIds.size} selected")
                        } else {
                            Text(if (isEnforcementSession) "New Media" else if (showUnmonitored) "Unmonitored" else "Inbox", modifier = Modifier.clickable { if (isEnforcementSession) { titleTapCount++; if (titleTapCount >= 5) { titleTapCount = 0; onDebugBypass() } } })
                            if (!isSelectionMode) {
                                Text("${displayItems.size} items", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectionViewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "clear")
                        }
                    } else if (showUnmonitored) {
                        IconButton(onClick = { viewModel.toggleUnmonitored() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back to inbox")
                        }
                    } else if (!isEnforcementSession) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            onOrganise(selectedIds, true)
                            selectionViewModel.clearSelection()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "move")
                        }
                        IconButton(onClick = {
                            onOrganise(selectedIds, false)
                            selectionViewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "copy")
                        }
                    }
                    
                    val settings by viewModel.enforcementSettings.collectAsState(com.example.cgallery.data.AppSettings())
                    val isSnoozed = settings.snoozeExpirationTime > System.currentTimeMillis() || settings.snoozeItemThreshold > 0

                    if (isEnforcementSession) {
                        var showSnoozeMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSnoozeMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "snooze")
                        }
                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Snooze 1 hour") },
                                onClick = { 
                                    viewModel.setSnooze(60)
                                    onBack() 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze 15 files") },
                                onClick = { 
                                    viewModel.setItemSnooze(15)
                                    onBack() 
                                }
                            )
                            if (isSnoozed) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Cancel Snooze") },
                                    onClick = { 
                                        viewModel.cancelSnooze()
                                        showSnoozeMenu = false
                                    }
                                )
                            }
                        }
                    } else if (!isSelectionMode) {
                        var showMenu by remember { mutableStateOf(false) }
                        if (isSnoozed) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.NotificationsPaused, contentDescription = "snoozed", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (!showUnmonitored) {
                            IconButton(onClick = { viewModel.toggleUnmonitored() }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "unmonitored")
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "settings")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "more")
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (isSnoozed) {
                                DropdownMenuItem(
                                    text = { Text("Cancel Snooze") },
                                    onClick = { viewModel.cancelSnooze(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, null) }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text("Diagnostics") },
                                onClick = { onDiagnosticsClick(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.BugReport, null) }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isSelectionMode && !isEnforcementSession && !showUnmonitored) {
                FloatingActionButton(
                    onClick = { if (!isScanning) viewModel.scanNow() }
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "scan")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isSelectionMode) {
                    val selectedMedia = remember(selectedIds, displayItems) { displayItems.filter { it.mediaStoreId in selectedIds } }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMedia, key = { it.mediaStoreId }) { item ->
                            AsyncImage(
                                model = item.mediaUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { previewUri = item.mediaUri },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    HorizontalDivider()
                }
                
                if (displayItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(0.6f))
                            Spacer(Modifier.height(8.dp))
                            Text(if (showUnmonitored) "All clear! No unmonitored files." else "You're all caught up!", style = MaterialTheme.typography.titleMedium)
                            Text("Your collection is perfectly organized.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(displayItems, key = { _, item -> item.mediaStoreId }) { index, item ->
                            val isSelected = item.mediaStoreId in selectedIds
                            InboxListItem(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectionViewModel.toggleMediaId(item.mediaStoreId)
                                    } else {
                                        if (showUnmonitored) selectionViewModel.setSelection(setOf(item.mediaStoreId))
                                        else onItemClick(index)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectionViewModel.setSelection(setOf(item.mediaStoreId))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Simple Preview Overlay
            AnimatedVisibility(
                visible = previewUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { previewUri = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.9f),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { previewUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "close preview", tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InboxListItem(
    item: InboxItemEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = item.mediaUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isSelectionMode || isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

