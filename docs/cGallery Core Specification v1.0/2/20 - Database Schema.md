# 20 - Database Schema

## Overview
cGallery uses a Room-based SQLite database (`virtual_albums.db`). The schema focuses on tracking physical locations, logical groupings, and inbox workflow states.

## Core Tables

### 1. `physical_albums`
Maps MediaStore buckets to the application's organizational structure.
*   `id` (Long, PK)
*   `bucketName` (String, Unique Index): The MediaStore identifier.
*   `isHidden` (Boolean)
*   `groupId` (Long?, FK): Link to `album_groups`.
*   `sortOrder` (Int)
*   `customCoverUri` (String?)
*   `customCoverCrop` (String?)

### 2. `album_groups`
Logical hierarchies for organization.
*   `id` (Long, PK)
*   `name` (String)
*   `parentId` (Long?, FK): Link to another `album_groups` (Nesting).
*   `sortOrder` (Int)
*   `customCoverUri` (String?)
*   `customCoverCrop` (String?)

### 3. `inbox_items`
Tracks the triage workflow.
*   `id` (Long, PK)
*   `mediaStoreId` (Long, Unique Index)
*   `mediaUri` (String)
*   `filename` (String)
*   `sourcePath` (String)
*   `detectedTimestamp` (Long)
*   `status` (String): Pending, Processing, Completed, Failed, Ignored.
*   `destinationPaths` (List<String>)
*   `operationType` (String): MOVE, COPY.

### 4. `monitored_folders`
Physical paths scanned for new media.
*   `id` (Long, PK)
*   `folderPath` (String, Unique Index)
*   `displayName` (String)
*   `isEnabled` (Boolean)
*   `ignoreBeforeTimestamp` (Long)

### 5. `inbox_stats`
Single-row table for telemetry.
*   `id` (Int, PK, default=1)
*   `totalDetected` / `totalCompleted` / `totalFailed` / `totalIgnored`
*   `totalProcessingTimeMs`
*   `sourceFolderCounts` / `destinationFolderCounts` (JSON Maps)

## Data Integrity
*   **Unique Constraints:** `bucketName`, `mediaStoreId`, and `folderPath` are strictly enforced to prevent duplicates.
*   **Foreign Keys:** `groupId` and `parentId` use `CASCADE` behavior for robust deletion handling.
*   **Type Converters:** Custom converters handle `List<String>`, `Map<String, Int>`, and Enum-to-String serialization via `kotlinx.serialization`.
