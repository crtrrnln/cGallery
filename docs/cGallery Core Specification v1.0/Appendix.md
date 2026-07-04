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
| v0.63 | - | Custom covers and cropping persistence. | Developer |
| v0.69 | July 2026 | Inbox System, Multi-destination logic, Mixed sorting. | Developer |
| v1.0.0 | July 2026 | Full Core Specification (Redesign). | Architect |

## Reference Files (Internal)
*   `app/data/VirtualAlbum.kt` (Room Schema)
*   `app/data/InboxModels.kt` (Inbox Schema)
*   `app/data/PhysicalAlbumManager.kt` (Enforcement Logic)
*   `app/data/InboxManager.kt` (Workflow Logic)
