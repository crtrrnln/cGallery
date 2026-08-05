package com.example.cgallery

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.example.cgallery.data.TrashItemEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    viewModel: MediaStoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.trashItems.collectAsState()
    val context = LocalContext.current
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val useModern = appSettings.useModernUI
    
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (useModern && !isSelectionMode) {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text("Trash")
                            Text("${items.size} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back")
                        }
                    },
                    actions = {
                        if (items.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "empty trash")
                            }
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(if (isSelectionMode) "${selectedIds.size} selected" else "Trash Bin") 
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (isSelectionMode) selectedIds = emptySet() else onBack() }) {
                            Icon(if (isSelectionMode) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back")
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = { 
                                val selected = items.filter { it.id in selectedIds }
                                viewModel.restoreFromTrash(selected)
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Rounded.Restore, contentDescription = "restore")
                            }
                            IconButton(onClick = { 
                                val selected = items.filter { it.id in selectedIds }
                                viewModel.deletePermanently(selected)
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = "delete permanently", tint = MaterialTheme.colorScheme.error)
                            }
                        } else if (items.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "empty trash")
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(64.dp), MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("Trash is empty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    itemsIndexed(items, key = { _, it -> it.id }) { _, item ->
                        val isSelected = item.id in selectedIds
                        TrashListItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            useModernUI = useModern,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Empty Trash?") },
            text = { Text("This will permanently delete all ${items.size} items in the trash. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.emptyTrash()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashListItem(
    item: TrashItemEntity,
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
            model = item.trashPath,
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
