// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.photos.media.AudioOutputDevice
import com.arinara.photos.media.DspState
import com.arinara.photos.media.EqPreset
import com.arinara.photos.media.VisualizerMode
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppBottomSheet
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppSlider
import com.arinara.photos.ui.theme.ElectricSlateTokens
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AudioEqStudioSheet(
    visible: Boolean,
    isMinimized: Boolean,
    onMinimizedChange: (Boolean) -> Unit,
    dspState: DspState,
    presets: List<EqPreset>,
    spectrumBins: FloatArray,
    isLosslessTrack: Boolean,
    onDismiss: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onToggleParametricMode: (Boolean) -> Unit,
    onChangePreampDb: (Float) -> Unit,
    onChangeBandGain: (Int, Float) -> Unit,
    onUpdateParametricBand: (Int, Int, Float, Float) -> Unit,
    onApplyPreset: (String) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onImportAutoEqProfile: (String, String) -> Unit,
    onChangeBassBoost: (Int) -> Unit,
    onChangeVirtualizer: (Int) -> Unit,
    onChangeLoudnessDb: (Float) -> Unit,
    onSwitchOutputDevice: (AudioOutputDevice) -> Unit,
    onChangeVisualizerMode: (VisualizerMode) -> Unit
) {
    var selectedBandIndex by remember { mutableIntStateOf(0) }
    var customSlotCounter by remember { mutableIntStateOf(1) }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "10-Band EQ & 60fps DSP Studio",
        isMinimized = isMinimized,
        onMinimizedChange = onMinimizedChange,
        minimizedPillText = "EQ Active: ${dspState.activePresetName} (${String.format("%+.1fdB", dspState.preampDb)})"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Master Toggle + Graphic vs Parametric Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPillChip(
                        label = if (dspState.eqEnabled) "EQ Enabled" else "EQ Bypassed",
                        leadingIcon = Icons.Outlined.Equalizer,
                        isSelected = dspState.eqEnabled,
                        onClick = { onToggleEqEnabled(!dspState.eqEnabled) }
                    )
                    AppPillChip(
                        label = if (dspState.isParametricMode) "Parametric (PEQ)" else "10-Band Graphic",
                        leadingIcon = Icons.Outlined.Tune,
                        isSelected = dspState.isParametricMode,
                        onClick = { onToggleParametricMode(!dspState.isParametricMode) }
                    )
                }

                AppButton(
                    text = "Save Custom",
                    leadingIcon = Icons.Outlined.Save,
                    onClick = {
                        onSaveCustomPreset("User Custom $customSlotCounter")
                        customSlotCounter = (customSlotCounter % 3) + 1
                    },
                    variant = AppButtonVariant.SECONDARY,
                    height = 34.dp,
                    horizontalPadding = 10.dp,
                    fontSize = 11.sp
                )
            }

            // 60fps Real-Time FFT Spectrum / Oscilloscope / 24kHz Lossless Spectrogram (F-E06)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "60fps Real-Time Analyzer",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Text(
                        text = if (isLosslessTrack) "24kHz+ True Lossless" else "16kHz Lossy Shelf",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VisualizerMode.entries.forEach { mode ->
                        AppPillChip(
                            label = mode.label,
                            isSelected = dspState.visualizerMode == mode,
                            onClick = { onChangeVisualizerMode(mode) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val curvePath = remember { Path() }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val binCount = spectrumBins.size.coerceAtLeast(1)

                    when (dspState.visualizerMode) {
                        VisualizerMode.BARS_32 -> {
                            val slotW = w / binCount
                            val barW = (slotW * 0.68f).coerceAtLeast(2.dp.toPx())
                            for (i in 0 until binCount) {
                                val mag = spectrumBins[i].coerceIn(0.05f, 1.0f)
                                val barH = h * mag
                                drawRoundRect(
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    topLeft = Offset(i * slotW, h - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }
                        }
                        VisualizerMode.WAVE_CURVE -> {
                            curvePath.reset()
                            val stepX = w / (binCount - 1).coerceAtLeast(1)
                            for (i in 0 until binCount) {
                                val x = i * stepX
                                val y = h * (1f - spectrumBins[i].coerceIn(0.05f, 0.95f))
                                if (i == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
                            }
                            drawPath(
                                path = curvePath,
                                color = ElectricSlateTokens.PrimaryAccent,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        VisualizerMode.OSCILLOSCOPE -> {
                            curvePath.reset()
                            val points = 64
                            val stepX = w / (points - 1)
                            val midY = h * 0.5f
                            for (i in 0 until points) {
                                val binVal = spectrumBins[i % binCount]
                                val x = i * stepX
                                val y = midY + sin(i * 0.35f) * cos(i * 0.12f) * (h * 0.42f * binVal)
                                if (i == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
                            }
                            drawPath(
                                path = curvePath,
                                color = ElectricSlateTokens.PrimaryAccent,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        VisualizerMode.WATERFALL_SPECTROGRAM -> {
                            // Draw 16kHz vs 24kHz Nyquist cutoff line + spectral density columns
                            val cutoffX = w * 0.78f
                            val slotW = w / binCount
                            for (i in 0 until binCount) {
                                val mag = spectrumBins[i].coerceIn(0.05f, 1.0f)
                                val colH = h * mag
                                val isUltraHighFreq = i >= 25
                                val color = if (isUltraHighFreq && isLosslessTrack) {
                                    ElectricSlateTokens.PrimaryAccent
                                } else if (isUltraHighFreq) {
                                    ElectricSlateTokens.Destructive.copy(alpha = 0.45f)
                                } else {
                                    ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.75f)
                                }
                                drawRect(
                                    color = color,
                                    topLeft = Offset(i * slotW, h - colH),
                                    size = Size(slotW * 0.82f, colH)
                                )
                            }
                            drawLine(
                                color = ElectricSlateTokens.TextPrimary,
                                start = Offset(cutoffX, 0f),
                                end = Offset(cutoffX, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Per-Output Device Auto-Preset Switcher (F-E05)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Text(
                    text = "Output Device Auto-Preset Binding",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioOutputDevice.entries.forEach { dev ->
                        AppPillChip(
                            label = dev.label,
                            leadingIcon = Icons.Outlined.Headphones,
                            isSelected = dspState.activeOutputDevice == dev,
                            onClick = { onSwitchOutputDevice(dev) }
                        )
                    }
                }
            }

            // Genre Presets & 1-Tap AutoEq Headphone Profiles (F-E02 & F-E03)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Text(
                    text = "Built-in Genre & Custom Presets",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        AppPillChip(
                            label = preset.name,
                            isSelected = dspState.activePresetName.equals(preset.name, ignoreCase = true),
                            onClick = { onApplyPreset(preset.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1-Tap AutoEq / Wavelet Reference Headphone Targets",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AUTOEQ_REFERENCE_PROFILES.forEach { (name, profileTxt) ->
                        AppPillChip(
                            label = "AutoEq: $name",
                            isSelected = dspState.activePresetName.contains(name, ignoreCase = true),
                            onClick = { onImportAutoEqProfile(name, profileTxt) }
                        )
                    }
                }
            }

            // 10-Band Frequency Curve + Band Editor (F-E01 & F-E02)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preamp Gain: ${String.format("%+.1f dB", dspState.preampDb)}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Text(
                        text = "Anti-Clipping Headroom Guard",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }
                AppSlider(
                    value = dspState.preampDb,
                    onValueChange = onChangePreampDb,
                    valueRange = -15f..15f,
                    steps = 0
                )

                // Spline Frequency Curve Visualization across 10 Bands
                val eqPath = remember { Path() }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val midY = h * 0.5f
                    drawLine(
                        color = ElectricSlateTokens.BorderSubtle,
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.dp.toPx()
                    )
                    eqPath.reset()
                    val count = dspState.bandGainsDb.size
                    val stepX = w / (count - 1).coerceAtLeast(1)
                    dspState.bandGainsDb.forEachIndexed { idx, gainDb ->
                        val x = idx * stepX
                        val norm = (gainDb / 15f).coerceIn(-1f, 1f)
                        val y = midY - (norm * (h * 0.42f))
                        if (idx == 0) eqPath.moveTo(x, y) else eqPath.lineTo(x, y)
                        drawCircle(
                            color = if (idx == selectedBandIndex) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                            radius = if (idx == selectedBandIndex) 5.dp.toPx() else 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    drawPath(
                        path = eqPath,
                        color = ElectricSlateTokens.PrimaryAccent,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Band Selector Chips (31Hz .. 16kHz)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DspState.BAND_LABELS.forEachIndexed { idx, label ->
                        val gain = dspState.bandGainsDb.getOrElse(idx) { 0f }
                        AppPillChip(
                            label = "${label}Hz (${String.format("%+.1f", gain)})",
                            isSelected = selectedBandIndex == idx,
                            onClick = { selectedBandIndex = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val activeGain = dspState.bandGainsDb.getOrElse(selectedBandIndex) { 0f }
                val activeFreqHz = DspState.BAND_FREQUENCIES_HZ.getOrElse(selectedBandIndex) { 1000 }

                if (!dspState.isParametricMode) {
                    Text(
                        text = "Band ${selectedBandIndex + 1} (${activeFreqHz} Hz): ${String.format("%+.1f dB", activeGain)}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                    AppSlider(
                        value = activeGain,
                        onValueChange = { onChangeBandGain(selectedBandIndex, it) },
                        valueRange = -15f..15f,
                        steps = 0
                    )
                } else {
                    val peq = dspState.parametricBands.getOrElse(selectedBandIndex) {
                        DspState.defaultParametricBands().first()
                    }
                    Text(
                        text = "PEQ Filter #${selectedBandIndex + 1} (${peq.filterType.label}): Fc ${peq.frequencyHz}Hz • Gain ${String.format("%+.1fdB", peq.gainDb)} • Q ${String.format("%.2f", peq.qFactor)}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                    AppSlider(
                        value = peq.frequencyHz.toFloat(),
                        onValueChange = {
                            onUpdateParametricBand(selectedBandIndex, it.roundToInt(), peq.gainDb, peq.qFactor)
                        },
                        valueRange = 20f..20000f,
                        steps = 0
                    )
                    AppSlider(
                        value = peq.gainDb,
                        onValueChange = {
                            onUpdateParametricBand(selectedBandIndex, peq.frequencyHz, it, peq.qFactor)
                        },
                        valueRange = -15f..15f,
                        steps = 0
                    )
                    AppSlider(
                        value = peq.qFactor,
                        onValueChange = {
                            onUpdateParametricBand(selectedBandIndex, peq.frequencyHz, peq.gainDb, it)
                        },
                        valueRange = 0.2f..10.0f,
                        steps = 0
                    )
                }
            }

            // Analog-Style Bass Boost, 3D Spatial Virtualizer & +15dB Loudness Enhancer (F-E04)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Text(
                    text = "Bass Boost (${dspState.bassBoostPercent}%) • 3D Spatializer (${dspState.virtualizerPercent}%)",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                AppSlider(
                    value = dspState.bassBoostPercent.toFloat(),
                    onValueChange = { onChangeBassBoost(it.roundToInt()) },
                    valueRange = 0f..100f,
                    steps = 0
                )
                AppSlider(
                    value = dspState.virtualizerPercent.toFloat(),
                    onValueChange = { onChangeVirtualizer(it.roundToInt()) },
                    valueRange = 0f..100f,
                    steps = 0
                )
                Text(
                    text = "Loudness Enhancer Preamp: +${String.format("%.1f dB", dspState.loudnessGainDb)} (Up to +15.0 dB)",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.PrimaryAccent
                )
                AppSlider(
                    value = dspState.loudnessGainDb,
                    onValueChange = onChangeLoudnessDb,
                    valueRange = 0f..15f,
                    steps = 0
                )
            }
        }
    }
}

private val AUTOEQ_REFERENCE_PROFILES = listOf(
    "Harman Target" to """
        Preamp: -3.2 dB
        Filter 1: ON LSC Fc 105 Hz Gain 5.5 dB Q 0.71
        Filter 2: ON PK Fc 220 Hz Gain -1.8 dB Q 1.40
        Filter 3: ON PK Fc 3200 Hz Gain 3.4 dB Q 1.80
        Filter 4: ON HSC Fc 10000 Hz Gain 2.5 dB Q 0.71
    """.trimIndent(),
    "Sennheiser HD650" to """
        Preamp: -4.8 dB
        Filter 1: ON LSC Fc 75 Hz Gain 5.2 dB Q 0.70
        Filter 2: ON PK Fc 180 Hz Gain -2.1 dB Q 1.20
        Filter 3: ON PK Fc 3100 Hz Gain 2.2 dB Q 2.10
        Filter 4: ON HSC Fc 9500 Hz Gain 3.0 dB Q 0.70
    """.trimIndent(),
    "Sony WH-1000XM5" to """
        Preamp: -3.5 dB
        Filter 1: ON LSC Fc 120 Hz Gain -4.2 dB Q 0.80
        Filter 2: ON PK Fc 450 Hz Gain 1.8 dB Q 1.50
        Filter 3: ON PK Fc 2600 Hz Gain 3.8 dB Q 1.60
        Filter 4: ON HSC Fc 8500 Hz Gain 2.0 dB Q 0.75
    """.trimIndent()
)
