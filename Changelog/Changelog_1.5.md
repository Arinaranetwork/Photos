<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog — Photos 1.5 (2026-10-26)

![Photos 1.5 Banner](../Assets/Banners/PhotosBanner_1.5_2026-10-26.svg)

## Added
- **Storage & Media Management Suite (`MediaRepository`, `MediaInspectorAndVaultEngine`, `MediaInspectorAndVaultSheet`, `HomeScreen`)**:
  - **Universal Delete (`Move to Trash` & `Delete Permanently`)**: Delete items from the Gallery Multi-Select tray, Audio Multi-Select tray, Single Photo Viewer (`PhotoCompareAndLoupeSheet`), Trash, or Album Folders with a confirmation sheet offering both **Move to Trash** and **Delete Permanently** alongside **Empty Trash**.
  - **Delete Entire Folder / Album**: Delete any album folder directly from the `Albums` tab or the active album filter header in `Home`, displaying item count and total folder size and offering both **Move to Trash** and **Delete Permanently**.
  - **3-Tier Smart Duplicate Detector & 1-Tap Clean Duplicates**: Detects **Exact Duplicates** (file size + SHA-256 hash), **Duplicate Copies** (`(1)`, `_copy`, `_edit`, `burst`), and **Burst Sequences** (`<= 3s` camera bursts), automatically marking the highest-resolution/favorited file as `Keep` and offering 1-tap `Move to Trash` or `Delete Permanently` for all redundant copies.
  - **Single-Resolution Image Compression & `Replace Originals` Mode**: Compress selected or high-res photos down to a single target resolution (`4K`, `1440p`, `1080p`, `720p`, `480p`, `50% Scale`, or `Original`) without upscaling smaller images, with a **Replace Originals** mode that replaces oversized originals in-place to reclaim storage immediately.
  - **Free Up Space Dashboard**: Dedicated storage analyzer accessible from the `Tools` Bento grid and `Albums` header summarizing reclaimable space across **Trash**, **Duplicates**, **High-Res Photos (`Compress`)**, and **Large Files (`> 25 MB`)**.
- **Expanded Multi-Format Image, Audio & Video Conversion (`BatchImageStudioEngine`, `VideoAudioConverterEngine`, `SuperUtilityStudioSheet`)**:
  - **9 Image Output Formats**: `JPG`, `PNG`, `WEBP`, `WEBP Lossless`, `BMP`, `ICO`, `TIFF`, `GIF`, and `TGA`.
  - **13 Audio Output Presets Across 7 Formats**: `MP3` (`320k`, `256k`, `192k`, `128k`), `FLAC` (`16-bit`, `24-bit`), `M4A` (`320k`, `256k`), `AAC` (`256k ADTS`), `OGG Opus` (`192k`), `WAV` (`16-bit`, `24-bit`), and `AIFF`, plus a standalone **Convert Audio** tab in `SuperUtilityStudioSheet`.
  - **5 Video Output Options**: `MP4 (H.264)`, `MP4 (HEVC)`, `WEBM (VP9)`, `WEBM (VP8)`, and `3GP`.
- **Flagship Device Engine & 4th `Tools` Bento Tab (`FlagshipDeviceProfile`, `MainActivity`, `HomeScreen`)**:
  - **Adaptive Flagship Scaling**: Detects multi-core CPU (`8+` cores), RAM (`8GB`/`12GB+`), peak display refresh rate (`120Hz`/`144Hz`), wide color gamut, and hardware `4K 120 FPS` / `8K` codec capabilities.
  - **4th `Tools` Navigation Tab & Split Preview/Dock Workspace**: Consolidates Free Up Space, Compress, PDF, Collage, Convert Images, Convert Video, Convert Audio, Combine Audio, GIF, Scan, QR Code, and Audio to Video into a 2-column Bento tab with a pinned top live preview and compact bottom tool dock across editors.

## Fixed
- **Zero-Stretch Orthographic Video Rotation (`HardwareGlVideoRenderPipeline`, `VideoEditorSheet`)**: Fixed video aspect-ratio stretching when rotating videos (`90°`/`270°` or free-angle `-45°..+45°`) by applying a true pixel-space orthographic MVP matrix (`computeOrthographicVideoMvpMatrix`) that clips protruding edges cleanly and fills untouched canvas areas with pure `#000000` black.
- **Music Page `Arrange Order` Mode & Queue Sync (`HomeScreen`, `MultiQueueManager`)**: Fixed tapping a song row while in `Arrange Order` mode so it never triggers playback or opens the player sheet, added `#1, #2...` rank badges, `Move to Bottom`, and quick sort presets (`Reverse`, `Title A–Z`, `Artist`, `Duration`, `Newest`, `Reset`), and synchronized arrangement order live with `MultiQueueManager` and persistent storage.
- **GitHub Release In-App Update Popup (`AppUpdateChecker`, `HomeScreen`)**: Replaced the recursive regex in `AppUpdateChecker` that caused `StackOverflowError` on large GitHub release bodies with a linear `O(N)` JSON string scanner + non-rate-limited GitHub Atom/HTML fallbacks, and added automatic check triggers on app launch and `ON_RESUME`.
- **Synced Lyrics Progressive Bottom Bokeh & Lazy Rendering (`AudiophilePlayerSheet`)**: Restored progressive optical blur across the bottom of the lyrics viewport while keeping 60fps sweep isolated to the active line so lyrics never recompose all at once.
