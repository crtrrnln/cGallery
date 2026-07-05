# cGallery

cGallery is a simple local gallery app for organising and browsing image collections and folders, designed for people with lots of images and videos who download lots but can't keep up with the sorting of everything.

It’s still in development, but it already works for daily use and I’m improving it version by version.

---

## Current Version

v0.72

---

## What it does right now

- lets you browse images and folders quickly
- allows you to organise things into albums/groups
- handles basic folder structure automatically
- tries to stay fast even with bigger collections
- runs locally (no cloud or anything like that)
- allows you to export and import: your album group structure, organisation of albums/album groups, custom covers of albums/album groups, favourited files, and monitored folders.

---

## Inbox system (and enforcement)

when you download a file, cGallery can automatically open and drop you into an isolated inbox view if you allow it to with Shizuku

from there, new files are handled before they ever sit in albums

the idea is that downloads don’t just get added to the collection automatically, you have to place them into an album from the inbox

this only fully “takes over” the system when Shizuku permissions are enabled

without Shizuku, the inbox still exists but it just works inside the app and you can open it manually whenever you want

enforcement is also optional, so you can choose whether you want to be forced into the inbox when opening the app or just use it as a normal view

---

## What I’m working on next

### v0.7x
- fixing everything broken in v0.7 😭

### v0.8
- cleaning up how folder/group structure works
- fixing edge cases with empty or weird folders
- making loading more efficient
- general UI cleanup and small fixes

### v0.9
- smoother navigation
- better search (if I fully implement it)
- general usability improvements
- polishing things based on actual use

### v1.0
- everything should feel consistent and stable
- no weird edge-case bugs in core features
- UI and structure fully cleaned up
- basically the “finished” baseline version

---

---

## Future ideas (not sure yet)

- better search/filtering
- auto grouping..?
- tags or metadata stuff
- maybe plugins later on, not sure
- cloud sync is probably not happening anytime soon (or ever)

---

## Status

It works right now and I use it exclusively, but I’m still actively changing things so stuff might shift a bit between versions. (I intend on imports to work consistently though when moving up versions)

---

## End goal

Make something I can actually rely on long term without getting messy or overcomplicated or needing AI stuff.