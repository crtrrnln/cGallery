package com.example.cgallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppSettingsRepository(application)
    private val dataSource = MediaStoreDataSource(application)
    private val db = VirtualAlbumDatabase.getDatabase(application)
    val settings = repo.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _storageStats = MutableStateFlow<DetailedStorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun updateTheme(v: ThemeAccent) = viewModelScope.launch { repo.updateThemeAccent(v) }
    fun updateGrid(v: GridDensity) = viewModelScope.launch { repo.updateGridDensity(v) }
    fun updateEfficiency(v: Boolean) = viewModelScope.launch { repo.updateEfficiencyMode(v) }
    fun updateBiometric(v: Boolean) = viewModelScope.launch { repo.updateBiometricEnabled(v) }
    fun updateModernUI(v: Boolean) = viewModelScope.launch { repo.updateModernUI(v) }

    fun calculateStorage() = viewModelScope.launch {
        if (_storageStats.value != null) return@launch
        _storageStats.value = dataSource.getDetailedStorageStats()
    }

    fun clearCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            getApplication<Application>().cacheDir.deleteRecursively()
            getApplication<Application>().cacheDir.mkdirs()
        }
    }

    fun refreshLibrary() = viewModelScope.launch {
        if (_isScanning.value) return@launch
        _isScanning.value = true
        try {
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                repairOldInboxDestinations()
                val roots = mutableListOf(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM),
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                )
                db.physicalAlbumDao().getAllAlbums().first().forEach { roots.add(java.io.File(it.bucketName)) }
                db.inboxDao().getCompletedItems().first().forEach { item -> item.destinationPaths.forEach { p -> roots.add(java.io.File(p).parentFile ?: java.io.File(p)) } }
                val exts = listOf("jpg", "jpeg", "png", "webp", "mp4", "mkv", "gif", "heic", "heif", "mov", "webm")
                val allFiles = mutableListOf<String>()
                roots.distinctBy { it.absolutePath }.forEach { root ->
                    if (root.exists()) {
                        allFiles.add(root.absolutePath)
                        root.walkTopDown().filter { it.isFile && it.extension.lowercase() in exts }.forEach { allFiles.add(it.absolutePath) }
                    }
                }
                if (allFiles.isNotEmpty()) {
                    val scanFiles = allFiles.distinct()
                    val latch = java.util.concurrent.CountDownLatch(scanFiles.size)
                    android.media.MediaScannerConnection.scanFile(context, scanFiles.toTypedArray(), null) { _, _ -> latch.countDown() }
                    latch.await(25, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        } finally {
            _isScanning.value = false
            RefreshEventBus.requestRefresh()
        }
    }

    private suspend fun repairOldInboxDestinations() = withContext(Dispatchers.IO) {
        val dao = db.inboxDao(); val completed = dao.getCompletedItems().first()
        completed.forEach { item ->
            val fixed = item.destinationPaths.map { path ->
                val f = java.io.File(path)
                if (f.isDirectory) java.io.File(f, item.filename).absolutePath else path
            }.filter { java.io.File(it).exists() }
            if (fixed.isNotEmpty() && fixed != item.destinationPaths) dao.updateItem(item.copy(destinationPaths = fixed))
        }
    }
}






