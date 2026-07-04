# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries.

## Features (v0.63)

### Customisation
- **Custom Album Covers**: Set any photo in an album as its cover.
- **Custom Crop**: Interactively adjust the zoom and position (crop) for your custom covers.
- **Custom Group Covers**: Change the cover of an album group, with a preview logic that respects custom album covers within the hierarchy.

### Album Management
- **Interactive Multi-Selection**: Select multiple destination folders when moving or copying files.
- **Physical Sync**: Direct filesystem synchronisation with automatic MediaStore scanning.
- **Import/Export Structure**: Save and restore your entire album organisation (groups, nested groups, and sort orders) to a single JSON file. Now includes **Custom Covers** and **Inbox Monitors**.
- **Album Groups**: Hierarchical groups for organised collections with unlimited nesting.
- **Delete Groups**: Remove groups with a single tap (albums inside are safely moved to the root).

### Inbox System
- **Inbox Foundation**: Dedicated workflow layer to track and organise newly detected media.
- **Multi-Destination Logic**: Organise a single photo into multiple physical folders simultaneously (MOVE or COPY).
- **"Inbox, not Everything Box"**: Automatically ignores existing folder contents when monitoring starts.
- **Manual Scan**: Trigger immediate detection of new media via the "Scan Now" feature.
- **Organisation Workflow**: Immersive full-screen UI to preview and multi-select destinations for pending media.

### Core Gallery
- **Persona Red Theme**: Custom vibrant red branding with high-performance drawing optimisations.
- **Optimised Image Grid**: 60 FPS scrolling with RGB_565 downsampled thumbnails for 4GB RAM devices.
- **Full-Screen Viewer**: Immersive viewing with swipe-down-to-exit and auto-pausing video support.

---
*Developed with a focus on speed, stability, and simplicity.*
