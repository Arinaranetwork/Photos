<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog - Photos 1.2
Release Date: 2026-10-25
Banner: /Assets/Banners/PhotosBanner_1.2_2026-10-25.png

## 1.2.0 - Video-to-MP3 Converter, Multi-Quality & Multi-FPS Video Support, Universal Share & Synced .LRC Lyrics

### Synchronized Auto-Scrolling `.LRC` Lyrics, Interactive Stamper & 30s Fade-Out Sleep Timer (`AudiophilePlayerSheet` & `MultiQueueManager`)
- **Smooth Auto-Scrolling `.LRC` Lyrics (`LazyColumn`)**: Replaced static lyric preview with a full `LazyColumn` synchronized lyric scroller that auto-centers the active timestamped lyric line via `animateScrollToItem` as the track plays, highlights active lines with Electric Blue accent cards, and supports tap-to-seek on any lyric line.
- **Interactive `.LRC` Lyric Editor & Real-Time Timestamp Stamper**: Added an in-app `.LRC` Lyric Editor & Stamper allowing users to paste or edit lyrics and tap `"Stamp Line [mm:ss.xx]"` in real time while listening, persisting `.lrc` sidecar files directly to storage.
- **Sleep Timer with Smooth 30-Second Volume Fade-Out**: Added live `mm:ss` countdown and automatic linear volume fade-out (`1.0f -> 0.0f`) over the final 30 seconds before pausing playback and restoring full volume.

### Video-to-MP3 / Audio Converter with Determinate Progress & Careful Audio Quality (`VideoAudioConverterEngine` & `VideoTheaterSheet`)
- **Multi-Format Audio Extraction & Encoding**: Convert any video into **`MP3 320 kbps (CBR Studio)`**, **`MP3 256 kbps (High VBR)`**, **`MP3 192 kbps (Standard)`**, **`MP3 128 kbps (Compact)`**, **`M4A (AAC 256 kbps Fast Remux)`**, or **`WAV (16-Bit Lossless PCM)`** while preserving source sample rate (`44.1 kHz` / `48 kHz`) and stereo channels.
- **Pre-Flight Output Size Estimation & Determinate Progress Bar**: Displays estimated output file size before conversion starts, updates a live `0%..100%` progress bar with elapsed/remaining media timestamps and cancel support, and exports directly into `MediaStore.Audio` (`Music/Photos`) and the in-app Music Player queue.

### Zero-Storage Live Playback Switcher & On-Demand Multi-Quality / Multi-FPS Video Transcoder (`VideoAudioConverterEngine` & `VideoTheaterSheet`)
- **Zero-Storage Live Playback Resolution & FPS Cap Switcher (`0 MB` Extra Disk Space)**: Switch playback resolution (`Original 4K`, `1440p QHD`, `1080p FHD`, `720p HD`, `480p SD`) and frame rate cap (`Original FPS`, `60 fps`, `30 fps`, `24 fps`) on the fly via `ExoPlayer` `TrackSelectionParameters` without pre-generating duplicate files.
- **On-Demand Hardware Multi-Resolution & Multi-FPS Video Converter**: Export a resized or frame-rate-converted copy only when requested (`4K 2160p`, `1440p`, `1080p`, `720p`, `480p` × `Original FPS`, `60 fps`, `30 fps`, `24 fps` × `Master`, `Balanced`, `Compact` bitrate profiles × `H.264 / AVC` or `H.265 / HEVC`) with pre-flight size estimation and live `0%..100%` progress bar.

### Universal Android `"Share to..."` Integration (`MediaShareHelper` & `file_paths.xml`)
- **1-Tap Native Android Share Sheet Everywhere**: Added `"Share to..."` across the Full-Screen Photo Viewer, Video Player header & transport bar, Music Player header, and Video/Audio Converter (`"Convert & Share..."` with optional temporary cache cleanup so temporary conversions take `0 MB` permanent storage).
