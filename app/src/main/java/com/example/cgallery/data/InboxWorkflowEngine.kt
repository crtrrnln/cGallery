package com.example.cgallery.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class InboxEvent {
    data class ItemDetected(val item: InboxItemEntity) : InboxEvent()
    data class ItemProcessing(val item: InboxItemEntity) : InboxEvent()
    data class ItemCompleted(val item: InboxItemEntity) : InboxEvent()
    data class ItemFailed(val item: InboxItemEntity, val reason: String) : InboxEvent()
}

class InboxWorkflowEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val inboxDao = db.inboxDao()
    private val operationDao = db.inboxOperationDao()

    private val _events = MutableSharedFlow<InboxEvent>()
    val events = _events.asSharedFlow()

    suspend fun onItemsDetected(items: List<InboxItemEntity>) {
        items.forEach {
            _events.emit(InboxEvent.ItemDetected(it))
        }
    }

    fun submitForProcessing(items: List<InboxItemEntity>, destinations: List<String>, type: InboxOperationType) {
        scope.launch {
            items.forEach { item ->
                val operation = InboxOperationEntity(
                    inboxItemId = item.id,
                    type = type,
                    destinationPaths = destinations
                )
                val opId = operationDao.insertOperation(operation)
                
                inboxDao.updateItem(item.copy(
                    status = InboxStatus.Queued,
                    operationId = opId,
                    lastTransitionTimestamp = System.currentTimeMillis()
                ))
                
                _events.emit(InboxEvent.ItemProcessing(item))
            }
        }
    }

    fun retryFailedItem(item: InboxItemEntity) {
        scope.launch {
            inboxDao.updateStatus(item.id, InboxStatus.RetryPending, System.currentTimeMillis())
            // Implementation of retry logic would reset the operation status or create a new one
            val op = item.operationId?.let { operationDao.getOperationById(it) }
            if (op != null) {
                operationDao.updateOperation(op.copy(status = OperationStatus.Queued, errorMessage = null))
            } else {
                // If no operation exists, re-submit?
            }
        }
    }

    fun ignoreItem(item: InboxItemEntity) {
        scope.launch {
            inboxDao.updateStatus(item.id, InboxStatus.Ignored, System.currentTimeMillis())
        }
    }
}
