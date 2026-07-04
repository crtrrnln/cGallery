package com.example.cgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.InboxItemEntity
import com.example.cgallery.data.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxProcessingScreen(
    startIndex: Int,
    viewModel: InboxViewModel,
    onOrganise: (Set<Long>, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.pendingItems.collectAsState()
    
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, items.size - 1),
        pageCount = { items.size }
    )

    // Handle auto-advancing or finishing if the list changes (item processed)
    LaunchedEffect(items.size) {
        if (items.isEmpty()) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organise (${pagerState.currentPage + 1}/${items.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentItem = items.getOrNull(pagerState.currentPage)
                    if (currentItem != null) {
                        IconButton(onClick = { onOrganise(setOf(currentItem.id), true) }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to Album")
                        }
                        IconButton(onClick = { onOrganise(setOf(currentItem.id), false) }) {
                            Icon(Icons.Default.Add, contentDescription = "Copy to Album")
                        }
                        IconButton(onClick = { viewModel.ignoreItem(currentItem) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Ignore")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            pageSpacing = 16.dp
        ) { page ->
            val item = items.getOrNull(page)
            if (item != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.mediaUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Optional: Filename overlay at bottom
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = item.filename,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
