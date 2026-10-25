# Phase 8 - ProductionHardeningAnd100Release

## Goal
Finalize all 65 features across the application, execute full unit test and `verifyAndroidRules` verification, bump version to `1.0.0`, generate `Changelog/Changelog_1.0.md`, package the release artifact `/Output/Release/Photos_1.0.0_Android.apk`, and verify every entry in `/Docs/Version/1.0/1.0/1.0.0/Anchor.md`.

## Scope
- End-to-end navigation and state integration across Home, Albums, Audio Player, 10-Band EQ, Video Player, Trimmer, Photo Compare/Loupe, Inspector, Private Vault, Trash, and Settings.
- Real device media indexing via Android `MediaStore` with automatic permissions on launch and `ON_RESUME` refresh, zero fabricated sample files, and real `ExoPlayer` + `PlayerView` video/audio playback.
- Borderless UI cards, chips, buttons, bottom sheets, and floating bars across the app, concise consumer-friendly copy, and removal of the Cut bottom tab and all `"Studio"` strings.
- Comprehensive unit test suite verifying all engines (`PhotosDesignTokensTest`, `PhotosPowerUserSuiteTest`).
- Version bump to `1.0.2` (`versionCode = 12`, `versionName = "1.0.2"`) in `app/build.gradle.kts`.
- Creation of `/Changelog/Changelog_1.0.md`, `/Assets/Banners/PhotosBanner_1.0_2026-10-25.png`, and `/Output/Release/Photos_1.0.2_Android.apk` (`/Output/Release/Photos_1.0.2_Beta_Android.apk`).
- Real-Time EQ DSP & Visual Rendering: API 28+ `DynamicsProcessing` 10-band hardware EQ + `Preamp` inputGain + 5-band `Equalizer` fallback, real-time 96-point cascaded RBJ Biquad frequency response curve (`20Hz`–`20kHz`) with overlaid live 30–60fps FFT spectrum, 2D time-frequency Waterfall Spectrogram ring buffer, and multi-band touch curve sculpting.
- Full-Screen Immersive Image Viewer (`PhotoCompareAndLoupeSheet`): Edge-to-edge `fillMaxSize()` viewport replacing the cramped `320.dp` bottom-sheet box, 1-tap Fullscreen immersion toggle (hiding all top/bottom HUD chrome), double-tap (`1.0x` <-> `2.5x`) and pinch (`1.0x..8.0x`) zoom, Previous/Next photo navigation, horizontally scrollable chip bar (preventing `"Focus"` text wrapping), and `AppSlider` thumb inset fix.

## Out Of Scope
- Desktop client build.

## Features
### Unified Navigation, Real Hardware Media & Borderless Polish
Seamless transitions between all screens and sheets using borderless `AppScreen`, `AppFloatingBar`, `AppBottomSheet`, and `SpringPhysics` with zero violations in `VerifyAndroidRulesTask`, indexing only real device files and playing real audio/video streams via `ExoPlayer`.

## UI Mockup
```
+--------------------------------------------------+
| Photos                     [Compare] [Vault] [:] |
+--------------------------------------------------+
| Continue Watching                                |
| [Video 01 • 12m left]  [Video 02 • 28m left]     |
|                                                  |
| [All] [Photos] [Videos] [Audio] [RAW] [GIFs]     |
| Real Device Media Grid                           |
+--------------------------------------------------+
| Mini-Player: [>] Track_01.flac      [24b] [||]   |
|          ( [Home]    [Albums]    [Settings] )    |
+--------------------------------------------------+
```

## Logic Notes
- Every entry in `Docs/Version/1.0/1.0/1.0.0/Anchor.md`, `Docs/Version/1.0/1.0/1.0.1/Anchor.md`, and `Docs/Version/1.0/1.0/1.0.2/Anchor.md` is individually verified with test and build evidence.

## Risks
- None.

## Dependencies
- Phases 1 through 7.

## Acceptance Criteria
- `.\gradlew.bat verifyAndroidRules testDebugUnitTest assembleDebug assembleRelease` succeeds with 0 errors and 0 warnings.
- `Photos_1.0.2_Android.apk` and `Photos_1.0.2_Beta_Android.apk` are packaged in `/Output/Release/`.
