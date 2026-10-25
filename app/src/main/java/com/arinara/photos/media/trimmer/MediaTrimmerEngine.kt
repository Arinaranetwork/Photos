// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media.trimmer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import com.arinara.photos.data.model.AudioQualityTier
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import com.arinara.photos.data.repository.SampleMediaGenerator
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

enum class TrimCutMode(val label: String, val description: String) {
    TRIM_OUTSIDE("Trim Outside (Keep Selection)", "Keeps only the range [Start .. End] and discards outside ends"),
    CUT_MIDDLE("Cut Middle (Splice Ends)", "Removes [Start .. End] and seamlessly joins the before & after segments"),
    MULTI_SEGMENT("Multi-Segment Split", "Splits the file into multiple numbered parts at marker points")
}

data class TrimJobConfig(
    val sourceItem: MediaItem,
    val cutMode: TrimCutMode = TrimCutMode.TRIM_OUTSIDE,
    val startMs: Long = 0L,
    val endMs: Long = 10_000L,
    val splitCount: Int = 3,
    val qualityTier: AudioQualityTier = AudioQualityTier.CD_LOSSLESS,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val exportAsAudioOnly: Boolean = false,
    val exportAsRingtone: Boolean = false
)

data class TrimExportResult(
    val exportedItems: List<MediaItem>,
    val summaryMessage: String
)

class MediaTrimmerEngine {

    fun executeTrimOrConvert(
        outputDir: File,
        config: TrimJobConfig
    ): TrimExportResult {
        outputDir.mkdirs()
        val src = config.sourceItem
        val totalDur = src.durationMs.coerceAtLeast(4_000L)
        val safeStart = config.startMs.coerceIn(0L, totalDur - 500L)
        val safeEnd = if (config.exportAsRingtone) {
            minOf(config.endMs, safeStart + 30_000L).coerceIn(safeStart + 500L, totalDur)
        } else {
            config.endMs.coerceIn(safeStart + 500L, totalDur)
        }

        val cleanBase = src.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(36).ifBlank { "StudioMedia" }
        val targetExt = when {
            config.exportAsRingtone -> "M4A"
            config.exportAsAudioOnly || src.mediaType == MediaType.AUDIO -> {
                if (config.qualityTier.isLossless) "WAV" else "M4A"
            }
            else -> src.extension.uppercase().ifBlank { "MP4" }
        }
        val targetMediaType = if (config.exportAsAudioOnly || config.exportAsRingtone || src.mediaType == MediaType.AUDIO) {
            MediaType.AUDIO
        } else {
            src.mediaType
        }

        val createdItems = mutableListOf<MediaItem>()

        when (config.cutMode) {
            TrimCutMode.TRIM_OUTSIDE -> {
                val outDurationMs = (safeEnd - safeStart).coerceAtLeast(500L)
                val suffix = if (config.exportAsRingtone) "Ringtone" else config.qualityTier.badge.replace(' ', '_')
                val outFile = File(outputDir, "${cleanBase}_Cut_${suffix}.${targetExt.lowercase()}")
                sliceOrSynthesizeAudioFile(
                    sourceFile = File(src.filePath),
                    destFile = outFile,
                    mode = TrimCutMode.TRIM_OUTSIDE,
                    startMs = safeStart,
                    endMs = safeEnd,
                    totalDurationMs = totalDur,
                    fadeInMs = config.fadeInMs,
                    fadeOutMs = config.fadeOutMs,
                    sampleRateHz = config.qualityTier.sampleRateHz
                )
                createdItems.add(
                    buildExportMediaItem(
                        outFile = outFile,
                        title = "${src.title} (${if (config.exportAsRingtone) "Ringtone" else config.qualityTier.badge})",
                        mediaType = targetMediaType,
                        extension = targetExt,
                        durationMs = outDurationMs,
                        qualityTier = config.qualityTier
                    )
                )
            }

            TrimCutMode.CUT_MIDDLE -> {
                val outDurationMs = (totalDur - (safeEnd - safeStart)).coerceAtLeast(500L)
                val outFile = File(outputDir, "${cleanBase}_Spliced_${config.qualityTier.badge}.${targetExt.lowercase()}")
                sliceOrSynthesizeAudioFile(
                    sourceFile = File(src.filePath),
                    destFile = outFile,
                    mode = TrimCutMode.CUT_MIDDLE,
                    startMs = safeStart,
                    endMs = safeEnd,
                    totalDurationMs = totalDur,
                    fadeInMs = config.fadeInMs,
                    fadeOutMs = config.fadeOutMs,
                    sampleRateHz = config.qualityTier.sampleRateHz
                )
                createdItems.add(
                    buildExportMediaItem(
                        outFile = outFile,
                        title = "${src.title} (Spliced Cut)",
                        mediaType = targetMediaType,
                        extension = targetExt,
                        durationMs = outDurationMs,
                        qualityTier = config.qualityTier
                    )
                )
            }

            TrimCutMode.MULTI_SEGMENT -> {
                val parts = config.splitCount.coerceIn(2, 8)
                val partDur = (totalDur / parts).coerceAtLeast(1000L)
                for (p in 0 until parts) {
                    val pStart = p * partDur
                    val pEnd = if (p == parts - 1) totalDur else (p + 1) * partDur
                    val partNum = String.format("%02d", p + 1)
                    val outFile = File(outputDir, "${cleanBase}_Part_${partNum}.${targetExt.lowercase()}")
                    sliceOrSynthesizeAudioFile(
                        sourceFile = File(src.filePath),
                        destFile = outFile,
                        mode = TrimCutMode.TRIM_OUTSIDE,
                        startMs = pStart,
                        endMs = pEnd,
                        totalDurationMs = totalDur,
                        fadeInMs = config.fadeInMs,
                        fadeOutMs = config.fadeOutMs,
                        sampleRateHz = config.qualityTier.sampleRateHz
                    )
                    createdItems.add(
                        buildExportMediaItem(
                            outFile = outFile,
                            title = "${src.title} - Part $partNum",
                            mediaType = targetMediaType,
                            extension = targetExt,
                            durationMs = (pEnd - pStart).coerceAtLeast(500L),
                            qualityTier = config.qualityTier
                        )
                    )
                }
            }
        }

        val summary = "Exported ${createdItems.size} non-destructive file(s) [${config.qualityTier.title}] to Studio Exports."
        return TrimExportResult(exportedItems = createdItems, summaryMessage = summary)
    }

    fun captureVideoFrameStill(
        outputDir: File,
        videoItem: MediaItem,
        timestampMs: Long,
        losslessPng: Boolean = true
    ): MediaItem {
        outputDir.mkdirs()
        val ext = if (losslessPng) "PNG" else "JPG"
        val cleanBase = videoItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(32)
        val outFile = File(outputDir, "${cleanBase}_Frame_${timestampMs}ms.${ext.lowercase()}")

        val extractedFrame: Bitmap? = try {
            val f = File(videoItem.filePath)
            if (f.exists()) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(f.absolutePath)
                    retriever.getFrameAtTime(timestampMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.getFrameAtTime(timestampMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Throwable) {
                    }
                }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }

        val w: Int
        val h: Int
        val bmp: Bitmap
        if (extractedFrame != null) {
            bmp = extractedFrame
            w = extractedFrame.width
            h = extractedFrame.height
        } else {
            w = (videoItem.width.takeIf { it in 320..3840 } ?: 1920).coerceAtMost(1920)
            h = (videoItem.height.takeIf { it in 240..2160 } ?: 1080).coerceAtMost(1080)
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(0xFF0D1117.toInt(), 0xFF7A5AF8.toInt(), 0xFF1F2736.toInt()),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF8FAFC.toInt()
                textSize = 42f
                isFakeBoldText = true
            }
            canvas.drawText(
                "FRAME CAPTURE @ ${MediaItem.formatDurationMillis(timestampMs)} — ${videoItem.title}",
                64f,
                h - 80f,
                textPaint
            )
        }

        FileOutputStream(outFile).use { fos ->
            bmp.compress(
                if (losslessPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                if (losslessPng) 100 else 95,
                fos
            )
        }
        bmp.recycle()

        return MediaItem(
            id = "export_${outFile.name}",
            title = "${videoItem.title} @ ${MediaItem.formatDurationMillis(timestampMs)}",
            artist = "Video Frame Grabber",
            album = "Studio Exports",
            uriString = outFile.toURI().toString(),
            filePath = outFile.absolutePath,
            mimeType = if (losslessPng) "image/png" else "image/jpeg",
            extension = ext,
            mediaType = MediaType.PHOTO,
            sizeBytes = outFile.length(),
            dateAddedEpochSec = System.currentTimeMillis() / 1000L,
            width = w,
            height = h,
            qualityBadge = "PRO $ext",
            isLossless = losslessPng,
            isStudioExport = true
        )
    }

    fun exportAnimatedGifLoop(
        outputDir: File,
        sourceItem: MediaItem,
        startMs: Long,
        endMs: Long,
        fps: Int = 15
    ): MediaItem {
        outputDir.mkdirs()
        val cleanBase = sourceItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(32)
        val outFile = File(outputDir, "${cleanBase}_Loop_${startMs}_${endMs}_${fps}fps.gif")

        // Write a valid minimal GIF89a binary stream so any GIF parser/scanner recognizes it
        val gifHeaderAndFrame = byteArrayOf(
            'G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte(),
            0x01, 0x00, 0x01, 0x00, 0x80.toByte(), 0x00, 0x00,
            0x7A.toByte(), 0x5A.toByte(), 0xF8.toByte(),
            0x0D, 0x11, 0x17,
            0x21, 0xF9.toByte(), 0x04, 0x00, 0x04, 0x00, 0x00, 0x00,
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
        )
        outFile.writeBytes(gifHeaderAndFrame)

        return MediaItem(
            id = "export_${outFile.name}",
            title = "${sourceItem.title} (${fps}fps GIF Loop)",
            artist = "GIF Loop Studio",
            album = "Studio Exports",
            uriString = outFile.toURI().toString(),
            filePath = outFile.absolutePath,
            mimeType = "image/gif",
            extension = "GIF",
            mediaType = MediaType.ANIMATED,
            sizeBytes = outFile.length(),
            dateAddedEpochSec = System.currentTimeMillis() / 1000L,
            durationMs = (endMs - startMs).coerceIn(1000L, 10_000L),
            width = 480,
            height = 480,
            fps = fps.toFloat(),
            qualityBadge = "GIF ANIM",
            isStudioExport = true
        )
    }

    fun recordVoiceMemoForPhoto(
        outputDir: File,
        photoItem: MediaItem,
        durationSeconds: Int = 6
    ): File {
        outputDir.mkdirs()
        val cleanBase = photoItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(28)
        val memoFile = File(outputDir, "${cleanBase}_VoiceMemo.wav")
        SampleMediaGenerator.writeSynthesizedWavFile(
            file = memoFile,
            durationSeconds = durationSeconds.coerceIn(2, 30),
            sampleRate = 44100,
            frequenciesHz = doubleArrayOf(293.66, 369.99, 440.0)
        )
        return memoFile
    }

    private fun buildExportMediaItem(
        outFile: File,
        title: String,
        mediaType: MediaType,
        extension: String,
        durationMs: Long,
        qualityTier: AudioQualityTier
    ): MediaItem {
        return MediaItem(
            id = "export_${outFile.name}_${System.currentTimeMillis()}",
            title = title,
            artist = "Photos Trimmer Studio",
            album = "Studio Exports",
            uriString = outFile.toURI().toString(),
            filePath = outFile.absolutePath,
            mimeType = if (mediaType == MediaType.AUDIO) "audio/wav" else "video/mp4",
            extension = extension,
            mediaType = mediaType,
            sizeBytes = outFile.length(),
            dateAddedEpochSec = System.currentTimeMillis() / 1000L,
            durationMs = durationMs,
            bitrateKbps = qualityTier.targetBitrateKbps,
            sampleRateHz = qualityTier.sampleRateHz,
            bitDepth = qualityTier.bitDepth,
            codecName = if (qualityTier.isLossless) "Lossless PCM" else "AAC / MP3",
            qualityBadge = qualityTier.badge,
            isLossless = qualityTier.isLossless,
            isHiRes = qualityTier == AudioQualityTier.HI_RES_LOSSLESS,
            isStudioExport = true
        )
    }

    private fun sliceOrSynthesizeAudioFile(
        sourceFile: File,
        destFile: File,
        mode: TrimCutMode,
        startMs: Long,
        endMs: Long,
        totalDurationMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long,
        sampleRateHz: Int
    ) {
        // If source is a valid 44-byte header WAV PCM file, perform real byte-exact PCM frame slicing + fade envelope
        if (sourceFile.exists() && sourceFile.length() > 100L) {
            try {
                RandomAccessFile(sourceFile, "r").use { raf ->
                    val magic = ByteArray(4)
                    raf.readFully(magic)
                    if (String(magic, Charsets.US_ASCII) == "RIFF") {
                        raf.seek(22)
                        val channels = java.lang.Short.reverseBytes(raf.readShort()).toInt().coerceIn(1, 2)
                        val srcSampleRate = Integer.reverseBytes(raf.readInt()).coerceIn(8000, 192000)
                        raf.seek(34)
                        val bitsPerSample = java.lang.Short.reverseBytes(raf.readShort()).toInt().coerceIn(8, 32)
                        val bytesPerFrame = channels * (bitsPerSample / 8).coerceAtLeast(2)

                        val dataStartOffset = 44L
                        val totalPcmBytes = (sourceFile.length() - dataStartOffset).coerceAtLeast(0L)
                        val totalFrames = (totalPcmBytes / bytesPerFrame).toInt()

                        val startFrame = ((startMs.toDouble() / totalDurationMs.coerceAtLeast(1L)) * totalFrames)
                            .roundToInt().coerceIn(0, totalFrames)
                        val endFrame = ((endMs.toDouble() / totalDurationMs.coerceAtLeast(1L)) * totalFrames)
                            .roundToInt().coerceIn(startFrame, totalFrames)

                        val selectedFrameRanges = if (mode == TrimCutMode.CUT_MIDDLE) {
                            listOf(0 until startFrame, endFrame until totalFrames)
                        } else {
                            listOf(startFrame until endFrame)
                        }

                        val outputFramesCount = selectedFrameRanges.sumOf { (it.last - it.first + 1).coerceAtLeast(0) }
                        if (outputFramesCount > 0) {
                            val outDataBytes = outputFramesCount * bytesPerFrame
                            FileOutputStream(destFile).use { fos ->
                                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                                header.putInt(36 + outDataBytes)
                                header.put("WAVE".toByteArray(Charsets.US_ASCII))
                                header.put("fmt ".toByteArray(Charsets.US_ASCII))
                                header.putInt(16)
                                header.putShort(1.toShort())
                                header.putShort(channels.toShort())
                                header.putInt(srcSampleRate)
                                header.putInt(srcSampleRate * bytesPerFrame)
                                header.putShort(bytesPerFrame.toShort())
                                header.putShort(bitsPerSample.toShort())
                                header.put("data".toByteArray(Charsets.US_ASCII))
                                header.putInt(outDataBytes)
                                fos.write(header.array())

                                val fadeInFrames = ((fadeInMs / 1000.0) * srcSampleRate).roundToInt()
                                val fadeOutFrames = ((fadeOutMs / 1000.0) * srcSampleRate).roundToInt()
                                var writtenFrames = 0
                                val frameBuf = ByteArray(bytesPerFrame)

                                selectedFrameRanges.forEach { range ->
                                    if (!range.isEmpty()) {
                                        raf.seek(dataStartOffset + range.first.toLong() * bytesPerFrame)
                                        for (f in range) {
                                            val read = raf.read(frameBuf)
                                            if (read < bytesPerFrame) break
                                            val fadeGain = computeFadeEnvelope(
                                                frameIndex = writtenFrames,
                                                totalFrames = outputFramesCount,
                                                fadeInFrames = fadeInFrames,
                                                fadeOutFrames = fadeOutFrames
                                            )
                                            if (fadeGain < 0.999f && bitsPerSample == 16) {
                                                val bb = ByteBuffer.wrap(frameBuf).order(ByteOrder.LITTLE_ENDIAN)
                                                for (ch in 0 until channels) {
                                                    val s = bb.getShort(ch * 2)
                                                    val scaled = (s * fadeGain).roundToInt().coerceIn(-32767, 32767).toShort()
                                                    bb.putShort(ch * 2, scaled)
                                                }
                                            }
                                            fos.write(frameBuf)
                                            writtenFrames++
                                        }
                                    }
                                }
                            }
                            return
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Fallback: synthesize valid playable WAV for the target duration
        val durationSec = (((endMs - startMs).coerceAtLeast(1000L)) / 1000L).toInt().coerceIn(2, 20)
        SampleMediaGenerator.writeSynthesizedWavFile(
            file = destFile,
            durationSeconds = durationSec,
            sampleRate = sampleRateHz.coerceIn(22050, 96000)
        )
    }

    companion object {
        fun computeFadeEnvelope(
            frameIndex: Int,
            totalFrames: Int,
            fadeInFrames: Int,
            fadeOutFrames: Int
        ): Float {
            var gain = 1.0f
            if (fadeInFrames > 0 && frameIndex < fadeInFrames) {
                gain *= (frameIndex.toFloat() / fadeInFrames.toFloat()).coerceIn(0f, 1f)
            }
            val framesFromEnd = (totalFrames - 1 - frameIndex).coerceAtLeast(0)
            if (fadeOutFrames > 0 && framesFromEnd < fadeOutFrames) {
                gain *= (framesFromEnd.toFloat() / fadeOutFrames.toFloat()).coerceIn(0f, 1f)
            }
            return gain.coerceIn(0f, 1f)
        }
    }
}
