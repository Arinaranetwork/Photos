---
Model    : Gemini 3.8 Flash (High)
Thinking : High
---

# Codex - Photos

## What Is This
Photos is a high-performance, privacy-first Android photo gallery and media management application built with Jetpack Compose. It organizes personal photos and video media into albums, collections, and date-based galleries using an offline-first architecture, fluid spring physics, and the Arinara Electric Slate dark theme design system.

## Who Is It For
Android smartphone users who prioritize device privacy, offline media management, instant local search, and a modern, cohesive gallery interface without cloud tracking, intrusive ads, or forced account logins.

## Business Model
Not determined - to be defined before public release.

## Pricing
Not determined - to be defined before public release.

## How to Use
1. Launch Photos to enter the main gallery timeline organized by date and album groupings on the dark canvas.
2. Tap into any photo or album card to view media with smooth spring zoom and pan transitions.
3. Use the bottom navigation floating dock to seamlessly switch between the Gallery Timeline, Albums view, and Application Settings.
4. Organize photos into albums, tag favorite captures, and inspect local image metadata (EXIF details, dimensions, capture date) directly on-device.

## Constraints & Assumptions
- **Platform**: Android (minSdk 24 Android 7.0 Nougat, compileSdk 36, targetSdk 36).
- **Offline First**: All storage, media indexing, thumbnail caching, and metadata extraction operate completely locally without external servers or cloud dependencies.
- **Hardware & Permissions**: Requires Android media storage permissions (`READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` on older SDKs) and device display capabilities.
- **Out of Scope for Initial Version**: Cloud backup sync, social media sharing feeds, collaborative cloud albums.
