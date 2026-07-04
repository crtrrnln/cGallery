# 40 - Performance

## Purpose
cGallery is built for speed. This document defines the performance targets and optimization strategies required to handle high-density media grids and intensive file operations.

## Performance Targets

| Metric | Target | Condition |
| :--- | :--- | :--- |
| **Startup Animation** | 60 FPS | Session-based cinematic reveal |
| **Grid Scroll** | 120 FPS | On supported devices |
| **Bucket Sync** | < 200ms | Library with 50+ buckets |
| **Inbox Scan** | < 1s | 5,000+ total media items |
| **Move/Rename** | < 50ms | Atomic operation (same partition) |

## Optimization Strategies

### 1. High-Density Rendering (Compose)
*   **Grid Spacing:** Use 2dp padding to maximize visual throughput.
*   **Lazy Loading:** Strict use of `LazyVerticalGrid` with key-based item tracking.
*   **Corner Optimization:** Pre-calculating 24dp rounded clip paths to avoid GPU overdraw.

### 2. Database & I/O
*   **Unique Indexing:** Strict indexing on `bucketName` and `mediaStoreId` to ensure `O(1)` lookups during discovery scans.
*   **Serialization:** Using `kotlinx.serialization` for JSON maps in `InboxStats` to minimize reflection overhead.
*   **Async Triage:** All MOVE/COPY operations are delegated to `Dispatchers.IO` to ensure the UI remains interactive during multi-destination processing.

### 3. MediaStore Sync
*   **Batch Scanning:** Instead of scanning every file individually, use batch `MediaScannerConnection` calls where possible.
*   **Metadata Caching:** Relying on MediaStore's pre-indexed metadata for the "Discovery Engine" rather than deep-reading physical file headers.

### 4. Memory Management
*   **Persistent Cropping:** Storing crop strings rather than transformed bitmaps to save storage and RAM.
*   **Collage covers:** Dynamically generating group covers from child album items using a prioritized LRU cache.

## Monitoring
*   **Developer Mode (v0.7):** Heartbeat monitors for Discovery and Enforcement engines.
*   **Debug Logs:** Detailed tracking of cross-partition fallback moves (Copy + Delete).
