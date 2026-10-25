// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.inspector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    HISTOGRAM_EXIF("Info"),
    PRIVATE_VAULT("Vault"),
    TRASH_DUPLICATES("Trash")
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
    if (!visible) return

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var pinInput by remember { mutableStateOf("") }
    var pinStatusMessage by remember { mutableStateOf("Enter 4-digit PIN (Default: 2026)") }
    var copiedHexNotice by remember { mutableStateOf<String?>(null) }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Details & Vault"
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
                            text = "Select an item to view its details.",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    } else {
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
                                    text = "Histogram",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = String.format("%.1f EV", inspectionReport.histogram.dynamicRangeEv),
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
                                    .height(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.BgCanvas)
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
                                    text = "Color Palette",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                if (copiedHexNotice != null) {
                                    Text(
                                        text = copiedHexNotice!!,
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
                                            .jigglePressClickable { copiedHexNotice = sw.hexCode }
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(ElectricSlateTokens.PrimaryAccent)
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
                                text = "File Information",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MetaRow("Name", inspectionReport.fileName)
                            MetaRow("Format", inspectionReport.containerFormat)
                            MetaRow("Resolution", inspectionReport.resolutionSummary)
                            MetaRow("Camera", inspectionReport.cameraMakeModel)
                            MetaRow("Exposure", inspectionReport.lensAndExposure)
                            MetaRow("Location", inspectionReport.gpsStatus)

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    text = "Remove Location",
                                    leadingIcon = Icons.Outlined.CleaningServices,
                                    onClick = { onScrubExifPrivacy(targetItem) },
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = if (targetItem.isVaulted) "Unhide" else "Hide",
                                    leadingIcon = Icons.Outlined.Lock,
                                    onClick = { onToggleVaultItem(targetItem, !targetItem.isVaulted) },
                                    variant = AppButtonVariant.SECONDARY
                                )
                            }
                        }
                    }
                }

                InspectorTab.PRIVATE_VAULT -> {
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
                                text = if (vaultUnlocked) "Private Vault" else "Locked Vault",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            if (vaultUnlocked) {
                                AppButton(
                                    text = "Lock",
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
                                        text = "PIN...",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AppButton(
                                text = "Unlock",
                                leadingIcon = Icons.Outlined.LockOpen,
                                onClick = {
                                    val ok = onUnlockVaultPin(pinInput.ifBlank { "2026" })
                                    pinStatusMessage = if (ok) "Unlocked" else "Invalid PIN"
                                },
                                variant = AppButtonVariant.PRIMARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (vaultItems.isEmpty()) {
                                Text(
                                    text = "No hidden items.",
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
                                                    text = vItem.formattedSize,
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    color = ElectricSlateTokens.TextMuted
                                                )
                                            }
                                            AppButton(
                                                text = "Unhide",
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
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Duplicates (${duplicateGroups.size})",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (duplicateGroups.isEmpty()) {
                            Text(
                                text = "No duplicates found.",
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
                                            .padding(10.dp)
                                    ) {
                                        group.items.forEachIndexed { idx, dupItem ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${dupItem.title} (${dupItem.formattedSize})",
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = ElectricSlateTokens.TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (idx > 0) {
                                                    AppButton(
                                                        text = "Delete",
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

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = 12.dp
                    ) {
                        Text(
                            text = "Trash (${trashedItems.size})",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (trashedItems.isEmpty()) {
                            Text(
                                text = "Trash is empty.",
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
                                                text = tItem.formattedSize,
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
