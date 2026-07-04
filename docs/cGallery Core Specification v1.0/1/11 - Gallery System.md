# 11 - Gallery System

## Purpose
The Gallery System is the presentation layer. Its primary goal is to provide a fluid, intelligent view of the media library, prioritized by the system's organizational logic.

## Design Philosophy: "Context over Chronology"
While traditional galleries are strictly chronological, cGallery's view is weighted by "Contextual Significance" and "Workflow State."

## Core Components

### 1. Unified Stream
*   The main view showing all `MANAGED` media.
*   **Intelligent Grouping:** Visually merges "Bursts" or "Near-Duplicates" into a single stack, with the "Best Shot" (determined by Detection Engine) on top.

### 2. Inbox View (The "Active Work" Zone)
*   A dedicated section for `UNPROCESSED` or `INBOX` state items.
*   **Behavior:** Items stay here until categorized or archived. High visibility to encourage user validation.

### 3. Album View
*   A grid of `Albums`, sorted by "Last Activity" or "Importance Score."
*   **Smart Covers:** Album covers are dynamically selected by the **Detection Engine** based on visual appeal and representativeness.

## State-Based Visibility Rules

| State | Visibility in Unified Stream | Visibility in Inbox | Visibility in Albums |
| :--- | :--- | :--- | :--- |
| `UNPROCESSED` | Hidden | Visible (Top) | Hidden |
| `INBOX` | Visible (Dimmed/Flagged) | Visible | Hidden |
| `MANAGED` | Visible (Normal) | Hidden | Visible |
| `ARCHIVED` | Hidden | Hidden | Hidden (Special Archive View Only) |
| `TRASHED` | Hidden | Hidden | Hidden |

## Interaction Patterns

### 1. The "Decision Swipe"
In the Inbox view, users can quickly:
*   **Swipe Right:** Accept system's proposed album grouping.
*   **Swipe Left:** Reject grouping (send back to general Inbox).
*   **Long Press:** Manually select target album.

### 2. Deep Zoom & Inspection
*   Support for high-resolution viewing.
*   **Metadata Overlay:** Show detected objects, faces, and location clusters.

## Implementation Details (Compose)
*   **Lazy Grids:** Use `LazyVerticalGrid` with adaptive cell sizes.
*   **Shared Element Transitions:** Smooth transitions from grid thumbnail to full-screen view.
*   **Pre-fetching:** Use the **Media Layer's** cache to pre-load thumbnails for off-screen items.

## Edge Cases

### 1. Empty States
*   **New User:** Show a "Scanning..." progress bar with educational tips on how the "Brain" works.
*   **No Results:** Provide suggestions for search or filters.

### 2. Fast Scrolling
*   Use a "Scroll-Bar Scrubber" that shows the Date/Location of the current view to help navigation.
*   **Placeholder UI:** Show shimmer/colors derived from average image color while thumbnails load.
