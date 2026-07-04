# 31 - Navigation

## Purpose
Navigation in cGallery is designed to minimize taps and keep the user focused on high-value curation.

## Navigation Architecture: Bottom Bar + Deep Links

### 1. Primary Destinations (Bottom Bar)
*   **Gallery:** The main unified stream of `MANAGED` items.
*   **Inbox:** The triage queue for `UNPROCESSED` items and `PROPOSED` albums.
*   **Albums:** The collection of all logical and physical albums.
*   **Search:** Dynamic entry point for filtering by content, date, and location.

### 2. Secondary Destinations (Overlay/Sub-screens)
*   **Settings:** Accessible from the Profile icon on the main screens.
*   **Trash/Archive:** Hidden sub-menus within the Albums or Settings screen.
*   **Developer Dashboard:** Hidden/Debug-only performance view.

## Navigation Flow

### From Gallery:
*   **Tap Thumbnail** -> Media Viewer.
*   **Long Press** -> Multi-select mode.
*   **Scroll Up (Fast)** -> Date Scrubber appears.

### From Inbox:
*   **Tap Group Card** -> Inbox Detail (fullscreen review of the cluster).
*   **Accept Action** -> Return to Inbox (card vanishes).

### From Media Viewer:
*   **Back/Swipe Down** -> Return to previous screen.
*   **Swipe Left/Right** -> Navigate within the current collection.
*   **Tap "Album Name"** -> Jump to Album Detail.

## Search Navigation (The "Omni-Box")
Search isn't just a list; it's a navigational tool.
*   **Type "Last Year"** -> Navigation jumps to the Gallery at the correct scroll position.
*   **Type "Cats"** -> Navigation opens a Filtered View.
*   **Type "Paris"** -> Navigation suggests jumping to the "Paris Trip" album.

## State Management
*   **Navigation Component:** Use Jetpack Navigation (Compose).
*   **Args:** Pass `media_id` or `album_id` as primary keys.
*   **Deep Links:** Support for system-level intents (e.g., `cgallery://album/{id}`).

## Back Stack Rules
*   The Bottom Bar destinations are "Top Level." Switching between them clears the local backstack for that tab.
*   The "Back" button should always eventually lead to the "Gallery" tab before exiting the app.
