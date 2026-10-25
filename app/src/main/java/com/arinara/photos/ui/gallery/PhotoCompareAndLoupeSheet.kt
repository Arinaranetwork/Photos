// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.gallery

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
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
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PhotoCompareMode(val label: String) {
    SINGLE_PRO("Single"),
    COMPARE_2UP("2-Up"),
    COMPARE_4UP("4-Up")
}

@Composable
fun PhotoCompareAndLoupeSheet(
    visible: Boolean,
    primaryItem: MediaItem?,
    comparisonCandidates: List<MediaItem>,
    onDismiss: () -> Unit,
    onOpenInspector: (MediaItem) -> Unit,
    onRecordVoiceNote: (MediaItem) -> Unit,
    onPlayVoiceNote: (MediaItem) -> Unit,
    onSelectPhoto: (MediaItem) -> Unit = {},
    onOpenEditor: (MediaItem) -> Unit = {}
) {
    if (!visible || primaryItem == null) return

    var activeItem by remember(primaryItem.id) { mutableStateOf(primaryItem) }
    var compareMode by remember { mutableStateOf(PhotoCompareMode.SINGLE_PRO) }
    var syncZoomLocked by remember { mutableStateOf(true) }
    var sharedZoomScale by remember { mutableFloatStateOf(1f) }
    var sharedPanOffset by remember { mutableStateOf(Offset.Zero) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    var swipeOffsetY by remember { mutableFloatStateOf(0f) }

    var isImmersedFullscreen by remember { mutableStateOf(false) }
    var loupeEnabled by remember { mutableStateOf(false) }
    var loupePositionFraction by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var focusPeakingEnabled by remember { mutableStateOf(false) }

    BackHandler(enabled = visible) {
        if (isImmersedFullscreen) {
            isImmersedFullscreen = false
        } else {
            onDismiss()
        }
    }

    val photoList = remember(comparisonCandidates, activeItem) {
        val filtered = comparisonCandidates.filter {
            it.mediaType == MediaType.PHOTO || it.mediaType == MediaType.RAW_PHOTO || it.mediaType == MediaType.ANIMATED
        }
        if (filtered.any { it.id == activeItem.id }) filtered else listOf(activeItem) + filtered
    }

    val activePhotoIndex = remember(photoList, activeItem.id) {
        photoList.indexOfFirst { it.id == activeItem.id }.coerceAtLeast(0)
    }

    val compareItems = remember(activeItem, photoList, compareMode) {
        val others = photoList.filter { it.id != activeItem.id }
        when (compareMode) {
            PhotoCompareMode.SINGLE_PRO -> listOf(activeItem)
            PhotoCompareMode.COMPARE_2UP -> listOfNotNull(activeItem, others.firstOrNull() ?: activeItem)
            PhotoCompareMode.COMPARE_4UP -> (listOf(activeItem) + others).take(4)
        }
    }

    val currentZoom by rememberUpdatedState(sharedZoomScale)
    val currentPan by rememberUpdatedState(sharedPanOffset)
    val currentLoupeEnabled by rememberUpdatedState(loupeEnabled)
    val currentLoupePos by rememberUpdatedState(loupePositionFraction)
    val currentImmersed by rememberUpdatedState(isImmersedFullscreen)
    val currentPhotoList by rememberUpdatedState(photoList)
    val currentActivePhotoIndex by rememberUpdatedState(activePhotoIndex)
    val currentCompareMode by rememberUpdatedState(compareMode)
    val currentSwipeX by rememberUpdatedState(swipeOffsetX)
    val currentSwipeY by rememberUpdatedState(swipeOffsetY)

    // Full-Screen Edge-to-Edge Immersive Image Viewer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElectricSlateTokens.BgCanvas)
    ) {
        // Full-Screen Interactive Photo Viewport (Supports 1-Finger Swipe Next/Prev & Swipe-Down Dismiss at 1.0x Zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (currentLoupeEnabled) {
                                val fx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                                val fy = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                                loupePositionFraction = Offset(fx, fy)
                            } else {
                                isImmersedFullscreen = !currentImmersed
                            }
                        },
                        onDoubleTap = {
                            if (currentZoom > 1.05f) {
                                sharedZoomScale = 1f
                                sharedPanOffset = Offset.Zero
                            } else {
                                sharedZoomScale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            swipeOffsetX = 0f
                            swipeOffsetY = 0f
                        },
                        onDragCancel = {
                            swipeOffsetX = 0f
                            swipeOffsetY = 0f
                        },
                        onDragEnd = {
                            val w = size.width.coerceAtLeast(1).toFloat()
                            val list = currentPhotoList
                            val idx = currentActivePhotoIndex
                            if (currentZoom <= 1.03f && !currentLoupeEnabled && currentCompareMode == PhotoCompareMode.SINGLE_PRO) {
                                val dx = currentSwipeX
                                val dy = currentSwipeY
                                if (dy > 110.dp.toPx() && abs(dy) > abs(dx) * 1.15f) {
                                    swipeOffsetX = 0f
                                    swipeOffsetY = 0f
                                    onDismiss()
                                    return@detectDragGestures
                                }
                                if (list.size > 1 && abs(dx) > w * 0.16f && abs(dx) >= abs(dy)) {
                                    val targetIdx = if (dx < 0f) {
                                        (idx + 1) % list.size
                                    } else {
                                        if (idx - 1 < 0) list.lastIndex else idx - 1
                                    }
                                    val nextPhoto = list[targetIdx]
                                    activeItem = nextPhoto
                                    onSelectPhoto(nextPhoto)
                                    sharedZoomScale = 1f
                                    sharedPanOffset = Offset.Zero
                                }
                            }
                            swipeOffsetX = 0f
                            swipeOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            if (currentLoupeEnabled) {
                                change.consume()
                                val fx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                                val fy = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                                loupePositionFraction = Offset(fx, fy)
                            } else if (currentZoom <= 1.03f && currentCompareMode == PhotoCompareMode.SINGLE_PRO) {
                                change.consume()
                                swipeOffsetX = (currentSwipeX + dragAmount.x).coerceIn(-size.width.toFloat(), size.width.toFloat())
                                swipeOffsetY = (currentSwipeY + dragAmount.y).coerceIn(-220f, 420f)
                            } else if (currentZoom > 1.03f) {
                                change.consume()
                                val maxPanX = (size.width * (currentZoom - 1f) * 0.5f).coerceAtLeast(120f)
                                val maxPanY = (size.height * (currentZoom - 1f) * 0.5f).coerceAtLeast(120f)
                                sharedPanOffset = Offset(
                                    x = (currentPan.x + dragAmount.x).coerceIn(-maxPanX, maxPanX),
                                    y = (currentPan.y + dragAmount.y).coerceIn(-maxPanY, maxPanY)
                                )
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            val newZoom = (currentZoom * zoom).coerceIn(1f, 8f)
                            sharedZoomScale = newZoom
                            if (newZoom > 1.01f) {
                                val maxPanX = (size.width * (newZoom - 1f) * 0.5f).coerceAtLeast(120f)
                                val maxPanY = (size.height * (newZoom - 1f) * 0.5f).coerceAtLeast(120f)
                                sharedPanOffset = Offset(
                                    x = (currentPan.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                    y = (currentPan.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                )
                            } else {
                                sharedPanOffset = Offset.Zero
                            }
                        }
                    }
                }
        ) {
            when (compareMode) {
                PhotoCompareMode.SINGLE_PRO -> {
                    PhotoViewportTile(
                        item = compareItems.first(),
                        zoomScale = sharedZoomScale,
                        panOffset = Offset(
                            x = sharedPanOffset.x + swipeOffsetX,
                            y = sharedPanOffset.y + (swipeOffsetY * 0.35f)
                        ),
                        focusPeaking = focusPeakingEnabled,
                        loupeEnabled = loupeEnabled,
                        loupeFraction = currentLoupePos,
                        showMetadataBadge = !isImmersedFullscreen,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                PhotoCompareMode.COMPARE_2UP -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        compareItems.take(2).forEach { item ->
                            PhotoViewportTile(
                                item = item,
                                zoomScale = sharedZoomScale,
                                panOffset = sharedPanOffset,
                                focusPeaking = focusPeakingEnabled,
                                loupeEnabled = loupeEnabled,
                                loupeFraction = currentLoupePos,
                                showMetadataBadge = !isImmersedFullscreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
                PhotoCompareMode.COMPARE_4UP -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            compareItems.take(2).forEach { item ->
                                PhotoViewportTile(
                                    item = item,
                                    zoomScale = sharedZoomScale,
                                    panOffset = sharedPanOffset,
                                    focusPeaking = focusPeakingEnabled,
                                    loupeEnabled = false,
                                    loupeFraction = currentLoupePos,
                                    showMetadataBadge = !isImmersedFullscreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                )
                            }
                        }
                        if (compareItems.size > 2) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                compareItems.drop(2).take(2).forEach { item ->
                                    PhotoViewportTile(
                                        item = item,
                                        zoomScale = sharedZoomScale,
                                        panOffset = sharedPanOffset,
                                        focusPeaking = focusPeakingEnabled,
                                        loupeEnabled = false,
                                        loupeFraction = currentLoupePos,
                                        showMetadataBadge = !isImmersedFullscreen,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtle Exit-Fullscreen Pill when in Full Immersion Mode
        AnimatedVisibility(
            visible = isImmersedFullscreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.78f))
                    .jigglePressClickable { isImmersedFullscreen = false }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = ElectricSlateTokens.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${(sharedZoomScale * 100).roundToInt()}%",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
            }
        }

        // Floating Top Header Bar (Hidden during Fullscreen Immersion)
        AnimatedVisibility(
            visible = !isImmersedFullscreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ElectricSlateTokens.BgCanvas.copy(alpha = 0.90f),
                                ElectricSlateTokens.BgCanvas.copy(alpha = 0.0f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.88f))
                        .jigglePressClickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeItem.title,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ElectricSlateTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dimsText = if (activeItem.width > 0 && activeItem.height > 0) {
                        "${activeItem.width}x${activeItem.height} • "
                    } else ""
                    Text(
                        text = "${activePhotoIndex + 1} / ${photoList.size} • $dimsText${activeItem.formattedSize}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.88f))
                            .jigglePressClickable { MediaShareHelper.shareMediaItem(context, activeItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Photo",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.88f))
                            .jigglePressClickable { onOpenEditor(activeItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Edit Photo",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.88f))
                            .jigglePressClickable { isImmersedFullscreen = true },
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.88f))
                            .jigglePressClickable { onOpenInspector(activeItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Details",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Floating Bottom HUD Deck (Hidden during Fullscreen Immersion)
        AnimatedVisibility(
            visible = !isImmersedFullscreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    contentPadding = 14.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Horizontally Scrollable Mode & Tool Chip Row (Prevents "Focus" vertical wrap)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PhotoCompareMode.entries.forEach { mode ->
                                AppPillChip(
                                    label = mode.label,
                                    isSelected = compareMode == mode,
                                    onClick = { compareMode = mode }
                                )
                            }
                            if (compareMode != PhotoCompareMode.SINGLE_PRO) {
                                AppPillChip(
                                    label = if (syncZoomLocked) "Sync" else "Free",
                                    leadingIcon = Icons.Outlined.Lock,
                                    isSelected = syncZoomLocked,
                                    onClick = { syncZoomLocked = !syncZoomLocked }
                                )
                            }
                            AppPillChip(
                                label = "Loupe",
                                leadingIcon = Icons.Outlined.ZoomIn,
                                isSelected = loupeEnabled,
                                onClick = { loupeEnabled = !loupeEnabled }
                            )
                            AppPillChip(
                                label = "Focus",
                                leadingIcon = Icons.Outlined.CenterFocusStrong,
                                isSelected = focusPeakingEnabled,
                                onClick = { focusPeakingEnabled = !focusPeakingEnabled }
                            )
                            AppPillChip(
                                label = "Fullscreen",
                                leadingIcon = Icons.Outlined.Fullscreen,
                                isSelected = isImmersedFullscreen,
                                onClick = { isImmersedFullscreen = true }
                            )
                        }

                        // Zoom Slider Row with AppSlider horizontal thumb inset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Zoom",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            AppSlider(
                                value = sharedZoomScale,
                                onValueChange = {
                                    sharedZoomScale = it
                                    if (it <= 1.05f) sharedPanOffset = Offset.Zero
                                },
                                valueRange = 1f..8f,
                                steps = 0,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(sharedZoomScale * 100).roundToInt()}%",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = "Reset",
                                onClick = {
                                    sharedZoomScale = 1f
                                    sharedPanOffset = Offset.Zero
                                },
                                variant = AppButtonVariant.SECONDARY,
                                height = 34.dp,
                                horizontalPadding = 12.dp,
                                fontSize = 11.sp
                            )
                        }

                        // Previous / Details / Next Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (photoList.size > 1) {
                                AppButton(
                                    text = "Prev",
                                    leadingIcon = Icons.Outlined.ChevronLeft,
                                    onClick = {
                                        val prevIdx = if (activePhotoIndex - 1 < 0) photoList.lastIndex else activePhotoIndex - 1
                                        val nextPhoto = photoList[prevIdx]
                                        activeItem = nextPhoto
                                        onSelectPhoto(nextPhoto)
                                        sharedZoomScale = 1f
                                        sharedPanOffset = Offset.Zero
                                    },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 38.dp,
                                    horizontalPadding = 12.dp,
                                    fontSize = 12.sp
                                )
                            }
                            AppButton(
                                text = "Edit",
                                leadingIcon = Icons.Outlined.Tune,
                                onClick = { onOpenEditor(activeItem) },
                                variant = AppButtonVariant.PRIMARY,
                                height = 38.dp,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "Details",
                                leadingIcon = Icons.Outlined.Info,
                                onClick = { onOpenInspector(activeItem) },
                                variant = AppButtonVariant.SECONDARY,
                                height = 38.dp,
                                modifier = Modifier.weight(1f)
                            )
                            if (photoList.size > 1) {
                                AppButton(
                                    text = "Next",
                                    leadingIcon = Icons.Outlined.ChevronRight,
                                    onClick = {
                                        val nextIdx = (activePhotoIndex + 1) % photoList.size
                                        val nextPhoto = photoList[nextIdx]
                                        activeItem = nextPhoto
                                        onSelectPhoto(nextPhoto)
                                        sharedZoomScale = 1f
                                        sharedPanOffset = Offset.Zero
                                    },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 38.dp,
                                    horizontalPadding = 12.dp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoViewportTile(
    item: MediaItem,
    zoomScale: Float,
    panOffset: Offset,
    focusPeaking: Boolean,
    loupeEnabled: Boolean,
    loupeFraction: Offset,
    showMetadataBadge: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var decodedBitmap by remember(item.uriString, item.filePath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uriString, item.filePath, loupeEnabled) {
        if (!loupeEnabled || decodedBitmap != null) return@LaunchedEffect
        decodedBitmap = withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                val f = File(item.filePath)
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.absolutePath, opts)?.asImageBitmap()
                } else {
                    context.contentResolver.openInputStream(Uri.parse(item.uriString))?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier.background(ElectricSlateTokens.BgCanvas)
    ) {
        AsyncImage(
            model = File(item.filePath).takeIf { it.exists() } ?: Uri.parse(item.uriString),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        )

        if (focusPeaking || loupeEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (focusPeaking) {
                    val gridStep = 28.dp.toPx()
                    var y = gridStep
                    while (y < h - gridStep) {
                        var x = gridStep
                        while (x < w - gridStep) {
                            val distCenter = kotlin.math.hypot((x - w * 0.5f).toDouble(), (y - h * 0.5f).toDouble()).toFloat()
                            if (distCenter < w * 0.36f && ((x.toInt() + y.toInt()) % 3 == 0)) {
                                drawCircle(
                                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.85f),
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            x += gridStep * 0.65f
                        }
                        y += gridStep * 0.65f
                    }
                }

                if (loupeEnabled) {
                    val cx = (loupeFraction.x * w).coerceIn(60f, w - 60f)
                    val cy = (loupeFraction.y * h).coerceIn(60f, h - 60f)
                    val radius = 56.dp.toPx()

                    drawCircle(
                        color = ElectricSlateTokens.BgCanvas,
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    val bmp = decodedBitmap
                    if (bmp != null) {
                        val srcW = (bmp.width * 0.14f).toInt().coerceAtLeast(16)
                        val srcH = (bmp.height * 0.14f).toInt().coerceAtLeast(16)
                        val srcX = ((loupeFraction.x * bmp.width).toInt() - srcW / 2).coerceIn(0, (bmp.width - srcW).coerceAtLeast(0))
                        val srcY = ((loupeFraction.y * bmp.height).toInt() - srcH / 2).coerceIn(0, (bmp.height - srcH).coerceAtLeast(0))
                        drawImage(
                            image = bmp,
                            srcOffset = IntOffset(srcX, srcY),
                            srcSize = IntSize(srcW, srcH),
                            dstOffset = IntOffset((cx - radius * 0.78f).toInt(), (cy - radius * 0.78f).toInt()),
                            dstSize = IntSize((radius * 1.56f).toInt(), (radius * 1.56f).toInt())
                        )
                    } else {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricSlateTokens.PrimaryAccent, ElectricSlateTokens.SurfaceElevated),
                                center = Offset(cx, cy),
                                radius = radius
                            ),
                            topLeft = Offset(cx - radius * 0.7f, cy - radius * 0.7f),
                            size = Size(radius * 1.4f, radius * 1.4f)
                        )
                    }

                    drawCircle(
                        color = ElectricSlateTokens.PrimaryAccent,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
