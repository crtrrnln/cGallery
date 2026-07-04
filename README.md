# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries.

## Features (v0.61)

### Inbox System
- **Inbox Foundation**: Dedicated workflow layer to track and organise newly detected media.
- **Multi-Destination Logic**: Organise a single photo into multiple physical folders simultaneously (MOVE or COPY).
- **"Inbox, not Everything Box"**: Automatically ignores existing folder contents when monitoring starts.
- **Manual Scan**: Trigger immediate detection of new media via the "Scan Now" feature.
- **Organisation Workflow**: Immersive full-screen UI to preview and multi-select destinations for pending media.

### Album Management
- **Interactive Multi-Selection**: Select multiple destination folders when moving or copying files.
- **Physical Sync**: Direct filesystem synchronisation with automatic MediaStore scanning.
- **Import/Export Structure**: Save and restore your entire album organisation (groups, nested groups, and sort orders) to a single JSON file.
- **Album Groups**: Hierarchical groups for organised collections with unlimited nesting.

### Core Gallery
- **Persona Red Theme**: Custom vibrant red branding with high-performance drawing optimisations.
- **Optimised Image Grid**: 60 FPS scrolling with RGB_565 downsampled thumbnails for 4GB RAM devices.
- **Full-Screen Viewer**: Immersive viewing with swipe-down-to-exit and auto-pausing video support.

## Tech Stack
- **Kotlin**: Core language.
- **Jetpack Compose**: Declarative UI toolkit.
- **Navigation 3**: State-driven navigation architecture.
- **Room Database**: Local SQLite database for persistent organisation state.
- **MediaStore API**: Native Android media indexing.
- **Coil**: Efficient image loading with memory-saving optimisations.

## Permissions Required
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`
- `MANAGE_EXTERNAL_STORAGE`: Required for physical file moving and organisational workflows.

---
*Developed with a focus on speed, stability, and simplicity.*
