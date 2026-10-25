# Phase 2 - UniversalMediaScannerAndSmartGallery

## Goal
Build the offline Universal Media Scanner, Smart Gallery Timeline, Album Explorer, 2-Up/4-Up Synchronized Photo Compare Mode, Floating Magnifier Loupe, Focus Peaking / Exposure Zebra Overlay, Animated GIF Frame Scrubber, and Sample Media Generator.

## Scope
- `MediaItem`, `MediaType` (`PHOTO`, `RAW_PHOTO`, `ANIMATED`, `VIDEO`, `AUDIO`), `AlbumItem`, `SortOrder`, and `MediaFilter` models.
- `MediaRepository` scanning `MediaStore.Files` + app-generated sample/studio media across all supported extensions (`jpg`, `jpeg`, `png`, `webp`, `heic`, `heif`, `avif`, `bmp`, `svg`, `dng`, `cr2`, `nef`, `arw`, `gif`, `mp4`, `mkv`, `webm`, `mov`, `avi`, `ts`, `3gp`, `mp3`, `flac`, `wav`, `m4a`, `aac`, `ogg`, `opus`).
- Built-in **Sample Studio Generator** (`SampleMediaGenerator`) that synthesizes real playable local WAV/MP4/PNG/GIF test files on-device so every audiophile, video, and photo feature works immediately even on a clean device or emulator.
- **2-Up / 4-Up Synchronized Photo Compare Screen** (`PhotoCompareScreen`) with locked zoom & pan coordinates across up to 4 selected images.
- **Photo Viewer Studio** with 400% Floating Magnifier Loupe, Sobel Focus Peaking (`#7A5AF8`), Exposure Zebra Stripes (`>=252` / `<=3`), and Animated GIF Frame Scrubber + Speed Control.

## Out Of Scope
- Cloud-synced albums.

## Features
### Universal Multi-Format Scanner (`F-G01`)
Queries local storage and parses format badges (`RAW`, `24-BIT FLAC`, `4K 60`, `MKV`, `GIF`, `MP3`), resolution, duration, bitrate, and sample rate.

### Synchronized 2-Up / 4-Up Photo Compare (`F-G02`)
Splits the canvas into 2 or 4 viewports sharing a single `scale` and `panOffset` state so pinching or dragging on any photo moves all compared photos in lockstep.

### Floating Magnifier Loupe & Focus Peaking / Zebras (`F-G03`, `F-G04`)
Provides an interactive `200%–400%` magnification lens and real-time pixel analysis overlays for sharpness edge peaking and highlight/shadow clipping.

### Animated GIF Frame Scrubber (`F-G05`)
Extracts individual GIF frames and allows frame-by-frame stepping (`-1f` / `+1f`), speed adjustment (`0.25x–4.0x`), and single-frame PNG export.

## UI Mockup
```
+--------------------------------------------------+
| Photos                     [Compare] [Search] [:]| (AppTopBar)
+--------------------------------------------------+
| [All] [Photos] [Videos] [Audio] [RAW] [GIFs]     | (AppPillChip)
|                                                  |
| +----------------------+ +---------------------+ |
| | [RAW • 24MP]         | | [FLAC • 24b/96kHz]  | | (AppCard)
| | Mountain_Dawn.dng    | | Midnight_Synth.flac | |
| +----------------------+ +---------------------+ |
+--------------------------------------------------+
|      ( [Home]   [Albums]   [Studio]  [Settings] )| (AppFloatingBar)
+--------------------------------------------------+
```

## Logic Notes
- All pointer inputs use `pointerInput(Unit)` with `rememberUpdatedState` (`AR-002`).
- Screens wrap content in `AppScreen` with `>= 100.dp` bottom clearance (`AR-006`, `AR-015`).

## Risks
- Risk: Large bitmap memory pressure in 4-Up Compare mode. Mitigation: Downsample comparison bitmaps to screen viewport dimensions via `BitmapFactory.Options.inSampleSize`.

## Dependencies
- Phase 1 presets, Coil (`coil-compose`, `coil-gif`, `coil-svg`, `coil-video`), `androidx.exifinterface`.

## Acceptance Criteria
- All image, video, and audio formats are indexed and filterable.
- 2-Up/4-Up Compare Mode, Loupe, Focus Peaking, and GIF Scrubber operate smoothly and pass `verifyAndroidRules`.
