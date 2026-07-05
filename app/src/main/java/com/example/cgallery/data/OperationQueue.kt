package com.example.cgallery.data

import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class OperationQueue(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val operationDao = db.inboxOperationDao()
    private val inboxDao = db.inboxDao()
    private val physicalAlbumManager = PhysicalAlbumManager(context)
    private var isProcessing = false

    fun start() {
        scope.launch {
            operationDao.getPendingOperations().collectLatest { operations ->
                if (operations.isNotEmpty() && !isProcessing) {
                    processOperations(operations)
                }
            }
        }
    }

    private suspend fun processOperations(operations: List<InboxOperationEntity>) {
        isProcessing = true
        for (op in operations) {
            executeOperation(op)
        }
        isProcessing = false
    }

    private suspend fun executeOperation(op: InboxOperationEntity) {
        val item = inboxDao.getItemById(op.inboxItemId) ?: return
        
        updateOpStatus(op, OperationStatus.Validating)
        inboxDao.updateStatus(item.id, InboxStatus.Processing, System.currentTimeMillis())

        if (!File(item.sourcePath).exists()) {
            failOperation(op, "Source file missing: ${item.sourcePath}")
            return
        }

        updateOpStatus(op, OperationStatus.Executing)
        
        val createdFiles = mutableListOf<String>()
        var success = true
        var errorMessage: String? = null

        try {
            if (op.type == InboxOperationType.MOVE) {
                val firstDest = op.destinationPaths.first()
                val moveResult = physicalAlbumManager.moveFile(item.sourcePath, firstDest)
                if (moveResult.isSuccess) {
                    val firstPath = moveResult.getOrThrow()
                    createdFiles.add(firstPath)
                    
                    for (i in 1 until op.destinationPaths.size) {
                        val copyResult = physicalAlbumManager.copyFile(firstPath, op.destinationPaths[i])
                        if (copyResult.isSuccess) {
                            createdFiles.add(copyResult.getOrThrow())
                        } else {
                            success = false
                            errorMessage = "Partial failure during copy: ${copyResult.exceptionOrNull()?.message}"
                            break
                        }
                    }
                } else {
                    success = false
                    errorMessage = moveResult.exceptionOrNull()?.message
                }
            } else {
                for (dest in op.destinationPaths) {
                    val copyResult = physicalAlbumManager.copyFile(item.sourcePath, dest)
                    if (copyResult.isSuccess) {
                        createdFiles.add(copyResult.getOrThrow())
                    } else {
                        success = false
                        errorMessage = copyResult.exceptionOrNull()?.message
                        break
                    }
                }
            }
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }

        if (success) {
            verifyOperation(op, item, createdFiles)
        } else {
            failOperation(op, errorMessage ?: "Unknown error")
        }
    }

    private suspend fun verifyOperation(op: InboxOperationEntity, item: InboxItemEntity, createdFiles: List<String>) {
        updateOpStatus(op, OperationStatus.Verifying)
        inboxDao.updateStatus(item.id, InboxStatus.Verifying, System.currentTimeMillis())

        val sourceSize = File(item.sourcePath).let { if (it.exists()) it.length() else -1 }
        
        var allVerified = true
        for (path in createdFiles) {
            val f = File(path)
            if (!f.exists() || (sourceSize != -1L && f.length() != sourceSize)) {
                allVerified = false
                break
            }
        }

        if (allVerified) {
            completeOperation(op, item, createdFiles)
        } else {
            failOperation(op, "Verification failed: file size mismatch or missing destination", verificationFailed = true)
        }
    }

    private suspend fun completeOperation(op: InboxOperationEntity, item: InboxItemEntity, createdFiles: List<String>) {
        val now = System.currentTimeMillis()
        operationDao.updateOperation(op.copy(status = OperationStatus.Completed, completedAt = now))
        inboxDao.updateItem(item.copy(
            status = InboxStatus.Completed,
            processingTimestamp = now,
            destinationPaths = op.destinationPaths,
            operationType = op.type
        ))

        val pathsToScan = (createdFiles + if (op.type == InboxOperationType.MOVE) listOf(item.sourcePath) else emptyList())
        MediaScannerConnection.scanFile(context, pathsToScan.toTypedArray(), null, null)
    }

    private suspend fun failOperation(op: InboxOperationEntity, message: String, verificationFailed: Boolean = false) {
        operationDao.updateOperation(op.copy(
            status = OperationStatus.Failed,
            errorMessage = message,
            verificationFailed = verificationFailed
        ))
        inboxDao.updateStatus(op.inboxItemId, InboxStatus.Failed, System.currentTimeMillis())
    }

    private suspend fun updateOpStatus(op: InboxOperationEntity, status: OperationStatus) {
        operationDao.updateOperation(op.copy(status = status, startedAt = System.currentTimeMillis()))
    }
}
