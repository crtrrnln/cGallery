# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organizational features for large media libraries.

## Features (v0.54)

### Core Gallery
- **Optimized Image Grid**: Smooth 60 FPS scrolling with stable keys, reduced recomposition, and cached image requests.
- **Efficient Loading**: Intelligent thumbnail handling, ultra-light 180px thumbs for low-end devices, and background MediaStore processing.
- **Memory Efficient**: Optimized for devices with 4GB RAM, using string interning and background grouping to handle 60,000+ images.
- **Video & GIF Support**: Full support for videos and animated GIFs with visual badges and auto-pausing on swipe.
- **Full-Screen Viewer**: Immersive media viewing with swipe-to-navigate and Samsung-style swipe-down-to-exit functionality.
- **Interactive Actions**: Support for sharing, editing (external), and deleting images.

### Album System
- **Pre-grouped Albums**: Instant album loading using pre-processed bucket data.
- **Physical Folder Albums**: Albums based on filesystem folders.
- **Album Groups**: Hierarchical groups for organized collections.
- **Nested Groups**: Support for unlimited nesting.
- **Special Albums**: Quick access to "Recent" and "Favourites".
- **Album Reordering**: Unified reordering mode for groups and albums.
- **Hide/Show Albums**: Toggle visibility without deleting.
- **Album Management**: Create folders and move files directly in-app.

### Performance & Stability (v0.54)
- **Zero O(N) Grid Operations**: Eliminated all list searches during grid rendering for butter-smooth scrolling even in massive libraries.
- **Background Search & Grouping**: Search results and album groupings calculated on `Dispatchers.Default`.
- **String Interning**: Reduced memory footprint by interning repetitive bucket names and paths.
- **Image Request Caching**: `ImageRequest` objects are now cached in composition to prevent redundant allocations.
- **Efficient Navigation State**: Navigation callbacks are stabilized to prevent unnecessary whole-screen recompositions.

## Tech Stack
- **Kotlin**: Core language.
- **Jetpack Compose**: Declarative UI toolkit.
- **Navigation 3**: State-driven navigation architecture.
- **Material 3 (M3)**: Modern design system with adaptive layout support.
- **MediaStore API**: Native Android media indexing and management.
- **Coil**: Efficient image loading with GIF support.
- **Room Database**: Local SQLite database for album and group persistence.
- **DataStore**: Lightweight preference-based storage for favourites.
- **Kotlin Coroutines**: Asynchronous data fetching and database operations.

## Permissions Required
- `READ_MEDIA_IMAGES` (on Android 13+)
- `READ_MEDIA_VIDEO` (on Android 13+)
- `READ_EXTERNAL_STORAGE` (on older versions)
- `MANAGE_EXTERNAL_STORAGE` (for folder creation and file moving)

## Architecture Overview
cGallery uses a clean architecture focused on direct MediaStore interactions and local database persistence.
- **MediaStoreDataSource**: Provides direct access to local media metadata with support for videos and GIFs.
- **PhysicalAlbumManager**: Manages physical folder albums with database synchronization.
- **AlbumGroupManager**: Handles album group hierarchy and operations.
- **FavoritesManager**: Handles persistence of favourite image IDs using DataStore.
- **Navigation 3**: Manages screen transitions through a centralized state-driven approach.
- **Room Database**: Persistent storage for albums, groups, and their relationships.
- No heavy abstraction layers or dependency injection frameworks are used to keep the codebase clean and maintainable.

---
*Developed with a focus on speed, stability, and simplicity.*
