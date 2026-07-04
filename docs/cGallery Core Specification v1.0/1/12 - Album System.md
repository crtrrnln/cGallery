# 12 - Album System

## Purpose
The Album System manages the organizational hierarchy of cGallery. It bridges the gap between physical MediaStore buckets and logical "Album Groups."

## Logical Structure

### 1. Album Groups (`AlbumGroupEntity`)
*   **Definition:** A logical container that can hold multiple `PhysicalAlbums` or other `AlbumGroups`.
*   **Hierarchical:** Supports nesting via `parentId`.
*   **Customization:** Supports 24dp rounded covers and custom 1:1 cropping data.

### 2. Physical Albums (`PhysicalAlbumEntity`)
*   **Definition:** A direct mapping to a physical folder (Bucket) on the device storage.
*   **Discovery:** Automatically synced via `PhysicalAlbumManager.syncAlbums`.
*   **Logic:** Can exist as a "root" item or be assigned to a `groupId`.

## Mixed Content Sorting
A core feature of cGallery v0.69+ is that **Groups** and **Albums** intermingle.
*   **Implementation:** The UI layer queries both tables, combines the results into a unified list, and sorts them by `sortOrder` then `name`.
*   **UI Grid:** Groups and Albums are indistinguishable at the top level except for the "Group" badge or collage cover.

## Cover Management
*   **Custom Covers:** Users can pick any image as a cover for a group or album.
*   **Persistent Cropping:** When a cover is selected, the user defines a 1:1 crop. This is stored as a string (`left,top,right,bottom`) in the database.
*   **Collage Logic:** Group covers automatically pull representative images from their children if no custom cover is set.

## Organizational Tools
*   **Move to Group:** A long-press action that allows reassigning an album to a different parent group.
*   **Administrative Overflow:** Creation, renaming, and hidden toggles are tucked into the 3-dot menu to maintain the Samsung-inspired aesthetic.
*   **Empty Album Persistence:** Unlike many galleries, cGallery allows and persists empty albums before media is added to them.
