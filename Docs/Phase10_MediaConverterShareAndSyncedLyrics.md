<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 10 - MediaConverterShareAndSyncedLyrics

## Goal
Deliver a zero-bloat multi-quality (`4K`/`1440p`/`1080p`/`720p`/`480p`) and multi-FPS (`Original`/`60fps`/`30fps`/`24fps`) video playback & on-demand hardware transcoding engine, a high-precision Video-to-MP3/M4A/WAV audio converter with a live `0%..100%` progress bar, universal Android `"Share to..."` integration across all media types, and an auto-scrolling synchronized `.LRC` lyrics editor with smooth sleep timer volume fade-out.

## Scope
- `VideoAudioConverterEngine.kt`: Hardware-accelerated `MediaExtractor` + `MediaCodec` + `MediaMuxer` video transcoder (resolution, FPS, bitrate tier, `H.264`/`HEVC`) and audio extractor/encoder (`MP3 320/256/192/128 kbps`, `M4A AAC`, `WAV Lossless PCM`) with real-time progress reporting (`StateFlow<ConversionProgressState>`) and pre-flight file size estimation.
- `MediaShareHelper.kt` & `file_paths.xml`: Native Android `FileProvider` and `Intent.createChooser(Intent.ACTION_SEND)` sharing for photos, videos, audio tracks, and converted outputs.
- `VideoTheaterSheet.kt`: New `Convert & Quality` tab providing zero-storage live playback resolution/FPS caps, Video-to-Audio conversion, Multi-Quality/Multi-FPS Video compression, determinate progress bar, and 1-tap `Share...` actions.
- `AudiophilePlayerSheet.kt`, `MultiQueueManager.kt`, & `HomeScreen.kt`: Auto-scrolling synchronized `.LRC` lyrics (`LazyColumn`), interactive `.LRC` timestamp stamper & editor, track sharing, and smooth 30-second sleep timer fade-out.

## Out Of Scope
- Cloud-based server transcoding (all conversion runs 100% locally on-device).
- Pre-generating multiple resolution copies in the background without user action.

## Features
### 1. Zero-Storage Live Playback Quality/FPS + On-Demand Hardware Video Transcoder
- In-player quality (`Original`, `1440p`, `1080p`, `720p`, `480p`) and frame rate (`Original`, `60fps`, `30fps`, `24fps`) controls adjust `ExoPlayer` `TrackSelectionParameters` and viewport rendering without creating extra files on disk.
- On-demand hardware video transcoder (`MediaExtractor` + `MediaCodec` + `MediaMuxer`) scales resolution (preserving aspect ratio and never upscaling above source), paces frame timestamps to target FPS (`60`/`30`/`24`), encodes with `H.264` or `HEVC` at `Master`, `Balanced`, or `Compact` bitrates, and reports `0%..100%` determinate progress.

### 2. Video-to-MP3 / M4A / WAV Audio Converter with Progress Bar
- Extracts and encodes audio from any video into `MP3 (320/256/192/128 kbps)`, `M4A (AAC-LC)`, or `WAV (16-bit Lossless PCM)` with live `0%..100%` progress, saving to `MediaStore.Audio` (`Music/Photos`) or temporary share cache.

### 3. Universal "Share to..." System Chooser
- 1-tap `Share...` in Photo Viewer, Photo Editor, Video Player, Music Player, and Converter output cards using `FileProvider` and `MediaStore` URIs.

### 4. Auto-Scrolling Synced `.LRC` Lyrics Editor & Smooth Sleep Fade-Out
- Auto-centers active lyric line during playback; interactive `.LRC` editor lets users paste lyrics, tap **"Stamp Line @ Current Time"** to tag `[mm:ss.xx]` in real time, and save `.lrc` files.
- Sleep timer smoothly fades volume from `100%` to `0%` over the final 30 seconds before pausing.

## UI Mockup
```
+-------------------------------------------------------------------+
| Convert & Quality / Share                                         |
| [Share Video...]   [Live Playback: Auto 4K | 1080p | 720p | 30fps]|
|-------------------------------------------------------------------|
| Convert Video to Audio (MP3 / M4A / WAV)                          |
| Format: [MP3 320k] [MP3 192k] [M4A 256k] [WAV Lossless]           |
| Est. Size: ~6.8 MB                                                |
| [Convert & Save to Music]   [Convert & Share Audio...]            |
|-------------------------------------------------------------------|
| Compress / Convert Video (Resolution + FPS + Quality)             |
| Res: [1080p] [720p] [480p]   FPS: [Original] [60] [30] [24]       |
| Quality: [Master] [Balanced] [Compact]   Codec: [H.264] [HEVC]    |
| Est. Output: ~24.5 MB (saves 78%)                                 |
| [Convert & Save Video]      [Convert & Share Video...]            |
|-------------------------------------------------------------------|
| Progress: [====================>        ] 68% (01:12 / 01:45)     |
+-------------------------------------------------------------------+
```

## Logic Notes
- Pre-flight file size estimation: `estimatedBytes = ((videoBitrateBps + audioBitrateBps) / 8) * durationSeconds`.
- Aspect-ratio-preserving resolution bounding ensures portrait (`9:16`) and landscape (`16:9`) videos keep their exact proportions and align dimensions to multiples of 16 for hardware `MediaCodec` compatibility.

## Risks
- Hardware `MediaCodec` encoder dimension alignment -> Align width and height to multiples of 16 (`(dim / 16) * 16`) and fall back gracefully if a device lacks HEVC hardware encoding.

## Dependencies
- Phase 3, Phase 5, Phase 9

## Acceptance Criteria
- Converting a video to MP3/M4A/WAV or a target resolution/FPS updates the progress bar from `0%` to `100%` and produces a real playable media file.
- Tapping `Share...` opens the Android system share sheet with read permission granted.
- Synced `.LRC` lyrics auto-scroll and the `.LRC` stamper saves valid `[mm:ss.xx]` files.
- `./gradlew verifyAndroidRules testDebugUnitTest assembleDebug assembleRelease` passes with 0 errors.
