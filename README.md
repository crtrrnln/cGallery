# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries.

## Features (v0.7pre1)

### Startup & UX
- **Cinematic Startup Experience**: Added a session-based startup animation featuring a sequence of brand-aligned descriptors (crafted, custom, curated...) ending with a smooth wipe-up reveal of the main gallery.
- **Robust Navigation**: Fixed navigation glitches where the backstack would desync from UI panes. System and in-app back buttons now properly pop the navigation state.
- **System Integration**: Now appears in the "Open with" menu for images/videos and functions as a standard media picker for other applications.

### Samsung-Inspired UI
- **Modern Aesthetics**: Updated all covers with 24dp rounded corners and refined grid spacing (12dp/16dp gaps) for a premium look and feel.
- **Tightened Media Grid**: Reduced photo spacing to 2dp in gallery and album views for a dense, high-performance visual experience.
- **Clean Management**: Administrative tools are tucked into a 3-dot overflow menu, keeping the interface focused and clutter-free.

### Advanced Organisation
- **Smarter Group Covers**: Group collage covers now react to item reordering and correctly pull representative images from nested album groups, respecting custom crops and positions.
- **Mixed Content Sorting**: Albums and Album Groups now intermingle based on sort order and name, rather than groups being pinned to the top.
- **Robust Creation**: Fixed empty album creation; new albums persist and show up immediately even before media is added.

### Enforced Inbox Engine (v0.7)
- **Automatic Organization Workflow**: Newly detected media enters a dedicated organizational workflow, ensuring your library remains organized as it grows.
- **Enforcement Sessions**: Integrates with **Shizuku** to automatically guide you to the Inbox when new media is detected.
- **Persistent Operation Queue**: File operations are now managed by a background queue that survives app restarts and ensures perfect data integrity.
- **Snooze System**: Respectfully postpone organization tasks with timed or count-based snoozing.
- **Local-First Verification**: Every file move or copy is verified for size and existence before being marked as complete.

## Roadmap
...
- ✅ **v0.7: Inbox Enforcement** - Automating the organization lifecycle through background scanning and forcing the user to organise newly discovered media.

---
*Developed with a focus on speed, stability, and simplicity.*
