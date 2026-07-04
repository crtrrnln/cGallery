# Inbox Architecture - v0.68

## System Overview
The Inbox system is a decoupled workflow layer designed to handle newly detected media from specific physical folders (Monitored Folders). It tracks the lifecycle of these items from detection to final organisation into user-selected destinations.

## Data Model
The system uses the following entities (persisted via Room):
- **InboxItemEntity**: Tracks a single MediaStore item's workflow state.
    - `mediaStoreId`: Reference to the original media.
    - `status`: `Pending`, `Processing`, `Completed`, `Failed`, `Ignored`.
    - `destinationPaths`: List of multiple physical target folders.
    - `operationType`: `COPY` or `MOVE`.
- **MonitoredFolderEntity**: User-defined physical paths to scan for new media.
    - `ignoreBeforeTimestamp`: Unix timestamp (seconds). Items added to MediaStore before this time are ignored.
- **InboxStatsEntity**: Local metrics for workflow analysis.

## Customisation (v0.63)
- **Custom Covers**: Both `PhysicalAlbumEntity` and `AlbumGroupEntity` support custom cover images via `customCoverUri`.
- **Persistent Cropping**: Users can interactively select a 1:1 crop for their covers, which is stored as a transformation string and applied during rendering.

## Workflow Phases

### 1. Detection Flow (Manual Scan)
- User triggers `scanNow()`.
- System queries `monitored_folders` for enabled paths.
- System queries MediaStore for all media.
- **"Inbox, not Everything Box" Filtering**:
    - For each item, the system finds the matching monitored folder.
    - If `item.dateAdded > folder.ignoreBeforeTimestamp`, the item is processed.
    - Otherwise, it is skipped (implicitly ignored), keeping the Inbox focused only on new content.
- New items are added to the database with `Pending` status.

### 2. Processing Flow
- User selects a `Pending` item.
- User chooses one or more destination folders and an operation (`MOVE` or `COPY`).
- **Multi-destination Logic**:
    - **COPY**: File is copied to every destination path. Original remains.
    - **MOVE**: File is physically moved to the first destination. Then, it is copied from that first destination to all subsequent destinations. The original path is then cleared from MediaStore.
- **Success Criteria**: All destination operations must succeed.
- **Sync**: `MediaScannerConnection` is triggered for all new paths to ensure MediaStore is updated.

## State Diagram
```
[MediaStore Detect] -> Pending
Pending -> Processing (via UI)
Processing -> Completed (All destinations success)
Processing -> Failed (Any destination failure)
Pending -> Ignored (User choice)
Failed -> Processing (Retry)
```

## Future Integration (v0.7)
The architecture is designed to support:
- **Background Enforcement**: Automatic scanning and notification.
- **Conflict Resolution**: Handling existing files in destination folders.
- **UI Stats**: Visualising organisation efficiency.
