# 22 - State Machines

## Purpose
The behavior of cGallery is governed by formal state machines. This ensures that media transitions are predictable, reversible, and consistent across all engines.

## 1. Media Item Lifecycle

### State Definitions
1.  **UNPROCESSED:** Initial state. Engines ignore this item except for the **Detection Engine**.
2.  **INBOX:** Item is "visible" to the user but not "placed." It's in the triage queue.
3.  **MANAGED:** Item is "filed." It has a primary album and/or physical directory.
4.  **ARCHIVED:** Item is suppressed from the main gallery view but retained in the DB.
5.  **TRASHED:** Item is marked for deletion.

### Transition Table

| Event | Current State | Target State | Side Effect |
| :--- | :--- | :--- | :--- |
| `DISCOVERED` | - | `UNPROCESSED` | Index metadata |
| `ANALYSIS_DONE (High Conf)` | `UNPROCESSED` | `MANAGED` | Move to Album folder |
| `ANALYSIS_DONE (Low Conf)` | `UNPROCESSED` | `INBOX` | Notify User |
| `USER_ACCEPT` | `INBOX` | `MANAGED` | Sync Physical |
| `USER_REJECT` | `INBOX` | `MANAGED` | Move to "General" Album |
| `USER_DELETE` | `ANY` | `TRASHED` | Set expiry timer |
| `USER_RESTORE` | `TRASHED` | `MANAGED` | Restore path |
| `PURGE_TIMER` | `TRASHED` | - (DELETED) | Physical Delete |

## 2. Album Lifecycle

### State Definitions
1.  **PROPOSED:** Created by Detection Engine. Invisible to regular gallery view unless in "Review Mode."
2.  **ACTIVE:** Confirmed by user or system threshold. Full visibility.
3.  **ARCHIVED:** Album is collapsed and hidden from main view.

### Transition Logic
*   **PROPOSED -> ACTIVE:** Triggered if `confidence > 0.9` or `user.confirm()`.
*   **ACTIVE -> ARCHIVED:** Triggered by `user.archive()`.

## 3. Enforcement State (Command Lifecycle)

Every file operation follows this flow:
1.  **PENDING:** Command created.
2.  **EXECUTING:** IO operation in progress.
3.  **SUCCESS:** IO complete, DB updated.
4.  **FAILED:** IO error.
5.  **RETRYING:** Waiting for backoff.

## Implementation Details
*   State machines are implemented using **Kotlin Sealed Classes** to represent states and events.
*   The **Workflow Engine** acts as the dispatcher.
*   **Idempotency Check:** Every transition must check if the target state is already reached before executing side effects.

## Conflict Resolution
If a user tries to delete an item (`MANAGED -> TRASHED`) while the Detection Engine is analyzing it (`ANALYZING`):
*   **Rule:** User actions cancel background analysis.
*   **Implementation:** The Workflow Engine sends a `CANCEL` signal to the Detection Engine for that `media_id`.
