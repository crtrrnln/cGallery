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
    private val favouritesManager = FavouritesManager(application)
    private val inboxDao = VirtualAlbumDatabase.getDatabase(application).inboxDao()
    private val appSettings = AppSettingsRepository(application)
    
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private val inboxStateFlow = inboxDao.getAllItems()
        .map { items ->
            val pending = items.filter { it.status != InboxStatus.Completed && it.status != InboxStatus.Ignored }.map { it.mediaStoreId }.toSet()
            val completed = items.filter { it.status == InboxStatus.Completed }.associateBy({ it.mediaStoreId }, { it.destinationPaths.firstOrNull() })
            pending to completed
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private val enforcementEnabledFlow = appSettings.settingsFlow
        .map { it.isEnforcementEnabled }
        .distinctUntilChanged()

    val mediaItems: StateFlow<List<MediaItem>> = combine(_mediaItems, inboxStateFlow, enforcementEnabledFlow) { items, inboxState, isEnforcement ->
        if (items.isEmpty()) return@combine emptyList()
        if (InboxManager.isBulkProcessing) return@combine mediaItems.value // Stop re-mapping during bulk moves

        val (pendingIds, completedMap) = inboxState

        if ((!isEnforcement || pendingIds.isEmpty()) && completedMap.isEmpty()) items
        else items.filter { !isEnforcement || it.id !in pendingIds }.map { item ->
            completedMap[item.id]?.let { newPath ->
                val f = File(newPath)
                item.copy(fullPath = newPath, bucketPath = f.parent ?: item.bucketPath, bucketName = f.parentFile?.name ?: item.bucketName)
            } ?: item
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cache these to avoid re-calculating on every search query or minor change
    val mediaItemsMap: StateFlow<Map<Long, MediaItem>> = mediaItems
        .map { it.associateBy { i -> i.id } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val mediaByBucket: StateFlow<Map<String, List<MediaItem>>> = mediaItems
        .map { it.groupBy { i -> i.bucketPath } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val favouriteIds: StateFlow<Set<Long>> = favouritesManager.favouriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favouriteMedia: StateFlow<List<MediaItem>> = combine(mediaItems, favouriteIds) { items, ids ->
        if (ids.isEmpty()) emptyList() else items.filter { it.id in ids }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Optimised search: only re-runs when query or items change
    val searchResults: StateFlow<List<MediaItem>> = combine(mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) emptyList() else items.filter { it.displayName.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albumResults: StateFlow<List<Pair<String, String>>> = combine(mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) emptyList() else items.filter { it.bucketName.contains(query, ignoreCase = true) }.map { it.bucketName to it.bucketPath }.distinct()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult = _operationResult.asSharedFlow()
    val physicalAlbums: StateFlow<List<PhysicalAlbumEntity>> = physicalAlbumManager.allAlbums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadMedia() }
    
    fun loadMedia(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            val items = dataSource.fetchMedia()
            _mediaItems.value = items
            withContext(Dispatchers.Default) { 
                physicalAlbumManager.syncAlbums(items.map { it.bucketPath }.distinct())
            }
            _isLoading.value = false
        }
    }

    fun moveMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            val itemsToMove = _mediaItems.value.filter { it.id in ids }
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
                kotlinx.coroutines.delay(500)
                loadMedia(showLoading = false)
            } else { _operationResult.emit(if (errors.isNotEmpty()) "failed: ${errors.first()}" else "failed to move") }
        }
    }

    fun copyMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            val itemsToCopy = _mediaItems.value.filter { it.id in ids }
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
                kotlinx.coroutines.delay(500)
                loadMedia(showLoading = false)
            } else { _operationResult.emit(if (errors.isNotEmpty()) "failed: ${errors.first()}" else "failed to copy") }
        }
    }

    fun createFolder(name: String, gid: Long? = null) {
        viewModelScope.launch {
            val res = physicalAlbumManager.createFolder(name, groupId = gid)
            if (res.isSuccess) { _operationResult.emit("album created: $name"); kotlinx.coroutines.delay(300); loadMedia(showLoading = false) } else { _operationResult.emit("fail to create: ${res.exceptionOrNull()?.message}") }
        }
    }

    fun toggleAlbumVisibility(name: String) { viewModelScope.launch { physicalAlbumManager.toggleAlbumVisibility(name) } }
    fun moveFile(src: String, target: String) { viewModelScope.launch { physicalAlbumManager.moveFile(src, target) } }
}
