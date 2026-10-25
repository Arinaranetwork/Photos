# Phase 5 - ComfyNetflixStyleTheaterVideoPlayer

## Goal
Build the Comfy Netflix-Style Theater Video Player featuring the "Continue Watching" Home Shelf, Episodic Folder Grouping, Auto-Play Next Episode (5s Countdown Card), `+85s` Skip Intro & Per-Folder Auto-Skip Rules, Bedtime Touch Lock, Hold-for-`2.0x` Speed, Extra-Dim Night Filter (`-80%`), `200%` Software Volume Boost, Ambient Cinema Glow, Chapter Markers, Dual Subtitles, Interactive Clickable Subtitle Transcript Search, 800% Pinch Zoom, Single-Frame Stepping, Mirror Flip, PiP, Video-to-Audio Background Handoff, Scene Bookmarks, and Video Storyboard Generator.

## Scope
- `ContinueWatchingRepository` tracking playback progress, remaining duration, and per-folder Intro/Outro auto-skip rules.
- `EpisodeSeriesAnalyzer` grouping numbered files in folders into an in-player Episodes drawer and powering the 5-second Auto-Play Next Episode countdown card.
- `SubtitleTranscriptEngine` parsing `.SRT`, `.VTT`, and `.ASS` files for simultaneous Dual Subtitles (Bottom Primary + Top Secondary) and the searchable, clickable Subtitle Transcript Sidebar.
- `VideoTheaterScreen` (`VideoTheaterSheet`) with Full-Screen Immersive mode (`fillMaxSize()`), native original aspect ratio & uncompressed resolution (`VideoAspectMode.FIT` / `RESIZE_MODE_FIT` default + `clearVideoSizeConstraints()`), 1-tap Fullscreen immersion toggle, Bedtime Touch Lock, Hold-for-`2.0x` speed, dual-zone vertical gestures (real hardware `WindowManager.LayoutParams.screenBrightness` + software dimming down to `-80%` Extra Dim, and `AudioManager` + `LoudnessEnhancer` volume boost up to `200%`), double-tap `-10s`/`+10s` ripple, 800% pinch zoom, `-1f`/`+1f` frame step, horizontal mirror flip, PiP, and 1-tap Background Audio handoff.
- `VideoStoryboardGenerator` exporting a `4x4` thumbnail contact sheet PNG with metadata and timestamps.

## Out Of Scope
- DRM-encrypted streaming service logins.

## Features
### Netflix Continuity & Episodic Intelligence (`F-V01`–`F-V04`)
Delivers the Continue Watching shelf, Season/Episode grouping, 5s Next Episode countdown, and `+85s` / per-folder auto-skip intro.

### Bedtime Ergonomics & Gesture HUD (`F-V05`–`F-V10`)
Provides 1-tap Bedtime Touch Lock, Hold-for-`2.0x`, Extra-Dim `-80%` night overlay, `200%` volume boost, ripple seek, and Ambilight edge glow.

### Dual Subtitles, Clickable Transcript & Pro Analysis (`F-V11`–`F-V20`)
Supports Dual Subtitles, searchable transcript jump-to-line, subtitle/audio sync offset (`+/-50ms`), 800% zoom, frame step, mirror flip, PiP, background audio handoff, scene bookmarks, and storyboard export.
- **System Brightness Preservation (`v1.1.2` `A-002`)**: Opening a video preserves the device's native system brightness (`WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE`) instead of forcing/locking brightness to max; custom window/software brightness only activates when the user explicitly drags the brightness gesture or slider, with a 1-tap `"System"` reset chip.
- **Minimized Picture-in-Picture Mode (`v1.1.2` `A-003`)**: Tapping `PiP` minimizes the video player into a compact, draggable Picture-in-Picture overlay (and supports Android system PiP with `RemoteAction` controls) where all complex drawers and HUDs are hidden and **only the core buttons are active: Go Left (`<<`), Play/Pause, and Go Right (`>>`)** (plus Expand and Close).

## UI Mockup
```
+--------------------------------------------------+
| < S01E01_Pilot.mp4       [Sub/Dual] [EQ] [Lock]  | (AppTopBar)
+--------------------------------------------------+
| [Secondary Sub: Top]                             |
|   |Bri|      ( Hold for 2.0x Speed )     |Vol|   |
|   |-40%|     << -10s          +10s >>   |150%|   |
| [Primary Sub: Bottom]     [Next Ep in 4s: Play]  |
+--------------------------------------------------+
| 14:20 =====|====(O)------|----------------- 24:00|
| [Bg Audio] [+85s Intro] [Episodes] [Transcript]  |
+--------------------------------------------------+
```

## Logic Notes
- Pointer inputs use `pointerInput(Unit)` (`AR-002`).
- All modals use `AppBottomSheet` or `ConnectedInspector` (`AR-009`).

## Risks
- Risk: Subtitle encoding variations. Mitigation: Detect UTF-8/UTF-16 BOM and parse `.srt`/`.vtt`/`.ass` timestamps resiliently.

## Dependencies
- Phase 3 & Phase 4 media and DSP engines.

## Acceptance Criteria
- All 20 video theater features work seamlessly offline and pass unit tests and `verifyAndroidRules`.
