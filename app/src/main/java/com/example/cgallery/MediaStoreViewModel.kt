package com.example.cgallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.FavoritesManager
import com.example.cgallery.data.MediaItem
import com.example.cgallery.data.MediaStoreDataSource
import com.example.cgallery.data.PhysicalAlbumManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application)
    private val physicalAlbumManager = PhysicalAlbumManager(application)
    private val favoritesManager = FavoritesManager(application)

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    val mediaByBucket: StateFlow<Map<String, List<MediaItem>>> = _mediaItems
        .map { items -> items.groupBy { it.bucketName } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val favoriteIds: StateFlow<Set<Long>> = favoritesManager.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteMedia: StateFlow<List<MediaItem>> = combine(_mediaItems, favoriteIds) { items, ids ->
        items.filter { it.id in ids }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            val items = dataSource.fetchMedia()
            _mediaItems.value = items
            
            // Sync physical albums with current MediaStore folders in background
            withContext(Dispatchers.Default) {
                val bucketNames = items.map { it.bucketName }.distinct()
                physicalAlbumManager.syncAlbums(bucketNames)
            }
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
