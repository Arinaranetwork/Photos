// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.gallery

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppBottomSheet
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppSlider
import com.arinara.photos.ui.theme.ElectricSlateTokens
import java.io.File
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
    onPlayVoiceNote: (MediaItem) -> Unit
) {
    if (!visible || primaryItem == null) return

    var compareMode by remember { mutableStateOf(PhotoCompareMode.SINGLE_PRO) }
    var syncZoomLocked by remember { mutableStateOf(true) }
    var sharedZoomScale by remember { mutableFloatStateOf(1f) }
    var sharedPanOffset by remember { mutableStateOf(Offset.Zero) }

    var loupeEnabled by remember { mutableStateOf(false) }
    var loupePositionFraction by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var focusPeakingEnabled by remember { mutableStateOf(false) }

    val compareItems = remember(primaryItem, comparisonCandidates, compareMode) {
        val photos = comparisonCandidates.filter {
            it.mediaType == MediaType.PHOTO || it.mediaType == MediaType.RAW_PHOTO || it.mediaType == MediaType.ANIMATED
        }
        val others = photos.filter { it.id != primaryItem.id }
        when (compareMode) {
            PhotoCompareMode.SINGLE_PRO -> listOf(primaryItem)
            PhotoCompareMode.COMPARE_2UP -> listOfNotNull(primaryItem, others.firstOrNull() ?: primaryItem)
            PhotoCompareMode.COMPARE_4UP -> (listOf(primaryItem) + others).take(4)
        }
    }

    val currentZoom by rememberUpdatedState(sharedZoomScale)
    val currentPan by rememberUpdatedState(sharedPanOffset)
    val currentLoupeEnabled by rememberUpdatedState(loupeEnabled)
    val currentLoupePos by rememberUpdatedState(loupePositionFraction)

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = primaryItem.title
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }

            // Photo Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElectricSlateTokens.BgCanvas)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (currentZoom * zoom).coerceIn(1f, 8f)
                            sharedZoomScale = newZoom
                            if (newZoom > 1f) {
                                sharedPanOffset = Offset(
                                    x = (currentPan.x + pan.x).coerceIn(-600f, 600f),
                                    y = (currentPan.y + pan.y).coerceIn(-400f, 400f)
                                )
                            } else {
                                sharedPanOffset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            if (currentLoupeEnabled) {
                                change.consume()
                                val fx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                                val fy = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                                loupePositionFraction = Offset(fx, fy)
                            }
                        }
                    }
            ) {
                when (compareMode) {
                    PhotoCompareMode.SINGLE_PRO -> {
                        PhotoViewportTile(
                            item = compareItems.first(),
                            zoomScale = sharedZoomScale,
                            panOffset = sharedPanOffset,
                            focusPeaking = focusPeakingEnabled,
                            loupeEnabled = loupeEnabled,
                            loupeFraction = currentLoupePos,
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(sharedZoomScale * 100).roundToInt()}%",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                }
            }

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
                AppButton(
                    text = "Reset",
                    onClick = {
                        sharedZoomScale = 1f
                        sharedPanOffset = Offset.Zero
                    },
                    variant = AppButtonVariant.SECONDARY,
                    height = 36.dp,
                    horizontalPadding = 12.dp,
                    fontSize = 12.sp
                )
            }

            AppButton(
                text = "Details",
                leadingIcon = Icons.Outlined.Info,
                onClick = { onOpenInspector(primaryItem) },
                variant = AppButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val decodedBitmap: ImageBitmap? = remember(item.uriString, item.filePath) {
        try {
            val f = File(item.filePath)
            if (f.exists()) {
                BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
            } else {
                context.contentResolver.openInputStream(Uri.parse(item.uriString))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ElectricSlateTokens.SurfacePanel)
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
                    if (decodedBitmap != null) {
                        val srcW = (decodedBitmap.width * 0.14f).toInt().coerceAtLeast(16)
                        val srcH = (decodedBitmap.height * 0.14f).toInt().coerceAtLeast(16)
                        val srcX = ((loupeFraction.x * decodedBitmap.width).toInt() - srcW / 2).coerceIn(0, (decodedBitmap.width - srcW).coerceAtLeast(0))
                        val srcY = ((loupeFraction.y * decodedBitmap.height).toInt() - srcH / 2).coerceIn(0, (decodedBitmap.height - srcH).coerceAtLeast(0))
                        drawImage(
                            image = decodedBitmap,
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

        if (item.width > 0 && item.height > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${item.width}x${item.height} • ${item.formattedSize}",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = ElectricSlateTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
