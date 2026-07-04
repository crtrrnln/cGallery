package com.example.cgallery.data

import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class InboxManager(private val context: Context) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val inboxDao = db.inboxDao()
    private val folderDao = db.monitoredFolderDao()
    private val statsDao = db.inboxStatsDao()
    private val physicalAlbumManager = PhysicalAlbumManager(context)
    private val mediaStoreDataSource = MediaStoreDataSource(context)

    suspend fun scanNow() = withContext(Dispatchers.IO) {
        val folders = folderDao.getEnabledFoldersSync()
        if (folders.isEmpty()) return@withContext

        val allMedia = mediaStoreDataSource.fetchMedia()
        var newCount = 0

        for (item in allMedia) {
            val matchingFolder = folders.find { item.bucketPath.startsWith(it.folderPath) }
            if (matchingFolder != null) {
                // Only process items added AFTER the folder was set to be monitored
                if (item.dateAdded > matchingFolder.ignoreBeforeTimestamp) {
                    if (!inboxDao.exists(item.id)) {
                        val inboxItem = InboxItemEntity(
                            mediaStoreId = item.id,
                            mediaUri = item.uri.toString(),
                            filename = item.displayName,
                            sourcePath = item.fullPath,
                            detectedTimestamp = System.currentTimeMillis(),
                            status = InboxStatus.Pending
                        )
                        inboxDao.insertItem(inboxItem)
                        newCount++
                    }
                }
            }
        }

        if (newCount > 0) {
            updateStats { it.copy(totalDetected = it.totalDetected + newCount) }
        }
    }

    suspend fun processItem(
        item: InboxItemEntity,
        destinationPaths: List<String>,
        operation: InboxOperation
    ): Boolean = withContext(Dispatchers.IO) {
        if (destinationPaths.isEmpty()) return@withContext false

        val startTime = System.currentTimeMillis()
        var successCount = 0
        val createdFiles = mutableListOf<String>()

        try {
            if (operation == InboxOperation.MOVE) {
                // First destination: MOVE
                val firstDest = destinationPaths.first()
                val moveResult = physicalAlbumManager.moveFile(item.sourcePath, firstDest)
                if (moveResult.isSuccess) {
                    successCount++
                    val firstCreatedPath = moveResult.getOrThrow()
                    createdFiles.add(firstCreatedPath)

                    // Additional destinations: COPY from the first created file
                    for (i in 1 until destinationPaths.size) {
                        val copyResult = physicalAlbumManager.copyFile(firstCreatedPath, destinationPaths[i])
                        if (copyResult.isSuccess) {
                            successCount++
                            createdFiles.add(copyResult.getOrThrow())
                        }
                    }
                }
            } else {
                // COPY to all destinations
                for (dest in destinationPaths) {
                    val copyResult = physicalAlbumManager.copyFile(item.sourcePath, dest)
                    if (copyResult.isSuccess) {
                        successCount++
                        createdFiles.add(copyResult.getOrThrow())
                    }
                }
            }

            val allSuccessful = successCount == destinationPaths.size
            val endTime = System.currentTimeMillis()
            val processingTime = endTime - startTime

            if (allSuccessful) {
                inboxDao.updateItem(
                    item.copy(
                        status = InboxStatus.Completed,
                        processingTimestamp = endTime,
                        destinationPaths = destinationPaths,
                        operationType = operation
                    )
                )

                // Trigger MediaStore scans
                MediaScannerConnection.scanFile(context, createdFiles.toTypedArray(), null) { _, _ -> }
                if (operation == InboxOperation.MOVE) {
                    MediaScannerConnection.scanFile(context, arrayOf(item.sourcePath), null) { _, _ -> }
                }

                // Update Stats
                updateStats { stats ->
                    val newSourceCounts = stats.sourceFolderCounts.toMutableMap()
                    newSourceCounts[item.sourcePath] = (newSourceCounts[item.sourcePath] ?: 0) + 1
                    
                    val newDestCounts = stats.destinationFolderCounts.toMutableMap()
                    destinationPaths.forEach { path ->
                        newDestCounts[path] = (newDestCounts[path] ?: 0) + 1
                    }

                    stats.copy(
                        totalCompleted = stats.totalCompleted + 1,
                        totalProcessingTimeMs = stats.totalProcessingTimeMs + processingTime,
                        sourceFolderCounts = newSourceCounts,
                        destinationFolderCounts = newDestCounts
                    )
                }
                return@withContext true
            } else {
                inboxDao.updateItem(
                    item.copy(
                        status = InboxStatus.Failed,
                        retryCount = item.retryCount + 1,
                        notes = "Failed to process all destinations. Succeeded: $successCount/${destinationPaths.size}"
                    )
                )
                updateStats { it.copy(totalFailed = it.totalFailed + 1) }
                return@withContext false
            }
        } catch (e: Exception) {
            inboxDao.updateItem(
                item.copy(
                    status = InboxStatus.Failed,
                    retryCount = item.retryCount + 1,
                    notes = "Error: ${e.message}"
                )
            )
            updateStats { it.copy(totalFailed = it.totalFailed + 1) }
            return@withContext false
        }
    }

    suspend fun ignoreItem(item: InboxItemEntity) = withContext(Dispatchers.IO) {
        inboxDao.updateItem(item.copy(status = InboxStatus.Ignored))
        updateStats { it.copy(totalIgnored = it.totalIgnored + 1) }
    }

    private suspend fun updateStats(transform: (InboxStatsEntity) -> InboxStatsEntity) {
        val currentStats = statsDao.getStats().first() ?: InboxStatsEntity()
        val newStats = transform(currentStats)
        statsDao.updateStats(newStats)
    }
}
