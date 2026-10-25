// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arinara.photos.PipCoreAction
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.media.FolderSkipRule
import com.arinara.photos.media.SubtitleCue
import com.arinara.photos.media.VideoAspectMode
import com.arinara.photos.media.VideoChapter
import com.arinara.photos.media.VideoSceneBookmark
import com.arinara.photos.media.VideoTheaterEngine
import com.arinara.photos.media.converter.AudioConvertPreset
import com.arinara.photos.media.converter.ConversionProgressState
import com.arinara.photos.media.converter.VideoAudioConverterEngine
import com.arinara.photos.media.converter.VideoCodecChoice
import com.arinara.photos.media.converter.VideoFpsPreset
import com.arinara.photos.media.converter.VideoQualityProfile
import com.arinara.photos.media.converter.VideoResolutionPreset
import com.arinara.photos.media.share.MediaShareHelper
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppSlider
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

enum class VideoTheaterDrawerTab(val label: String) {
    CONTROLS_HUD("Controls"),
    CONVERT_QUALITY("Convert & Quality"),
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
    isPipMinimized: Boolean = false,
    onPipMinimizedChange: (Boolean) -> Unit = {},
    isInSystemPipMode: Boolean = false,
    pipActionTick: Int = 0,
    lastPipAction: PipCoreAction? = null,
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
    onOpenTrimmerForVideo: (MediaItem) -> Unit,
    onExportedConvertedMedia: (MediaItem, String) -> Unit = { _, _ -> }
) {
    if (!visible || videoItem == null) return

    val context = LocalContext.current
    val hostActivity = remember(context) { context.findHostActivity() }
    val coroutineScope = rememberCoroutineScope()
    val converterEngine = remember { VideoAudioConverterEngine() }

    // Live Playback Quality & FPS Switcher State (0 MB extra disk storage)
    var livePlaybackResolution by remember(videoItem.id) { mutableStateOf(VideoResolutionPreset.ORIGINAL) }
    var livePlaybackFps by remember(videoItem.id) { mutableStateOf(VideoFpsPreset.ORIGINAL) }

    // On-Demand Audio & Video Converter State
    var selectedAudioPreset by remember { mutableStateOf(AudioConvertPreset.MP3_320K) }
    var selectedVideoResolution by remember { mutableStateOf(VideoResolutionPreset.FHD_1080P) }
    var selectedVideoFps by remember { mutableStateOf(VideoFpsPreset.FPS_30) }
    var selectedVideoQuality by remember { mutableStateOf(VideoQualityProfile.BALANCED) }
    var selectedVideoCodec by remember { mutableStateOf(VideoCodecChoice.H264_AVC) }
    var conversionProgress by remember(videoItem.id) { mutableStateOf(ConversionProgressState()) }
    var activeConversionJob by remember { mutableStateOf<Job?>(null) }

    val videoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                .setMaxVideoBitrate(Int.MAX_VALUE)
                .build()
        }
    }

    var localIsPlaying by remember(videoItem.id) { mutableStateOf(true) }
    var localPositionMs by remember(videoItem.id) { mutableLongStateOf(videoItem.resumePositionMs) }
    var localDurationMs by remember(videoItem.id) { mutableLongStateOf(videoItem.durationMs.coerceAtLeast(1000L)) }
    var nativeVideoWidth by remember(videoItem.id) { mutableIntStateOf(videoItem.width) }
    var nativeVideoHeight by remember(videoItem.id) { mutableIntStateOf(videoItem.height) }
    var nativePixelRatio by remember(videoItem.id) { mutableFloatStateOf(1.0f) }

    val initialSystemBrightness = remember(context) {
        try {
            (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f)
                .coerceIn(0.05f, 1.0f)
        } catch (_: Throwable) {
            0.50f
        }
    }
    var isBrightnessOverridden by remember(videoItem.id) { mutableStateOf(false) }
    var brightnessLevel by remember(videoItem.id) { mutableFloatStateOf(initialSystemBrightness) }

    var activeTab by remember { mutableStateOf(VideoTheaterDrawerTab.CONTROLS_HUD) }
    var isFullscreenImmersion by remember { mutableStateOf(false) }
    var touchLockEnabled by remember { mutableStateOf(false) }
    var isHold2xActive by remember { mutableStateOf(false) }
    var volumeBoostPercent by remember { mutableIntStateOf(100) }
    var activeSpeed by remember(playbackSpeed) { mutableFloatStateOf(playbackSpeed.coerceIn(0.25f, 4.0f)) }
    var seekRippleCaption by remember { mutableStateOf<String?>(null) }

    var pipDragOffsetX by remember { mutableFloatStateOf(0f) }
    var pipDragOffsetY by remember { mutableFloatStateOf(0f) }

    var dualSubtitlesEnabled by remember { mutableStateOf(true) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var transcriptSearchQuery by remember { mutableStateOf("") }

    // Default to VideoAspectMode.FIT ("Original") so videos play at their exact native aspect ratio without stretching
    var aspectMode by remember { mutableStateOf(VideoAspectMode.FIT) }
    var videoZoomScale by remember(videoItem.id) { mutableFloatStateOf(1.0f) }
    var videoPanOffset by remember(videoItem.id) { mutableStateOf(Offset.Zero) }
    var mirrorFlipHorizontal by remember { mutableStateOf(false) }

    var videoAbStartMs by remember(videoItem.id) { mutableStateOf<Long?>(null) }
    var videoAbEndMs by remember(videoItem.id) { mutableStateOf<Long?>(null) }

    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    BackHandler(enabled = visible && !isPipMinimized && !isInSystemPipMode) {
        if (isFullscreenImmersion) {
            isFullscreenImmersion = false
        } else {
            try {
                videoPlayer.pause()
            } catch (_: Exception) {
            }
            onDismiss()
        }
    }

    // Listen to ExoPlayer events for exact original video dimensions, play/pause state, and audioSessionId
    DisposableEffect(videoPlayer, videoItem.id, videoItem.uriString) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    nativeVideoWidth = videoSize.width
                    nativeVideoHeight = videoSize.height
                    nativePixelRatio = videoSize.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1.0f
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                localIsPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = videoPlayer.duration
                if (dur > 0L) {
                    localDurationMs = dur
                }
            }
        }
        videoPlayer.addListener(listener)
        try {
            val file = File(videoItem.filePath)
            val mediaUri = if (file.exists()) Uri.fromFile(file) else Uri.parse(videoItem.uriString)
            val exoItem = ExoMediaItem.fromUri(mediaUri)
            videoPlayer.setMediaItem(exoItem)
            videoPlayer.prepare()
            if (videoItem.resumePositionMs > 0L) {
                videoPlayer.seekTo(videoItem.resumePositionMs)
            }
            videoPlayer.playbackParameters = PlaybackParameters(activeSpeed)
            videoPlayer.playWhenReady = true
            localIsPlaying = true
        } catch (_: Exception) {
        }

        try {
            val sessionId = videoPlayer.audioSessionId
            if (sessionId > 0) {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = volumeBoostPercent > 100
                }
            }
        } catch (_: Throwable) {
            loudnessEnhancer = null
        }

        onDispose {
            videoPlayer.removeListener(listener)
            try {
                videoPlayer.stop()
            } catch (_: Exception) {
            }
        }
    }

    // Apply zero-storage live playback resolution & FPS constraints on the fly (0 MB extra disk space)
    LaunchedEffect(livePlaybackResolution, livePlaybackFps, nativeVideoWidth, nativeVideoHeight, videoPlayer) {
        try {
            val builder = videoPlayer.trackSelectionParameters.buildUpon()
            if (livePlaybackResolution == VideoResolutionPreset.ORIGINAL) {
                builder.clearVideoSizeConstraints()
                builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                builder.setMaxVideoBitrate(Int.MAX_VALUE)
            } else {
                val (maxW, maxH) = VideoAudioConverterEngine.computeTargetVideoDimensions(
                    srcWidth = nativeVideoWidth.takeIf { it > 0 } ?: 3840,
                    srcHeight = nativeVideoHeight.takeIf { it > 0 } ?: 2160,
                    preset = livePlaybackResolution
                )
                val capBps = VideoAudioConverterEngine.computeTargetVideoBitrateBps(
                    width = maxW,
                    height = maxH,
                    fps = if (livePlaybackFps.targetFps > 0) livePlaybackFps.targetFps else 60,
                    quality = VideoQualityProfile.BALANCED,
                    codec = VideoCodecChoice.H264_AVC
                )
                builder.setMaxVideoSize(maxW, maxH)
                builder.setMaxVideoBitrate(capBps)
            }
            if (livePlaybackFps == VideoFpsPreset.ORIGINAL) {
                builder.setMaxVideoFrameRate(Int.MAX_VALUE)
            } else {
                builder.setMaxVideoFrameRate(livePlaybackFps.targetFps)
            }
            videoPlayer.trackSelectionParameters = builder.build()
        } catch (_: Throwable) {
        }
    }

    // Apply hardware screen brightness ONLY if the user explicitly overrides it (do not lock brightness on open)
    LaunchedEffect(isBrightnessOverridden, brightnessLevel, hostActivity) {
        try {
            hostActivity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = if (isBrightnessOverridden) {
                    brightnessLevel.coerceIn(0.01f, 1.0f)
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                win.attributes = lp
            }
        } catch (_: Throwable) {
        }
    }

    // Apply real volume & hardware LoudnessEnhancer (for 101%..200% boost)
    LaunchedEffect(volumeBoostPercent, loudnessEnhancer) {
        try {
            videoPlayer.volume = (volumeBoostPercent / 100f).coerceIn(0f, 1f)
            loudnessEnhancer?.let { le ->
                if (volumeBoostPercent > 100) {
                    val gainMb = ((volumeBoostPercent - 100) * 15).coerceIn(0, 1500)
                    le.enabled = true
                    le.setTargetGain(gainMb)
                } else {
                    le.setTargetGain(0)
                    le.enabled = false
                }
            }
        } catch (_: Throwable) {
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                hostActivity?.window?.let { win ->
                    val lp = win.attributes
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    win.attributes = lp
                }
            } catch (_: Throwable) {
            }
            try {
                loudnessEnhancer?.release()
            } catch (_: Throwable) {
            }
            try {
                videoPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    // Real-time playback position & A-B loop evaluation ticker
    LaunchedEffect(videoPlayer) {
        while (true) {
            try {
                val pos = videoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = videoPlayer.duration
                if (dur > 0L) {
                    localDurationMs = dur
                }
                val abStart = videoAbStartMs
                val abEnd = videoAbEndMs
                if (abStart != null && abEnd != null && abEnd > abStart && pos >= abEnd) {
                    videoPlayer.seekTo(abStart)
                    localPositionMs = abStart
                    onSeekToMs(abStart)
                } else {
                    localPositionMs = pos
                    onSeekToMs(pos)
                }
            } catch (_: Exception) {
            }
            delay(150L)
        }
    }

    val safeDuration = localDurationMs.coerceAtLeast(1000L)
    val activeCue = remember(subtitleCues, localPositionMs, subtitleOffsetMs) {
        VideoTheaterEngine.findActiveSubtitleCue(subtitleCues, localPositionMs, subtitleOffsetMs)
    }
    val filteredTranscript = remember(subtitleCues, transcriptSearchQuery) {
        VideoTheaterEngine.searchSubtitleTranscript(subtitleCues, transcriptSearchQuery)
    }

    val nativeAspectRatio = remember(nativeVideoWidth, nativeVideoHeight, nativePixelRatio) {
        if (nativeVideoWidth > 0 && nativeVideoHeight > 0) {
            ((nativeVideoWidth.toFloat() * nativePixelRatio) / nativeVideoHeight.toFloat()).coerceIn(0.35f, 3.2f)
        } else {
            null
        }
    }
    val isPortraitVideo = (nativeAspectRatio ?: 1.77f) < 1.0f

    val currentTouchLocked by rememberUpdatedState(touchLockEnabled)
    val currentPosMs by rememberUpdatedState(localPositionMs)
    val currentDurMs by rememberUpdatedState(safeDuration)
    val currentBrightness by rememberUpdatedState(brightnessLevel)
    val currentVolPercent by rememberUpdatedState(volumeBoostPercent)
    val currentZoom by rememberUpdatedState(videoZoomScale)
    val currentPan by rememberUpdatedState(videoPanOffset)
    val currentSpeed by rememberUpdatedState(activeSpeed)
    val currentFullscreen by rememberUpdatedState(isFullscreenImmersion)

    val handleGoLeft: () -> Unit = {
        val target = (localPositionMs - 10_000L).coerceAtLeast(0L)
        videoPlayer.seekTo(target)
        localPositionMs = target
        onSeekToMs(target)
    }

    val handlePlayPause: () -> Unit = {
        if (videoPlayer.isPlaying) {
            videoPlayer.pause()
            localIsPlaying = false
        } else {
            videoPlayer.play()
            localIsPlaying = true
        }
        onPlayPauseToggle()
    }

    val handleGoRight: () -> Unit = {
        val target = (localPositionMs + 10_000L).coerceAtMost(safeDuration)
        videoPlayer.seekTo(target)
        localPositionMs = target
        onSeekToMs(target)
    }

    // Respond to system Picture-in-Picture RemoteAction triggers (Go Left, Play/Pause, Go Right)
    LaunchedEffect(pipActionTick) {
        if (pipActionTick > 0) {
            when (lastPipAction) {
                PipCoreAction.GO_LEFT -> handleGoLeft()
                PipCoreAction.PLAY_PAUSE -> handlePlayPause()
                PipCoreAction.GO_RIGHT -> handleGoRight()
                null -> Unit
            }
        }
    }

    // Picture-in-Picture Minimized Mode (In-App Floating PiP or System PiP):
    // Minimizes the video player so ONLY the core buttons are active: Play/Pause, Go Left, and Go Right
    if (isPipMinimized || isInSystemPipMode) {
        val currentPipX by rememberUpdatedState(pipDragOffsetX)
        val currentPipY by rememberUpdatedState(pipDragOffsetY)

        Box(
            modifier = if (isInSystemPipMode) {
                Modifier
                    .fillMaxSize()
                    .background(ElectricSlateTokens.BgCanvas)
            } else {
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .statusBarsPadding()
                    .padding(end = 16.dp, bottom = 92.dp)
            },
            contentAlignment = if (isInSystemPipMode) Alignment.Center else Alignment.BottomEnd
        ) {
            val pipCardModifier = if (isInSystemPipMode) {
                Modifier
                    .fillMaxSize()
                    .background(ElectricSlateTokens.BgCanvas)
            } else {
                Modifier
                    .offset { IntOffset(pipDragOffsetX.roundToInt(), pipDragOffsetY.roundToInt()) }
                    .width(264.dp)
                    .height(176.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElectricSlateTokens.SurfacePanel)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pipDragOffsetX = (currentPipX + dragAmount.x).coerceIn(-520f, 120f)
                            pipDragOffsetY = (currentPipY + dragAmount.y).coerceIn(-1200f, 180f)
                        }
                    }
            }

            Box(
                modifier = pipCardModifier,
                contentAlignment = Alignment.Center
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
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top-Right Expand & Close Mini Header (hidden when in OS system PiP window)
                if (!isInSystemPipMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.78f))
                                .jigglePressClickable { onPipMinimizedChange(false) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fullscreen,
                                contentDescription = "Expand Video",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.78f))
                                .jigglePressClickable {
                                    try {
                                        videoPlayer.pause()
                                    } catch (_: Exception) {
                                    }
                                    onPipMinimizedChange(false)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close PiP",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Bottom Core Transport Bar: ONLY Go Left, Play/Pause, and Go Right active
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.82f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Core Button 1: Go Left (-10s)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable { handleGoLeft() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FastRewind,
                                contentDescription = "Go Left",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Left",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }
                    }

                    // Core Button 2: Play / Pause
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent)
                            .jigglePressClickable { handlePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (localIsPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Core Button 3: Go Right (+10s)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable { handleGoRight() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Right",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Outlined.FastForward,
                                contentDescription = "Go Right",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Full-Screen Edge-to-Edge Video Theater Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ElectricSlateTokens.BgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Top Header Bar (Hidden when in 100% Fullscreen Immersion Mode)
        AnimatedVisibility(
            visible = !isFullscreenImmersion,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = videoItem.title,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val resText = if (nativeVideoWidth > 0 && nativeVideoHeight > 0) {
                        "${nativeVideoWidth}×${nativeVideoHeight} • "
                    } else {
                        ""
                    }
                    val liveQualityBadge = if (livePlaybackResolution != VideoResolutionPreset.ORIGINAL || livePlaybackFps != VideoFpsPreset.ORIGINAL) {
                        " • Live: ${livePlaybackResolution.label} ${livePlaybackFps.label}"
                    } else {
                        ""
                    }
                    Text(
                        text = "${resText}${aspectMode.label} • ${String.format("%.2fx", activeSpeed)}$liveQualityBadge",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable {
                                MediaShareHelper.shareMediaItem(context, videoItem)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Video",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable {
                                isFullscreenImmersion = false
                                onPipMinimizedChange(true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureInPictureAlt,
                            contentDescription = "Minimize Picture in Picture",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable { isFullscreenImmersion = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable {
                                try {
                                    videoPlayer.pause()
                                } catch (_: Exception) {
                                }
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Video Player",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Native-Ratio Video Viewport (Full-Screen in Immersion mode, or adaptive native-ratio stage above controls)
        val viewportContainerModifier = if (isFullscreenImmersion) {
            Modifier
                .fillMaxSize()
                .background(ElectricSlateTokens.BgCanvas)
        } else {
            Modifier
                .fillMaxWidth()
                .height(if (isPortraitVideo) 360.dp else 236.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ElectricSlateTokens.SurfacePanel)
        }

        Box(
            modifier = viewportContainerModifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!currentTouchLocked && currentFullscreen) {
                                isFullscreenImmersion = false
                            }
                        },
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
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (isHold2xActive) {
                                isHold2xActive = false
                                videoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
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
                            isBrightnessOverridden = true
                            brightnessLevel = VideoTheaterEngine.applyBrightnessGestureDelta(currentBrightness, delta)
                        } else {
                            val deltaPct = (-dragAmount / 3.5f).roundToInt()
                            val nextVol = VideoTheaterEngine.applyVolumeBoostGestureDelta(currentVolPercent, deltaPct)
                            volumeBoostPercent = nextVol
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (currentTouchLocked) return@detectTransformGestures
                        val nextZoom = (currentZoom * zoom).coerceIn(1f, 8f)
                        videoZoomScale = nextZoom
                        videoPanOffset = if (nextZoom > 1.02f) {
                            Offset(
                                x = (currentPan.x + pan.x).coerceIn(-600f, 600f),
                                y = (currentPan.y + pan.y).coerceIn(-500f, 500f)
                            )
                        } else Offset.Zero
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val targetAspectRatio: Float? = when (aspectMode) {
                VideoAspectMode.FIT -> nativeAspectRatio
                VideoAspectMode.FILL_CROP -> null
                VideoAspectMode.RATIO_16_9 -> 16f / 9f
                VideoAspectMode.RATIO_21_9 -> 21f / 9f
                VideoAspectMode.RATIO_4_3 -> 4f / 3f
            }

            val playerFrameModifier = if (targetAspectRatio != null) {
                Modifier.aspectRatio(targetAspectRatio, matchHeightConstraintsFirst = isPortraitVideo)
            } else {
                Modifier.fillMaxSize()
            }

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
                        VideoAspectMode.RATIO_16_9,
                        VideoAspectMode.RATIO_21_9,
                        VideoAspectMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = playerFrameModifier
                    .graphicsLayer {
                        scaleX = (if (mirrorFlipHorizontal) -1f else 1f) * videoZoomScale
                        scaleY = videoZoomScale
                        translationX = videoPanOffset.x
                        translationY = videoPanOffset.y
                    }
            )

            // Software + Extra-Dim Brightness Scrim (ONLY applied when user explicitly overrides brightness)
            if (isBrightnessOverridden && brightnessLevel < 0.99f) {
                val dimAlpha = if (brightnessLevel >= 0f) {
                    ((1.0f - brightnessLevel) * 0.55f).coerceIn(0f, 0.55f)
                } else {
                    (0.55f + abs(brightnessLevel) * 0.40f).coerceIn(0.55f, 0.88f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ElectricSlateTokens.BgCanvas.copy(alpha = dimAlpha))
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
                    .padding(12.dp)
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

            // Top-Right Status Pill + Fullscreen Toggle
            if (!touchLockEnabled) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val hudLabel = when {
                            isHold2xActive -> "2.0x"
                            seekRippleCaption != null -> seekRippleCaption!!
                            else -> "${String.format("%.2f", activeSpeed).trimEnd('0').trimEnd('.')}x"
                        }
                        Text(
                            text = hudLabel,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.88f))
                            .jigglePressClickable { isFullscreenImmersion = !isFullscreenImmersion }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFullscreenImmersion) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            if (isFullscreenImmersion) {
                                Text(
                                    text = "Exit",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Skip +85s button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (isFullscreenImmersion) 84.dp else 12.dp, end = 12.dp)
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
                        .padding(bottom = if (isFullscreenImmersion) 88.dp else 12.dp, start = 24.dp, end = 88.dp)
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

            // Floating Quick-Transport Overlay when in Fullscreen Immersion Mode
            if (isFullscreenImmersion && !touchLockEnabled) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.86f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent)
                            .jigglePressClickable {
                                if (videoPlayer.isPlaying) {
                                    videoPlayer.pause()
                                    localIsPlaying = false
                                } else {
                                    videoPlayer.play()
                                    localIsPlaying = true
                                }
                                onPlayPauseToggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (localIsPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = MediaItem.formatDurationMillis(localPositionMs),
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                    AppSlider(
                        value = localPositionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                        onValueChange = {
                            val target = it.toLong()
                            videoPlayer.seekTo(target)
                            localPositionMs = target
                            onSeekToMs(target)
                        },
                        valueRange = 0f..safeDuration.toFloat(),
                        steps = 0,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = MediaItem.formatDurationMillis(safeDuration),
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }
            }
        }

        if (isFullscreenImmersion) {
            return@Column
        }

        // 3. Scrollable Controls & Settings Drawer below Video Viewport
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (touchLockEnabled) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentPadding = 12.dp
                ) {
                    Text(
                        text = "Screen locked. Tap 'Locked' in the top-left corner of the video to unlock controls.",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    AppButton(
                        text = "Fullscreen",
                        leadingIcon = Icons.Outlined.Fullscreen,
                        onClick = { isFullscreenImmersion = true },
                        variant = AppButtonVariant.SECONDARY,
                        height = 36.dp,
                        horizontalPadding = 10.dp,
                        fontSize = 12.sp
                    )
                    AppButton(
                        text = "PiP",
                        leadingIcon = Icons.Outlined.PictureInPictureAlt,
                        onClick = {
                            isFullscreenImmersion = false
                            onPipMinimizedChange(true)
                        },
                        variant = AppButtonVariant.SECONDARY,
                        height = 36.dp,
                        horizontalPadding = 10.dp,
                        fontSize = 12.sp
                    )
                    AppButton(
                        text = "System PiP",
                        leadingIcon = Icons.Outlined.PictureInPictureAlt,
                        onClick = {
                            isFullscreenImmersion = false
                            onEnterPipMode()
                        },
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
                    AppButton(
                        text = "Convert / MP3",
                        leadingIcon = Icons.Outlined.HighQuality,
                        onClick = {
                            activeTab = VideoTheaterDrawerTab.CONVERT_QUALITY
                        },
                        variant = AppButtonVariant.SECONDARY,
                        height = 36.dp,
                        horizontalPadding = 10.dp,
                        fontSize = 12.sp
                    )
                    AppButton(
                        text = "Share",
                        leadingIcon = Icons.Outlined.Share,
                        onClick = {
                            MediaShareHelper.shareMediaItem(context, videoItem)
                        },
                        variant = AppButtonVariant.SECONDARY,
                        height = 36.dp,
                        horizontalPadding = 10.dp,
                        fontSize = 12.sp
                    )
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBrightnessOverridden) {
                                    "Brightness (${(brightnessLevel * 100).roundToInt()}%)"
                                } else {
                                    "Brightness (System Default)"
                                },
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AppPillChip(
                                    label = "System Auto",
                                    isSelected = !isBrightnessOverridden,
                                    onClick = {
                                        isBrightnessOverridden = false
                                        brightnessLevel = initialSystemBrightness
                                    }
                                )
                                AppPillChip(
                                    label = "Extra-Dim",
                                    isSelected = isBrightnessOverridden && brightnessLevel < 0f,
                                    onClick = {
                                        isBrightnessOverridden = true
                                        brightnessLevel = -0.45f
                                    }
                                )
                            }
                        }
                        AppSlider(
                            value = brightnessLevel,
                            onValueChange = {
                                isBrightnessOverridden = true
                                brightnessLevel = it
                            },
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
                                volumeBoostPercent = it.roundToInt()
                            },
                            valueRange = 0f..200f,
                            steps = 0
                        )
                        Text(
                            text = "Speed (${String.format("%.2fx", activeSpeed)})",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                AppPillChip(
                                    label = "${spd}x",
                                    isSelected = abs(activeSpeed - spd) < 0.04f,
                                    onClick = {
                                        activeSpeed = spd
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

                VideoTheaterDrawerTab.CONVERT_QUALITY -> {
                    val srcW = nativeVideoWidth.takeIf { it > 0 } ?: videoItem.width.takeIf { it > 0 } ?: 1920
                    val srcH = nativeVideoHeight.takeIf { it > 0 } ?: videoItem.height.takeIf { it > 0 } ?: 1080
                    val srcFps = videoItem.fps.takeIf { it > 0f } ?: 30f

                    // 1. Live Conversion Progress Card (Shown when running or completed)
                    if (conversionProgress.isRunning || conversionProgress.completedItem != null || conversionProgress.errorMessage != null) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlow = conversionProgress.isRunning,
                            cornerRadius = 16.dp,
                            contentPadding = 12.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (conversionProgress.isRunning) {
                                        "Converting (${conversionProgress.progressPercent}%)"
                                    } else if (conversionProgress.errorMessage != null) {
                                        "Conversion Error"
                                    } else {
                                        "Conversion Complete (100%)"
                                    },
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (conversionProgress.errorMessage != null) {
                                        ElectricSlateTokens.Destructive
                                    } else {
                                        ElectricSlateTokens.PrimaryAccent
                                    }
                                )
                                Text(
                                    text = "${MediaItem.formatDurationMillis(conversionProgress.processedDurationMs)} / ${MediaItem.formatDurationMillis(safeDuration)}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = conversionProgress.errorMessage ?: conversionProgress.statusLabel,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Determinate 0%..100% Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.TrackInactive)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(conversionProgress.progressFraction.coerceIn(0.02f, 1f))
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(ElectricSlateTokens.PrimaryAccent)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (conversionProgress.isRunning) {
                                    AppButton(
                                        text = "Cancel Conversion",
                                        onClick = {
                                            activeConversionJob?.cancel()
                                            conversionProgress = ConversionProgressState(
                                                isRunning = false,
                                                progressFraction = 0f,
                                                statusLabel = "Conversion cancelled"
                                            )
                                        },
                                        variant = AppButtonVariant.DESTRUCTIVE,
                                        height = 36.dp,
                                        fontSize = 12.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (conversionProgress.completedItem != null) {
                                    val doneItem = conversionProgress.completedItem!!
                                    AppButton(
                                        text = "Share Converted File...",
                                        leadingIcon = Icons.Outlined.Share,
                                        onClick = {
                                            val file = conversionProgress.completedFile
                                            if (file != null && file.exists()) {
                                                MediaShareHelper.shareFile(context, file, doneItem.mimeType, doneItem.title)
                                            } else {
                                                MediaShareHelper.shareMediaItem(context, doneItem)
                                            }
                                        },
                                        variant = AppButtonVariant.PRIMARY,
                                        height = 36.dp,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Zero-Storage Live Playback Quality & FPS Switcher (0 MB Disk Used)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Live Playback Quality & FPS (0 MB Extra Storage)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Native source: ${srcW}×${srcH} @ ${srcFps.roundToInt()}fps • Switching live quality scales playback on the GPU without duplicating files.",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoResolutionPreset.entries.forEach { res ->
                                AppPillChip(
                                    label = res.label,
                                    isSelected = livePlaybackResolution == res,
                                    onClick = { livePlaybackResolution = res }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoFpsPreset.entries.forEach { fps ->
                                AppPillChip(
                                    label = fps.label,
                                    isSelected = livePlaybackFps == fps,
                                    onClick = { livePlaybackFps = fps }
                                )
                            }
                        }
                    }

                    // 3. Convert Video to Audio (MP3 / M4A / WAV) Card
                    val estAudioBytes = remember(safeDuration, selectedAudioPreset) {
                        VideoAudioConverterEngine.estimateAudioOutputSizeBytes(safeDuration, selectedAudioPreset)
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
                                text = "Convert Video to MP3 / Audio",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = "Est. ~${VideoAudioConverterEngine.formatEstimatedSizeMb(estAudioBytes)}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedAudioPreset.detail,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AudioConvertPreset.entries.forEach { preset ->
                                AppPillChip(
                                    label = preset.label,
                                    isSelected = selectedAudioPreset == preset,
                                    onClick = { selectedAudioPreset = preset }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "Convert & Save",
                                leadingIcon = Icons.Outlined.MusicNote,
                                onClick = {
                                    if (conversionProgress.isRunning) return@AppButton
                                    activeConversionJob = coroutineScope.launch {
                                        val outDir = File(context.filesDir, "converted_audio")
                                        val finalState = withContext(Dispatchers.IO) {
                                            converterEngine.convertVideoToAudio(
                                                context = context,
                                                outputDir = outDir,
                                                videoItem = videoItem.copy(durationMs = safeDuration),
                                                preset = selectedAudioPreset,
                                                temporaryShareOnly = false,
                                                onProgress = { state -> conversionProgress = state }
                                            )
                                        }
                                        conversionProgress = finalState
                                        finalState.completedItem?.let { item ->
                                            onExportedConvertedMedia(item, finalState.statusLabel)
                                        }
                                    }
                                },
                                variant = AppButtonVariant.PRIMARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Convert & Share...",
                                leadingIcon = Icons.Outlined.Share,
                                onClick = {
                                    if (conversionProgress.isRunning) return@AppButton
                                    activeConversionJob = coroutineScope.launch {
                                        val outDir = File(context.cacheDir, "converted_share")
                                        val finalState = withContext(Dispatchers.IO) {
                                            converterEngine.convertVideoToAudio(
                                                context = context,
                                                outputDir = outDir,
                                                videoItem = videoItem.copy(durationMs = safeDuration),
                                                preset = selectedAudioPreset,
                                                temporaryShareOnly = true,
                                                onProgress = { state -> conversionProgress = state }
                                            )
                                        }
                                        conversionProgress = finalState
                                        val shareFile = finalState.completedFile
                                        val doneItem = finalState.completedItem
                                        if (shareFile != null && doneItem != null) {
                                            MediaShareHelper.shareFile(context, shareFile, doneItem.mimeType, doneItem.title)
                                        }
                                    }
                                },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. On-Demand Multi-Quality & Multi-FPS Video Converter Card
                    val (targetW, targetH) = remember(srcW, srcH, selectedVideoResolution) {
                        VideoAudioConverterEngine.computeTargetVideoDimensions(srcW, srcH, selectedVideoResolution)
                    }
                    val targetFpsVal = remember(srcFps, selectedVideoFps) {
                        VideoAudioConverterEngine.resolveTargetFps(srcFps, selectedVideoFps)
                    }
                    val estVideoBytes = remember(
                        safeDuration,
                        srcW,
                        srcH,
                        srcFps,
                        selectedVideoResolution,
                        selectedVideoFps,
                        selectedVideoQuality,
                        selectedVideoCodec
                    ) {
                        VideoAudioConverterEngine.estimateVideoOutputSizeBytes(
                            durationMs = safeDuration,
                            srcWidth = srcW,
                            srcHeight = srcH,
                            srcFps = srcFps,
                            resPreset = selectedVideoResolution,
                            fpsPreset = selectedVideoFps,
                            quality = selectedVideoQuality,
                            codec = selectedVideoCodec
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
                                text = "Convert Video Resolution & FPS",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = "${targetW}×${targetH} @ ${targetFpsVal}fps (~${VideoAudioConverterEngine.formatEstimatedSizeMb(estVideoBytes)})",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Target Resolution:",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoResolutionPreset.entries.forEach { res ->
                                AppPillChip(
                                    label = res.label,
                                    isSelected = selectedVideoResolution == res,
                                    onClick = { selectedVideoResolution = res }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Target Frame Rate (FPS):",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoFpsPreset.entries.forEach { fps ->
                                AppPillChip(
                                    label = fps.label,
                                    isSelected = selectedVideoFps == fps,
                                    onClick = { selectedVideoFps = fps }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Quality Profile & Codec:",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoQualityProfile.entries.forEach { q ->
                                AppPillChip(
                                    label = q.label,
                                    isSelected = selectedVideoQuality == q,
                                    onClick = { selectedVideoQuality = q }
                                )
                            }
                            VideoCodecChoice.entries.forEach { c ->
                                AppPillChip(
                                    label = c.label,
                                    isSelected = selectedVideoCodec == c,
                                    onClick = { selectedVideoCodec = c }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = "Convert & Save",
                                leadingIcon = Icons.Outlined.HighQuality,
                                onClick = {
                                    if (conversionProgress.isRunning) return@AppButton
                                    activeConversionJob = coroutineScope.launch {
                                        val outDir = File(context.filesDir, "converted_video")
                                        val finalState = withContext(Dispatchers.IO) {
                                            converterEngine.transcodeVideoMultiQuality(
                                                context = context,
                                                outputDir = outDir,
                                                videoItem = videoItem.copy(
                                                    width = srcW,
                                                    height = srcH,
                                                    fps = srcFps,
                                                    durationMs = safeDuration
                                                ),
                                                resolutionPreset = selectedVideoResolution,
                                                fpsPreset = selectedVideoFps,
                                                qualityProfile = selectedVideoQuality,
                                                codecChoice = selectedVideoCodec,
                                                temporaryShareOnly = false,
                                                onProgress = { state -> conversionProgress = state }
                                            )
                                        }
                                        conversionProgress = finalState
                                        finalState.completedItem?.let { item ->
                                            onExportedConvertedMedia(item, finalState.statusLabel)
                                        }
                                    }
                                },
                                variant = AppButtonVariant.PRIMARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Convert & Share...",
                                leadingIcon = Icons.Outlined.Share,
                                onClick = {
                                    if (conversionProgress.isRunning) return@AppButton
                                    activeConversionJob = coroutineScope.launch {
                                        val outDir = File(context.cacheDir, "converted_share")
                                        val finalState = withContext(Dispatchers.IO) {
                                            converterEngine.transcodeVideoMultiQuality(
                                                context = context,
                                                outputDir = outDir,
                                                videoItem = videoItem.copy(
                                                    width = srcW,
                                                    height = srcH,
                                                    fps = srcFps,
                                                    durationMs = safeDuration
                                                ),
                                                resolutionPreset = selectedVideoResolution,
                                                fpsPreset = selectedVideoFps,
                                                qualityProfile = selectedVideoQuality,
                                                codecChoice = selectedVideoCodec,
                                                temporaryShareOnly = true,
                                                onProgress = { state -> conversionProgress = state }
                                            )
                                        }
                                        conversionProgress = finalState
                                        val shareFile = finalState.completedFile
                                        val doneItem = finalState.completedItem
                                        if (shareFile != null && doneItem != null) {
                                            MediaShareHelper.shareFile(context, shareFile, doneItem.mimeType, doneItem.title)
                                        }
                                    }
                                },
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
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
                            text = "Aspect Ratio & Original Quality",
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Zoom ${(videoZoomScale * 100).roundToInt()}%",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppSlider(
                                value = videoZoomScale,
                                onValueChange = {
                                    videoZoomScale = it
                                    if (it <= 1.05f) videoPanOffset = Offset.Zero
                                },
                                valueRange = 1f..8f,
                                steps = 0,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Reset",
                                onClick = {
                                    videoZoomScale = 1f
                                    videoPanOffset = Offset.Zero
                                },
                                variant = AppButtonVariant.SECONDARY,
                                height = 34.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppPillChip(
                                label = if (mirrorFlipHorizontal) "Mirrored" else "Mirror Video",
                                leadingIcon = Icons.Outlined.Flip,
                                isSelected = mirrorFlipHorizontal,
                                onClick = { mirrorFlipHorizontal = !mirrorFlipHorizontal }
                            )
                            AppButton(
                                text = "-1 Frame",
                                onClick = {
                                    videoPlayer.pause()
                                    localIsPlaying = false
                                    val target = VideoTheaterEngine.computeFrameStepTargetMs(
                                        localPositionMs,
                                        videoItem.fps,
                                        -1,
                                        safeDuration
                                    )
                                    videoPlayer.seekTo(target)
                                    localPositionMs = target
                                    onSeekToMs(target)
                                },
                                variant = AppButtonVariant.SECONDARY,
                                height = 34.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                            AppButton(
                                text = "+1 Frame",
                                onClick = {
                                    videoPlayer.pause()
                                    localIsPlaying = false
                                    val target = VideoTheaterEngine.computeFrameStepTargetMs(
                                        localPositionMs,
                                        videoItem.fps,
                                        1,
                                        safeDuration
                                    )
                                    videoPlayer.seekTo(target)
                                    localPositionMs = target
                                    onSeekToMs(target)
                                },
                                variant = AppButtonVariant.SECONDARY,
                                height = 34.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                            AppButton(
                                text = "Save Frame",
                                leadingIcon = Icons.Outlined.CameraAlt,
                                onClick = { onCaptureFramePng(videoItem, localPositionMs) },
                                variant = AppButtonVariant.SECONDARY,
                                height = 34.dp,
                                horizontalPadding = 10.dp,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
