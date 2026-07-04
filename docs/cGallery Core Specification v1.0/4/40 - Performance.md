# 40 - Performance

## Purpose
This document defines the performance targets and optimization strategies to ensure cGallery remains responsive even with libraries of 100,000+ items.

## Performance Targets

| Metric | Target | Condition |
| :--- | :--- | :--- |
| **App Launch (Cold)** | < 800ms | 10k items library |
| **Grid Scroll** | 60 FPS (Stable) | 120Hz support where available |
| **Thumbnail Load** | < 50ms | Once in viewport |
| **Search Query** | < 100ms | DB indexed |
| **Engine Overhead** | < 5% CPU | Idle/Background |
| **Inference Latency** | < 2s | Single item analysis |

## Optimization Strategies

### 1. Database Optimization
*   **Indexing:** All columns used in `WHERE` or `ORDER BY` clauses (state, timestamp, album_id) must be indexed.
*   **Projection:** Never use `SELECT *`. Only query the columns needed for the specific UI component.
*   **Batching:** All writes to the DB must be batched using transactions (e.g., 50 inserts per transaction).

### 2. UI Rendering (Compose)
*   **Lazy Loading:** Use `LazyVerticalGrid` and `LazyColumn`.
*   **Sub-composition:** Minimize recomposition by using `remember` and `derivedStateOf`.
*   **Image Pipeline:** Use **Coil** or **Glide** with a custom `Fetcher` that prioritizes the **Media Layer's** local thumbnail cache.

### 3. Background Engine Management
*   **Priority Queuing:**
    *   `IMMEDIATE`: State changes triggered by user.
    *   `HIGH`: Ingestion of new files.
    *   `LOW`: Deep ML inference and stats.
*   **Resource Throttling:** Engines pause when the UI is "Active" and the library is being scrolled to prevent frame drops.

### 4. Memory Management
*   **Bitmaps:** Use `RGB_565` for thumbnails to save 50% RAM vs `ARGB_8888`.
*   **Caching:** 
    *   L1: Memory cache (LRU).
    *   L2: Disk cache (Internal storage).
    *   L3: Physical files (Originals).

### 5. Disk I/O
*   Avoid frequent small writes. Buffer `EnforcementLog` entries and commit in chunks.
*   Use `FileChannel` or `BufferedSource` for metadata extraction.

## Monitoring
*   **Tracepoints:** Use `androidx.tracing` to mark engine start/stop events.
*   **LeakCanary:** Integrated in debug builds to detect memory leaks.
*   **Firebase Performance:** (Optional) Track real-world metrics on diverse hardware.
