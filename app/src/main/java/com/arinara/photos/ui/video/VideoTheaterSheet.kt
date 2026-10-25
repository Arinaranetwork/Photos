// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.media.FolderSkipRule
import com.arinara.photos.media.SubtitleCue
import com.arinara.photos.media.VideoAspectMode
import com.arinara.photos.media.VideoChapter
import com.arinara.photos.media.VideoSceneBookmark
import com.arinara.photos.media.VideoTheaterEngine
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

enum class VideoTheaterDrawerTab(val label: String) {
    CONTROLS_HUD("Cinema HUD"),
    EPISODES_CHAPTERS("Episodes & Chapters"),
    SUBTITLES_TRANSCRIPT("Dual Sub & Search"),
    OPTICS_STORYBOARD("Optics & Storyboard")
}

@Composable
fun VideoTheaterSheet(
    visible: Boolean,
    videoItem: MediaItem?,
    seriesEpisodes: List<MediaItem>,
    nextEpisode: MediaItem?,
    chapters: List<VideoChapter>,
    subtitleCues: List<SubtitleCue>,
    sceneBookmarks: List<VideoSceneBookmark>,
    folderSkipRule: FolderSkipRule,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    videoDurationMs: Long,
    playbackSpeed: Float,
    onDismiss: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSelectEpisode: (MediaItem) -> Unit,
    onSaveFolderSkipRule: (FolderSkipRule) -> Unit,
    onAddSceneBookmark: (String) -> Unit,
    onCaptureFramePng: (MediaItem, Long) -> Unit,
    onGenerateStoryboard: (MediaItem) -> Unit,
    onHandOffToBackgroundAudio: (MediaItem, Long) -> Unit,
    onEnterPipMode: () -> Unit,
    onOpenTrimmerForVideo: (MediaItem) -> Unit
) {
    if (videoItem == null) return

    var activeTab by remember { mutableStateOf(VideoTheaterDrawerTab.CONTROLS_HUD) }
    var touchLockEnabled by remember { mutableStateOf(false) }
    var isHold2xActive by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.85f) } // -0.80f (Extra-Dim) .. 1.0f
    var volumeBoostPercent by remember { mutableIntStateOf(100) } // 0% .. 200%
    var seekRippleCaption by remember { mutableStateOf<String?>(null) }

    var ambientGlowEnabled by remember { mutableStateOf(true) }
    var dualSubtitlesEnabled by remember { mutableStateOf(true) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var audioSyncOffsetMs by remember { mutableLongStateOf(0L) }
    var transcriptSearchQuery by remember { mutableStateOf("") }

    var aspectMode by remember { mutableStateOf(VideoAspectMode.RATIO_16_9) }
    var videoZoomScale by remember { mutableFloatStateOf(1.0f) }
    var videoPanOffset by remember { mutableStateOf(Offset.Zero) }
    var mirrorFlipHorizontal by remember { mutableStateOf(false) }

    var videoAbStartMs by remember { mutableStateOf<Long?>(null) }
    var videoAbEndMs by remember { mutableStateOf<Long?>(null) }

    val safeDuration = videoDurationMs.coerceAtLeast(1000L)
    val activeCue = remember(subtitleCues, playbackPositionMs, subtitleOffsetMs) {
        VideoTheaterEngine.findActiveSubtitleCue(subtitleCues, playbackPositionMs, subtitleOffsetMs)
    }
    val filteredTranscript = remember(subtitleCues, transcriptSearchQuery) {
        VideoTheaterEngine.searchSubtitleTranscript(subtitleCues, transcriptSearchQuery)
    }

    val currentTouchLocked by rememberUpdatedState(touchLockEnabled)
    val currentPosMs by rememberUpdatedState(playbackPositionMs)
    val currentDurMs by rememberUpdatedState(safeDuration)
    val currentBrightness by rememberUpdatedState(brightnessLevel)
    val currentVolPercent by rememberUpdatedState(volumeBoostPercent)
    val currentZoom by rememberUpdatedState(videoZoomScale)
    val currentPan by rememberUpdatedState(videoPanOffset)
    val currentSpeed by rememberUpdatedState(playbackSpeed)

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Netflix-Style Cinema Theater"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theater Viewport Canvas with Ambient Glow, Bedtime Touch Lock, Hold-for-2x, Dual-Zone Gestures & Dual Subtitles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ElectricSlateTokens.BgCanvas)
                    .border(
                        BorderStroke(
                            width = if (ambientGlowEnabled) 2.dp else 1.dp,
                            color = if (ambientGlowEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (currentTouchLocked) return@detectTapGestures
                                val isLeftHalf = tapOffset.x < size.width * 0.5f
                                val deltaMs = if (isLeftHalf) -10_000L else 10_000L
                                val target = (currentPosMs + deltaMs).coerceIn(0L, currentDurMs)
                                seekRippleCaption = if (isLeftHalf) "⏪ -10s Seek" else "+10s Seek ⏩"
                                onSeekToMs(target)
                            },
                            onLongPress = {
                                if (currentTouchLocked) return@detectTapGestures
                                isHold2xActive = true
                                onSetPlaybackSpeed(2.0f)
                            },
                            onPress = {
                                tryAwaitRelease()
                                if (isHold2xActive) {
                                    isHold2xActive = false
                                    onSetPlaybackSpeed(1.0f)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (currentTouchLocked) return@detectVerticalDragGestures
                            change.consume()
                            val isLeftZone = change.position.x < size.width * 0.5f
                            if (isLeftZone) {
                                val delta = -dragAmount / 350f
                                brightnessLevel = VideoTheaterEngine.applyBrightnessGestureDelta(currentBrightness, delta)
                            } else {
                                val deltaPct = (-dragAmount / 3.5f).roundToInt()
                                volumeBoostPercent = VideoTheaterEngine.applyVolumeBoostGestureDelta(currentVolPercent, deltaPct)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (currentTouchLocked) return@detectTransformGestures
                            val nextZoom = (currentZoom * zoom).coerceIn(1f, 8f)
                            videoZoomScale = nextZoom
                            videoPanOffset = if (nextZoom > 1f) {
                                Offset(
                                    x = (currentPan.x + pan.x).coerceIn(-500f, 500f),
                                    y = (currentPan.y + pan.y).coerceIn(-300f, 300f)
                                )
                            } else Offset.Zero
                        }
                    }
            ) {
                // Cinema Frame + Ambient Glow Backdrop
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = (if (mirrorFlipHorizontal) -1f else 1f) * videoZoomScale
                            scaleY = videoZoomScale
                            translationX = videoPanOffset.x
                            translationY = videoPanOffset.y
                        }
                ) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricSlateTokens.PrimaryAccent.copy(alpha = if (ambientGlowEnabled) 0.42f else 0.18f),
                                ElectricSlateTokens.SurfacePanel,
                                ElectricSlateTokens.BgCanvas
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.45f),
                            radius = size.maxDimension * 0.75f
                        )
                    )

                    // Cinema letterbox bars based on selected aspect mode
                    val barHeight = when (aspectMode) {
                        VideoAspectMode.RATIO_21_9 -> size.height * 0.14f
                        VideoAspectMode.RATIO_16_9 -> size.height * 0.06f
                        else -> 0f
                    }
                    if (barHeight > 0f) {
                        drawRect(
                            color = ElectricSlateTokens.BgCanvas,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, barHeight)
                        )
                        drawRect(
                            color = ElectricSlateTokens.BgCanvas,
                            topLeft = Offset(0f, size.height - barHeight),
                            size = Size(size.width, barHeight)
                        )
                    }
                }

                // Sub-Zero Extra-Dim Night Filter Scrim (when brightnessLevel < 0f, down to -80%) (F-V07)
                if (brightnessLevel < 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ElectricSlateTokens.BgCanvas.copy(alpha = abs(brightnessLevel).coerceIn(0f, 0.80f)))
                    )
                }

                // Top Secondary Subtitle (Dual Subtitles Mode - F-V11)
                if (dualSubtitlesEnabled && activeCue?.secondaryText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 48.dp, end = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.82f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeCue.secondaryText,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Top-Left Bedtime Touch Lock Button (Always accessible even when locked - F-V06)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (touchLockEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.9f)
                        )
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                        .jigglePressClickable { touchLockEnabled = !touchLockEnabled }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (touchLockEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "Touch Lock",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (touchLockEnabled) "Touch Locked (Tap to Unlock)" else "Lock Screen",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }

                // Top-Right Status Pill (Speed / Zoom / Volume Boost / Brightness HUD)
                if (!touchLockEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.9f))
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val brightPct = (brightnessLevel * 100).roundToInt()
                        val hudLabel = when {
                            isHold2xActive -> "⚡ 2.0x Hold Speed"
                            seekRippleCaption != null -> seekRippleCaption!!
                            else -> "Bri ${brightPct}% • Vol ${volumeBoostPercent}% • ${(currentSpeed)}x"
                        }
                        Text(
                            text = hudLabel,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                    }
                }

                // Center Video Title & +85s Skip Intro Floating Button (F-V04)
                if (!touchLockEnabled) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = videoItem.title,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ElectricSlateTokens.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${videoItem.resolutionLabel} • ${videoItem.codecName} • ${aspectMode.label}",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }

                    // Floating "+85s Skip Intro" Pill in bottom-right of viewport
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent)
                            .border(1.dp, ElectricSlateTokens.PrimaryAccent, CircleShape)
                            .jigglePressClickable {
                                val target = VideoTheaterEngine().computeSkipIntroTargetMs(
                                    playbackPositionMs,
                                    safeDuration,
                                    folderSkipRule.skipIntroDurationMs
                                )
                                onSeekToMs(target)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Skip Intro (+85s) ⏭",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }

                // Bottom Primary Subtitle Overlay (F-V11)
                if (activeCue != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 24.dp, end = 140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.85f))
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeCue.primaryText,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (touchLockEnabled) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentPadding = 12.dp
                ) {
                    Text(
                        text = "Bedtime Touch Lock is active. Accidental taps and swipes are ignored while watching in bed. Tap 'Touch Locked' in the top-left of the video frame to unlock controls.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }
                return@Column
            }

            // Segmented Chapter Timeline & Seekbar (F-V10)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = 12.dp
            ) {
                val activeChapter = chapters.firstOrNull { playbackPositionMs in it.startMs..it.endMs } ?: chapters.firstOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeChapter?.title ?: "Timeline",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                    Text(
                        text = "${MediaItem.formatDurationMillis(playbackPositionMs)} / ${MediaItem.formatDurationMillis(safeDuration)} (${videoItem.remainingMinutesLabel})",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                }

                // Segmented Chapter Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .padding(top = 4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val gap = 4.dp.toPx()
                    chapters.forEach { ch ->
                        val startX = (ch.startMs.toFloat() / safeDuration) * w
                        val endX = (ch.endMs.toFloat() / safeDuration) * w
                        val segW = (endX - startX - gap).coerceAtLeast(4f)
                        val isPassed = playbackPositionMs >= ch.startMs
                        drawRoundRect(
                            color = if (isPassed) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                            topLeft = Offset(startX, 0f),
                            size = Size(segW, h),
                            cornerRadius = CornerRadius(h / 2f, h / 2f)
                        )
                    }
                }

                AppSlider(
                    value = playbackPositionMs.toFloat(),
                    onValueChange = {
                        seekRippleCaption = null
                        val target = it.toLong()
                        val abStart = videoAbStartMs
                        val abEnd = videoAbEndMs
                        if (abStart != null && abEnd != null && target > abEnd) {
                            onSeekToMs(abStart)
                        } else {
                            onSeekToMs(target)
                        }
                    },
                    valueRange = 0f..safeDuration.toFloat(),
                    steps = 0
                )

                // Transport & Quick Actions Row: Play/Pause, -10s/+10s, Frame Step (-1f/+1f), PiP, Background Audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppButton(
                            text = if (isPlaying) "Pause" else "Play",
                            leadingIcon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            onClick = onPlayPauseToggle,
                            variant = AppButtonVariant.PRIMARY,
                            height = 36.dp,
                            horizontalPadding = 12.dp,
                            fontSize = 12.sp
                        )
                        AppButton(
                            text = "-1f",
                            onClick = {
                                onSeekToMs(
                                    VideoTheaterEngine.computeFrameStepTargetMs(
                                        playbackPositionMs,
                                        videoItem.fps,
                                        -1,
                                        safeDuration
                                    )
                                )
                            },
                            variant = AppButtonVariant.SECONDARY,
                            height = 36.dp,
                            horizontalPadding = 10.dp,
                            fontSize = 12.sp
                        )
                        AppButton(
                            text = "+1f",
                            onClick = {
                                onSeekToMs(
                                    VideoTheaterEngine.computeFrameStepTargetMs(
                                        playbackPositionMs,
                                        videoItem.fps,
                                        1,
                                        safeDuration
                                    )
                                )
                            },
                            variant = AppButtonVariant.SECONDARY,
                            height = 36.dp,
                            horizontalPadding = 10.dp,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppButton(
                            text = "PiP",
                            leadingIcon = Icons.Outlined.PictureInPictureAlt,
                            onClick = onEnterPipMode,
                            variant = AppButtonVariant.SECONDARY,
                            height = 36.dp,
                            horizontalPadding = 10.dp,
                            fontSize = 12.sp
                        )
                        AppButton(
                            text = "BG Audio",
                            leadingIcon = Icons.Outlined.Headphones,
                            onClick = { onHandOffToBackgroundAudio(videoItem, playbackPositionMs) },
                            variant = AppButtonVariant.SECONDARY,
                            height = 36.dp,
                            horizontalPadding = 10.dp,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Auto-Play Next Episode Card when Next Episode exists in Series (F-V03)
            if (nextEpisode != null) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = true,
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
                                text = "UP NEXT IN SERIES (5s Auto-Play Ready)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            Text(
                                text = nextEpisode.title,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AppButton(
                            text = "Play Next Ep",
                            leadingIcon = Icons.Outlined.SkipNext,
                            onClick = { onSelectEpisode(nextEpisode) },
                            variant = AppButtonVariant.PRIMARY,
                            height = 36.dp,
                            horizontalPadding = 12.dp,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Drawer Sub-Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoTheaterDrawerTab.entries.forEach { tab ->
                    AppPillChip(
                        label = tab.label,
                        isSelected = activeTab == tab,
                        onClick = { activeTab = tab }
                    )
                }
            }

            when (activeTab) {
                VideoTheaterDrawerTab.CONTROLS_HUD -> {
                    // Extra-Dim Brightness (-80%..100%), 200% Volume Preamp Boost & Playback Speed
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Brightness & Sub-Zero Extra-Dim (${(brightnessLevel * 100).roundToInt()}%)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        AppSlider(
                            value = brightnessLevel,
                            onValueChange = { brightnessLevel = it },
                            valueRange = -0.80f..1.00f,
                            steps = 0
                        )
                        Text(
                            text = "Volume & 200% Preamp Boost (${volumeBoostPercent}%)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                        AppSlider(
                            value = volumeBoostPercent.toFloat(),
                            onValueChange = { volumeBoostPercent = it.roundToInt() },
                            valueRange = 0f..200f,
                            steps = 0
                        )
                        Text(
                            text = "Playback Speed (${String.format("%.2fx", playbackSpeed)}) — Or Long-Press Video for 2.0x",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { spd ->
                                AppPillChip(
                                    label = "${spd}x",
                                    isSelected = abs(playbackSpeed - spd) < 0.04f,
                                    onClick = { onSetPlaybackSpeed(spd) }
                                )
                            }
                        }
                    }

                    // Video A-B Section Loop & Folder Auto-Skip Rules (F-V04 & F-V17)
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
                                text = "Video A-B Loop & Folder Auto-Skip Rules",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppPillChip(
                                label = if (folderSkipRule.autoSkipIntro) "Auto-Skip ON" else "Auto-Skip OFF",
                                isSelected = folderSkipRule.autoSkipIntro,
                                onClick = {
                                    onSaveFolderSkipRule(folderSkipRule.copy(autoSkipIntro = !folderSkipRule.autoSkipIntro))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "Loop A: ${videoAbStartMs?.let { MediaItem.formatDurationMillis(it) } ?: "--:--"}",
                                onClick = { videoAbStartMs = playbackPositionMs },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Loop B: ${videoAbEndMs?.let { MediaItem.formatDurationMillis(it) } ?: "--:--"}",
                                onClick = { videoAbEndMs = (playbackPositionMs).coerceAtLeast((videoAbStartMs ?: 0L) + 1000L) },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (videoAbStartMs != null) {
                                AppButton(
                                    text = "Clear",
                                    onClick = {
                                        videoAbStartMs = null
                                        videoAbEndMs = null
                                    },
                                    variant = AppButtonVariant.DESTRUCTIVE,
                                    height = 36.dp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                VideoTheaterDrawerTab.EPISODES_CHAPTERS -> {
                    // Episodic Season/Episode Drawer (F-V02)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Series Episodes (${seriesEpisodes.size})",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            seriesEpisodes.forEach { ep ->
                                val isCurrent = ep.id == videoItem.id
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
                                        .jigglePressClickable { onSelectEpisode(ep) }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.title,
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ep.qualityBadge} • ${ep.formattedDuration} • ${(ep.resumeProgressFraction * 100).roundToInt()}% watched",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    if (isCurrent) {
                                        Text(
                                            text = "PLAYING",
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

                    // Chapters & Scene Bookmarks (F-V10 & F-V18)
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
                                text = "Chapters & Scene Bookmarks",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Bookmark Scene",
                                leadingIcon = Icons.Outlined.BookmarkAdd,
                                onClick = { onAddSceneBookmark("Highlight @ ${MediaItem.formatDurationMillis(playbackPositionMs)}") },
                                variant = AppButtonVariant.SECONDARY,
                                height = 32.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chapters.forEach { ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.BgCanvas)
                                        .jigglePressClickable { onSeekToMs(ch.startMs) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ch.title,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    Text(
                                        text = MediaItem.formatDurationMillis(ch.startMs),
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                            }
                            sceneBookmarks.forEach { bm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .jigglePressClickable { onSeekToMs(bm.timestampMs) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "★ ${bm.caption}",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    Text(
                                        text = MediaItem.formatDurationMillis(bm.timestampMs),
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                            }
                        }
                    }
                }

                VideoTheaterDrawerTab.SUBTITLES_TRANSCRIPT -> {
                    // Dual Subtitle Studio, Sync Offset & Clickable Transcript Search (F-V11, F-V12, F-V13)
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
                                text = "Dual Subtitles & Sync Studio",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppPillChip(
                                label = if (dualSubtitlesEnabled) "Dual EN+JP ON" else "Single Sub",
                                leadingIcon = Icons.Outlined.ClosedCaption,
                                isSelected = dualSubtitlesEnabled,
                                onClick = { dualSubtitlesEnabled = !dualSubtitlesEnabled }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sub Delay: ${subtitleOffsetMs}ms",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            AppPillChip(label = "-50ms", isSelected = false, onClick = { subtitleOffsetMs -= 50L })
                            AppPillChip(label = "Reset", isSelected = subtitleOffsetMs == 0L, onClick = { subtitleOffsetMs = 0L })
                            AppPillChip(label = "+50ms", isSelected = false, onClick = { subtitleOffsetMs += 50L })
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audio Delay: ${audioSyncOffsetMs}ms",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            AppPillChip(label = "-50ms", isSelected = false, onClick = { audioSyncOffsetMs -= 50L })
                            AppPillChip(label = "Reset", isSelected = audioSyncOffsetMs == 0L, onClick = { audioSyncOffsetMs = 0L })
                            AppPillChip(label = "+50ms", isSelected = false, onClick = { audioSyncOffsetMs += 50L })
                        }
                    }

                    // Searchable Clickable Subtitle Transcript Jump-to-Timestamp (F-V12)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Clickable Subtitle Transcript Search (Tap Line to Seek)",
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
                                value = transcriptSearchQuery,
                                onValueChange = { transcriptSearchQuery = it },
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
                            if (transcriptSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search dialogue lines (e.g. 'quantum', 'Sector 9', 'lock')...",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredTranscript.forEach { cue ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.BgCanvas)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .jigglePressClickable { onSeekToMs(cue.startMs) }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cue.primaryText,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        if (cue.secondaryText != null) {
                                            Text(
                                                text = cue.secondaryText,
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                    }
                                    Text(
                                        text = MediaItem.formatDurationMillis(cue.startMs),
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

                VideoTheaterDrawerTab.OPTICS_STORYBOARD -> {
                    // Aspect Ratio, Mirror Flip, Ambient Glow, Frame Grabber & 4x4 Storyboard Export (F-V09, F-V14, F-V19, F-V20)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Aspect Ratio, Mirror Flip & Ambient Glow",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoAspectMode.entries.forEach { mode ->
                                AppPillChip(
                                    label = mode.label,
                                    isSelected = aspectMode == mode,
                                    onClick = { aspectMode = mode }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPillChip(
                                label = if (mirrorFlipHorizontal) "Mirror Flip ON" else "Mirror Flip (Dance/Guitar)",
                                leadingIcon = Icons.Outlined.Flip,
                                isSelected = mirrorFlipHorizontal,
                                onClick = { mirrorFlipHorizontal = !mirrorFlipHorizontal }
                            )
                            AppPillChip(
                                label = if (ambientGlowEnabled) "Ambient Glow ON" else "Ambient Glow OFF",
                                isSelected = ambientGlowEnabled,
                                onClick = { ambientGlowEnabled = !ambientGlowEnabled }
                            )
                        }
                    }

                    // 1-Tap Frame Grabber, 4x4 Storyboard Generator & Non-Destructive Video Cutter
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Cinema Capture & Studio Tools",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "Grab Frame PNG",
                                leadingIcon = Icons.Outlined.CameraAlt,
                                onClick = { onCaptureFramePng(videoItem, playbackPositionMs) },
                                variant = AppButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "4x4 Storyboard",
                                leadingIcon = Icons.Outlined.GridOn,
                                onClick = { onGenerateStoryboard(videoItem) },
                                variant = AppButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AppButton(
                            text = "Open in Non-Destructive Cutter / Audio Extractor",
                            leadingIcon = Icons.Outlined.ContentCut,
                            onClick = { onOpenTrimmerForVideo(videoItem) },
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
