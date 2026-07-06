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
    private val statsDao = db.inboxStatsDao()
    private val physicalAlbumManager = PhysicalAlbumManager(application)
    private val enforcementRepository = AppSettingsRepository(application)

    val pendingItems: StateFlow<List<InboxItemEntity>> = inboxDao.getPendingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monitoredFolders: StateFlow<List<MonitoredFolderEntity>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val physicalAlbums: StateFlow<List<PhysicalAlbumEntity>> = physicalAlbumManager.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats = statsDao.getStats()
    val enforcementSettings = enforcementRepository.settingsFlow

    private val _allMediaFolders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val allMediaFolders = _allMediaFolders.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _needsRefresh = MutableStateFlow(false)
    val needsRefresh = _needsRefresh.asStateFlow()

    private val _operationResult = MutableSharedFlow<String>()
    val operationResult = _operationResult.asSharedFlow()

    init {
        loadMediaFolders()
    }

    fun clearRefreshFlag() { _needsRefresh.value = false }

    fun loadMediaFolders() {
        viewModelScope.launch {
            _allMediaFolders.value = dataSource.fetchMediaFolders()
        }
    }

    fun scanNow() {
        viewModelScope.launch {
            _isScanning.value = true
            val newCount = manager.scanNow()
            _isScanning.value = false
            _operationResult.emit(if (newCount > 0) "found $newCount items" else "scan done")
        }
    }

    fun addMonitoredFolder(path: String, name: String) {
        viewModelScope.launch {
            folderDao.insertFolder(
                MonitoredFolderEntity(
                    folderPath = path,
                    displayName = name,
                    ignoreBeforeTimestamp = System.currentTimeMillis() / 1000
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

    fun processItem(item: InboxItemEntity, destinations: List<String>, operation: InboxOperationType) {
        viewModelScope.launch {
            val success = manager.processItem(item, destinations, operation)
            if (success) {
                _operationResult.emit("done: ${item.filename}")
            } else {
                _operationResult.emit("fail: ${item.filename}")
            }
        }
    }

    fun processItems(ids: Set<Long>, targetFolders: List<String>, isMove: Boolean) {
        viewModelScope.launch {
            _isScanning.value = true 
            _needsRefresh.value = true
            val operation = if (isMove) InboxOperationType.MOVE else InboxOperationType.COPY
            
            // Mark as Queued immediately to stop EnforcementEngine from re-triggering
            val itemsToProcess = inboxDao.getPendingItems().first().filter { it.id in ids }
            itemsToProcess.forEach { inboxDao.updateItem(it.copy(status = InboxStatus.Queued)) }
            
            var successCount = 0
            itemsToProcess.forEach { item ->
                if (manager.processItem(item, targetFolders, operation)) {
                    successCount++
                }
            }
            
            _isScanning.value = false
            _operationResult.emit("processed $successCount/${ids.size}")
        }
    }

    fun ignoreItem(item: InboxItemEntity) {
        viewModelScope.launch {
            manager.ignoreItem(item)
            _operationResult.emit("ignored")
        }
    }

    fun updateAlbumCover(bucketName: String, uri: String?, crop: String?) {
        viewModelScope.launch {
            physicalAlbumManager.updateAlbumCover(bucketName, uri, crop)
        }
    }

    fun updateGroupCover(groupId: Long, uri: String?, crop: String?) {
        viewModelScope.launch {
            physicalAlbumManager.updateGroupCover(groupId, uri, crop)
        }
    }

    fun updateEnforcementEnabled(enabled: Boolean) {
        viewModelScope.launch {
            enforcementRepository.updateEnforcementEnabled(enabled)
        }
    }

    fun updateShizukuEnabled(enabled: Boolean) {
        viewModelScope.launch {
            enforcementRepository.updateShizukuEnabled(enabled)
        }
    }

    fun setSnooze(durationMinutes: Int) {
        viewModelScope.launch {
            val expiration = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
            enforcementRepository.setSnooze(expiration, 0)
        }
    }

    fun setItemSnooze(count: Int) {
        viewModelScope.launch {
            val currentCount = pendingItems.value.size
            enforcementRepository.setSnooze(0, currentCount + count)
        }
    }
}
