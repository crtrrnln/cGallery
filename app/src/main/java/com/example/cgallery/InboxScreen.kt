package com.example.cgallery

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.AppSettings
import com.example.cgallery.data.AppSettingsRepository
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
    
    val context = LocalContext.current
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val useModern = appSettings.useModernUI

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
            if (useModern && !isSelectionMode) {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(if (isEnforcementSession) "New Media" else if (showUnmonitored) "Unmonitored" else "Inbox", 
                                modifier = Modifier.clickable { if (isEnforcementSession) { titleTapCount++; if (titleTapCount >= 5) { titleTapCount = 0; onDebugBypass() } } })
                            Text("${displayItems.size} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    },
                    navigationIcon = {
                        if (showUnmonitored) {
                            IconButton(onClick = { viewModel.toggleUnmonitored() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back to inbox")
                            }
                        } else if (!isEnforcementSession) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back")
                            }
                        }
                    },
                    actions = {
                        if (!showUnmonitored) {
                            IconButton(onClick = { viewModel.toggleUnmonitored() }) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = "unmonitored")
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Rounded.Settings, contentDescription = "settings")
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "more")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Diagnostics") },
                                onClick = { onDiagnosticsClick(); showMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.BugReport, null) }
                            )
                        }
                    }
                )
            } else {
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
                        } else {
                            if (!showUnmonitored) {
                                IconButton(onClick = { viewModel.toggleUnmonitored() }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "unmonitored")
                                }
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = "settings")
                            }
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "more")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Diagnostics") },
                                    onClick = { onDiagnosticsClick(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.BugReport, null) }
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isSelectionMode && !isEnforcementSession && !showUnmonitored) {
                FloatingActionButton(
                    onClick = { if (!isScanning) viewModel.scanNow() },
                    shape = if (useModern) RoundedCornerShape(20.dp) else FloatingActionButtonDefaults.shape
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
                            Icon(Icons.Rounded.DoneAll, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f))
                            Spacer(Modifier.height(16.dp))
                            Text(if (showUnmonitored) "All clear! No unmonitored files." else "You're all caught up!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Your collection is perfectly organized.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (useModern) 140.dp else 120.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(if (useModern) 12.dp else 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (useModern) 12.dp else 4.dp),
                        verticalArrangement = Arrangement.spacedBy(if (useModern) 12.dp else 4.dp)
                    ) {
                        itemsIndexed(displayItems, key = { _, item -> item.mediaStoreId }) { index, item ->
                            val isSelected = item.mediaStoreId in selectedIds
                            InboxListItem(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                useModernUI = useModern,
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
    useModernUI: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")
    
    val shape = if (useModernUI) RoundedCornerShape(24.dp) else RoundedCornerShape(8.dp)
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (useModernUI) 4.dp else 0.dp, shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = {
                    if (useModernUI) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    if (useModernUI) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        AsyncImage(
            model = item.mediaUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (useModernUI) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.3f)), startY = 300f)))
        }

        if (isSelectionMode || isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.TopEnd).padding(if (useModernUI) 8.dp else 0.dp)
            )
        }
    }
}
