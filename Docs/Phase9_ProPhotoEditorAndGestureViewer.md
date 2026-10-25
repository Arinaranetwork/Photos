# Phase 9 - ProPhotoEditorAndGestureViewer

## Goal
Deliver a gesture-driven full-screen image viewer (horizontal swipe left/right between photos with live drag physics and vertical swipe-down to dismiss) and a 10-Module, 70+ Parameter real non-destructive Pro Photo Editor with zero mock/fabricated effects, real mathematical LUT + convolution + 3D homography/keystone + 8-band HSL + 3-way color grading + RGB primary calibration + selective masks + privacy redaction, and full-resolution `MediaStore` export.

## Scope
- **Gesture-Driven Full-Screen Image Viewer (`PhotoCompareAndLoupeSheet.kt`)**:
  - Horizontal swipe left/right gesture at `1.0x` zoom to smoothly navigate Next / Previous photos with live horizontal translation physics (`swipeOffsetX`).
  - Vertical swipe-down gesture at `1.0x` zoom (`swipeOffsetY > 110.dp`) to dismiss the viewer back to the gallery.
  - Direct 1-tap **"Edit"** launch button in both the top header and bottom control deck.
- **10-Module / 70+ Parameter Real Photo Editing Engine (`PhotoEditorEngine.kt`)**:
  1. **Module 1 — Transform, 3D Keystone & Lens Optics (10 Params)**: Interactive Crop (`left`, `top`, `right`, `bottom`), 9 Aspect Ratio locks (`Free`, `Original`, `1:1`, `4:5`, `5:4`, `3:2`, `2:3`, `16:9`, `9:16`), Straighten (`-45°..+45°`), `90°` Rotate steps, Flip Horizontal/Vertical, Vertical 3D Keystone (`-100..+100`), Horizontal 3D Keystone (`-100..+100`), Anamorphic Aspect Stretch X (`0.75x..1.50x`), and Radial Lens Barrel/Pincushion Distortion (`-100..+100`).
  2. **Module 2 — Light, Exposure & Dynamic Range (10 Params)**: Exposure (`-4.0..+4.0 EV`), Brilliance / Smart HDR (`-100..+100`), Contrast (`-100..+100`), Contrast Pivot (`0.15..0.85`), Highlights (`-100..+100`), Midtones (`-100..+100`), Shadows (`-100..+100`), Whites (`-100..+100`), Blacks (`-100..+100`), and Dehaze (`-100..+100`).
  3. **Module 3 — 4-Channel Point Curves & Parametric Zones (8 Params)**: Interactive 5-node spline curves for `Master Luma`, `Red`, `Green`, and `Blue` channels + Parametric Zone sliders (`Highlights`, `Lights`, `Darks`, `Shadows`).
  4. **Module 4 — 8-Band HSL Per-Color Mixer (24 Params)**: Cosine-windowed `Hue Shift` (`-100..+100`), `Saturation` (`-100..+100`), and `Luminance` (`-100..+100`) across all 8 color bands (`Red`, `Orange`, `Yellow`, `Green`, `Aqua`, `Blue`, `Purple`, `Magenta`).
  5. **Module 5 — 3-Way Color Grading Wheels (11 Params)**: Independent `Shadows`, `Midtones`, and `Highlights` color grading (`Hue 0°..360°`, `Saturation 0..100`, `Luminance -100..+100`) plus `Blending` (`0..100`) and `Balance` (`-100..+100`).
  6. **Module 6 — White Balance, Eyedropper & Camera RGB Calibration (11 Params)**: `Temperature` (`-100..+100`), `Tint` (`-100..+100`), 1-Tap Neutral Gray Eyedropper on photo canvas, `Vibrance` (`-100..+100`), `Saturation` (`-100..+100`), `Shadow Tint` (`-100..+100`), and `Red / Green / Blue Primary Hue & Saturation` calibration (`-100..+100`).
  7. **Module 7 — Detail, Texture, Edge-Masked Sharpening & Denoise (7 Params)**: `Texture` (`-100..+100`), `Clarity` (`-100..+100`), `Sharpening Amount` (`0..150`), `Sharpening Radius` (`0.5..3.0`), `Sharpening Edge Masking` (`0..100`), `Luminance Denoise` (`0..100`), and `Chroma Denoise` (`0..100`).
  8. **Module 8 — Vignette, Grain, Cine Halation Bloom & Defringe (9 Params)**: `Vignette Amount` (`-100..+100`), `Vignette Midpoint` (`0..100`), `Vignette Roundness` (`-100..+100`), `Vignette Feather` (`0..100`), `Film Grain Amount` (`0..100`), `Grain Size` (`1..100`), `Grain Roughness` (`0..100`), `Halation / Pro-Mist Highlight Bloom` (`0..100`), and `Chromatic Aberration Defringe` (`-100..+100`).
  9. **Module 9 — Selective Masks & Privacy Redaction**: Radial Spotlight Mask & Linear Gradient Mask (`Mask Exposure`, `Mask Contrast`, `Mask Saturation`, `Mask Temperature`, `Mask Feather`, `Invert Mask`) + interactive **Privacy Blur / Mosaic Pixelate Redaction** regions.
  10. **Module 10 — 16 Curated Film Stocks, Creative FX & MediaStore Export**: 16 Film LUTs (`0..100%` Intensity) + `Posterize Levels` (`2..64`) + `Solarize` (`0..100`), 20-step Undo/Redo, Copy/Paste Edit Recipe, Before/After Split Curtain, Live RGB Histogram / RGB Parade / Zebra Clipping overlays, and full-resolution `JPEG` / `PNG` / `WEBP` `MediaStore` export.

## Out Of Scope
- Cloud AI generative inpainting requiring external servers.

## Features
### Gesture Swipe Navigation & 10-Module Non-Destructive Photo Mastering
Swipe horizontally through photos with one finger at `1.0x` zoom, swipe down to close, or tap **Edit** to open the 10-module darkroom with real pixel pipeline rendering and live RGB/Zebra scopes.

## UI Mockup
```
+--------------------------------------------------+
| [X] [Undo] [Redo]   [RGB Histogram | Zebra] [Save]|
+--------------------------------------------------+
|                                                  |
|           Live Edited Photo Canvas               |
|         (Before |<--->| After Split)             |
|                                                  |
+--------------------------------------------------+
| [Transform] [Light] [Curves] [HSL] [3-Way] ...   |
| [Exposure +0.4] [Brilliance +25] [Pivot 0.50]    |
| Parameter =======(O)================ [0] [Reset] |
+--------------------------------------------------+
```

## Logic Notes
- Uses `ElectricSlateTokens` and `ElmsSans` exclusively (`AR-001`–`AR-016`) with zero `.border(...)` outlines (`R-010`) and zero `"Studio"` text (`R-012`).
- Two-tier rendering: downsampled `960px` interactive preview on `Dispatchers.Default` for responsive slider scrubbing, and full-resolution `Bitmap` processing on `Dispatchers.IO` when exporting to `MediaStore`.

## Risks
- Risk: Large bitmap memory allocation during full-res export. Mitigation: Process row-by-row / reusable `IntArray` pixel buffers and safe `inSampleSize` fallback if device RAM is constrained.

## Dependencies
- Phases 1–8.

## Acceptance Criteria
- Horizontal swipe left/right and vertical swipe-down gestures work smoothly in `PhotoCompareAndLoupeSheet.kt`.
- All 10 modules and 70+ parameters in `PhotoEditorEngine.kt` and `PhotoEditorSheet.kt` perform genuine mathematical pixel/geometry processing (verified by unit tests in `PhotosPowerUserSuiteTest.kt`).
- `.\gradlew.bat verifyAndroidRules testDebugUnitTest assembleDebug assembleRelease` succeeds with 0 errors.
