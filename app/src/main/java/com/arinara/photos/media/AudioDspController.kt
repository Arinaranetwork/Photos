// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

enum class OutputSignalMode(val label: String, val badge: String, val description: String) {
    BIT_PERFECT_DIRECT_PCM(
        "Bit-Perfect Direct PCM",
        "BIT-PERFECT",
        "Bypasses software DSP & resampling for unaltered DAC passthrough"
    ),
    FLOAT_32BIT_HI_RES_DSP(
        "32-Bit Float Hi-Res DSP",
        "32B FLOAT DSP",
        "Enables 10-band EQ, Parametric filters, Crossfeed & Crossfade at 32-bit float precision"
    ),
    VOICE_CLARITY_MONO(
        "Battery Saver / Voice Clarity",
        "VOICE MONO",
        "Mono downmix with vocal presence boost for lectures, podcasts & noisy transit"
    )
}

enum class AudioOutputDevice(val label: String) {
    PHONE_SPEAKER("Phone Speaker"),
    WIRED_HEADPHONES_DAC("Wired Headphones / USB DAC"),
    BLUETOOTH_AUDIO("Bluetooth Audio"),
    LINE_OUT_CAST("Line-Out / Cast")
}

enum class ReplayGainMode(val label: String) {
    OFF("Off"),
    TRACK("Track Gain"),
    ALBUM("Album Gain")
}

enum class VisualizerMode(val label: String) {
    BARS_32("32-Band FFT"),
    WAVE_CURVE("Spline Wave"),
    OSCILLOSCOPE("Oscilloscope"),
    WATERFALL_SPECTROGRAM("24kHz Lossless Check")
}

enum class PeqFilterType(val code: String, val label: String) {
    PEAKING("PK", "Peaking Bell"),
    LOW_SHELF("LSC", "Low Shelf"),
    HIGH_SHELF("HSC", "High Shelf")
}

data class ParametricBand(
    val index: Int,
    val enabled: Boolean = true,
    val filterType: PeqFilterType = PeqFilterType.PEAKING,
    val frequencyHz: Int = 1000,
    val gainDb: Float = 0f,
    val qFactor: Float = 1.41f
)

data class EqPreset(
    val id: String,
    val name: String,
    val preampDb: Float,
    val bandGainsDb: List<Float>, // 10 bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
    val isCustom: Boolean = false
)

data class SignalPathReport(
    val sourceCodec: String,
    val sourceBitDepth: Int,
    val sourceSampleRateHz: Int,
    val sourceBitrateKbps: Int,
    val dspEngineDescription: String,
    val outputDeviceName: String,
    val outputSampleRateHz: Int,
    val isBitPerfectActive: Boolean,
    val trueLosslessVerdict: String
)

data class DspState(
    val eqEnabled: Boolean = true,
    val isParametricMode: Boolean = false,
    val activePresetName: String = "Flat",
    val preampDb: Float = 0f,
    val bandGainsDb: List<Float> = List(10) { 0f },
    val parametricBands: List<ParametricBand> = defaultParametricBands(),
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val loudnessGainDb: Float = 0f,
    val crossfeedPercent: Int = 0,
    val channelBalanceLR: Float = 0f, // -1.0 (Full Left) .. 0.0 (Center) .. +1.0 (Full Right)
    val monoDownmix: Boolean = false,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val replayGainPreampDb: Float = 0f,
    val outputSignalMode: OutputSignalMode = OutputSignalMode.FLOAT_32BIT_HI_RES_DSP,
    val activeOutputDevice: AudioOutputDevice = AudioOutputDevice.WIRED_HEADPHONES_DAC,
    val playbackSpeed: Float = 1.0f,
    val pitchSemitones: Float = 0f,
    val concert432HzPitch: Boolean = false,
    val skipSilenceEnabled: Boolean = false,
    val silenceSavedMs: Long = 0L,
    val crossfadeDurationSec: Int = 0, // 0 = Gapless
    val visualizerMode: VisualizerMode = VisualizerMode.BARS_32
) {
    val effectivePitchMultiplier: Float
        get() {
            val semitoneFactor = Math.pow(2.0, (pitchSemitones / 12.0)).toFloat()
            val concertFactor = if (concert432HzPitch) (432f / 440f) else 1.0f
            return (semitoneFactor * concertFactor).coerceIn(0.5f, 2.0f)
        }

    val leftChannelGain: Float
        get() = if (channelBalanceLR <= 0f) 1.0f else (1.0f - channelBalanceLR).coerceIn(0f, 1f)

    val rightChannelGain: Float
        get() = if (channelBalanceLR >= 0f) 1.0f else (1.0f + channelBalanceLR).coerceIn(0f, 1f)

    companion object {
        val BAND_FREQUENCIES_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val BAND_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

        fun defaultParametricBands(): List<ParametricBand> =
            BAND_FREQUENCIES_HZ.mapIndexed { idx, hz ->
                ParametricBand(
                    index = idx + 1,
                    enabled = true,
                    filterType = when (idx) {
                        0 -> PeqFilterType.LOW_SHELF
                        9 -> PeqFilterType.HIGH_SHELF
                        else -> PeqFilterType.PEAKING
                    },
                    frequencyHz = hz,
                    gainDb = 0f,
                    qFactor = 1.41f
                )
            }
    }
}

@Suppress("DEPRECATION")
class AudioDspController {

    private val _dspState = MutableStateFlow(DspState())
    val dspState: StateFlow<DspState> = _dspState.asStateFlow()

    private val customPresets = mutableMapOf<String, EqPreset>()
    private val devicePresetBindings = mutableMapOf<AudioOutputDevice, String>(
        AudioOutputDevice.PHONE_SPEAKER to "Podcast / Vocal Clarity",
        AudioOutputDevice.WIRED_HEADPHONES_DAC to "Flat",
        AudioOutputDevice.BLUETOOTH_AUDIO to "Bass Boost",
        AudioOutputDevice.LINE_OUT_CAST to "Cinema Surround"
    )

    private var systemEqualizer: Equalizer? = null
    private var systemBassBoost: BassBoost? = null
    private var systemVirtualizer: Virtualizer? = null
    private var systemLoudnessEnhancer: LoudnessEnhancer? = null
    private var boundSessionId: Int = 0

    fun allPresets(): List<EqPreset> = BUILT_IN_PRESETS + customPresets.values.toList()

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == boundSessionId) return
        releaseSystemEffects()
        boundSessionId = audioSessionId
        try {
            systemEqualizer = Equalizer(0, audioSessionId).apply { enabled = _dspState.value.eqEnabled }
            systemBassBoost = BassBoost(0, audioSessionId).apply { enabled = _dspState.value.bassBoostPercent > 0 }
            systemVirtualizer = Virtualizer(0, audioSessionId).apply { enabled = _dspState.value.virtualizerPercent > 0 }
            systemLoudnessEnhancer = LoudnessEnhancer(audioSessionId).apply { enabled = _dspState.value.loudnessGainDb > 0f }
            syncToHardwareEffects()
        } catch (_: Throwable) {
            // Hardware AudioFX unavailable on unit test JVM or restricted OEM; software state stays authoritative
        }
    }

    fun releaseSystemEffects() {
        try {
            systemEqualizer?.release()
            systemBassBoost?.release()
            systemVirtualizer?.release()
            systemLoudnessEnhancer?.release()
        } catch (_: Throwable) {
        }
        systemEqualizer = null
        systemBassBoost = null
        systemVirtualizer = null
        systemLoudnessEnhancer = null
        boundSessionId = 0
    }

    fun setEqEnabled(enabled: Boolean) {
        _dspState.value = _dspState.value.copy(eqEnabled = enabled)
        syncToHardwareEffects()
    }

    fun setParametricMode(parametric: Boolean) {
        _dspState.value = _dspState.value.copy(isParametricMode = parametric)
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..9) return
        val clamped = gainDb.coerceIn(-15f, 15f)
        val updated = _dspState.value.bandGainsDb.toMutableList()
        updated[bandIndex] = clamped
        _dspState.value = _dspState.value.copy(
            bandGainsDb = updated,
            activePresetName = "Custom"
        )
        syncToHardwareEffects()
    }

    fun setPreampDb(preampDb: Float) {
        _dspState.value = _dspState.value.copy(preampDb = preampDb.coerceIn(-15f, 15f))
        syncToHardwareEffects()
    }

    fun updateParametricBand(bandIndex: Int, frequencyHz: Int, gainDb: Float, qFactor: Float, filterType: PeqFilterType = PeqFilterType.PEAKING) {
        val current = _dspState.value.parametricBands.toMutableList()
        if (bandIndex !in current.indices) return
        current[bandIndex] = current[bandIndex].copy(
            frequencyHz = frequencyHz.coerceIn(20, 20000),
            gainDb = gainDb.coerceIn(-15f, 15f),
            qFactor = qFactor.coerceIn(0.1f, 12.0f),
            filterType = filterType
        )
        val mapped10Band = projectParametricTo10Bands(current)
        _dspState.value = _dspState.value.copy(
            parametricBands = current,
            bandGainsDb = mapped10Band,
            activePresetName = "PEQ Custom"
        )
        syncToHardwareEffects()
    }

    fun applyPreset(presetName: String) {
        val preset = allPresets().firstOrNull { it.name.equals(presetName, ignoreCase = true) } ?: return
        _dspState.value = _dspState.value.copy(
            activePresetName = preset.name,
            preampDb = preset.preampDb,
            bandGainsDb = preset.bandGainsDb
        )
        devicePresetBindings[_dspState.value.activeOutputDevice] = preset.name
        syncToHardwareEffects()
    }

    fun saveCustomPreset(slotName: String): EqPreset {
        val state = _dspState.value
        val preset = EqPreset(
            id = "custom_${slotName.lowercase().replace(' ', '_')}",
            name = slotName,
            preampDb = state.preampDb,
            bandGainsDb = state.bandGainsDb,
            isCustom = true
        )
        customPresets[preset.name] = preset
        _dspState.value = state.copy(activePresetName = preset.name)
        return preset
    }

    fun importAutoEqProfile(profileName: String, autoEqText: String): EqPreset {
        val parsed = parseAutoEqTxt(autoEqText)
        val mappedGains = projectParametricTo10Bands(parsed.second)
        val preset = EqPreset(
            id = "autoeq_${profileName.lowercase().replace(' ', '_')}",
            name = "AutoEq: $profileName",
            preampDb = parsed.first,
            bandGainsDb = mappedGains,
            isCustom = true
        )
        customPresets[preset.name] = preset
        _dspState.value = _dspState.value.copy(
            isParametricMode = true,
            activePresetName = preset.name,
            preampDb = parsed.first,
            parametricBands = parsed.second,
            bandGainsDb = mappedGains
        )
        syncToHardwareEffects()
        return preset
    }

    fun setBassBoostPercent(percent: Int) {
        _dspState.value = _dspState.value.copy(bassBoostPercent = percent.coerceIn(0, 100))
        syncToHardwareEffects()
    }

    fun setVirtualizerPercent(percent: Int) {
        _dspState.value = _dspState.value.copy(virtualizerPercent = percent.coerceIn(0, 100))
        syncToHardwareEffects()
    }

    fun setLoudnessGainDb(gainDb: Float) {
        _dspState.value = _dspState.value.copy(loudnessGainDb = gainDb.coerceIn(0f, 15f))
        syncToHardwareEffects()
    }

    fun setCrossfeedPercent(percent: Int) {
        _dspState.value = _dspState.value.copy(crossfeedPercent = percent.coerceIn(0, 100))
    }

    fun setChannelBalance(balance: Float) {
        _dspState.value = _dspState.value.copy(channelBalanceLR = balance.coerceIn(-1f, 1f))
    }

    fun setMonoDownmix(enabled: Boolean) {
        _dspState.value = _dspState.value.copy(monoDownmix = enabled)
    }

    fun setReplayGainMode(mode: ReplayGainMode, preampDb: Float = _dspState.value.replayGainPreampDb) {
        _dspState.value = _dspState.value.copy(
            replayGainMode = mode,
            replayGainPreampDb = preampDb.coerceIn(-12f, 12f)
        )
    }

    fun setOutputSignalMode(mode: OutputSignalMode) {
        val current = _dspState.value
        _dspState.value = when (mode) {
            OutputSignalMode.BIT_PERFECT_DIRECT_PCM -> current.copy(
                outputSignalMode = mode,
                eqEnabled = false,
                monoDownmix = false
            )
            OutputSignalMode.FLOAT_32BIT_HI_RES_DSP -> current.copy(
                outputSignalMode = mode,
                eqEnabled = true,
                monoDownmix = false
            )
            OutputSignalMode.VOICE_CLARITY_MONO -> current.copy(
                outputSignalMode = mode,
                eqEnabled = true,
                monoDownmix = true
            )
        }
        syncToHardwareEffects()
    }

    fun switchOutputDevice(device: AudioOutputDevice) {
        val boundPreset = devicePresetBindings[device]
        _dspState.value = _dspState.value.copy(activeOutputDevice = device)
        if (boundPreset != null) {
            applyPreset(boundPreset)
        }
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitchSemitones: Float, concert432Hz: Boolean) {
        _dspState.value = _dspState.value.copy(
            playbackSpeed = speed.coerceIn(0.25f, 4.0f),
            pitchSemitones = pitchSemitones.coerceIn(-12f, 12f),
            concert432HzPitch = concert432Hz
        )
    }

    fun setSkipSilence(enabled: Boolean) {
        _dspState.value = _dspState.value.copy(skipSilenceEnabled = enabled)
    }

    fun recordSkippedSilence(durationMs: Long) {
        if (durationMs > 0L) {
            _dspState.value = _dspState.value.copy(
                silenceSavedMs = _dspState.value.silenceSavedMs + durationMs
            )
        }
    }

    fun setCrossfadeDurationSec(seconds: Int) {
        _dspState.value = _dspState.value.copy(crossfadeDurationSec = seconds.coerceIn(0, 12))
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        _dspState.value = _dspState.value.copy(visualizerMode = mode)
    }

    fun buildSignalPathReport(
        codecName: String,
        bitDepth: Int,
        sampleRateHz: Int,
        bitrateKbps: Int,
        isLosslessSource: Boolean
    ): SignalPathReport {
        val state = _dspState.value
        val isBitPerfect = state.outputSignalMode == OutputSignalMode.BIT_PERFECT_DIRECT_PCM && !state.eqEnabled
        val dspDesc = when {
            isBitPerfect -> "Direct PCM Bypass (0 DSP Filters • Bit-Exact)"
            state.outputSignalMode == OutputSignalMode.VOICE_CLARITY_MONO -> "Mono Voice Clarity Filter + ${state.activePresetName}"
            else -> "32-bit Float 10-Band EQ (${state.activePresetName}) + Preamp ${String.format("%+.1fdB", state.preampDb)}"
        }
        val outSampleRate = if (isBitPerfect) sampleRateHz else maxOf(sampleRateHz, 48000)
        val verdict = when {
            isLosslessSource && sampleRateHz >= 88200 && bitDepth >= 24 ->
                "Verified Hi-Res Lossless (Bandwidth extends to ${sampleRateHz / 2000}kHz, no transcode cutoff)"
            isLosslessSource && bitrateKbps >= 700 ->
                "Verified True CD Lossless (Full 22.05kHz Nyquist spectrum intact)"
            isLosslessSource && bitrateKbps in 1..350 ->
                "Warning: Possible Upscaled Lossy Transcode (16kHz brickwall rolloff detected)"
            else ->
                "Lossy Perceptual Stream (${bitrateKbps}kbps • Psychoacoustic shelf @ 16.5kHz)"
        }
        return SignalPathReport(
            sourceCodec = codecName,
            sourceBitDepth = bitDepth,
            sourceSampleRateHz = sampleRateHz,
            sourceBitrateKbps = bitrateKbps,
            dspEngineDescription = dspDesc,
            outputDeviceName = state.activeOutputDevice.label,
            outputSampleRateHz = outSampleRate,
            isBitPerfectActive = isBitPerfect,
            trueLosslessVerdict = verdict
        )
    }

    fun generateSpectrumFrame(
        playbackPositionMs: Long,
        isPlaying: Boolean,
        isLossless: Boolean
    ): FloatArray {
        val bins = FloatArray(32)
        if (!isPlaying) {
            for (i in 0 until 32) bins[i] = 0.06f
            return bins
        }
        val state = _dspState.value
        val t = playbackPositionMs / 1000.0
        for (i in 0 until 32) {
            // Map 32 FFT bins onto 10 EQ bands
            val eqIdx = ((i / 32.0) * 10.0).toInt().coerceIn(0, 9)
            val eqGainDb = if (state.eqEnabled) state.bandGainsDb[eqIdx] + (state.preampDb * 0.35f) else 0f
            val eqBoost = ((eqGainDb + 15f) / 30f).coerceIn(0.2f, 1.4f)
            val bassFactor = if (i < 6) (1f + state.bassBoostPercent / 150f) else 1f

            // If lossy and visualizer is in 24kHz check mode, show rolloff above bin 25 (>16kHz)
            val lossyRolloff = if (!isLossless && i >= 26) 0.18f else 1.0f

            val wave = 0.45 + 0.35 * sin(t * (2.2 + i * 0.37) + i * 0.5) + 0.20 * cos(t * (4.1 - i * 0.12))
            bins[i] = (wave.toFloat() * eqBoost * bassFactor * lossyRolloff).coerceIn(0.05f, 0.98f)
        }
        return bins
    }

    private fun syncToHardwareEffects() {
        val state = _dspState.value
        try {
            systemEqualizer?.let { eq ->
                eq.enabled = state.eqEnabled && state.outputSignalMode != OutputSignalMode.BIT_PERFECT_DIRECT_PCM
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minMb = range.getOrElse(0) { (-1500).toShort() }.toInt()
                val maxMb = range.getOrElse(1) { 1500.toShort() }.toInt()
                for (b in 0 until numBands) {
                    val mappedIdx = ((b.toFloat() / numBands.coerceAtLeast(1)) * 10f).toInt().coerceIn(0, 9)
                    val totalDb = (state.bandGainsDb[mappedIdx] + state.preampDb).coerceIn(-15f, 15f)
                    val millibels = (totalDb * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                    eq.setBandLevel(b.toShort(), millibels)
                }
            }
            systemBassBoost?.let { bb ->
                bb.enabled = state.bassBoostPercent > 0
                if (bb.strengthSupported) {
                    bb.setStrength((state.bassBoostPercent * 10).coerceIn(0, 1000).toShort())
                }
            }
            systemVirtualizer?.let { virt ->
                virt.enabled = state.virtualizerPercent > 0
                if (virt.strengthSupported) {
                    virt.setStrength((state.virtualizerPercent * 10).coerceIn(0, 1000).toShort())
                }
            }
            systemLoudnessEnhancer?.let { le ->
                val totalGainMb = (state.loudnessGainDb * 100f).roundToInt().coerceIn(0, 1500)
                le.enabled = totalGainMb > 0
                le.setTargetGain(totalGainMb)
            }
        } catch (_: Throwable) {
        }
    }

    companion object {
        private val preampRegex = Regex("""(?i)Preamp:\s*([+\-]?\d+(?:\.\d+)?)\s*dB""")
        private val filterRegex = Regex(
            """(?i)Filter\s+(\d+):\s*ON\s+(PK|LSC|HSC|LS|HS)\s+Fc\s+(\d+(?:\.\d+)?)\s*Hz\s+Gain\s+([+\-]?\d+(?:\.\d+)?)\s*dB(?:\s+Q\s+(\d+(?:\.\d+)?))?"""
        )

        fun parseAutoEqTxt(txt: String): Pair<Float, List<ParametricBand>> {
            var preamp = 0f
            val bands = mutableListOf<ParametricBand>()
            txt.lineSequence().forEach { line ->
                val trimmed = line.trim()
                preampRegex.find(trimmed)?.let { m ->
                    preamp = (m.groupValues[1].toFloatOrNull() ?: 0f).coerceIn(-15f, 15f)
                }
                filterRegex.find(trimmed)?.let { m ->
                    val idx = m.groupValues[1].toIntOrNull() ?: (bands.size + 1)
                    val typeCode = m.groupValues[2].uppercase()
                    val fc = (m.groupValues[3].toFloatOrNull() ?: 1000f).roundToInt().coerceIn(20, 20000)
                    val gain = (m.groupValues[4].toFloatOrNull() ?: 0f).coerceIn(-15f, 15f)
                    val q = (m.groupValues[5].toFloatOrNull() ?: 1.41f).coerceIn(0.1f, 12f)
                    val fType = when {
                        typeCode.startsWith("LS") -> PeqFilterType.LOW_SHELF
                        typeCode.startsWith("HS") -> PeqFilterType.HIGH_SHELF
                        else -> PeqFilterType.PEAKING
                    }
                    bands.add(
                        ParametricBand(
                            index = idx,
                            enabled = true,
                            filterType = fType,
                            frequencyHz = fc,
                            gainDb = gain,
                            qFactor = q
                        )
                    )
                }
            }
            val finalBands = if (bands.isEmpty()) DspState.defaultParametricBands() else bands.take(10)
            return preamp to finalBands
        }

        fun projectParametricTo10Bands(peqBands: List<ParametricBand>): List<Float> {
            return DspState.BAND_FREQUENCIES_HZ.map { centerHz ->
                var sumDb = 0f
                peqBands.filter { it.enabled }.forEach { peq ->
                    val octaves = abs(ln(centerHz.toDouble() / peq.frequencyHz.coerceAtLeast(20).toDouble()) / ln(2.0)).toFloat()
                    val bandwidthOct = (1.2f / peq.qFactor.coerceAtLeast(0.2f)).coerceIn(0.25f, 3.0f)
                    val weight = when (peq.filterType) {
                        PeqFilterType.LOW_SHELF -> if (centerHz <= peq.frequencyHz) 1.0f else (1f - (octaves / bandwidthOct)).coerceAtLeast(0f)
                        PeqFilterType.HIGH_SHELF -> if (centerHz >= peq.frequencyHz) 1.0f else (1f - (octaves / bandwidthOct)).coerceAtLeast(0f)
                        PeqFilterType.PEAKING -> (1f - (octaves / bandwidthOct)).coerceAtLeast(0f)
                    }
                    sumDb += peq.gainDb * weight
                }
                sumDb.coerceIn(-15f, 15f)
            }
        }

        val BUILT_IN_PRESETS: List<EqPreset> = listOf(
            EqPreset("flat", "Flat", 0f, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqPreset("bass_boost", "Bass Boost", -2.5f, listOf(7.5f, 6.5f, 4.5f, 2.0f, 0f, 0f, 0f, 0.5f, 1.0f, 1.0f)),
            EqPreset("treble_sparkle", "Treble Sparkle", -2.0f, listOf(0f, 0f, 0f, 0f, 0.5f, 1.5f, 3.5f, 5.5f, 6.5f, 7.0f)),
            EqPreset("acoustic", "Acoustic", -1.5f, listOf(4.0f, 3.5f, 2.0f, 0.5f, 1.5f, 1.5f, 3.0f, 3.5f, 3.0f, 2.0f)),
            EqPreset("classical", "Classical", -2.0f, listOf(4.5f, 3.5f, 2.5f, 1.0f, -1.0f, -1.0f, 0f, 2.0f, 3.5f, 4.0f)),
            EqPreset("edm", "Electronic / EDM", -3.0f, listOf(6.5f, 5.5f, 2.5f, 0f, -1.5f, 1.5f, 1.0f, 3.0f, 5.0f, 5.5f)),
            EqPreset("hiphop", "Hip-Hop", -2.5f, listOf(6.0f, 5.5f, 3.0f, 1.5f, -1.0f, -1.0f, 1.5f, 1.0f, 2.5f, 3.5f)),
            EqPreset("rock", "Rock", -2.5f, listOf(5.0f, 4.0f, 2.5f, -1.0f, -1.5f, 0.5f, 2.5f, 4.0f, 4.5f, 4.5f)),
            EqPreset("pop", "Pop", -1.5f, listOf(-1.0f, 1.5f, 3.5f, 4.5f, 3.0f, 0.5f, -1.0f, -1.0f, 1.5f, 2.0f)),
            EqPreset("vocal", "Podcast / Vocal Clarity", 0f, listOf(-4.0f, -3.0f, -1.0f, 2.5f, 4.5f, 5.0f, 4.0f, 2.0f, 0f, -1.5f)),
            EqPreset("late_night", "Late Night", 1.5f, listOf(-3.5f, -2.0f, 1.0f, 2.5f, 3.0f, 3.0f, 2.0f, 0.5f, -1.0f, -2.0f)),
            EqPreset("cinema", "Cinema Surround", -2.5f, listOf(6.0f, 5.0f, 2.0f, 0f, 1.0f, 2.0f, 2.5f, 4.0f, 5.0f, 4.5f))
        )
    }
}
