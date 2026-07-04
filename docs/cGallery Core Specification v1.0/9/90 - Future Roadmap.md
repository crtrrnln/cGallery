# 90 - Future Roadmap

## Purpose
This document outlines the long-term vision for cGallery beyond the initial v1.0 release.

## Phase 2: Enhanced Intelligence (v1.1 - v1.5)
*   **Deep Scene Search:** Natural language search for photos (e.g., "Me at the beach with a dog").
*   **Automatic Collage & Highlights:** AI-generated memory movies and grid layouts.
*   **Video Understanding:** Detecting key moments within video files to generate dynamic previews.
*   **Face Recognition (Local):** Grouping people into a "People" section without cloud involvement.

## Phase 3: Ecosystem & Connectivity (v2.0)
*   **Desktop Companion:** A sister app for macOS/Windows that syncs the organization state.
*   **Private Cloud Sync:** End-to-end encrypted backup to personal S3 buckets or NAS (Nextcloud/Synology).
*   **Collaborative Albums:** Shared organization between family members while maintaining local privacy.

## Phase 4: Pro-Grade Tools (v3.0)
*   **RAW Support:** Full support for high-fidelity photo editing and metadata management.
*   **Automated Backup Enforcement:** Rules to ensure specific albums are always backed up to physical cold storage.
*   **AI-Assistant:** A chat-based interface to ask questions about the library (e.g., "When was the last time I saw a movie with Jane?").

## Ongoing Technical Goals
*   **KMP Migration:** Moving the core engines to Kotlin Multiplatform for use on non-Android platforms.
*   **On-Device LLM:** Integrating a small, local Large Language Model for better metadata generation and natural language processing.
*   **Zero-Battery Impact:** Moving more of the processing to dedicated AI chips (NPUs) on modern smartphones.

## Community & Open Source
*   **Plugin System:** Allowing third-party developers to write their own "Detection Modules" (e.g., a "Bird Species Identifier").
*   **Public API:** A local Intent-based API for other apps to query the cGallery brain.
