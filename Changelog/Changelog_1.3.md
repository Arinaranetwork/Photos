<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog — Photos 1.3 (2026-10-25)

![Photos 1.3 Banner](../Assets/Banners/PhotosBanner_1.2_2026-10-25.svg)

## Added
- **Real-Time PCM Audio DSP Pipeline (`RealTimePcmDspAudioProcessor`)**: Sample-level Media3 `AudioProcessor` executing 10-band Parametric EQ (`Peaking`, `Low Shelf`, `High Shelf`, `Low-Pass`, `High-Pass`, `Band-Pass`, `Notch` with adjustable Frequency, Gain, and Q), Mid/Side (`M/S`) Stereo Width (`0–200%`), Binaural Crossfeed (`%` and Cutoff Hz), Left/Right Channel Balance, Mono Downmix, Phase Polarity Flip (`L`/`R`), Sub-Bass Exciter, Tube Warmth, and Soft-Knee Limiter in real time.
- **Connected Loop & Counted Repeat Modes (`Off`, `1x`, `2x`, `3x`, `5x`, `∞`, `ALL`, `A–B`)**: Choose whether playback stops and rewinds to `00:00` at the end of a track (`Off`), repeats an exact number of times (`1x`, `2x`, `3x`, `5x`) with a live remaining counter before rewinding to `00:00`, or loops continuously.
- **Background Audio Notification & Lockscreen Controls**: Live Android `MediaSession` and `MediaStyle` notification displaying the active track, artist, album art, loop mode, and transport controls (`Previous`, `Play/Pause`, `Next`, `Loop`, `Stop`).
- **Built-In Video Editor**: Cut/Trim (`Start`–`End`), Rotate (`90°` steps + fine angle), Mirror Horizontal, Flip Vertical, Scale (`0.5x–4.0x`), Move/Pan (`X`/`Y`), Brightness, Contrast, Saturation, Warmth, Cinema filter presets, Speed, and Audio Mute/Volume with live preview and `.mp4` export.
- **Video Frame Thumbnails**: Automatic memory- and disk-cached video frame thumbnails across the Gallery grid, Continue Watching cards, Album covers, and video timeline scrubbing.

## Fixed
- **Audio End-of-Track Rewind**: Fixed tracks reaching their final second remaining stuck at the end; reaching the end now immediately triggers the active Loop/Repeat directive or stops and resets to `00:00`.
- **Queue Scrolling Performance**: Replaced eager queue rendering with a virtualized `LazyColumn` isolated from playback position ticks to eliminate lag.
- **App-Wide Back Navigation**: Added hierarchical `BackHandler` support across all bottom sheets, full-screen editors/viewers, sub-tabs, and navigation tabs so pressing Back always returns to the previous view.
- **Video Mirroring**: Switched the video viewport to a hardware `TextureView` transform surface so horizontal mirroring, vertical flipping, rotation, scaling, and panning render accurately on all devices.

## Changed
- **Decluttered Functional UI**: Removed `"System Auto"` brightness chip and stripped verbose/promotional descriptions across audio, equalizer, and video controls.
