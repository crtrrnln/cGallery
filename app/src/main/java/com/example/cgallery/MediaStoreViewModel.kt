package com.example.cgallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.MediaScannerConnection

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application)
    private val physicalAlbumManager = PhysicalAlbumManager(application)
    private val favoritesManager = FavoritesManager(application)

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    val mediaItemsMap: StateFlow<Map<Long, MediaItem>> = _mediaItems
        .map { items -> items.associateBy { it.id } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val mediaByBucket: StateFlow<Map<String, List<MediaItem>>> = _mediaItems
        .map { items -> items.groupBy { it.bucketPath } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val favoriteIds: StateFlow<Set<Long>> = favoritesManager.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteMedia: StateFlow<List<MediaItem>> = combine(_mediaItems, favoriteIds) { items, ids ->
        if (ids.isEmpty()) emptyList() else items.filter { it.id in ids }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<MediaItem>> = combine(_mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) emptyList()
        else {
            val lowerQuery = query.lowercase()
            items.filter { it.displayName.lowercase().contains(lowerQuery) }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumResults: StateFlow<List<Pair<String, String>>> = combine(_mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) emptyList()
        else {
            val lowerQuery = query.lowercase()
            items.filter { it.bucketName.lowercase().contains(lowerQuery) }
                .map { it.bucketName to it.bucketPath }
                .distinct()
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationResult = MutableSharedFlow<String>()
    val operationResult = _operationResult.asSharedFlow()

    val physicalAlbums: StateFlow<List<PhysicalAlbumEntity>> = physicalAlbumManager.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                // Important: Use full parent paths for synchronization to ensure valid file moves
                val bucketPaths = items.map { it.bucketPath }.distinct()
                physicalAlbumManager.syncAlbums(bucketPaths)
            }
            _isLoading.value = false
        }
    }

    fun moveMediaToAlbum(targetFolderPath: String, mediaIds: Set<Long>) {
        viewModelScope.launch {
            _isLoading.value = true
            val itemsToMove = _mediaItems.value.filter { it.id in mediaIds }
            val movedFiles = mutableListOf<String>()
            val sourceFiles = itemsToMove.map { it.fullPath }
            
            withContext(Dispatchers.IO) {
                itemsToMove.forEach { item ->
                    val result = physicalAlbumManager.moveFile(item.fullPath, targetFolderPath)
                    if (result.isSuccess) {
                        movedFiles.add(result.getOrThrow())
                    }
                }
            }
            
            if (movedFiles.isNotEmpty()) {
                // Scan new files and old files (to remove from MediaStore)
                MediaScannerConnection.scanFile(
                    getApplication(),
                    (movedFiles + sourceFiles).toTypedArray(),
                    null
                ) { _, _ -> }
                
                _operationResult.emit("Successfully moved ${movedFiles.size} items")
                
                // Give the scanner a tiny bit of time to start before reloading
                kotlinx.coroutines.delay(500)
                loadMedia()
            } else {
                _operationResult.emit("Failed to move items")
                _isLoading.value = false
            }
        }
    }

    fun copyMediaToAlbum(targetFolderPath: String, mediaIds: Set<Long>) {
        viewModelScope.launch {
            _isLoading.value = true
            val itemsToCopy = _mediaItems.value.filter { it.id in mediaIds }
            val copiedFiles = mutableListOf<String>()
            
            withContext(Dispatchers.IO) {
                itemsToCopy.forEach { item ->
                    val result = physicalAlbumManager.copyFile(item.fullPath, targetFolderPath)
                    if (result.isSuccess) {
                        copiedFiles.add(result.getOrThrow())
                    }
                }
            }
            
            if (copiedFiles.isNotEmpty()) {
                MediaScannerConnection.scanFile(
                    getApplication(),
                    copiedFiles.toTypedArray(),
                    null
                ) { _, _ -> }
                
                _operationResult.emit("Successfully copied ${copiedFiles.size} items")
                
                kotlinx.coroutines.delay(500)
                loadMedia()
            } else {
                _operationResult.emit("Failed to copy items")
                _isLoading.value = false
            }
        }
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
