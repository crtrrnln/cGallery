# 02 - Design Principles

## 1. File System is the Source of Truth
Metadata in cGallery exists to reflect and manage the physical file system. If an album is deleted, the folder is affected. If a file is moved, the disk is updated. The app is a lens through which the user shapes their storage.

## 2. Samsung-Inspired Aesthetics
The UI follows a specific design language:
*   **24dp Rounded Corners:** Used for all album and group covers.
*   **High Density:** 2dp spacing in grids to maximize information density.
*   **Administrative Minimalism:** Tools and "dangerous" operations are hidden in 3-dot overflow menus to keep the main view focused on media.

## 3. "Inbox, Not Everything Box"
The Inbox system must never overwhelm the user. It uses strict `ignoreBeforeTimestamp` filters on monitored folders to ensure only *new* content is surfaced for triage.

## 4. Multi-Destination Atomicity
The system supports organizing a single photo into multiple destinations. The **Enforcement Engine** must handle this atomically:
*   **MOVE** to the first destination.
*   **COPY** to all subsequent destinations from the new location.
*   **SYNC** all changes with the MediaStore immediately.

## 5. Mixed Content Sorting
Logical hierarchies (Album Groups) and physical entities (Albums) are treated as first-class peers. In the grid, groups and albums intermingle based on sort order and naming, providing a cohesive browsing experience.

## 6. Performance over Everything
Animations must be session-based and cinematic but never block the user. Scrolling must be buttery smooth. File operations must occur in background workers with robust error handling.

## 7. Explicit User Intent
No "magic" AI organization. The system detects new files, but the user decides where they go. The system's job is to make that decision as fast and frictionless as possible.
