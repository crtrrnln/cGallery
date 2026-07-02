package com.example.cgallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaStoreDataSource
import com.example.cgallery.data.PhysicalAlbumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application)
    private val physicalAlbumManager = PhysicalAlbumManager(application)

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            _mediaItems.value = dataSource.fetchMedia()
            // Sync physical albums with current MediaStore folders
            val bucketNames = _mediaItems.value.map { it.bucketName }.distinct()
            physicalAlbumManager.syncAlbums(bucketNames)
            _isLoading.value = false
        }
    }

    fun addMediaToAlbum(albumId: Long, mediaIds: Set<Long>) {
        // TODO: Implement for physical folders
    }

    fun toggleAlbumVisibility(bucketName: String) {
        viewModelScope.launch {
            physicalAlbumManager.toggleAlbumVisibility(bucketName)
        }
    }

    fun moveFile(sourcePath: String, targetFolderPath: String) {
        viewModelScope.launch {
            physicalAlbumManager.moveFile(sourcePath, targetFolderPath)
        }
    }
}
