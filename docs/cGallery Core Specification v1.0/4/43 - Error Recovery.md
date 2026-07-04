# 43 - Error Recovery

## Purpose
Digital assets are precious. cGallery must prioritize data integrity and provide robust recovery mechanisms for when things go wrong.

## Error Categories & Responses

### 1. File System Errors
*   **Error:** Permission Denied (SAF/MediaStore).
*   **Response:** Pause the **Enforcement Engine**, show a "Fix Permissions" banner in the UI.
*   **Error:** Disk Full.
*   **Response:** Suspend all ingestion and movement. Notify user with specific space required.

### 2. Database Errors
*   **Error:** SQLite Constraint Violation.
*   **Response:** Rollback transaction, log the conflict, and mark the operation as `Failed`.
*   **Error:** Database Corruption.
*   **Response:** Use the "Import Structure" (v0.61) feature to restore organization from a previous backup.

### 3. File Operation Failures (Enforcement Engine)
*   **Error:** File disappeared during move or rename.
*   **Response:** Verify destination existence. If missing, mark `InboxItemEntity` as `Failed` and do not delete the source record.

### 4. Lifecycle Conflicts
*   **Error:** Source file modified during triage.
*   **Response:** Abort the current MOVE/COPY command, re-verify file size/hash, and prompt the user.

## Automatic Self-Healing (The "Janitor")
A process (triggered by "Scan Now") that ensures consistency:
1.  **Orphan Check:** Find albums in the database whose physical bucket path no longer exists.
2.  **Zombie Check:** Find files in the managed root that are not yet indexed.
3.  **Sync Check:** Ensure `MediaScannerConnection` is triggered for all recent I/O.

## User-Facing Recovery Tools
*   **"Export/Import Structure":** Allows users to backup their logical grouping and organization history to a JSON file.
*   **"Fix Broken Links":** Re-runs the `syncAlbums` logic to align with MediaStore.
*   **"Export Debug Bundle":** Zips internal logs and the database (excluding media) for technical support.

## Logging & Auditing
Every significant error is recorded in the `notes` field of the `inbox_items` or the `EnforcementLog` (if implemented).

## Recovery Priority
1.  **Safety First:** Never delete a file if its status is uncertain.
2.  **User Intent Second:** If an operation fails, keep the item in the `Failed` state for manual intervention.
3.  **Persistence:** Use Room migrations to preserve data across app updates.
