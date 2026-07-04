# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries.

## Features (v0.65)

### UI Layout Fixes
- **Eliminated Ghost Padding**: Fixed a double-padding issue that caused excessive empty space at the top and bottom of most screens.
- **Unified Inset Handling**: Screens now correctly handle status and navigation bars independently of the main app container.

### Album Management
- **Universal Folder Creation**: Added "Create Folder" to the Album Group detail screen, allowing you to create new physical albums directly inside a group.
- **Fixed Creation Logic**: Refined the folder creation process to immediately trigger a MediaStore scan and provide visual feedback via snackbars.
- **Hierarchical Group Operations**: Enabled Move and Delete operations for album groups located inside other groups.

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
