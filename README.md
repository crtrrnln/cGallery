# cGallery (v0.31)

cGallery is a refined, high-performance Android media gallery built with the latest Material Design 3 standards. It provides a fluid, responsive experience for browsing and managing local images across various form factors.

## 🚀 Features

*   **Interactive Paging Viewer**: A full-screen image browser that supports horizontal swiping via `HorizontalPager`, allowing users to fluidly navigate through their entire collection.
*   **Adaptive Gallery Grid**: A responsive media grid powered by the **Compose Material Adaptive** library, ensuring a seamless experience across phones, foldables, and tablets.
*   **Contextual Multi-Select**: An intuitive selection mode for batch operations, enabling users to share or delete multiple images simultaneously via `ContentResolver`.
*   **Refined UI & Motion**: A polished Material 3 interface featuring a subtle versioning header, vibrant color schemes, and expressive motion.
*   **Media Actions**: Integrated support for sharing, editing, and secure deletion of images using modern MediaStore APIs (Android 11+ support).

## 🛠 Technical Stack

*   **Kotlin & Jetpack Compose**: Modern, declarative UI development.
*   **Jetpack Navigation 3**: State-driven navigation architecture.
*   **Compose Material Adaptive**: Responsive layouts for multiple window sizes.
*   **Material Design 3 (M3)**: Dynamic color systems and expressive components.
*   **Coil**: High-performance image loading optimized for grids and paging.
*   **Kotlin Coroutines**: Asynchronous MediaStore queries and file operations.

## ⚙️ Setup & Requirements

### Prerequisites
*   **Android Studio**: Ladybug (2024.2.1) or newer.
*   **Android SDK**: Minimum API level 29, targeting **API 37**.

### Installation
1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Ensure your IDE is configured to use Java 17 for Gradle.
4.  Sync Gradle and run the `:app:assembleDebug` task.

## 📂 Permissions

The app requires the following permissions to function:
*   `READ_MEDIA_IMAGES` (Android 13+)
*   `READ_EXTERNAL_STORAGE` (API 32 and below)

A built-in permission handling system (via Accompanist) ensures users are prompted gracefully.

---
*Developed with ❤️ as a cutting-edge Android Gallery Showcase.*
