package com.example.cgallery.data
import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class OperationQueue(private val context: Context, private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val opDao = db.inboxOperationDao()
    private val inboxDao = db.inboxDao()
    private val physicalAlbumManager = PhysicalAlbumManager(context)
    private var isProcessing = false

    fun start() {
        scope.launch {
            opDao.getPendingOperations().collectLatest { ops ->
                if (ops.isNotEmpty() && !isProcessing) {
                    isProcessing = true
                    for (op in ops) { execOp(op) }
                    isProcessing = false
                }
            }
        }
    }

    private suspend fun execOp(op: InboxOperationEntity) {
        val item = inboxDao.getItemById(op.inboxItemId) ?: return
        updateStatus(op, OperationStatus.Validating)
        inboxDao.updateStatus(item.id, InboxStatus.Processing, System.currentTimeMillis())

        if (!File(item.sourcePath).exists()) {
            fail(op, "source missing"); return
        }

        updateStatus(op, OperationStatus.Executing)
        val created = mutableListOf<String>(); var ok = true; var err: String? = null

        try {
            if (op.type == InboxOperationType.MOVE) {
                val moveRes = physicalAlbumManager.moveFile(item.sourcePath, op.destinationPaths.first())
                if (moveRes.isSuccess) {
                    val first = moveRes.getOrThrow(); created.add(first)
                    for (i in 1 until op.destinationPaths.size) {
                        val cRes = physicalAlbumManager.copyFile(first, op.destinationPaths[i])
                        if (cRes.isSuccess) { created.add(cRes.getOrThrow()) } else { ok = false; err = "copy fail"; break }
                    }
                } else { ok = false; err = moveRes.exceptionOrNull()?.message }
            } else {
                for (dest in op.destinationPaths) {
                    val cRes = physicalAlbumManager.copyFile(item.sourcePath, dest)
                    if (cRes.isSuccess) created.add(cRes.getOrThrow()) else { ok = false; err = cRes.exceptionOrNull()?.message; break }
                }
            }
        } catch (e: Exception) { ok = false; err = e.message }

        if (ok) verify(op, item, created) else fail(op, err ?: "unknown")
    }

    private suspend fun verify(op: InboxOperationEntity, item: InboxItemEntity, created: List<String>) {
        updateStatus(op, OperationStatus.Verifying)
        inboxDao.updateStatus(item.id, InboxStatus.Verifying, System.currentTimeMillis())
        val srcSize = File(item.sourcePath).let { if (it.exists()) it.length() else -1 }
        var verified = true
        for (path in created) {
            val f = File(path)
            if (!f.exists() || (srcSize != -1L && f.length() != srcSize)) { verified = false; break }
        }
        if (verified) done(op, item, created) else fail(op, "verify fail", true)
    }

    private suspend fun done(op: InboxOperationEntity, item: InboxItemEntity, created: List<String>) {
        val now = System.currentTimeMillis()
        opDao.updateOperation(op.copy(status = OperationStatus.Completed, completedAt = now))
        inboxDao.updateItem(item.copy(status = InboxStatus.Completed, processingTimestamp = now, destinationPaths = op.destinationPaths, operationType = op.type))
        val scanPaths = (created + if (op.type == InboxOperationType.MOVE) listOf(item.sourcePath) else emptyList())
        MediaScannerConnection.scanFile(context, scanPaths.toTypedArray(), null, null)
    }

    private suspend fun fail(op: InboxOperationEntity, msg: String, verifyFail: Boolean = false) {
        opDao.updateOperation(op.copy(status = OperationStatus.Failed, errorMessage = msg, verificationFailed = verifyFail))
        inboxDao.updateStatus(op.inboxItemId, InboxStatus.Failed, System.currentTimeMillis())
    }

    private suspend fun updateStatus(op: InboxOperationEntity, s: OperationStatus) = opDao.updateOperation(op.copy(status = s, startedAt = System.currentTimeMillis()))
}
