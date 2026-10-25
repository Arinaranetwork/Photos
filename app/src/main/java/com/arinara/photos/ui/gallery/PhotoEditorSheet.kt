// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.media.editor.CurveChannel
import com.arinara.photos.media.editor.EditorAspectPreset
import com.arinara.photos.media.editor.EditorModule
import com.arinara.photos.media.editor.FilmStockPreset
import com.arinara.photos.media.editor.HslBandAdjustment
import com.arinara.photos.media.editor.HslColorBand
import com.arinara.photos.media.editor.PhotoEditRecipe
import com.arinara.photos.media.editor.PhotoEditorEngine
import com.arinara.photos.media.editor.PhotoExportFormat
import com.arinara.photos.media.editor.RedactionMode
import com.arinara.photos.media.editor.RedactionRegion
import com.arinara.photos.media.editor.ScopeMode
import com.arinara.photos.media.editor.SelectiveMaskType
import com.arinara.photos.media.inspector.RgbHistogramData
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.components.presets.AppSlider
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PhotoEditorSheet(
    visible: Boolean,
    targetItem: MediaItem?,
    copiedRecipe: PhotoEditRecipe?,
    onCopyRecipe: (PhotoEditRecipe) -> Unit,
    onDismiss: () -> Unit,
    onExportedPhoto: (MediaItem, String) -> Unit
) {
    if (!visible || targetItem == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = visible, onBack = onDismiss)

    var activeRecipe by remember(targetItem.id) { mutableStateOf(PhotoEditRecipe()) }
    val undoStack = remember(targetItem.id) { mutableStateListOf<PhotoEditRecipe>() }
    val redoStack = remember(targetItem.id) { mutableStateListOf<PhotoEditRecipe>() }

    fun updateRecipe(newRecipe: PhotoEditRecipe, pushUndo: Boolean = false) {
        if (pushUndo && newRecipe != activeRecipe) {
            if (undoStack.size >= 20) undoStack.removeAt(0)
            undoStack.add(activeRecipe)
            redoStack.clear()
        }
        activeRecipe = newRecipe
    }

    var selectedModule by remember { mutableStateOf(EditorModule.LIGHT) }
    var splitCurtainEnabled by remember { mutableStateOf(false) }
    var splitCurtainFraction by remember { mutableFloatStateOf(0.5f) }
    var scopeMode by remember { mutableStateOf(ScopeMode.HISTOGRAM_RGB) }
    var zebraClippingEnabled by remember { mutableStateOf(false) }
    var eyedropperActive by remember { mutableStateOf(false) }
    var redactTapModeActive by remember { mutableStateOf(false) }
    var selectedRedactionMode by remember { mutableStateOf(RedactionMode.PIXELATE_MOSAIC) }
    var redactBrushRadius by remember { mutableFloatStateOf(0.08f) }
    var redactBrushStrength by remember { mutableFloatStateOf(65f) }

    var exportFormat by remember { mutableStateOf(PhotoExportFormat.JPEG_95) }
    var preserveExif by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Parameter indices per module
    var selectedTransformParamIdx by remember { mutableIntStateOf(0) }
    var selectedLightParamIdx by remember { mutableIntStateOf(0) }
    var selectedCurveChannel by remember { mutableStateOf(CurveChannel.MASTER_LUMA) }
    var selectedParametricZoneIdx by remember { mutableIntStateOf(0) }
    var selectedHslBand by remember { mutableStateOf(HslColorBand.RED) }
    var selectedGradeWheelIdx by remember { mutableIntStateOf(0) }
    var selectedCalibrationParamIdx by remember { mutableIntStateOf(0) }
    var selectedDetailParamIdx by remember { mutableIntStateOf(0) }
    var selectedOpticsParamIdx by remember { mutableIntStateOf(0) }
    var selectedMaskParamIdx by remember { mutableIntStateOf(0) }

    // Decode preview source bitmap on IO thread
    var previewSourceBitmap by remember(targetItem.id) { mutableStateOf<Bitmap?>(null) }
    var previewProcessedBitmap by remember(targetItem.id) { mutableStateOf<Bitmap?>(null) }
    var liveHistogram by remember(targetItem.id) { mutableStateOf<RgbHistogramData?>(null) }

    LaunchedEffect(targetItem.uriString, targetItem.filePath) {
        val decoded = withContext(Dispatchers.IO) {
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val f = File(targetItem.filePath)
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.absolutePath, boundsOpts)
                } else {
                    context.contentResolver.openInputStream(Uri.parse(targetItem.uriString))?.use {
                        BitmapFactory.decodeStream(it, null, boundsOpts)
                    }
                }
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
                var sample = 1
                while (maxDim / sample > 960) {
                    sample *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.absolutePath, decodeOpts)
                } else {
                    context.contentResolver.openInputStream(Uri.parse(targetItem.uriString))?.use {
                        BitmapFactory.decodeStream(it, null, decodeOpts)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
        previewSourceBitmap = decoded
        if (decoded != null) {
            previewProcessedBitmap = decoded
            liveHistogram = withContext(Dispatchers.Default) {
                PhotoEditorEngine.computeLiveHistogram(decoded)
            }
        }
    }

    // Re-evaluate non-destructive pixel pipeline on Default dispatcher whenever activeRecipe changes
    LaunchedEffect(previewSourceBitmap, activeRecipe) {
        val src = previewSourceBitmap ?: return@LaunchedEffect
        val (processed, hist) = withContext(Dispatchers.Default) {
            val out = PhotoEditorEngine.processBitmap(src, activeRecipe)
            val h = PhotoEditorEngine.computeLiveHistogram(out)
            out to h
        }
        previewProcessedBitmap = processed
        liveHistogram = hist
    }

    val currentZoom by rememberUpdatedState(zoomScale)
    val currentPan by rememberUpdatedState(panOffset)
    val currentSplitEnabled by rememberUpdatedState(splitCurtainEnabled)
    val currentEyedropper by rememberUpdatedState(eyedropperActive)
    val currentRedactTap by rememberUpdatedState(redactTapModeActive)
    val currentRecipe by rememberUpdatedState(activeRecipe)
    val currentSrcBitmap by rememberUpdatedState(previewSourceBitmap)
    val currentRedactMode by rememberUpdatedState(selectedRedactionMode)
    val currentRedactRad by rememberUpdatedState(redactBrushRadius)
    val currentRedactStr by rememberUpdatedState(redactBrushStrength)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElectricSlateTokens.BgCanvas)
    ) {
        // 1. Center Interactive Darkroom Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 68.dp, bottom = 248.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val w = size.width.coerceAtLeast(1).toFloat()
                            val h = size.height.coerceAtLeast(1).toFloat()
                            val nx = (offset.x / w).coerceIn(0f, 1f)
                            val ny = (offset.y / h).coerceIn(0f, 1f)
                            val bmp = currentSrcBitmap
                            when {
                                currentEyedropper && bmp != null -> {
                                    val (newTemp, newTint) = PhotoEditorEngine.sampleNeutralWhiteBalance(bmp, nx, ny)
                                    updateRecipe(
                                        currentRecipe.copy(temperature = newTemp, tint = newTint),
                                        pushUndo = true
                                    )
                                    eyedropperActive = false
                                }
                                currentRedactTap -> {
                                    val newRegion = RedactionRegion(
                                        centerX = nx,
                                        centerY = ny,
                                        radius = currentRedactRad,
                                        mode = currentRedactMode,
                                        strength = currentRedactStr
                                    )
                                    updateRecipe(
                                        currentRecipe.copy(redactionRegions = currentRecipe.redactionRegions + newRegion),
                                        pushUndo = true
                                    )
                                }
                                currentRecipe.maskType != SelectiveMaskType.NONE && selectedModule == EditorModule.MASK_REDACT -> {
                                    updateRecipe(
                                        currentRecipe.copy(maskCenterX = nx, maskCenterY = ny),
                                        pushUndo = true
                                    )
                                }
                            }
                        },
                        onDoubleTap = {
                            if (currentZoom > 1.05f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            } else {
                                zoomScale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        if (currentSplitEnabled) {
                            change.consume()
                            val w = size.width.coerceAtLeast(1).toFloat()
                            splitCurtainFraction = (change.position.x / w).coerceIn(0.05f, 0.95f)
                        } else if (currentRecipe.maskType != SelectiveMaskType.NONE && selectedModule == EditorModule.MASK_REDACT && !currentRedactTap) {
                            change.consume()
                            val w = size.width.coerceAtLeast(1).toFloat()
                            val h = size.height.coerceAtLeast(1).toFloat()
                            updateRecipe(
                                currentRecipe.copy(
                                    maskCenterX = (change.position.x / w).coerceIn(0.05f, 0.95f),
                                    maskCenterY = (change.position.y / h).coerceIn(0.05f, 0.95f)
                                )
                            )
                        } else if (currentZoom > 1.01f) {
                            change.consume()
                            panOffset = Offset(
                                x = (currentPan.x + dragAmount.x).coerceIn(-600f, 600f),
                                y = (currentPan.y + dragAmount.y).coerceIn(-600f, 600f)
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newZ = (currentZoom * zoom).coerceIn(1f, 8f)
                        zoomScale = newZ
                        if (newZ > 1.01f) {
                            panOffset = Offset(
                                x = (currentPan.x + pan.x).coerceIn(-600f, 600f),
                                y = (currentPan.y + pan.y).coerceIn(-600f, 600f)
                            )
                        } else {
                            panOffset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val editedBmp = previewProcessedBitmap
            val origBmp = previewSourceBitmap

            if (editedBmp != null) {
                val editedImageBitmap = remember(editedBmp) { editedBmp.asImageBitmap() }
                val origImageBitmap = remember(origBmp) { origBmp?.asImageBitmap() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                ) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val bmpW = editedImageBitmap.width.coerceAtLeast(1)
                    val bmpH = editedImageBitmap.height.coerceAtLeast(1)
                    val fitScale = min(canvasW / bmpW.toFloat(), canvasH / bmpH.toFloat())
                    val dstW = (bmpW * fitScale).roundToInt().coerceAtLeast(1)
                    val dstH = (bmpH * fitScale).roundToInt().coerceAtLeast(1)
                    val dstLeft = ((canvasW - dstW) / 2f).roundToInt()
                    val dstTop = ((canvasH - dstH) / 2f).roundToInt()

                    drawImage(
                        image = editedImageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bmpW, bmpH),
                        dstOffset = IntOffset(dstLeft, dstTop),
                        dstSize = IntSize(dstW, dstH)
                    )

                    // Before / After Split Curtain Overlay
                    if (splitCurtainEnabled && origImageBitmap != null) {
                        val splitX = dstLeft + dstW * splitCurtainFraction
                        clipRect(
                            left = dstLeft.toFloat(),
                            top = dstTop.toFloat(),
                            right = splitX,
                            bottom = (dstTop + dstH).toFloat()
                        ) {
                            drawImage(
                                image = origImageBitmap,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(origImageBitmap.width, origImageBitmap.height),
                                dstOffset = IntOffset(dstLeft, dstTop),
                                dstSize = IntSize(dstW, dstH)
                            )
                        }
                        drawLine(
                            color = ElectricSlateTokens.PrimaryAccent,
                            start = Offset(splitX, dstTop.toFloat()),
                            end = Offset(splitX, (dstTop + dstH).toFloat()),
                            strokeWidth = 2.5.dp.toPx()
                        )
                        drawCircle(
                            color = ElectricSlateTokens.PrimaryAccent,
                            radius = 10.dp.toPx(),
                            center = Offset(splitX, dstTop + dstH * 0.5f)
                        )
                    }

                    // Rule-of-Thirds & Crop Guides when in Transform module
                    if (selectedModule == EditorModule.TRANSFORM) {
                        val thirdW = dstW / 3f
                        val thirdH = dstH / 3f
                        for (i in 1..2) {
                            drawLine(
                                color = ElectricSlateTokens.TextPrimary.copy(alpha = 0.35f),
                                start = Offset(dstLeft + i * thirdW, dstTop.toFloat()),
                                end = Offset(dstLeft + i * thirdW, (dstTop + dstH).toFloat()),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = ElectricSlateTokens.TextPrimary.copy(alpha = 0.35f),
                                start = Offset(dstLeft.toFloat(), dstTop + i * thirdH),
                                end = Offset((dstLeft + dstW).toFloat(), dstTop + i * thirdH),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Selective Mask Center Guide
                    if (selectedModule == EditorModule.MASK_REDACT && activeRecipe.maskType != SelectiveMaskType.NONE) {
                        val mcx = dstLeft + activeRecipe.maskCenterX * dstW
                        val mcy = dstTop + activeRecipe.maskCenterY * dstH
                        val mRad = activeRecipe.maskRadius * min(dstW, dstH)
                        drawCircle(
                            color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.75f),
                            radius = mRad,
                            center = Offset(mcx, mcy),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = ElectricSlateTokens.PrimaryAccent,
                            radius = 5.dp.toPx(),
                            center = Offset(mcx, mcy)
                        )
                    }
                }
            }
        }

        // 2. Top Floating Header + Live Scopes Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel)
                            .jigglePressClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Editor",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Undo Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel)
                            .jigglePressClickable {
                                if (undoStack.isNotEmpty()) {
                                    val prev = undoStack.removeAt(undoStack.lastIndex)
                                    redoStack.add(activeRecipe)
                                    activeRecipe = prev
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfacePanel)
                            .jigglePressClickable {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    undoStack.add(activeRecipe)
                                    activeRecipe = next
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Horizontally Scrollable Action Pills (Before/After Split, Scope, Zebra, Copy/Paste Recipe)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppPillChip(
                        label = if (splitCurtainEnabled) "Split On" else "Split",
                        leadingIcon = Icons.Outlined.Compare,
                        isSelected = splitCurtainEnabled,
                        onClick = { splitCurtainEnabled = !splitCurtainEnabled }
                    )
                    AppPillChip(
                        label = scopeMode.label,
                        isSelected = scopeMode != ScopeMode.OFF,
                        onClick = {
                            val nextOrd = (scopeMode.ordinal + 1) % ScopeMode.entries.size
                            scopeMode = ScopeMode.entries[nextOrd]
                        }
                    )
                    AppPillChip(
                        label = "Zebra",
                        isSelected = zebraClippingEnabled,
                        onClick = { zebraClippingEnabled = !zebraClippingEnabled }
                    )
                    AppPillChip(
                        label = "Copy",
                        leadingIcon = Icons.Outlined.ContentCopy,
                        isSelected = false,
                        onClick = { onCopyRecipe(activeRecipe) }
                    )
                    if (copiedRecipe != null) {
                        AppPillChip(
                            label = "Paste",
                            leadingIcon = Icons.Outlined.ContentPaste,
                            isSelected = true,
                            onClick = { updateRecipe(copiedRecipe, pushUndo = true) }
                        )
                    }
                    if (activeRecipe.activeParameterCount() > 0) {
                        AppPillChip(
                            label = "Reset All (${activeRecipe.activeParameterCount()})",
                            leadingIcon = Icons.Outlined.Refresh,
                            isSelected = false,
                            onClick = { updateRecipe(PhotoEditRecipe(), pushUndo = true) }
                        )
                    }
                }

                AppButton(
                    text = if (isSaving) "Saving..." else "Save",
                    leadingIcon = Icons.Outlined.Save,
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            coroutineScope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    PhotoEditorEngine.exportEditedPhoto(
                                        context = context,
                                        sourceItem = targetItem,
                                        recipe = activeRecipe,
                                        exportFormat = exportFormat,
                                        preserveExif = preserveExif
                                    )
                                }
                                isSaving = false
                                if (res.success && res.exportedItem != null) {
                                    onExportedPhoto(res.exportedItem, res.message)
                                }
                            }
                        }
                    },
                    variant = AppButtonVariant.PRIMARY,
                    height = 36.dp,
                    horizontalPadding = 12.dp,
                    fontSize = 12.sp
                )
            }

            // Live Compact RGB Histogram / RGB Parade & Clipping Readout Pill
            val hist = liveHistogram
            if (scopeMode != ScopeMode.OFF && hist != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElectricSlateTokens.SurfacePanel.copy(alpha = 0.90f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val histPath = remember { Path() }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val bins = hist.lumaBins.size.coerceAtLeast(2)

                        if (scopeMode == ScopeMode.HISTOGRAM_RGB) {
                            histPath.reset()
                            for (i in 0 until bins) {
                                val x = (i.toFloat() / (bins - 1)) * w
                                val y = h * (1f - hist.lumaBins[i].coerceIn(0f, 1f))
                                if (i == 0) histPath.moveTo(x, y) else histPath.lineTo(x, y)
                            }
                            drawPath(
                                path = histPath,
                                color = ElectricSlateTokens.PrimaryAccent,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        } else {
                            // RGB Parade (3 side-by-side waveforms: Red, Green, Blue)
                            val thirdW = w / 3f
                            val channels = listOf(hist.redBins, hist.greenBins, hist.blueBins)
                            channels.forEachIndexed { cIdx, arr ->
                                val xStart = cIdx * thirdW
                                for (i in 0 until bins step 2) {
                                    val x = xStart + (i.toFloat() / bins) * (thirdW - 4.dp.toPx())
                                    val barH = (arr[i] * h).coerceAtLeast(1.5.dp.toPx())
                                    drawLine(
                                        color = if (cIdx == 0) ElectricSlateTokens.Destructive else if (cIdx == 1) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.PrimaryAccent,
                                        start = Offset(x, h),
                                        end = Offset(x, h - barH),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    if (zebraClippingEnabled || hist.highlightClipPercent > 0.5f || hist.shadowClipPercent > 0.5f) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Hi Clip: ${String.format("%.1f%%", hist.highlightClipPercent)}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = if (hist.highlightClipPercent > 1.5f) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted
                            )
                            Text(
                                text = "Lo Clip: ${String.format("%.1f%%", hist.shadowClipPercent)}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = if (hist.shadowClipPercent > 1.5f) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Bottom Floating 10-Module / 70+ Parameter Editor Deck
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = 12.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Module Selector Row (10 Modules)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EditorModule.entries.forEach { mod ->
                            AppPillChip(
                                label = mod.label,
                                isSelected = selectedModule == mod,
                                onClick = {
                                    selectedModule = mod
                                    eyedropperActive = false
                                    redactTapModeActive = false
                                }
                            )
                        }
                    }

                    // Active Module Parameter Controls
                    when (selectedModule) {
                        EditorModule.TRANSFORM -> {
                            // Aspect Presets + Quick Rotate/Flip Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppPillChip(
                                    label = "Rotate 90°",
                                    leadingIcon = Icons.Outlined.RotateRight,
                                    isSelected = activeRecipe.rotateQuarterTurns != 0,
                                    onClick = {
                                        updateRecipe(
                                            activeRecipe.copy(rotateQuarterTurns = (activeRecipe.rotateQuarterTurns + 1) % 4),
                                            pushUndo = true
                                        )
                                    }
                                )
                                AppPillChip(
                                    label = "Flip H",
                                    leadingIcon = Icons.Outlined.Flip,
                                    isSelected = activeRecipe.flipHorizontal,
                                    onClick = {
                                        updateRecipe(activeRecipe.copy(flipHorizontal = !activeRecipe.flipHorizontal), pushUndo = true)
                                    }
                                )
                                AppPillChip(
                                    label = "Flip V",
                                    isSelected = activeRecipe.flipVertical,
                                    onClick = {
                                        updateRecipe(activeRecipe.copy(flipVertical = !activeRecipe.flipVertical), pushUndo = true)
                                    }
                                )
                                EditorAspectPreset.entries.forEach { preset ->
                                    AppPillChip(
                                        label = preset.label,
                                        isSelected = activeRecipe.aspectPreset == preset,
                                        onClick = {
                                            updateRecipe(activeRecipe.copy(aspectPreset = preset), pushUndo = true)
                                        }
                                    )
                                }
                            }

                            val transformLabels = listOf(
                                "Straighten ${String.format("%+.1f°", activeRecipe.straightenDegrees)}",
                                "Keystone V ${activeRecipe.keystoneVertical.roundToInt()}",
                                "Keystone H ${activeRecipe.keystoneHorizontal.roundToInt()}",
                                "Anamorphic ${String.format("%.2fx", activeRecipe.anamorphicScaleX)}",
                                "Lens Distort ${activeRecipe.lensDistortion.roundToInt()}",
                                "Crop Left ${(activeRecipe.cropLeft * 100).roundToInt()}%",
                                "Crop Top ${(activeRecipe.cropTop * 100).roundToInt()}%",
                                "Crop Right ${(activeRecipe.cropRight * 100).roundToInt()}%",
                                "Crop Bottom ${(activeRecipe.cropBottom * 100).roundToInt()}%"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                transformLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedTransformParamIdx == idx,
                                        onClick = { selectedTransformParamIdx = idx }
                                    )
                                }
                            }

                            val (curVal, range) = when (selectedTransformParamIdx) {
                                0 -> activeRecipe.straightenDegrees to (-45f..45f)
                                1 -> activeRecipe.keystoneVertical to (-100f..100f)
                                2 -> activeRecipe.keystoneHorizontal to (-100f..100f)
                                3 -> activeRecipe.anamorphicScaleX to (0.75f..1.50f)
                                4 -> activeRecipe.lensDistortion to (-100f..100f)
                                5 -> activeRecipe.cropLeft to (0f..0.80f)
                                6 -> activeRecipe.cropTop to (0f..0.80f)
                                7 -> activeRecipe.cropRight to (0.20f..1f)
                                else -> activeRecipe.cropBottom to (0.20f..1f)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppSlider(
                                    value = curVal,
                                    onValueChange = { v ->
                                        val updated = when (selectedTransformParamIdx) {
                                            0 -> activeRecipe.copy(straightenDegrees = v)
                                            1 -> activeRecipe.copy(keystoneVertical = v)
                                            2 -> activeRecipe.copy(keystoneHorizontal = v)
                                            3 -> activeRecipe.copy(anamorphicScaleX = v)
                                            4 -> activeRecipe.copy(lensDistortion = v)
                                            5 -> activeRecipe.copy(cropLeft = v.coerceAtMost(activeRecipe.cropRight - 0.12f))
                                            6 -> activeRecipe.copy(cropTop = v.coerceAtMost(activeRecipe.cropBottom - 0.12f))
                                            7 -> activeRecipe.copy(cropRight = v.coerceAtLeast(activeRecipe.cropLeft + 0.12f))
                                            else -> activeRecipe.copy(cropBottom = v.coerceAtLeast(activeRecipe.cropTop + 0.12f))
                                        }
                                        updateRecipe(updated)
                                    },
                                    valueRange = range,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Reset",
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.TRANSFORM), pushUndo = true) },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        EditorModule.LIGHT -> {
                            val lightLabels = listOf(
                                "Exposure ${String.format("%+.2f EV", activeRecipe.exposureEv)}",
                                "Brilliance ${activeRecipe.brilliance.roundToInt()}",
                                "Contrast ${activeRecipe.contrast.roundToInt()}",
                                "Pivot ${String.format("%.2f", activeRecipe.contrastPivot)}",
                                "Highlights ${activeRecipe.highlights.roundToInt()}",
                                "Midtones ${activeRecipe.midtones.roundToInt()}",
                                "Shadows ${activeRecipe.shadows.roundToInt()}",
                                "Whites ${activeRecipe.whites.roundToInt()}",
                                "Blacks ${activeRecipe.blacks.roundToInt()}",
                                "Dehaze ${activeRecipe.dehaze.roundToInt()}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                lightLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedLightParamIdx == idx,
                                        onClick = { selectedLightParamIdx = idx }
                                    )
                                }
                            }

                            val (curVal, range) = when (selectedLightParamIdx) {
                                0 -> activeRecipe.exposureEv to (-4f..4f)
                                1 -> activeRecipe.brilliance to (-100f..100f)
                                2 -> activeRecipe.contrast to (-100f..100f)
                                3 -> activeRecipe.contrastPivot to (0.15f..0.85f)
                                4 -> activeRecipe.highlights to (-100f..100f)
                                5 -> activeRecipe.midtones to (-100f..100f)
                                6 -> activeRecipe.shadows to (-100f..100f)
                                7 -> activeRecipe.whites to (-100f..100f)
                                8 -> activeRecipe.blacks to (-100f..100f)
                                else -> activeRecipe.dehaze to (-100f..100f)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppSlider(
                                    value = curVal,
                                    onValueChange = { v ->
                                        val updated = when (selectedLightParamIdx) {
                                            0 -> activeRecipe.copy(exposureEv = v)
                                            1 -> activeRecipe.copy(brilliance = v)
                                            2 -> activeRecipe.copy(contrast = v)
                                            3 -> activeRecipe.copy(contrastPivot = v)
                                            4 -> activeRecipe.copy(highlights = v)
                                            5 -> activeRecipe.copy(midtones = v)
                                            6 -> activeRecipe.copy(shadows = v)
                                            7 -> activeRecipe.copy(whites = v)
                                            8 -> activeRecipe.copy(blacks = v)
                                            else -> activeRecipe.copy(dehaze = v)
                                        }
                                        updateRecipe(updated)
                                    },
                                    valueRange = range,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Auto",
                                    leadingIcon = Icons.Outlined.AutoFixHigh,
                                    onClick = {
                                        updateRecipe(
                                            activeRecipe.copy(
                                                exposureEv = 0.18f,
                                                brilliance = 24f,
                                                contrast = 14f,
                                                highlights = -18f,
                                                shadows = 22f,
                                                whites = 8f,
                                                blacks = -6f
                                            ),
                                            pushUndo = true
                                        )
                                    },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                                AppButton(
                                    text = "Reset",
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.LIGHT), pushUndo = true) },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        EditorModule.CURVES -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurveChannel.entries.forEach { ch ->
                                    AppPillChip(
                                        label = ch.label,
                                        isSelected = selectedCurveChannel == ch,
                                        onClick = { selectedCurveChannel = ch }
                                    )
                                }
                                AppPillChip(
                                    label = "S-Curve",
                                    isSelected = false,
                                    onClick = {
                                        updateRecipe(
                                            activeRecipe.copy(lumaCurveNodes = listOf(0.0f, 0.19f, 0.50f, 0.81f, 1.0f)),
                                            pushUndo = true
                                        )
                                    }
                                )
                                AppPillChip(
                                    label = "Matte Lift",
                                    isSelected = false,
                                    onClick = {
                                        updateRecipe(
                                            activeRecipe.copy(lumaCurveNodes = listOf(0.08f, 0.26f, 0.52f, 0.78f, 0.96f)),
                                            pushUndo = true
                                        )
                                    }
                                )
                                AppPillChip(
                                    label = "Reset Curve",
                                    isSelected = false,
                                    onClick = {
                                        updateRecipe(activeRecipe.resetModule(EditorModule.CURVES), pushUndo = true)
                                    }
                                )
                            }

                            val activeNodes = when (selectedCurveChannel) {
                                CurveChannel.MASTER_LUMA -> activeRecipe.lumaCurveNodes
                                CurveChannel.RED -> activeRecipe.redCurveNodes
                                CurveChannel.GREEN -> activeRecipe.greenCurveNodes
                                CurveChannel.BLUE -> activeRecipe.blueCurveNodes
                            }
                            val currentChannel by rememberUpdatedState(selectedCurveChannel)

                            fun applyCurveTouch(tx: Float, ty: Float, cw: Float, ch: Float) {
                                val xFrac = (tx / cw.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                val yVal = (1f - (ty / ch.coerceAtLeast(1f))).coerceIn(0f, 1f)
                                val nodeIdx = (xFrac * 4f).roundToInt().coerceIn(0, 4)
                                val rec = currentRecipe
                                val oldList = when (currentChannel) {
                                    CurveChannel.MASTER_LUMA -> rec.lumaCurveNodes
                                    CurveChannel.RED -> rec.redCurveNodes
                                    CurveChannel.GREEN -> rec.greenCurveNodes
                                    CurveChannel.BLUE -> rec.blueCurveNodes
                                }.toMutableList()
                                oldList[nodeIdx] = yVal
                                val newRec = when (currentChannel) {
                                    CurveChannel.MASTER_LUMA -> rec.copy(lumaCurveNodes = oldList)
                                    CurveChannel.RED -> rec.copy(redCurveNodes = oldList)
                                    CurveChannel.GREEN -> rec.copy(greenCurveNodes = oldList)
                                    CurveChannel.BLUE -> rec.copy(blueCurveNodes = oldList)
                                }
                                updateRecipe(newRec)
                            }

                            val splinePath = remember { Path() }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.BgCanvas)
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            applyCurveTouch(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            applyCurveTouch(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                drawLine(
                                    color = ElectricSlateTokens.BorderSubtle,
                                    start = Offset(0f, h),
                                    end = Offset(w, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                splinePath.reset()
                                val steps = 48
                                for (i in 0..steps) {
                                    val xf = i.toFloat() / steps
                                    val yf = PhotoEditorEngine.evaluateMonotoneCubicCurve(activeNodes, xf)
                                    val px = xf * w
                                    val py = (1f - yf) * h
                                    if (i == 0) splinePath.moveTo(px, py) else splinePath.lineTo(px, py)
                                }
                                drawPath(
                                    path = splinePath,
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    style = Stroke(width = 2.2.dp.toPx())
                                )
                                for (idx in 0..4) {
                                    val nx = (idx / 4f) * w
                                    val ny = (1f - activeNodes[idx].coerceIn(0f, 1f)) * h
                                    drawCircle(
                                        color = ElectricSlateTokens.TextPrimary,
                                        radius = 4.5.dp.toPx(),
                                        center = Offset(nx, ny)
                                    )
                                }
                            }

                            // 4 Parametric Zone Sliders
                            val zoneLabels = listOf(
                                "Shadows ${activeRecipe.parametricShadows.roundToInt()}",
                                "Darks ${activeRecipe.parametricDarks.roundToInt()}",
                                "Lights ${activeRecipe.parametricLights.roundToInt()}",
                                "Highlights ${activeRecipe.parametricHighlights.roundToInt()}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                zoneLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedParametricZoneIdx == idx,
                                        onClick = { selectedParametricZoneIdx = idx }
                                    )
                                }
                            }
                            val zoneVal = when (selectedParametricZoneIdx) {
                                0 -> activeRecipe.parametricShadows
                                1 -> activeRecipe.parametricDarks
                                2 -> activeRecipe.parametricLights
                                else -> activeRecipe.parametricHighlights
                            }
                            AppSlider(
                                value = zoneVal,
                                onValueChange = { v ->
                                    val updated = when (selectedParametricZoneIdx) {
                                        0 -> activeRecipe.copy(parametricShadows = v)
                                        1 -> activeRecipe.copy(parametricDarks = v)
                                        2 -> activeRecipe.copy(parametricLights = v)
                                        else -> activeRecipe.copy(parametricHighlights = v)
                                    }
                                    updateRecipe(updated)
                                },
                                valueRange = -100f..100f,
                                steps = 0
                            )
                        }

                        EditorModule.HSL_8BAND -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HslColorBand.entries.forEach { band ->
                                    val adj = activeRecipe.hslBands[band] ?: HslBandAdjustment()
                                    val modified = abs(adj.hueShift) > 0.5f || abs(adj.saturation) > 0.5f || abs(adj.luminance) > 0.5f
                                    AppPillChip(
                                        label = if (modified) "${band.label} •" else band.label,
                                        isSelected = selectedHslBand == band,
                                        onClick = { selectedHslBand = band }
                                    )
                                }
                                AppPillChip(
                                    label = "Reset HSL",
                                    isSelected = false,
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.HSL_8BAND), pushUndo = true) }
                                )
                            }

                            val activeBandAdj = activeRecipe.hslBands[selectedHslBand] ?: HslBandAdjustment()

                            fun updateBand(newAdj: HslBandAdjustment) {
                                val nextMap = activeRecipe.hslBands.toMutableMap()
                                nextMap[selectedHslBand] = newAdj
                                updateRecipe(activeRecipe.copy(hslBands = nextMap))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Hue ${activeBandAdj.hueShift.roundToInt()}",
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextSecondary
                                )
                                AppSlider(
                                    value = activeBandAdj.hueShift,
                                    onValueChange = { updateBand(activeBandAdj.copy(hueShift = it)) },
                                    valueRange = -100f..100f,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sat ${activeBandAdj.saturation.roundToInt()}",
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextSecondary
                                )
                                AppSlider(
                                    value = activeBandAdj.saturation,
                                    onValueChange = { updateBand(activeBandAdj.copy(saturation = it)) },
                                    valueRange = -100f..100f,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Lum ${activeBandAdj.luminance.roundToInt()}",
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextSecondary
                                )
                                AppSlider(
                                    value = activeBandAdj.luminance,
                                    onValueChange = { updateBand(activeBandAdj.copy(luminance = it)) },
                                    valueRange = -100f..100f,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        EditorModule.COLOR_GRADE_3WAY -> {
                            val wheelTabs = listOf("Shadows Wheel", "Midtones Wheel", "Highlights Wheel", "Blend & Balance")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                wheelTabs.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedGradeWheelIdx == idx,
                                        onClick = { selectedGradeWheelIdx = idx }
                                    )
                                }
                                AppPillChip(
                                    label = "Reset 3-Way",
                                    isSelected = false,
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.COLOR_GRADE_3WAY), pushUndo = true) }
                                )
                            }

                            if (selectedGradeWheelIdx < 3) {
                                val (hue, sat, lum) = when (selectedGradeWheelIdx) {
                                    0 -> Triple(activeRecipe.shadowsHue, activeRecipe.shadowsSat, activeRecipe.shadowsLuma)
                                    1 -> Triple(activeRecipe.midtonesHue, activeRecipe.midtonesSat, activeRecipe.midtonesLuma)
                                    else -> Triple(activeRecipe.highlightsHue, activeRecipe.highlightsSat, activeRecipe.highlightsLuma)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Hue ${hue.roundToInt()}°", fontFamily = ElmsSans, fontSize = 11.sp, color = ElectricSlateTokens.TextSecondary)
                                    AppSlider(
                                        value = hue,
                                        onValueChange = { v ->
                                            val updated = when (selectedGradeWheelIdx) {
                                                0 -> activeRecipe.copy(shadowsHue = v)
                                                1 -> activeRecipe.copy(midtonesHue = v)
                                                else -> activeRecipe.copy(highlightsHue = v)
                                            }
                                            updateRecipe(updated)
                                        },
                                        valueRange = 0f..360f,
                                        steps = 0,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Sat ${sat.roundToInt()}", fontFamily = ElmsSans, fontSize = 11.sp, color = ElectricSlateTokens.TextSecondary)
                                    AppSlider(
                                        value = sat,
                                        onValueChange = { v ->
                                            val updated = when (selectedGradeWheelIdx) {
                                                0 -> activeRecipe.copy(shadowsSat = v)
                                                1 -> activeRecipe.copy(midtonesSat = v)
                                                else -> activeRecipe.copy(highlightsSat = v)
                                            }
                                            updateRecipe(updated)
                                        },
                                        valueRange = 0f..100f,
                                        steps = 0,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Luma ${lum.roundToInt()}", fontFamily = ElmsSans, fontSize = 11.sp, color = ElectricSlateTokens.TextSecondary)
                                    AppSlider(
                                        value = lum,
                                        onValueChange = { v ->
                                            val updated = when (selectedGradeWheelIdx) {
                                                0 -> activeRecipe.copy(shadowsLuma = v)
                                                1 -> activeRecipe.copy(midtonesLuma = v)
                                                else -> activeRecipe.copy(highlightsLuma = v)
                                            }
                                            updateRecipe(updated)
                                        },
                                        valueRange = -100f..100f,
                                        steps = 0,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Blend ${activeRecipe.gradeBlending.roundToInt()}", fontFamily = ElmsSans, fontSize = 11.sp, color = ElectricSlateTokens.TextSecondary)
                                    AppSlider(
                                        value = activeRecipe.gradeBlending,
                                        onValueChange = { updateRecipe(activeRecipe.copy(gradeBlending = it)) },
                                        valueRange = 0f..100f,
                                        steps = 0,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Balance ${activeRecipe.gradeBalance.roundToInt()}", fontFamily = ElmsSans, fontSize = 11.sp, color = ElectricSlateTokens.TextSecondary)
                                    AppSlider(
                                        value = activeRecipe.gradeBalance,
                                        onValueChange = { updateRecipe(activeRecipe.copy(gradeBalance = it)) },
                                        valueRange = -100f..100f,
                                        steps = 0,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        EditorModule.CALIBRATION -> {
                            val calLabels = listOf(
                                "Temp ${activeRecipe.temperature.roundToInt()}",
                                "Tint ${activeRecipe.tint.roundToInt()}",
                                "Vibrance ${activeRecipe.vibrance.roundToInt()}",
                                "Saturation ${activeRecipe.saturation.roundToInt()}",
                                "Shadow Tint ${activeRecipe.shadowTint.roundToInt()}",
                                "Red Hue ${activeRecipe.redPrimaryHue.roundToInt()}",
                                "Red Sat ${activeRecipe.redPrimarySat.roundToInt()}",
                                "Green Hue ${activeRecipe.greenPrimaryHue.roundToInt()}",
                                "Green Sat ${activeRecipe.greenPrimarySat.roundToInt()}",
                                "Blue Hue ${activeRecipe.bluePrimaryHue.roundToInt()}",
                                "Blue Sat ${activeRecipe.bluePrimarySat.roundToInt()}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppPillChip(
                                    label = if (eyedropperActive) "Tap Neutral Gray..." else "WB Eyedropper",
                                    leadingIcon = Icons.Outlined.Colorize,
                                    isSelected = eyedropperActive,
                                    onClick = { eyedropperActive = !eyedropperActive }
                                )
                                calLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedCalibrationParamIdx == idx,
                                        onClick = { selectedCalibrationParamIdx = idx }
                                    )
                                }
                            }
                            val curVal = when (selectedCalibrationParamIdx) {
                                0 -> activeRecipe.temperature
                                1 -> activeRecipe.tint
                                2 -> activeRecipe.vibrance
                                3 -> activeRecipe.saturation
                                4 -> activeRecipe.shadowTint
                                5 -> activeRecipe.redPrimaryHue
                                6 -> activeRecipe.redPrimarySat
                                7 -> activeRecipe.greenPrimaryHue
                                8 -> activeRecipe.greenPrimarySat
                                9 -> activeRecipe.bluePrimaryHue
                                else -> activeRecipe.bluePrimarySat
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppSlider(
                                    value = curVal,
                                    onValueChange = { v ->
                                        val updated = when (selectedCalibrationParamIdx) {
                                            0 -> activeRecipe.copy(temperature = v)
                                            1 -> activeRecipe.copy(tint = v)
                                            2 -> activeRecipe.copy(vibrance = v)
                                            3 -> activeRecipe.copy(saturation = v)
                                            4 -> activeRecipe.copy(shadowTint = v)
                                            5 -> activeRecipe.copy(redPrimaryHue = v)
                                            6 -> activeRecipe.copy(redPrimarySat = v)
                                            7 -> activeRecipe.copy(greenPrimaryHue = v)
                                            8 -> activeRecipe.copy(greenPrimarySat = v)
                                            9 -> activeRecipe.copy(bluePrimaryHue = v)
                                            else -> activeRecipe.copy(bluePrimarySat = v)
                                        }
                                        updateRecipe(updated)
                                    },
                                    valueRange = -100f..100f,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Reset",
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.CALIBRATION), pushUndo = true) },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        EditorModule.DETAIL -> {
                            val detailLabels = listOf(
                                "Texture ${activeRecipe.texture.roundToInt()}",
                                "Clarity ${activeRecipe.clarity.roundToInt()}",
                                "Sharpen ${activeRecipe.sharpenAmount.roundToInt()}",
                                "Radius ${String.format("%.1fpx", activeRecipe.sharpenRadius)}",
                                "Edge Mask ${activeRecipe.sharpenMasking.roundToInt()}",
                                "Luma Denoise ${activeRecipe.lumaDenoise.roundToInt()}",
                                "Chroma Denoise ${activeRecipe.chromaDenoise.roundToInt()}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detailLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedDetailParamIdx == idx,
                                        onClick = { selectedDetailParamIdx = idx }
                                    )
                                }
                            }
                            val (curVal, range) = when (selectedDetailParamIdx) {
                                0 -> activeRecipe.texture to (-100f..100f)
                                1 -> activeRecipe.clarity to (-100f..100f)
                                2 -> activeRecipe.sharpenAmount to (0f..150f)
                                3 -> activeRecipe.sharpenRadius to (0.5f..3.0f)
                                4 -> activeRecipe.sharpenMasking to (0f..100f)
                                5 -> activeRecipe.lumaDenoise to (0f..100f)
                                else -> activeRecipe.chromaDenoise to (0f..100f)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppSlider(
                                    value = curVal,
                                    onValueChange = { v ->
                                        val updated = when (selectedDetailParamIdx) {
                                            0 -> activeRecipe.copy(texture = v)
                                            1 -> activeRecipe.copy(clarity = v)
                                            2 -> activeRecipe.copy(sharpenAmount = v)
                                            3 -> activeRecipe.copy(sharpenRadius = v)
                                            4 -> activeRecipe.copy(sharpenMasking = v)
                                            5 -> activeRecipe.copy(lumaDenoise = v)
                                            else -> activeRecipe.copy(chromaDenoise = v)
                                        }
                                        updateRecipe(updated)
                                    },
                                    valueRange = range,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Reset",
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.DETAIL), pushUndo = true) },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        EditorModule.OPTICS_FX -> {
                            val opticsLabels = listOf(
                                "Vignette ${activeRecipe.vignetteAmount.roundToInt()}",
                                "Vig Midpoint ${activeRecipe.vignetteMidpoint.roundToInt()}",
                                "Vig Roundness ${activeRecipe.vignetteRoundness.roundToInt()}",
                                "Vig Feather ${activeRecipe.vignetteFeather.roundToInt()}",
                                "Grain ${activeRecipe.grainAmount.roundToInt()}",
                                "Grain Size ${activeRecipe.grainSize.roundToInt()}",
                                "Grain Rough ${activeRecipe.grainRoughness.roundToInt()}",
                                "Halation Bloom ${activeRecipe.halationBloom.roundToInt()}",
                                "Defringe ${activeRecipe.chromaticDefringe.roundToInt()}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                opticsLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedOpticsParamIdx == idx,
                                        onClick = { selectedOpticsParamIdx = idx }
                                    )
                                }
                            }
                            val (curVal, range) = when (selectedOpticsParamIdx) {
                                0 -> activeRecipe.vignetteAmount to (-100f..100f)
                                1 -> activeRecipe.vignetteMidpoint to (0f..100f)
                                2 -> activeRecipe.vignetteRoundness to (-100f..100f)
                                3 -> activeRecipe.vignetteFeather to (0f..100f)
                                4 -> activeRecipe.grainAmount to (0f..100f)
                                5 -> activeRecipe.grainSize to (1f..100f)
                                6 -> activeRecipe.grainRoughness to (0f..100f)
                                7 -> activeRecipe.halationBloom to (0f..100f)
                                else -> activeRecipe.chromaticDefringe to (-100f..100f)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppSlider(
                                    value = curVal,
                                    onValueChange = { v ->
                                        val updated = when (selectedOpticsParamIdx) {
                                            0 -> activeRecipe.copy(vignetteAmount = v)
                                            1 -> activeRecipe.copy(vignetteMidpoint = v)
                                            2 -> activeRecipe.copy(vignetteRoundness = v)
                                            3 -> activeRecipe.copy(vignetteFeather = v)
                                            4 -> activeRecipe.copy(grainAmount = v)
                                            5 -> activeRecipe.copy(grainSize = v)
                                            6 -> activeRecipe.copy(grainRoughness = v)
                                            7 -> activeRecipe.copy(halationBloom = v)
                                            else -> activeRecipe.copy(chromaticDefringe = v)
                                        }
                                        updateRecipe(updated)
                                    },
                                    valueRange = range,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Reset",
                                    onClick = { updateRecipe(activeRecipe.resetModule(EditorModule.OPTICS_FX), pushUndo = true) },
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 32.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        EditorModule.MASK_REDACT -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SelectiveMaskType.entries.forEach { mType ->
                                    AppPillChip(
                                        label = "Mask: ${mType.label}",
                                        isSelected = activeRecipe.maskType == mType,
                                        onClick = {
                                            redactTapModeActive = false
                                            updateRecipe(activeRecipe.copy(maskType = mType), pushUndo = true)
                                        }
                                    )
                                }
                                if (activeRecipe.maskType != SelectiveMaskType.NONE) {
                                    AppPillChip(
                                        label = "Invert Mask",
                                        isSelected = activeRecipe.maskInvert,
                                        onClick = { updateRecipe(activeRecipe.copy(maskInvert = !activeRecipe.maskInvert), pushUndo = true) }
                                    )
                                }
                                AppPillChip(
                                    label = if (redactTapModeActive) "Tap Photo to Redact" else "Privacy Redact",
                                    leadingIcon = Icons.Outlined.BlurOn,
                                    isSelected = redactTapModeActive,
                                    onClick = { redactTapModeActive = !redactTapModeActive }
                                )
                                if (activeRecipe.redactionRegions.isNotEmpty()) {
                                    AppPillChip(
                                        label = "Clear Redact (${activeRecipe.redactionRegions.size})",
                                        isSelected = false,
                                        onClick = { updateRecipe(activeRecipe.copy(redactionRegions = emptyList()), pushUndo = true) }
                                    )
                                }
                            }

                            val maskLabels = listOf(
                                "Mask Exp ${String.format("%+.1f EV", activeRecipe.maskExposureEv)}",
                                "Mask Contrast ${activeRecipe.maskContrast.roundToInt()}",
                                "Mask Sat ${activeRecipe.maskSaturation.roundToInt()}",
                                "Mask Temp ${activeRecipe.maskTemperature.roundToInt()}",
                                "Mask Radius ${(activeRecipe.maskRadius * 100).roundToInt()}%",
                                "Mask Feather ${activeRecipe.maskFeather.roundToInt()}",
                                "Redact Size ${(redactBrushRadius * 100).roundToInt()}%",
                                "Redact Strength ${redactBrushStrength.roundToInt()}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                maskLabels.forEachIndexed { idx, lbl ->
                                    AppPillChip(
                                        label = lbl,
                                        isSelected = selectedMaskParamIdx == idx,
                                        onClick = { selectedMaskParamIdx = idx }
                                    )
                                }
                            }
                            val (curVal, range) = when (selectedMaskParamIdx) {
                                0 -> activeRecipe.maskExposureEv to (-2f..2f)
                                1 -> activeRecipe.maskContrast to (-100f..100f)
                                2 -> activeRecipe.maskSaturation to (-100f..100f)
                                3 -> activeRecipe.maskTemperature to (-100f..100f)
                                4 -> activeRecipe.maskRadius to (0.08f..0.90f)
                                5 -> activeRecipe.maskFeather to (5f..100f)
                                6 -> redactBrushRadius to (0.03f..0.25f)
                                else -> redactBrushStrength to (10f..100f)
                            }
                            AppSlider(
                                value = curVal,
                                onValueChange = { v ->
                                    when (selectedMaskParamIdx) {
                                        0 -> updateRecipe(activeRecipe.copy(maskExposureEv = v))
                                        1 -> updateRecipe(activeRecipe.copy(maskContrast = v))
                                        2 -> updateRecipe(activeRecipe.copy(maskSaturation = v))
                                        3 -> updateRecipe(activeRecipe.copy(maskTemperature = v))
                                        4 -> updateRecipe(activeRecipe.copy(maskRadius = v))
                                        5 -> updateRecipe(activeRecipe.copy(maskFeather = v))
                                        6 -> redactBrushRadius = v
                                        else -> redactBrushStrength = v
                                    }
                                },
                                valueRange = range,
                                steps = 0
                            )
                        }

                        EditorModule.FILM_EXPORT -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilmStockPreset.entries.forEach { stock ->
                                    AppPillChip(
                                        label = stock.label,
                                        isSelected = activeRecipe.filmStock == stock,
                                        onClick = { updateRecipe(activeRecipe.copy(filmStock = stock), pushUndo = true) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Film ${activeRecipe.filmIntensity.roundToInt()}%",
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextSecondary
                                )
                                AppSlider(
                                    value = activeRecipe.filmIntensity,
                                    onValueChange = { updateRecipe(activeRecipe.copy(filmIntensity = it)) },
                                    valueRange = 0f..100f,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhotoExportFormat.entries.forEach { fmt ->
                                    AppPillChip(
                                        label = fmt.label,
                                        isSelected = exportFormat == fmt,
                                        onClick = { exportFormat = fmt }
                                    )
                                }
                                AppPillChip(
                                    label = if (preserveExif) "Keep EXIF" else "Scrub EXIF",
                                    isSelected = preserveExif,
                                    onClick = { preserveExif = !preserveExif }
                                )
                                AppPillChip(
                                    label = if (activeRecipe.posterizeLevels < 64) "Posterize ${activeRecipe.posterizeLevels}" else "Posterize Off",
                                    isSelected = activeRecipe.posterizeLevels < 64,
                                    onClick = {
                                        val next = if (activeRecipe.posterizeLevels >= 64) 6 else if (activeRecipe.posterizeLevels == 6) 12 else 64
                                        updateRecipe(activeRecipe.copy(posterizeLevels = next), pushUndo = true)
                                    }
                                )
                                AppPillChip(
                                    label = if (activeRecipe.solarizeThreshold > 0f) "Solarize On" else "Solarize Off",
                                    isSelected = activeRecipe.solarizeThreshold > 0f,
                                    onClick = {
                                        val next = if (activeRecipe.solarizeThreshold > 0f) 0f else 48f
                                        updateRecipe(activeRecipe.copy(solarizeThreshold = next), pushUndo = true)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
