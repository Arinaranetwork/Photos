# Phase 2 - PowerUserMediaEngineEqAndTrimmer

## Goal
Transform Photos into a self-contained Power-User Universal Media Gallery, Player, Audio DSP Studio, and Non-Destructive Trimmer/Extractor that supports wide image, video, and audio formats locally without requiring external player, EQ, or editing applications.

## Scope
- **Universal Media Indexing & Multi-Format Browser**:
  - Unified local scanner (`MediaStore` + direct folder file walker) indexing Photos (`JPEG`, `PNG`, `WebP`, `HEIC`, `HEIF`, `AVIF`, `GIF`, `BMP`, `DNG`/`RAW`), Videos (`MP4`, `MKV`, `WebM`, `MOV`, `AVI`, `TS`, `3GP`), and Audio (`MP3`, `FLAC`, `WAV`, `M4A`, `AAC`, `OGG`, `Opus`).
  - Filterable timeline & folders by format type (`All`, `Photos`, `Videos`, `Audio`, `RAW/Animated`).
- **Pro Video & Audio Player (`Media3` / `ExoPlayer`)**:
  - Gesture-driven playback canvas (pinch-to-zoom up to 800% on live video and photos, vertical swipe for brightness/volume with overdrive boost, horizontal scrub with frame preview, double-tap seek).
  - Frame-by-frame stepping, A-B loop repeat, variable playback speed (`0.25x` to `4.0x` with pitch lock), multi-audio track & subtitle (`.SRT`, `.VTT`, `.ASS`) switcher, and Picture-in-Picture (PiP).
- **Background Playback & Video-to-Audio Handoff (`MediaSessionService`)**:
  - Foreground `MediaSessionService` allowing continuous background playback for both music (`MP3`/`FLAC`/`WAV`) and video audio streams (`MP4`/`MKV`) when the screen is off or app is backgrounded.
  - Persistent mini-player pill docked above `AppFloatingBar` with real-time progress, play/pause, and expand-to-player spring transition.
- **10-Band Equalizer & Audio FX DSP Engine (`DynamicsProcessing` / `AudioEffect`)**:
  - Hardware-accelerated 10-band graphic equalizer (`31Hz` to `16kHz`) attached to the active `audioSessionId` via `DynamicsProcessing` (API 28+) with fallback to 5-band `Equalizer`.
  - Preamp gain, Bass Boost, Virtualizer (spatial width), and Loudness Enhancer (+15dB boost for quiet recordings).
  - Built-in & user-saved EQ presets (`Flat`, `Bass Boost`, `Vocal / Lecture`, `Cinema`, `Late Night`, `Custom`) plus real-time 60fps FFT spectrum visualizer rendered in `PrimaryAccent` (`#7A5AF8`).
- **Built-In Media Cutter, Trimmer & Audio Extractor (`MediaMuxer` / `Media3 Transformer`)**:
  - Interactive timeline trimmer with dual range handles, video filmstrip thumbnails, and audio waveform preview.
  - Lossless Fast-Cut mode (`MediaExtractor` + `MediaMuxer` stream copy for near-instant, zero-re-encode MP4/MP3/M4A trimming) and Frame-Exact cut mode.
  - One-tap "Extract Audio" (`MP4`/`MKV` -> `.m4a` / `.aac`) and "Export Frame" (save exact video frame as lossless PNG/JPEG).
- **Deep Codec & EXIF/Histogram Inspector**:
  - Live RGB + Luminance histogram for photos and one-tap EXIF privacy scrubber (strip GPS/device serial).
  - Full stream diagnostics (container format, video/audio codec profile, bitrate, FPS, color space, audio sample rate, channel count).

## Out Of Scope
- Cloud transcoding or external server streaming.
- Multi-track non-linear video compositing (color LUT grading across multiple timelines).

## Features
### Universal Multi-Format Engine
Combines Coil 2 (with GIF, SVG, and video frame decoders) + Android `ImageDecoder` / `ExifInterface` for images and AndroidX `Media3 ExoPlayer` for video and audio containers, enabling seamless playback of formats stock galleries reject.

### Background Audio & Video-as-Audio Service
Uses `MediaSessionService` with system notification controls and lockscreen metadata. Toggling "Background Audio" on any video seamlessly detaches the surface renderer while keeping the audio pipeline and DSP equalizer active in the background.

### 10-Band EQ & DSP Studio
Encapsulated in an `AppBottomSheet` and `ConnectedInspector` using `AppSlider` controls and `AppPillChip` preset selectors. Applies real-time `DynamicsProcessing` bands directly to the Media3 audio session.

### Non-Destructive Trimmer & Audio Demuxer
Provides an in-app studio screen with start/end timestamp markers (millisecond precision), A-B preview loop, lossless packet demux/remux (`MediaExtractor` + `MediaMuxer`), and audio track extraction saved into `/storage/emulated/0/Movies/Photos` or `/Music/Photos`.

## UI Mockup
```
+------------------------------------------+
| <  track_or_video.mp4        [Info] [EQ] | (AppTopBar, <= 56dp)
+------------------------------------------+
|                                          |
|      [ Video / Photo / Waveform ]        | (Pinch zoom, A-B loop)
|      [   60fps FFT Spectrum     ]        |
|                                          |
+------------------------------------------+
| 01:14.20 =======[O]------------ 04:32.00 | (Frame-accurate seek)
| [Bg Audio] [Speed 1.0x] [Cut/Trim] [Sub] | (AppPillChip bar)
+------------------------------------------+
| ConnectedInspector / AppBottomSheet:     |
| 10-Band EQ: [Flat] [Bass] [Vocal] [Cust] |
| 31Hz  62Hz  125Hz 250Hz ... 8kHz  16kHz  |
| [|]   [|]    [|]   [|]       [|]   [|]   | (AppSlider bands)
| Bass Boost: [====O----]  Loudness: [+6dB]|
+------------------------------------------+
| MiniPlayer: [>] Lecture_04.mp4   [Pause] | (Floating mini-pill)
|      ( [Home]  [Albums]  [Settings] )    | (AppFloatingBar)
+------------------------------------------+
```

## Logic Notes
- All UI components strictly use `ElectricSlateTokens` and `ui.components.presets.*` (`AR-001` through `AR-016`).
- Pointer inputs for zoom, scrub, and trim handles use `pointerInput(Unit)` with `rememberUpdatedState` (`AR-002`).
- Trimming and audio extraction never overwrite the source file; outputs are written atomically to a new file and indexed via `MediaScannerConnection`.

## Risks
- Risk: `DynamicsProcessing` requires API 28+. Mitigation: Use `DynamicsProcessing` on API 28+ (10-band) and automatically fall back to standard `android.media.audiofx.Equalizer` (5-band) on API 24-27.
- Risk: Background service lifecycle restrictions on Android 14+ (API 34+). Mitigation: Declare `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission and foreground service type `mediaPlayback` in `AndroidManifest.xml`.

## Dependencies
- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui`
- `androidx.media3:media3-session`
- `androidx.media3:media3-transformer`
- `androidx.media3:media3-effect`
- `androidx.exifinterface:exifinterface`
- `io.coil-kt:coil-gif`, `io.coil-kt:coil-svg`, `io.coil-kt:coil-video`

## Acceptance Criteria
- User can browse and play photos, GIFs, MP4/MKV videos, and MP3/FLAC/WAV audio files inside Photos.
- Audio and video-audio continue playing in the background with system media notification controls when background mode is active.
- 10-band Equalizer, Bass Boost, and Loudness Enhancer actively modify playback audio and persist presets.
- User can trim MP4/MP3/M4A media and extract audio from video files non-destructively.
- `verifyAndroidRules` and unit tests pass with zero violations.
