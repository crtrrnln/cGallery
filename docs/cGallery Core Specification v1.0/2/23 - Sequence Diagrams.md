# 23 - Sequence Diagrams

## 1. Media Ingestion & Auto-Organization

This diagram shows the flow from discovering a new file to it being placed in an album.

```mermaid
sequenceDiagram
    participant OS as Android OS (MediaStore)
    participant ML as Media Layer
    participant DB as Database
    participant WE as Workflow Engine
    participant DE as Detection Engine
    participant AS as Album System
    participant EE as Enforcement Engine

    OS->>ML: New File Event
    ML->>DB: Create MediaItem (UNPROCESSED)
    DB-->>WE: ON_INSERT Event
    WE->>DE: Start Analysis(media_id)
    DE->>DE: Generate Embeddings & GPS Match
    DE->>DB: Save InferenceSignal
    DE->>AS: Check for Matching Album
    AS->>DB: Create/Update PROPOSED Album
    WE->>DB: Update State (INBOX)
    WE->>AS: Suggest Assignment
    Note over WE,AS: If Confidence > 0.9, Auto-Promote
    WE->>DB: Update State (MANAGED)
    WE->>EE: Request File Sync
    EE->>OS: Move File to Album Folder
```

## 2. User Triage (Inbox Action)

This diagram shows how the system responds to a user accepting a suggested grouping.

```mermaid
sequenceDiagram
    participant UI as Gallery UI
    participant WE as Workflow Engine
    participant DB as Database
    participant EE as Enforcement Engine
    participant OS as File System

    UI->>WE: User Accepts Suggestion(media_id, album_id)
    WE->>DB: Update State (MANAGED)
    WE->>DB: Update AlbumMembership
    WE->>EE: Schedule Sync(media_id)
    EE->>OS: Move file from /Inbox/ to /Albums/Paris/
    OS-->>EE: Success
    EE->>DB: Log Operation Success
    DB-->>UI: Update View (Reactive)
```

## 3. Duplicate Detection & Resolution

```mermaid
sequenceDiagram
    participant ML as Media Layer
    participant DB as Database
    participant DE as Detection Engine
    participant UI as UI Alert

    ML->>ML: Calculate SHA-256 Hash
    ML->>DB: Query hash
    DB-->>ML: Hash exists (media_id_existing)
    ML->>DB: Create FileInstance (pointing to existing media_id)
    DB-->>DE: ON_CONFLICT Trigger
    DE->>DE: Compare visual quality
    DE->>DB: Save DuplicateSignal
    DB-->>UI: Show "Review Duplicates" in Inbox
```

## Key Timing Constraints
*   **Ingestion (ML to DB):** < 100ms per file.
*   **Inference (DE):** 500ms - 2s (backgrounded).
*   **UI Update (DB to View):** < 16ms (60fps target).
*   **File Move (EE):** Variable (disk I/O).
