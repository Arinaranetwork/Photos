# Phase 7 - DeepExifCodecInspectorAndPrivateVault

## Goal
Implement the Deep EXIF, IPTC & Video/Audio Codec Stream Inspector, Live 256-Bin RGB + Luminance Histogram, 1-Tap EXIF Privacy Scrubber, Batch Image Converter/Resizer & Token Renamer, 6-Swatch Color Palette Extractor, PIN-Locked Private Vault, and 30-Day Recycle Bin + Duplicate/Large File Finder.

## Scope
- `MediaInspectorEngine` calculating 256-bin R, G, B, and Luminance histograms, extracting complete EXIF/codec metadata + `SHA-256` file checksums, extracting 6 dominant `#HEX`/`RGB`/`HSL` color swatches, and scrubbing sensitive EXIF tags (`GPS`, serials, device info) on export.
- `BatchMediaProcessor` executing batch image format conversion (`JPEG`, `PNG`, `WebP`), dimension resizing, batch EXIF date shifting, and token-based batch renaming (`{date}_{time}_{album}_{width}x{height}_{counter}`).
- `PrivateVaultAndTrashManager` managing the PIN-protected encrypted/sandboxed Private Vault directory, 30-day Recycle Bin (Trash) with restore, and fast Duplicate / Large File detection.

## Out Of Scope
- Cloud vault backup.

## Features
### Live 256-Bin RGB Histogram & Deep Stream Inspector (`F-G06`, `F-G07`, `F-G10`)
Renders R, G, B, and Luminance curves, full camera EXIF & container codec tables, and 6-swatch dominant hex palette cards.

### EXIF Privacy Scrubber & Batch Studio (`F-G08`, `F-G09`)
Strips GPS/device metadata with 1 tap and batch-converts, resizes, or token-renames multiple files.

### PIN-Locked Private Vault, Recycle Bin & Duplicate Cleaner (`F-G11`, `F-G12`)
Secures private media behind a PIN lock, retains deleted items for 30 days in Trash, and identifies duplicate files.

## UI Mockup
```
+--------------------------------------------------+
| Inspector & Privacy Studio               [Close] | (AppBottomSheet)
+--------------------------------------------------+
| Live 256-Bin RGB + Luminance Histogram           | (Canvas)
| Palette: [#0D1117] [#7A5AF8] [#1F2736] [#94A3B8] |
| EXIF: ISO 100 • 1/500s • f/1.8 • 24mm • No GPS   | (AppCard)
| [Scrub EXIF Privacy Tags] [Move to Private Vault]| (AppButton)
+--------------------------------------------------+
```

## Logic Notes
- PIN hashes are stored using `SHA-256` with salt; no hardcoded secrets (`Section 12`).

## Risks
- Risk: Accidental permanent data loss. Mitigation: Default delete moves items to the 30-day Recycle Bin with 1-tap restore.

## Dependencies
- `androidx.exifinterface:exifinterface`.

## Acceptance Criteria
- RGB histogram, EXIF scrubber, palette extractor, batch token renamer, PIN Vault, and Trash pass unit tests and `verifyAndroidRules`.
