# 15 - Discovery Engine

## Purpose
The Discovery Engine (formerly the Detection Engine) is responsible for identifying new media that qualifies for the Inbox triage workflow. It provides the "trigger" for the system's organizational lifecycle without the use of AI or external inference.

## Detection Logic: "The Temporal Gate"
Discovery is based on a strict spatio-temporal filter applied to Monitored Folders.

### 1. Monitored Path Filtering
The engine only considers media located within paths explicitly defined in the `monitored_folders` table (e.g., `/storage/emulated/0/DCIM/Camera`).

### 2. The `ignoreBeforeTimestamp` Rule
To prevent the Inbox from being flooded with historical media, every monitored folder has an `ignoreBeforeTimestamp`.
*   **Logic:** `media.dateAdded > folder.ignoreBeforeTimestamp`
*   **Significance:** This ensures that only files added to the device *after* the user enabled monitoring for that folder are queued for triage.

## Ingestion Pipeline (`scanNow`)

1.  **Source Sync:** Query the `monitored_folders` table for all enabled paths.
2.  **MediaStore Query:** Fetch all media items (Images/Videos) from the Android MediaStore.
3.  **Matching:** For each media item, find the `MonitoredFolderEntity` whose `folderPath` is a prefix of the media's bucket path.
4.  **Temporal Validation:** Apply the `ignoreBeforeTimestamp` filter.
5.  **Database Check:** Verify the `mediaStoreId` does not already exist in the `inbox_items` table (to prevent duplicate triage).
6.  **Promotion:** Create a new `InboxItemEntity` with status `Pending`.

## Manual vs. Automatic Discovery
*   **Current (v0.7pre1):** Discovery is triggered manually by the user via the "Scan Now" action in the administrative menu.
*   **Future:** Background discovery using `WorkManager` or `FileObserver` to provide real-time Inbox updates.

## Performance Considerations
*   **Indexing:** The `inbox_items` table uses a unique index on `mediaStoreId` to ensure rapid lookups during the scan process.
*   **IO Efficiency:** The engine relies on MediaStore metadata rather than reading individual file headers, allowing it to scan thousands of items in sub-second timeframes.
