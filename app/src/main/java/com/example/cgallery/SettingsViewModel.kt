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
    val settings = repo.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _storageStats = MutableStateFlow<DetailedStorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun updateTheme(v: ThemeAccent) = viewModelScope.launch { repo.updateThemeAccent(v) }
    fun updateGrid(v: GridDensity) = viewModelScope.launch { repo.updateGridDensity(v) }
    fun updateEfficiency(v: Boolean) = viewModelScope.launch { repo.updateEfficiencyMode(v) }
    fun updateBiometric(v: Boolean) = viewModelScope.launch { repo.updateBiometricEnabled(v) }

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
        _isScanning.value = true
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val roots = arrayOf(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            )
            
            val allFiles = mutableListOf<String>()
            roots.forEach { root ->
                if (root.exists()) {
                    root.walkTopDown().filter { it.isFile && (it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "mp4", "mkv", "gif")) }.forEach { 
                        allFiles.add(it.absolutePath) 
                    }
                }
            }
            
            if (allFiles.isNotEmpty()) {
                android.media.MediaScannerConnection.scanFile(context, allFiles.toTypedArray(), null) { _, _ -> }
            }
        }
        _isScanning.value = false
    }
}
