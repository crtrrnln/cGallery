package com.example.cgallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaStoreDataSource
import com.example.cgallery.data.VirtualAlbumManager
import com.example.cgallery.data.AlbumWithMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application)
    private val albumManager = VirtualAlbumManager(application)
    
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val virtualAlbumsWithMedia: StateFlow<List<AlbumWithMedia>> = 
        albumManager.allAlbumsWithMedia.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            _mediaItems.value = dataSource.fetchMedia()
            _isLoading.value = false
        }
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            albumManager.createAlbum(name)
        }
    }

    fun addMediaToAlbum(albumId: Long, mediaIds: Set<Long>) {
        viewModelScope.launch {
            mediaIds.forEach { mediaId ->
                albumManager.addMediaToAlbum(albumId, mediaId)
            }
        }
    }
}
