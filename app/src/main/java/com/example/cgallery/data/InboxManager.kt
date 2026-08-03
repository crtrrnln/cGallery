package com.example.cgallery.data
import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class InboxManager(private val context: Context) {
    companion object {
        var isBulkProcessing = false
    }
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val inboxDao = db.inboxDao(); private val folderDao = db.monitoredFolderDao(); private val statsDao = db.inboxStatsDao()
    private val physicalAlbumManager = PhysicalAlbumManager(context)
    private val mediaStoreDataSource = MediaStoreDataSource(context)

    suspend fun scanNow(fullScan: Boolean = false): Int = withContext(Dispatchers.IO) {
        val folders = folderDao.getEnabledFoldersSync()
        if (folders.isEmpty()) return@withContext 0
        val since = if (fullScan) 0L else (System.currentTimeMillis() / 1000) - (24 * 60 * 60)
        val allMedia = mediaStoreDataSource.fetchMedia(since); var newCount = 0
        for (item in allMedia) {
            val matchingFolder = folders.find { item.bucketPath.startsWith(it.folderPath) }
            if (matchingFolder != null && item.dateAdded > matchingFolder.ignoreBeforeTimestamp) {
                if (!inboxDao.exists(item.id)) {
                    inboxDao.insertItem(InboxItemEntity(mediaStoreId = item.id, mediaUri = item.uri.toString(), filename = item.displayName, sourcePath = item.fullPath, detectedTimestamp = System.currentTimeMillis(), status = InboxStatus.Detected))
                    newCount++
                }
            }
        }
        if (newCount > 0) updateStats { it.copy(totalDetected = it.totalDetected + newCount) }
        newCount
    }

    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        inboxDao.getPendingItems().first().size
    }

    suspend fun processItem(item: InboxItemEntity, targets: List<String>, op: InboxOperationType): Boolean = withContext(Dispatchers.IO) {
        if (targets.isEmpty()) return@withContext false
        val start = System.currentTimeMillis(); var successCount = 0; val created = mutableListOf<String>()
        try {
            if (op == InboxOperationType.MOVE) {
                val moveRes = physicalAlbumManager.moveFile(item.sourcePath, targets.first())
                if (moveRes.isSuccess) {
                    successCount++; val firstPath = moveRes.getOrThrow(); created.add(firstPath)
                    for (i in 1 until targets.size) {
                        val cRes = physicalAlbumManager.copyFile(firstPath, targets[i])
                        if (cRes.isSuccess) { successCount++; created.add(cRes.getOrThrow()) }
                    }
                }
            } else {
                for (dest in targets) {
                    val cRes = physicalAlbumManager.copyFile(item.sourcePath, dest)
                    if (cRes.isSuccess) { successCount++; created.add(cRes.getOrThrow()) }
                }
            }
            val allOk = successCount == targets.size; val end = System.currentTimeMillis(); val time = end - start
            if (allOk) {
                inboxDao.updateItem(item.copy(status = InboxStatus.Completed, processingTimestamp = end, destinationPaths = created, operationType = op))
                val scanPaths = (created + if (op == InboxOperationType.MOVE) listOf(item.sourcePath) else emptyList())
                MediaScannerConnection.scanFile(context, scanPaths.toTypedArray(), null) { _, _ -> 
                    RefreshEventBus.requestRefresh()
                }
                updateStats { stats ->
                    val nSrc = stats.sourceFolderCounts.toMutableMap(); nSrc[item.sourcePath] = (nSrc[item.sourcePath] ?: 0) + 1
                    val nDest = stats.destinationFolderCounts.toMutableMap(); targets.forEach { nDest[it] = (nDest[it] ?: 0) + 1 }
                    stats.copy(totalCompleted = stats.totalCompleted + 1, totalProcessingTimeMs = stats.totalProcessingTimeMs + time, sourceFolderCounts = nSrc, destinationFolderCounts = nDest)
                }
                return@withContext true
            } else {
                inboxDao.updateItem(item.copy(status = InboxStatus.Failed, retryCount = item.retryCount + 1, notes = "Succeeded: $successCount/${targets.size}"))
                updateStats { it.copy(totalFailed = it.totalFailed + 1) }; return@withContext false
            }
        } catch (e: Exception) {
            inboxDao.updateItem(item.copy(status = InboxStatus.Failed, retryCount = item.retryCount + 1, notes = "Error: ${e.message}"))
            updateStats { it.copy(totalFailed = it.totalFailed + 1) }; return@withContext false
        }
    }

    suspend fun ignoreItem(item: InboxItemEntity) = withContext(Dispatchers.IO) {
        inboxDao.updateItem(item.copy(status = InboxStatus.Ignored))
        updateStats { it.copy(totalIgnored = it.totalIgnored + 1) }
    }

    private suspend fun updateStats(transform: (InboxStatsEntity) -> InboxStatsEntity) {
        val current = statsDao.getStats().first() ?: InboxStatsEntity()
        statsDao.updateStats(transform(current))
    }

    suspend fun processItemsBulk(items: List<InboxItemEntity>, targets: List<String>, op: InboxOperationType): Int = withContext(Dispatchers.IO) {
        if (targets.isEmpty() || items.isEmpty()) return@withContext 0
        var successCount = 0
        val allCreatedPaths = mutableListOf<String>()
        val sourcePathsToScan = mutableListOf<String>()
        val startTime = System.currentTimeMillis()

        for (item in items) {
            val itemCreatedPaths = mutableListOf<String>()
            try {
                if (op == InboxOperationType.MOVE) {
                    val moveRes = physicalAlbumManager.moveFile(item.sourcePath, targets.first())
                    if (moveRes.isSuccess) {
                        val firstPath = moveRes.getOrThrow()
                        itemCreatedPaths.add(firstPath)
                        sourcePathsToScan.add(item.sourcePath)
                        for (i in 1 until targets.size) {
                            val cRes = physicalAlbumManager.copyFile(firstPath, targets[i])
                            if (cRes.isSuccess) itemCreatedPaths.add(cRes.getOrThrow())
                        }
                    }
                } else {
                    for (dest in targets) {
                        val cRes = physicalAlbumManager.copyFile(item.sourcePath, dest)
                        if (cRes.isSuccess) itemCreatedPaths.add(cRes.getOrThrow())
                    }
                }

                if (itemCreatedPaths.size == targets.size) {
                    successCount++
                    allCreatedPaths.addAll(itemCreatedPaths)
                    inboxDao.updateItem(item.copy(
                        status = InboxStatus.Completed,
                        processingTimestamp = System.currentTimeMillis(),
                        destinationPaths = itemCreatedPaths,
                        operationType = op
                    ))
                } else {
                    inboxDao.updateItem(item.copy(
                        status = InboxStatus.Failed,
                        retryCount = item.retryCount + 1,
                        notes = "Partial success: ${itemCreatedPaths.size}/${targets.size}"
                    ))
                }
            } catch (e: Exception) {
                inboxDao.updateItem(item.copy(status = InboxStatus.Failed, retryCount = item.retryCount + 1, notes = "Error: ${e.message}"))
            }
        }

        if (allCreatedPaths.isNotEmpty()) {
            val totalTime = System.currentTimeMillis() - startTime
            val scanPaths = (allCreatedPaths + sourcePathsToScan).toTypedArray()
            MediaScannerConnection.scanFile(context, scanPaths, null) { _, _ ->
                RefreshEventBus.requestRefresh()
            }
            
            updateStats { stats ->
                val nSrc = stats.sourceFolderCounts.toMutableMap()
                val nDest = stats.destinationFolderCounts.toMutableMap()
                // Simple stat update for bulk
                stats.copy(
                    totalCompleted = stats.totalCompleted + successCount,
                    totalProcessingTimeMs = stats.totalProcessingTimeMs + totalTime
                )
            }
        }
        successCount
    }
}

