<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog — Photos 1.6 (2026-10-27)

## Added
- **Unified Visual Tool Media Picker (`ToolMediaPickerSheet.kt`, `HomeScreen.kt`, `SuperUtilityStudioSheet.kt`, `PdfStudioSheet.kt`, `AudioArrangerSheet.kt`, `VideoEditorSheet.kt`)**:
  - **Predictable Pre-Tool Selection**: Eliminated silent random auto-selection (`allPhotos.take(4)`, `allPhotos.take(8)`, `allVideos.firstOrNull()`, `allAudioTracks.take(2)`) when launching tools from the `Tools` Bento grid without prior gallery selection; opening any tool now opens `ToolMediaPickerSheet` filtered to the tool's required media type (`MULTI_PHOTO`, `SINGLE_PHOTO`, `SINGLE_VIDEO`, `SINGLE_PDF`, `MULTI_AUDIO`, `SINGLE_AUDIO`), or uses pre-selected gallery/audio items directly.
  - **Visual Grid, Album Filters, Search & Ordered Selection**: Added album/folder filter chips (`All`, `Camera`, `Screenshots`, etc.), instant title/artist/album search, sort chips (`Newest`, `Oldest`, `Largest`, `Name`), `1080p+` filter, 3-column visual thumbnail grid with resolution/duration/format badges, numbered selection order badges (`1, 2, 3...`), and a bottom selected mini-thumbnail tray with 1-tap `Continue (N)`.
  - **In-Tool Visual Media Cards**: Replaced text-only item rows (`"Use 8"` / `"Use 24"`) and `.take(20)` text chips inside `SuperUtilityStudioSheet`, `PdfStudioSheet`, `AudioArrangerSheet`, and `VideoEditorSheet` with visual thumbnail cards (`ToolSelectedMediaSection` and `ToolSingleMediaCard`) supporting `+ Select` / `Change`, `Clear`, reorder, and remove controls.
- **Expanded Video Color Grading Controls (`VideoEditorSheet.kt`, `VideoAudioConverterEngine.kt`, `HardwareGlVideoRenderPipeline.kt`, `PlaybackAndStudioPreferences.kt`)**:
  - Added **Tint** (`-100..+100`), **Highlights** (`-100..+100`), **Shadows** (`-100..+100`), and **Vignette** (`-100..+100`) sliders alongside **Brightness**, **Contrast**, **Saturation**, and **Warmth**, plus a 1-tap **Reset Color** action and full custom preset persistence (`VideoEditorCustomPreset`).
- **Per-Image & Rotate-All Rotation + Custom Background Color in PDF & Image Tools (`PdfStudioEngine.kt`, `PdfStudioSheet.kt`, `BatchImageStudioEngine.kt`, `SuperUtilityStudioSheet.kt`, `ToolMediaPickerSheet.kt`)**:
  - **Rotate Each Image or All Images in PDF & Image Studios**: Added per-image `Rotate 90°` buttons directly on every item row in `ToolSelectedMediaSection` and on the live PDF preview stage, plus 1-tap `Rotate All 90°` and `Reset 0°` actions across `PDF Create`, `PDF Pages`, `Collage`, and `Convert Images`.
  - **Custom Background Color Picker**: Added page and canvas background color selection with curated presets (`White`, `Warm Paper`, `Soft Gray`, `Slate Dark`, `Pure Black`, `Navy`) and a `Custom` color builder featuring `#RRGGBB` hex input and live `Red`/`Green`/`Blue` (`0–255`) sliders with automatic footer/watermark contrast adaptation.

## Fixed
- **Real-Time OpenGL ES 2.0 Video Color Grading in Live Preview & Export (`GlVideoPreviewTextureView`, `HardwareGlVideoRenderPipeline.kt`, `VideoEditorSheet.kt`)**:
  - Replaced the broken `PlayerView` + `Modifier.graphicsLayer` color preview in `VideoEditorSheet` with `GlVideoPreviewTextureView`—a dedicated OpenGL ES 2.0 `TextureView` + `EGL14` + `GLES20` OES fragment shader renderer sharing `VERTEX_SHADER`, `FRAGMENT_SHADER_OES`, `computeOrthographicVideoMvpMatrix`, and `computeColorGradeUniforms` with `HardwareGlVideoRenderPipeline`.
  - All 8 color grading parameters (`Brightness`, `Contrast`, `Saturation`, `Warmth`, `Tint`, `Highlights`, `Shadows`, `Vignette`) and all 6 built-in presets (`Original`, `Vivid`, `Teal & Orange`, `Warm Vintage`, `Noir B&W`, `Cyberpunk`) + custom presets now update the live video preview immediately at 60/120fps whether playback is playing or paused, and bake into exported videos with resilient hardware encoder/decoder fallback.
