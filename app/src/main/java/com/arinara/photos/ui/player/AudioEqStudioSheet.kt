// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.photos.media.AudioDspController
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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

@Composable
fun AudioEqStudioSheet(
    visible: Boolean,
    isMinimized: Boolean,
    onMinimizedChange: (Boolean) -> Unit,
    dspState: DspState,
    presets: List<EqPreset>,
    spectrumBins: FloatArray,
    waveformSamples: FloatArray = FloatArray(64),
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

    val waterfallRows = remember { mutableStateListOf<FloatArray>() }
    LaunchedEffect(spectrumBins) {
        if (spectrumBins.any { it > 0.005f }) {
            if (waterfallRows.size >= 18) {
                waterfallRows.removeAt(0)
            }
            waterfallRows.add(spectrumBins.copyOf())
        }
    }

    val biquadResponseCurveDb = remember(
        dspState.eqEnabled,
        dspState.isParametricMode,
        dspState.preampDb,
        dspState.bandGainsDb,
        dspState.parametricBands,
        dspState.bassBoostPercent
    ) {
        AudioDspController.computeFrequencyResponseCurveDb(dspState, numPoints = 96)
    }

    val currentDspState by rememberUpdatedState(dspState)
    val currentSelectedBandIndex by rememberUpdatedState(selectedBandIndex)
    val currentOnChangeBandGain by rememberUpdatedState(onChangeBandGain)
    val currentOnUpdateParametricBand by rememberUpdatedState(onUpdateParametricBand)

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Equalizer",
        isMinimized = false,
        onMinimizedChange = null,
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
                val fillPath = remember { Path() }
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
                                val mag = spectrumBins[i].coerceIn(0f, 1.0f)
                                val barH = (h * mag).coerceAtLeast(2.dp.toPx())
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
                            fillPath.reset()
                            val stepX = w / (binCount - 1).coerceAtLeast(1)
                            var prevX = 0f
                            var prevY = h
                            for (i in 0 until binCount) {
                                val x = i * stepX
                                val y = h * (1f - spectrumBins[i].coerceIn(0f, 0.96f))
                                if (i == 0) {
                                    curvePath.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    val midX = (prevX + x) * 0.5f
                                    curvePath.cubicTo(midX, prevY, midX, y, x, y)
                                    fillPath.cubicTo(midX, prevY, midX, y, x, y)
                                }
                                prevX = x
                                prevY = y
                            }
                            fillPath.lineTo(w, h)
                            fillPath.close()
                            drawPath(
                                path = fillPath,
                                color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.18f)
                            )
                            drawPath(
                                path = curvePath,
                                color = ElectricSlateTokens.PrimaryAccent,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        VisualizerMode.OSCILLOSCOPE -> {
                            curvePath.reset()
                            val waveCount = waveformSamples.size.coerceAtLeast(2)
                            val stepX = w / (waveCount - 1)
                            val midY = h * 0.5f
                            drawLine(
                                color = ElectricSlateTokens.BorderSubtle,
                                start = Offset(0f, midY),
                                end = Offset(w, midY),
                                strokeWidth = 1.dp.toPx()
                            )
                            var prevX = 0f
                            var prevY = midY
                            for (i in 0 until waveCount) {
                                val sample = waveformSamples[i].coerceIn(-1f, 1f)
                                val x = i * stepX
                                val y = midY - (sample * (h * 0.44f))
                                if (i == 0) {
                                    curvePath.moveTo(x, y)
                                } else {
                                    val midX = (prevX + x) * 0.5f
                                    curvePath.cubicTo(midX, prevY, midX, y, x, y)
                                }
                                prevX = x
                                prevY = y
                            }
                            drawPath(
                                path = curvePath,
                                color = ElectricSlateTokens.PrimaryAccent,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        VisualizerMode.WATERFALL_SPECTROGRAM -> {
                            val rows = waterfallRows.size.coerceAtLeast(1)
                            val rowH = h / 18f
                            val slotW = w / binCount
                            if (waterfallRows.isEmpty()) {
                                for (i in 0 until binCount) {
                                    val mag = spectrumBins[i].coerceIn(0f, 1.0f)
                                    val colH = (h * mag).coerceAtLeast(2.dp.toPx())
                                    drawRect(
                                        color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.75f),
                                        topLeft = Offset(i * slotW, h - colH),
                                        size = Size(slotW * 0.85f, colH)
                                    )
                                }
                            } else {
                                for (r in 0 until rows) {
                                    val rowBins = waterfallRows[r]
                                    val ageAlpha = ((r + 1).toFloat() / rows.toFloat()).coerceIn(0.12f, 1.0f)
                                    val yTop = h - ((rows - r) * rowH)
                                    for (i in 0 until minOf(binCount, rowBins.size)) {
                                        val mag = rowBins[i].coerceIn(0f, 1.0f)
                                        if (mag > 0.02f) {
                                            val cellColor = if (mag > 0.72f) {
                                                ElectricSlateTokens.TextPrimary.copy(alpha = (mag * ageAlpha).coerceIn(0.15f, 0.95f))
                                            } else {
                                                ElectricSlateTokens.PrimaryAccent.copy(alpha = (mag * ageAlpha).coerceIn(0.10f, 0.90f))
                                            }
                                            drawRect(
                                                color = cellColor,
                                                topLeft = Offset(i * slotW, yTop.coerceAtLeast(0f)),
                                                size = Size((slotW * 0.90f).coerceAtLeast(2f), (rowH * 0.92f).coerceAtLeast(2f))
                                            )
                                        }
                                    }
                                }
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

            // Real-Time Biquad Transfer-Function Frequency Response Curve (20Hz - 20kHz) + Live FFT Overlay + Interactive Band Editor
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
                        text = "Preamp: ${String.format("%+.1f dB", dspState.preampDb)}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    val peakDb = (biquadResponseCurveDb.maxOrNull() ?: 0f)
                    Text(
                        text = "Real-Time • Peak ${String.format("%+.1f dB", peakDb)}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (peakDb > 6f) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                    )
                }
                AppSlider(
                    value = dspState.preampDb,
                    onValueChange = onChangePreampDb,
                    valueRange = -15f..15f,
                    steps = 0
                )

                val eqPath = remember { Path() }
                val eqFillPath = remember { Path() }
                val rtaPath = remember { Path() }
                val logMin = remember { ln(20.0) }
                val logSpan = remember { ln(20000.0 / 20.0) }

                fun applyRealTimeCurveTouch(touchX: Float, touchY: Float, canvasW: Float, canvasH: Float, lockToSelected: Boolean) {
                    val xFrac = (touchX / canvasW.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val touchedFreqHz = exp(logMin + xFrac * logSpan).roundToInt().coerceIn(20, 20000)
                    val newGain = (((0.5f - (touchY / canvasH.coerceAtLeast(1f))) / 0.42f) * 15f).coerceIn(-15f, 15f)
                    val stateSnap = currentDspState
                    if (stateSnap.isParametricMode) {
                        val targetIdx = if (lockToSelected) {
                            currentSelectedBandIndex.coerceIn(0, stateSnap.parametricBands.lastIndex.coerceAtLeast(0))
                        } else {
                            var bestIdx = 0
                            var bestDist = Float.MAX_VALUE
                            stateSnap.parametricBands.forEachIndexed { idx, pBand ->
                                val nodeFrac = ((ln(pBand.frequencyHz.coerceAtLeast(20).toDouble()) - logMin) / logSpan).toFloat()
                                val dist = abs(nodeFrac - xFrac)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    bestIdx = idx
                                }
                            }
                            bestIdx
                        }
                        selectedBandIndex = targetIdx
                        val peq = stateSnap.parametricBands.getOrElse(targetIdx) {
                            DspState.defaultParametricBands().first()
                        }
                        currentOnUpdateParametricBand(targetIdx, touchedFreqHz, newGain, peq.qFactor)
                    } else {
                        var bestIdx = 0
                        var bestDist = Float.MAX_VALUE
                        DspState.BAND_FREQUENCIES_HZ.forEachIndexed { idx, freqHz ->
                            val nodeFrac = ((ln(freqHz.toDouble()) - logMin) / logSpan).toFloat()
                            val dist = abs(nodeFrac - xFrac)
                            if (dist < bestDist) {
                                bestDist = dist
                                bestIdx = idx
                            }
                        }
                        selectedBandIndex = bestIdx
                        currentOnChangeBandGain(bestIdx, newGain)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                applyRealTimeCurveTouch(
                                    touchX = offset.x,
                                    touchY = offset.y,
                                    canvasW = size.width.toFloat(),
                                    canvasH = size.height.toFloat(),
                                    lockToSelected = false
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    applyRealTimeCurveTouch(
                                        touchX = offset.x,
                                        touchY = offset.y,
                                        canvasW = size.width.toFloat(),
                                        canvasH = size.height.toFloat(),
                                        lockToSelected = false
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    applyRealTimeCurveTouch(
                                        touchX = change.position.x,
                                        touchY = change.position.y,
                                        canvasW = size.width.toFloat(),
                                        canvasH = size.height.toFloat(),
                                        lockToSelected = currentDspState.isParametricMode
                                    )
                                }
                            )
                        }
                        .padding(10.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val midY = h * 0.5f
                    val topGridY = midY - (12f / 18f) * (h * 0.45f)
                    val botGridY = midY + (12f / 18f) * (h * 0.45f)

                    // Vertical logarithmic frequency reference lines
                    DspState.BAND_FREQUENCIES_HZ.forEachIndexed { idx, freqHz ->
                        val xFrac = ((ln(freqHz.toDouble()) - logMin) / logSpan).toFloat().coerceIn(0f, 1f)
                        val gx = xFrac * w
                        drawLine(
                            color = if (idx == selectedBandIndex) {
                                ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.35f)
                            } else {
                                ElectricSlateTokens.BorderSubtle.copy(alpha = 0.25f)
                            },
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // +12dB, 0dB, -12dB horizontal reference lines
                    drawLine(
                        color = ElectricSlateTokens.BorderSubtle.copy(alpha = 0.45f),
                        start = Offset(0f, topGridY),
                        end = Offset(w, topGridY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = ElectricSlateTokens.BorderSubtle,
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = ElectricSlateTokens.BorderSubtle.copy(alpha = 0.45f),
                        start = Offset(0f, botGridY),
                        end = Offset(w, botGridY),
                        strokeWidth = 1.dp.toPx()
                    )

                    // 1. Live Real-Time 32-Band FFT Spectrum Overlay behind Biquad Curve
                    val specCount = spectrumBins.size.coerceAtLeast(1)
                    if (spectrumBins.any { it > 0.01f }) {
                        rtaPath.reset()
                        val slotW = w / specCount
                        rtaPath.moveTo(0f, h)
                        for (i in 0 until specCount) {
                            val mag = spectrumBins[i].coerceIn(0f, 1f)
                            val sx = (i + 0.5f) * slotW
                            val sy = h - (mag * h * 0.88f)
                            rtaPath.lineTo(sx, sy)
                            drawRoundRect(
                                color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.22f),
                                topLeft = Offset(i * slotW + 1f, sy),
                                size = Size((slotW * 0.72f).coerceAtLeast(2f), (h - sy).coerceAtLeast(2f)),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                        rtaPath.lineTo(w, h)
                        rtaPath.close()
                        drawPath(
                            path = rtaPath,
                            color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.10f)
                        )
                    }

                    // 2. Continuous Real-Time Biquad Transfer-Function Curve (96 log-spaced frequency points)
                    eqPath.reset()
                    eqFillPath.reset()
                    val nPts = biquadResponseCurveDb.size.coerceAtLeast(2)
                    var prevX = 0f
                    var prevY = midY
                    for (i in 0 until nPts) {
                        val x = (i.toFloat() / (nPts - 1)) * w
                        val db = biquadResponseCurveDb[i].coerceIn(-18f, 18f)
                        val y = midY - (db / 18f) * (h * 0.45f)
                        if (i == 0) {
                            eqPath.moveTo(x, y)
                            eqFillPath.moveTo(x, midY)
                            eqFillPath.lineTo(x, y)
                        } else {
                            val ctrlX = (prevX + x) * 0.5f
                            eqPath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                            eqFillPath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                        }
                        prevX = x
                        prevY = y
                    }
                    eqFillPath.lineTo(w, midY)
                    eqFillPath.close()

                    drawPath(
                        path = eqFillPath,
                        color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.24f)
                    )
                    drawPath(
                        path = eqPath,
                        color = ElectricSlateTokens.PrimaryAccent,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 3. Interactive Band Control Nodes positioned at true logarithmic frequencies
                    if (dspState.isParametricMode) {
                        dspState.parametricBands.forEachIndexed { idx, pBand ->
                            val xFrac = ((ln(pBand.frequencyHz.coerceAtLeast(20).toDouble()) - logMin) / logSpan).toFloat().coerceIn(0f, 1f)
                            val x = xFrac * w
                            val gainDb = (pBand.gainDb + dspState.preampDb).coerceIn(-18f, 18f)
                            val y = midY - (gainDb / 18f) * (h * 0.45f)
                            if (idx == selectedBandIndex) {
                                drawCircle(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.28f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            drawCircle(
                                color = if (idx == selectedBandIndex) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                                radius = if (idx == selectedBandIndex) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    } else {
                        DspState.BAND_FREQUENCIES_HZ.forEachIndexed { idx, freqHz ->
                            val xFrac = ((ln(freqHz.toDouble()) - logMin) / logSpan).toFloat().coerceIn(0f, 1f)
                            val x = xFrac * w
                            val gainDb = (dspState.bandGainsDb.getOrElse(idx) { 0f } + dspState.preampDb).coerceIn(-18f, 18f)
                            val y = midY - (gainDb / 18f) * (h * 0.45f)
                            if (idx == selectedBandIndex) {
                                drawCircle(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.28f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            drawCircle(
                                color = if (idx == selectedBandIndex) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                                radius = if (idx == selectedBandIndex) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("31Hz", "125Hz", "500Hz", "2kHz", "8kHz", "16kHz").forEach { freqLbl ->
                        Text(
                            text = freqLbl,
                            fontFamily = ElmsSans,
                            fontSize = 10.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                // Real-Time 10-Band Multi-Touch Vertical Fader Bank
                if (!dspState.isParametricMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val bandIdx = ((offset.x / w) * 10f).toInt().coerceIn(0, 9)
                                    selectedBandIndex = bandIdx
                                    val gainDb = (((0.5f - (offset.y / h)) / 0.42f) * 15f).coerceIn(-15f, 15f)
                                    currentOnChangeBandGain(bandIdx, gainDb)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val bandIdx = ((change.position.x / w) * 10f).toInt().coerceIn(0, 9)
                                    selectedBandIndex = bandIdx
                                    val gainDb = (((0.5f - (change.position.y / h)) / 0.42f) * 15f).coerceIn(-15f, 15f)
                                    currentOnChangeBandGain(bandIdx, gainDb)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val midY = h * 0.5f
                        val slotW = w / 10f
                        val trackW = 5.dp.toPx()

                        drawLine(
                            color = ElectricSlateTokens.BorderSubtle.copy(alpha = 0.5f),
                            start = Offset(0f, midY),
                            end = Offset(w, midY),
                            strokeWidth = 1.dp.toPx()
                        )

                        for (idx in 0 until 10) {
                            val cx = (idx + 0.5f) * slotW
                            val gainDb = dspState.bandGainsDb.getOrElse(idx) { 0f }.coerceIn(-15f, 15f)
                            val thumbY = midY - (gainDb / 15f) * (h * 0.42f)

                            // Inactive vertical fader slot
                            drawRoundRect(
                                color = ElectricSlateTokens.TrackInactive,
                                topLeft = Offset(cx - trackW / 2f, h * 0.08f),
                                size = Size(trackW, h * 0.84f),
                                cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                            )

                            // Active gain bar from 0dB center line
                            val barTop = minOf(midY, thumbY)
                            val barHeight = abs(thumbY - midY).coerceAtLeast(2.dp.toPx())
                            drawRoundRect(
                                color = if (idx == selectedBandIndex) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.65f),
                                topLeft = Offset(cx - trackW / 2f, barTop),
                                size = Size(trackW, barHeight),
                                cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                            )

                            // Fader thumb
                            drawCircle(
                                color = if (idx == selectedBandIndex) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                                radius = if (idx == selectedBandIndex) 6.dp.toPx() else 4.5.dp.toPx(),
                                center = Offset(cx, thumbY)
                            )
                        }
                    }
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
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
