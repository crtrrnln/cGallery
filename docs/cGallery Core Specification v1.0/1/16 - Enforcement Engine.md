# 16 - Enforcement Engine

## Purpose
The Enforcement Engine is the "muscle" of cGallery. It translates logical organizational decisions into physical file system actions and ensures the Android OS remains in sync with these changes.

## Core Responsibilities

### 1. Physical Relocation (File Sync)
The engine implements the atomic move/copy logic found in `PhysicalAlbumManager`.
*   **Atomic Move:** 
    1.  Verify source existence.
    2.  Ensure target directory exists (`mkdirs`).
    3.  Attempt `renameTo`.
    4.  If failed, perform `copyTo` + `delete`.
*   **Conflict Prevention:** If a target file already exists, the engine returns a `Failure` to prevent accidental overwrites.

### 2. MediaStore Reconciliation
Android's `MediaStore` is often delayed. The Enforcement Engine forces synchronization:
*   **Trigger:** `MediaScannerConnection.scanFile()`.
*   **Scope:** Must be called for *all* created files and the *source* file (if moved).
*   **Significance:** This ensures that the new files appear immediately in cGallery and other system-integrated apps.

### 3. Background Enforcement (Future v1.0)
Currently manual (`scanNow`), the engine is architected to support:
*   **Periodic Scanning:** Running the Inbox detection logic in the background via `WorkManager`.
*   **Notification:** Alerting the user when new "Pending" items are available in the Inbox.

## Execution Logic (The Multi-Destination Loop)

```kotlin
// Logic for moving to 3 folders
val firstDest = destinations[0]
if (moveFile(source, firstDest).isSuccess) {
    for (i in 1 until destinations.size) {
        copyFile(firstDest, destinations[i])
    }
    // Final Step: Scan everything
    MediaScannerConnection.scanFile(context, allPaths, null)
}
```

## Performance Targets
*   **Move Latency:** < 50ms (for same-partition rename).
*   **Throughput:** Must handle batches of 50+ media items without blocking the UI thread.
*   **Retry Logic:** Failed operations remain in the `Failed` state, allowing the user to troubleshoot (e.g., full disk) and retry manually.
