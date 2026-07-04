# 14 - Workflow Engine

## Purpose
The Workflow Engine manages the lifecycle of media as it moves from the "Inbox" to its final "Managed" state. It ensures that complex multi-step operations (like multi-destination moves) are handled with stateful integrity.

## Lifecycle States (`InboxStatus`)

| State | Description |
| :--- | :--- |
| **Pending** | Newly detected media, awaiting user decision. |
| **Processing** | I/O operations are currently in progress. |
| **Completed** | File(s) moved/copied successfully and MediaStore synced. |
| **Failed** | An I/O error occurred. Ready for retry. |
| **Ignored** | User explicitly removed item from the triage queue. |

## Transition Rules

### 1. Ingestion Transition
*   **Trigger:** `InboxManager.scanNow()` finds an item meeting the temporal and path filters.
*   **Target:** `Pending`.

### 2. Execution Transition
*   **Trigger:** User clicks "Confirm" on a processing screen.
*   **Initial Action:** Set state to `Processing`.
*   **Subsequent Action:** Execute I/O via the **Enforcement Engine**.
*   **Outcome (Success):** Transition to `Completed`, update `processingTimestamp`, and record destination paths.
*   **Outcome (Failure):** Transition to `Failed`, increment `retryCount`, and log the error in `notes`.

### 3. Cleanup Transition
*   **Trigger:** User swipes to ignore or clicks "Ignore."
*   **Target:** `Ignored`. Item remains in DB but is hidden from the active Inbox UI.

## Concurrency & Safety
*   **IO Isolation:** All workflow transitions involving I/O are performed on `Dispatchers.IO`.
*   **State Locking:** The database uses `OnConflictStrategy.IGNORE` for initial ingestion to prevent race conditions during rapid scans.
*   **Stats Integration:** Every `Completed` or `Failed` transition triggers an update to `InboxStatsEntity` to maintain system-wide metrics.
