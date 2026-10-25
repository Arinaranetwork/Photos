// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.photos.data.model.CueTrackSlice
import com.arinara.photos.data.model.LrcLyricLine
import com.arinara.photos.data.model.MediaFilter
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import com.arinara.photos.data.model.SortOrder
import com.arinara.photos.data.repository.MediaRepository
import com.arinara.photos.media.AudioDspController
import com.arinara.photos.media.LoopTickDirective
import com.arinara.photos.media.MultiQueueManager
import com.arinara.photos.media.SubtitleCue
import com.arinara.photos.media.VideoTheaterEngine
import com.arinara.photos.media.inspector.DeepMediaInspectionReport
import com.arinara.photos.media.inspector.DuplicateGroup
import com.arinara.photos.media.inspector.MediaInspectorAndVaultEngine
import com.arinara.photos.media.trimmer.MediaTrimmerEngine
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppDropdownMenu
import com.arinara.photos.ui.components.presets.AppDropdownMenuItem
import com.arinara.photos.ui.components.presets.AppFloatingBar
import com.arinara.photos.ui.components.presets.AppFloatingBarItem
import com.arinara.photos.ui.components.presets.AppListItem
import com.arinara.photos.ui.components.presets.AppListItemAction
import com.arinara.photos.ui.components.presets.AppNoticeBanner
import com.arinara.photos.ui.components.presets.AppNoticeVariant
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppScreen
import com.arinara.photos.ui.components.presets.AppTopBar
import com.arinara.photos.ui.gallery.PhotoCompareAndLoupeSheet
import com.arinara.photos.ui.inspector.InspectorTab
import com.arinara.photos.ui.inspector.MediaInspectorAndVaultSheet
import com.arinara.photos.ui.player.AudioEqStudioSheet
import com.arinara.photos.ui.player.AudiophilePlayerSheet
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable
import com.arinara.photos.ui.video.VideoTheaterSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PhotosNavTab {
    HOME,
    ALBUMS,
    EQ_DECK
}

private val HOME_FILTER_CHIPS = listOf(
    MediaFilter.ALL,
    MediaFilter.PHOTOS,
    MediaFilter.VIDEO,
    MediaFilter.AUDIO_LOSSLESS,
    MediaFilter.FAVORITES
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onRequestPipMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val mediaRepository = remember(context) { MediaRepository(context) }
    val multiQueueManager = remember { MultiQueueManager() }
    val audioDspController = remember { AudioDspController() }
    val videoTheaterEngine = remember { VideoTheaterEngine() }
    val mediaTrimmerEngine = remember { MediaTrimmerEngine() }
    val inspectorAndVaultEngine = remember { MediaInspectorAndVaultEngine() }

    val allMediaItems by mediaRepository.allItems.collectAsState()
    val activeQueueState by multiQueueManager.activeQueueState.collectAsState()
    val loopMode by multiQueueManager.loopMode.collectAsState()
    val abLoopState by multiQueueManager.abLoopState.collectAsState()
    val bookmarks by multiQueueManager.bookmarks.collectAsState()
    val dspState by audioDspController.dspState.collectAsState()

    var selectedNavTab by remember { mutableStateOf(PhotosNavTab.HOME) }
    var selectedFilter by remember { mutableStateOf(MediaFilter.ALL) }
    var selectedSortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchBarVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var gridColumns by remember { mutableIntStateOf(2) }

    var statusNoticeText by remember { mutableStateOf<String?>(null) }
    var hasMediaPermission by remember { mutableStateOf(false) }

    // Active Modal Sheets
    var showPhotoCompareSheet by remember { mutableStateOf(false) }
    var selectedPhotoItem by remember { mutableStateOf<MediaItem?>(null) }

    var showAudiophilePlayerSheet by remember { mutableStateOf(false) }
    var showEqStudioSheet by remember { mutableStateOf(false) }
    var isEqSheetMinimized by remember { mutableStateOf(false) }

    var showVideoTheaterSheet by remember { mutableStateOf(false) }
    var activeVideoItem by remember { mutableStateOf<MediaItem?>(null) }

    var showInspectorVaultSheet by remember { mutableStateOf(false) }
    var inspectorInitialTab by remember { mutableStateOf(InspectorTab.HISTOGRAM_EXIF) }
    var inspectorTargetItem by remember { mutableStateOf<MediaItem?>(null) }
    var vaultUnlockedState by remember { mutableStateOf(false) }

    // Playback State (Audio & Video)
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioPositionMs by remember { mutableLongStateOf(0L) }
    var audioDurationMs by remember { mutableLongStateOf(0L) }
    var sleepTimerRemainingMin by remember { mutableIntStateOf(0) }
    var spectrumBins by remember { mutableStateOf(FloatArray(32)) }
    var waveformSamples by remember { mutableStateOf(FloatArray(64)) }

    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkMediaPermissionsGranted(): Boolean {
        return requiredPermissions.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun syncDeviceMedia() {
        hasMediaPermission = checkMediaPermissionsGranted()
        val loaded = mediaRepository.refreshMediaLibrary()
        val audioTracks = loaded.filter { it.mediaType == MediaType.AUDIO && !it.isTrashed }
        if (multiQueueManager.activeQueueState.value.tracks.isEmpty() && audioTracks.isNotEmpty()) {
            multiQueueManager.seedQueues(audioTracks)
        }
    }

    // Real ExoPlayer instance for device audio playback
    val exoPlayer = remember(context) {
        try {
            ExoPlayer.Builder(context).build().also { player ->
                audioDspController.bindAudioSession(player.audioSessionId)
            }
        } catch (_: Throwable) {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            audioDspController.releaseSystemEffects()
            try {
                exoPlayer?.release()
            } catch (_: Throwable) {
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coroutineScope.launch {
            syncDeviceMedia()
        }
    }

    // Auto-request permissions & load real device media on launch
    LaunchedEffect(Unit) {
        if (checkMediaPermissionsGranted()) {
            syncDeviceMedia()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Auto-refresh when returning to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    syncDeviceMedia()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentAudioTrack = activeQueueState.currentTrack

    // Sync currentAudioTrack to ExoPlayer
    LaunchedEffect(currentAudioTrack?.id) {
        val track = currentAudioTrack ?: return@LaunchedEffect
        audioPositionMs = activeQueueState.savedPositionMs
        audioDurationMs = track.durationMs.coerceAtLeast(1000L)
        try {
            exoPlayer?.let { player ->
                val f = File(track.filePath)
                val uri = if (f.exists()) Uri.fromFile(f) else Uri.parse(track.uriString)
                player.setMediaItem(ExoMediaItem.fromUri(uri))
                player.prepare()
                if (audioPositionMs > 0L) player.seekTo(audioPositionMs)
                player.playWhenReady = isAudioPlaying
            }
        } catch (_: Throwable) {
        }
    }

    // Sync play/pause & speed/pitch to ExoPlayer
    LaunchedEffect(isAudioPlaying, dspState.playbackSpeed, dspState.effectivePitchMultiplier, dspState.skipSilenceEnabled) {
        try {
            exoPlayer?.let { player ->
                player.playWhenReady = isAudioPlaying
                player.skipSilenceEnabled = dspState.skipSilenceEnabled
                player.playbackParameters = PlaybackParameters(
                    dspState.playbackSpeed,
                    dspState.effectivePitchMultiplier
                )
            }
        } catch (_: Throwable) {
        }
    }

    // Immediate real-time EQ spectrum/waveform refresh whenever dspState changes while EQ Studio is open
    LaunchedEffect(dspState, showEqStudioSheet, isAudioPlaying) {
        if (showEqStudioSheet) {
            if (isAudioPlaying && currentAudioTrack != null) {
                spectrumBins = audioDspController.generateSpectrumFrame(
                    playbackPositionMs = audioPositionMs,
                    isPlaying = true,
                    isLossless = currentAudioTrack.isLossless
                )
                waveformSamples = audioDspController.captureWaveformFrame(isPlaying = true)
            } else {
                spectrumBins = FloatArray(32)
                waveformSamples = FloatArray(64)
            }
        }
    }

    // Efficient Audio Position, Loop Evaluation & Real Hardware Visualizer Ticker
    // Runs ONLY while audio is actively playing to eliminate idle UI recomposition lag
    LaunchedEffect(isAudioPlaying, currentAudioTrack?.id, showEqStudioSheet, showAudiophilePlayerSheet) {
        if (!isAudioPlaying || currentAudioTrack == null) {
            if (showEqStudioSheet) {
                spectrumBins = FloatArray(32)
                waveformSamples = FloatArray(64)
            }
            return@LaunchedEffect
        }
        while (isAudioPlaying) {
            val tickMs = when {
                showEqStudioSheet -> 33L
                showAudiophilePlayerSheet -> 120L
                else -> 500L
            }
            delay(tickMs)
            val realDur = try {
                exoPlayer?.duration?.takeIf { it > 0L }
            } catch (_: Throwable) {
                null
            } ?: currentAudioTrack.durationMs.coerceAtLeast(1000L)
            if (audioDurationMs != realDur) {
                audioDurationMs = realDur
            }

            val realPos = try {
                exoPlayer?.currentPosition?.coerceAtLeast(0L)
            } catch (_: Throwable) {
                null
            } ?: audioPositionMs
            audioPositionMs = realPos.coerceAtMost(realDur)

            when (val directive = multiQueueManager.evaluatePositionTick(audioPositionMs, realDur)) {
                is LoopTickDirective.SeekToMs -> {
                    audioPositionMs = directive.targetMs
                    try {
                        exoPlayer?.seekTo(directive.targetMs)
                    } catch (_: Throwable) {
                    }
                }
                is LoopTickDirective.AdvanceToTrack -> {
                    multiQueueManager.selectTrackAt(directive.nextIndex, resetAbLoop = false)
                    audioPositionMs = 0L
                    try {
                        exoPlayer?.seekTo(0L)
                    } catch (_: Throwable) {
                    }
                }
                is LoopTickDirective.StopEndOfQueue -> {
                    isAudioPlaying = false
                    audioPositionMs = 0L
                }
                is LoopTickDirective.ContinuePlayback -> Unit
            }

            if (showEqStudioSheet) {
                spectrumBins = audioDspController.generateSpectrumFrame(
                    playbackPositionMs = audioPositionMs,
                    isPlaying = true,
                    isLossless = currentAudioTrack.isLossless
                )
                waveformSamples = audioDspController.captureWaveformFrame(
                    isPlaying = true,
                    sampleCount = 64
                )
            }
        }
    }

    val filteredItems = remember(allMediaItems, selectedFilter, selectedSortOrder, searchQuery, selectedAlbumName) {
        val base = mediaRepository.filterAndSort(
            items = allMediaItems,
            filter = selectedFilter,
            sortOrder = selectedSortOrder,
            searchQuery = searchQuery
        )
        if (selectedAlbumName != null) {
            base.filter { it.album.equals(selectedAlbumName, ignoreCase = true) }
        } else {
            base
        }
    }

    val chunkedGridRows = remember(filteredItems, gridColumns) {
        filteredItems.chunked(gridColumns.coerceIn(1, 3))
    }

    val continueWatchingVideos = remember(allMediaItems) {
        videoTheaterEngine.getContinueWatchingItems(allMediaItems)
    }

    val albumBuckets = remember(allMediaItems) {
        mediaRepository.buildAlbumBuckets(allMediaItems)
    }

    val vaultItems = remember(allMediaItems) {
        allMediaItems.filter { it.isVaulted && !it.isTrashed }
    }

    val trashedItems = remember(allMediaItems) {
        allMediaItems.filter { it.isTrashed }
    }

    val allAudioTracks = remember(allMediaItems) {
        allMediaItems.filter { it.mediaType == MediaType.AUDIO && !it.isTrashed }
    }

    val isAnySheetOpen = showPhotoCompareSheet ||
        showAudiophilePlayerSheet ||
        showEqStudioSheet ||
        showVideoTheaterSheet ||
        showInspectorVaultSheet

    Box(modifier = modifier.fillMaxSize()) {
        AppScreen(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    title = when (selectedNavTab) {
                        PhotosNavTab.HOME -> selectedAlbumName ?: "Photos"
                        PhotosNavTab.ALBUMS -> "Albums"
                        PhotosNavTab.EQ_DECK -> "Music"
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (searchBarVisible) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated
                                    )
                                    .jigglePressClickable { searchBarVisible = !searchBarVisible },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .jigglePressClickable {
                                        gridColumns = when (gridColumns) {
                                            2 -> 3
                                            3 -> 1
                                            else -> 2
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GridView,
                                    contentDescription = "Grid",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .jigglePressClickable { sortMenuExpanded = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = "Sort",
                                        tint = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AppDropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    SortOrder.entries.forEach { order ->
                                        AppDropdownMenuItem(
                                            text = order.label,
                                            trailingText = if (selectedSortOrder == order) "✓" else null,
                                            onClick = {
                                                selectedSortOrder = order
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isAnySheetOpen) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentAudioTrack != null) {
                            PersistentMiniPlayerPill(
                                track = currentAudioTrack,
                                isPlaying = isAudioPlaying,
                                positionMs = audioPositionMs,
                                loopShortBadge = loopMode.shortBadge,
                                onExpandPlayer = { showAudiophilePlayerSheet = true },
                                onPlayPause = { isAudioPlaying = !isAudioPlaying },
                                onNext = {
                                    val nextIdx = (activeQueueState.activeIndex + 1) % activeQueueState.tracks.size.coerceAtLeast(1)
                                    multiQueueManager.selectTrackAt(nextIdx, resetAbLoop = false)
                                    audioPositionMs = 0L
                                },
                                onCycleLoop = { multiQueueManager.cycleLoopMode() },
                                onOpenEq = {
                                    isEqSheetMinimized = false
                                    showEqStudioSheet = true
                                }
                            )
                        }

                        AppFloatingBar {
                            AppFloatingBarItem(
                                icon = Icons.Outlined.Home,
                                contentDescription = "Photos",
                                isSelected = selectedNavTab == PhotosNavTab.HOME,
                                onClick = {
                                    selectedNavTab = PhotosNavTab.HOME
                                    selectedAlbumName = null
                                }
                            )
                            AppFloatingBarItem(
                                icon = Icons.Outlined.Collections,
                                contentDescription = "Albums",
                                isSelected = selectedNavTab == PhotosNavTab.ALBUMS,
                                onClick = { selectedNavTab = PhotosNavTab.ALBUMS }
                            )
                            AppFloatingBarItem(
                                icon = Icons.Outlined.Equalizer,
                                contentDescription = "Music",
                                isSelected = selectedNavTab == PhotosNavTab.EQ_DECK,
                                onClick = { selectedNavTab = PhotosNavTab.EQ_DECK }
                            )
                        }
                    }
                }
            }
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (statusNoticeText != null) {
                item {
                    AppNoticeBanner(
                        visible = true,
                        title = "Photos",
                        message = statusNoticeText!!,
                        variant = AppNoticeVariant.SUCCESS,
                        onDismiss = { statusNoticeText = null }
                    )
                }
            }

            if (searchBarVisible) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.SurfacePanel)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
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
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search photos, videos, music, albums...",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }
            }

            when (selectedNavTab) {
                PhotosNavTab.HOME -> {
                    // Filter Chips Row
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(HOME_FILTER_CHIPS) { filter ->
                                AppPillChip(
                                    label = filter.label,
                                    isSelected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                            if (selectedAlbumName != null) {
                                item {
                                    AppPillChip(
                                        label = "All Albums",
                                        isSelected = false,
                                        onClick = { selectedAlbumName = null }
                                    )
                                }
                            }
                        }
                    }

                    // Continue Watching Shelf
                    if (continueWatchingVideos.isNotEmpty() &&
                        (selectedFilter == MediaFilter.ALL || selectedFilter == MediaFilter.VIDEO) &&
                        selectedAlbumName == null
                    ) {
                        item {
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                contentPadding = 14.dp
                            ) {
                                Text(
                                    text = "Continue Watching",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(continueWatchingVideos, key = { it.id }) { vid ->
                                        ContinueWatchingCard(
                                            video = vid,
                                            onResume = {
                                                isAudioPlaying = false
                                                activeVideoItem = vid
                                                videoPositionMs = vid.resumePositionMs
                                                isVideoPlaying = true
                                                showVideoTheaterSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                contentPadding = 20.dp
                            ) {
                                Text(
                                    text = if (!hasMediaPermission) "Allow Media Access" else "No Media Found",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (!hasMediaPermission) {
                                        "Grant permission to view and play photos, videos, and music stored on your device."
                                    } else {
                                        "No photos, videos, or audio files matched the current filter."
                                    },
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                if (!hasMediaPermission) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AppButton(
                                        text = "Grant Access",
                                        onClick = { permissionLauncher.launch(requiredPermissions) },
                                        variant = AppButtonVariant.PRIMARY
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = chunkedGridRows,
                            key = { row -> row.first().id },
                            contentType = { "media_grid_row" }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    UniversalMediaTileCard(
                                        item = item,
                                        compact = gridColumns == 3,
                                        modifier = Modifier.weight(1f),
                                        onOpenPrimary = {
                                            when (item.mediaType) {
                                                MediaType.AUDIO -> {
                                                    val idx = allAudioTracks.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                                    multiQueueManager.setQueueTracks(allAudioTracks, startIndex = idx)
                                                    audioPositionMs = 0L
                                                    isAudioPlaying = true
                                                    showAudiophilePlayerSheet = true
                                                }
                                                MediaType.VIDEO -> {
                                                    isAudioPlaying = false
                                                    activeVideoItem = item
                                                    videoPositionMs = item.resumePositionMs
                                                    isVideoPlaying = true
                                                    showVideoTheaterSheet = true
                                                }
                                                else -> {
                                                    selectedPhotoItem = item
                                                    showPhotoCompareSheet = true
                                                }
                                            }
                                        },
                                        onToggleFavorite = {
                                            mediaRepository.toggleFavorite(item.id)
                                        }
                                    )
                                }
                                if (rowItems.size < gridColumns) {
                                    repeat(gridColumns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                PhotosNavTab.ALBUMS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppButton(
                                text = "Vault (${vaultItems.size})",
                                leadingIcon = Icons.Outlined.Lock,
                                onClick = {
                                    inspectorInitialTab = InspectorTab.PRIVATE_VAULT
                                    showInspectorVaultSheet = true
                                },
                                variant = AppButtonVariant.PRIMARY,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Trash (${trashedItems.size})",
                                leadingIcon = Icons.Outlined.DeleteOutline,
                                onClick = {
                                    inspectorInitialTab = InspectorTab.TRASH_DUPLICATES
                                    showInspectorVaultSheet = true
                                },
                                variant = AppButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    items(albumBuckets, key = { it.id }) { bucket ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = 14.dp,
                            onClick = {
                                selectedAlbumName = bucket.name
                                selectedNavTab = PhotosNavTab.HOME
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(ElectricSlateTokens.SurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val cover = bucket.coverItem
                                        if (cover != null && cover.mediaType != MediaType.AUDIO) {
                                            AsyncImage(
                                                model = Uri.parse(cover.uriString),
                                                contentDescription = bucket.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Collections,
                                                contentDescription = null,
                                                tint = ElectricSlateTokens.PrimaryAccent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bucket.name,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = ElectricSlateTokens.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${bucket.itemCount} items",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                                AppPillChip(
                                    label = "${bucket.itemCount}",
                                    isSelected = false,
                                    onClick = {
                                        selectedAlbumName = bucket.name
                                        selectedNavTab = PhotosNavTab.HOME
                                    }
                                )
                            }
                        }
                    }
                }

                PhotosNavTab.EQ_DECK -> {
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            contentPadding = 16.dp
                        ) {
                            Text(
                                text = "Music & Equalizer",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${allAudioTracks.size} tracks • ${loopMode.label} • ${dspState.activePresetName}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AppButton(
                                    text = "Now Playing",
                                    leadingIcon = Icons.Outlined.Album,
                                    onClick = {
                                        if (currentAudioTrack == null && allAudioTracks.isNotEmpty()) {
                                            multiQueueManager.setQueueTracks(allAudioTracks, startIndex = 0)
                                        }
                                        showAudiophilePlayerSheet = true
                                    },
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Equalizer",
                                    leadingIcon = Icons.Outlined.Equalizer,
                                    onClick = {
                                        isEqSheetMinimized = false
                                        showEqStudioSheet = true
                                    },
                                    variant = AppButtonVariant.SECONDARY,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Queue Switcher Card
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = 14.dp
                        ) {
                            Text(
                                text = "Queues",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            multiQueueManager.getAllSlotStates().forEach { slotState ->
                                val isCur = slotState.slot == activeQueueState.slot
                                AppListItem(
                                    title = slotState.slot.displayName,
                                    subtitle = "${slotState.tracks.size} tracks",
                                    leadingIcon = Icons.Outlined.QueuePlayNext,
                                    action = if (isCur) {
                                        AppListItemAction.Custom {
                                            Text(
                                                text = "Active",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.PrimaryAccent
                                            )
                                        }
                                    } else AppListItemAction.Chevron,
                                    onClick = {
                                        val switched = multiQueueManager.switchQueueSlot(slotState.slot, audioPositionMs)
                                        audioPositionMs = switched.savedPositionMs
                                    }
                                )
                            }
                        }
                    }

                    // All Music Tracks on Device
                    if (allAudioTracks.isNotEmpty()) {
                        items(allAudioTracks, key = { it.id }) { track ->
                            val isCurrent = currentAudioTrack?.id == track.id
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                contentPadding = 12.dp,
                                onClick = {
                                    val idx = allAudioTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                    multiQueueManager.setQueueTracks(allAudioTracks, startIndex = idx)
                                    audioPositionMs = 0L
                                    isAudioPlaying = true
                                    showAudiophilePlayerSheet = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ElectricSlateTokens.SurfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MusicNote,
                                                contentDescription = null,
                                                tint = ElectricSlateTokens.PrimaryAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            if (!track.albumArtUriString.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = Uri.parse(track.albumArtUriString),
                                                    contentDescription = track.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                fontFamily = ElmsSans,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = if (isCurrent) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${track.artist} • ${track.formattedDuration}",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .jigglePressClickable {
                                                multiQueueManager.playNext(track)
                                                statusNoticeText = "Playing next: ${track.title}"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.QueueMusic,
                                            contentDescription = "Play Next",
                                            tint = ElectricSlateTokens.PrimaryAccent,
                                            modifier = Modifier.size(18.dp)
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

        // 1. Full-Screen Immersive Photo Viewer & Compare Sheet (Lazy-mounted only when visible)
        if (showPhotoCompareSheet) {
            PhotoCompareAndLoupeSheet(
                visible = true,
                primaryItem = selectedPhotoItem,
                comparisonCandidates = allMediaItems,
                onDismiss = { showPhotoCompareSheet = false },
                onOpenInspector = { item ->
                    showPhotoCompareSheet = false
                    inspectorTargetItem = item
                    inspectorInitialTab = InspectorTab.HISTOGRAM_EXIF
                    showInspectorVaultSheet = true
                },
                onRecordVoiceNote = {},
                onPlayVoiceNote = {},
                onSelectPhoto = { selectedPhotoItem = it }
            )
        }

        // 2. Music Player Sheet (Lazy-mounted & loads .lrc/.cue off the UI thread)
        if (showAudiophilePlayerSheet) {
            var currentLyrics by remember { mutableStateOf<List<LrcLyricLine>>(emptyList()) }
            var currentCueSlices by remember { mutableStateOf<List<CueTrackSlice>>(emptyList()) }

            LaunchedEffect(currentAudioTrack?.filePath, audioDurationMs) {
                val path = currentAudioTrack?.filePath ?: ""
                if (path.isBlank()) {
                    currentLyrics = emptyList()
                    currentCueSlices = emptyList()
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val base = path.substringBeforeLast('.')
                    val lrcFile = File("$base.lrc")
                    val parsedLrc = if (lrcFile.exists()) {
                        MultiQueueManager.parseLrcLyrics(lrcFile.readText())
                    } else emptyList()

                    val cueFile = File("$base.cue")
                    val parsedCue = if (cueFile.exists()) {
                        MultiQueueManager.parseCueSheet(
                            cueFile.readText(),
                            path,
                            audioDurationMs.coerceAtLeast(1000L)
                        )
                    } else emptyList()

                    currentLyrics = parsedLrc
                    currentCueSlices = parsedCue
                }
            }

            val currentSignalReport = remember(currentAudioTrack, dspState) {
                audioDspController.buildSignalPathReport(
                    codecName = currentAudioTrack?.codecName ?: "PCM",
                    bitDepth = currentAudioTrack?.bitDepth ?: 16,
                    sampleRateHz = currentAudioTrack?.sampleRateHz ?: 44100,
                    bitrateKbps = currentAudioTrack?.bitrateKbps ?: 320,
                    isLosslessSource = currentAudioTrack?.isLossless ?: false
                )
            }

            AudiophilePlayerSheet(
                visible = true,
                currentTrack = currentAudioTrack,
                isPlaying = isAudioPlaying,
                playbackPositionMs = audioPositionMs,
                trackDurationMs = audioDurationMs.coerceAtLeast(currentAudioTrack?.durationMs ?: 1000L),
                loopMode = loopMode,
                abLoopState = abLoopState,
                activeQueueState = activeQueueState,
                allQueueSlots = multiQueueManager.getAllSlotStates(),
                dspState = dspState,
                signalPathReport = currentSignalReport,
                lyrics = currentLyrics,
                cueSlices = currentCueSlices,
                bookmarks = currentAudioTrack?.let { multiQueueManager.getBookmarksForMedia(it.id) }.orEmpty() + bookmarks.filter { it.mediaId == currentAudioTrack?.id }.distinctBy { it.id },
                sleepTimerRemainingMin = sleepTimerRemainingMin,
                onDismiss = { showAudiophilePlayerSheet = false },
                onPlayPauseToggle = { isAudioPlaying = !isAudioPlaying },
                onSeekToMs = { pos ->
                    audioPositionMs = pos
                    try {
                        exoPlayer?.seekTo(pos)
                    } catch (_: Throwable) {
                    }
                },
                onSkipPrevious = {
                    if (audioPositionMs > 3_000L) {
                        audioPositionMs = 0L
                        try {
                            exoPlayer?.seekTo(0L)
                        } catch (_: Throwable) {
                        }
                    } else {
                        val prevIdx = (activeQueueState.activeIndex - 1).coerceAtLeast(0)
                        multiQueueManager.selectTrackAt(prevIdx, resetAbLoop = false)
                        audioPositionMs = 0L
                    }
                },
                onSkipNext = {
                    val nextIdx = (activeQueueState.activeIndex + 1) % activeQueueState.tracks.size.coerceAtLeast(1)
                    multiQueueManager.selectTrackAt(nextIdx, resetAbLoop = false)
                    audioPositionMs = 0L
                },
                onCycleLoopMode = { multiQueueManager.cycleLoopMode() },
                onMarkAbStart = { multiQueueManager.markAbPointA(it) },
                onMarkAbEnd = { multiQueueManager.markAbPointB(it) },
                onNudgeAbBounds = { dStart, dEnd ->
                    multiQueueManager.nudgeAbBounds(dStart, dEnd, audioDurationMs.coerceAtLeast(1000L))
                },
                onSetAbMaxLoops = { multiQueueManager.setAbMaxIterations(it) },
                onClearAbLoop = { multiQueueManager.clearAbLoop() },
                onToggleShuffle = { multiQueueManager.toggleShuffle() },
                onSwitchQueueSlot = { slot ->
                    val switched = multiQueueManager.switchQueueSlot(slot, audioPositionMs)
                    audioPositionMs = switched.savedPositionMs
                },
                onSelectQueueTrack = { idx ->
                    multiQueueManager.selectTrackAt(idx)
                    audioPositionMs = 0L
                    isAudioPlaying = true
                },
                onMoveQueueTrack = { from, to -> multiQueueManager.moveQueueItem(from, to) },
                onRemoveQueueTrack = { idx -> multiQueueManager.removeFromQueue(idx) },
                onSaveQueueM3u8 = {
                    val outDir = File(context.filesDir, "playlists")
                    val m3u = multiQueueManager.saveActiveQueueAsM3u8(outDir, activeQueueState.slot.displayName)
                    statusNoticeText = "Saved playlist: ${m3u.name}"
                },
                onSelectOutputSignalMode = { audioDspController.setOutputSignalMode(it) },
                onChangeSpeedAndPitch = { spd, pitch, c432 ->
                    audioDspController.setPlaybackSpeedAndPitch(spd, pitch, c432)
                },
                onToggleSkipSilence = { audioDspController.setSkipSilence(it) },
                onChangeCrossfadeSec = { audioDspController.setCrossfadeDurationSec(it) },
                onChangeCrossfeedPercent = { audioDspController.setCrossfeedPercent(it) },
                onChangeChannelBalance = { audioDspController.setChannelBalance(it) },
                onChangeReplayGainMode = { audioDspController.setReplayGainMode(it) },
                onSetSleepTimerMin = { sleepTimerRemainingMin = it },
                onAddBookmark = { label ->
                    currentAudioTrack?.let { multiQueueManager.addBookmark(it.id, audioPositionMs, label) }
                },
                onRemoveBookmark = { multiQueueManager.removeBookmark(it) },
                onSaveTagEdits = { title, artist, album ->
                    currentAudioTrack?.let {
                        mediaRepository.updateMediaTags(it.id, title, artist, album)
                        statusNoticeText = "Saved tags for '$title'"
                    }
                },
                onOpenEqStudio = {
                    showAudiophilePlayerSheet = false
                    isEqSheetMinimized = false
                    showEqStudioSheet = true
                },
                onOpenTrimmerForCurrent = {}
            )
        }

        // 3. Equalizer Sheet (Lazy-mounted, no bottom nav bar overlap)
        if (showEqStudioSheet) {
            AudioEqStudioSheet(
                visible = true,
                isMinimized = false,
                onMinimizedChange = {},
                dspState = dspState,
                presets = audioDspController.allPresets(),
                spectrumBins = spectrumBins,
                waveformSamples = waveformSamples,
                isLosslessTrack = currentAudioTrack?.isLossless ?: true,
                onDismiss = { showEqStudioSheet = false },
                onToggleEqEnabled = { audioDspController.setEqEnabled(it) },
                onToggleParametricMode = { audioDspController.setParametricMode(it) },
                onChangePreampDb = { audioDspController.setPreampDb(it) },
                onChangeBandGain = { idx, gain -> audioDspController.setBandGain(idx, gain) },
                onUpdateParametricBand = { idx, fc, gain, q ->
                    audioDspController.updateParametricBand(idx, fc, gain, q)
                },
                onApplyPreset = { audioDspController.applyPreset(it) },
                onSaveCustomPreset = { name ->
                    audioDspController.saveCustomPreset(name)
                    statusNoticeText = "Saved preset: $name"
                },
                onImportAutoEqProfile = { name, txt ->
                    val preset = audioDspController.importAutoEqProfile(name, txt)
                    statusNoticeText = "Applied ${preset.name}"
                },
                onChangeBassBoost = { audioDspController.setBassBoostPercent(it) },
                onChangeVirtualizer = { audioDspController.setVirtualizerPercent(it) },
                onChangeLoudnessDb = { audioDspController.setLoudnessGainDb(it) },
                onSwitchOutputDevice = { audioDspController.switchOutputDevice(it) },
                onChangeVisualizerMode = { audioDspController.setVisualizerMode(it) }
            )
        }

        // 4. Real Video Player Sheet (Lazy-mounted so ExoPlayer is allocated only during video playback)
        if (showVideoTheaterSheet) {
            var currentVideoSubtitles by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
            LaunchedEffect(activeVideoItem?.filePath) {
                val path = activeVideoItem?.filePath ?: ""
                if (path.isBlank()) {
                    currentVideoSubtitles = emptyList()
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val base = path.substringBeforeLast('.')
                    val srtFile = File("$base.srt")
                    val enFile = File("$base.en.srt")
                    val secondFile = File("$base.jp.srt")
                    val primaryCues = when {
                        srtFile.exists() -> VideoTheaterEngine.parseSrtSubtitles(srtFile.readText())
                        enFile.exists() -> VideoTheaterEngine.parseSrtSubtitles(enFile.readText())
                        else -> emptyList()
                    }
                    val secondaryCues = if (secondFile.exists()) {
                        VideoTheaterEngine.parseSrtSubtitles(secondFile.readText())
                    } else emptyList()
                    currentVideoSubtitles = VideoTheaterEngine.mergeDualSubtitles(primaryCues, secondaryCues)
                }
            }

            VideoTheaterSheet(
                visible = true,
                videoItem = activeVideoItem,
                seriesEpisodes = activeVideoItem?.let { videoTheaterEngine.getSeriesEpisodes(allMediaItems, it) }.orEmpty(),
                nextEpisode = activeVideoItem?.let { videoTheaterEngine.findNextEpisode(allMediaItems, it) },
                chapters = videoTheaterEngine.defaultChaptersForVideo(activeVideoItem?.durationMs ?: 18_000L),
                subtitleCues = currentVideoSubtitles,
                sceneBookmarks = activeVideoItem?.let { videoTheaterEngine.getSceneBookmarks(it.id) }.orEmpty(),
                folderSkipRule = activeVideoItem?.let { videoTheaterEngine.getFolderSkipRule(it) } ?: com.arinara.photos.media.FolderSkipRule("Default"),
                isPlaying = isVideoPlaying,
                playbackPositionMs = videoPositionMs,
                videoDurationMs = activeVideoItem?.durationMs ?: 18_000L,
                playbackSpeed = videoPlaybackSpeed,
                onDismiss = {
                    activeVideoItem?.let { mediaRepository.updateResumePosition(it.id, videoPositionMs) }
                    isVideoPlaying = false
                    showVideoTheaterSheet = false
                },
                onPlayPauseToggle = { isVideoPlaying = !isVideoPlaying },
                onSeekToMs = { pos ->
                    videoPositionMs = pos
                    activeVideoItem?.let { mediaRepository.updateResumePosition(it.id, pos) }
                },
                onSetPlaybackSpeed = { videoPlaybackSpeed = it },
                onSelectEpisode = { ep ->
                    activeVideoItem?.let { mediaRepository.updateResumePosition(it.id, videoPositionMs) }
                    activeVideoItem = ep
                    videoPositionMs = ep.resumePositionMs
                    isVideoPlaying = true
                },
                onSaveFolderSkipRule = { rule ->
                    videoTheaterEngine.setFolderSkipRule(rule)
                },
                onAddSceneBookmark = { caption ->
                    activeVideoItem?.let { videoTheaterEngine.addSceneBookmark(it.id, videoPositionMs, caption) }
                },
                onCaptureFramePng = { vid, tsMs ->
                    val outDir = File(context.filesDir, "captures")
                    val frameItem = mediaTrimmerEngine.captureVideoFrameStill(outDir, vid, tsMs, losslessPng = true)
                    mediaRepository.addOrUpdateMediaItem(frameItem)
                    statusNoticeText = "Saved frame: ${frameItem.title}"
                },
                onGenerateStoryboard = {},
                onHandOffToBackgroundAudio = { vid, posMs ->
                    isVideoPlaying = false
                    showVideoTheaterSheet = false
                    val audioEquivalent = vid.copy(
                        mediaType = MediaType.AUDIO,
                        qualityBadge = vid.extension
                    )
                    multiQueueManager.playNext(audioEquivalent)
                    val targetIdx = (activeQueueState.activeIndex + 1).coerceAtMost(multiQueueManager.activeQueueState.value.tracks.lastIndex)
                    multiQueueManager.selectTrackAt(targetIdx, resetAbLoop = false)
                    audioPositionMs = posMs
                    isAudioPlaying = true
                },
                onEnterPipMode = onRequestPipMode,
                onOpenTrimmerForVideo = {}
            )
        }

        // 5. Details & Vault Sheet (Lazy-mounted & computes SHA-256 / RGB Histogram off the main thread)
        if (showInspectorVaultSheet) {
            val targetForInspector = inspectorTargetItem ?: allMediaItems.firstOrNull()
            var currentInspectionReport by remember { mutableStateOf<DeepMediaInspectionReport?>(null) }
            var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }

            LaunchedEffect(targetForInspector?.id, allMediaItems.size) {
                withContext(Dispatchers.IO) {
                    currentInspectionReport = targetForInspector?.let { inspectorAndVaultEngine.inspectMediaItem(it) }
                    duplicateGroups = inspectorAndVaultEngine.findDuplicatesAndBurstGroups(allMediaItems)
                }
            }

            MediaInspectorAndVaultSheet(
                visible = true,
                initialTab = inspectorInitialTab,
                targetItem = targetForInspector,
                inspectionReport = currentInspectionReport,
                vaultUnlocked = vaultUnlockedState,
                vaultItems = vaultItems,
                trashedItems = trashedItems,
                duplicateGroups = duplicateGroups,
                onDismiss = { showInspectorVaultSheet = false },
                onScrubExifPrivacy = { item ->
                    val outDir = File(context.filesDir, "cleaned")
                    val cleanItem = inspectorAndVaultEngine.scrubExifPrivacyMetadata(item, outDir)
                    mediaRepository.addOrUpdateMediaItem(cleanItem)
                    inspectorTargetItem = cleanItem
                    statusNoticeText = "Removed location metadata: ${cleanItem.title}"
                },
                onUnlockVaultPin = { pin ->
                    val ok = inspectorAndVaultEngine.unlockVaultWithPin(pin)
                    vaultUnlockedState = inspectorAndVaultEngine.isVaultUnlocked()
                    ok
                },
                onLockVault = {
                    inspectorAndVaultEngine.lockVault()
                    vaultUnlockedState = false
                },
                onToggleVaultItem = { item, toVault ->
                    mediaRepository.setVaulted(item.id, toVault)
                    statusNoticeText = if (toVault) "Hidden '${item.title}'" else "Restored '${item.title}'"
                },
                onMoveToTrash = { item ->
                    mediaRepository.moveToTrash(item.id)
                },
                onRestoreFromTrash = { item ->
                    mediaRepository.restoreFromTrash(item.id)
                }
            )
        }
    }
}

@Composable
private fun PersistentMiniPlayerPill(
    track: MediaItem,
    isPlaying: Boolean,
    positionMs: Long,
    loopShortBadge: String,
    onExpandPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleLoop: () -> Unit,
    onOpenEq: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ElectricSlateTokens.SurfaceElevated)
            .jigglePressClickable(onClick = onExpandPlayer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfacePanel),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                if (!track.albumArtUriString.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(track.albumArtUriString),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${MediaItem.formatDurationMillis(positionMs)}",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.SurfacePanel)
                    .jigglePressClickable(onClick = onCycleLoop)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = loopShortBadge,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = ElectricSlateTokens.PrimaryAccent
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.PrimaryAccent)
                    .jigglePressClickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = ElectricSlateTokens.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.SurfacePanel)
                    .jigglePressClickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = "Next",
                    tint = ElectricSlateTokens.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.SurfacePanel)
                    .jigglePressClickable(onClick = onOpenEq),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Equalizer,
                    contentDescription = "Equalizer",
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    video: MediaItem,
    onResume: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ElectricSlateTokens.BgCanvas)
            .jigglePressClickable(onClick = onResume)
            .padding(10.dp)
    ) {
        Text(
            text = video.title,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ElectricSlateTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = video.remainingMinutesLabel,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = ElectricSlateTokens.TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(ElectricSlateTokens.TrackInactive)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(video.resumeProgressFraction.coerceIn(0.08f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.PrimaryAccent)
            )
        }
    }
}

@Composable
private fun UniversalMediaTileCard(
    item: MediaItem,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onOpenPrimary: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val thumbSizePx = if (compact) 256 else 384
    val thumbnailRequest = remember(item.id, item.uriString, thumbSizePx) {
        ImageRequest.Builder(context)
            .data(Uri.parse(item.uriString))
            .size(thumbSizePx, thumbSizePx)
            .crossfade(false)
            .memoryCacheKey("thumb_${item.id}_$thumbSizePx")
            .diskCacheKey("thumb_${item.id}")
            .build()
    }

    AppCard(
        modifier = modifier,
        onClick = onOpenPrimary,
        cornerRadius = 20.dp,
        contentPadding = if (compact) 6.dp else 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 96.dp else 140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ElectricSlateTokens.BgCanvas)
        ) {
            when (item.mediaType) {
                MediaType.PHOTO, MediaType.RAW_PHOTO, MediaType.ANIMATED -> {
                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MediaType.VIDEO -> {
                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Play",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                MediaType.AUDIO -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(if (compact) 28.dp else 36.dp)
                        )
                        if (!item.albumArtUriString.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(item.albumArtUriString),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Favorite Icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.72f))
                    .jigglePressClickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Duration Pill for Video/Audio
            if (item.durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.formattedDuration,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                }
            }
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.artist.isNotBlank()) {
                Text(
                    text = item.artist,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
