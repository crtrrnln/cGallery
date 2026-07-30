# Implementation Plan - Refresh Fixes and Shizuku Reliability

This plan addresses the persistent issues with album refreshing, the non-functional refresh button, and Shizuku enforcement failures.

## User Review Required

> [!IMPORTANT]
> The version will be updated to `v0.9/1.0rc2.1`.
> I will improve the library refresh logic to be more thorough and ensure UI updates are triggered upon completion.

## Proposed Changes

### Refresh System Improvements
#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/SettingsViewModel.kt)
- Wrap `refreshLibrary()` logic in `try-finally` to ensure `isScanning` is always reset.
- Trigger `RefreshEventBus.requestRefresh()` after the media scan is initiated.
- Add a listener to the `MediaScannerConnection` to trigger another refresh once the scan batch completes (or at least after a reasonable delay).
- Optimize the file walk logic.

#### [MODIFY] [MediaStoreViewModel.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/MediaStoreViewModel.kt)
- Ensure `loadMedia()` is robust and triggers `syncAlbums` even if the initial MediaStore query seems empty (to allow cleanup).

### Shizuku Reliability
#### [MODIFY] [ShizukuManager.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/data/ShizukuManager.kt)
- Switch from `sh -c` to passing arguments directly to `newProcess` to avoid shell escaping issues.
- Refine the `am start` command.
- Add additional checks for Shizuku availability before attempting launch.

### Versioning & UI
#### [MODIFY] [InboxScreen.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/InboxScreen.kt)
- Observe `enforcementSettings` from `InboxViewModel`.
- Show "Cancel Snooze" option in the top bar menu if a snooze is currently active, even in non-enforcement sessions.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/build.gradle.kts)
- Update `versionName` to `"v0.9/1.0rc2.1"`.

#### [MODIFY] [Navigation.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/Navigation.kt), [HomeScreen.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/HomeScreen.kt), [StartupAnimation.kt](file:///C:/Users/Luke/Documents/GitHub/cGallery/app/src/main/java/com/example/cgallery/StartupAnimation.kt)
- Update version strings to `"v0.9/1.0rc2.1"`.

## Verification Plan

### Automated Tests
- Run `gradle assembleDebug` to verify compilation.

### Manual Verification
- **Refresh Button:** Go to Settings -> Refresh Library. Verify the icon changes to a loading state and the gallery/albums update after a few moments.
- **Inbox Enforcement:** Trigger an inbox event and verify the app opens via Shizuku.
- **Album Content:** Move a file and verify it appears in the target album immediately.
