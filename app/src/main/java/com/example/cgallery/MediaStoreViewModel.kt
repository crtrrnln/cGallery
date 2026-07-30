package com.example.cgallery
import android.app.Application
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class MediaStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = MediaStoreDataSource(application)
    private val physicalAlbumManager = PhysicalAlbumManager(application)
    private val favouritesManager = FavouritesManager(application)
    private val inboxDao = VirtualAlbumDatabase.getDatabase(application).inboxDao()
    private val appSettings = AppSettingsRepository(application)
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())

    // Optimized inbox state flow with better filtering
    private val inboxStateFlow = inboxDao.getAllItems()
        .map { items ->
            val pending = items
                .asSequence()
                .filter { it.status != InboxStatus.Completed && it.status != InboxStatus.Ignored }
                .map { it.mediaStoreId }
                .toSet()
            val completed = items
                .asSequence()
                .filter { it.status == InboxStatus.Completed }
                .associate { it.mediaStoreId to it.destinationPaths.firstOrNull() }
            pending to completed
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    // Optimized media items flow with better performance
    val mediaItems: StateFlow<List<MediaItem>> = combine(
        _mediaItems,
        inboxStateFlow,
        appSettings.settingsFlow.map { it.isEnforcementEnabled }.distinctUntilChanged()
    ) { items, inboxState, isEnf ->
        val (pendingIds, completedMap) = inboxState
        if (items.isEmpty()) return@combine items
        if ((!isEnf || pendingIds.isEmpty()) && completedMap.isEmpty()) return@combine items
        
        items.asSequence().filter { item ->
            !(isEnf && item.id in pendingIds)
        }.map { item ->
            val path = completedMap[item.id]
            if (path != null) {
                item.copy(
                    fullPath = path,
                    bucketPath = path.substringBeforeLast('/'),
                    bucketName = path.substringBeforeLast('/').substringAfterLast('/')
                )
            } else {
                item
            }
        }.toList()
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cached derived flows with proper sharing
    val mediaItemsMap = mediaItems
        .map { items -> items.associateBy { it.id } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    
    val mediaByBucket = mediaItems
        .map { items -> items.groupBy { it.bucketPath } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    
    val favouriteMedia = combine(mediaItems, favouritesManager.favouriteIds) { items, ids ->
        items.filter { it.id in ids }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    // Optimized search with case-insensitive comparison
    val searchResults = combine(mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) return@combine emptyList()
        val lowerQuery = query.lowercase()
        items.filter { it.displayName.lowercase().contains(lowerQuery) }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val albumResults = combine(mediaItems, _searchQuery) { items, query ->
        if (query.isBlank()) return@combine emptyList()
        val lowerQuery = query.lowercase()
        items
            .asSequence()
            .filter { it.bucketName.lowercase().contains(lowerQuery) }
            .map { it.bucketName to it.bucketPath }
            .distinct()
            .toList()
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult = _operationResult.asSharedFlow()
    
    private val loadToken = AtomicLong(0)

    // Content observer with debouncing
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            viewModelScope.launch {
                delay(500) // Debounce for batch system updates
                loadMedia(false)
            }
        }
    }

    init { 
        loadMedia()
        getApplication<Application>().contentResolver.apply {
            registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
            registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        }
        
        viewModelScope.launch {
            RefreshEventBus.refreshRequests.collect {
                delay(300) // Debounce refresh requests
                loadMedia(false)
            }
        }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.apply {
            unregisterContentObserver(observer)
        }
    }

    fun loadMedia(showLoading: Boolean = false) {
        viewModelScope.launch {
            val token = loadToken.incrementAndGet()
            if (showLoading || _mediaItems.value.isEmpty()) {
                _isLoading.value = true
            }
            
            val items = withContext(Dispatchers.IO) {
                dataSource.fetchMedia()
            }
            
            if (token != loadToken.get()) return@launch
            
            _mediaItems.value = items
            
            // Sync albums in background
            launch(Dispatchers.Default) {
                physicalAlbumManager.syncAlbums(items.map { it.bucketPath }.distinct())
            }
            
            if (token == loadToken.get()) {
                _isLoading.value = false
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun moveMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            val itemsToMove = _mediaItems.value.filter { it.id in ids }
            val created = mutableListOf<String>()
            val sourceFiles = itemsToMove.map { it.fullPath }
            val errors = mutableListOf<String>()
            
            withContext(Dispatchers.IO) {
                itemsToMove.forEach { item ->
                    val res = physicalAlbumManager.moveFile(item.fullPath, targets.first())
                    if (res.isSuccess) {
                        val path = res.getOrThrow()
                        created.add(path)
                        // Copy to additional targets
                        for (i in 1 until targets.size) {
                            val cRes = physicalAlbumManager.copyFile(path, targets[i])
                            if (cRes.isSuccess) {
                                created.add(cRes.getOrThrow())
                            } else {
                                errors.add("fail copy")
                            }
                        }
                    } else {
                        errors.add("fail move")
                    }
                }
            }
            
            if (created.isNotEmpty()) { 
                MediaScannerConnection.scanFile(
                    getApplication(),
                    (created + sourceFiles).toTypedArray(),
                    null
                ) { _, _ -> 
                    RefreshEventBus.requestRefresh()
                }
                _operationResult.emit(
                    if (errors.isEmpty()) "moved ${itemsToMove.size}" else "partial success"
                )
            } else {
                _operationResult.emit("failed to move")
            }
        }
    }
    
    fun copyMediaToAlbum(targets: List<String>, ids: Set<Long>) {
        viewModelScope.launch {
            val itemsToCopy = _mediaItems.value.filter { it.id in ids }
            val created = mutableListOf<String>()
            val errors = mutableListOf<String>()
            
            withContext(Dispatchers.IO) {
                itemsToCopy.forEach { item ->
                    targets.forEach { dest ->
                        val res = physicalAlbumManager.copyFile(item.fullPath, dest)
                        if (res.isSuccess) {
                            created.add(res.getOrThrow())
                        } else {
                            errors.add("fail")
                        }
                    }
                }
            }
            
            if (created.isNotEmpty()) { 
                MediaScannerConnection.scanFile(
                    getApplication(),
                    created.toTypedArray(),
                    null
                ) { _, _ -> 
                    RefreshEventBus.requestRefresh()
                }
                _operationResult.emit("copied ${itemsToCopy.size}")
            } else {
                _operationResult.emit("failed to copy")
            }
        }
    }
    
    fun createFolder(name: String, gid: Long? = null) {
        viewModelScope.launch {
            val res = physicalAlbumManager.createFolder(name, groupId = gid)
            if (res.isSuccess) {
                _operationResult.emit("created: $name")
                RefreshEventBus.requestRefresh()
            } else {
                _operationResult.emit("failed")
            }
        }
    }
    
    fun toggleAlbumVisibility(name: String) {
        viewModelScope.launch {
            physicalAlbumManager.toggleAlbumVisibility(name)
        }
    }

    fun updateMediaDate(id: Long, type: MediaType, newDateSeconds: Long) {
        viewModelScope.launch {
            if (dataSource.updateMediaDate(id, type, newDateSeconds)) {
                _operationResult.emit("Date updated")
                loadMedia(false)
            } else {
                _operationResult.emit("Failed to update date")
            }
        }
    }

    fun saveEditedImage(originalItem: MediaItem, bitmap: Bitmap) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val originalFile = File(originalItem.fullPath)
                    val tempFile = File(originalFile.parent, "temp_${originalFile.name}")
                    val fos = java.io.FileOutputStream(tempFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    fos.close()
                    
                    if (originalFile.delete() && tempFile.renameTo(originalFile)) {
                        MediaScannerConnection.scanFile(
                            getApplication(),
                            arrayOf(originalFile.absolutePath),
                            null
                        ) { _, _ -> 
                            RefreshEventBus.requestRefresh()
                        }
                        _operationResult.emit("Saved")
                    } else {
                        tempFile.delete()
                        _operationResult.emit("Failed to save")
                    }
                } catch (e: Exception) {
                    _operationResult.emit("Error: ${e.message}")
                }
            }
        }
    }
}