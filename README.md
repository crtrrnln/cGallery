# cGallery

cGallery is a fast, local-first Android gallery application designed with advanced organisational features for large media libraries.

## Features (v0.68)

### Navigation & UX
- **Robust Backstack Management**: Fixed a critical navigation glitch where the backstack would desync from the UI panes. System and in-app back buttons now properly pop the navigation state.
- **Enhanced Cover Cropping**: Completely rewritten cover adjustment tool. Covers now feature intelligent clamping to prevent empty space and high-precision edge snapping for perfect alignment.
- **Normalized Scaling**: Coordinate storage has been standardized, ensuring custom covers look identical regardless of device screen density or resolution.

### Samsung-Inspired UI
- **Modern Aesthetics**: Updated all covers with 24dp rounded corners and refined grid spacing (12dp/16dp gaps) for a premium look and feel.
- **Tightened Media Grid**: Reduced photo spacing to 2dp in gallery and album views for a dense, high-performance visual experience.
- **Clean Management**: Administrative tools are tucked into a 3-dot overflow menu, keeping the interface focused and clutter-free.

### Advanced Organisation
- **Smarter Group Covers**: Group collage covers now react to item reordering and correctly pull representative images from nested album groups.
- **Mixed Content Sorting**: Albums and Album Groups now intermingle based on sort order and name, rather than groups being pinned to the top.
- **Robust Creation**: Fixed empty album creation; new albums persist and show up immediately even before media is added.

### Core Gallery
- **Persona Red Theme**: Custom vibrant red branding with high-performance drawing optimisations.
- **Optimised Image Grid**: 60 FPS scrolling with RGB_565 downsampled thumbnails for 4GB RAM devices.
- **Full-Screen Viewer**: Immersive viewing with swipe-down-to-exit and auto-pausing video support.

---
*Developed with a focus on speed, stability, and simplicity.*
