# Implementation Plan - Video Support and Virtual Albums (v0.41)

This plan outlines the steps to add video playback, optimize gallery performance, and implement virtual albums similar to Samsung Gallery.

## Proposed Changes

### 1. Dependencies and Permissions
- Add Media3 (ExoPlayer, UI) for video playback.
- Add `coil-video` for video thumbnails.
- Update permissions to include `READ_MEDIA_VIDEO`.

#### [libs.versions.toml](file:///C:/Users/poo/Documents/GitHub/cGallery/gradle/libs.versions.toml)
- Add `media3` versions and libraries.
- Add `coil-video`.

#### [AndroidManifest.xml](file:///C:/Users/poo/Documents/GitHub/cGallery/app/src/main/AndroidManifest.xml)
- Add `android.permission.READ_MEDIA_VIDEO`.

### 2. Data Layer Refactoring
- Rename `LocalImage` to `MediaItem`.
- Add `MediaType` (IMAGE, VIDEO).
- Update `MediaStoreDataSource` to fetch both images and videos.
- Implement Room database for Virtual Albums.

#### [MediaItem.kt](file:///C:/Users/poo/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/data/MediaItem.kt) (RENAME from `LocalImage.kt`)
- `data class MediaItem(id, uri, name, bucketName, relativePath, type, duration)`

#### [MediaStoreDataSource.kt](file:///C:/Users/poo/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/data/MediaStoreDataSource.kt)
- Update `fetchMedia()` to query both images and videos.
- Use `ContentResolver.query` with `BUNDLE_QUERY_SORT_ORDER` if possible for better performance.

#### [NEW] [VirtualAlbumDatabase.kt](file:///C:/Users/poo/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/data/VirtualAlbumDatabase.kt)
- Define `AlbumEntity` and `MediaAlbumCrossRef`.
- Define `AlbumDao`.

### 3. Performance Optimization
- Implement a `MediaStoreViewModel` to cache media items and avoid re-fetching on every screen.
- Configure Coil `ImageLoader` with `VideoFrameDecoder`.
- Optimize `LazyVerticalGrid` with fixed thumbnail sizes and `key`s.

#### [NEW] [MediaStoreViewModel.kt](file:///C:/Users/poo/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/MediaStoreViewModel.kt)
- Expose `StateFlow<List<MediaItem>>`.
- Handle refreshing and initial load.

### 4. Video Playback and UI
- Update `ViewerScreen` to handle videos using `Media3 ExoPlayer`.
- Add video indicators in the gallery grid.

#### [ViewerScreen.kt](file:///C:/Users/poo/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/ViewerScreen.kt)
- Integrate `AndroidView` with `PlayerView`.
- Manage `ExoPlayer` lifecycle.

### 5. Virtual Albums UI
- Add ability to create albums.
- Add "Move to Album" action in selection mode.
- Update `AlbumsScreen` to show virtual albums.

---

## Verification Plan

### Automated Tests
- Run `./gradlew test` to ensure no regressions in existing logic.
- Add unit tests for `MediaStoreDataSource` mapping.

### Manual Verification
- **Video Playback**: Verify that videos appear in the gallery and can be played in full screen.
- **Performance**: Scroll through a large gallery to check for lag.
- **Virtual Albums**:
    - Create a new virtual album.
    - Select multiple items and "move" them to the new album.
    - Verify they appear in the virtual album but remain in their physical locations.
- **Permissions**: Verify that the app correctly asks for both image and video permissions on fresh install.
