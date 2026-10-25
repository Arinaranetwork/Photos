// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
        title = "Equalizer",
        isMinimized = isMinimized,
        onMinimizedChange = onMinimizedChange,
        minimizedPillText = "EQ: ${dspState.activePresetName}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPillChip(
                        label = if (dspState.eqEnabled) "On" else "Off",
                        leadingIcon = Icons.Outlined.Equalizer,
                        isSelected = dspState.eqEnabled,
                        onClick = { onToggleEqEnabled(!dspState.eqEnabled) }
                    )
                    AppPillChip(
                        label = if (dspState.isParametricMode) "Parametric" else "10-Band",
                        leadingIcon = Icons.Outlined.Tune,
                        isSelected = dspState.isParametricMode,
                        onClick = { onToggleParametricMode(!dspState.isParametricMode) }
                    )
                }

                AppButton(
                    text = "Save Preset",
                    leadingIcon = Icons.Outlined.Save,
                    onClick = {
                        onSaveCustomPreset("Custom $customSlotCounter")
                        customSlotCounter = (customSlotCounter % 3) + 1
                    },
                    variant = AppButtonVariant.SECONDARY,
                    height = 34.dp,
                    horizontalPadding = 10.dp,
                    fontSize = 11.sp
                )
            }

            // Visualizer Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
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
                        .height(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas)
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
                            val slotW = w / binCount
                            for (i in 0 until binCount) {
                                val mag = spectrumBins[i].coerceIn(0.05f, 1.0f)
                                val colH = h * mag
                                drawRect(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.85f),
                                    topLeft = Offset(i * slotW, h - colH),
                                    size = Size(slotW * 0.82f, colH)
                                )
                            }
                        }
                    }
                }
            }

            // Output Profile & Presets
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Text(
                    text = "Presets",
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

            // 10-Band Frequency Curve + Band Editor
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Text(
                    text = "Preamp: ${String.format("%+.1f dB", dspState.preampDb)}",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                AppSlider(
                    value = dspState.preampDb,
                    onValueChange = onChangePreampDb,
                    valueRange = -15f..15f,
                    steps = 0
                )

                val eqPath = remember { Path() }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas)
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
                        text = "${activeFreqHz} Hz: ${String.format("%+.1f dB", activeGain)}",
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
                        text = "${peq.frequencyHz}Hz • ${String.format("%+.1fdB", peq.gainDb)} • Q ${String.format("%.2f", peq.qFactor)}",
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

            // Bass Boost, Virtualizer & Loudness
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                Text(
                    text = "Bass Boost (${dspState.bassBoostPercent}%) • Spatial (${dspState.virtualizerPercent}%)",
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
                    text = "Loudness Boost: +${String.format("%.1f dB", dspState.loudnessGainDb)}",
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
