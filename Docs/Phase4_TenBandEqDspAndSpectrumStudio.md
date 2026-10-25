# Phase 4 - TenBandEqDspAndSpectrumStudio

## Goal
Implement the 10-Band Graphic Equalizer, Parametric EQ Mode, AutoEq Headphone Profile Importer, Preamp, Sub-Bass Boost, Spatial Virtualizer, `+15dB` Loudness Enhancer, Per-Device Auto-Presets, and 60fps FFT Spectrum / Oscilloscope / Fake-Lossless Waterfall Spectrogram.

## Scope
- `AudioDspController` binding to `audioSessionId` using `DynamicsProcessing` (10-band on API 28+) with 5-band `Equalizer` fallback, `BassBoost`, `Virtualizer`, and `LoudnessEnhancer`.
- Parametric EQ filter calculator (Frequency `20Hz–20kHz`, Gain `-15dB..+15dB`, `Q-Factor`, Filter Type) and Wavelet/AutoEq `.txt` parser.
- Per-device automatic preset switching (`Speaker`, `Wired Headphones / USB DAC`, `Bluetooth`).
- 60fps real-time `FftSpectrumVisualizer` supporting 3 modes: `FFT Bars`, `Oscilloscope Waveform`, and `Frequency Waterfall Spectrogram (0–24kHz)`.

## Out Of Scope
- Root-level system-wide audio hooking outside the app's media session.

## Features
### 10-Band Graphic & Parametric Equalizer (`F-E01`, `F-E02`)
Tunes `31Hz`, `62Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz` (`-15dB` to `+15dB`) plus Parametric Q-factor bands and AutoEq imports.

### Preamp, Bass, Virtualizer & +15dB Loudness (`F-E03`, `F-E04`, `F-E05`, `F-E06`)
Provides dedicated DSP sliders, automatic output-device preset switching, and minimize-to-pill `AppBottomSheet` live tuning.

### 60fps FFT, Oscilloscope & Waterfall Spectrogram (`F-A14`)
Renders real-time spectral analysis in `PrimaryAccent` (`#7A5AF8`) to visualize frequencies and verify true lossless audio.

## UI Mockup
```
+--------------------------------------------------+
| 10-Band EQ & DSP Studio        [Minimize] [Close]| (AppBottomSheet)
+--------------------------------------------------+
| [FFT Bars] [Oscilloscope] [Spectrogram 24kHz]    |
| +----------------------------------------------+ |
| |   ||  ||||  ||||||  ||||  ||  . . .          | | (60fps Canvas)
| +----------------------------------------------+ |
| [Flat] [Bass Boost] [Vocal] [Cinema] [LateNight] | (AppPillChip)
| 31Hz..16kHz Sliders + Preamp / Bass / Loudness   | (AppSlider)
+--------------------------------------------------+
```

## Logic Notes
- Uses `ElectricSlateTokens` exclusively (`AR-001`, `AR-005`).

## Risks
- Risk: Hardware audiofx unsupported on some emulators. Mitigation: Graceful try-catch wrapper around `DynamicsProcessing` / `Equalizer` maintaining UI state and software simulation fallback.

## Dependencies
- Phase 3 playback service.

## Acceptance Criteria
- 10-band EQ, Parametric bands, AutoEq parser, device presets, and 60fps visualizers pass unit tests and `verifyAndroidRules`.
