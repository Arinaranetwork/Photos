// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.studio

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.photos.data.model.AudioQualityTier
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.media.inspector.BatchImageFormat
import com.arinara.photos.media.inspector.MediaInspectorAndVaultEngine
import com.arinara.photos.media.trimmer.TrimCutMode
import com.arinara.photos.media.trimmer.TrimJobConfig
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppBottomSheet
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppSlider
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

enum class StudioToolTab(val label: String) {
    CUTTER_TRIMMER("Fast Cutter & Split"),
    AUDIO_CONVERTER("Quality & Extract"),
    BATCH_CONVERTER("Batch & Rename")
}

@Composable
fun MediaTrimmerStudioSheet(
    visible: Boolean,
    targetItem: MediaItem?,
    allMediaItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onExecuteTrimJob: (TrimJobConfig) -> Unit,
    onExportGifLoop: (MediaItem, Long, Long, Int) -> Unit,
    onExecuteBatchConvert: (BatchImageFormat, Float, Int, Boolean) -> Unit
) {
    val activeItem = targetItem ?: allMediaItems.firstOrNull() ?: return
    val totalDurationMs = activeItem.durationMs.coerceAtLeast(10_000L)

    var selectedTab by remember { mutableStateOf(StudioToolTab.CUTTER_TRIMMER) }
    var cutMode by remember { mutableStateOf(TrimCutMode.TRIM_OUTSIDE) }
    var startMs by remember(activeItem.id) { mutableLongStateOf(1_000L) }
    var endMs by remember(activeItem.id) { mutableLongStateOf((totalDurationMs * 0.75f).toLong().coerceAtLeast(3_000L)) }
    var splitCount by remember { mutableIntStateOf(3) }

    var selectedQualityTier by remember { mutableStateOf(AudioQualityTier.CD_LOSSLESS) }
    var fadeInMs by remember { mutableLongStateOf(500L) }
    var fadeOutMs by remember { mutableLongStateOf(1_000L) }
    var exportAudioOnly by remember { mutableStateOf(false) }
    var exportAsRingtone by remember { mutableStateOf(false) }
    var gifFps by remember { mutableIntStateOf(15) }

    var batchFormat by remember { mutableStateOf(BatchImageFormat.WEBP) }
    var batchScale by remember { mutableFloatStateOf(1.0f) }
    var batchQuality by remember { mutableIntStateOf(92) }
    var batchStripExif by remember { mutableStateOf(true) }
    var renamePattern by remember { mutableStateOf("{date}_{album}_{counter}") }

    val renamePreviews = remember(allMediaItems, renamePattern) {
        MediaInspectorAndVaultEngine().previewBatchTokenRename(allMediaItems.take(4), renamePattern)
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Non-Destructive Cutter & Quality Studio"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sub-Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudioToolTab.entries.forEach { tab ->
                    AppPillChip(
                        label = tab.label,
                        isSelected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }

            // Target Source File Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Source: ${activeItem.title}",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeItem.extension} • ${activeItem.formattedSize} • ${MediaItem.formatDurationMillis(totalDurationMs)} (Source file is never overwritten)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                    Text(
                        text = activeItem.qualityBadge,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                }
            }

            when (selectedTab) {
                StudioToolTab.CUTTER_TRIMMER -> {
                    // Cut Mode Selector: Trim Outside, Cut Middle, Multi-Segment Split (F-S01, F-S02)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Cut Operation Mode",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TrimCutMode.entries.forEach { mode ->
                                val isSel = cutMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                        .border(
                                            1.dp,
                                            if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .jigglePressClickable { cutMode = mode }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.label,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = mode.description,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Waveform & Dual-Range Handle Scrubbers (F-S01)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        val startFrac = (startMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                        val endFrac = (endMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.BgCanvas)
                                .padding(6.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val x1 = startFrac * w
                            val x2 = endFrac * w
                            drawRoundRect(
                                color = if (cutMode == TrimCutMode.CUT_MIDDLE) {
                                    ElectricSlateTokens.Destructive.copy(alpha = 0.25f)
                                } else {
                                    ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.25f)
                                },
                                topLeft = Offset(x1, 0f),
                                size = Size((x2 - x1).coerceAtLeast(4f), h),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            val bars = 40
                            val step = w / bars
                            for (i in 0 until bars) {
                                val f = i.toFloat() / bars
                                val inSelection = f in startFrac..endFrac
                                val keep = if (cutMode == TrimCutMode.CUT_MIDDLE) !inSelection else inSelection
                                val amp = (0.25f + 0.65f * abs(sin(i * 0.45f))).coerceIn(0.2f, 0.95f)
                                val bh = h * amp
                                drawRoundRect(
                                    color = if (keep) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                                    topLeft = Offset(i * step, (h - bh) / 2f),
                                    size = Size(step * 0.6f, bh),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (cutMode != TrimCutMode.MULTI_SEGMENT) {
                            Text(
                                text = "In-Point (Start): ${MediaItem.formatDurationMillis(startMs)} (${startMs}ms)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppSlider(
                                value = startMs.toFloat(),
                                onValueChange = { startMs = it.toLong().coerceIn(0L, (endMs - 500L).coerceAtLeast(0L)) },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                steps = 0
                            )
                            Text(
                                text = "Out-Point (End): ${MediaItem.formatDurationMillis(endMs)} (${endMs}ms)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            AppSlider(
                                value = endMs.toFloat(),
                                onValueChange = { endMs = it.toLong().coerceIn(startMs + 500L, totalDurationMs) },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                steps = 0
                            )
                        } else {
                            Text(
                                text = "Multi-Segment Split Count: $splitCount Equal Parts",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            AppSlider(
                                value = splitCount.toFloat(),
                                onValueChange = { splitCount = it.roundToInt().coerceIn(2, 8) },
                                valueRange = 2f..8f,
                                steps = 5
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "Execute Lossless Fast-Cut",
                                leadingIcon = Icons.Outlined.ContentCut,
                                onClick = {
                                    onExecuteTrimJob(
                                        TrimJobConfig(
                                            sourceItem = activeItem,
                                            cutMode = cutMode,
                                            startMs = startMs,
                                            endMs = endMs,
                                            splitCount = splitCount,
                                            qualityTier = selectedQualityTier,
                                            fadeInMs = fadeInMs,
                                            fadeOutMs = fadeOutMs,
                                            exportAsAudioOnly = exportAudioOnly,
                                            exportAsRingtone = exportAsRingtone
                                        )
                                    )
                                },
                                variant = AppButtonVariant.PRIMARY,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Export GIF (${gifFps}fps)",
                                leadingIcon = Icons.Outlined.AutoAwesomeMotion,
                                onClick = { onExportGifLoop(activeItem, startMs, endMs, gifFps) },
                                variant = AppButtonVariant.SECONDARY
                            )
                        }
                    }
                }

                StudioToolTab.AUDIO_CONVERTER -> {
                    // Multi-Quality Audio Extractor & Converter (F-S03 & F-S04)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Select Output Audio Quality Tier",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AudioQualityTier.entries.forEach { tier ->
                                val isSel = selectedQualityTier == tier
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                        .border(
                                            1.dp,
                                            if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .jigglePressClickable { selectedQualityTier = tier }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tier.title,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "${tier.bitDepth}-bit / ${tier.sampleRateHz / 1000}kHz • ${tier.targetBitrateKbps} kbps",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    Text(
                                        text = tier.badge,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Fade-In (${fadeInMs}ms) & Fade-Out (${fadeOutMs}ms) Envelope",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        AppSlider(
                            value = fadeInMs.toFloat(),
                            onValueChange = { fadeInMs = it.toLong() },
                            valueRange = 0f..5000f,
                            steps = 0
                        )
                        AppSlider(
                            value = fadeOutMs.toFloat(),
                            onValueChange = { fadeOutMs = it.toLong() },
                            valueRange = 0f..5000f,
                            steps = 0
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPillChip(
                                label = if (exportAudioOnly) "Video -> Audio Only ON" else "Extract Audio Only",
                                isSelected = exportAudioOnly,
                                onClick = { exportAudioOnly = !exportAudioOnly }
                            )
                            AppPillChip(
                                label = if (exportAsRingtone) "Ringtone (<=30s) ON" else "Ringtone Mode",
                                isSelected = exportAsRingtone,
                                onClick = { exportAsRingtone = !exportAsRingtone }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        AppButton(
                            text = "Extract / Transcode Audio (${selectedQualityTier.badge})",
                            leadingIcon = Icons.Outlined.GraphicEq,
                            onClick = {
                                onExecuteTrimJob(
                                    TrimJobConfig(
                                        sourceItem = activeItem,
                                        cutMode = TrimCutMode.TRIM_OUTSIDE,
                                        startMs = startMs,
                                        endMs = endMs,
                                        qualityTier = selectedQualityTier,
                                        fadeInMs = fadeInMs,
                                        fadeOutMs = fadeOutMs,
                                        exportAsAudioOnly = true,
                                        exportAsRingtone = exportAsRingtone
                                    )
                                )
                            },
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                StudioToolTab.BATCH_CONVERTER -> {
                    // Batch Image Converter / Resizer & Token Renamer (F-S07 & F-S08)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Batch Image Format Converter & Resizer",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BatchImageFormat.entries.forEach { fmt ->
                                AppPillChip(
                                    label = fmt.ext,
                                    isSelected = batchFormat == fmt,
                                    onClick = { batchFormat = fmt }
                                )
                            }
                            AppPillChip(
                                label = if (batchStripExif) "Strip EXIF ON" else "Keep EXIF",
                                isSelected = batchStripExif,
                                onClick = { batchStripExif = !batchStripExif }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Resolution Scale (${(batchScale * 100).roundToInt()}%) • Quality (${batchQuality}%)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        AppSlider(
                            value = batchScale,
                            onValueChange = { batchScale = it },
                            valueRange = 0.25f..1.0f,
                            steps = 0
                        )
                        AppSlider(
                            value = batchQuality.toFloat(),
                            onValueChange = { batchQuality = it.roundToInt() },
                            valueRange = 50f..100f,
                            steps = 0
                        )
                        AppButton(
                            text = "Run Batch Conversion to ${batchFormat.ext}",
                            leadingIcon = Icons.Outlined.Transform,
                            onClick = {
                                onExecuteBatchConvert(batchFormat, batchScale, batchQuality, batchStripExif)
                            },
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Batch Token Pattern Renamer Preview (F-S08)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Batch Token Pattern Renamer ({date}, {time}, {album}, {counter})",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.BgCanvas)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = renamePattern,
                                onValueChange = { renamePattern = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                ),
                                cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            renamePreviews.forEach { prev ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = prev.originalName,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "→ ${prev.newName}",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
