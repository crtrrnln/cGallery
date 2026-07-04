# 10 - Media Layer

## Purpose
The Media Layer is the low-level interface between cGallery and the Android OS. It abstracts the complexities of the MediaStore and the physical file system.

## Components

### 1. MediaStoreDataSource
*   **Role:** Queries the system MediaStore for images and videos.
*   **Logic:** 
    *   Retrieves `BUCKET_DISPLAY_NAME`, `DATA` (path), `DATE_ADDED`, and `_ID`.
    *   Filters by MIME type to ensure only supported media is ingested.

### 2. PhysicalAlbumManager
*   **Role:** Manages the alignment between MediaStore buckets and cGallery albums.
*   **Core Logic (`syncAlbums`):**
    *   Compares current MediaStore buckets with the `physical_albums` table.
    *   Inserts new albums with incremented `sortOrder`.
    *   Prunes orphaned records if the bucket no longer exists in MediaStore AND is gone from disk.

### 3. FileOperator (I/O)
*   Uses `java.io.File` for direct manipulation of the managed root.
*   **Rename Strategy:** Attempts atomic `renameTo()` first for performance.
*   **Fallback Strategy:** Performs `copyTo` followed by `delete` for cross-partition moves.

## Data Schema (Physical Entities)

### `PhysicalAlbumEntity`
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | Primary Key. |
| `bucketName` | String | Unique MediaStore bucket identifier. |
| `isHidden` | Boolean | UI visibility toggle. |
| `groupId` | Long? | Reference to a logical `AlbumGroup`. |
| `sortOrder` | Int | Manual or automatic position in grid. |
| `customCoverUri` | String? | Path to user-selected cover image. |
| `customCoverCrop` | String? | Transformation string (L,T,R,B). |

## Implementation Rules
*   **MediaStore Consistency:** Every physical move must be followed by a `MediaScannerConnection.scanFile` call to prevent desync between the app and the system gallery.
*   **Bucket Uniqueness:** `bucketName` is indexed as UNIQUE in the database.
*   **Sort Logic:** Mixed sorting means albums must be aware of their sibling groups during grid rendering.
