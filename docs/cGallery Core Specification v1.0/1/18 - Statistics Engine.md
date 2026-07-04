# 18 - Statistics Engine

## Purpose
The Statistics Engine monitors the health of the media library and the performance of the system's engines. It provides data for the "Developer Mode" and internal optimization.

## Metrics Categories

### 1. Library Health
*   **Total Assets:** Count of `MediaItems` by state (`MANAGED`, `INBOX`, `ARCHIVED`).
*   **Storage Distribution:** Space used by MIME type (Photos vs. Videos).
*   **Inbox Age:** Average time a `MediaItem` spends in the `INBOX` state.
*   **Grouping Efficiency:** % of items that are part of an Album vs. Loose.

### 2. Engine Performance
*   **Ingestion Latency:** Time from file discovery to `INBOX` availability.
*   **Inference Accuracy:** Ratio of "Confirmed" vs. "Rejected" suggested groupings.
*   **Processing Backlog:** Number of items waiting in `UNPROCESSED` state.
*   **IO Throughput:** Speed of file moves and metadata writes.

### 3. Visual Analytics
*   **Color Distribution:** Predominant colors in the library (useful for "Search by Color").
*   **Temporal Heatmap:** When most media is captured (by hour/day/month).
*   **Spatial Density:** Geographical clusters of media.

## Data Persistence
Statistics are stored in a dedicated `StatisticsLog` table, aggregated hourly or daily to save space.

### Schema: `DailySummary`
| Field | Description |
| :--- | :--- |
| `date` | Primary Key. |
| `new_items_count` | Number of items ingested today. |
| `manual_edits_count` | Number of user corrections. |
| `total_storage_mb` | Snapshot of library size. |
| `avg_inference_confidence` | Mean confidence score from Detection Engine. |

## Event Triggers
The Statistics Engine subscribes to **Workflow Engine** events:
*   `ON_STATE_CHANGE` -> Update state counts.
*   `ON_ALBUM_CREATED` -> Update grouping stats.
*   `ON_ERROR` -> Increment error counters for specific engines.

## Usage in UI
*   **Settings/Storage:** Show breakdown of space used and potential savings from deleting duplicates.
*   **Dashboard:** High-level summary of "How organized is your life?"
*   **Developer Mode:** Real-time performance graphs for each engine.

## Pruning Policy
Raw statistics logs are kept for 90 days. Aggregated daily summaries are kept for 2 years.
