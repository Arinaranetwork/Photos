# Phase 6 - NonDestructiveTrimmerAndQualityStudio

## Goal
Implement the Non-Destructive Media Cutter, Trimmer & Quality Studio supporting Lossless Fast-Cut (`MediaExtractor` + `MediaMuxer`), Trim Outside (Keep) vs. Cut Middle & Join, Multi-Segment Highlight Splitter, 1-Tap Video-to-Audio Extractor with 4 Quality Tiers (`Lossless Original`, `320kbps`, `192kbps`, `96kbps`), Audio Fade-In/Fade-Out, Native-Resolution Video Frame Grabber (`PNG`/`JPEG`), Video-to-Animated-GIF Maker, and Voice-Over Photo Notes.

## Scope
- `MediaTrimmerEngine` executing packet-level lossless demux/remux for `MP4`, `MOV`, `3GP`, `M4A`, `AAC`, and `WAV` files without re-encoding quality loss.
- `AudioQualityConverter` generating `Lossless WAV/M4A`, `Studio High 320kbps`, `Balanced 192kbps`, and `Compact Voice 96kbps` outputs with optional Fade-In/Fade-Out envelopes.
- `VideoFrameAndGifExporter` capturing exact native-resolution `PNG`/`JPEG` still frames and synthesizing loopable/Boomerang animated `GIF`s.
- `VoicePhotoNoteManager` pairing recorded or attached audio clips with photographs.
- `MediaTrimmerScreen` providing dual start/end range handles, `-100ms`/`+100ms` nudge controls, waveform/filmstrip preview, and multi-segment markers.

## Out Of Scope
- Cloud video rendering.

## Features
### Lossless Fast-Cut & Multi-Segment Trimmer (`F-S01`–`F-S04`)
Cuts video and audio ranges in under 1 second via stream copy (`Trim Outside`, `Cut Middle`, and `Multi-Segment Split`).

### Multi-Quality Audio Extractor & Converter (`F-S05`, `F-S06`, `F-A08`)
Extracts audio tracks from videos or converts existing audio into 4 selectable bitrate/lossless tiers with Fade-In/Out.

### Frame Grabber, GIF Maker & Voice Photos (`F-S07`–`F-S09`)
Exports native-resolution video frames, animated GIFs/Boomerangs, and audio-attached Voice Photos.

## UI Mockup
```
+--------------------------------------------------+
| < Non-Destructive Trimmer & Studio       [Export]| (AppTopBar)
+--------------------------------------------------+
| [Trim: Keep Inside] [Cut Middle] [Multi-Segment] | (AppPillChip)
| Start: 00:12.400  |======[A====B]====|  01:45.000|
| [-100ms] [+100ms]                    [-100][+100]|
| [Lossless Fast-Cut] [Extract Audio] [Grab Frame] | (AppButton)
| Quality: [Lossless] [320kbps] [192kbps] [96kbps] | (AppPillChip)
+--------------------------------------------------+
```

## Logic Notes
- Source files are strictly read-only; all exports write to new timestamped files in app/media storage.

## Risks
- Risk: Keyframe alignment on stream copy. Mitigation: Seek `MediaExtractor` with `SEEK_TO_PREVIOUS_SYNC` for video tracks and exact sample time for audio tracks.

## Dependencies
- Android `MediaExtractor`, `MediaMuxer`, `MediaMetadataRetriever`.

## Acceptance Criteria
- Lossless trimming, audio extraction across quality tiers, frame capture, and GIF export pass unit tests and `verifyAndroidRules`.
