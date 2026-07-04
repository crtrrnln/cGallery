# 30 - UI Specification

## Design Language: "Samsung-Inspired Premium"
The UI focuses on density, fluidity, and refined aesthetics, targeted at high-performance Android devices.

## Layout Principles
*   **Corners:** Uniform 24dp rounded corners on all cards and covers.
*   **Grid Spacing:** 2dp gaps for the primary media grid; 12dp/16dp gaps for album/group collections.
*   **Persona Red Theme:** A custom vibrant red brand color used for accents, buttons, and progress indicators.

## Core Screens

### 1. Main Gallery
*   **Startup:** Cinematic session-based animation with brand descriptors ("Crafted", "Custom", "Curated").
*   **Grid:** High-density 4-column layout (default).
*   **Transitions:** Smooth wipe-up reveal upon completion of the startup sequence.

### 2. Inbox Workflow
*   **Navigation:** Dedicated back button to exit the triage state.
*   **Item Selection:** Clear visualization of the `Pending` queue.
*   **Processing Dialog:** Shows progress for multi-destination MOVE/COPY operations.

### 3. Album Grouping (Mixed View)
*   **Interaction:** Long-press to trigger administrative tools.
*   **Intermingled List:** Groups and albums sorted together, breaking the "Groups at Top" convention for a more organic feel.
*   **Badge System:** Subtle icons on covers indicating "Group" or "Hidden" status.

### 4. Media Viewer
*   **Immersion:** Edge-to-edge rendering.
*   **Interactions:** 
    *   Swipe-down-to-exit.
    *   Auto-pausing video logic when navigating away.
    *   3-dot menu for physical operations (Delete, Move, Copy).

### 5. Cover Picker & Cropper
*   **Persistent 1:1 Tool:** A custom UI for selecting and cropping covers.
*   **Real-time Preview:** Shows how the 24dp rounding will look in the final grid.

## Performance Requirements
*   **Frame Rate:** 60/120 FPS target for all transitions.
*   **Responsiveness:** Immediate UI updates for empty album creation and sort order changes.
*   **Administrative Access:** Quick-access 3-dot menus to minimize navigational depth for management tasks.
