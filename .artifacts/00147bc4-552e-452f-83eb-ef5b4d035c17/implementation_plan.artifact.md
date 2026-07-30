# Implementation Plan - Fix Folder Creation and Selection Organization

This plan addresses two issues:
1. **Folder Creation Failure**: Empty folders were being immediately deleted from the database by the sync logic before they could be used.
2. **Messy Selection Screen**: The album selection screen (used for moving/copying files) displayed all albums in a flat, unorganized list instead of following the established group hierarchy.

## User Review Required

> [!NOTE]
> I will be changing the sync logic to allow empty directories to exist as albums in the database. This is necessary to support creating new, empty albums for future use.

## Proposed Changes

### [Data Management]

#### [MODIFY] [PhysicalAlbumManager.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/data/PhysicalAlbumManager.kt)
- **Fix sync logic**: Update `syncAlbums` to skip deletion of folders that physically exist on disk, even if they are empty and not yet indexed by MediaStore.

### [UI & Navigation]

#### [MODIFY] [AlbumsScreen.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/AlbumsScreen.kt)
- **Fix organization in selection mode**: Update the `displayItems` logic to always filter albums based on their group assignment, even when in `selectionMode`. This ensures the root selection screen only shows root groups and ungrouped albums.
- **Improve sorting**: Ensure consistent sorting between normal and selection modes.

#### [MODIFY] [GroupDetailScreen.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/GroupDetailScreen.kt)
- **Consistency**: Ensure the album/group list in a group detail view also respects the `selectionMode` organization if needed (though it seems mostly correct already).

## Verification Plan

### Manual Verification
1. **Folder Creation**:
    - Go to the Albums screen.
    - Click "Create Album" (or the "+" icon in move mode).
    - Enter a name and confirm.
    - **Expected Result**: The new empty album should appear immediately and persist even after a library refresh.
2. **Move Operation Organization**:
    - Select a few images in the Gallery.
    - Click the "Move" icon.
    - **Expected Result**: The selection screen should look identical to the main Albums screen (organized by groups), not a flat list of every folder on the device.
    - Navigate into a group and verify you can select an album inside it.
