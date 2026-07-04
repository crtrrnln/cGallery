# 21 - Data Models

## Purpose
This document defines the Room entities and Enums that constitute the core data layer of cGallery, reflecting the actual implementation in `VirtualAlbum.kt` and `InboxModels.kt`.

## Core Enums

### `InboxStatus`
Defines the state of an item within the triage workflow.
```kotlin
enum class InboxStatus {
    Pending,
    Processing,
    Completed,
    Failed,
    Ignored
}
```

### `InboxOperation`
```kotlin
enum class InboxOperation {
    COPY,
    MOVE
}
```

## Room Entities

### `PhysicalAlbumEntity`
Represents a physical MediaStore bucket.
```kotlin
data class PhysicalAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bucketName: String, // Unique Index
    val isHidden: Boolean = false,
    val groupId: Long? = null,
    val sortOrder: Int = 0,
    val customCoverUri: String? = null,
    val customCoverCrop: String? = null // "left,top,right,bottom"
)
```

### `AlbumGroupEntity`
A logical hierarchy for nesting albums.
```kotlin
data class AlbumGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val customCoverUri: String? = null,
    val customCoverCrop: String? = null
)
```

### `InboxItemEntity`
Tracks the lifecycle of a detected media item.
```kotlin
data class InboxItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long, // Unique Index
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
```

### `MonitoredFolderEntity`
A physical path monitored for new media.
```kotlin
data class MonitoredFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderPath: String, // Unique Index
    val displayName: String,
    val isEnabled: Boolean = true,
    val ignoreBeforeTimestamp: Long = 0L
)
```

### `InboxStatsEntity`
Telemetry for the Inbox workflow.
```kotlin
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
```

## Data Transformation
The system uses **Type Converters** to serialize complex types into SQLite-compatible formats using `kotlinx.serialization`:
*   `List<String>` -> JSON String.
*   `Map<String, Int>` -> JSON String.
*   `Enums` -> String.name.
