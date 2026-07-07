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
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val paths = arrayOf(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).absolutePath,
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).absolutePath
            )
            android.media.MediaScannerConnection.scanFile(context, paths, null) { _, _ -> }
        }
    }
}
