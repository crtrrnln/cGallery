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
*   **Response:** Rollback transaction, log the conflict, mark the item as `ERROR_STATE`.
*   **Error:** Database Corruption.
*   **Response:** Restore from the most recent metadata export (if available) or trigger a full library re-scan (`UNPROCESSED` flow).

### 3. Inference Engine Failures
*   **Error:** ML Model crash/out-of-memory.
*   **Response:** Kill the process, retry once with reduced complexity, then fall back to basic heuristic clustering (Timestamp-only).

### 4. Lifecycle Conflicts
*   **Error:** File disappeared during move.
*   **Response:** Check if it exists at the destination. If not, mark as `MISSING`. Do not delete the database record yet.

## Automatic Self-Healing (The "Janitor")
A background process that runs weekly or on-demand:
1.  **Orphan Check:** Find `MediaItems` with no physical file.
2.  **Zombie Check:** Find files in managed folders with no DB entry.
3.  **Hash Verification:** Re-calculate hashes for a random 1% of the library to detect silent corruption.
4.  **Consistency Check:** Ensure every `MANAGED` item belongs to an active album.

## User-Facing Recovery Tools
*   **"Reset Library":** Deletes the database but *not* the photos. Useful for a fresh start with the "Brain."
*   **"Fix Broken Links":** Triggers the Orpan/Zombie check manually.
*   **"Export Logs":** For technical support.

## Logging & Auditing
Every error is logged in the `ErrorLog` table:
| Field | Description |
| :--- | :--- |
| `timestamp` | When it happened. |
| `module` | `DETECTION`, `ENFORCEMENT`, etc. |
| `severity` | `WARNING`, `CRITICAL`, `FATAL`. |
| `context` | `media_id`, `path`, etc. |
| `stack_trace` | Technical details. |

## Recovery Priority
1.  **Safety First:** Never delete a file if its status is uncertain.
2.  **Metadata Second:** Re-indexing is cheap; physical files are irreplaceable.
3.  **UI Continuity:** Don't crash the app; show a "Reduced Functionality" state.
