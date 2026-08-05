package com.example.cgallery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cgallery.data.AppSettings
import com.example.cgallery.data.AppSettingsRepository
import com.example.cgallery.data.InboxItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxProcessingScreen(
    startIndex: Int,
    viewModel: InboxViewModel,
    onOrganise: (Set<Long>, Boolean) -> Unit,
    onBack: () -> Unit,
    isEnforcementSession: Boolean = false
) {
    val items by viewModel.pendingItems.collectAsState()
    val context = LocalContext.current
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val appSettings by appSettingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val useModern = appSettings.useModernUI

    // SAFETY GUARD: If items are empty (e.g. last item just processed), 
    // we must not call rememberPagerState with invalid bounds.
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, items.size - 1),
        pageCount = { items.size }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Organise", style = MaterialTheme.typography.titleMedium)
                        Text("${pagerState.currentPage + 1} of ${items.size}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                    }
                },
                navigationIcon = {
                    if (!isEnforcementSession) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
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
                            Icon(Icons.Default.Delete, contentDescription = "Ignore", tint = if (useModern) Color(0xFFFF5252) else LocalContentColor.current)
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val item = items.getOrNull(page)
                if (item != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = item.mediaUri,
                            contentDescription = item.filename,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
