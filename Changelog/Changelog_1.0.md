<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog - Photos 1.0
Release Date: 2026-10-25
Banner: /Assets/Banners/PhotosBanner_1.0_2026-10-25.png

## 1.0.0 - The All-in-One Power-User Media Suite Release

### Universal Media Scanner & Pro Gallery (Phases 1–2)
- **Electric Slate Design System**: Built on the unified Electric Slate palette (`#0D1117` canvas, `#161C26` panel, `#7A5AF8` Electric Violet accent) with `ElmsSans` typography, `SpringPhysics` tactile interactions, and `VerifyAndroidRulesTask` enforcement (`AR-001`–`AR-016`).
- **Wide Multi-Format Indexing**: Offline support across standard and RAW images (`JPEG`, `PNG`, `WebP`, `HEIC`, `AVIF`, `GIF`, `SVG`, `DNG`/`CR2`/`NEF`/`ARW`), video containers (`MP4`, `MKV`, `WebM`, `MOV`, `AVI`), and lossless/lossy audio (`FLAC`, `WAV`, `ALAC`, `AIFF`, `MP3`, `M4A`, `OGG`, `Opus`).
- **Format & Quality Badges**: Instant visual badges (`HI-RES 24B/96K`, `LOSSLESS WAV`, `RAW DNG`, `4K 60`, `GIF ANIM`, `320K HQ`) on every tile.
- **2-Up / 4-Up Synchronized Photo Compare & 400% Magnifier Loupe**: Compare burst shots side-by-side with locked pinch-zoom & pan coordinates, floating 400% pixel loupe lens, Sobel Focus Peaking (`#7A5AF8`), Exposure Zebra Stripes, and Animated GIF Frame Scrubber (`0.25x..2.0x`).

### Audiophile Music Player, 4-State Loop & 5 Multi-Queues (Phase 3)
- **Persistent Background Playback & Mini-Player Pill**: Foreground `MediaSessionService` with lock-screen controls and floating mini-player dock.
- **4-State Precision Loop Engine**: Cycle between `Loop Off`, `Loop Queue`, `Loop 1 Track`, and **Precision A-B Section Loop** with millisecond nudge (`-100ms`/`+100ms`) and automatic loop count exit (`∞`, `3x`, `5x`, `10x`).
- **Arrange What Plays Next & 5 Independent Multi-Queue Slots**: Reorderable Up Next queue (`Play Next`, `Add to Queue`, move up/down, `.M3U8` export) plus 5 independent queues (`Main Queue`, `Study / Lo-Fi`, `Workout`, `Podcasts & Lectures`, `Late Night`) that remember their active track and exact millisecond timestamp.
- **Lossless Signal Path Inspector & Output Switcher**: `ConnectedInspector` popup displaying `Source -> Decoder -> DSP -> Output Device` plus `Bit-Perfect Direct PCM`, `32-Bit Float Hi-Res DSP`, and `Voice Clarity Mono` modes.
- **Pitch/Tempo, Skip Silence, LRC Lyrics, CUE Splitter & Tag Editor**: Independent speed (`0.25x..4.0x`) & pitch (`-12..+12` semitones + `432Hz`), Smart Skip Silence with "Time Saved" counter, synced `.LRC` lyrics with tap-line-to-seek, `.CUE` virtual chapter splitting, timestamp bookmarks, and ID3/FLAC tag editing.

### 10-Band EQ, Parametric DSP & 60fps Spectrogram Studio (Phase 4)
- **10-Band Graphic Equalizer & Parametric EQ (PEQ)**: 10 bands (`31Hz`–`16kHz`, `-15dB..+15dB`) with Preamp headroom control, Parametric filter mode (`Hz`, `dB`, `Q`), and 1-tap Wavelet/AutoEq reference headphone target importer (`Harman Target`, `Sennheiser HD650`, `Sony WH-1000XM5`).
- **Bass Boost, 3D Virtualizer, +15dB Loudness Enhancer & Per-Device Auto-Presets**: Automatic preset switching across `Phone Speaker`, `Wired Headphones / USB DAC`, `Bluetooth Audio`, and `Line-Out / Cast`.
- **60fps Real-Time Spectrum & 24kHz Fake-Lossless Waterfall Analyzer**: 4 live visualizer modes (`32-Band FFT`, `Spline Wave`, `Oscilloscope`, `24kHz Lossless Check`) inside a Minimize-to-Pill `AppBottomSheet`.

### Comfy Netflix-Style Video Theater (Phase 5)
- **Continue Watching Shelf & Episodic Series Engine**: Automatic season/episode grouping (`S01E01`) with resume progress bars, remaining time labels, and **Auto-Play Next Episode** handoff.
- **Bedtime Touch Lock, Hold-for-2x Speed & +85s Skip Intro**: Single-tap screen lock for comfortable viewing in bed, long-press anywhere for instant `2.0x` speed, and floating `+85s Skip Intro` button with per-folder auto-skip rules.
- **Sub-Zero Extra-Dim (-80%) & 200% Volume Preamp Gestures**: Dual-zone vertical swipe HUD for night-safe dimming and software volume boost.
- **Dual Subtitles & Clickable Subtitle Transcript Search**: Simultaneous primary + secondary language subtitles (`EN` + `JP`), `+/-50ms` subtitle/audio sync offset, and searchable transcript drawer where tapping any dialogue line seeks directly to that scene.
- **Cinema Optics, PiP, Background Audio Handoff & 4x4 Storyboard Export**: 800% video zoom, `-1f`/`+1f` frame stepping, horizontal mirror flip (for dance/guitar practice), Picture-in-Picture, 1-tap background audio handoff, and `4x4` contact sheet PNG export.

### Non-Destructive Trimmer, Multi-Quality Audio Extractor, Deep EXIF & PIN Vault (Phases 6–7)
- **Lossless Fast-Cut Trimmer & Multi-Segment Splitter**: `Trim Outside`, `Cut Middle (Splice Ends)`, and `Multi-Segment Split` (`2..8` parts) without ever modifying the original source file.
- **1-Tap Video-to-Audio Extractor & Multi-Quality Converter**: Export audio in `Hi-Res Lossless (24-bit/96kHz)`, `CD Lossless (16-bit/44.1kHz)`, `320kbps`, `192kbps`, or `96kbps` with Fade-In/Out envelopes and Ringtone mode (`<=30s`).
- **Live 256-Bin RGB Histogram, 6-Swatch Palette, 1-Tap EXIF Privacy Scrubber & PIN Vault**: Real-time RGB + Luminance histogram, `SHA-256` file digest, 1-tap GPS/serial EXIF scrubber, batch image converter & token renamer (`{date}_{album}_{counter}`), salted `SHA-256` PIN Private Vault (with Decoy PIN), and 30-day Recycle Bin + Duplicate/Burst Finder.
