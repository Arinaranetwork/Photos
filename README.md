<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

![Photos 1.0 Banner](Assets/Banners/PhotosBanner_1.0_2026-10-25.png)

# Photos

**Latest Version:** `1.0.3` (Beta) — [Changelog](Changelog/Changelog_1.0.md)

## What Is This
**Photos** is an all-in-one, offline-first Android media gallery, full-screen image viewer, audiophile lossless music player, 10-band real-time EQ & DSP engine, and cinema video player built on the **Electric Slate** design system.

## Who Is It For
Photographers, audiophiles, cinephiles, and power users who want a single native Android application for high-resolution photos (`JPEG`, `PNG`, `WebP`, `HEIC`, `AVIF`, `GIF`, `DNG`/`CR2`/`NEF`/`ARW`), lossless music (`FLAC`, `WAV`, `ALAC`, `MP3`, `AAC`, `Opus`), and cinema video (`MP4`, `MKV`, `WebM`).

## Shipped Features (`v1.0.3`)
- **Full-Screen Native-Ratio Video Theater**:
  - Plays every portrait (`9:16`) and landscape (`16:9`/`21:9`) video at its **100% original resolution, bitrate, and native aspect ratio** (`Original` fit mode by default, plus optional `16:9`, `21:9`, `4:3`, and `Zoom Crop` modes) with **1-Tap Fullscreen Cinema Mode**.
  - **Working Hardware + Extra-Dim Brightness (`-80%`–`100%`) & `200%` Hardware Volume Boost**: Real window hardware brightness control + sub-zero Extra-Dim overlay, `LoudnessEnhancer` `+15dB` audio preamp boost (`101%`–`200%`), A-B loop auto-seek, bedtime touch lock, hold-for-`2x` speed, `+85s` Skip Intro, dual subtitles, and real original-resolution video frame capture.
- **Full-Screen Edge-to-Edge Immersive Image Viewer**:
  - 100% edge-to-edge photo viewport (`Single`, `2-Up`, and `4-Up` synchronized comparison modes).
  - **1-Tap Fullscreen Immersion**: Tap the photo or the top-bar Fullscreen button to hide all UI chrome for an unobstructed view.
  - **Double-Tap & Pinch Zoom (`100%`–`800%`)**: Instant double-tap zoom (`1.0x` <-> `2.5x`), smooth pinch-to-zoom, `Prev` / `Next` photo navigation, `400%` floating Magnifier Loupe, and Focus Peaking overlay.
- **Real-Time 10-Band & Parametric EQ Studio**:
  - Hardware-accelerated Android API 28+ `DynamicsProcessing` (10-band `PreEq` + `PostEq` + `Preamp` inputGain) alongside `Equalizer`, `BassBoost`, `Virtualizer`, and `+15dB` `LoudnessEnhancer`.
  - **Real-Time Live FFT Spectrum & Biquad Response Graph**: Live 30–60fps hardware `Visualizer` FFT spectrum overlaid directly behind the 96-point cascaded RBJ Biquad frequency response curve (`20Hz`–`20kHz`), plus a scrolling 2D time-frequency Waterfall Spectrogram and interactive 10-band touch faders.
- **Audiophile Music Player & 5 Multi-Queue Slots**:
  - 4-state loop (`Off`, `Queue`, `Track`, and precision `A-B Section Loop`), 5 independent queue slots with millisecond position memory, synced `.LRC` lyrics, `.CUE` sheet splitter, and bit-perfect signal path inspector.
- **Deep EXIF Inspector & Private PIN Vault**:
  - Live 256-bin RGB + Luminance histogram, `SHA-256` integrity hash, 1-tap GPS/EXIF privacy scrubber, Duplicate & Burst finder, and salted `SHA-256` PIN Vault.

## Installation & Run Steps
1. Download [`Photos_1.0.3_Beta_Android.apk`](Output/Release/Photos_1.0.3_Beta_Android.apk) (or [`Photos_1.0.3_Android.apk`](Output/Release/Photos_1.0.3_Android.apk)) from `/Output/Release` or the [GitHub Releases](https://github.com/Arinaranetwork/photos/releases) page.
2. Install on any Android 8.0+ (`API 26+`) device:
   ```powershell
   adb install -r Output/Release/Photos_1.0.3_Beta_Android.apk
   ```
3. To build from source:
   ```powershell
   .\gradlew.bat verifyAndroidRules testDebugUnitTest assembleDebug assembleRelease
   ```

## Configuration
Copy `.env.Example` to `.env` for optional signing keystore environment variables (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`). No API keys or external servers are required; Photos operates 100% offline.

## License & Owner
Copyright (c) 2026 **Arinara Network**. All Rights Reserved. See [LICENSE](LICENSE).
