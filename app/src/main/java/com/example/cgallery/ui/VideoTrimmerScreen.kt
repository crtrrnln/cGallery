package com.example.cgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cgallery.data.MediaItem
import com.example.cgallery.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerScreen(
    item: MediaItem,
    onTrim: (Long, Long) -> Unit,
    onBack: () -> Unit
) {
    var start by remember { mutableStateOf(0f) }
    var end by remember { mutableStateOf(1f) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trim Video") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton({ onTrim((start * item.duration).toLong(), (end * item.duration).toLong()) }) {
                        Icon(Icons.Default.Save, "save")
                    }
                }
            )
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p).background(Color.Black)) {
            Box(Modifier.weight(1f)) {
                VideoPlayer(item.uri, true, Modifier.fillMaxSize())
            }
            
            Column(Modifier.padding(16.dp)) {
                Text("Select Range", color = Color.White)
                RangeSlider(
                    value = start..end,
                    onValueChange = { range ->
                        start = range.start
                        end = range.endInclusive
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Start: ${(start * item.duration / 1000).toInt()}s", color = Color.White)
                    Text("End: ${(end * item.duration / 1000).toInt()}s", color = Color.White)
                }
            }
        }
    }
}
