package com.example.cgallery.data

import androidx.room.*
import kotlinx.serialization.Serializable

enum class InboxStatus {
    Detected,
    Queued,
    WaitingForUser,
    Processing,
    Verifying,
    Completed,
    Ignored,
    Failed,
    RetryPending,
    Cancelled
}

enum class InboxOperationType {
    COPY,
    MOVE
}

@Entity(tableName = "inbox_items", indices = [Index(value = ["mediaStoreId"], unique = true)])
data class InboxItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long,
    val mediaUri: String,
    val filename: String,
    val sourcePath: String,
    val detectedTimestamp: Long,
    val processingTimestamp: Long? = null,
    val lastTransitionTimestamp: Long = System.currentTimeMillis(),
    val status: InboxStatus = InboxStatus.Detected,
    val destinationPaths: List<String> = emptyList(),
    val operationType: InboxOperationType = InboxOperationType.MOVE,
    val retryCount: Int = 0,
    val notes: String? = null,
    val operationId: Long? = null
)

@Entity(tableName = "inbox_operations")
data class InboxOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inboxItemId: Long,
    val type: InboxOperationType,
    val destinationPaths: List<String>,
    val status: OperationStatus = OperationStatus.Queued,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val verificationFailed: Boolean = false
)

enum class OperationStatus {
    Queued,
    Validating,
    Executing,
    Verifying,
    Completed,
    Failed
}

@Serializable
@Entity(tableName = "monitored_folders", indices = [Index(value = ["folderPath"], unique = true)])
data class MonitoredFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderPath: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val ignoreBeforeTimestamp: Long = 0L 
)

@Entity(tableName = "inbox_stats")
data class InboxStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalDetected: Int = 0,
    val totalCompleted: Int = 0,
    val totalFailed: Int = 0,
    val totalIgnored: Int = 0,
    val totalProcessingTimeMs: Long = 0L,
    val sourceFolderCounts: Map<String, Int> = emptyMap(),
    val destinationFolderCounts: Map<String, Int> = emptyMap()
)
