# 15 - Detection Engine

## Purpose
The Detection Engine provides the "Intelligence" in cGallery. It performs deep analysis of media content and metadata to generate `InferenceSignals` that drive organization.

## Analysis Modules

### 1. Similarity Module (Clustering)
*   **Goal:** Identify bursts, near-duplicates, and related sequences.
*   **Algorithm:**
    1.  **Perceptual Hashing (pHash):** Fast comparison of image structures.
    2.  **Temporal Proximity:** Images taken within seconds/minutes of each other.
    3.  **Visual Embeddings:** Use a lightweight MobileNet or similar model to generate feature vectors.
*   **Output:** `SimilarityCluster` (e.g., "These 5 photos are the same sunset").

### 2. Event Module (Spatio-Temporal)
*   **Goal:** Group media into cohesive "Events" (Trips, Parties, Outings).
*   **Algorithm:**
    1.  **DBSCAN (Density-Based Spatial Clustering of Applications with Noise):** Cluster GPS + Timestamp coordinates.
    2.  **Gap Analysis:** Detect significant breaks in photo-taking activity (e.g., > 4 hours).
*   **Output:** `EventSignal` (Name: "Weekend in Madrid", Range: "Friday-Sunday").

### 3. Subject & Quality Module (Scoring)
*   **Goal:** Identify "Best Shots" and categorize content.
*   **Scoring Criteria:**
    *   **Focus/Sharpness:** Is the subject in focus?
    *   **Composition:** Rule of thirds, exposure.
    *   **Content:** Faces detected, smile detection.
    *   **Uniqueness:** Is this a unique shot in a cluster?
*   **Output:** `QualityScore` (0.0 - 1.0) and `ContentTags` (e.g., #nature, #document).

## Signal Processing Pipeline

1.  **Metadata Extract:** Fast pass on EXIF.
2.  **Featurization:** Generate pHash and visual embeddings (deferred to idle/charging).
3.  **Signal Generation:** Emit `InferenceSignal`.
4.  **Feedback Loop:** If a user manually moves an item *away* from a suggested cluster, the engine adjusts the weights for future inferences for that user.

## Inference Signals

| Signal Type | Payload | Use Case |
| :--- | :--- | :--- |
| `CLUSTER_BURST` | `List<MediaID>`, `BestShotID` | Stack visual duplicates. |
| `SUGGEST_ALBUM` | `List<MediaID>`, `ProposedName` | Create new dynamic album. |
| `QUALITY_ALERT` | `MediaID`, `Score` | Hide blurry/bad shots from main grid. |
| `DOC_DETECTED` | `MediaID` | Move receipts/screenshots to "Documents" album. |

## Execution Strategy
*   **Incremental:** Analyze new items as they arrive.
*   **Full Scan:** Triggered on app update or user request.
*   **Thermal Management:** If CPU temperature exceeds threshold, pause intensive ML tasks.

## Edge Cases

### 1. No GPS Metadata
*   **Fallback:** Use visual similarity to link with items that *do* have GPS taken around the same time.
*   **Heuristic:** Assume "Same Day + Similar Visuals" = "Same Location".

### 2. Low Light / High Noise
*   Avoid over-penalizing "artistic" low light.
*   Weight temporal proximity higher than visual similarity for event grouping.
