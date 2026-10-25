<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog - Photos 1.1
Release Date: 2026-10-25
Banner: /Assets/Banners/PhotosBanner_1.1_2026-10-25.png

## 1.1.0 - Gesture-Driven Full-Screen Viewer & 10-Module / 70+ Parameter Pro Photo Editor

### Gesture-Driven Full-Screen Image Viewer (`PhotoCompareAndLoupeSheet`)
- **Horizontal 1-Finger Swipe Navigation**: Swipe left or right at `1.0x` zoom with live 1:1 horizontal translation physics to smoothly flip to the Next or Previous photo in the gallery.
- **Vertical Swipe-Down to Dismiss**: Swipe downward (`> 110.dp`) at `1.0x` zoom to close the full-screen viewer back to the gallery.
- **Direct 1-Tap Darkroom Launch**: Added `"Edit"` action buttons in both the top floating header and bottom control deck to launch the full-screen Pro Photo Editor for the active photo.

### 10-Module / 70+ Parameter Non-Destructive Pro Photo Editor (`PhotoEditorEngine` & `PhotoEditorSheet`)
- **Module 1 — Transform, 3D Keystone & Lens Optics (10 Parameters)**: Interactive Crop bounds (`Left`, `Top`, `Right`, `Bottom`), 9 Aspect Ratio locks (`Free`, `Original`, `1:1`, `4:5`, `5:4`, `3:2`, `2:3`, `16:9`, `9:16`), fine Straighten (`-45°..+45°` with auto-zoom), `90°` Rotate steps, Horizontal/Vertical Mirror Flip, 3D Vertical & Horizontal Keystone homography perspective correction (`-100..+100`), Anamorphic Aspect Stretch X (`0.75x..1.50x`), and Radial Lens Barrel/Pincushion Distortion correction (`-100..+100`) with bilinear sub-pixel interpolation.
- **Module 2 — Light, Exposure & Dynamic Range (10 Parameters)**: Photometric Exposure (`-4.00..+4.00 EV`), Brilliance / Smart HDR (`-100..+100`), S-Curve Contrast (`-100..+100`), dynamic Contrast Pivot (`0.15..0.85`), Highlights (`-100..+100`), Midtones (`-100..+100`), Shadows (`-100..+100`), Whites (`-100..+100`), Blacks (`-100..+100`), and Dark-Channel Atmospheric Dehaze (`-100..+100`).
- **Module 3 — 4-Channel Monotone Cubic Point Curves & Parametric Zones (8 Parameters)**: Interactive 5-node Fritsch–Carlson monotone cubic spline curves across `Master Luma`, `Red`, `Green`, and `Blue` channels plus 4 Parametric Zone sliders (`Shadows`, `Darks`, `Lights`, `Highlights`).
- **Module 4 — 8-Band HSL Per-Color Mixer (24 Parameters)**: Cosine-windowed `Hue Shift` (`-100..+100`), `Saturation` (`-100..+100`), and `Luminance` (`-100..+100`) across all 8 color bands (`Red`, `Orange`, `Yellow`, `Green`, `Aqua`, `Blue`, `Purple`, `Magenta`).
- **Module 5 — 3-Way Color Grading Wheels (11 Parameters)**: Independent `Shadows`, `Midtones`, and `Highlights` color wheels (`Hue 0°..360°`, `Saturation 0..100`, `Luminance -100..+100`) plus `Blending` (`0..100`) and `Balance` (`-100..+100`).
- **Module 6 — White Balance, Neutral Eyedropper & Camera RGB Calibration (11 Parameters)**: `Temperature` (`-100..+100`), `Tint` (`-100..+100`), 1-Tap Neutral Gray Eyedropper on the photo canvas, skin-tone-aware `Vibrance` (`-100..+100`), `Saturation` (`-100..+100`), `Shadow Tint` (`-100..+100`), and Camera Raw `Red / Green / Blue Primary Hue & Saturation` calibration (`-100..+100`).
- **Module 7 — Detail, Texture, Edge-Masked Sharpening & Denoise (7 Parameters)**: High-frequency `Texture` (`-100..+100`), midtone `Clarity` (`-100..+100`), Unsharp Mask `Sharpening Amount` (`0..150`), `Sharpening Radius` (`0.5..3.0px`), gradient-threshold `Edge Masking` (`0..100`), bilateral edge-preserving `Luminance Denoise` (`0..100`), and `Chroma Denoise` (`0..100`).
- **Module 8 — Vignette, Silver-Halide Film Grain, Halation Bloom & Defringe (9 Parameters)**: Superellipse `Vignette Amount`, `Midpoint`, `Roundness`, and `Feather`, emulsion-weighted `Film Grain Amount`, `Grain Size`, and `Grain Roughness`, warm specular `Halation / Pro-Mist Highlight Bloom` (`0..100`), and `Chromatic Aberration Defringe` (`-100..+100`).
- **Module 9 — Selective Masks & Privacy Redaction**: Interactive `Radial Spotlight` and `Linear Gradient` selective masks (`Mask Exposure`, `Mask Contrast`, `Mask Saturation`, `Mask Temp`, `Mask Radius`, `Mask Feather`, `Invert Mask`) plus tap-to-apply `Privacy Redaction` regions (`Pixelate Mosaic` & `Blur`).
- **Module 10 — 16 Curated Film Stocks, Live Scopes & Non-Destructive MediaStore Export**: 16 film LUT profiles (`Portra 400`, `Gold 200`, `Ektar 100`, `Velvia 50`, `Pro 400H`, `Classic Chrome`, `Cinestill 800T`, `Ilford HP5`, `Tri-X 400`, `Leica Monochrom`, `Teal & Orange`, `Bleach Bypass`, `Nocturne`, `Golden Hour`, `Matte Fade`) with `0..100%` intensity, `Posterize` & `Solarize`, draggable **Before/After Split Curtain**, **Live RGB Histogram / RGB Parade / Clipping Readouts**, 20-step **Undo/Redo**, **Copy/Paste Edit Recipe**, and full-resolution `JPG 95%`, `JPG 100%`, `PNG`, and `WebP` export to `MediaStore`.
