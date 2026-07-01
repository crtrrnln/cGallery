# cGallery

cGallery is a fast, local-first Android gallery application designed with organizational features for large media libraries.

## Features (v0.4)
- **Image Grid Gallery**: Fast, smooth scrolling grid of all local images.
- **Full-Screen Viewer**: Immersive photo viewing with swipe-to-navigate functionality.
- **Interactive Actions**: Support for sharing, editing (external), and deleting images via modern MediaStore APIs.
- **Albums**: Automatic grouping of images by system buckets (e.g., Camera) and filesystem directories.
- **Favorites System**: Locally persistent favorites to quickly access your best shots.
- **Search Functionality**: In-memory search by filenames and album names.
- **Bottom Navigation**: Simple tab-based navigation for Gallery, Albums, Favorites, and Search.

## Tech Stack
- **Kotlin**: Core language.
- **Jetpack Compose**: Declarative UI toolkit.
- **Navigation 3**: State-driven navigation architecture.
- **Material 3 (M3)**: Modern design system with adaptive layout support.
- **MediaStore API**: Native Android media indexing and management.
- **Coil**: Efficient image loading.
- **DataStore**: Lightweight preference-based storage for favorites.
- **Kotlin Coroutines**: Asynchronous data fetching.

## Permissions Required
- `READ_MEDIA_IMAGES` (on Android 13+)
- `READ_EXTERNAL_STORAGE` (on older versions)

## Architecture Overview
cGallery uses a simple, flat architecture focused on direct MediaStore interactions.
- **MediaStoreDataSource**: Provides direct access to local media metadata.
- **FavoritesManager**: Handles persistence of favorite image IDs using DataStore.
- **Navigation 3**: Manages screen transitions through a centralized state-driven approach.
- No heavy abstraction layers or dependency injection frameworks are used to keep the codebase clean and maintainable.

## Future Roadmap
- **Enforced Inbox Workflow**: Planned features to streamline new media organization.
- **Advanced Organization Tools**: Automated sorting and batch tagging.
- **Optional Automation**: AI-assisted categorization and cleanup.

---
*Developed with a focus on speed, stability, and simplicity.*
