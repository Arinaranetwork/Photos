// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.player

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    NOW_PLAYING("Player"),
    UP_NEXT_QUEUES("Queue"),
    LYRICS_CUE("Lyrics"),
    SPEED_DSP("Audio"),
    TAGS_BOOKMARKS("Tags")
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
    sleepTimerRemainingSec: Int = sleepTimerRemainingMin * 60,
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
    onSaveLrcLyrics: (String) -> Unit = {},
    onShareCurrentTrack: (MediaItem) -> Unit = {},
    onOpenEqStudio: () -> Unit,
    onOpenTrimmerForCurrent: () -> Unit
) {
    if (!visible || currentTrack == null) return

    var activeSubTab by remember { mutableStateOf(PlayerSubTab.NOW_PLAYING) }
    var showSignalPathInspector by remember { mutableStateOf(false) }
    var lyricOffsetMs by remember { mutableLongStateOf(0L) }
    var isEditingLyrics by remember(currentTrack.id) { mutableStateOf(false) }
    var editableLrcText by remember(currentTrack.id, lyrics) {
        mutableStateOf(
            if (lyrics.isNotEmpty()) {
                MultiQueueManager.serializeLrcLyrics(lyrics)
            } else {
                "[00:00.00] ${currentTrack.title} - ${currentTrack.artist}\nPaste or type lyrics here, then tap Stamp Line while playing"
            }
        )
    }
    var stampCursorLineIndex by remember(currentTrack.id) { mutableIntStateOf(0) }

    var editTitle by remember(currentTrack.id) { mutableStateOf(currentTrack.title) }
    var editArtist by remember(currentTrack.id) { mutableStateOf(currentTrack.artist) }
    var editAlbum by remember(currentTrack.id) { mutableStateOf(currentTrack.album) }
    var newBookmarkLabel by remember { mutableStateOf("Bookmark") }

    val safeDuration = trackDurationMs.coerceAtLeast(1000L)
    val progressFraction = (playbackPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Now Playing"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            // Track Header Card with Real Album Art
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
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricSlateTokens.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        if (!currentTrack.albumArtUriString.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(currentTrack.albumArtUriString),
                                contentDescription = currentTrack.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
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
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AppPillChip(
                        label = currentTrack.extension,
                        leadingIcon = Icons.Outlined.GraphicEq,
                        isSelected = showSignalPathInspector,
                        onClick = { showSignalPathInspector = !showSignalPathInspector }
                    )
                }

                if (showSignalPathInspector) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ConnectedInspector(
                        visible = true,
                        panelWidth = 310.dp,
                        neckHeight = 14.dp
                    ) {
                        SignalStageRow("Format", "${signalPathReport.sourceCodec} (${signalPathReport.sourceBitrateKbps} kbps)")
                        SignalStageRow("Sample Rate", "${signalPathReport.sourceSampleRateHz / 1000} kHz / ${signalPathReport.sourceBitDepth}-bit")
                        SignalStageRow("Output", signalPathReport.outputDeviceName)
                    }
                }
            }

            when (activeSubTab) {
                PlayerSubTab.NOW_PLAYING -> {
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
                                .height(48.dp)
                        ) {
                            val barCount = 44
                            val spacing = size.width / barCount
                            val barW = (spacing * 0.58f).coerceAtLeast(2.dp.toPx())

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
                            value = playbackPositionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
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
                                text = MediaItem.formatDurationMillis(safeDuration),
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }

                    // Primary Transport Deck: Loop, Prev, Play/Pause, Next, Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppPillChip(
                            label = loopMode.shortBadge,
                            leadingIcon = if (loopMode == LoopMode.TRACK) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                            isSelected = loopMode != LoopMode.OFF,
                            onClick = onCycleLoopMode
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.SurfaceElevated)
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
                            label = "Shuffle",
                            leadingIcon = Icons.Outlined.Shuffle,
                            isSelected = activeQueueState.isShuffled,
                            onClick = onToggleShuffle
                        )
                    }

                    // A-B Loop Card
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
                                text = "A-B Loop",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            if (abLoopState.isConfigured) {
                                Text(
                                    text = "Loop #${abLoopState.completedIterations + 1}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }
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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppButton(
                            text = "Equalizer",
                            leadingIcon = Icons.Outlined.Equalizer,
                            onClick = onOpenEqStudio,
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                        AppButton(
                            text = "Share",
                            leadingIcon = Icons.Outlined.Share,
                            onClick = { onShareCurrentTrack(currentTrack) },
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                PlayerSubTab.UP_NEXT_QUEUES -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Queues",
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
                                            text = "${slotState.tracks.size} tracks",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "Active",
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
                                text = "Up Next (${activeQueueState.tracks.size})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Save Playlist",
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
                                            text = "${track.artist} • ${track.formattedDuration}",
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
                                                    contentDescription = "Up",
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
                                                    contentDescription = "Down",
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
                    val activeLyricIdx = MultiQueueManager.findActiveLyricIndex(lyrics, playbackPositionMs, lyricOffsetMs)
                    val lyricListState = rememberLazyListState()

                    LaunchedEffect(activeLyricIdx, isEditingLyrics) {
                        if (!isEditingLyrics && activeLyricIdx >= 0 && activeLyricIdx < lyrics.size) {
                            try {
                                lyricListState.animateScrollToItem(
                                    index = (activeLyricIdx - 1).coerceAtLeast(0)
                                )
                            } catch (_: Throwable) {
                            }
                        }
                    }

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
                                text = if (isEditingLyrics) ".LRC Lyric Editor" else "Synced Lyrics",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!isEditingLyrics) {
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
                                AppPillChip(
                                    label = if (isEditingLyrics) "View Synced" else "Edit .LRC",
                                    isSelected = isEditingLyrics,
                                    onClick = {
                                        if (!isEditingLyrics && lyrics.isNotEmpty()) {
                                            editableLrcText = MultiQueueManager.serializeLrcLyrics(lyrics)
                                        }
                                        isEditingLyrics = !isEditingLyrics
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditingLyrics) {
                            val totalLines = editableLrcText.lines().size.coerceAtLeast(1)
                            val safeLineIdx = stampCursorLineIndex.coerceIn(0, totalLines - 1)
                            val currentStampTag = MultiQueueManager.formatLrcTimestamp(playbackPositionMs)

                            Text(
                                text = "Paste lyrics below or tap 'Stamp Line' as each verse plays to tag $currentStampTag:",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    text = "Stamp Line #${safeLineIdx + 1} @ $currentStampTag",
                                    leadingIcon = Icons.Outlined.Timer,
                                    onClick = {
                                        editableLrcText = MultiQueueManager.stampPlainLyricLine(
                                            rawText = editableLrcText,
                                            lineIndex = safeLineIdx,
                                            timestampMs = playbackPositionMs
                                        )
                                        stampCursorLineIndex = (safeLineIdx + 1) % totalLines.coerceAtLeast(1)
                                    },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 36.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Line 1",
                                    onClick = { stampCursorLineIndex = 0 },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 36.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.BgCanvas)
                                    .padding(12.dp)
                            ) {
                                BasicTextField(
                                    value = editableLrcText,
                                    onValueChange = { editableLrcText = it },
                                    textStyle = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AppButton(
                                text = "Save Synced .LRC Lyrics",
                                leadingIcon = Icons.Outlined.Save,
                                onClick = {
                                    onSaveLrcLyrics(editableLrcText)
                                    isEditingLyrics = false
                                },
                                variant = AppButtonVariant.PRIMARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (lyrics.isEmpty()) {
                            Text(
                                text = "No .lrc lyrics file found for this track. Tap 'Edit .LRC' above to create or sync lyrics.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        } else {
                            LazyColumn(
                                state = lyricListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(lyrics) { idx, line ->
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

                    if (cueSlices.isNotEmpty()) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            contentPadding = 12.dp
                        ) {
                            Text(
                                text = "Chapters (${cueSlices.size})",
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
                                                text = "${MediaItem.formatDurationMillis(slice.startMs)} – ${MediaItem.formatDurationMillis(slice.endMs)}",
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
                    }
                }

                PlayerSubTab.SPEED_DSP -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Output Mode",
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
                                        .jigglePressClickable { onSelectOutputSignalMode(mode) }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mode.label,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                text = "Speed (${String.format("%.2fx", dspState.playbackSpeed)}) & Pitch (${String.format("%+.1f", dspState.pitchSemitones)})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppPillChip(
                                label = "432Hz",
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
                                text = "Skip Silence",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppPillChip(
                                label = if (dspState.skipSilenceEnabled) "On" else "Off",
                                isSelected = dspState.skipSilenceEnabled,
                                onClick = { onToggleSkipSilence(!dspState.skipSilenceEnabled) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Crossfade: ${dspState.crossfadeDurationSec}s",
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
                        Text(
                            text = "Balance: ${String.format("%+.2f", dspState.channelBalanceLR)}",
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
                        val sleepStatusLabel = if (sleepTimerRemainingSec > 0) {
                            val minsPart = sleepTimerRemainingSec / 60
                            val secsPart = sleepTimerRemainingSec % 60
                            val fadeNote = if (sleepTimerRemainingSec <= 30) " • Smooth Fade-Out Active" else ""
                            String.format("Sleep Timer (%02d:%02d%s):", minsPart, secsPart, fadeNote)
                        } else {
                            "Sleep Timer (30s Smooth Fade-Out):"
                        }
                        Text(
                            text = sleepStatusLabel,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (sleepTimerRemainingSec in 1..30) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0 to "Off", 1 to "1m", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, label) ->
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
                                text = "Bookmarks (${bookmarks.size})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Add @ ${MediaItem.formatDurationMillis(playbackPositionMs)}",
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
                                text = "No bookmarks saved yet.",
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

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Edit Tags",
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
                            text = "Save",
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
