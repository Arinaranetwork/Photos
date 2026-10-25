// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.arinara.photos.data.model.AudioBookmark
import com.arinara.photos.data.model.CueTrackSlice
import com.arinara.photos.data.model.LoopMode
import com.arinara.photos.data.model.LrcLyricLine
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.media.AbLoopState
import com.arinara.photos.media.DspState
import com.arinara.photos.media.MultiQueueManager
import com.arinara.photos.media.OutputSignalMode
import com.arinara.photos.media.QueueSlot
import com.arinara.photos.media.QueueSlotState
import com.arinara.photos.media.ReplayGainMode
import com.arinara.photos.media.SignalPathReport
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppBottomSheet
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppSlider
import com.arinara.photos.ui.components.presets.ConnectedInspector
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PlayerSubTab(val label: String) {
    NOW_PLAYING("Deck & Loop"),
    UP_NEXT_QUEUES("Up Next & 5 Queues"),
    LYRICS_CUE("LRC & CUE"),
    SPEED_DSP("Pitch & Signal"),
    TAGS_BOOKMARKS("Tags & Notes")
}

@Composable
fun AudiophilePlayerSheet(
    visible: Boolean,
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    trackDurationMs: Long,
    loopMode: LoopMode,
    abLoopState: AbLoopState,
    activeQueueState: QueueSlotState,
    allQueueSlots: List<QueueSlotState>,
    dspState: DspState,
    signalPathReport: SignalPathReport,
    lyrics: List<LrcLyricLine>,
    cueSlices: List<CueTrackSlice>,
    bookmarks: List<AudioBookmark>,
    sleepTimerRemainingMin: Int,
    onDismiss: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleLoopMode: () -> Unit,
    onMarkAbStart: (Long) -> Unit,
    onMarkAbEnd: (Long) -> Unit,
    onNudgeAbBounds: (Long, Long) -> Unit,
    onSetAbMaxLoops: (Int) -> Unit,
    onClearAbLoop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSwitchQueueSlot: (QueueSlot) -> Unit,
    onSelectQueueTrack: (Int) -> Unit,
    onMoveQueueTrack: (Int, Int) -> Unit,
    onRemoveQueueTrack: (Int) -> Unit,
    onSaveQueueM3u8: () -> Unit,
    onSelectOutputSignalMode: (OutputSignalMode) -> Unit,
    onChangeSpeedAndPitch: (Float, Float, Boolean) -> Unit,
    onToggleSkipSilence: (Boolean) -> Unit,
    onChangeCrossfadeSec: (Int) -> Unit,
    onChangeCrossfeedPercent: (Int) -> Unit,
    onChangeChannelBalance: (Float) -> Unit,
    onChangeReplayGainMode: (ReplayGainMode) -> Unit,
    onSetSleepTimerMin: (Int) -> Unit,
    onAddBookmark: (String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onSaveTagEdits: (String, String, String) -> Unit,
    onOpenEqStudio: () -> Unit,
    onOpenTrimmerForCurrent: () -> Unit
) {
    if (currentTrack == null) return

    var activeSubTab by remember { mutableStateOf(PlayerSubTab.NOW_PLAYING) }
    var showSignalPathInspector by remember { mutableStateOf(false) }
    var lyricOffsetMs by remember { mutableLongStateOf(0L) }

    var editTitle by remember(currentTrack.id) { mutableStateOf(currentTrack.title) }
    var editArtist by remember(currentTrack.id) { mutableStateOf(currentTrack.artist) }
    var editAlbum by remember(currentTrack.id) { mutableStateOf(currentTrack.album) }
    var newBookmarkLabel by remember { mutableStateOf("Chorus / Solo Highlight") }

    val safeDuration = trackDurationMs.coerceAtLeast(1000L)
    val progressFraction = (playbackPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Audiophile Hi-Res Deck"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sub-Navigation Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerSubTab.entries.forEach { tab ->
                    AppPillChip(
                        label = tab.label,
                        isSelected = activeSubTab == tab,
                        onClick = { activeSubTab = tab }
                    )
                }
            }

            // Track Header Card + Tappable Lossless Signal Path Badge (F-A07)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = currentTrack.isLossless,
                cornerRadius = 20.dp,
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ElectricSlateTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentTrack.artist} • ${currentTrack.album}",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentTrack.audioSpecSummary,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AppPillChip(
                        label = if (signalPathReport.isBitPerfectActive) "BIT-PERFECT" else currentTrack.qualityBadge,
                        leadingIcon = Icons.Outlined.GraphicEq,
                        isSelected = showSignalPathInspector,
                        onClick = { showSignalPathInspector = !showSignalPathInspector }
                    )
                }

                // ConnectedInspector Signal Path Chain Popup (F-A07)
                if (showSignalPathInspector) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ConnectedInspector(
                        visible = true,
                        panelWidth = 310.dp,
                        neckHeight = 14.dp
                    ) {
                        Text(
                            text = "Lossless Signal Path Chain",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SignalStageRow("1. Source", "${signalPathReport.sourceCodec} (${signalPathReport.sourceBitDepth}-bit / ${signalPathReport.sourceSampleRateHz / 1000}kHz • ${signalPathReport.sourceBitrateKbps}kbps)")
                        SignalStageRow("2. Engine", signalPathReport.dspEngineDescription)
                        SignalStageRow("3. Output", "${signalPathReport.outputDeviceName} @ ${signalPathReport.outputSampleRateHz / 1000}kHz")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = signalPathReport.trueLosslessVerdict,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }
            }

            when (activeSubTab) {
                PlayerSubTab.NOW_PLAYING -> {
                    // Interactive Waveform Seekbar + A-B Loop Brackets (F-A02 & F-A03)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        val abStartFrac = abLoopState.startMs?.let { (it.toFloat() / safeDuration).coerceIn(0f, 1f) }
                        val abEndFrac = abLoopState.endMs?.let { (it.toFloat() / safeDuration).coerceIn(0f, 1f) }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            val barCount = 44
                            val spacing = size.width / barCount
                            val barW = (spacing * 0.58f).coerceAtLeast(2.dp.toPx())

                            // Highlight A-B loop region if active
                            if (abStartFrac != null && abEndFrac != null && abEndFrac > abStartFrac) {
                                val xStart = abStartFrac * size.width
                                val xEnd = abEndFrac * size.width
                                drawRoundRect(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.20f),
                                    topLeft = Offset(xStart, 0f),
                                    size = Size(xEnd - xStart, size.height),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }

                            for (i in 0 until barCount) {
                                val frac = i.toFloat() / barCount
                                val amp = (0.28f + 0.65f * abs(sin(i * 0.55f + 0.8f))).coerceIn(0.18f, 0.96f)
                                val barH = size.height * amp
                                val isPlayed = frac <= progressFraction
                                val inAbRange = abStartFrac != null && abEndFrac != null && frac in abStartFrac..abEndFrac
                                val barColor = when {
                                    inAbRange -> ElectricSlateTokens.PrimaryAccent
                                    isPlayed -> ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.85f)
                                    else -> ElectricSlateTokens.TrackInactive
                                }
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(i * spacing, (size.height - barH) / 2f),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }
                        }

                        AppSlider(
                            value = playbackPositionMs.toFloat(),
                            onValueChange = { onSeekToMs(it.toLong()) },
                            valueRange = 0f..safeDuration.toFloat(),
                            steps = 0
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = MediaItem.formatDurationMillis(playbackPositionMs),
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = "-${MediaItem.formatDurationMillis((safeDuration - playbackPositionMs).coerceAtLeast(0L))} / ${MediaItem.formatDurationMillis(safeDuration)}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }

                    // Primary Transport Deck: 4-State Loop, Prev, Play/Pause, Next, Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppPillChip(
                            label = "${loopMode.label} (${loopMode.shortBadge})",
                            leadingIcon = if (loopMode == LoopMode.TRACK) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                            isSelected = loopMode != LoopMode.OFF,
                            onClick = onCycleLoopMode
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                    .jigglePressClickable(onClick = onSkipPrevious),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.PrimaryAccent)
                                    .border(1.dp, ElectricSlateTokens.PrimaryAccent, CircleShape)
                                    .jigglePressClickable(onClick = onPlayPauseToggle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                    .jigglePressClickable(onClick = onSkipNext),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SkipNext,
                                    contentDescription = "Next",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        AppPillChip(
                            label = if (activeQueueState.isShuffled) "Shuffled" else "Shuffle",
                            leadingIcon = Icons.Outlined.Shuffle,
                            isSelected = activeQueueState.isShuffled,
                            onClick = onToggleShuffle
                        )
                    }

                    // Precision A-B Section Loop Studio Panel (F-A03)
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
                                text = "Precision A-B Section Loop",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = if (abLoopState.isConfigured) {
                                    "Loop #${abLoopState.completedIterations + 1}" +
                                        if (abLoopState.maxIterations > 0) "/${abLoopState.maxIterations}" else " (∞)"
                                } else {
                                    "Tap Set A & Set B"
                                },
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "A: ${abLoopState.startMs?.let { MediaItem.formatDurationMillis(it) } ?: "--:--"}",
                                onClick = { onMarkAbStart(playbackPositionMs) },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "B: ${abLoopState.endMs?.let { MediaItem.formatDurationMillis(it) } ?: "--:--"}",
                                onClick = { onMarkAbEnd(playbackPositionMs) },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "-0.1s",
                                onClick = { onNudgeAbBounds(-100L, 0L) },
                                variant = AppButtonVariant.OUTLINED,
                                height = 36.dp,
                                horizontalPadding = 8.dp,
                                fontSize = 12.sp
                            )
                            AppButton(
                                text = "+0.1s",
                                onClick = { onNudgeAbBounds(0L, 100L) },
                                variant = AppButtonVariant.OUTLINED,
                                height = 36.dp,
                                horizontalPadding = 8.dp,
                                fontSize = 12.sp
                            )
                            if (abLoopState.startMs != null) {
                                AppButton(
                                    text = "Clear",
                                    onClick = onClearAbLoop,
                                    variant = AppButtonVariant.DESTRUCTIVE,
                                    height = 36.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Max Loops:",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            listOf(0 to "∞", 3 to "3x", 5 to "5x", 10 to "10x").forEach { (count, label) ->
                                AppPillChip(
                                    label = label,
                                    isSelected = abLoopState.maxIterations == count,
                                    onClick = { onSetAbMaxLoops(count) }
                                )
                            }
                        }
                    }

                    // Quick Launch Row: 10-Band EQ Studio & Lossless Audio Cutter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppButton(
                            text = "10-Band EQ & Spectrogram",
                            leadingIcon = Icons.Outlined.Equalizer,
                            onClick = onOpenEqStudio,
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                        AppButton(
                            text = "Cut / Convert Track",
                            leadingIcon = Icons.Outlined.ContentCut,
                            onClick = onOpenTrimmerForCurrent,
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                PlayerSubTab.UP_NEXT_QUEUES -> {
                    // 5 Independent Multi-Queue Slots with Position Memory (F-A05)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "5 Multi-Queue Slots (Auto-Saves Track & Timestamp)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            allQueueSlots.forEach { slotState ->
                                val isSelected = slotState.slot == activeQueueState.slot
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .jigglePressClickable { onSwitchQueueSlot(slotState.slot) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slotState.slot.displayName,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "${slotState.tracks.size} tracks • Resume @ ${MediaItem.formatDurationMillis(slotState.savedPositionMs)}",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "ACTIVE",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.PrimaryAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Arrange What Plays Next — Reorderable Up Next List (F-A04)
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
                                text = "Arrange Up Next (${activeQueueState.tracks.size})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Export .M3U8",
                                leadingIcon = Icons.Outlined.Save,
                                onClick = onSaveQueueM3u8,
                                variant = AppButtonVariant.SECONDARY,
                                height = 32.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeQueueState.tracks.forEachIndexed { idx, track ->
                                val isCurrent = idx == activeQueueState.activeIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                        .border(
                                            1.dp,
                                            if (isCurrent) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .jigglePressClickable { onSelectQueueTrack(idx) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${idx + 1}. ${track.title}",
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.qualityBadge} • ${track.formattedDuration}",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (idx > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(ElectricSlateTokens.SurfacePanel)
                                                    .jigglePressClickable { onMoveQueueTrack(idx, idx - 1) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    tint = ElectricSlateTokens.TextPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        if (idx < activeQueueState.tracks.lastIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(ElectricSlateTokens.SurfacePanel)
                                                    .jigglePressClickable { onMoveQueueTrack(idx, idx + 1) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    tint = ElectricSlateTokens.TextPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        if (activeQueueState.tracks.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(ElectricSlateTokens.SurfacePanel)
                                                    .jigglePressClickable { onRemoveQueueTrack(idx) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Remove",
                                                    tint = ElectricSlateTokens.Destructive,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PlayerSubTab.LYRICS_CUE -> {
                    // Synced LRC Lyrics Engine with Tap-Line-to-Seek & Offset (F-A15)
                    val activeLyricIdx = MultiQueueManager.findActiveLyricIndex(lyrics, playbackPositionMs, lyricOffsetMs)
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
                                text = "Synced .LRC Lyrics (Tap Line to Seek)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AppPillChip(
                                    label = "-100ms",
                                    isSelected = false,
                                    onClick = { lyricOffsetMs -= 100L }
                                )
                                AppPillChip(
                                    label = "${lyricOffsetMs}ms",
                                    isSelected = lyricOffsetMs != 0L,
                                    onClick = { lyricOffsetMs = 0L }
                                )
                                AppPillChip(
                                    label = "+100ms",
                                    isSelected = false,
                                    onClick = { lyricOffsetMs += 100L }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lyrics.isEmpty()) {
                            Text(
                                text = "No companion .LRC file found for this track.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                lyrics.forEachIndexed { idx, line ->
                                    val isCurrent = idx == activeLyricIdx
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                            .jigglePressClickable { onSeekToMs(line.timestampMs) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = line.text,
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (isCurrent) 16.sp else 14.sp,
                                            color = if (isCurrent) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = MediaItem.formatDurationMillis(line.timestampMs),
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CUE Sheet Virtual Track Splitter (F-A16)
                    if (cueSlices.isNotEmpty()) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            contentPadding = 12.dp
                        ) {
                            Text(
                                text = "CUE Sheet Virtual Chapters (${cueSlices.size} Tracks)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                cueSlices.forEach { slice ->
                                    val inSlice = playbackPositionMs in slice.startMs until slice.endMs
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (inSlice) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                            .border(
                                                1.dp,
                                                if (inSlice) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .jigglePressClickable { onSeekToMs(slice.startMs) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "0${slice.trackNumber}. ${slice.title}",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                            Text(
                                                text = "${slice.performer} • ${MediaItem.formatDurationMillis(slice.startMs)} – ${MediaItem.formatDurationMillis(slice.endMs)}",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                        Text(
                                            text = "JUMP",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.PrimaryAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PlayerSubTab.SPEED_DSP -> {
                    // Output Signal Quality & Resampler Mode Switcher (F-A08)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Output Signal Path & Resampler Mode",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutputSignalMode.entries.forEach { mode ->
                                val selected = dspState.outputSignalMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.BgCanvas)
                                        .border(
                                            1.dp,
                                            if (selected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .jigglePressClickable { onSelectOutputSignalMode(mode) }
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
                                    Text(
                                        text = mode.badge,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Independent Pitch, Tempo & 432Hz Tuning (F-A10)
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
                                text = "Tempo (${String.format("%.2fx", dspState.playbackSpeed)}) & Pitch (${String.format("%+.1f st", dspState.pitchSemitones)})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppPillChip(
                                label = if (dspState.concert432HzPitch) "432Hz Active" else "432Hz Natural",
                                isSelected = dspState.concert432HzPitch,
                                onClick = {
                                    onChangeSpeedAndPitch(
                                        dspState.playbackSpeed,
                                        dspState.pitchSemitones,
                                        !dspState.concert432HzPitch
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        AppSlider(
                            value = dspState.playbackSpeed,
                            onValueChange = {
                                onChangeSpeedAndPitch(it, dspState.pitchSemitones, dspState.concert432HzPitch)
                            },
                            valueRange = 0.25f..3.0f,
                            steps = 0
                        )
                        AppSlider(
                            value = dspState.pitchSemitones,
                            onValueChange = {
                                onChangeSpeedAndPitch(dspState.playbackSpeed, it, dspState.concert432HzPitch)
                            },
                            valueRange = -12f..12f,
                            steps = 0
                        )
                    }

                    // Smart Skip Silence, Crossfade, Crossfeed, Balance & Sleep Timer (F-A09, F-A11..F-A14)
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
                            Column {
                                Text(
                                    text = "Smart Skip Silence",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "Time Saved: ${MediaItem.formatDurationMillis(dspState.silenceSavedMs)}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }
                            AppPillChip(
                                label = if (dspState.skipSilenceEnabled) "Active" else "Off",
                                isSelected = dspState.skipSilenceEnabled,
                                onClick = { onToggleSkipSilence(!dspState.skipSilenceEnabled) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Crossfade: ${if (dspState.crossfadeDurationSec == 0) "Gapless (0s)" else "${dspState.crossfadeDurationSec}s"} • Headphone Crossfeed: ${dspState.crossfeedPercent}%",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        AppSlider(
                            value = dspState.crossfadeDurationSec.toFloat(),
                            onValueChange = { onChangeCrossfadeSec(it.roundToInt()) },
                            valueRange = 0f..12f,
                            steps = 0
                        )
                        AppSlider(
                            value = dspState.crossfeedPercent.toFloat(),
                            onValueChange = { onChangeCrossfeedPercent(it.roundToInt()) },
                            valueRange = 0f..100f,
                            steps = 0
                        )
                        Text(
                            text = "L/R Channel Balance: ${String.format("%+.2f", dspState.channelBalanceLR)}",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        AppSlider(
                            value = dspState.channelBalanceLR,
                            onValueChange = onChangeChannelBalance,
                            valueRange = -1f..1f,
                            steps = 0
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sleep Fade:",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, label) ->
                                AppPillChip(
                                    label = label,
                                    isSelected = sleepTimerRemainingMin == mins,
                                    onClick = { onSetSleepTimerMin(mins) }
                                )
                            }
                        }
                    }
                }

                PlayerSubTab.TAGS_BOOKMARKS -> {
                    // Timestamp Bookmarks & Study Notes (F-A18)
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
                                text = "Timestamp Bookmarks (${bookmarks.size})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Bookmark @ ${MediaItem.formatDurationMillis(playbackPositionMs)}",
                                leadingIcon = Icons.Outlined.BookmarkAdd,
                                onClick = { onAddBookmark(newBookmarkLabel) },
                                variant = AppButtonVariant.PRIMARY,
                                height = 34.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (bookmarks.isEmpty()) {
                            Text(
                                text = "No bookmarks saved for this track yet.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                bookmarks.forEach { bm ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.BgCanvas)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .jigglePressClickable { onSeekToMs(bm.timestampMs) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${MediaItem.formatDurationMillis(bm.timestampMs)} • ${bm.label}",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "Delete",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.Destructive,
                                            modifier = Modifier.jigglePressClickable { onRemoveBookmark(bm.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Built-in Audio Tag & Metadata Editor (F-A17)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "ID3 / FLAC / Vorbis Tag Editor",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TagInputField("Title", editTitle) { editTitle = it }
                        Spacer(modifier = Modifier.height(6.dp))
                        TagInputField("Artist", editArtist) { editArtist = it }
                        Spacer(modifier = Modifier.height(6.dp))
                        TagInputField("Album", editAlbum) { editAlbum = it }
                        Spacer(modifier = Modifier.height(10.dp))
                        AppButton(
                            text = "Save Tags to Track",
                            leadingIcon = Icons.Outlined.Edit,
                            onClick = { onSaveTagEdits(editTitle, editArtist, editAlbum) },
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalStageRow(stage: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stage,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = ElectricSlateTokens.TextMuted
        )
        Text(
            text = detail,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = ElectricSlateTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TagInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = ElectricSlateTokens.TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElectricSlateTokens.BgCanvas)
                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                ),
                cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
