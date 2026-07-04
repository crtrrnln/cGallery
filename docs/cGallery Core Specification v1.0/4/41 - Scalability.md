# 41 - Scalability

## Purpose
cGallery must scale linearly with the size of the user's media library. A library with 1,000,000 items should not be significantly slower than one with 1,000.

## Architectural Scalability

### 1. Paging Data
*   **Room Paging:** Use `Paging 3` library for all large list queries.
*   **Windowing:** Only load 50-100 items into memory at a time.
*   **Pre-fetching:** Request the next page when the user is 10 items from the bottom.

### 2. Sharded Detection
*   The **Detection Engine** should not attempt to analyze the entire library in one pass.
*   **Chunking:** Divide the library into temporal chunks (e.g., "This Month", "Last Month") and process them in reverse chronological order.

### 3. Sparse Indexing
*   Visual embeddings are high-dimensional. For massive libraries, use **Locality Sensitive Hashing (LSH)** or a **Vector Database** (like ObjectBox or a lightweight specialized SQLite extension) to find similar items without O(n²) comparisons.

## Storage Scalability

### 1. Database Growth
*   Estimated 1KB per `MediaItem` in the DB.
*   100k items = 100MB DB file.
*   **Management:** Periodically run `VACUUM` to reclaim space.

### 2. Thumbnail Cache
*   Thumbnails can consume gigabytes.
*   **Strategy:** 
    *   Quota-based cache (e.g., max 2GB).
    *   LRU (Least Recently Used) eviction.
    *   Tiered Quality: High-res for recent/active albums, low-res for old/archived media.

## Concurrency Scalability

### 1. Engine Parallelism
*   **Scalable Pool:** Use a `CoroutineDispatcher` with a fixed number of threads (e.g., `n+1` where n is CPU core count).
*   **Isolation:** Ensure the **Detection Engine** doesn't starve the **Media Layer** of CPU cycles.

### 2. Contention Management
*   SQLite only supports one writer at a time.
*   **Strategy:** Use a **Producer-Consumer** pattern with an internal queue to serialize DB writes, while allowing unlimited parallel reads (via WAL mode).

## Edge Cases

### 1. Sudden Import
*   If 50,000 photos are added at once (e.g., SD card insertion):
    1.  Phase 1: Metadata index (Fast).
    2.  Phase 2: Deferred inference (Slow, backgrounded).
    3.  Notification: "Organizing your new photos... this may take some time."

### 2. Low Memory Devices
*   Drop the ML inference complexity.
*   Reduce the thumbnail cache size.
*   Disable non-essential background stats.
