package com.example.cgallery
import android.app.Application
import android.media.MediaScannerConnection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application)
    private val physicalAlbumManager = PhysicalAlbumManager(application)
    private val favoritesManager = FavoritesManager(application)
    private val inboxDao = VirtualAlbumDatabase.getDatabase(application).inboxDao()
    private val enforcementSettings = EnforcementSettingsRepository(application)
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = combine(_mediaItems, inboxDao.getAllItems(), enforcementSettings.settingsFlow) { items, inboxItems, settings ->
        val pendingIds = if (settings.isEnforcementEnabled) {
            inboxItems.filter { it.status != InboxStatus.Completed && it.status != InboxStatus.Ignored }.map { it.mediaStoreId }.toSet()
        } else emptySet()
        val completedMap = inboxItems.filter { it.status == InboxStatus.Completed }.associateBy({ it.mediaStoreId }, { it.destinationPaths.firstOrNull() })
        items.filter { it.id !in pendingIds }.map { item ->
            completedMap[item.id]?.let { newPath ->
                val f = File(newPath)
                item.copy(fullPath = newPath, bucketPath = f.parent ?: item.bucketPath, bucketName = f.parentFile?.name ?: item.bucketName)
            } ?: item
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mediaItemsMap: StateFlow<Map<Long, MediaItem>> = _mediaItems.map { it.associateBy { i -> i.id } }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val mediaByBucket: StateFlow<Map<String, List<MediaItem>>> = _mediaItems.map { it.groupBy { i -> i.bucketPath } }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val favoriteIds: StateFlow<Set<Long>> = favoritesManager.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteMedia: StateFlow<List<MediaItem>> = combine(_mediaItems, favoriteIds) { items, ids ->
        if (ids.isEmpty()) emptyList() else items.filter { it.id in ids }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val searchResults: StateFlow<List<MediaItem>> = combine(_mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) emptyList() else items.filter { it.displayName.lowercase().contains(query.lowercase()) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albumResults: StateFlow<List<Pair<String, String>>> = combine(_mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) emptyList() else items.filter { it.bucketName.lowercase().contains(query.lowercase()) }.map { it.bucketName to it.bucketPath }.distinct()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult = _operationResult.asSharedFlow()
    val physicalAlbums: StateFlow<List<PhysicalAlbumEntity>> = physicalAlbumManager.allAlbums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    init { loadMedia() }
    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            val items = dataSource.fetchMedia()
            _mediaItems.value = items
            withContext(Dispatchers.Default) { physicalAlbumManager.syncAlbums(items.map { it.bucketPath }.distinct()) }
            _isLoading.value = false
        }
    }
    fun moveMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            _isLoading.value = true; val itemsToMove = _mediaItems.value.filter { it.id in ids }
            val created = mutableListOf<String>(); val sourceFiles = itemsToMove.map { it.fullPath }; val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                itemsToMove.forEach { item ->
                    val first = targets.first(); val res = physicalAlbumManager.moveFile(item.fullPath, first)
                    if (res.isSuccess) {
                        val path = res.getOrThrow(); created.add(path)
                        for (i in 1 until targets.size) {
                            val cRes = physicalAlbumManager.copyFile(path, targets[i])
                            if (cRes.isSuccess) created.add(cRes.getOrThrow()) else errors.add("fail copy: ${cRes.exceptionOrNull()?.message}")
                        }
                    } else errors.add("fail move: ${res.exceptionOrNull()?.message}")
                }
            }
            if (created.isNotEmpty()) {
                MediaScannerConnection.scanFile(getApplication(), (created + sourceFiles).toTypedArray(), null) { _, _ -> }
                _operationResult.emit(if (errors.isEmpty()) "moved ${itemsToMove.size} items" else "partial success: ${errors.size} errors")
                loadMedia()
            } else { _operationResult.emit(if (errors.isNotEmpty()) "failed: ${errors.first()}" else "failed to move"); _isLoading.value = false }
        }
    }
    fun copyMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            _isLoading.value = true; val itemsToCopy = _mediaItems.value.filter { it.id in ids }
            val created = mutableListOf<String>(); val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                itemsToCopy.forEach { item ->
                    for (dest in targets) {
                        val res = physicalAlbumManager.copyFile(item.fullPath, dest)
                        if (res.isSuccess) created.add(res.getOrThrow()) else errors.add("fail: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
            if (created.isNotEmpty()) {
                MediaScannerConnection.scanFile(getApplication(), created.toTypedArray(), null) { _, _ -> }
                _operationResult.emit(if (errors.isEmpty()) "copied ${itemsToCopy.size} items" else "partial success: ${errors.size} errors")
                loadMedia()
            } else { _operationResult.emit(if (errors.isNotEmpty()) "failed: ${errors.first()}" else "failed to copy"); _isLoading.value = false }
        }
    }
    fun createFolder(name: String, gid: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true; val res = physicalAlbumManager.createFolder(name, groupId = gid)
            if (res.isSuccess) { _operationResult.emit("album created: $name"); loadMedia() } else { _operationResult.emit("fail to create: ${res.exceptionOrNull()?.message}") }
            _isLoading.value = false
        }
    }
    fun toggleAlbumVisibility(name: String) { viewModelScope.launch { physicalAlbumManager.toggleAlbumVisibility(name) } }
    fun moveFile(src: String, target: String) { viewModelScope.launch { physicalAlbumManager.moveFile(src, target) } }
}
