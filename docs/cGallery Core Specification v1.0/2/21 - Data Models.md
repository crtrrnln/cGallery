# 21 - Data Models

## Purpose
This document defines the core Kotlin data classes and enums used across the application. These models bridge the gap between the Database (Room) and the UI/Business Logic.

## Core Enums

### `MediaItemState`
Defines the lifecycle stage of a media item.
```kotlin
enum class MediaItemState {
    UNPROCESSED,    // Just discovered
    ANALYZING,      // Detection Engine is working
    INBOX,          // Waiting for user/auto-promotion
    MANAGED,        // Assigned to an album
    ARCHIVED,       // Hidden
    TRASHED,        // Marked for deletion
    ERROR_STATE     // System failed to process
}
```

### `AlbumType`
```kotlin
enum class AlbumType {
    DYNAMIC,        // Inferred by Brain
    STATIC,         // Manually created
    SMART,          // Rule-based
    SYSTEM_INBOX,   // Special virtual album
    SYSTEM_TRASH    // Special virtual album
}
```

## Data Entities (Room)

### `MediaItem`
```kotlin
data class MediaItem(
    val id: String = UUID.randomUUID().toString(),
    val hash: String,
    val mimeType: String,
    val originalPath: String,
    val currentPath: String?,
    val size: Long,
    val width: Int?,
    val height: Int?,
    val timestamp: Long,
    val isTimestampEstimated: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val state: MediaItemState = MediaItemState.UNPROCESSED,
    val qualityScore: Float? = null,
    val version: Int = 1
)
```

### `Album`
```kotlin
data class Album(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val type: AlbumType,
    val status: AlbumStatus = AlbumStatus.PROPOSED,
    val coverItemId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val constraints: AlbumConstraints? = null
)
```

## Value Objects (Domain Models)

### `InferenceSignal`
```kotlin
data class InferenceSignal(
    val mediaId: String,
    val type: SignalType,
    val confidence: Float,
    val payload: Map<String, Any>
)
```

### `AlbumConstraints` (for Smart Albums)
```kotlin
data class AlbumConstraints(
    val dateRange: LongRange? = null,
    val locations: List<CircleArea>? = null,
    val tags: List<String> = emptyList(),
    val minQuality: Float? = null
)
```

## UI Models (MVI/MVVM)

### `GalleryThumbnail`
A lightweight model for grid rendering.
```kotlin
data class GalleryThumbnail(
    val id: String,
    val uri: Uri,
    val state: MediaItemState,
    val isSelected: Boolean = false,
    val stackCount: Int = 1 // For burst grouping
)
```

## Mapping Logic
*   `toEntity()`: Domain model to Room entity.
*   `toDomain()`: Room entity to Domain model.
*   `toUiModel()`: Domain model to UI-specific state.
