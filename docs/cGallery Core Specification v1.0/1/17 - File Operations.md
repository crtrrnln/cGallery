# 17 - File Operations

## Purpose
This module provides a unified abstraction layer for all direct disk and MediaStore interactions. It ensures that file handling is consistent across Android versions (Scoped Storage, SAF, MediaStore).

## Abstraction Layer: `FileOperator`

The system should not call `java.io.File` directly. Instead, it uses a `FileOperator` interface.

### Key Methods
*   `move(source: Uri, target: Path): Result<Uri>`
*   `copy(source: Uri, target: Path): Result<Uri>`
*   `delete(uri: Uri): Result<Boolean>`
*   `readMetadata(uri: Uri): MetadataMap`
*   `writeMetadata(uri: Uri, updates: MetadataMap): Result<Boolean>`
*   `getThumbnail(uri: Uri, size: Size): Bitmap?`

## Android Version Compatibility

### Android 10+ (Scoped Storage)
*   **Media Capture:** Use MediaStore for images/videos in standard collections.
*   **Organized Folders:** Use `Storage Access Framework (SAF)` for full directory access if the user chooses a custom managed directory.
*   **Permissions:** Request `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` (legacy) or `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`.

## Atomic Operations Pattern
To prevent file corruption or orphaned records:

1.  **Prepare:** Check destination space and permissions.
2.  **Shadow Write (Optional):** Copy to a `.tmp` file first.
3.  **Rename:** Use atomic `renameTo()` if on the same partition.
4.  **Verify:** Check hash of the moved file.
5.  **Commit:** Update the Database `MediaItem` path.
6.  **Cleanup:** Remove source/temp files.

## Conflict Strategies
When a destination path is occupied:
1.  **HASH_MATCH:** If the existing file has the same hash as the source, simply delete the source (de-duplication).
2.  **RENAME_APPEND:** Append a counter suffix: `file.jpg` -> `file (1).jpg`.
3.  **REPLACE:** Only if the source is explicitly marked as an "Update" to an existing record.

## Background Execution
*   All file operations must run on a `Dispatchers.IO` thread.
*   Use `WorkManager` for long-running batch operations to ensure completion if the app is backgrounded.

## Logging & Audit
Every file operation must be logged in an internal `FileOperationAudit` table:
*   `Timestamp`
*   `OperationType` (MOVE, DELETE, RENAME)
*   `SourcePath`
*   `DestinationPath`
*   `Status`
*   `ErrorMessage` (if applicable)
