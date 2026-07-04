# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries.

## Features (v0.6)

### Inbox System (New)
- **Inbox Foundation**: Dedicated workflow layer to track and organise newly detected media.
- **"Inbox, not Everything Box"**: When a folder is first monitored, cGallery automatically ignores its existing contents, ensuring you only process *new* media. You can manually reset this filter in settings if you wish to scan all history.
- **Monitored Folders**: Configure specific physical folders (e.g., Downloads, Twitter) to be scanned for new content.
- **Multi-Destination Logic**: Organise a single photo into multiple physical folders simultaneously (MOVE or COPY).
- **Manual Scan**: Trigger immediate detection of new media via the "Scan Now" feature.
- **Organisation Workflow**: Immersive UI to choose destinations and operations for pending media.

### Core Gallery
- **Optimised Image Grid**: Smooth 60 FPS scrolling with stable keys, reduced recomposition, and cached image requests.
- **Efficient Loading**: Intelligent thumbnail handling, ultra-light 180px thumbs for low-end devices, and background MediaStore processing.
- **Memory Efficient**: Optimised for devices with 4GB RAM, using string interning and background grouping to handle 60,000+ images.
- **Video & GIF Support**: Full support for videos and animated GIFs with visual badges and auto-pausing on swipe.
- **Full-Screen Viewer**: Immersive media viewing with swipe-to-navigate and Samsung-style swipe-down-to-exit functionality.
- **Interactive Actions**: Support for sharing, editing (external), and deleting images.

### Album System
- **Physical Album Management**: Add, move, and copy photos between physical albums with direct filesystem synchronisation.
- **Pre-grouped Albums**: Instant album loading using pre-processed bucket data.
- **Physical Folder Albums**: Albums based on filesystem folders.
- **Album Groups**: Hierarchical groups for organised collections.
- **Nested Groups**: Support for unlimited nesting.
- **Special Albums**: Quick access to "Recent" and "Favourites".

## Future Roadmap
- **v0.7 Enforcement**: Automatic background scanning and enforcement of organisational rules.
- **v0.8 Intelligence**: Smart suggestions for destination folders based on previous organisation patterns.

## Tech Stack
- **Kotlin**: Core language.
- **Jetpack Compose**: Declarative UI toolkit.
- **Navigation 3**: State-driven navigation architecture.
- **Room Database**: Local SQLite database for persistent organisation state.
- **MediaStore API**: Native Android media indexing.
- **Coil**: Efficient image loading with RGB_565 optimisation.

## Permissions Required
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`
- `MANAGE_EXTERNAL_STORAGE`: Required for physical file moving and organisational workflows.

---
*Developed with a focus on speed, stability, and simplicity.*
