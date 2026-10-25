// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.video

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Flip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

enum class VideoTheaterDrawerTab(val label: String) {
    CONTROLS_HUD("Controls"),
    EPISODES_CHAPTERS("Episodes"),
    SUBTITLES_TRANSCRIPT("Subtitles"),
    OPTICS_STORYBOARD("Display")
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
    if (!visible || videoItem == null) return

    val context = LocalContext.current
    val videoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var localIsPlaying by remember(videoItem.id) { mutableStateOf(true) }
    var localPositionMs by remember(videoItem.id) { mutableLongStateOf(videoItem.resumePositionMs) }
    var localDurationMs by remember(videoItem.id) { mutableLongStateOf(videoItem.durationMs.coerceAtLeast(1000L)) }

    DisposableEffect(videoItem.id, videoItem.uriString) {
        try {
            val exoItem = ExoMediaItem.fromUri(Uri.parse(videoItem.uriString))
            videoPlayer.setMediaItem(exoItem)
            videoPlayer.prepare()
            if (videoItem.resumePositionMs > 0L) {
                videoPlayer.seekTo(videoItem.resumePositionMs)
            }
            videoPlayer.playWhenReady = true
            localIsPlaying = true
        } catch (_: Exception) {
        }
        onDispose {
            try {
                videoPlayer.stop()
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(videoPlayer, localIsPlaying) {
        while (true) {
            try {
                val pos = videoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = videoPlayer.duration
                localPositionMs = pos
                if (dur > 0L) {
                    localDurationMs = dur
                }
                localIsPlaying = videoPlayer.isPlaying
                onSeekToMs(pos)
            } catch (_: Exception) {
            }
            delay(200L)
        }
    }

    var activeTab by remember { mutableStateOf(VideoTheaterDrawerTab.CONTROLS_HUD) }
    var touchLockEnabled by remember { mutableStateOf(false) }
    var isHold2xActive by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.85f) }
    var volumeBoostPercent by remember { mutableIntStateOf(100) }
    var seekRippleCaption by remember { mutableStateOf<String?>(null) }

    var dualSubtitlesEnabled by remember { mutableStateOf(true) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var transcriptSearchQuery by remember { mutableStateOf("") }

    var aspectMode by remember { mutableStateOf(VideoAspectMode.RATIO_16_9) }
    var videoZoomScale by remember { mutableFloatStateOf(1.0f) }
    var videoPanOffset by remember { mutableStateOf(Offset.Zero) }
    var mirrorFlipHorizontal by remember { mutableStateOf(false) }

    var videoAbStartMs by remember { mutableStateOf<Long?>(null) }
    var videoAbEndMs by remember { mutableStateOf<Long?>(null) }

    val safeDuration = localDurationMs.coerceAtLeast(1000L)
    val activeCue = remember(subtitleCues, localPositionMs, subtitleOffsetMs) {
        VideoTheaterEngine.findActiveSubtitleCue(subtitleCues, localPositionMs, subtitleOffsetMs)
    }
    val filteredTranscript = remember(subtitleCues, transcriptSearchQuery) {
        VideoTheaterEngine.searchSubtitleTranscript(subtitleCues, transcriptSearchQuery)
    }

    val currentTouchLocked by rememberUpdatedState(touchLockEnabled)
    val currentPosMs by rememberUpdatedState(localPositionMs)
    val currentDurMs by rememberUpdatedState(safeDuration)
    val currentBrightness by rememberUpdatedState(brightnessLevel)
    val currentVolPercent by rememberUpdatedState(volumeBoostPercent)
    val currentZoom by rememberUpdatedState(videoZoomScale)
    val currentPan by rememberUpdatedState(videoPanOffset)
    val currentSpeed by rememberUpdatedState(playbackSpeed)

    AppBottomSheet(
        visible = visible,
        onDismiss = {
            try {
                videoPlayer.pause()
            } catch (_: Exception) {
            }
            onDismiss()
        },
        title = videoItem.title
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Real Video Viewport using Media3 PlayerView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ElectricSlateTokens.BgCanvas)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (currentTouchLocked) return@detectTapGestures
                                val isLeftHalf = tapOffset.x < size.width * 0.5f
                                val deltaMs = if (isLeftHalf) -10_000L else 10_000L
                                val target = (currentPosMs + deltaMs).coerceIn(0L, currentDurMs)
                                seekRippleCaption = if (isLeftHalf) "-10s" else "+10s"
                                videoPlayer.seekTo(target)
                                localPositionMs = target
                                onSeekToMs(target)
                            },
                            onLongPress = {
                                if (currentTouchLocked) return@detectTapGestures
                                isHold2xActive = true
                                videoPlayer.playbackParameters = PlaybackParameters(2.0f)
                                onSetPlaybackSpeed(2.0f)
                            },
                            onPress = {
                                tryAwaitRelease()
                                if (isHold2xActive) {
                                    isHold2xActive = false
                                    videoPlayer.playbackParameters = PlaybackParameters(1.0f)
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
                                val nextVol = VideoTheaterEngine.applyVolumeBoostGestureDelta(currentVolPercent, deltaPct)
                                volumeBoostPercent = nextVol
                                videoPlayer.volume = (nextVol / 100f).coerceIn(0f, 1f)
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
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = videoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.player = videoPlayer
                        view.resizeMode = when (aspectMode) {
                            VideoAspectMode.FILL_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            VideoAspectMode.RATIO_21_9, VideoAspectMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = (if (mirrorFlipHorizontal) -1f else 1f) * videoZoomScale
                            scaleY = videoZoomScale
                            translationX = videoPanOffset.x
                            translationY = videoPanOffset.y
                        }
                )

                // Extra-Dim Night Scrim
                if (brightnessLevel < 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ElectricSlateTokens.BgCanvas.copy(alpha = abs(brightnessLevel).coerceIn(0f, 0.80f)))
                    )
                }

                // Top Secondary Subtitle
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

                // Top-Left Screen Lock Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (touchLockEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.85f)
                        )
                        .jigglePressClickable { touchLockEnabled = !touchLockEnabled }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (touchLockEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "Lock",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (touchLockEnabled) "Locked" else "Lock",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }

                // Top-Right Status Pill
                if (!touchLockEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val hudLabel = when {
                            isHold2xActive -> "2.0x"
                            seekRippleCaption != null -> seekRippleCaption!!
                            else -> "${currentSpeed}x"
                        }
                        Text(
                            text = hudLabel,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                    }

                    // Skip +85s button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.88f))
                            .jigglePressClickable {
                                val target = VideoTheaterEngine().computeSkipIntroTargetMs(
                                    localPositionMs,
                                    safeDuration,
                                    folderSkipRule.skipIntroDurationMs
                                )
                                videoPlayer.seekTo(target)
                                localPositionMs = target
                                onSeekToMs(target)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+85s",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }

                // Bottom Primary Subtitle Overlay
                if (activeCue != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 24.dp, end = 88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.85f))
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
                        text = "Screen locked. Tap 'Locked' in the top-left corner to unlock controls.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }
                return@Column
            }

            // Timeline & Transport Card
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
                        text = MediaItem.formatDurationMillis(localPositionMs),
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                    Text(
                        text = MediaItem.formatDurationMillis(safeDuration),
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }

                if (chapters.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(top = 2.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val gap = 3.dp.toPx()
                        chapters.forEach { ch ->
                            val startX = (ch.startMs.toFloat() / safeDuration) * w
                            val endX = (ch.endMs.toFloat() / safeDuration) * w
                            val segW = (endX - startX - gap).coerceAtLeast(4f)
                            val isPassed = localPositionMs >= ch.startMs
                            drawRoundRect(
                                color = if (isPassed) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                                topLeft = Offset(startX, 0f),
                                size = Size(segW, h),
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                        }
                    }
                }

                AppSlider(
                    value = localPositionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                    onValueChange = {
                        seekRippleCaption = null
                        val target = it.toLong()
                        val abStart = videoAbStartMs
                        val abEnd = videoAbEndMs
                        val finalTarget = if (abStart != null && abEnd != null && target > abEnd) abStart else target
                        videoPlayer.seekTo(finalTarget)
                        localPositionMs = finalTarget
                        onSeekToMs(finalTarget)
                    },
                    valueRange = 0f..safeDuration.toFloat(),
                    steps = 0
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = if (localIsPlaying) "Pause" else "Play",
                            leadingIcon = if (localIsPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            onClick = {
                                if (videoPlayer.isPlaying) {
                                    videoPlayer.pause()
                                    localIsPlaying = false
                                } else {
                                    videoPlayer.play()
                                    localIsPlaying = true
                                }
                                onPlayPauseToggle()
                            },
                            variant = AppButtonVariant.PRIMARY,
                            height = 36.dp,
                            horizontalPadding = 14.dp,
                            fontSize = 12.sp
                        )
                        AppButton(
                            text = "-10s",
                            onClick = {
                                val target = (localPositionMs - 10_000L).coerceAtLeast(0L)
                                videoPlayer.seekTo(target)
                                localPositionMs = target
                                onSeekToMs(target)
                            },
                            variant = AppButtonVariant.SECONDARY,
                            height = 36.dp,
                            horizontalPadding = 10.dp,
                            fontSize = 12.sp
                        )
                        AppButton(
                            text = "+10s",
                            onClick = {
                                val target = (localPositionMs + 10_000L).coerceAtMost(safeDuration)
                                videoPlayer.seekTo(target)
                                localPositionMs = target
                                onSeekToMs(target)
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
                            text = "Audio",
                            leadingIcon = Icons.Outlined.Headphones,
                            onClick = {
                                videoPlayer.pause()
                                onHandOffToBackgroundAudio(videoItem, localPositionMs)
                            },
                            variant = AppButtonVariant.SECONDARY,
                            height = 36.dp,
                            horizontalPadding = 10.dp,
                            fontSize = 12.sp
                        )
                    }
                }
            }

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
                                text = "Next Episode",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
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
                            text = "Next",
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

            // Sub-Navigation Tabs
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
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Brightness (${(brightnessLevel * 100).roundToInt()}%)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
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
                            text = "Volume (${volumeBoostPercent}%)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                        AppSlider(
                            value = volumeBoostPercent.toFloat(),
                            onValueChange = {
                                val v = it.roundToInt()
                                volumeBoostPercent = v
                                videoPlayer.volume = (v / 100f).coerceIn(0f, 1f)
                            },
                            valueRange = 0f..200f,
                            steps = 0
                        )
                        Text(
                            text = "Speed (${String.format("%.2fx", playbackSpeed)})",
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
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                AppPillChip(
                                    label = "${spd}x",
                                    isSelected = abs(playbackSpeed - spd) < 0.04f,
                                    onClick = {
                                        videoPlayer.playbackParameters = PlaybackParameters(spd)
                                        onSetPlaybackSpeed(spd)
                                    }
                                )
                            }
                        }
                    }

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "A-B Loop",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "A: ${videoAbStartMs?.let { MediaItem.formatDurationMillis(it) } ?: "--:--"}",
                                onClick = { videoAbStartMs = localPositionMs },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "B: ${videoAbEndMs?.let { MediaItem.formatDurationMillis(it) } ?: "--:--"}",
                                onClick = { videoAbEndMs = (localPositionMs).coerceAtLeast((videoAbStartMs ?: 0L) + 1000L) },
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
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Episodes (${seriesEpisodes.size})",
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
                                            text = ep.formattedDuration,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    if (isCurrent) {
                                        Text(
                                            text = "Playing",
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
                                text = "Bookmarks",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Add Bookmark",
                                leadingIcon = Icons.Outlined.BookmarkAdd,
                                onClick = { onAddSceneBookmark("Bookmark @ ${MediaItem.formatDurationMillis(localPositionMs)}") },
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
                                        .jigglePressClickable {
                                            videoPlayer.seekTo(ch.startMs)
                                            localPositionMs = ch.startMs
                                            onSeekToMs(ch.startMs)
                                        }
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
                                        .jigglePressClickable {
                                            videoPlayer.seekTo(bm.timestampMs)
                                            localPositionMs = bm.timestampMs
                                            onSeekToMs(bm.timestampMs)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bm.caption,
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
                                text = "Subtitles",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppPillChip(
                                label = if (dualSubtitlesEnabled) "Dual Subtitles" else "Single Subtitle",
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
                                text = "Offset: ${subtitleOffsetMs}ms",
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
                    }

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Search Subtitles",
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
                                    text = "Search subtitle lines...",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (filteredTranscript.isEmpty()) {
                            Text(
                                text = "No external .srt subtitle file found next to this video.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredTranscript.forEach { cue ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.BgCanvas)
                                            .jigglePressClickable {
                                                videoPlayer.seekTo(cue.startMs)
                                                localPositionMs = cue.startMs
                                                onSeekToMs(cue.startMs)
                                            }
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
                }

                VideoTheaterDrawerTab.OPTICS_STORYBOARD -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Aspect Ratio & Orientation",
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
                                label = if (mirrorFlipHorizontal) "Mirrored" else "Mirror Video",
                                leadingIcon = Icons.Outlined.Flip,
                                isSelected = mirrorFlipHorizontal,
                                onClick = { mirrorFlipHorizontal = !mirrorFlipHorizontal }
                            )
                            AppButton(
                                text = "Save Frame",
                                leadingIcon = Icons.Outlined.CameraAlt,
                                onClick = { onCaptureFramePng(videoItem, localPositionMs) },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
