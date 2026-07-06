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

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application); private val physicalAlbumManager = PhysicalAlbumManager(application)
    private val favouritesManager = FavouritesManager(application); private val inboxDao = VirtualAlbumDatabase.getDatabase(application).inboxDao()
    private val appSettings = AppSettingsRepository(application); private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private val inboxStateFlow = inboxDao.getAllItems().map { items ->
        val pending = items.filter { it.status != InboxStatus.Completed && it.status != InboxStatus.Ignored }.map { it.mediaStoreId }.toSet()
        val completed = items.filter { it.status == InboxStatus.Completed }.associate { it.mediaStoreId to it.destinationPaths.firstOrNull() }
        pending to completed
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    val mediaItems: StateFlow<List<MediaItem>> = combine(_mediaItems, inboxStateFlow, appSettings.settingsFlow.map { it.isEnforcementEnabled }.distinctUntilChanged()) { items, inboxState, isEnf ->
        if (items.isEmpty() || InboxManager.isBulkProcessing) return@combine items
        val (pendingIds, completedMap) = inboxState
        if ((!isEnf || pendingIds.isEmpty()) && completedMap.isEmpty()) items
        else {
            val result = ArrayList<MediaItem>(items.size)
            for (item in items) {
                if (isEnf && item.id in pendingIds) continue
                val path = completedMap[item.id]
                if (path != null) result.add(item.copy(fullPath = path, bucketPath = path.substringBeforeLast('/'), bucketName = path.substringBeforeLast('/').substringAfterLast('/')))
                else result.add(item)
            }
            result
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaItemsMap = mediaItems.map { it.associateBy { i -> i.id } }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    val mediaByBucket = mediaItems.map { it.groupBy { i -> i.bucketPath } }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    val favouriteMedia = combine(mediaItems, favouritesManager.favouriteIds) { items, ids -> items.filter { it.id in ids } }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _searchQuery = MutableStateFlow(""); val searchQuery = _searchQuery.asStateFlow()
    val searchResults = combine(mediaItems, _searchQuery) { items, query -> if (query.isBlank()) emptyList() else items.filter { it.displayName.contains(query, true) } }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val albumResults = combine(mediaItems, _searchQuery) { items, query -> if (query.isBlank()) emptyList() else items.filter { it.bucketName.contains(query, true) }.map { it.bucketName to it.bucketPath }.distinct() }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false); val isLoading = _isLoading.asStateFlow()
    private val _operationResult = MutableSharedFlow<String>(); val operationResult = _operationResult.asSharedFlow()

    init { loadMedia() }
    fun loadMedia(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading || _mediaItems.value.isEmpty()) _isLoading.value = true
            val items = dataSource.fetchMedia(); _mediaItems.value = items
            withContext(Dispatchers.Default) { physicalAlbumManager.syncAlbums(items.map { it.bucketPath }.distinct()) }
            _isLoading.value = false
        }
    }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun moveMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            val itemsToMove = _mediaItems.value.filter { it.id in ids }; val created = mutableListOf<String>(); val sourceFiles = itemsToMove.map { it.fullPath }; val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                itemsToMove.forEach { item ->
                    val res = physicalAlbumManager.moveFile(item.fullPath, targets.first())
                    if (res.isSuccess) {
                        val path = res.getOrThrow(); created.add(path)
                        for (i in 1 until targets.size) { val cRes = physicalAlbumManager.copyFile(path, targets[i]); if (cRes.isSuccess) created.add(cRes.getOrThrow()) else errors.add("fail copy") }
                    } else errors.add("fail move")
                }
            }
            if (created.isNotEmpty()) { MediaScannerConnection.scanFile(getApplication(), (created + sourceFiles).toTypedArray(), null) { _, _ -> }; _operationResult.emit(if (errors.isEmpty()) "moved ${itemsToMove.size}" else "partial success"); kotlinx.coroutines.delay(500); loadMedia(false) }
            else _operationResult.emit("failed to move")
        }
    }
    fun copyMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            val itemsToCopy = _mediaItems.value.filter { it.id in ids }; val created = mutableListOf<String>(); val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                itemsToCopy.forEach { item ->
                    targets.forEach { dest -> val res = physicalAlbumManager.copyFile(item.fullPath, dest); if (res.isSuccess) created.add(res.getOrThrow()) else errors.add("fail") }
                }
            }
            if (created.isNotEmpty()) { MediaScannerConnection.scanFile(getApplication(), created.toTypedArray(), null) { _, _ -> }; _operationResult.emit("copied ${itemsToCopy.size}"); kotlinx.coroutines.delay(500); loadMedia(false) }
            else _operationResult.emit("failed to copy")
        }
    }
    fun createFolder(name: String, gid: Long? = null) { viewModelScope.launch { val res = physicalAlbumManager.createFolder(name, groupId = gid); if (res.isSuccess) { _operationResult.emit("created: $name"); kotlinx.coroutines.delay(300); loadMedia(false) } else _operationResult.emit("failed") } }
    fun toggleAlbumVisibility(name: String) = viewModelScope.launch { physicalAlbumManager.toggleAlbumVisibility(name) }
}
