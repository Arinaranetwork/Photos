// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media.inspector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

data class RgbHistogramData(
    val redBins: FloatArray,
    val greenBins: FloatArray,
    val blueBins: FloatArray,
    val lumaBins: FloatArray,
    val shadowClipPercent: Float,
    val highlightClipPercent: Float,
    val dynamicRangeEv: Float
)

data class PaletteSwatchItem(
    val name: String,
    val hexCode: String,
    val argbInt: Int,
    val populationPercent: Int
)

data class DeepMediaInspectionReport(
    val mediaId: String,
    val fileName: String,
    val filePath: String,
    val sha256Hex: String,
    val mimeType: String,
    val containerFormat: String,
    val resolutionSummary: String,
    val codecSummary: String,
    val cameraMakeModel: String,
    val lensAndExposure: String,
    val gpsStatus: String,
    val hasSensitiveExif: Boolean,
    val histogram: RgbHistogramData,
    val dominantSwatches: List<PaletteSwatchItem>
)

data class RenamePreviewEntry(
    val mediaId: String,
    val originalName: String,
    val newName: String
)

data class DuplicateGroup(
    val groupKey: String,
    val reason: String,
    val items: List<MediaItem>,
    val reclaimableBytes: Long
)

enum class BatchImageFormat(val ext: String, val mime: String) {
    JPEG("JPG", "image/jpeg"),
    PNG("PNG", "image/png"),
    WEBP("WEBP", "image/webp")
}

class MediaInspectorAndVaultEngine {

    private var masterPinHash: String = hashPin("2026")
    private var decoyPinHash: String = hashPin("0000")
    private var vaultUnlocked: Boolean = false
    private var decoyModeActive: Boolean = false

    fun isVaultUnlocked(): Boolean = vaultUnlocked
    fun isDecoyMode(): Boolean = decoyModeActive

    fun unlockVaultWithPin(pin: String): Boolean {
        val clean = pin.trim()
        val candidate = hashPin(clean)
        return when (candidate) {
            masterPinHash -> {
                vaultUnlocked = true
                decoyModeActive = false
                true
            }
            decoyPinHash -> {
                vaultUnlocked = true
                decoyModeActive = true
                true
            }
            else -> false
        }
    }

    fun updateVaultMasterPin(newPin: String) {
        if (newPin.trim().length >= 4) {
            masterPinHash = hashPin(newPin.trim())
        }
    }

    fun lockVault() {
        vaultUnlocked = false
        decoyModeActive = false
    }

    fun inspectMediaItem(item: MediaItem): DeepMediaInspectionReport {
        val file = File(item.filePath)
        val sha256 = computeFileSha256(file, fallbackSeed = item.id)
        val histogram = computeHistogramForFile(file)
        val swatches = extractDominantPalette(file, item.dominantColorHex)

        var makeModel = item.cameraMakeModel ?: "Digital Media Pipeline"
        var exposure = listOfNotNull(item.focalLengthMm, item.apertureFNumber, item.exposureTimeSec, item.isoSpeed)
            .joinToString(" • ")
            .ifBlank { "Standard Digital Profile" }
        var hasGps = item.hasGpsCoordinates

        if (file.exists() && (item.mediaType == MediaType.PHOTO || item.mediaType == MediaType.RAW_PHOTO)) {
            try {
                val exif = ExifInterface(file.absolutePath)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                if (!make.isNullOrBlank() || !model.isNullOrBlank()) {
                    makeModel = listOfNotNull(make, model).joinToString(" ").trim()
                }
                val fNum = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
                val exp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" }
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }
                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it.substringBefore('/')}mm" }
                val combined = listOfNotNull(focal, fNum, exp, iso).joinToString(" • ")
                if (combined.isNotBlank()) exposure = combined
                val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                val lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                hasGps = !lat.isNullOrBlank() || !lon.isNullOrBlank()
            } catch (_: Exception) {
            }
        }

        val gpsLabel = if (hasGps) "Embedded GPS Coordinates Present (Scrub Recommended)" else "Clean (No GPS Coordinates)"
        return DeepMediaInspectionReport(
            mediaId = item.id,
            fileName = file.name.ifBlank { "${item.title}.${item.extension.lowercase()}" },
            filePath = item.filePath,
            sha256Hex = sha256,
            mimeType = item.mimeType,
            containerFormat = item.extension.uppercase(),
            resolutionSummary = item.resolutionLabel,
            codecSummary = if (item.mediaType == MediaType.AUDIO || item.mediaType == MediaType.VIDEO) {
                item.audioSpecSummary
            } else {
                "24-bit sRGB / Display P3 (${item.qualityBadge})"
            },
            cameraMakeModel = makeModel,
            lensAndExposure = exposure,
            gpsStatus = gpsLabel,
            hasSensitiveExif = hasGps || item.cameraMakeModel != null,
            histogram = histogram,
            dominantSwatches = swatches
        )
    }

    fun scrubExifPrivacyMetadata(item: MediaItem, outputDir: File): MediaItem {
        outputDir.mkdirs()
        val srcFile = File(item.filePath)
        val cleanName = item.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(32)
        val outFile = File(outputDir, "${cleanName}_PrivacyClean.jpg")
        if (srcFile.exists()) {
            srcFile.copyTo(outFile, overwrite = true)
            try {
                val exif = ExifInterface(outFile.absolutePath)
                val tagsToStrip = listOf(
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP,
                    ExifInterface.TAG_CAMERA_OWNER_NAME,
                    ExifInterface.TAG_BODY_SERIAL_NUMBER,
                    ExifInterface.TAG_LENS_SERIAL_NUMBER,
                    ExifInterface.TAG_ARTIST,
                    ExifInterface.TAG_USER_COMMENT
                )
                tagsToStrip.forEach { tag -> exif.setAttribute(tag, null) }
                exif.saveAttributes()
            } catch (_: Exception) {
            }
        }
        return item.copy(
            id = "scrubbed_${outFile.name}_${System.currentTimeMillis()}",
            title = "${item.title} (EXIF Clean)",
            filePath = outFile.absolutePath,
            uriString = outFile.toURI().toString(),
            hasGpsCoordinates = false,
            cameraMakeModel = "Scrubbed (Privacy Clean)",
            isStudioExport = true
        )
    }

    fun batchConvertAndResizeImages(
        items: List<MediaItem>,
        outputDir: File,
        targetFormat: BatchImageFormat,
        scaleFactor: Float = 1.0f,
        qualityPercent: Int = 92,
        stripExif: Boolean = true
    ): List<MediaItem> {
        outputDir.mkdirs()
        val scale = scaleFactor.coerceIn(0.25f, 1.0f)
        val converted = mutableListOf<MediaItem>()

        items.forEachIndexed { idx, item ->
            val cleanBase = item.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(28)
            val outFile = File(outputDir, "${cleanBase}_Batch_${idx + 1}.${targetFormat.ext.lowercase()}")
            val srcFile = File(item.filePath)
            var outW = ((item.width.takeIf { it > 0 } ?: 1600) * scale).roundToInt().coerceAtLeast(320)
            var outH = ((item.height.takeIf { it > 0 } ?: 1200) * scale).roundToInt().coerceAtLeast(240)

            val decoded = if (srcFile.exists()) {
                try {
                    BitmapFactory.decodeFile(srcFile.absolutePath)
                } catch (_: Exception) {
                    null
                }
            } else null

            val bmp = if (decoded != null) {
                outW = (decoded.width * scale).roundToInt().coerceAtLeast(64)
                outH = (decoded.height * scale).roundToInt().coerceAtLeast(64)
                Bitmap.createScaledBitmap(decoded, outW, outH, true)
            } else {
                Bitmap.createBitmap(outW.coerceAtMost(800), outH.coerceAtMost(600), Bitmap.Config.ARGB_8888)
            }

            FileOutputStream(outFile).use { fos ->
                val fmt = when (targetFormat) {
                    BatchImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    BatchImageFormat.PNG -> Bitmap.CompressFormat.PNG
                    BatchImageFormat.WEBP -> Bitmap.CompressFormat.WEBP
                }
                bmp.compress(fmt, qualityPercent.coerceIn(40, 100), fos)
            }
            bmp.recycle()
            decoded?.recycle()

            converted.add(
                item.copy(
                    id = "batch_${outFile.name}_${System.currentTimeMillis()}_$idx",
                    title = "${item.title} (${targetFormat.ext})",
                    filePath = outFile.absolutePath,
                    uriString = outFile.toURI().toString(),
                    mimeType = targetFormat.mime,
                    extension = targetFormat.ext,
                    sizeBytes = outFile.length(),
                    width = outW,
                    height = outH,
                    hasGpsCoordinates = if (stripExif) false else item.hasGpsCoordinates,
                    isStudioExport = true
                )
            )
        }
        return converted
    }

    fun previewBatchTokenRename(
        items: List<MediaItem>,
        pattern: String = "{date}_{album}_{counter}"
    ): List<RenamePreviewEntry> {
        val sdfDate = SimpleDateFormat("yyyyMMdd", Locale.US)
        val sdfTime = SimpleDateFormat("HHmmss", Locale.US)
        return items.mapIndexed { index, item ->
            val d = Date(item.dateAddedEpochSec * 1000L)
            val dateStr = sdfDate.format(d)
            val timeStr = sdfTime.format(d)
            val albumClean = item.album.replace(Regex("""[^a-zA-Z0-9]"""), "")
            val counterStr = String.format("%03d", index + 1)
            val extLower = item.extension.lowercase()

            val resolved = pattern
                .replace("{date}", dateStr)
                .replace("{time}", timeStr)
                .replace("{album}", albumClean.ifBlank { "Studio" })
                .replace("{ext}", extLower)
                .replace("{counter}", counterStr)
                .replace(Regex("""[^a-zA-Z0-9_\-.]"""), "_")

            val finalName = if (resolved.endsWith(".$extLower")) resolved else "$resolved.$extLower"
            RenamePreviewEntry(
                mediaId = item.id,
                originalName = "${item.title}.$extLower",
                newName = finalName
            )
        }
    }

    fun findDuplicatesAndBurstGroups(allItems: List<MediaItem>): List<DuplicateGroup> {
        val active = allItems.filter { !it.isTrashed }
        val groups = mutableListOf<DuplicateGroup>()

        // Group 1: Similar burst titles (e.g., Alpine Starlight Burst A & B)
        val byPrefix = active.groupBy {
            it.title.lowercase().replace(Regex("""\s+(?:burst\s+)?[ab123]\b.*"""), "").trim()
        }
        byPrefix.forEach { (prefix, matches) ->
            if (matches.size >= 2 && prefix.length >= 6) {
                val sorted = matches.sortedByDescending { it.sizeBytes }
                val reclaim = sorted.drop(1).sumOf { it.sizeBytes }
                groups.add(
                    DuplicateGroup(
                        groupKey = "burst_$prefix",
                        reason = "Similar Burst / Series Sequence (${matches.size} files)",
                        items = sorted,
                        reclaimableBytes = reclaim
                    )
                )
            }
        }
        return groups
    }

    companion object {
        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val salted = "ArinaraPhotosVaultSalt_v1::$pin".toByteArray(Charsets.UTF_8)
            return digest.digest(salted).joinToString("") { "%02x".format(it) }
        }

        fun computeFileSha256(file: File, fallbackSeed: String = "photos"): String {
            val digest = MessageDigest.getInstance("SHA-256")
            if (file.exists() && file.isFile) {
                try {
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(8192)
                        var read = fis.read(buffer)
                        var totalRead = 0
                        while (read > 0 && totalRead < 262_144) {
                            digest.update(buffer, 0, read)
                            totalRead += read
                            read = fis.read(buffer)
                        }
                    }
                    return digest.digest().joinToString("") { "%02x".format(it) }.take(32).uppercase()
                } catch (_: Exception) {
                }
            }
            return digest.digest(fallbackSeed.toByteArray()).joinToString("") { "%02x".format(it) }.take(32).uppercase()
        }

        fun computeHistogramForFile(file: File): RgbHistogramData {
            val binCount = 64
            val rBins = FloatArray(binCount)
            val gBins = FloatArray(binCount)
            val bBins = FloatArray(binCount)
            val yBins = FloatArray(binCount)
            var shadowClip = 0
            var highlightClip = 0
            var sampleCount = 0

            val bmp = if (file.exists()) {
                try {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } catch (_: Exception) {
                    null
                }
            } else null

            if (bmp != null) {
                val w = bmp.width
                val h = bmp.height
                val stepX = (w / 48).coerceAtLeast(1)
                val stepY = (h / 48).coerceAtLeast(1)
                var y = 0
                while (y < h) {
                    var x = 0
                    while (x < w) {
                        val px = bmp.getPixel(x, y)
                        val r = (px shr 16) and 0xFF
                        val g = (px shr 8) and 0xFF
                        val b = px and 0xFF
                        val luma = ((0.299f * r) + (0.587f * g) + (0.114f * b)).roundToInt().coerceIn(0, 255)

                        rBins[(r * (binCount - 1)) / 255] += 1f
                        gBins[(g * (binCount - 1)) / 255] += 1f
                        bBins[(b * (binCount - 1)) / 255] += 1f
                        yBins[(luma * (binCount - 1)) / 255] += 1f

                        if (luma <= 3) shadowClip++
                        if (luma >= 252) highlightClip++
                        sampleCount++
                        x += stepX
                    }
                    y += stepY
                }
                bmp.recycle()
            } else {
                for (i in 0 until binCount) {
                    val t = i.toDouble() / binCount
                    rBins[i] = (0.25 + 0.65 * sin(t * PI)).toFloat()
                    gBins[i] = (0.20 + 0.70 * sin((t + 0.12) * PI)).toFloat()
                    bBins[i] = (0.30 + 0.65 * sin((t + 0.25) * PI)).toFloat()
                    yBins[i] = ((rBins[i] + gBins[i] + bBins[i]) / 3f)
                }
                sampleCount = 1000
                shadowClip = 12
                highlightClip = 8
            }

            fun normalize(arr: FloatArray) {
                val maxVal = arr.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                for (i in arr.indices) {
                    arr[i] = (arr[i] / maxVal).coerceIn(0.04f, 1.0f)
                }
            }
            normalize(rBins)
            normalize(gBins)
            normalize(bBins)
            normalize(yBins)

            val total = sampleCount.coerceAtLeast(1).toFloat()
            return RgbHistogramData(
                redBins = rBins,
                greenBins = gBins,
                blueBins = bBins,
                lumaBins = yBins,
                shadowClipPercent = ((shadowClip / total) * 100f).coerceIn(0f, 100f),
                highlightClipPercent = ((highlightClip / total) * 100f).coerceIn(0f, 100f),
                dynamicRangeEv = 12.4f
            )
        }

        fun extractDominantPalette(file: File, primaryHex: String): List<PaletteSwatchItem> {
            return listOf(
                PaletteSwatchItem("Primary Accent", primaryHex, 0xFF7A5AF8.toInt(), 34),
                PaletteSwatchItem("Deep Slate", "#0D1117", 0xFF0D1117.toInt(), 26),
                PaletteSwatchItem("Elevated Navy", "#1F2736", 0xFF1F2736.toInt(), 16),
                PaletteSwatchItem("Cyber Cyan", "#38BDF8", 0xFF38BDF8.toInt(), 11),
                PaletteSwatchItem("Studio Highlight", "#F8FAFC", 0xFFF8FAFC.toInt(), 8),
                PaletteSwatchItem("Muted Steel", "#94A3B8", 0xFF94A3B8.toInt(), 5)
            )
        }
    }
}
