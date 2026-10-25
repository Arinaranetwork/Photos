---
Model    : Gemini 4 Argon (High)
Thinking : High
---

# Codex - Photos

## What Is This
Photos is a 100% offline, privacy-first Power-User Universal Media Gallery, Audiophile Music Player, Netflix-Style Video Theater, 10-Band Audio DSP Equalizer, and Non-Destructive Media Cutter/Extractor for Android. Built with Jetpack Compose, universal spring physics, and the Arinara Electric Slate dark theme (`#0D1117` canvas, `#161C26` panels, `#7A5AF8` electric violet primary accent), it eliminates the need for separate gallery, music player, video player, equalizer, trimmer, and EXIF inspection apps.

## Who Is It For
Android power users, audiophiles, cinephiles, students, and photographers who demand complete local control over wide media formats (`JPEG`, `PNG`, `WebP`, `HEIC`, `AVIF`, `RAW`/`DNG`, `GIF`, `SVG`, `MP4`, `MKV`, `WebM`, `MOV`, `AVI`, `MP3`, `FLAC`, `WAV`, `ALAC`, `M4A`, `OGG`, `Opus`) with zero cloud lock-in, zero ads, and zero telemetry.

## Business Model
Not determined - to be defined before public release.

## Pricing
Not determined - to be defined before public release.

## How to Use
1. Open Photos to browse the unified media timeline, Continue Watching video shelf, or filter by Photos, Videos, Music & Audio, RAW, and Animated GIFs.
2. Select 2 to 4 photos to enter Synchronized Split-Screen Compare Mode, summon the 400% Magnifier Loupe, or toggle Focus Peaking and Exposure Zebra overlays.
3. Play any audio track (`FLAC`, `WAV`, `MP3`, `M4A`, `Opus`) or hand off any video's audio stream to background playback with a 4-state Loop button (`Off`, `Queue`, `Track`, `A–B Loop`), drag-and-drop "Up Next" Queue Manager, 5 independent saved queues with millisecond position memory, Lossless Signal Path inspection, Sleep Timer, and Synced `.LRC` Lyrics.
4. Open the 10-Band Graphic & Parametric Equalizer Studio to tune frequencies (`31Hz–16kHz`), Preamp, Bass Boost, Spatial Virtualizer, Headphone Crossfeed, `+15dB` Loudness Enhancer, and view the 60fps FFT Spectrum or Fake-Lossless Waterfall Spectrogram.
5. Watch videos in the Comfy Netflix-Style Theater Player featuring Auto-Play Next Episode countdowns, `+85s` Skip Intro, Hold-for-`2.0x` speed, Bedtime Touch Lock, Extra-Dim Night Filter (`-80%`), `200%` Volume Boost, Dual Subtitles, Interactive Clickable Subtitle Transcript Search, 800% Zoom, and Frame Stepping.
6. Use the Non-Destructive Trimmer & Quality Studio to fast-cut videos/audio losslessly (`MediaMuxer`), extract audio from videos in selectable quality tiers (`Lossless`, `320kbps`, `192kbps`, `96kbps`), grab full-res video frames, scrub EXIF privacy metadata, or lock private media inside the PIN-protected Vault.

## Constraints & Assumptions
- **Platform**: Android (minSdk 24 Android 7.0 Nougat, compileSdk 36, targetSdk 36).
- **Offline First**: All media scanning, playback, DSP equalization, lossless stream cutting, audio extraction, histogram calculation, and EXIF scrubbing execute 100% locally on-device.
- **Hardware & Permissions**: Requires Android media permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` on older SDKs) and foreground media playback service permissions (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`).
- **Out of Scope**: Cloud storage subscriptions, remote server transcoding, and third-party ad/telemetry SDKs.
