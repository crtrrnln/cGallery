package com.example.cgallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cgallery.SettingsViewModel
import com.example.cgallery.data.GridDensity
import com.example.cgallery.data.ThemeAccent
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onNavigateToStorage: () -> Unit) {
    val settings by viewModel.settings.collectAsState(); var tapCount by remember { mutableStateOf(0) }; val ctx = LocalContext.current
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Control Centre") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }) }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p)) {
            item { SectionHeader("Appearance") }
            item {
                var showTM by remember { mutableStateOf(false) }
                SettingsOption("Theme Accent", if (settings.themeAccent == ThemeAccent.INNOCENT_SIN_RED) "Innocent Sin Red" else "Eternal Punishment Blue", Icons.Default.Palette) {
                    Box {
                        TextButton({ showTM = true }) { Text("Change") }
                        DropdownMenu(showTM, { showTM = false }) {
                            DropdownMenuItem({ Text("Innocent Sin Red") }, { viewModel.updateTheme(ThemeAccent.INNOCENT_SIN_RED); showTM = false })
                            DropdownMenuItem({ Text("Eternal Punishment Blue") }, { viewModel.updateTheme(ThemeAccent.ETERNAL_PUNISHMENT_BLUE); showTM = false })
                        }
                    }
                }
            }
            item {
                var showGM by remember { mutableStateOf(false) }
                SettingsOption("Grid Density", if (settings.gridDensity == GridDensity.COMFORTABLE) "Comfortable" else "Compact", Icons.Default.GridView) {
                    Box {
                        TextButton({ showGM = true }) { Text("Change") }
                        DropdownMenu(showGM, { showGM = false }) {
                            DropdownMenuItem({ Text("Comfortable (3 columns)") }, { viewModel.updateGrid(GridDensity.COMFORTABLE); showGM = false })
                            DropdownMenuItem({ Text("Compact (5 columns)") }, { viewModel.updateGrid(GridDensity.COMPACT); showGM = false })
                        }
                    }
                }
            }
            item { SectionHeader("Storage & Performance") }
            item { ListItem(headlineContent = { Text("Storage") }, supportingContent = { Text("Drives and cache management") }, leadingContent = { Icon(Icons.Default.Storage, null) }, trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }, modifier = Modifier.clickable(onClick = onNavigateToStorage)) }
            item { SettingsToggle("Efficiency Mode", "Downsample thumbnails to keep everything fast", settings.efficiencyMode, Icons.Default.Speed) { viewModel.updateEfficiency(it) } }
            item { ListItem(headlineContent = { Text("Refresh Library") }, supportingContent = { Text("Scan for new media files") }, leadingContent = { Icon(Icons.Default.Refresh, null) }, modifier = Modifier.clickable { viewModel.refreshLibrary() }) }
            item { SectionHeader("Security") }
            item { SettingsToggle("Biometric Lock", "Require biometrics to open app", settings.isBiometricEnabled, Icons.Default.Fingerprint) { viewModel.updateBiometric(it) } }
            item { SectionHeader("System") }
            item {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("cGallery v0.86", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable {
                        tapCount++; if (tapCount >= 5) { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/crtrrnln/cGallery"))); tapCount = 0 }
                    })
                }
            }
        }
    }
}
@Composable
fun SectionHeader(title: String) { Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)) }
@Composable
fun SettingsOption(label: String, value: String, icon: ImageVector, action: @Composable () -> Unit) { ListItem(headlineContent = { Text(label) }, supportingContent = { Text(value) }, leadingContent = { Icon(icon, null) }, trailingContent = action) }
@Composable
fun SettingsToggle(label: String, desc: String, checked: Boolean, icon: ImageVector, onCheckedChange: (Boolean) -> Unit) { ListItem(headlineContent = { Text(label) }, supportingContent = { Text(desc) }, leadingContent = { Icon(icon, null) }, trailingContent = { Switch(checked, onCheckedChange) }) }
fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB"); val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
