# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries. It focuses on speed, stability, and simplicity while providing a premium, refined user experience.

## 🚀 Key Features

### Navigation & UX
- **Three-Pane Adaptive UI**: Leverages Material 3 Adaptive layouts for a responsive experience across different screen sizes.
- **Robust Navigation**: Built with Jetpack Compose Navigation 3, ensuring a reliable backstack that stays in sync with UI panes.
- **Customizable Covers**: A completely rewritten cover adjustment tool featuring intelligent clamping and high-precision edge snapping.
- **Normalized Scaling**: Coordinate storage for custom covers is standardized, ensuring consistent looks across devices with different screen densities.

### 📂 Advanced Organisation
- **Custom Metadata Import**: Seamlessly import your album structure and metadata using cGallery's JSON export format (`BACKUP_ALBUM_DB.txt`).
- **Album Groups**: Organize albums into groups with support for nested hierarchies.
- **Intelligent Collages**: Group covers automatically generate collages from representative images within nested albums.
- **Seamless Sorting**: Albums and Album Groups intermingle naturally based on sort order and name.

### 📥 Inbox & Monitored Folders
- **Streamlined Media Intake**: Monitor specific device folders to automatically identify new media.
- **Inbox Workflow**: Review and process new items into organized albums with ease.
- **Automated Stats**: Track your processing progress with built-in inbox statistics.

### 🖼️ Core Gallery Experience
- **High-Performance Grid**: 60 FPS scrolling achieved through RGB_565 downsampled thumbnails, optimized for devices with limited RAM (4GB+).
- **Immersive Viewer**: Full-screen media viewing with intuitive swipe-down-to-exit and auto-pausing video support.
- **Modern Aesthetics**: Persona Red theme with 24dp rounded corners and refined spacing for a clean, premium feel.

## 🛠️ Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
- **Adaptive Layouts**: [Material 3 Adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive) & Navigation 3.
- **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) for local metadata and organization.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for high-performance image, GIF, and video thumbnail loading.
- **Media Playback**: [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) for smooth video playback.
- **Dependency Management**: Gradle Version Catalogs.
- **Serialization**: [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) for JSON parsing.

## 🏗️ Project Structure

The project follows a clean architecture approach within the `app` module:

- `com.example.cgallery`: Contains main UI screens and navigation logic.
    - `data`: Data layer including Room entities, DAOs, and Managers (`MediaStoreDataSource`, `PhysicalAlbumManager`, `InboxManager`).
    - `ui`: Reusable UI components and theme definitions.
- `docs`: Additional project documentation and performance reports.

## 🏁 Getting Started

### Prerequisites
- JDK 17
- Android Studio (Ladybug or newer recommended)
- Android SDK 34+

### Building the Project
Clone the repository and run the following command to build the debug APK:

```bash
./gradlew assembleDebug
```

### Importing Data
1. Use the cGallery export tool to generate a `BACKUP_ALBUM_DB.txt` file.
2. Place the file on your device's internal storage.
3. Open cGallery, navigate to the Albums screen, and use the import tool to select the file.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details (or contact the developer if not present).

---
*Developed with a focus on speed, stability, and simplicity.*
