# cGallery Samsung Import Implementation

## Project Overview
This is an Android gallery application with Samsung Gallery import functionality. The project uses Kotlin, Jetpack Compose, Room database, and Media3 ExoPlayer for video playback.

## Current Implementation Status

### Completed Features
- ✅ Video playback fixes in ViewerScreen.kt (fetchImages → fetchMedia)
- ✅ Samsung Gallery JSON import from BACKUP_ALBUM_DB.txt
- ✅ Room database entities for Samsung albums and folders
- ✅ Database migration from version 1 to 2
- ✅ SamsungImportManager for import process management
- ✅ SamsungGalleryImporter for JSON parsing and album reconstruction
- ✅ Import UI in AlbumsScreen.kt with file picker
- ✅ Fallback cover logic (albums without default_cover_path use absPath)
- ✅ Basic folder management (create, move, rename, delete)
- ✅ Database support for folder hierarchy via parentId

### Pending Features
- ⏳ Display folder hierarchy (nested folders) in UI
- ⏳ UI controls for moving albums between folders
- ⏳ UI controls for moving folders into other folders
- ⏳ UI controls for renaming folders
- ⏳ UI controls for deleting folders
- ⏳ Drag-and-drop or selection interface for folder management

## Key Files and Their Purpose

### Data Layer
- `VirtualAlbum.kt` - Room database entities and DAOs
  - `SamsungAlbumEntity` - Album data from Samsung export
  - `SamsungFolderEntity` - Folder data with parentId for hierarchy
  - `SamsungAlbumDao` - Album CRUD operations
  - `SamsungFolderDao` - Folder CRUD operations including move/rename/delete
  - Database version 2 with migration

- `SamsungGalleryImporter.kt` - JSON parsing and album reconstruction
  - `SamsungAlbumRaw` - Data class matching Samsung JSON format
  - `parseJson()` - Deserializes JSON string to SamsungAlbumRaw list
  - `importAlbums()` - Converts raw data to GalleryRoot structure
  - `parseCover()` - Extracts cover information from cover_rect string
  - Fields made nullable: defaultCoverPath, coverRect, albumType, albumLevel, essentialAlbumOrder, absPath

- `SamsungImportManager.kt` - Import process management
  - `importFromJson()` - Main import function
  - `createFolder()` - Create new folders
  - `moveAlbum()` - Move albums between folders
  - `moveFolder()` - Move folders into other folders
  - `renameFolder()` - Rename folders
  - `deleteFolder()` - Delete folders
  - `clearImportedData()` - Clear all imported data

### UI Layer
- `AlbumsScreen.kt` - Main albums display
  - Samsung albums display with folder grouping
  - Import dialog with file picker
  - Create folder dialog
  - Albums display in horizontal rows within folders (Samsung Gallery style)
  - Orphan albums display in grid

- `ViewerScreen.kt` - Media viewer with video playback
  - Fixed fetchImages → fetchMedia for MediaItem support

### Build Configuration
- `app/build.gradle.kts` - Added kotlinx.serialization.json dependency
- `gradle/libs.versions.toml` - Added kotlinx-serialization-json library definition

## Database Schema

### Samsung Albums Table
```kotlin
@Entity(tableName = "samsung_albums")
data class SamsungAlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val path: String,
    val folderId: Long?,
    val coverPath: String,
    val coverCrop: String?,
    val sortPrimary: Long,
    val sortSecondary: Long
)
```

### Samsung Folders Table
```kotlin
@Entity(tableName = "samsung_folders")
data class SamsungFolderEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val parentId: Long?  // Supports folder hierarchy
)
```

## Samsung JSON Format
The BACKUP_ALBUM_DB.txt file contains a JSON array with the following structure:
```json
{
  "__bucketID": 1900935501,
  "__Title": "Screenshots",
  "album_order": 4000000000,
  "__absPath": "/storage/DFF1-DCE8/Screenshots",
  "default_cover_path": "/storage/DFF1-DCE8/Screenshots/Screenshot_20220211-085228_Discord.jpg",
  "cover_rect": "0,0,0,0;2147564762;0;1;1;720;1280;1644580348;;0;331730;1644580348000;0;0",
  "__albumType": 0,
  "__albumLevel": 0,
  "essential_album_order": 0,
  "folder_id": 293426901,
  "folder_name": "Group 6"
}
```

## Important Notes

### Folder Hierarchy Approach
The Samsung JSON does NOT contain explicit parent-child relationships for folders. The hierarchy is inferred from group relationships, which is complex and not deterministic. Instead, we've implemented:
- Manual folder management via UI
- Database support for parentId in SamsungFolderEntity
- Ability to create, move, rename, and delete folders manually
- Albums can be moved between folders

### Build Requirements
- JDK 17 required
- JAVA_HOME environment variable must be set
- Build command: `./gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` (Linux)

### Dependencies Added
- kotlinx.serialization-core
- kotlinx.serialization-json

### Known Issues
- Folder hierarchy is not automatically detected from JSON (too complex/non-deterministic)
- Manual folder management UI is basic (create only, no move/rename/delete UI yet)
- Albums in folders display horizontally (not full grid due to nested grid issues)

## Next Steps for Arch Linux

1. **Display folder hierarchy in UI**
   - Use SamsungFolderDao.getRootFolders() and getChildFolders()
   - Implement recursive folder display in AlbumsScreen.kt
   - Consider using expandable/collapsible folder items

2. **Add folder management UI**
   - Long-press on folders to show context menu
   - Options: Rename, Delete, Move to parent, Move to another folder
   - Similar for albums: Move to folder

3. **Test the import**
   - Import BACKUP_ALBUM_DB.txt
   - Verify albums display correctly
   - Test folder creation and management

4. **Improve UI layout**
   - Consider using LazyVerticalGrid within folders instead of horizontal rows
   - Add folder icons
   - Improve visual hierarchy

## Environment Setup (Arch Linux)
```bash
# Ensure JDK 17 is installed
sudo pacman -S jdk17-openjdk

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Build the project
cd /path/to/cGallery
./gradlew assembleDebug
```

## Testing
After building, install the APK and test:
1. Import Samsung JSON file
2. Verify albums display with covers
3. Create a new folder
4. Test folder management operations
