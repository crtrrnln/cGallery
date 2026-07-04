package com.example.cgallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cgallery.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InboxViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = InboxManager(application)
    private val dataSource = MediaStoreDataSource(application)
    private val db = VirtualAlbumDatabase.getDatabase(application)
    private val inboxDao = db.inboxDao()
    private val folderDao = db.monitoredFolderDao()
    private val physicalAlbumManager = PhysicalAlbumManager(application)

    val pendingItems: StateFlow<List<InboxItemEntity>> = inboxDao.getPendingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monitoredFolders: StateFlow<List<MonitoredFolderEntity>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val physicalAlbums: StateFlow<List<PhysicalAlbumEntity>> = physicalAlbumManager.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allMediaFolders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val allMediaFolders = _allMediaFolders.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _operationResult = MutableSharedFlow<String>()
    val operationResult = _operationResult.asSharedFlow()

    init {
        loadMediaFolders()
    }

    fun loadMediaFolders() {
        viewModelScope.launch {
            _allMediaFolders.value = dataSource.fetchMediaFolders()
        }
    }

    fun scanNow() {
        viewModelScope.launch {
            _isScanning.value = true
            manager.scanNow()
            _isScanning.value = false
            _operationResult.emit("Scan complete")
        }
    }

    fun addMonitoredFolder(path: String, name: String) {
        viewModelScope.launch {
            // Set ignoreBeforeTimestamp to current time so old files are ignored
            folderDao.insertFolder(
                MonitoredFolderEntity(
                    folderPath = path,
                    displayName = name,
                    ignoreBeforeTimestamp = System.currentTimeMillis() / 1000 // MediaStore uses seconds
                )
            )
        }
    }

    fun removeMonitoredFolder(folder: MonitoredFolderEntity) {
        viewModelScope.launch {
            folderDao.deleteFolder(folder)
        }
    }

    fun toggleMonitoredFolder(folder: MonitoredFolderEntity) {
        viewModelScope.launch {
            folderDao.updateFolder(folder.copy(isEnabled = !folder.isEnabled))
        }
    }

    fun resetFolderFilter(folder: MonitoredFolderEntity) {
        viewModelScope.launch {
            folderDao.updateFolder(folder.copy(ignoreBeforeTimestamp = 0L))
        }
    }

    fun processItem(item: InboxItemEntity, destinations: List<String>, operation: InboxOperation) {
        viewModelScope.launch {
            val success = manager.processItem(item, destinations, operation)
            if (success) {
                _operationResult.emit("Successfully processed ${item.filename}")
            } else {
                _operationResult.emit("Failed to process ${item.filename}")
            }
        }
    }

    fun processItems(ids: Set<Long>, targetFolders: List<String>, isMove: Boolean) {
        viewModelScope.launch {
            _isScanning.value = true // Reusing scanning for general work
            val operation = if (isMove) InboxOperation.MOVE else InboxOperation.COPY
            var successCount = 0
            val itemsToProcess = inboxDao.getPendingItems().first().filter { it.id in ids }
            
            itemsToProcess.forEach { item ->
                if (manager.processItem(item, targetFolders, operation)) {
                    successCount++
                }
            }
            
            _isScanning.value = false
            _operationResult.emit("Processed $successCount/${ids.size} items")
        }
    }

    fun ignoreItem(item: InboxItemEntity) {
        viewModelScope.launch {
            manager.ignoreItem(item)
            _operationResult.emit("Item ignored")
        }
    }
}
