# Master Feature Blueprint & Roadmap to 1.0.0 - Photos
Last Updated: 2026-10-24

## 1. Executive Product Philosophy
**Photos** is designed to be the ultimate **100% offline, zero-telemetry Power-User Universal Media Gallery, Audiophile Music Player, Netflix-Style Home Theater, and Non-Destructive Media Studio** for Android.

Whereas mainstream default apps (Google Photos, Samsung Gallery, Apple Photos) strip away local control to push cloud subscriptions—and force power users to juggle 6+ separate apps (**Musicolet**, **Poweramp**, **Neutron**, **Wavelet**, **Next Player**, **MPV**, **PotPlayer**, **Aves**, **FastStone**, **LosslessCut**)—**Photos** unifies over **65+ deeply engineered features** under the cohesive **Arinara Electric Slate** dark design system (`#0D1117` canvas, `#161C26` panels, `#1F2736` elevated surfaces, `#262F3E` 1dp subtle borders, `#7A5AF8` electric violet primary accent, `#DC5A5A` destructive alert, and universal `SpringPhysics`).

---

## 2. Pillar 1 — Audiophile & Power-User Audio Engine (18 Features)
*Benchmark References: Musicolet, Poweramp, Neutron Music Player, Symfonium, Wavelet, Apple Music Lossless.*

### F-A01: 4-State Loop Button & Sample-Accurate A–B Section Loop
- **What It Does**: A tactile loop control on the player bar that cycles through four distinct repeat states, plus custom range looping:
  1. **Loop Off**: Plays through the active queue once and stops cleanly at the end.
  2. **Loop Queue (`Repeat All`)**: Wraps around from the final track back to Track #1 infinitely.
  3. **Loop Track (`Repeat One`)**: Sample-accurate, zero-gap infinite repeat of the current track (built for ambient tracks, study loops, and favorite songs).
  4. **A–B Section Loop (`[A -> B]`)**: Places draggable `Point A` (start) and `Point B` (end) markers directly on the audio waveform progress bar with millisecond fine-tune buttons (`-100ms` / `+100ms`).
- **Power-User Extras**: Displays a live loop counter badge (`Loop #7`) and an optional **"Auto-Advance after N Loops"** setting (e.g., repeat a difficult language phrase or guitar solo 5 times, then automatically continue playing the rest of the track).

### F-A02: Interactive "Up Next" Queue Arranger (Drag-and-Drop)
- **What It Does**: A full-height `AppBottomSheet` drawer showing the currently playing track at the top followed by the complete upcoming track list.
- **Capabilities**:
  - **Drag-and-Drop Reordering**: Grab the 6-dot handle on any `AppListItem` row to move tracks up or down in real time with `JigglePress` spring feedback.
  - **1-Tap "Play Next" vs. "Add to End of Queue"**: Context menu on any song, album, folder, or video lets you inject it immediately after the current track (*Play Next*) or append it to the bottom (*Add to Queue*).
  - **Multi-Select Queue Actions**: Select multiple queued tracks to move them to the top, remove them from the queue, or save the current queue as a permanent `.m3u8` / internal playlist.
  - **Swipe-to-Remove & "Clear Upcoming"**: Remove individual tracks or wipe all upcoming tracks while keeping the active song playing uninterrupted.

### F-A03: 5 Independent Saved Multi-Queues with Position Memory (Musicolet Architecture)
- **What It Does**: Maintains up to **5 simultaneous, independent playback queues**:
  - *Queue 1: Main Music*
  - *Queue 2: Deep Focus / Lo-Fi*
  - *Queue 3: Workout / High Energy*
  - *Queue 4: Podcasts & Lectures*
  - *Queue 5: Late Night / Sleep*
- **Why Power Users Love It**: Each queue independently remembers its own track list, active track index, shuffle/loop mode, AND exact millisecond timestamp. You can pause a 3-hour audiobook in *Queue 4* at `01:42:19`, switch to *Queue 1* to listen to music all afternoon, and switch back to *Queue 4* at night to resume at `01:42:19` with zero friction.

### F-A04: Transparent History-Aware Shuffle
- **What It Does**: Unlike basic players that hide the shuffle order and accidentally repeat songs, toggling Shuffle in **Photos** uses a history-aware Fisher-Yates algorithm and **visibly reorders the "Up Next" queue** so you can see exactly what will play next—and drag-and-drop to tweak the shuffled order.

### F-A05: Native Lossless & Hi-Res Codec Engine
- **What It Does**: Decodes both audiophile lossless and everyday compressed formats completely offline:
  - **Lossless & Uncompressed**: `FLAC` (16-bit / 24-bit / 32-bit up to `192kHz`), `WAV` (Uncompressed PCM & 32-bit IEEE Float), `ALAC` (Apple Lossless in `.m4a`), `AIFF`, `APE`.
  - **Lossy & High-Efficiency**: `MP3` (up to `320kbps` CBR & VBR), `AAC` / `M4A`, `OPUS`, `OGG Vorbis`, `WMA`.

### F-A06: Live Audio Quality Badge & Signal Path Inspector
- **What It Does**: Renders an interactive pill badge directly on the player screen classifying the audio stream:
  - `HI-RES LOSSLESS • 24-bit / 96kHz FLAC • 3120 kbps`
  - `LOSSLESS • 16-bit / 44.1kHz WAV • 1411 kbps`
  - `HIGH QUALITY • 320 kbps MP3`
  - `STANDARD • 128 kbps AAC`
- **Signal Path Popup (`ConnectedInspector`)**: Tapping the badge opens a live technical diagram showing every stage the audio passes through:
  1. **Source Container & Codec** (e.g., `FLAC 24-bit / 96.0 kHz Stereo`)
  2. **Decoder Engine** (`Media3 / Internal PCM Float32 Pipeline`)
  3. **DSP Stage** (`10-Band EQ Active` or `Bit-Perfect Bypass`)
  4. **Hardware Output Sink** (`Internal Speaker`, `Wired 3.5mm / USB-C DAC`, or `Bluetooth A2DP`).

### F-A07: Playback Audio Quality / Output Mode Switcher
- **What It Does**: Allows the user to switch how the audio engine processes sound depending on their listening scenario:
  1. **Hi-Res Bit-Perfect Direct Mode**: Bypasses all EQ, resampling, and spatial filters to deliver pure, unaltered PCM samples to wired IEMs or external USB-C DACs.
  2. **Audiophile 32-Bit Float DSP Mode**: Runs the full 10-band EQ, crossfeed, and spatial virtualizer with internal headroom limiting to prevent digital clipping.
  3. **Voice / Lecture Clarity Mode**: Applies a high-pass rumble filter (`< 80Hz`), boosts vocal presence (`1kHz–4kHz`), enables dynamic compression, and optionally downmixes to Mono.

### F-A08: Multi-Quality Audio Converter & Exporter
- **What It Does**: Lets users convert between audio quality tiers on-device (or choose the exact quality when extracting audio from a video):
  - **Tier 1 — Lossless Original**: Direct stream copy or uncompressed `WAV` (`1411+ kbps`).
  - **Tier 2 — Studio High (`320 kbps` AAC/M4A)**: Transparent archival quality at 25% of lossless file size.
  - **Tier 3 — Balanced Standard (`192 kbps` AAC/M4A)**: Everyday portable listening quality.
  - **Tier 4 — Compact Voice (`96 kbps` Mono/Stereo M4A)**: Shrinks 1-hour lectures or podcasts to ~40 MB.

### F-A09: True Gapless Playback & Configurable Crossfade (`0s–12s`)
- **What It Does**: Pre-buffers the next track in the queue so live concert albums and concept albums (e.g., *Dark Side of the Moon*, DJ mixes) transition with **0.0ms of silence**, plus an optional `1s–12s` equal-power volume crossfade curve for party/playlist shuffling.

### F-A10: Independent Pitch & Tempo Shifter (Time-Stretch + Semitone Pitch)
- **What It Does**: Decouples playback speed from musical pitch:
  - **Speed / Tempo (`0.25x` to `4.0x` in `0.05x` steps)**: Speed up lectures or slow down fast instrumentals while keeping voices natural.
  - **Pitch Shift (`-12` to `+12` semitones + `432Hz` Detune Toggle)**: Shift the key of a song for singing/instrument practice or enable the popular `432Hz` reference pitch mode.

### F-A11: Skip Silence (Smart Dead-Air Remover)
- **What It Does**: Uses real-time RMS amplitude detection (`SilenceSkippingAudioProcessor`) to automatically skip over silent pauses in lectures, voice memos, and interviews—with a live counter showing **"Time Saved: 14m 22s"**.

### F-A12: ReplayGain & Smart Loudness Normalization
- **What It Does**: Reads embedded `REPLAYGAIN_TRACK_GAIN` / `REPLAYGAIN_ALBUM_GAIN` tags (and computes peak RMS fallback) so jumping from a quiet 1980s master to a loud modern track never blasts your ears.

### F-A13: Headphone Crossfeed DSP & L/R Channel Balance
- **What It Does**:
  - **Crossfeed (`0%–100%` blend)**: Blends a delayed, frequency-shaped portion of the Left channel into the Right channel (and vice versa) to simulate listening to studio monitors in a room—eliminating ear fatigue on hard-panned stereo tracks.
  - **L/R Balance & Mono Toggle**: Fine-tune Left/Right volume balance (`-100% L` to `+100% R`) to compensate for asymmetrical hearing or single-earbud use.

### F-A14: Real-Time 60fps FFT Spectrum, Oscilloscope & Fake-Lossless Spectrogram
- **What It Does**: Renders three selectable 60fps hardware-accelerated visualizers on the player and EQ screens:
  1. **60-Bar Electric Violet FFT Spectrum** (with peak-hold dots).
  2. **Time-Domain Oscilloscope Waveform**.
  3. **Scrolling Frequency Waterfall Spectrogram (`0Hz–24kHz`)**: Allows audiophiles to visually inspect whether a file is true lossless (frequencies extending cleanly above `20kHz`) or a transcode fake (hard shelf cutoff at `16kHz`).

### F-A15: Sleep Timer with Gentle Fade-Out & "Shake to Extend"
- **What It Does**: Set a timer for `15m`, `30m`, `45m`, `60m`, `90m`, or **"Finish Current Track / Episode"**. During the final 30 seconds, volume smoothly ramps down to `0%`. Optional accelerometer toggle lets you gently shake the phone in the dark to add `+15 minutes` without opening your eyes.

### F-A16: Synchronized `.LRC` Lyrics & Tap-to-Seek Lines
- **What It Does**: Reads embedded `USLT`/`SYLT` ID3 tags and local `.lrc` sidecar files. Auto-scrolls time-synced lyrics highlighted in `#F8FAFC` over `#94A3B8` muted lines; tapping any lyric line jumps playback to that exact timestamp.

### F-A17: CUE Sheet Virtual Track Splitter & Timestamp Bookmarks
- **What It Does**:
  - Parses `.cue` files paired with single-file `.flac`/`.wav` album rips and splits them into individual virtual tracks in the library with zero file duplication.
  - Lets users drop named **Timestamp Bookmarks** (e.g., `00:44:12 - Best Drop` or `01:12:05 - Exam Formula`) inside any audio file.

### F-A18: Built-In Offline Audio Tag & Cover Art Editor
- **What It Does**: Edit `Title`, `Artist`, `Album`, `Album Artist`, `Track #`, `Year`, `Genre`, `Lyrics`, and embedded `Cover Art` (pick from gallery or crop) directly on local `MP3`, `FLAC`, `M4A`, and `OGG` files.

---

## 3. Pillar 2 — 10-Band EQ & Pro Audio FX Studio (6 Features)
*Benchmark References: Poweramp Equalizer, Wavelet, Neutron.*

### F-E01: 10-Band Graphic Equalizer (`31Hz` to `16kHz`)
- **What It Does**: 10 precision vertical/horizontal `AppSlider` bands (`31Hz`, `62Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz`) with `-15.0 dB` to `+15.0 dB` range, magnetic `0.0 dB` center snap, and a live spline curve graph showing the composite frequency response.

### F-E02: Parametric EQ Mode & AutoEq Headphone Profile Importer
- **What It Does**: Switch from standard 10-Band Graphic mode to **Parametric Mode** (customizable Center Frequency `20Hz–20kHz`, Gain `-15dB..+15dB`, Filter Type `Peaking / Low-Shelf / High-Shelf`, and `Q-Factor`), plus 1-tap import of standard Wavelet/AutoEq `.txt` headphone calibration curves.

### F-E03: Preamp, Sub-Bass Boost & 3D Spatial Virtualizer
- **What It Does**:
  - **Preamp Gain (`-15dB` to `+15dB`)**: Compensates for positive EQ boosts to prevent clipping.
  - **Sub-Bass Punch (`0%–100%`)**: Enhances low-end warmth (`40Hz–90Hz`).
  - **Spatial Virtualizer (`0%–100%`)**: Expands stereo soundstage width.

### F-E04: Loudness Enhancer & Night-Mode Dynamic Compressor
- **What It Does**: Boosts quiet audio recordings, whispered movie dialogue, or unamplified lectures up to **+15.0 dB** (`LoudnessEnhancer`) while clamping sudden loud peaks.

### F-E05: Per-Device Automatic Preset Switching
- **What It Does**: Automatically detects the active audio route and loads your preferred preset for:
  - **Phone Built-In Speakers**
  - **Wired Headphones / USB-C DAC**
  - **Bluetooth Earbuds / Car Audio**

### F-E06: Minimize-to-Pill Live Tuning (`AppBottomSheet`)
- **What It Does**: Collapse the EQ Studio into a floating bottom pill (`[EQ: Bass Boost • +4.5dB]`) so you can browse photos, read notes, or scrub videos while toggling EQ on/off with 1 tap.

---

## 4. Pillar 3 — "Comfy Netflix-Style" Theater Video Player (20 Features)
*Benchmark References: Netflix Mobile, Next Player, MPV-Android, PotPlayer, Infuse, IINA.*

### F-V01: Netflix-Style "Continue Watching" Home Carousel
- **What It Does**: Displays a top horizontal shelf of partially watched videos with wide 16:9 frame thumbnails, electric violet progress bars, remaining time readouts (`18m left`), and 1-tap instant resume.

### F-V02: Smart Episodic Folder Grouping ("Season / Series" Intelligence)
- **What It Does**: Automatically parses episode patterns (`S01E01`, `E02`, `Ep 3`, `Part 4`, `Lecture 05`) within folders and builds an in-player **"Episodes" Drawer** showing thumbnails, durations, watched checkmarks, and synopses.

### F-V03: Auto-Play Next Episode with 5-Second Countdown Card
- **What It Does**: As a video reaches its final 5 seconds, a sleek floating card slides into the bottom-right corner with the next episode's thumbnail and a `Next Episode in 5s... [Play Now] [Cancel]` button.

### F-V04: "+85s Skip Intro" Pill & Per-Folder Auto-Skip Intro/Outro Rules
- **What It Does**:
  - Provides a 1-tap **`+85s Skip Intro`** button on the player HUD (customizable duration).
  - Lets users set a **Per-Folder Skip Rule** (e.g., *"Automatically start at `01:30` and skip to next episode at `22:15` for all videos in this folder"*), mimicking Netflix's *Skip Intro* & *Skip Credits* completely offline!

### F-V05: Bedtime Screen Touch Lock (1-Tap Accidental Touch Shield)
- **What It Does**: Tapping the **Lock icon** hides all player UI and disables all gestures/touches so you can hold the phone in bed or rest it on a pillow without accidental seeks or exits. Tapping anywhere reveals only a subtle, dimmed `[Tap Twice to Unlock]` pill.

### F-V06: Press-and-Hold Anywhere for Instant `2.0x` Speed
- **What It Does**: Holding a finger anywhere on the video canvas immediately engages `2.0x` speed (configurable to `1.5x`, `2.0x`, or `3.0x`) with a minimal top badge (`2.0x Speed >>`); releasing restores your previous speed (`1.0x`).

### F-V07: Extra-Dim Night Filter (`0%` down to `-80%` Below Hardware Minimum)
- **What It Does**: Swiping down on the left-side brightness zone reduces hardware backlight to `0%`, and continuing to swipe down smoothly applies a **Software Night Dimmer Overlay (`-1%` to `-80%`)**—preventing blinding bright scenes when watching in a pitch-black bedroom.

### F-V08: Dual-Zone Vertical Gestures + `200%` Software Volume Boost
- **What It Does**:
  - **Left Vertical Swipe**: Brightness (`100%` to `-80%` Extra Dim).
  - **Right Vertical Swipe**: Volume (`0%` to `100%` system volume, then `101%` to `200%` Software Preamp Boost highlighted in `#7A5AF8`).
  - **Horizontal Swipe**: Smooth frame-accurate time scrubbing with live delta readout (`+00:42 [14:20]`).

### F-V09: Double-Tap Edge Ripple Seek (`-10s` / `+10s` Accumulator)
- **What It Does**: Double-tapping the left or right third of the screen skips backward/forward by `10s` (customizable: `5s`, `10s`, `15s`, `30s`), accumulating consecutive taps (`+10s` $\rightarrow$ `+20s` $\rightarrow$ `+30s`) with a spring scale-pop animation.

### F-V10: Ambient Cinema Glow (Real-Time Ambilight Canvas Bleed)
- **What It Does**: Samples the edge colors of the current video frame at low overhead and casts a soft, diffused radial glow onto the `#0D1117` letterbox/pillarbox bars for a cozy home-theater feel.

### F-V11: Live Filmstrip Thumbnail Scrubbing & Embedded Chapter Markers
- **What It Does**: Dragging the timeline seek bar pops up a live frame preview box above your finger and displays tick marks for embedded `MKV`/`MP4` chapters with chapter title snapping.

### F-V12: Dual Subtitles (Simultaneous Top & Bottom Tracks)
- **What It Does**: Allows loading **two subtitle tracks at the same time** (e.g., *Primary English* at the bottom + *Secondary Japanese / Spanish* at the top)—a game-changer for language learners and international cinema fans.

### F-V13: Interactive Clickable Subtitle Transcript Sidebar (PotPlayer-Style)
- **What It Does**: Opens a side/bottom drawer listing every subtitle line in the video with timestamps and a live search bar. Type any word or phrase spoken in the movie or lecture, see every matching line, and **tap any line to jump the video to that exact second!**

### F-V14: Pro Subtitle Styling & Sync Offset Studio
- **What It Does**: Full control over `.SRT`, `.VTT`, and `.ASS`/`.SSA` subtitles:
  - Font size (`12sp`–`32sp`), font weight, text color, outline stroke, and background pill opacity (`0%–100%`).
  - Vertical position slider (move subtitles into the black letterbox bar so they never cover the video!).
  - **Subtitle Delay / Sync Offset (`-50ms` / `+50ms` step buttons)** to fix out-of-sync external subtitles on the fly.

### F-V15: Multi-Audio Track Switcher & Audio Delay Sync
- **What It Does**: Switch between all embedded audio streams (`5.1 Surround`, `Stereo`, different language dubs, commentary tracks) and adjust **Audio Sync Offset (`-50ms` / `+50ms`)** to fix Bluetooth headphone latency!

### F-V16: 800% Pinch-to-Zoom, Pan Lock & Aspect Ratio Presets
- **What It Does**: Pinch with two fingers to zoom into any corner of a live playing video up to `800%` (e.g., reading small code or whiteboard writing in a recorded lecture), lock the pan position, or cycle aspect modes (`Fit`, `Fill Crop`, `Stretch`, `16:9`, `4:3`, `21:9`).

### F-V17: Single-Frame Stepping (`-1f` / `+1f`) & Video Mirror/Flip
- **What It Does**: Step forward or backward **one exact video frame at a time** while paused, plus a **Horizontal Mirror Flip** toggle (essential for learning dance routines, guitar fingerings, or workouts from videos!).

### F-V18: Seamless Picture-in-Picture (PiP) & Video-to-Background-Audio Handoff
- **What It Does**:
  - **PiP Mode**: shrinks video into a draggable system floating window when multitasking.
  - **1-Tap Background Audio Mode (Headphones Icon)**: Detaches the video surface and plays the video's audio stream in the background (with lockscreen controls, Mini-Player pill, and 10-band EQ) so you can listen to video podcasts, music videos, or lectures with your screen turned off!

### F-V19: Scene Bookmarks with Visual Thumbnails
- **What It Does**: Drop named visual bookmarks at key moments in a movie or lecture (`[00:28:14 - Key Diagram]`) and jump between them from the Bookmarks drawer.

### F-V20: 1-Tap Video Storyboard / Contact Sheet Generator
- **What It Does**: Generates a high-resolution `4x4` or `5x5` grid image (`PNG`) of evenly spaced thumbnails across an entire video with duration, resolution, codec header, and timestamps printed on each tile.

---

## 5. Pillar 4 — Non-Destructive Media Cutter, Trimmer & Studio (9 Features)
*Benchmark References: LosslessCut, InShot Pro, FFmpeg Stream Muxers.*

### F-S01: Lossless Fast-Cut Video & Audio Trimmer (`MediaExtractor` + `MediaMuxer`)
- **What It Does**: Cuts `MP4`, `MOV`, `3GP`, `MP3`, `M4A`, `AAC`, and `WAV` files between custom `Start` and `End` handles using **direct packet stream copying (zero re-encoding)**. Trimming a 500 MB video takes less than 1 second and preserves **100% of original video/audio bitrate and quality**.

### F-S02: Dual-Handle Zoomable Filmstrip & Waveform Timeline
- **What It Does**: Renders a combined video frame filmstrip + audio peak waveform with pinch-to-zoom timeline magnification, A–B preview looping of the cut region, and `-100ms` / `+100ms` precision nudge buttons.

### F-S03: "Trim Outside" vs. "Cut Middle & Join" Modes
- **What It Does**:
  - **Keep Selection (Trim Outside)**: Exports only the highlighted range `[Start .. End]`.
  - **Remove Selection (Cut Middle)**: Cuts out an unwanted middle section (like an ad break or mistake) and joins `[0 .. Start]` + `[End .. Duration]` seamlessly.

### F-S04: Multi-Segment Highlight Splitter
- **What It Does**: Mark multiple `[Start, End]` ranges inside a long recording and batch-export all segments as numbered clips in one tap.

### F-S05: 1-Tap Video-to-Audio Extractor (Multi-Quality Tiers)
- **What It Does**: Extracts the audio stream from any video into `/Music/Photos/` with selectable quality:
  - **Lossless Stream Copy (`.m4a` / `.aac` — instant, 0% quality loss)**
  - **Uncompressed Studio `.wav` (`1411 kbps`)**
  - **High Quality `.m4a` (`320 kbps`)**
  - **Balanced `.m4a` (`192 kbps`)**
  - **Compact Voice `.m4a` (`96 kbps`)**

### F-S06: Audio Fade-In / Fade-Out & Volume Gain Export
- **What It Does**: When trimming an audio track (e.g., for a custom ringtone, alarm, or intro clip), optionally apply a smooth `1s–5s` Fade-In / Fade-Out envelope and normalize peak volume.

### F-S07: Native-Resolution Video Frame Grabber (`PNG` / `JPEG`)
- **What It Does**: Step to any exact video frame and export a full-resolution (`4K`/`1080p`), un-watermarked `PNG` (lossless) or `JPEG` snapshot directly into your photo gallery without screen UI overlays.

### F-S08: Video-to-Animated-GIF & Boomerang Maker
- **What It Does**: Convert any `1s–15s` video range into a smooth, loopable Animated `GIF` with selectable FPS (`10 / 15 / 24 / 30 fps`), resolution scale, and forward/reverse Boomerang loop option.

### F-S09: Voice-Over Photo Note Attachment
- **What It Does**: Attach or record an audio clip paired with any photo (creating a playable "Voice Photo" card that plays its audio memory when viewed).

---

## 6. Pillar 5 — Power-User Gallery, Pro Photo Culling, EXIF & Vault (12 Features)
*Benchmark References: Aves Gallery, Fossify Gallery, FastStone Image Viewer, FastRawViewer.*

### F-G01: Unified Multi-Format Timeline & Smart Filter Chips
- **What It Does**: Browses Photos (`JPEG`, `PNG`, `WebP`, `HEIC`, `AVIF`, `BMP`, `SVG`), Pro `RAW` (`DNG`, `CR2`, `NEF`, `ARW`), Animated (`GIF`, `Animated WebP`), Videos, and Audio in one blazing-fast grid or list with crisp format/quality badges (`RAW`, `4K 60`, `FLAC`, `24-BIT`, `GIF`).

### F-G02: Synchronized 2-Up & 4-Up Photo Compare Mode (FastStone-Style)
- **What It Does**: Select 2, 3, or 4 similar photos (e.g., burst shots or edits) and open **Split-Screen Compare Mode**. Pinching to zoom or panning on one image **simultaneously zooms and pans all compared images to the exact same coordinate**, making it effortless to pick the sharpest shot and delete the rest!

### F-G03: 1-Tap Floating Magnifier Loupe (`200%–400%` Lens)
- **What It Does**: Toggle the **Loupe Tool** (or hold with one finger) to summon a high-magnification circular/square lens over any photo to inspect fine focus details without losing your full-frame composition view.

### F-G04: Focus Peaking & Exposure Zebra Stripes Overlay
- **What It Does**:
  - **Focus Peaking**: Highlights razor-sharp in-focus edges in `#7A5AF8` electric violet using real-time Sobel edge detection so photographers can instantly verify critical focus.
  - **Zebra Stripes**: Highlights blown-out overexposed highlights (`>= 252`) and crushed shadows (`<= 3`).

### F-G05: Animated GIF / WebP Frame Scrubber & Speed Controller
- **What It Does**: Unlike stock galleries that only play GIFs passively, **Photos** lets you pause any GIF/Animated WebP, scrub through it frame-by-frame (`Frame 14 / 48`), change its playback speed (`0.25x–4.0x`), or extract individual frames as PNGs.

### F-G06: Live 256-Bin RGB + Luminance Histogram
- **What It Does**: Renders a real-time overlaid or sheet-mounted R, G, B, and Luminance exposure histogram for any photo or video frame.

### F-G07: Deep EXIF, IPTC & Video/Audio Codec Stream Inspector
- **What It Does**: Displays every technical detail of a file:
  - **Photo**: Camera model, Lens, ISO, Shutter Speed, Aperture, Focal Length, 35mm equivalent, Color Space, File Hash (`SHA-256`), and GPS coordinates.
  - **Video/Audio**: Container format, Video Codec profile/level, Bitrate, Frame Rate, Color Range/HDR transfer, Audio Codec, Sample Rate, Bit Depth, and Channel Layout.

### F-G08: 1-Tap EXIF Privacy Scrubber & Date/Time Batch Shifter
- **What It Does**:
  - **EXIF Privacy Scrubber**: Strips GPS coordinates, camera serial numbers, and software tags before sharing or exporting.
  - **Batch EXIF Date Fixer**: Shift or repair incorrect `Date Taken` timestamps across multiple files at once.

### F-G09: Batch Image Converter, Resizer & Token Renamer
- **What It Does**:
  - Batch-convert selected images (`HEIC` / `AVIF` / `RAW` / `WebP` $\rightarrow$ `JPEG` / `PNG` / `WebP`) with quality & dimension scaling.
  - Batch-rename files using customizable token templates: `{date}_{time}_{folder}_{width}x{height}_{counter}`.

### F-G10: Built-In Color Palette Swatch Extractor
- **What It Does**: Extracts the 6 dominant hex colors (`#HEX` + `RGB` + `HSL`) from any photograph so designers and artists can copy palette codes with 1 tap.

### F-G11: PIN-Locked Private Vault (Encrypted Local Sandbox)
- **What It Does**: Move sensitive photos, videos, or audio files into a PIN-protected app-private Vault directory hidden from Android `MediaStore` and external apps.

### F-G12: 30-Day Recycle Bin & Duplicate / Large File Finder
- **What It Does**:
  - Safe Trash bin holding deleted items for 30 days before permanent removal.
  - Storage Analyzer that identifies exact duplicate files (via size + fast hash) and top space-consuming media.

---

## 7. Phased Version Roadmap (`v0.1.0` $\rightarrow$ `v1.0.0` Stable)

| Target Version | Phase File | Features Shipped | Key Deliverables |
| :--- | :--- | :--- | :--- |
| **`v0.1.0`** *(Verified)* | `Phase1_BoilerplateAndArchitecture.md` | Foundation & Design System | Gradle toolchain, `Rules.md`, `Android.md`, `ElectricSlateTokens`, `SpringPhysics`, 12 `ui.components.presets`, automated `VerifyAndroidRulesTask`. |
| **`v0.2.0`** | `Phase2_UniversalMediaScannerAndSmartGallery.md` | `F-G01`, `F-G02`, `F-G03`, `F-G04`, `F-G05` | Offline `MediaRepository` (Photos, RAW, GIF, Video, Audio), format/quality badges, **2-Up/4-Up Synchronized Photo Compare**, **Magnifier Loupe**, **Focus Peaking/Zebras**, **GIF Frame Scrubber**, and built-in Sample Media Generator. |
| **`v0.3.0`** | `Phase3_AudiophileAudioPlayerQueueAndLossless.md` | `F-A01` through `F-A18` | `PhotosMediaPlaybackService`, Mini-Player Pill, **4-State Loop (`Off`/`Queue`/`Track`/`A-B`)**, **Drag-and-Drop Up Next Queue**, **5 Multi-Queues with Position Memory**, **Lossless/Hi-Res Signal Path & Output Mode Selector**, Gapless, Crossfade, Pitch/Tempo, Skip Silence, ReplayGain, Crossfeed, Sleep Timer, `.LRC` Lyrics, CUE Splitter, and Tag Editor. |
| **`v0.4.0`** | `Phase4_TenBandEqDspAndSpectrumStudio.md` | `F-E01` through `F-E06`, `F-A14` | **10-Band Graphic EQ + Parametric EQ**, AutoEq profile importer, Preamp, Bass Boost, Spatial Virtualizer, `+15dB` Loudness Enhancer, Per-Device Auto-Presets, and **60fps FFT / Oscilloscope / Fake-Lossless Waterfall Spectrogram**. |
| **`v0.5.0`** | `Phase5_ComfyNetflixStyleTheaterVideoPlayer.md` | `F-V01` through `F-V20` | **Continue Watching Shelf**, Episodic Folder Grouping, **Auto-Play Next Episode (5s Countdown)**, **`+85s` Skip Intro & Folder Auto-Skip**, **Bedtime Touch Lock**, **Hold-for-`2.0x` Speed**, **Extra-Dim (`-80%`) & `200%` Volume Gestures**, Ambient Glow, Chapters, **Dual Subtitles**, **Interactive Clickable Subtitle Transcript**, 800% Zoom, Frame Step, Mirror Flip, PiP, Background Audio Handoff, Scene Bookmarks, and Video Storyboard Generator. |
| **`v0.6.0`** | `Phase6_NonDestructiveTrimmerAndQualityStudio.md` | `F-S01` through `F-S09`, `F-A08` | **Lossless Fast-Cut Trimmer (`MediaMuxer`)**, Trim Outside / Cut Middle, Multi-Segment Splitter, **Video-to-Audio Extractor & Multi-Quality Converter (`Lossless`, `320k`, `192k`, `96k`)**, Fade-In/Out, **Native Frame Grabber (`PNG`/`JPEG`)**, Video-to-GIF Maker, and Voice-Over Photo Notes. |
| **`v0.7.0`** | `Phase7_DeepExifCodecInspectorAndPrivateVault.md` | `F-G06` through `F-G12` | **Live 256-Bin RGB Histogram**, Deep EXIF & Codec Stream Inspector, **1-Tap EXIF Privacy Scrubber**, Batch Converter & Token Renamer, Color Palette Extractor, **PIN-Locked Private Vault**, and 30-Day Recycle Bin + Duplicate Finder. |
| **`v1.0.0`** | `Phase8_ProductionHardeningAnd100Release.md` | Full Polish & Release | 60fps startup & scroll optimization, full unit/instrumentation test coverage, release banner (`PhotosBanner_1.0_<YYYY-MM-DD>.png`), `Changelog_1.0.md`, and signed release APK (`Photos_1.0.0_Android.apk`). |
