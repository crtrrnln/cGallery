# 33 - Developer Mode

## Purpose
Developer Mode is a diagnostic suite for engineering and QA to monitor the state of the "Brain" and the performance of internal engines.

## Activation
*   Tapping the "App Version" in Settings 7 times.
*   Persistent notification when active.

## Features

### 1. Engine Heartbeat
A real-time dashboard showing the status of each core module:
*   **Media Layer:** "Scanning..." or "Idle". Last file found.
*   **Detection Engine:** Current task (e.g., "pHash Generation"), Queue size.
*   **Workflow Engine:** Pending state transitions.
*   **Enforcement Engine:** IO queue depth, Success/Failure rate.

### 2. Inference Inspector
When viewing a `MediaItem`, developers can see raw engine data:
*   Raw Confidence Scores for every suggested album.
*   Visual Embedding vector (first 8 values).
*   pHash value.
*   Metadata dump (EXIF, MediaStore ID).

### 3. Database Explorer
*   Query runner for the Room database.
*   Table size visualization.
*   Trigger manual DB maintenance (Vacuum, Index Rebuild).

### 4. Stress Test Tools
*   **Force Re-Scan:** Trigger a full scan of all monitored folders.
*   **Simulate Load:** Artificially inject 1,000 dummy `MediaItems`.
*   **Thermal Throttling Toggle:** Disable heat-based engine pausing (DANGER).

### 5. Logging Console
*   Tail the internal system logs.
*   Filter by tag (`DETECTION`, `WORKFLOW`, `IO`).
*   "Export Debug Bundle": Zips the DB (without photos), logs, and system info for bug reporting.

## Visual Overlays (Optional)
*   **FPS Counter:** Standard Android overlay.
*   **Memory Usage:** Current heap vs. available.
*   **Engine Badges:** Small dots on thumbnails indicating they are still being processed.

## Security
*   Developer Mode data is never included in standard backups.
*   Cannot be activated in production "Release" builds without a special debug signature.
