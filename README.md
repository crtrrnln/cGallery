# cGallery

cGallery is a local-first Android gallery application built for people
who keep large, highly organised media libraries.

Unlike traditional gallery apps, cGallery separates **viewing** from
**organisation**. It provides a fast gallery experience while
introducing an Inbox workflow that helps process newly downloaded media
before it becomes part of your permanent collection.

The application is designed to remain responsive even with libraries
containing tens of thousands of images and videos, while giving users
complete control over how their media is organised.

------------------------------------------------------------------------

## Features

### Gallery

-   Fast MediaStore-backed gallery with smooth scrolling for large
    libraries.
-   Full-screen image and video viewer.
-   Multi-select support.
-   Search across media and albums.
-   Favorites.
-   Adaptive layouts using Material 3.

### Album Organisation

-   User-defined albums independent of physical storage locations.
-   Album Groups with nested hierarchies.
-   Automatic collage generation for album groups.
-   Custom album cover positioning with persistent normalized
    coordinates.
-   Import and export of album metadata using cGallery's JSON backup
    format (`BACKUP_ALBUM_DB.txt`).

### Inbox Workflow

The Inbox is one of cGallery's core concepts.

Instead of leaving downloaded media scattered across folders, monitored
folders can be scanned for new files and routed into an Inbox where they
can be organised before becoming part of the main library.

Current capabilities include:

-   Monitored folders
-   Detection of newly added media
-   Manual processing workflow
-   Processing statistics
-   Support for assigning media to multiple destination folders by
    creating additional copies where required

This workflow is intended to make ongoing organisation practical instead
of requiring occasional large cleanup sessions.

### Performance

Performance has been a major focus throughout development.

Current optimisations include:

-   Efficient MediaStore queries
-   Thumbnail downsampling using RGB_565 where appropriate
-   Lazy Compose grids
-   Optimised image loading through Coil
-   Reduced unnecessary recomposition
-   Stable scrolling on lower-memory devices
-   Background processing for media operations

------------------------------------------------------------------------

## Technology

Component          Technology
  ------------------ -------------------------
Language           Kotlin
UI                 Jetpack Compose
Design System      Material 3
Adaptive Layouts   Material 3 Adaptive
Navigation         Navigation 3
Database           Room
Image Loading      Coil
Video Playback     Media3 ExoPlayer
Serialization      Kotlinx Serialization
Build System       Gradle Version Catalogs

------------------------------------------------------------------------

## Project Structure

``` text
app/
├── data/
│   ├── database/
│   ├── managers/
│   ├── datastore/
│   ├── models/
│   └── MediaStore integration
│
├── ui/
│   ├── screens/
│   ├── components/
│   ├── theme/
│   └── navigation/
│
└── util/

docs/
```

(The exact structure may evolve as the project grows.)

------------------------------------------------------------------------

## Building

### Requirements

-   JDK 17
-   Android Studio Ladybug or newer
-   Android SDK 34+

Clone the repository:

``` bash
git clone <repository>
```

Build:

``` bash
./gradlew assembleDebug
```

Run directly from Android Studio or install the generated APK.

------------------------------------------------------------------------

## Importing Album Data

cGallery can import previously exported organisational metadata.

1.  Generate a `BACKUP_ALBUM_DB.txt` export.
2.  Copy it to the device.
3.  Open the Albums screen.
4.  Choose **Import Metadata**.
5.  Select the backup file.

Only album organisation metadata is imported. Media files themselves are
not modified.

------------------------------------------------------------------------

## Project Goals

The long-term direction of cGallery is:

-   Fast local-first gallery
-   Advanced organisational tools
-   Flexible album management
-   Powerful Inbox workflow
-   Excellent performance on both flagship and lower-end devices
-   No cloud dependency required for core functionality

------------------------------------------------------------------------

## License

Licensed under the MIT License.

See the `LICENSE` file for details.
