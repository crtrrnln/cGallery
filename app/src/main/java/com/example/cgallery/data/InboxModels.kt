package com.example.cgallery.data

import androidx.room.*
import kotlinx.serialization.Serializable

enum class InboxStatus {
    Pending,
    Processing,
    Completed,
    Failed,
    Ignored
}

enum class InboxOperation {
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
    val status: InboxStatus = InboxStatus.Pending,
    val destinationPaths: List<String> = emptyList(),
    val operationType: InboxOperation = InboxOperation.MOVE,
    val retryCount: Int = 0,
    val notes: String? = null
)

@Serializable
@Entity(tableName = "monitored_folders", indices = [Index(value = ["folderPath"], unique = true)])
data class MonitoredFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderPath: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val ignoreBeforeTimestamp: Long = 0L // Items added before this will be ignored
)

@Entity(tableName = "inbox_stats")
data class InboxStatsEntity(
    @PrimaryKey val id: Int = 1, // Only one stats row
    val totalDetected: Int = 0,
    val totalCompleted: Int = 0,
    val totalFailed: Int = 0,
    val totalIgnored: Int = 0,
    val totalProcessingTimeMs: Long = 0L,
    val sourceFolderCounts: Map<String, Int> = emptyMap(),
    val destinationFolderCounts: Map<String, Int> = emptyMap()
)
