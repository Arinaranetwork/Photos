// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.arinara.photos.data.model.MediaFilter
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import com.arinara.photos.data.model.SortOrder
import com.arinara.photos.data.repository.MediaRepository
import com.arinara.photos.media.AudioDspController
import com.arinara.photos.media.LoopTickDirective
import com.arinara.photos.media.MultiQueueManager
import com.arinara.photos.media.VideoTheaterEngine
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
import com.arinara.photos.ui.studio.MediaTrimmerStudioSheet
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable
import com.arinara.photos.ui.video.VideoTheaterSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

enum class PhotosNavTab {
    HOME,
    ALBUMS,
    STUDIO,
    EQ_DECK
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onRequestPipMode: () -> Unit = {}
) {
    val context = LocalContext.current
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
    var gridColumns by remember { mutableIntStateOf(2) } // 1 = List, 2 = 2-Col Pro, 3 = 3-Col Dense

    var statusNoticeText by remember { mutableStateOf<String?>(null) }

    // Active Modal Sheets
    var showPhotoCompareSheet by remember { mutableStateOf(false) }
    var selectedPhotoItem by remember { mutableStateOf<MediaItem?>(null) }

    var showAudiophilePlayerSheet by remember { mutableStateOf(false) }
    var showEqStudioSheet by remember { mutableStateOf(false) }
    var isEqSheetMinimized by remember { mutableStateOf(false) }

    var showVideoTheaterSheet by remember { mutableStateOf(false) }
    var activeVideoItem by remember { mutableStateOf<MediaItem?>(null) }

    var showTrimmerStudioSheet by remember { mutableStateOf(false) }
    var trimmerTargetItem by remember { mutableStateOf<MediaItem?>(null) }

    var showInspectorVaultSheet by remember { mutableStateOf(false) }
    var inspectorInitialTab by remember { mutableStateOf(InspectorTab.HISTOGRAM_EXIF) }
    var inspectorTargetItem by remember { mutableStateOf<MediaItem?>(null) }
    var vaultUnlockedState by remember { mutableStateOf(false) }

    // Playback State (Audio & Video)
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioPositionMs by remember { mutableLongStateOf(0L) }
    var sleepTimerRemainingMin by remember { mutableIntStateOf(0) }
    var spectrumBins by remember { mutableStateOf(FloatArray(32) { 0.08f }) }

    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }

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

    // Permission Launcher for Android 13+ & Older
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coroutineScope.launch {
            mediaRepository.refreshMediaLibrary()
            statusNoticeText = "Local MediaStore & Studio Reference Library synchronized."
        }
    }

    // Initial Load & Queue Seed
    LaunchedEffect(Unit) {
        val loaded = mediaRepository.refreshMediaLibrary()
        val audioTracks = loaded.filter { it.mediaType == MediaType.AUDIO }
        multiQueueManager.seedQueues(audioTracks)
    }

    val currentAudioTrack = activeQueueState.currentTrack

    // Sync currentAudioTrack to ExoPlayer
    LaunchedEffect(currentAudioTrack?.id) {
        val track = currentAudioTrack ?: return@LaunchedEffect
        audioPositionMs = activeQueueState.savedPositionMs
        try {
            exoPlayer?.let { player ->
                val f = File(track.filePath)
                val uri = if (f.exists()) android.net.Uri.fromFile(f) else android.net.Uri.parse(track.uriString)
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

    // 60fps Ticker for Audio Loop Evaluation, Spectrum Bars & Video Progress
    LaunchedEffect(isAudioPlaying, isVideoPlaying, currentAudioTrack?.id, activeVideoItem?.id) {
        while (true) {
            delay(50L)
            if (isAudioPlaying && currentAudioTrack != null) {
                val trackDur = currentAudioTrack.durationMs.coerceAtLeast(10_000L)
                val exoPos = try {
                    exoPlayer?.currentPosition?.takeIf { it > 0L }
                } catch (_: Throwable) {
                    null
                }
                val nextPos = exoPos ?: (audioPositionMs + (50L * dspState.playbackSpeed).toLong())
                audioPositionMs = nextPos.coerceAtMost(trackDur)

                if (dspState.skipSilenceEnabled && (audioPositionMs % 1500L < 60L)) {
                    audioDspController.recordSkippedSilence(45L)
                }

                when (val directive = multiQueueManager.evaluatePositionTick(audioPositionMs, trackDur)) {
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

                spectrumBins = audioDspController.generateSpectrumFrame(
                    playbackPositionMs = audioPositionMs,
                    isPlaying = true,
                    isLossless = currentAudioTrack.isLossless
                )
            } else {
                spectrumBins = audioDspController.generateSpectrumFrame(
                    playbackPositionMs = audioPositionMs,
                    isPlaying = false,
                    isLossless = currentAudioTrack?.isLossless ?: true
                )
            }

            if (isVideoPlaying && activeVideoItem != null) {
                val vidDur = activeVideoItem!!.durationMs.coerceAtLeast(10_000L)
                val nextVidPos = videoPositionMs + (50L * videoPlaybackSpeed).toLong()
                if (nextVidPos >= vidDur) {
                    val nextEp = videoTheaterEngine.findNextEpisode(allMediaItems, activeVideoItem!!)
                    if (nextEp != null) {
                        mediaRepository.updateResumePosition(activeVideoItem!!.id, vidDur)
                        activeVideoItem = nextEp
                        videoPositionMs = nextEp.resumePositionMs
                        statusNoticeText = "Auto-Playing Next Episode: ${nextEp.title}"
                    } else {
                        isVideoPlaying = false
                        videoPositionMs = 0L
                    }
                } else {
                    videoPositionMs = nextVidPos
                }
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

    val duplicateGroups = remember(allMediaItems) {
        inspectorAndVaultEngine.findDuplicatesAndBurstGroups(allMediaItems)
    }

    AppScreen(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = when (selectedNavTab) {
                    PhotosNavTab.HOME -> selectedAlbumName ?: "Photos Studio"
                    PhotosNavTab.ALBUMS -> "Albums & Vault"
                    PhotosNavTab.STUDIO -> "Cutter & Batch Studio"
                    PhotosNavTab.EQ_DECK -> "Audiophile & 10-Band EQ"
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Toggle Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (searchBarVisible) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated
                                )
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
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

                        // Grid Column Density Switcher
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
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
                                contentDescription = "Grid Density",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Sort Order Dropdown
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Persistent Floating Mini-Player Pill (F-A01)
                if (currentAudioTrack != null) {
                    PersistentMiniPlayerPill(
                        track = currentAudioTrack,
                        isPlaying = isAudioPlaying,
                        positionMs = audioPositionMs,
                        loopShortBadge = loopMode.shortBadge,
                        queueName = activeQueueState.slot.displayName,
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
                        contentDescription = "Gallery",
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
                        icon = Icons.Outlined.ContentCut,
                        contentDescription = "Studio",
                        isSelected = selectedNavTab == PhotosNavTab.STUDIO,
                        onClick = { selectedNavTab = PhotosNavTab.STUDIO }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Equalizer,
                        contentDescription = "Audio & EQ",
                        isSelected = selectedNavTab == PhotosNavTab.EQ_DECK,
                        onClick = { selectedNavTab = PhotosNavTab.EQ_DECK }
                    )
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
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Optional Status Notice Banner
            if (statusNoticeText != null) {
                item {
                    AppNoticeBanner(
                        visible = true,
                        title = "Studio Engine",
                        message = statusNoticeText!!,
                        variant = AppNoticeVariant.SUCCESS,
                        onDismiss = { statusNoticeText = null }
                    )
                }
            }

            // Collapsible Search Bar
            if (searchBarVisible) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.SurfacePanel)
                            .border(1.dp, ElectricSlateTokens.PrimaryAccent, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                text = "Search by title, codec (FLAC, DNG, MKV), camera (Sony, Nikon), album...",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }
            }

            when (selectedNavTab) {
                PhotosNavTab.HOME -> {
                    // Format & Quality Filter Chips Row (F-G01, F-G02)
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(MediaFilter.entries) { filter ->
                                AppPillChip(
                                    label = filter.label,
                                    isSelected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                        }
                    }

                    // Netflix-Style "Continue Watching" Shelf (F-V01)
                    if (continueWatchingVideos.isNotEmpty() &&
                        (selectedFilter == MediaFilter.ALL || selectedFilter == MediaFilter.VIDEO) &&
                        selectedAlbumName == null
                    ) {
                        item {
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                isGlow = true,
                                cornerRadius = 20.dp,
                                contentPadding = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Continue Watching (${continueWatchingVideos.size})",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    Text(
                                        text = "Netflix Cinema Mode",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(continueWatchingVideos, key = { it.id }) { vid ->
                                        ContinueWatchingCard(
                                            video = vid,
                                            onResume = {
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

                    // Quick Scan & Permission Sync Header Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredItems.size} Media Items • ${selectedSortOrder.label}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (selectedAlbumName != null) {
                                    AppButton(
                                        text = "All Albums",
                                        onClick = { selectedAlbumName = null },
                                        variant = AppButtonVariant.OUTLINED,
                                        height = 32.dp,
                                        horizontalPadding = 10.dp,
                                        fontSize = 11.sp
                                    )
                                }
                                AppButton(
                                    text = "Scan Device",
                                    leadingIcon = Icons.Outlined.Refresh,
                                    onClick = {
                                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            arrayOf(
                                                Manifest.permission.READ_MEDIA_IMAGES,
                                                Manifest.permission.READ_MEDIA_VIDEO,
                                                Manifest.permission.READ_MEDIA_AUDIO
                                            )
                                        } else {
                                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                        permissionLauncher.launch(perms)
                                    },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Universal Media Grid / List Rows
                    val chunkedRows = filteredItems.chunked(gridColumns.coerceIn(1, 3))
                    items(chunkedRows) { rowItems ->
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
                                                val audioList = allMediaItems.filter { it.mediaType == MediaType.AUDIO && !it.isTrashed }
                                                val idx = audioList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                                multiQueueManager.setQueueTracks(audioList, startIndex = idx)
                                                audioPositionMs = 0L
                                                isAudioPlaying = true
                                                showAudiophilePlayerSheet = true
                                            }
                                            MediaType.VIDEO -> {
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
                                    onPlayNextInQueue = {
                                        multiQueueManager.playNext(item)
                                        statusNoticeText = "Queued '${item.title}' to Play Next in ${activeQueueState.slot.displayName}"
                                    },
                                    onOpenTrimmer = {
                                        trimmerTargetItem = item
                                        showTrimmerStudioSheet = true
                                    },
                                    onOpenInspector = {
                                        inspectorTargetItem = item
                                        inspectorInitialTab = InspectorTab.HISTOGRAM_EXIF
                                        showInspectorVaultSheet = true
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

                PhotosNavTab.ALBUMS -> {
                    // Smart Albums, Pinned Folders, PIN Vault & 30-Day Recycle Bin (F-G03, F-G11, F-G12)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppButton(
                                text = "Private Vault (${vaultItems.size})",
                                leadingIcon = Icons.Outlined.Lock,
                                onClick = {
                                    inspectorInitialTab = InspectorTab.PRIVATE_VAULT
                                    showInspectorVaultSheet = true
                                },
                                variant = AppButtonVariant.PRIMARY,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Trash & Dupes (${trashedItems.size + duplicateGroups.size})",
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
                            isGlow = bucket.isPinned,
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (bucket.isPinned) "★ ${bucket.name}" else bucket.name,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${bucket.itemCount} items • ${bucket.photoCount} Photos • ${bucket.videoCount} Videos • ${bucket.audioCount} Audio",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                    Text(
                                        text = bucket.folderPath,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                AppPillChip(
                                    label = "${bucket.itemCount} files",
                                    isSelected = bucket.isPinned,
                                    onClick = {
                                        selectedAlbumName = bucket.name
                                        selectedNavTab = PhotosNavTab.HOME
                                    }
                                )
                            }
                        }
                    }
                }

                PhotosNavTab.STUDIO -> {
                    // Power-User Non-Destructive Cutter, Multi-Quality Audio Extractor & Optics Hub
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlow = true,
                            cornerRadius = 24.dp,
                            contentPadding = 16.dp
                        ) {
                            Text(
                                text = "Non-Destructive Media Cutter & Multi-Quality Studio",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Trim outside ends, splice middle sections, split multi-part chapters, extract audio from video, or convert between Hi-Res 24-bit/96kHz, CD Lossless, 320kbps, 192kbps, and 96kbps.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            AppButton(
                                text = "Launch Cutter & Quality Converter",
                                leadingIcon = Icons.Outlined.ContentCut,
                                onClick = {
                                    trimmerTargetItem = currentAudioTrack ?: allMediaItems.firstOrNull()
                                    showTrimmerStudioSheet = true
                                },
                                variant = AppButtonVariant.PRIMARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = 8.dp
                        ) {
                            AppListItem(
                                title = "2-Up / 4-Up Sync Photo Compare & 400% Loupe",
                                subtitle = "Compare burst shots with locked zoom/pan, Focus Peaking & Exposure Zebras",
                                leadingIcon = Icons.Outlined.Compare,
                                action = AppListItemAction.Chevron,
                                onClick = {
                                    selectedPhotoItem = allMediaItems.firstOrNull {
                                        it.mediaType == MediaType.PHOTO || it.mediaType == MediaType.RAW_PHOTO
                                    }
                                    showPhotoCompareSheet = true
                                }
                            )
                            AppListItem(
                                title = "Live 256-Bin RGB Histogram & EXIF Scrubber",
                                subtitle = "Inspect color channels, camera telemetry, SHA-256 & strip GPS metadata",
                                leadingIcon = Icons.Outlined.Info,
                                action = AppListItemAction.Chevron,
                                onClick = {
                                    inspectorTargetItem = allMediaItems.firstOrNull()
                                    inspectorInitialTab = InspectorTab.HISTOGRAM_EXIF
                                    showInspectorVaultSheet = true
                                }
                            )
                            AppListItem(
                                title = "Comfy Netflix-Style Video Theater",
                                subtitle = "Hold-for-2x speed, Bedtime Touch Lock, Dual Subtitles & Transcript Search",
                                leadingIcon = Icons.Outlined.Movie,
                                action = AppListItemAction.Chevron,
                                onClick = {
                                    val vid = allMediaItems.firstOrNull { it.mediaType == MediaType.VIDEO }
                                    if (vid != null) {
                                        activeVideoItem = vid
                                        videoPositionMs = vid.resumePositionMs
                                        isVideoPlaying = true
                                        showVideoTheaterSheet = true
                                    }
                                }
                            )
                        }
                    }
                }

                PhotosNavTab.EQ_DECK -> {
                    // Audiophile Hi-Res Deck, 5 Multi-Queues & 10-Band EQ Studio Hub
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlow = true,
                            cornerRadius = 24.dp,
                            contentPadding = 16.dp
                        ) {
                            Text(
                                text = "Audiophile Hi-Res Player & 10-Band DSP Studio",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Active Queue: ${activeQueueState.slot.displayName} • Loop: ${loopMode.label} • Preset: ${dspState.activePresetName} (${dspState.outputSignalMode.badge})",
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
                                    text = "Open Hi-Res Deck",
                                    leadingIcon = Icons.Outlined.Album,
                                    onClick = { showAudiophilePlayerSheet = true },
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "10-Band EQ & FFT",
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

                    // 5 Multi-Queue Quick Switcher Cards
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = 14.dp
                        ) {
                            Text(
                                text = "5 Independent Multi-Queue Slots",
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
                                    subtitle = "${slotState.tracks.size} tracks • Saved @ ${MediaItem.formatDurationMillis(slotState.savedPositionMs)}",
                                    leadingIcon = Icons.Outlined.QueuePlayNext,
                                    action = if (isCur) {
                                        AppListItemAction.Custom {
                                            Text(
                                                text = "ACTIVE",
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
                                        statusNoticeText = "Switched to ${slotState.slot.displayName} (Restored @ ${MediaItem.formatDurationMillis(switched.savedPositionMs)})"
                                    }
                                )
                            }
                        }
                    }

                    // Privacy & Architecture Summary Card
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = 14.dp
                        ) {
                            Text(
                                text = "Photos v1.0.0 — 100% Offline Power-User Media Suite",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Zero cloud telemetry, zero tracking, zero ads. All media indexing, lossless audio DSP, subtitle transcript search, non-destructive cutting, and SHA-256 Private Vault protection run strictly on-device.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Pro Photo Compare, 400% Loupe, Focus Peaking & GIF Scrubber Sheet
    PhotoCompareAndLoupeSheet(
        visible = showPhotoCompareSheet,
        primaryItem = selectedPhotoItem,
        comparisonCandidates = allMediaItems,
        onDismiss = { showPhotoCompareSheet = false },
        onOpenInspector = { item ->
            showPhotoCompareSheet = false
            inspectorTargetItem = item
            inspectorInitialTab = InspectorTab.HISTOGRAM_EXIF
            showInspectorVaultSheet = true
        },
        onRecordVoiceNote = { item ->
            val outDir = File(context.filesDir, "studio_exports")
            val memoFile = mediaTrimmerEngine.recordVoiceMemoForPhoto(outDir, item, durationSeconds = 6)
            mediaRepository.attachVoiceNote(item.id, memoFile.absolutePath)
            selectedPhotoItem = item.copy(voiceNotePath = memoFile.absolutePath)
            statusNoticeText = "Recorded 6s Voice-Over Photo Memo: ${memoFile.name}"
        },
        onPlayVoiceNote = { item ->
            val path = item.voiceNotePath
            if (path != null) {
                val memoMedia = MediaItem(
                    id = "memo_${item.id}",
                    title = "Voice Memo: ${item.title}",
                    artist = "Photo Voice Note",
                    album = "Voice Memos",
                    uriString = File(path).toURI().toString(),
                    filePath = path,
                    mimeType = "audio/wav",
                    extension = "WAV",
                    mediaType = MediaType.AUDIO,
                    sizeBytes = File(path).length(),
                    dateAddedEpochSec = System.currentTimeMillis() / 1000L,
                    durationMs = 6_000L,
                    qualityBadge = "LOSSLESS WAV",
                    isLossless = true
                )
                multiQueueManager.playNext(memoMedia)
                showPhotoCompareSheet = false
                isAudioPlaying = true
                showAudiophilePlayerSheet = true
            }
        }
    )

    // 2. Audiophile Music Player, 4-State Loop, 5 Multi-Queues, LRC & CUE Sheet
    val currentLyrics = remember(currentAudioTrack?.filePath) {
        val path = currentAudioTrack?.filePath ?: ""
        val lrcFile = if (path.isNotBlank()) File(path.substringBeforeLast('.') + ".lrc") else null
        val raw = if (lrcFile != null && lrcFile.exists()) {
            lrcFile.readText()
        } else {
            """
            [00:00.00] ♪ ${currentAudioTrack?.title ?: "Studio Track"} — Lossless Signal Active
            [00:03.50] 4-State Loop (Off / Queue / Track / A-B Section) ready
            [00:07.00] Tap any synced lyric line to seek directly to that timestamp
            [00:11.00] 10-Band Equalizer & 60fps FFT Spectrogram running in real time
            """.trimIndent()
        }
        MultiQueueManager.parseLrcLyrics(raw)
    }

    val currentCueSlices = remember(currentAudioTrack?.filePath, currentAudioTrack?.durationMs) {
        val path = currentAudioTrack?.filePath ?: ""
        val cueFile = if (path.isNotBlank()) File(path.substringBeforeLast('.') + ".cue") else null
        if (cueFile != null && cueFile.exists()) {
            MultiQueueManager.parseCueSheet(
                cueFile.readText(),
                path,
                currentAudioTrack?.durationMs ?: 18_000L
            )
        } else emptyList()
    }

    val currentSignalReport = remember(currentAudioTrack, dspState) {
        audioDspController.buildSignalPathReport(
            codecName = currentAudioTrack?.codecName ?: "FLAC / PCM",
            bitDepth = currentAudioTrack?.bitDepth ?: 24,
            sampleRateHz = currentAudioTrack?.sampleRateHz ?: 96000,
            bitrateKbps = currentAudioTrack?.bitrateKbps ?: 1411,
            isLosslessSource = currentAudioTrack?.isLossless ?: true
        )
    }

    AudiophilePlayerSheet(
        visible = showAudiophilePlayerSheet,
        currentTrack = currentAudioTrack,
        isPlaying = isAudioPlaying,
        playbackPositionMs = audioPositionMs,
        trackDurationMs = currentAudioTrack?.durationMs ?: 18_000L,
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
            multiQueueManager.nudgeAbBounds(dStart, dEnd, currentAudioTrack?.durationMs ?: 18_000L)
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
            val outDir = File(context.filesDir, "studio_exports")
            val m3u = multiQueueManager.saveActiveQueueAsM3u8(outDir, activeQueueState.slot.displayName)
            statusNoticeText = "Exported queue playlist: ${m3u.name}"
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
                statusNoticeText = "Updated tags for '$title'"
            }
        },
        onOpenEqStudio = {
            showAudiophilePlayerSheet = false
            isEqSheetMinimized = false
            showEqStudioSheet = true
        },
        onOpenTrimmerForCurrent = {
            showAudiophilePlayerSheet = false
            trimmerTargetItem = currentAudioTrack
            showTrimmerStudioSheet = true
        }
    )

    // 3. 10-Band EQ, Parametric EQ, AutoEq Importer & 60fps Spectrum Studio Sheet
    AudioEqStudioSheet(
        visible = showEqStudioSheet,
        isMinimized = isEqSheetMinimized,
        onMinimizedChange = { isEqSheetMinimized = it },
        dspState = dspState,
        presets = audioDspController.allPresets(),
        spectrumBins = spectrumBins,
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
            statusNoticeText = "Saved custom 10-band EQ preset: $name"
        },
        onImportAutoEqProfile = { name, txt ->
            val preset = audioDspController.importAutoEqProfile(name, txt)
            statusNoticeText = "Imported ${preset.name} parametric curve"
        },
        onChangeBassBoost = { audioDspController.setBassBoostPercent(it) },
        onChangeVirtualizer = { audioDspController.setVirtualizerPercent(it) },
        onChangeLoudnessDb = { audioDspController.setLoudnessGainDb(it) },
        onSwitchOutputDevice = { audioDspController.switchOutputDevice(it) },
        onChangeVisualizerMode = { audioDspController.setVisualizerMode(it) }
    )

    // 4. Comfy Netflix-Style Video Theater Sheet
    val currentVideoSubtitles = remember(activeVideoItem?.id) {
        val rootDir = File(context.filesDir, "studio_samples")
        val enFile = File(rootDir, "Cyberpunk_Odyssey_S01E01.en.srt")
        val jpFile = File(rootDir, "Cyberpunk_Odyssey_S01E01.jp.srt")
        val enCues = if (enFile.exists()) VideoTheaterEngine.parseSrtSubtitles(enFile.readText()) else emptyList()
        val jpCues = if (jpFile.exists()) VideoTheaterEngine.parseSrtSubtitles(jpFile.readText()) else emptyList()
        VideoTheaterEngine.mergeDualSubtitles(enCues, jpCues)
    }

    VideoTheaterSheet(
        visible = showVideoTheaterSheet,
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
            statusNoticeText = "Saved video scene bookmark @ ${MediaItem.formatDurationMillis(videoPositionMs)}"
        },
        onCaptureFramePng = { vid, tsMs ->
            val outDir = File(context.filesDir, "studio_exports")
            val frameItem = mediaTrimmerEngine.captureVideoFrameStill(outDir, vid, tsMs, losslessPng = true)
            mediaRepository.addOrUpdateMediaItem(frameItem)
            statusNoticeText = "Saved lossless PNG frame: ${frameItem.title}"
        },
        onGenerateStoryboard = { vid ->
            val sheetItem = videoTheaterEngine.generateStoryboardContactSheet(context, vid, rows = 4, cols = 4)
            mediaRepository.addOrUpdateMediaItem(sheetItem)
            statusNoticeText = "Generated 4x4 Storyboard Contact Sheet PNG: ${sheetItem.title}"
        },
        onHandOffToBackgroundAudio = { vid, posMs ->
            // Seamless Video-to-Audio Background Handoff (F-V16)
            isVideoPlaying = false
            showVideoTheaterSheet = false
            val audioEquivalent = vid.copy(
                mediaType = MediaType.AUDIO,
                qualityBadge = "VIDEO AUDIO"
            )
            multiQueueManager.playNext(audioEquivalent)
            val targetIdx = (activeQueueState.activeIndex + 1).coerceAtMost(multiQueueManager.activeQueueState.value.tracks.lastIndex)
            multiQueueManager.selectTrackAt(targetIdx, resetAbLoop = false)
            audioPositionMs = posMs
            isAudioPlaying = true
            statusNoticeText = "Handed off '${vid.title}' to Background Audio Player @ ${MediaItem.formatDurationMillis(posMs)}"
        },
        onEnterPipMode = onRequestPipMode,
        onOpenTrimmerForVideo = { vid ->
            isVideoPlaying = false
            showVideoTheaterSheet = false
            trimmerTargetItem = vid
            showTrimmerStudioSheet = true
        }
    )

    // 5. Non-Destructive Cutter, Multi-Quality Audio Extractor & Batch Studio Sheet
    MediaTrimmerStudioSheet(
        visible = showTrimmerStudioSheet,
        targetItem = trimmerTargetItem,
        allMediaItems = allMediaItems,
        onDismiss = { showTrimmerStudioSheet = false },
        onExecuteTrimJob = { jobConfig ->
            val outDir = File(context.filesDir, "studio_exports")
            val result = mediaTrimmerEngine.executeTrimOrConvert(outDir, jobConfig)
            result.exportedItems.forEach { mediaRepository.addOrUpdateMediaItem(it) }
            statusNoticeText = result.summaryMessage
            showTrimmerStudioSheet = false
        },
        onExportGifLoop = { src, sMs, eMs, fps ->
            val outDir = File(context.filesDir, "studio_exports")
            val gifItem = mediaTrimmerEngine.exportAnimatedGifLoop(outDir, src, sMs, eMs, fps)
            mediaRepository.addOrUpdateMediaItem(gifItem)
            statusNoticeText = "Exported Animated GIF Loop: ${gifItem.title}"
            showTrimmerStudioSheet = false
        },
        onExecuteBatchConvert = { fmt, scale, quality, stripExif ->
            val outDir = File(context.filesDir, "studio_exports")
            val photos = allMediaItems.filter { it.mediaType == MediaType.PHOTO || it.mediaType == MediaType.RAW_PHOTO }.take(3)
            val converted = inspectorAndVaultEngine.batchConvertAndResizeImages(
                items = photos,
                outputDir = outDir,
                targetFormat = fmt,
                scaleFactor = scale,
                qualityPercent = quality,
                stripExif = stripExif
            )
            converted.forEach { mediaRepository.addOrUpdateMediaItem(it) }
            statusNoticeText = "Batch-converted ${converted.size} images to ${fmt.ext}"
            showTrimmerStudioSheet = false
        }
    )

    // 6. Deep EXIF, 256-Bin RGB Histogram, Palette Extractor & PIN Private Vault Sheet
    val currentInspectionReport = remember(inspectorTargetItem) {
        inspectorTargetItem?.let { inspectorAndVaultEngine.inspectMediaItem(it) }
    }

    MediaInspectorAndVaultSheet(
        visible = showInspectorVaultSheet,
        initialTab = inspectorInitialTab,
        targetItem = inspectorTargetItem ?: allMediaItems.firstOrNull(),
        inspectionReport = currentInspectionReport ?: allMediaItems.firstOrNull()?.let { inspectorAndVaultEngine.inspectMediaItem(it) },
        vaultUnlocked = vaultUnlockedState,
        vaultItems = vaultItems,
        trashedItems = trashedItems,
        duplicateGroups = duplicateGroups,
        onDismiss = { showInspectorVaultSheet = false },
        onScrubExifPrivacy = { item ->
            val outDir = File(context.filesDir, "studio_exports")
            val cleanItem = inspectorAndVaultEngine.scrubExifPrivacyMetadata(item, outDir)
            mediaRepository.addOrUpdateMediaItem(cleanItem)
            inspectorTargetItem = cleanItem
            statusNoticeText = "Scrubbed GPS & camera serial metadata: ${cleanItem.title}"
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
            statusNoticeText = if (toVault) "Moved '${item.title}' to PIN Private Vault" else "Restored '${item.title}' from Vault"
        },
        onMoveToTrash = { item ->
            mediaRepository.moveToTrash(item.id)
            statusNoticeText = "Moved '${item.title}' to 30-Day Recycle Bin"
        },
        onRestoreFromTrash = { item ->
            mediaRepository.restoreFromTrash(item.id)
            statusNoticeText = "Restored '${item.title}' from Recycle Bin"
        }
    )
}

@Composable
private fun PersistentMiniPlayerPill(
    track: MediaItem,
    isPlaying: Boolean,
    positionMs: Long,
    loopShortBadge: String,
    queueName: String,
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
            .border(BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent), RoundedCornerShape(24.dp))
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f))
                    .border(1.dp, ElectricSlateTokens.PrimaryAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
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
                    text = "${track.qualityBadge} • $queueName • ${MediaItem.formatDurationMillis(positionMs)}",
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
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
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
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
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
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                    .jigglePressClickable(onClick = onOpenEq),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Equalizer,
                    contentDescription = "EQ",
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
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ElectricSlateTokens.BgCanvas)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
            .jigglePressClickable(onClick = onResume)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = video.qualityBadge,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = ElectricSlateTokens.PrimaryAccent
            )
            Text(
                text = video.remainingMinutesLabel,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = ElectricSlateTokens.TextMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.title,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ElectricSlateTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Progress Bar
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
    onPlayNextInQueue: () -> Unit,
    onOpenTrimmer: () -> Unit,
    onOpenInspector: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AppCard(
        modifier = modifier,
        onClick = onOpenPrimary,
        isGlow = item.isLossless || item.isHdr,
        cornerRadius = 20.dp,
        contentPadding = if (compact) 8.dp else 12.dp
    ) {
        // Preview Thumbnail / Visualizer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 84.dp else 118.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ElectricSlateTokens.BgCanvas)
                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(14.dp))
        ) {
            if (item.mediaType == MediaType.PHOTO || item.mediaType == MediaType.RAW_PHOTO || item.mediaType == MediaType.ANIMATED) {
                AsyncImage(
                    model = File(item.filePath).takeIf { it.exists() } ?: item.uriString,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.mediaType == MediaType.AUDIO) Icons.Outlined.AudioFile else Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(if (compact) 28.dp else 38.dp)
                    )
                }
            }

            // Top-Left Format / Quality Badge (F-G02)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.85f))
                    .border(1.dp, ElectricSlateTokens.PrimaryAccent, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.qualityBadge,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
            }

            // Top-Right Favorite Toggle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.8f))
                    .jigglePressClickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom-Right Duration or Resolution Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (item.durationMs > 0L) item.formattedDuration else item.formattedSize,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 12.sp else 14.sp,
            color = ElectricSlateTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!compact) {
            Text(
                text = "${item.artist} • ${item.formattedSize}",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = ElectricSlateTokens.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.mediaType == MediaType.AUDIO || item.mediaType == MediaType.VIDEO) {
                    Text(
                        text = "+Queue",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.jigglePressClickable(onClick = onPlayNextInQueue)
                    )
                    Text(
                        text = "Cut/Convert",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.jigglePressClickable(onClick = onOpenTrimmer)
                    )
                } else {
                    Text(
                        text = "Compare/Loupe",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.jigglePressClickable(onClick = onOpenPrimary)
                    )
                }
                Text(
                    text = "Inspect",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.TextMuted,
                    modifier = Modifier.jigglePressClickable(onClick = onOpenInspector)
                )
            }
        }
    }
}
