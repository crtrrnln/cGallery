# 32 - Settings

## Purpose
The Settings module allows users to customize the system's "Brain" and manage storage/privacy preferences.

## Settings Categories

### 1. General
*   **Theme:** Light, Dark, System Default, Dynamic Color.
*   **Language:** App-specific locale overrides.

### 2. The "Brain" (Inference Tuning)
*   **Auto-Organization Toggle:** Enable/Disable the Detection Engine.
*   **Confidence Threshold:** Slider (Relaxed -> Strict) for auto-promotion from Inbox to Managed.
*   **Burst Grouping:** Enable/Disable visual stacking of similar images.
*   **Face/Object Detection:** Toggle for content-based indexing (Privacy-focused).

### 3. Storage & Folders (Enforcement)
*   **Managed Root:** Select the primary directory for physical organization (e.g., `/Internal/Pictures/cGallery/`).
*   **Physical Sync:** Toggle for moving files on disk vs. logical-only albums.
*   **Original File Cleanup:** After moving a file to a managed folder, should the original be deleted? (Yes/No).
*   **Trash Expiry:** 7, 30 (default), or 90 days before permanent deletion.

### 4. Media Sources
*   **Monitored Folders:** List of directories the **Media Layer** watches (e.g., Camera, WhatsApp, Downloads).
*   **Excluded Folders:** Blacklist for folders like `.thumbnails`, system caches, etc.

### 5. Performance
*   **Background Intensity:** Low (Battery Save) vs. High (Plugged-in only).
*   **Cache Management:** Clear thumbnails, rebuild index.

### 6. Privacy & Data
*   **Export Database:** Backup the logical organization metadata.
*   **Wipe Brain:** Delete all inference signals and reset the Detection Engine weights.

## UI Pattern
*   **Nested List:** Group related settings into sub-screens.
*   **Live Preview:** When changing grid density or theme, show a small preview snippet.
*   **Immediate Application:** Settings change the system behavior in real-time (via `PreferenceStore` or `DataStore` observation).

## Default Configurations (Out of the box)
*   Auto-Organization: **ON**
*   Confidence Threshold: **0.85**
*   Physical Sync: **OFF** (Safe default)
*   Trash Expiry: **30 Days**
