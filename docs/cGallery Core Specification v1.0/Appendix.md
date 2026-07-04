# Appendix

## Glossary of Terms

| Term | Definition |
| :--- | :--- |
| **Bucket** | A physical directory recognized by the Android MediaStore as a container for media. |
| **Managed Root** | The user-defined destination for organized physical albums. |
| **Album Group** | A logical hierarchy in cGallery that can contain albums or other groups. |
| **Monitored Folder** | A physical path (like Camera or Downloads) that the system scans for new media. |
| **Mixed Sorting** | The UI pattern where albums and groups are intermingled in the same list. |
| **Enforcement** | The act of physically moving/copying files to match the user's organizational intent. |

## Technology Stack

*   **Language:** Kotlin (100%)
*   **UI Framework:** Jetpack Compose
*   **Database:** Room Persistence Library
*   **Serialization:** kotlinx.serialization (JSON)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Media Access:** MediaStore API + java.io.File
*   **Architecture:** Manager-based with Decoupled Engines

## Revision History

| Version | Date | Description | Author |
| :--- | :--- | :--- | :--- |
| v0.1 | 1 July 2026 | Barebones skeleton structure. | crtrrnln |
| v0.2 | 1 July 2026 | Read-only gallery grid. | crtrrnln |
| v0.3 | 1 July 2026 | Initial image viewer. | crtrrnln |
| v0.31 | 1 July 2026 | Full-screen viewer with file management. | crtrrnln |
| v0.4 | 1 July 2026 | Initial album and favorites functionality. | crtrrnln |
| v0.5 | 2 July 2026 | Album revamp (scrapped Samsung importing). | crtrrnln |
| v0.51 | 2 July 2026 | Hiding albums and GIF support. | crtrrnln |
| v0.52 | 2 July 2026 | Album nesting and groups. | crtrrnln |
| v0.53 | 3 July 2026 | Optimization pass 1. | crtrrnln |
| v0.54 | 3 July 2026 | Optimization pass 2. | crtrrnln |
| v0.55 | 3 July 2026 | Fixed file moving/copying logic. | crtrrnln |
| v0.56 | 3 July 2026 | Fixed color theming apparently..? | crtrrnln |
| v0.6 | 3 July 2026 | Initial Inbox System. | crtrrnln |
| v0.61 | 3 July 2026 | Organization Export/Import. | crtrrnln |
| v0.62 | 3 July 2026 | Fixed multi-destination logic and group deletion. | crtrrnln |
| v0.63 | 4 July 2026 | Custom covers and organization backup. | crtrrnln |
| v0.64 | 4 July 2026 | Various fixes. | crtrrnln |
| v0.65 | 4 July 2026 | Various fixes. | crtrrnln |
| v0.66 | 4 July 2026 | Various fixes. | crtrrnln |
| v0.67 | 4 July 2026 | Fixes regarding album group covers. | crtrrnln |
| v0.68 | 4 July 2026 | Album cropping and navigation fixes. | crtrrnln |
| v0.69 | 4 July 2026 | Opening animation and media picker support. | crtrrnln |
| v0.7pre1 | 4 July 2026 | Design documentation (Core Spec v1.0). | crtrrnln |

## Reference Files (Internal)
*   `app/data/VirtualAlbum.kt` (Room Schema)
*   `app/data/InboxModels.kt` (Inbox Schema)
*   `app/data/PhysicalAlbumManager.kt` (Enforcement Logic)
*   `app/data/InboxManager.kt` (Workflow Logic)
