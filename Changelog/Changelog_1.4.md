<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Changelog — Photos 1.4 (2026-10-26)

![Photos 1.4 Banner](../Assets/Banners/PhotosBanner_1.4_2026-10-26.svg)

## Added
- **On-Device PDF Studio (`PdfStudioEngine` & `PdfStudioSheet`)**: Full 100% offline PDF workstation eliminating external PDF tools:
  - **Images → PDF**: Convert multiple gallery photos into a multi-page `.pdf` document with page size presets (`A4 Portrait`, `A4 Landscape`, `US Letter`, `Fit Image`), margin controls (`None`, `Small`, `Standard`), document enhancement filters (`Original`, `Auto Contrast`, `B&W Document`, `Magic Whiteboard`), DPI/compression presets (`300 DPI Print`, `150 DPI Standard`, `72 DPI Compact`), optional page numbers (`Page X of Y`), and diagonal watermark stamping.
  - **PDF → Images**: Render every page or custom page ranges (`1-3, 5`) of any PDF via hardware `PdfRenderer` into `.JPG`, `.PNG`, or `.WEBP` images at `300 DPI`, `150 DPI`, or `96 DPI`.
  - **Merge PDFs**: Combine multiple PDF documents in custom order into a single unified `.pdf` with total page and size summary.
  - **Split, Reorder, Rotate & Delete PDF Pages**: Visual page-by-page organizer with thumbnail previews, individual `90°` page rotation, page deletion, reordering (`↑`/`↓`), and custom page-range extraction (`e.g. 1-3, 5`).
  - **Compress PDF**: Downsample and re-encode heavy PDF documents (`150 DPI Standard` / `96 DPI Compact` / `72 DPI Ultra`) with before/after byte savings report.
  - **Sign & Watermark PDF**: Draw a vector ink signature on a touch signature pad (`Black`, `Royal Blue`, `Crimson Red` ink) or stamp custom diagonal watermarks onto selected pages of a PDF.
  - **Built-In Native PDF Reader & Gallery Filter (`MediaType.PDF`, `MediaFilter.PDF_DOCS`)**: Indexes `.pdf` documents in `MediaRepository` (`MediaStore.Files` + app studio output), renders live page-1 thumbnails with page-count badges (`PDF • Np`), and opens a native zoomable multi-page PDF reader with quick launch into PDF Studio tools.
- **Gallery Multi-Select Mode & Floating Super-Suite Action Bar (`HomeScreen`)**:
  - Long-press any photo, video, or PDF card (or tap `Select` in the top action row) to enter Multi-Select Mode with ordered selection badges (`1, 2, 3...`), `Select All`, and `Clear`.
  - Contextual floating action bar provides one-tap access to **Images → PDF**, **Merge PDFs**, **Batch Convert**, **Stitch / Collage**, **GIF Maker**, and **Share**.
- **Batch Image Studio, Long Screenshot Stitcher, Perspective Scanner & Offline QR Studio (`BatchImageStudioEngine` & `SuperUtilityStudioSheet`)**:
  - **Batch Format Converter & Size Compressor**: Convert and compress dozens of images at once between `.JPG`, `.PNG`, and `.WEBP` with quality slider (`10–100%`), max-edge resize (`Original`, `3840px 4K`, `2560px QHD`, `1920px FHD`, `1080px`), and EXIF stripping.
  - **Long Screenshot Stitcher & Grid Collage Builder**: Combine selected photos into a seamless **Vertical Long Stitch**, **Horizontal Panorama Strip**, **2×2 Grid**, **3×3 Grid**, or **1+2 Hero Collage** with adjustable spacing, corner radius, and background fill (`Slate Dark`, `Pure Black`, `Paper White`).
  - **4-Corner Perspective Document Scanner**: Homography perspective correction (`Matrix.setPolyToPoly`) with interactive 4-corner handles and document enhancement modes (`Magic Color`, `Crisp B&W`, `Grayscale`, `Original`) exporting either a high-res image or a 1-page `.PDF`.
  - **Offline QR Code Generator**: Pure-Kotlin Version-3 Byte-mode QR code encoder (Reed-Solomon GF(256) ECC + 8-mask penalty evaluation) for URLs, Wi-Fi credentials (`WIFI:S:...;T:WPA;P:...;;`), and plain text, with custom foreground/background colors, High-Res `.PNG` export, and printable **A4 Multi-QR Sheet `.PDF`** export.
- **Animated GIF89a Maker & Audio-to-MP4 Social Video Synth (`GifAndMediaSynthEngine`)**:
  - **Animated GIF89a Maker**: Pure-Kotlin GIF89a encoder with 6×6×6 RGB + grayscale palette quantization, Netscape 2.0 infinite loop extension, LZW compression, configurable FPS (`4..15 FPS`), resolution (`320p`, `480p`, `640p`), **Boomerang (`Forward + Reverse`)**, and Reverse order from selected photos or video clips.
  - **Audio + Cover Art → `.MP4` Video Synth**: Hardware H.264 (`video/avc`) + AAC (`audio/mp4a-latm`) `MediaCodec` + `MediaMuxer` synthesizer that turns any audio track plus cover art into a shareable `1080×1080` (`1:1`), `1080×1920` (`9:16`), or `1920×1080` (`16:9`) `.MP4` video with animated radial spectrum bars directly from the Super-Suite or full-screen Music Player (`MP4 Video` pill).
- **Persistent Audio & Video Loop Modes (`PlaybackAndStudioPreferences`)**:
  - Automatically saves and restores both **Audio Loop Mode** (`Off`, `1x`, `2x`, `3x`, `5x`, `∞`, `ALL`) and **Video Loop Mode** (`Off`, `1x`, `2x`, `3x`, `5x`, `∞`, `All`) across track/video switches and app restarts.
- **5x–15x Faster Hardware GPU Video Render, Single-Flight Render Lock, Stop Render & Quality Options (`HardwareGlVideoRenderPipeline` & `VideoEditorSheet`)**:
  - **Zero-Copy Hardware OpenGL ES 2.0 Surface-to-Surface Video Render**: Replaces slow `MediaMetadataRetriever` frame seeking with sequential `MediaExtractor` + hardware `MediaCodec` Decoder → `EGL14`/`GLES20` `GL_TEXTURE_EXTERNAL_OES` shader (4×5 ColorMatrix + spatial crop/zoom/flip/rotate) → hardware `MediaCodec` Encoder at `KEY_OPERATING_RATE = Short.MAX_VALUE` and `KEY_PRIORITY = 0`, plus an instant `<0.5s` lossless stream-copy remux fast path when only trimming or 90°/180°/270° rotation is applied.
  - **Single-Flight Render Lock & Stop Render**: Blocks duplicate render requests while a video export is active and adds a **Stop Render** button in both the top action bar and export progress card that cancels the job and deletes partial output files cleanly.
  - **Video Editor Quality Tab (`VideoEditorToolTab.QUALITY`)**: Select output Resolution (`Original`, `4K`, `1440p`, `1080p`, `720p`, `480p` filtered to source dimensions), Frame Rate (`Original`, `60 FPS`, `30 FPS`, `24 FPS`), Bitrate Profile (`High`, `Balanced`, `Compact`), and Codec (`H.264`, `HEVC`) with live estimated output size (`~MB`).
- **Persistent Custom Presets Across Equalizer, Video Editor & Photo Editor (`PlaybackAndStudioPreferences`)**:
  - Custom presets in **Equalizer** (including full 10-band Parametric EQ filters and DSP parameters), **Video Editor** (Color + Transform + Speed + Quality), and **Photo Editor** (full `PhotoEditRecipe` darkroom parameters) persist across app restarts and support 1-tap apply and deletion (`×`).
- **Audio Player `3 Dots` (`•••`) Track Power Actions Menu & Song Cover Art Editor (`AudiophilePlayerSheet`)**:
  - **Dedicated `3 Dots` Power Actions Menu**: Tapping `•••` opens the Track Power Actions drawer (**Edit Tags & Cover Art**, **Trim / Cut Audio**, **Audio + Cover → MP4**, **Stamp `.LRC` Lyrics**, **Quick 15s A–B Loop**, **Sleep Timer**, **Bookmark Here**, **Share Audio**), while the lossless quality pill toggles the Signal Path Inspector.
  - **Song Cover Art Image Picker & ID3v2.3 `APIC` Embedder in `Tags`**: Pick any image from the system photo picker or an inline gallery photo strip in the `Tags` tab to set or replace a song's cover art (`768×768` JPEG + `ID3v2.3` `APIC`/`TIT2`/`TPE1`/`TALB` frame embedding for writable `.mp3` files), with instant live artwork cache invalidation across the player, queue, and Android media notification.
