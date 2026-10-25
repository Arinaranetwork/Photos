# Phase 3 - AudiophileAudioPlayerQueueAndLossless

## Goal
Implement the Audiophile Music & Audio Player featuring a foreground `MediaSessionService`, 4-state Loop + A–B Section Loop, Drag-and-Drop "Up Next" Queue Manager, 5 Independent Saved Multi-Queues with position memory, Lossless/Hi-Res Signal Path Inspector, Output Quality Switcher, Gapless/Crossfade, Independent Pitch/Tempo (`432Hz`), Skip Silence, ReplayGain, Headphone Crossfeed, Sleep Timer, Synced `.LRC` Lyrics, CUE Sheet Splitter, and Offline Tag Editor.

## Scope
- `PhotosMediaPlaybackService` (`MediaSessionService`) for continuous background audio & video-audio playback.
- `MultiQueueManager` supporting 5 independent named queues (`Main`, `Study / Lo-Fi`, `Workout`, `Podcasts & Lectures`, `Late Night`) with per-queue track list, active index, loop mode, and exact millisecond resume position.
- `LoopState` (`OFF`, `QUEUE`, `TRACK`, `AB_SECTION`) with loop counter and auto-advance after $N$ loops.
- `AudioQualitySignalPath` inspector classifying `HI-RES LOSSLESS`, `LOSSLESS`, `HIGH QUALITY`, and `STANDARD` streams with output mode switcher (`Bit-Perfect Direct PCM`, `32-Bit Float DSP`, `Voice Clarity Mono`).
- Synced `.LRC` lyric parser, `.CUE` virtual track parser, Sleep Timer with 30s fade-out, and ID3/Vorbis metadata tag editor.

## Out Of Scope
- Online lyric scraping.

## Features
### 4-State Loop & A–B Loop (`F-A01`)
Cycles `Off -> Loop Queue -> Loop Track -> A-B Section Loop` with millisecond start/end markers and loop count tracking.

### Up Next Queue & 5 Multi-Queues (`F-A02`, `F-A03`, `F-A04`)
Provides `Play Next`, `Add to Queue`, drag-and-drop reordering, history-aware visible shuffle, and 5 persistent queues with position memory.

### Lossless Signal Path & Output Modes (`F-A05`, `F-A06`, `F-A07`)
Inspects bit depth, sample rate, bitrate, codec, DSP status, and hardware output route.

### Audiophile Comfort Controls (`F-A09`–`F-A18`)
Includes Gapless, Crossfade (`0–12s`), Pitch/Tempo (`432Hz`), Skip Silence with "Time Saved" counter, ReplayGain, Crossfeed (`0–100%`), L/R Balance, Sleep Timer, `.LRC` Lyrics, CUE Splitter, and Tag Editor.

## UI Mockup
```
+--------------------------------------------------+
| < Audiophile Player            [Signal] [Queues] | (AppTopBar)
+--------------------------------------------------+
| [HI-RES LOSSLESS • 24-bit / 96kHz FLAC]          | (AppPillChip)
| 01:12 [====[A]=====(O)=====[B]---------] 04:05   | (AppSlider)
| [Shuffle]  [|<<]   [Play/Pause]   [>>|]  [Loop]  | (AppButton)
| [Up Next (5 Queues)] [EQ] [Lyrics] [Sleep] [Tag] | (AppPillChip)
+--------------------------------------------------+
```

## Logic Notes
- `MultiQueueManager` persists all 5 queues and their playback positions to local JSON preferences.
- `PhotosMediaPlaybackService` posts media notifications and updates the docked Mini-Player pill.

## Risks
- Risk: Background service termination on Android 14+. Mitigation: Declare `foregroundServiceType="mediaPlayback"` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.

## Dependencies
- `androidx.media3:media3-exoplayer`, `media3-session`, `media3-ui`.

## Acceptance Criteria
- All 4 loop states, 5 multi-queues, lossless signal path inspector, pitch/tempo, lyrics, sleep timer, and background playback pass unit tests and `verifyAndroidRules`.
