# 13 - Inbox System

## Purpose
The Inbox System is a decoupled workflow layer for triaging new media. It transforms a "Media Stream" into an "Organized Library" by requiring explicit destination decisions for new assets.

## Core Entities

### `InboxItemEntity`
*   **mediaStoreId:** Unique link to the system record.
*   **status:** Tracks the item through the workflow (Pending -> Completed).
*   **destinationPaths:** A list of one or more target folder paths.
*   **operationType:** Either `MOVE` or `COPY`.
*   **retryCount:** Tracks failure attempts for robust error recovery.

### `MonitoredFolderEntity`
*   **folderPath:** The physical path to watch (e.g., `/DCIM/Camera`).
*   **ignoreBeforeTimestamp:** A Unix timestamp filter. Only media added *after* this time is considered "Inbox worthy."

## The Triage Workflow

### 1. Manual Scan (`scanNow`)
1.  Query all enabled `monitored_folders`.
2.  Fetch all media from MediaStore.
3.  **Filtering:** If `media.fullPath` starts with `folder.folderPath` AND `media.dateAdded > folder.ignoreBeforeTimestamp`, proceed.
4.  **Deduplication:** Check if `mediaStoreId` already exists in `inbox_items`.
5.  **Ingestion:** Create `Pending` record.

### 2. Decision Logic
The UI presents the user with a choice:
*   **Target Albums:** One or more destinations.
*   **Mode:** 
    *   **MOVE:** Clean up the source folder.
    *   **COPY:** Keep the original and create duplicates in the library.

### 3. Execution (The Multi-Destination Loop)
*   If **MOVE**:
    *   `moveFile` to `destinations[0]`.
    *   `copyFile` from `destinations[0]` to `destinations[1..n]`.
    *   Clean up the original file path.
*   If **COPY**:
    *   `copyFile` to all `destinations[0..n]`.

## UX Design
*   **Inbox Navigation:** A dedicated workflow with a clear exit/back button.
*   **Scan Now:** A prominent action in the administrative menu to trigger manual detection.
*   **Efficiency:** Designed to handle "batch" decisions, though v0.7pre1 focuses on per-item processing.
