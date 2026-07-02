# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organizational features for large media libraries.

## Features (v0.52)

### Core Gallery
- **Image Grid Gallery**: Fast, smooth scrolling grid of all local images with lazy loading.
- **Video & GIF Support**: Full support for videos and animated GIFs with visual badges to identify media types.
- **Full-Screen Viewer**: Immersive media viewing with swipe-to-navigate functionality.
- **Interactive Actions**: Support for sharing, editing (external), and deleting images via modern MediaStore APIs.

### Album System
- **Physical Folder Albums**: Albums are based on actual filesystem folders for true organization.
- **Album Groups**: Create hierarchical groups to organize albums into collections.
- **Nested Groups**: Support for unlimited nesting of groups within groups.
- **Special Albums**: Quick access to "Recent" (all media) and "Favourites" from the albums screen.
- **Album Reordering**: Drag-free reordering of albums using up/down buttons in reorder mode.
- **Hide/Show Albums**: Toggle visibility of albums without deleting them.
- **Album Management**: Create new folders and move files between folders directly in the app.

### Organization Tools
- **Favourites System**: Locally persistent favourites to quickly access your best shots.
- **Search Functionality**: In-memory search by filenames and album names.
- **Bottom Navigation**: Simple tab-based navigation for Gallery, Albums, Favourites, and Search.
- **Group Management**: Create, move, and delete album groups with confirmation dialogs.
- **Visual Group Hierarchy**: Nested groups displayed with clear visual indentation in selection dialogs.

### User Experience
- **Material 3 Design**: Modern, adaptive UI with support for different screen sizes.
- **Canadian/British Spelling**: Consistent use of "Favourites" and other British spellings throughout the app.
- **Performance Optimized**: Coil image loading with memory and disk caching for smooth scrolling.
- **GIF Playback**: Native GIF support with Coil's GifDecoder for smooth animations.

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

## Usage

### Creating Album Groups
1. Navigate to the Albums screen
2. Tap the folder icon to create a new group
3. Groups can be nested by long-pressing a group and selecting a parent

### Organizing Albums
1. Long-press an album to open the move-to-group dialog
2. Select a group (including nested groups) or choose "Root" to ungroup
3. Albums in groups are hidden from the main albums list
4. Use the reorder mode (folder icon) to rearrange album order

### Managing Files
1. Create new folders directly in the app
2. Move files between folders using the file operations
3. Hide/show albums without deleting them using the visibility toggle

---
*Developed with a focus on speed, stability, and simplicity.*
