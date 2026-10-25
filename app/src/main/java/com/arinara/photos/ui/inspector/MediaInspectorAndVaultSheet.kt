// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.inspector

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.media.inspector.DeepMediaInspectionReport
import com.arinara.photos.media.inspector.DuplicateGroup
import com.arinara.photos.theme.ElmsSans
import com.arinara.photos.ui.components.presets.AppBottomSheet
import com.arinara.photos.ui.components.presets.AppButton
import com.arinara.photos.ui.components.presets.AppButtonVariant
import com.arinara.photos.ui.components.presets.AppCard
import com.arinara.photos.ui.components.presets.AppPillChip
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.jigglePressClickable

enum class InspectorTab(val label: String) {
    HISTOGRAM_EXIF("RGB Histogram & EXIF"),
    PRIVATE_VAULT("PIN Private Vault"),
    TRASH_DUPLICATES("Trash & Duplicates")
}

@Composable
fun MediaInspectorAndVaultSheet(
    visible: Boolean,
    initialTab: InspectorTab = InspectorTab.HISTOGRAM_EXIF,
    targetItem: MediaItem?,
    inspectionReport: DeepMediaInspectionReport?,
    vaultUnlocked: Boolean,
    vaultItems: List<MediaItem>,
    trashedItems: List<MediaItem>,
    duplicateGroups: List<DuplicateGroup>,
    onDismiss: () -> Unit,
    onScrubExifPrivacy: (MediaItem) -> Unit,
    onUnlockVaultPin: (String) -> Boolean,
    onLockVault: () -> Unit,
    onToggleVaultItem: (MediaItem, Boolean) -> Unit,
    onMoveToTrash: (MediaItem) -> Unit,
    onRestoreFromTrash: (MediaItem) -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var pinInput by remember { mutableStateOf("") }
    var pinStatusMessage by remember { mutableStateOf("Enter Vault PIN (Default: 2026 | Decoy: 0000)") }
    var copiedHexNotice by remember { mutableStateOf<String?>(null) }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Deep EXIF, RGB Histogram & Private Vault"
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
                InspectorTab.entries.forEach { tab ->
                    AppPillChip(
                        label = tab.label,
                        isSelected = activeTab == tab,
                        onClick = { activeTab = tab }
                    )
                }
            }

            when (activeTab) {
                InspectorTab.HISTOGRAM_EXIF -> {
                    if (targetItem == null || inspectionReport == null) {
                        Text(
                            text = "Select any media item to inspect its 256-bin RGB histogram, EXIF metadata, and SHA-256 hash.",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    } else {
                        // Live 256-Bin RGB + Luminance Histogram Card (F-G08)
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
                                    text = "Live 256-Bin RGB + Luma Histogram",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "DR ${String.format("%.1f EV", inspectionReport.histogram.dynamicRangeEv)}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            val rPath = remember { Path() }
                            val gPath = remember { Path() }
                            val yPath = remember { Path() }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.BgCanvas)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val bins = inspectionReport.histogram.lumaBins.size.coerceAtLeast(2)
                                val stepX = w / (bins - 1)

                                rPath.reset()
                                gPath.reset()
                                yPath.reset()

                                for (i in 0 until bins) {
                                    val x = i * stepX
                                    val ry = h * (1f - inspectionReport.histogram.redBins[i].coerceIn(0f, 1f))
                                    val gy = h * (1f - inspectionReport.histogram.greenBins[i].coerceIn(0f, 1f))
                                    val ly = h * (1f - inspectionReport.histogram.lumaBins[i].coerceIn(0f, 1f))
                                    if (i == 0) {
                                        rPath.moveTo(x, ry)
                                        gPath.moveTo(x, gy)
                                        yPath.moveTo(x, ly)
                                    } else {
                                        rPath.lineTo(x, ry)
                                        gPath.lineTo(x, gy)
                                        yPath.lineTo(x, ly)
                                    }
                                }
                                drawPath(rPath, ElectricSlateTokens.Destructive, style = Stroke(width = 1.5.dp.toPx()))
                                drawPath(gPath, ElectricSlateTokens.PrimaryAccent, style = Stroke(width = 2.dp.toPx()))
                                drawPath(yPath, ElectricSlateTokens.TextPrimary, style = Stroke(width = 1.5.dp.toPx()))
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Shadows Clip: ${String.format("%.1f%%", inspectionReport.histogram.shadowClipPercent)}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Text(
                                    text = "Highlights Clip: ${String.format("%.1f%%", inspectionReport.histogram.highlightClipPercent)}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }

                        // 6-Swatch Dominant Color Palette Extractor (F-G09)
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
                                    text = "6-Swatch Dominant Color Palette",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                if (copiedHexNotice != null) {
                                    Text(
                                        text = "Copied $copiedHexNotice",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                inspectionReport.dominantSwatches.forEach { sw ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.BgCanvas)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .jigglePressClickable { copiedHexNotice = sw.hexCode }
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(ElectricSlateTokens.PrimaryAccent)
                                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = sw.hexCode,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = ElectricSlateTokens.TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${sw.populationPercent}%",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 10.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Deep EXIF, Codec Stream & SHA-256 Telemetry + 1-Tap EXIF Privacy Scrubber (F-G08 & F-G10)
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            contentPadding = 12.dp
                        ) {
                            Text(
                                text = "Stream Codec & Camera EXIF Telemetry",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MetaRow("File Name", inspectionReport.fileName)
                            MetaRow("Container & MIME", "${inspectionReport.containerFormat} (${inspectionReport.mimeType})")
                            MetaRow("Resolution", inspectionReport.resolutionSummary)
                            MetaRow("Stream / Color", inspectionReport.codecSummary)
                            MetaRow("Camera / Engine", inspectionReport.cameraMakeModel)
                            MetaRow("Optics / Exposure", inspectionReport.lensAndExposure)
                            MetaRow("GPS Privacy", inspectionReport.gpsStatus)
                            MetaRow("SHA-256 Digest", inspectionReport.sha256Hex)

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    text = "1-Tap Scrub EXIF & GPS",
                                    leadingIcon = Icons.Outlined.CleaningServices,
                                    onClick = { onScrubExifPrivacy(targetItem) },
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = if (targetItem.isVaulted) "Restore Vault" else "Move to Vault",
                                    leadingIcon = Icons.Outlined.Lock,
                                    onClick = { onToggleVaultItem(targetItem, !targetItem.isVaulted) },
                                    variant = AppButtonVariant.SECONDARY
                                )
                            }
                        }
                    }
                }

                InspectorTab.PRIVATE_VAULT -> {
                    // PIN-Locked Private Vault (F-G11)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        isGlow = vaultUnlocked,
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (vaultUnlocked) "Private Vault Unlocked" else "PIN-Locked Private Vault",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            if (vaultUnlocked) {
                                AppButton(
                                    text = "Lock Vault",
                                    leadingIcon = Icons.Outlined.Lock,
                                    onClick = onLockVault,
                                    variant = AppButtonVariant.SECONDARY,
                                    height = 34.dp,
                                    horizontalPadding = 10.dp,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pinStatusMessage,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )

                        if (!vaultUnlocked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.BgCanvas)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (pinInput.isEmpty()) {
                                    Text(
                                        text = "Enter 4-digit PIN (e.g. 2026)...",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    text = "Unlock with PIN",
                                    leadingIcon = Icons.Outlined.LockOpen,
                                    onClick = {
                                        val ok = onUnlockVaultPin(pinInput.ifBlank { "2026" })
                                        pinStatusMessage = if (ok) {
                                            "Vault unlocked via SHA-256 salted PIN verification."
                                        } else {
                                            "Invalid PIN. Try default PIN 2026."
                                        }
                                    },
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (vaultItems.isEmpty()) {
                                Text(
                                    text = "Vault is currently empty. Use 'Move to Vault' on any photo, video, or audio item to hide it from the public gallery.",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    vaultItems.forEach { vItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ElectricSlateTokens.BgCanvas)
                                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = vItem.title,
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = ElectricSlateTokens.TextPrimary
                                                )
                                                Text(
                                                    text = "${vItem.qualityBadge} • ${vItem.formattedSize}",
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    color = ElectricSlateTokens.TextMuted
                                                )
                                            }
                                            AppButton(
                                                text = "Restore",
                                                onClick = { onToggleVaultItem(vItem, false) },
                                                variant = AppButtonVariant.SECONDARY,
                                                height = 32.dp,
                                                horizontalPadding = 10.dp,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                InspectorTab.TRASH_DUPLICATES -> {
                    // Duplicate & Burst Sequence Finder (F-G12)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Duplicate & Similar Burst Finder (${duplicateGroups.size} Groups)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (duplicateGroups.isEmpty()) {
                            Text(
                                text = "No duplicate or redundant burst groups detected.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                duplicateGroups.forEach { group ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.BgCanvas)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = group.reason,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.PrimaryAccent
                                        )
                                        group.items.forEachIndexed { idx, dupItem ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${if (idx == 0) "★ Keep: " else "Candidate: "}${dupItem.title} (${dupItem.formattedSize})",
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = ElectricSlateTokens.TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (idx > 0) {
                                                    AppButton(
                                                        text = "Trash",
                                                        leadingIcon = Icons.Outlined.DeleteOutline,
                                                        onClick = { onMoveToTrash(dupItem) },
                                                        variant = AppButtonVariant.DESTRUCTIVE,
                                                        height = 30.dp,
                                                        horizontalPadding = 8.dp,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 30-Day Recycle Bin (F-G12)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "30-Day Recycle Bin (${trashedItems.size} Items)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (trashedItems.isEmpty()) {
                            Text(
                                text = "Recycle Bin is empty.",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                trashedItems.forEach { tItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.BgCanvas)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tItem.title,
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                            Text(
                                                text = "${tItem.qualityBadge} • ${tItem.formattedSize} • 30 days remaining",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                        AppButton(
                                            text = "Restore",
                                            leadingIcon = Icons.Outlined.RestoreFromTrash,
                                            onClick = { onRestoreFromTrash(tItem) },
                                            variant = AppButtonVariant.PRIMARY,
                                            height = 32.dp,
                                            horizontalPadding = 10.dp,
                                            fontSize = 11.sp
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
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = ElectricSlateTokens.TextMuted
        )
        Text(
            text = value,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = ElectricSlateTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
