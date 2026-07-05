package com.example.cgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.InboxItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onItemClick: (Int) -> Unit,
    onOrganise: (Set<Long>, Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onDiagnosticsClick: () -> Unit = {},
    onBack: () -> Unit,
    isEnforcementSession: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items by viewModel.pendingItems.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedIds.size} selected")
                    } else {
                        Text(if (isEnforcementSession) "New Media" else "Inbox")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "clear")
                        }
                    } else if (!isEnforcementSession) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                        }
                    }
                },
                actions = {
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
                            DropdownMenuItem(
                                text = { Text("Snooze 50 files") },
                                onClick = { 
                                    viewModel.setItemSnooze(50)
                                    onBack() 
                                }
                            )
                        }
                    } else if (isSelectionMode) {
                        IconButton(onClick = {
                            onOrganise(selectedIds, true)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "move")
                        }
                        IconButton(onClick = {
                            onOrganise(selectedIds, false)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "copy")
                        }
                    } else {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "settings")
                        }
                        IconButton(onClick = onDiagnosticsClick) {
                            Icon(Icons.Default.BugReport, contentDescription = "debug")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isSelectionMode && !isEnforcementSession) {
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
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Inbox is empty", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val isSelected = item.id in selectedIds
                    InboxListItem(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            } else {
                                onItemClick(index)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedIds = setOf(item.id)
                            }
                        }
                    )
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

        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
