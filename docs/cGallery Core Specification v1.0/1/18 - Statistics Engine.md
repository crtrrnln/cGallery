# 18 - Statistics Engine

## Purpose
The Statistics Engine provides telemetry on the library's health and the efficiency of the Inbox workflow. It persists data in the `inbox_stats` table for use in the UI and optimization.

## Metrics Categories

### 1. Workflow Telemetry (`InboxStatsEntity`)
*   **Total Detected:** Lifetime count of all media identified by the Discovery Engine.
*   **Total Completed:** Count of successfully triaged items (MOVE/COPY finished).
*   **Total Failed:** Count of items that encountered I/O errors during triage.
*   **Total Ignored:** Items explicitly removed from the Inbox by the user.

### 2. Performance Tracking
*   **Processing Efficiency:** Calculated as `totalProcessingTimeMs / totalCompleted`. This measures the average time for the Enforcement Engine to finalize a triage request.
*   **Error Rate:** % of total detections that result in a `Failed` state.

### 3. Logistical Distribution
*   **Source Folder Counts:** A frequency map of where new media is being discovered (e.g., "70% from Downloads").
*   **Destination Folder Counts:** A frequency map of where users are organizing their media, identifying the most "Active" albums in the managed root.

## Data Persistence
Statistics are stored in a single-row `inbox_stats` table for high-speed updates.

### Schema: `InboxStatsEntity`
| Field | Description |
| :--- | :--- |
| `id` | PK (Constant 1). |
| `totalDetected` | Total unique `mediaStoreId`s ingested. |
| `totalCompleted` | Success count. |
| `totalFailed` | Failure count. |
| `totalIgnored` | Suppression count. |
| `totalProcessingTimeMs` | Aggregated I/O and Sync time. |
| `sourceFolderCounts` | JSON-serialized map of source paths. |
| `destinationFolderCounts` | JSON-serialized map of target paths. |

## Update Cycle
The Statistics Engine does not run on a timer; it is reactive to **Workflow Engine** events:
1.  **On Discovery:** Increment `totalDetected`.
2.  **On Completion:** Increment `totalCompleted`, add duration to `totalProcessingTimeMs`, and update frequency maps.
3.  **On Failure:** Increment `totalFailed`.

## Usage in UI
*   **Dashboard:** Shows "Library Health" (Completed vs. Total).
*   **Inbox Detail:** Can show average processing speed to the user.
*   **Export/Import:** The statistics are included in the organization export (v0.61) to preserve library history.
