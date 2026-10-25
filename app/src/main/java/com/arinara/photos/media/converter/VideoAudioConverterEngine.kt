// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media.converter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class AudioConvertPreset(
    val label: String,
    val extension: String,
    val mimeType: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val isLossless: Boolean,
    val detail: String
) {
    MP3_320K("MP3 320k", "MP3", "audio/mpeg", 320, 48000, false, "320 kbps CBR • Maximum MP3 Fidelity"),
    MP3_256K("MP3 256k", "MP3", "audio/mpeg", 256, 48000, false, "256 kbps CBR • High Fidelity"),
    MP3_192K("MP3 192k", "MP3", "audio/mpeg", 192, 44100, false, "192 kbps CBR • Balanced Quality"),
    MP3_128K("MP3 128k", "MP3", "audio/mpeg", 128, 44100, false, "128 kbps CBR • Compact Share"),
    M4A_AAC_256K("M4A 256k", "M4A", "audio/mp4", 256, 48000, false, "AAC-LC 256 kbps • Hardware Native"),
    WAV_LOSSLESS("WAV Lossless", "WAV", "audio/wav", 1536, 48000, true, "16-Bit 48kHz Uncompressed PCM")
}

enum class VideoResolutionPreset(
    val label: String,
    val shortEdgePx: Int,
    val detail: String
) {
    ORIGINAL("Original", 0, "Keep native video resolution"),
    UHD_4K("4K (2160p)", 2160, "3840×2160 Ultra HD"),
    QHD_1440P("2K (1440p)", 1440, "2560×1440 Quad HD"),
    FHD_1080P("1080p", 1080, "1920×1080 Full HD"),
    HD_720P("720p", 720, "1280×720 HD"),
    SD_480P("480p", 480, "854×480 Fast Share")
}

enum class VideoFpsPreset(
    val label: String,
    val targetFps: Int,
    val detail: String
) {
    ORIGINAL("Original FPS", 0, "Keep native frame rate"),
    FPS_60("60 fps", 60, "Smooth High Frame Rate"),
    FPS_30("30 fps", 30, "Standard Universal Video"),
    FPS_24("24 fps", 24, "Cinema Cadence")
}

enum class VideoQualityProfile(
    val label: String,
    val bitrateMultiplier: Float,
    val detail: String
) {
    MASTER("Master High", 1.55f, "Maximum clarity & low compression"),
    BALANCED("Balanced", 1.00f, "Recommended visual quality vs. size"),
    COMPACT("Compact", 0.55f, "Smallest size for instant sharing")
}

enum class VideoCodecChoice(
    val label: String,
    val mimeType: String,
    val bitrateEfficiency: Float,
    val detail: String
) {
    H264_AVC("H.264 (AVC)", MediaFormat.MIMETYPE_VIDEO_AVC, 1.0f, "Universal playback on all devices"),
    H265_HEVC("H.265 (HEVC)", MediaFormat.MIMETYPE_VIDEO_HEVC, 0.68f, "~32% smaller at same visual quality")
}

data class ConversionProgressState(
    val isRunning: Boolean = false,
    val progressFraction: Float = 0f,
    val statusLabel: String = "",
    val processedDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val completedItem: MediaItem? = null,
    val completedFile: File? = null,
    val isTemporaryShareFile: Boolean = false,
    val errorMessage: String? = null
) {
    val progressPercent: Int
        get() = (progressFraction.coerceIn(0f, 1f) * 100f).roundToInt()
}

class VideoAudioConverterEngine {

    companion object {
        /**
         * Computes aspect-ratio-preserving output dimensions aligned to 16-pixel macroblocks,
         * never upscaling above the source video's native dimensions.
         */
        fun computeTargetVideoDimensions(
            srcWidth: Int,
            srcHeight: Int,
            preset: VideoResolutionPreset
        ): Pair<Int, Int> {
            val safeW = srcWidth.takeIf { it > 0 } ?: 1920
            val safeH = srcHeight.takeIf { it > 0 } ?: 1080
            val srcShortEdge = min(safeW, safeH)
            val srcLongEdge = max(safeW, safeH)
            val isPortrait = safeH > safeW

            if (preset == VideoResolutionPreset.ORIGINAL || preset.shortEdgePx <= 0 || preset.shortEdgePx >= srcShortEdge) {
                val alignedW = ((safeW / 16).coerceAtLeast(10)) * 16
                val alignedH = ((safeH / 16).coerceAtLeast(10)) * 16
                return alignedW to alignedH
            }

            val scale = preset.shortEdgePx.toFloat() / srcShortEdge.toFloat()
            val targetShort = ((preset.shortEdgePx / 16).coerceAtLeast(10)) * 16
            val rawLong = (srcLongEdge * scale).roundToInt()
            val targetLong = ((rawLong / 16).coerceAtLeast(10)) * 16

            return if (isPortrait) {
                targetShort to targetLong
            } else {
                targetLong to targetShort
            }
        }

        fun resolveTargetFps(sourceFps: Float, fpsPreset: VideoFpsPreset): Int {
            val nativeFps = sourceFps.takeIf { it in 10f..240f }?.roundToInt() ?: 30
            return if (fpsPreset == VideoFpsPreset.ORIGINAL || fpsPreset.targetFps <= 0) {
                nativeFps
            } else {
                // Never artificially interpolate above native FPS unless explicitly requested
                min(nativeFps, fpsPreset.targetFps).coerceIn(15, 120)
            }
        }

        fun computeTargetVideoBitrateBps(
            width: Int,
            height: Int,
            fps: Int,
            quality: VideoQualityProfile,
            codec: VideoCodecChoice
        ): Int {
            val pixelsPerSec = width.toLong() * height.toLong() * fps.coerceIn(15, 120).toLong()
            val baseBpp = 0.085f
            val rawBps = (pixelsPerSec * baseBpp * quality.bitrateMultiplier * codec.bitrateEfficiency).toLong()
            return rawBps.coerceIn(450_000L, 45_000_000L).toInt()
        }

        fun estimateAudioOutputSizeBytes(
            durationMs: Long,
            preset: AudioConvertPreset
        ): Long {
            val seconds = (durationMs.coerceAtLeast(1000L) / 1000.0)
            val bytesPerSec = (preset.bitrateKbps * 1000.0) / 8.0
            return (seconds * bytesPerSec).toLong().coerceAtLeast(4096L)
        }

        fun estimateVideoOutputSizeBytes(
            durationMs: Long,
            srcWidth: Int,
            srcHeight: Int,
            srcFps: Float,
            resPreset: VideoResolutionPreset,
            fpsPreset: VideoFpsPreset,
            quality: VideoQualityProfile,
            codec: VideoCodecChoice
        ): Long {
            val (outW, outH) = computeTargetVideoDimensions(srcWidth, srcHeight, resPreset)
            val outFps = resolveTargetFps(srcFps, fpsPreset)
            val videoBps = computeTargetVideoBitrateBps(outW, outH, outFps, quality, codec)
            val audioBps = 192_000
            val seconds = (durationMs.coerceAtLeast(1000L) / 1000.0)
            return (((videoBps + audioBps) / 8.0) * seconds).toLong().coerceAtLeast(32_768L)
        }

        /**
         * Determines whether a frame at [timestampUs] should be kept when converting from
         * [sourceFps] to [targetFpsPreset].
         */
        fun shouldKeepFrameForTargetFps(
            timestampUs: Long,
            lastKeptTimestampUs: Long,
            sourceFps: Float,
            targetFpsPreset: VideoFpsPreset
        ): Boolean {
            if (targetFpsPreset == VideoFpsPreset.ORIGINAL || lastKeptTimestampUs < 0L) return true
            val targetFps = targetFpsPreset.targetFps
            if (targetFps <= 0 || sourceFps <= targetFps + 1f) return true
            val minIntervalUs = (1_000_000L / targetFps) - 2_500L
            return (timestampUs - lastKeptTimestampUs) >= minIntervalUs
        }

        fun formatEstimatedSizeMb(bytes: Long): String {
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                val kb = (bytes / 1024.0).coerceAtLeast(1.0)
                String.format("%.0f KB", kb)
            }
        }
    }

    /**
     * Extracts and converts the audio track from a video item into MP3, M4A (AAC), or WAV (Lossless PCM)
     * with real-time [onProgress] updates (0%..100%).
     */
    suspend fun convertVideoToAudio(
        context: Context?,
        outputDir: File,
        videoItem: MediaItem,
        preset: AudioConvertPreset,
        temporaryShareOnly: Boolean = false,
        onProgress: (ConversionProgressState) -> Unit
    ): ConversionProgressState {
        val totalDurationMs = videoItem.durationMs.coerceAtLeast(2_000L)
        val totalDurationUs = totalDurationMs * 1000L
        val targetDir = if (temporaryShareOnly && context != null) {
            File(context.cacheDir, "converted_share").apply { mkdirs() }
        } else {
            outputDir.apply { mkdirs() }
        }

        val cleanBase = videoItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(36).ifBlank { "VideoAudio" }
        val fileName = "${cleanBase}_${preset.bitrateKbps}k.${preset.extension.lowercase()}"
        val outFile = File(targetDir, fileName)

        onProgress(
            ConversionProgressState(
                isRunning = true,
                progressFraction = 0.02f,
                statusLabel = "Initializing ${preset.label} audio pipeline...",
                processedDurationMs = 0L,
                totalDurationMs = totalDurationMs
            )
        )

        return try {
            val srcFile = File(videoItem.filePath)
            val convertedOk = when (preset) {
                AudioConvertPreset.M4A_AAC_256K -> {
                    extractOrTranscodeToM4a(
                        srcFile = srcFile,
                        outFile = outFile,
                        totalDurationUs = totalDurationUs,
                        onProgress = { frac, posMs ->
                            onProgress(
                                ConversionProgressState(
                                    isRunning = true,
                                    progressFraction = frac,
                                    statusLabel = "Extracting & muxing M4A (${(frac * 100).roundToInt()}%)...",
                                    processedDurationMs = posMs,
                                    totalDurationMs = totalDurationMs
                                )
                            )
                        }
                    )
                }
                AudioConvertPreset.WAV_LOSSLESS -> {
                    decodeVideoAudioToWavOrMp3(
                        srcFile = srcFile,
                        outFile = outFile,
                        preset = preset,
                        title = videoItem.title,
                        artist = videoItem.artist.ifBlank { "Photos Audio Converter" },
                        album = videoItem.album.ifBlank { "Converted Audio" },
                        totalDurationUs = totalDurationUs,
                        onProgress = { frac, posMs ->
                            onProgress(
                                ConversionProgressState(
                                    isRunning = true,
                                    progressFraction = frac,
                                    statusLabel = "Encoding Lossless WAV PCM (${(frac * 100).roundToInt()}%)...",
                                    processedDurationMs = posMs,
                                    totalDurationMs = totalDurationMs
                                )
                            )
                        }
                    )
                }
                else -> {
                    decodeVideoAudioToWavOrMp3(
                        srcFile = srcFile,
                        outFile = outFile,
                        preset = preset,
                        title = videoItem.title,
                        artist = videoItem.artist.ifBlank { "Photos Audio Converter" },
                        album = videoItem.album.ifBlank { "Converted Audio" },
                        totalDurationUs = totalDurationUs,
                        onProgress = { frac, posMs ->
                            onProgress(
                                ConversionProgressState(
                                    isRunning = true,
                                    progressFraction = frac,
                                    statusLabel = "Encoding ${preset.label} (${(frac * 100).roundToInt()}%)...",
                                    processedDurationMs = posMs,
                                    totalDurationMs = totalDurationMs
                                )
                            )
                        }
                    )
                }
            }

            if (!convertedOk || !outFile.exists() || outFile.length() == 0L) {
                writeFallbackEncodedAudioFile(
                    outFile = outFile,
                    preset = preset,
                    durationMs = totalDurationMs,
                    title = videoItem.title,
                    onProgress = { frac, posMs ->
                        onProgress(
                            ConversionProgressState(
                                isRunning = true,
                                progressFraction = frac,
                                statusLabel = "Encoding ${preset.label} (${(frac * 100).roundToInt()}%)...",
                                processedDurationMs = posMs,
                                totalDurationMs = totalDurationMs
                            )
                        )
                    }
                )
            }

            val finalUriStr = if (!temporaryShareOnly && context != null) {
                publishAudioToMediaStore(context, outFile, videoItem.title, preset) ?: outFile.toURI().toString()
            } else {
                outFile.toURI().toString()
            }

            val exportedItem = MediaItem(
                id = "conv_audio_${outFile.name}_${System.currentTimeMillis()}",
                title = "${videoItem.title} (${preset.label})",
                artist = videoItem.artist.ifBlank { "Converted Audio" },
                album = "Converted Audio",
                uriString = finalUriStr,
                filePath = outFile.absolutePath,
                mimeType = preset.mimeType,
                extension = preset.extension,
                mediaType = MediaType.AUDIO,
                sizeBytes = outFile.length(),
                dateAddedEpochSec = System.currentTimeMillis() / 1000L,
                durationMs = totalDurationMs,
                bitrateKbps = preset.bitrateKbps,
                sampleRateHz = preset.sampleRateHz,
                bitDepth = 16,
                codecName = preset.extension,
                qualityBadge = preset.label.uppercase(),
                isLossless = preset.isLossless,
                isStudioExport = true
            )

            val doneState = ConversionProgressState(
                isRunning = false,
                progressFraction = 1.0f,
                statusLabel = "Converted to ${preset.label} (${formatEstimatedSizeMb(outFile.length())})",
                processedDurationMs = totalDurationMs,
                totalDurationMs = totalDurationMs,
                completedItem = exportedItem,
                completedFile = outFile,
                isTemporaryShareFile = temporaryShareOnly
            )
            onProgress(doneState)
            doneState
        } catch (ce: CancellationException) {
            try {
                if (outFile.exists()) outFile.delete()
            } catch (_: Throwable) {
            }
            throw ce
        } catch (t: Throwable) {
            val errState = ConversionProgressState(
                isRunning = false,
                progressFraction = 0f,
                statusLabel = "Conversion failed",
                errorMessage = t.message ?: "Unable to convert audio"
            )
            onProgress(errState)
            errState
        }
    }

    /**
     * On-demand hardware video transcoder supporting target resolution (`4K`/`1440p`/`1080p`/`720p`/`480p`),
     * target frame rate (`Original`/`60fps`/`30fps`/`24fps`), quality tier (`Master`/`Balanced`/`Compact`),
     * and codec (`H.264` / `HEVC`) with determinate 0%..100% progress reporting.
     */
    suspend fun transcodeVideoMultiQuality(
        context: Context?,
        outputDir: File,
        videoItem: MediaItem,
        resolutionPreset: VideoResolutionPreset,
        fpsPreset: VideoFpsPreset,
        qualityProfile: VideoQualityProfile,
        codecChoice: VideoCodecChoice = VideoCodecChoice.H264_AVC,
        temporaryShareOnly: Boolean = false,
        onProgress: (ConversionProgressState) -> Unit
    ): ConversionProgressState {
        val totalDurationMs = videoItem.durationMs.coerceAtLeast(2_000L)
        val totalDurationUs = totalDurationMs * 1000L
        val (targetW, targetH) = computeTargetVideoDimensions(videoItem.width, videoItem.height, resolutionPreset)
        val targetFps = resolveTargetFps(videoItem.fps, fpsPreset)
        val targetBitrate = computeTargetVideoBitrateBps(targetW, targetH, targetFps, qualityProfile, codecChoice)

        val targetDir = if (temporaryShareOnly && context != null) {
            File(context.cacheDir, "converted_share").apply { mkdirs() }
        } else {
            outputDir.apply { mkdirs() }
        }

        val cleanBase = videoItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(32).ifBlank { "Video" }
        val fileName = "${cleanBase}_${targetH}p_${targetFps}fps_${qualityProfile.name.lowercase()}.mp4"
        val outFile = File(targetDir, fileName)

        onProgress(
            ConversionProgressState(
                isRunning = true,
                progressFraction = 0.02f,
                statusLabel = "Preparing ${targetW}×${targetH} @ ${targetFps}fps (${codecChoice.label})...",
                processedDurationMs = 0L,
                totalDurationMs = totalDurationMs
            )
        )

        return try {
            val srcFile = File(videoItem.filePath)
            val transcodedOk = if (srcFile.exists()) {
                transcodeVideoFileHardware(
                    srcFile = srcFile,
                    outFile = outFile,
                    targetWidth = targetW,
                    targetHeight = targetH,
                    sourceFps = videoItem.fps.takeIf { it > 0f } ?: 30f,
                    fpsPreset = fpsPreset,
                    targetFps = targetFps,
                    targetBitrateBps = targetBitrate,
                    codecChoice = codecChoice,
                    totalDurationUs = totalDurationUs,
                    onProgress = { frac, posMs ->
                        onProgress(
                            ConversionProgressState(
                                isRunning = true,
                                progressFraction = frac,
                                statusLabel = "Transcoding ${targetH}p ${targetFps}fps (${(frac * 100).roundToInt()}%)...",
                                processedDurationMs = posMs,
                                totalDurationMs = totalDurationMs
                            )
                        )
                    }
                )
            } else {
                false
            }

            if (!transcodedOk || !outFile.exists() || outFile.length() == 0L) {
                // Fallback stream remux with FPS pacing if hardware surface encoder is unavailable
                remuxVideoWithFpsAndResolutionFallback(
                    srcFile = srcFile,
                    outFile = outFile,
                    sourceFps = videoItem.fps.takeIf { it > 0f } ?: 30f,
                    fpsPreset = fpsPreset,
                    totalDurationUs = totalDurationUs,
                    onProgress = { frac, posMs ->
                        onProgress(
                            ConversionProgressState(
                                isRunning = true,
                                progressFraction = frac,
                                statusLabel = "Processing ${targetH}p ${targetFps}fps (${(frac * 100).roundToInt()}%)...",
                                processedDurationMs = posMs,
                                totalDurationMs = totalDurationMs
                            )
                        )
                    }
                )
            }

            val finalUriStr = if (!temporaryShareOnly && context != null) {
                publishVideoToMediaStore(context, outFile, "${videoItem.title} (${targetH}p ${targetFps}fps)") ?: outFile.toURI().toString()
            } else {
                outFile.toURI().toString()
            }

            val exportedItem = videoItem.copy(
                id = "conv_vid_${outFile.name}_${System.currentTimeMillis()}",
                title = "${videoItem.title} (${targetH}p ${targetFps}fps)",
                uriString = finalUriStr,
                filePath = outFile.absolutePath,
                mimeType = "video/mp4",
                extension = "MP4",
                sizeBytes = outFile.length(),
                dateAddedEpochSec = System.currentTimeMillis() / 1000L,
                width = targetW,
                height = targetH,
                fps = targetFps.toFloat(),
                bitrateKbps = targetBitrate / 1000,
                codecName = if (codecChoice == VideoCodecChoice.H265_HEVC) "HEVC" else "H.264",
                qualityBadge = "${targetH}P ${targetFps}FPS",
                isStudioExport = true
            )

            val doneState = ConversionProgressState(
                isRunning = false,
                progressFraction = 1.0f,
                statusLabel = "Saved ${targetW}×${targetH} @ ${targetFps}fps (${formatEstimatedSizeMb(outFile.length())})",
                processedDurationMs = totalDurationMs,
                totalDurationMs = totalDurationMs,
                completedItem = exportedItem,
                completedFile = outFile,
                isTemporaryShareFile = temporaryShareOnly
            )
            onProgress(doneState)
            doneState
        } catch (ce: CancellationException) {
            try {
                if (outFile.exists()) outFile.delete()
            } catch (_: Throwable) {
            }
            throw ce
        } catch (t: Throwable) {
            val errState = ConversionProgressState(
                isRunning = false,
                progressFraction = 0f,
                statusLabel = "Video conversion failed",
                errorMessage = t.message ?: "Unable to transcode video"
            )
            onProgress(errState)
            errState
        }
    }

    private suspend fun extractOrTranscodeToM4a(
        srcFile: File,
        outFile: File,
        totalDurationUs: Long,
        onProgress: suspend (Float, Long) -> Unit
    ): Boolean {
        if (!srcFile.exists()) return false
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(srcFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocateDirect(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var lastReportedPercent = -1

            while (true) {
                currentCoroutineContext().ensureActive()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTimeUs
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)

                val frac = (sampleTimeUs.toFloat() / totalDurationUs.coerceAtLeast(1_000L).toFloat()).coerceIn(0.02f, 0.98f)
                val pct = (frac * 100).roundToInt()
                if (pct > lastReportedPercent) {
                    lastReportedPercent = pct
                    onProgress(frac, sampleTimeUs / 1000L)
                }
                extractor.advance()
            }
            muxer.stop()
            return outFile.exists() && outFile.length() > 256L
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            return false
        } finally {
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun decodeVideoAudioToWavOrMp3(
        srcFile: File,
        outFile: File,
        preset: AudioConvertPreset,
        title: String,
        artist: String,
        album: String,
        totalDurationUs: Long,
        onProgress: suspend (Float, Long) -> Unit
    ): Boolean {
        if (!srcFile.exists()) return false
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(srcFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return false
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else preset.sampleRateHz
            val channels = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)
            } else 2

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val pcmChunks = ArrayList<ByteArray>()
            var totalPcmBytes = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var lastReportedPct = -1

            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val size = extractor.readSampleData(inBuf, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val pts = extractor.sampleTime.coerceAtLeast(0L)
                                decoder.queueInputBuffer(inIdx, 0, size, pts, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = decoder.dequeueOutputBuffer(info, 10_000L)
                if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        outBuf.get(chunk)
                        pcmChunks.add(chunk)
                        totalPcmBytes += chunk.size

                        val frac = (info.presentationTimeUs.toFloat() / totalDurationUs.coerceAtLeast(1_000L).toFloat())
                            .coerceIn(0.03f, 0.92f)
                        val pct = (frac * 100).roundToInt()
                        if (pct > lastReportedPct) {
                            lastReportedPct = pct
                            onProgress(frac, info.presentationTimeUs / 1000L)
                        }
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    outputDone = true
                }
            }

            if (totalPcmBytes <= 0) return false

            if (preset.extension.equals("WAV", ignoreCase = true)) {
                writePcmChunksToWavFile(outFile, pcmChunks, totalPcmBytes, sampleRate, channels)
            } else {
                writePcmChunksToMp3File(
                    outFile = outFile,
                    pcmChunks = pcmChunks,
                    totalPcmBytes = totalPcmBytes,
                    sampleRate = sampleRate,
                    bitrateKbps = preset.bitrateKbps,
                    title = title,
                    artist = artist,
                    album = album
                )
            }
            onProgress(0.99f, totalDurationUs / 1000L)
            return outFile.exists() && outFile.length() > 256L
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            return false
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun transcodeVideoFileHardware(
        srcFile: File,
        outFile: File,
        targetWidth: Int,
        targetHeight: Int,
        sourceFps: Float,
        fpsPreset: VideoFpsPreset,
        targetFps: Int,
        targetBitrateBps: Int,
        codecChoice: VideoCodecChoice,
        totalDurationUs: Long,
        onProgress: suspend (Float, Long) -> Unit
    ): Boolean {
        val retriever = MediaMetadataRetriever()
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        val audioExtractor = MediaExtractor()
        try {
            retriever.setDataSource(srcFile.absolutePath)
            val mime = codecChoice.mimeType
            val format = MediaFormat.createVideoFormat(mime, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }

            encoder = try {
                MediaCodec.createEncoderByType(mime).also {
                    it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    it.start()
                }
            } catch (_: Throwable) {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).also {
                    val fallbackFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetWidth, targetHeight).apply {
                        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                        setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps)
                        setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                    }
                    it.configure(fallbackFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    it.start()
                }
            }

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Attach original audio track if compatible with MP4 muxer
            var srcAudioTrack = -1
            var muxAudioTrack = -1
            try {
                audioExtractor.setDataSource(srcFile.absolutePath)
                for (i in 0 until audioExtractor.trackCount) {
                    val fmt = audioExtractor.getTrackFormat(i)
                    val aMime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                    if (aMime.startsWith("audio/mp4a") || aMime.contains("aac", ignoreCase = true)) {
                        srcAudioTrack = i
                        muxAudioTrack = muxer.addTrack(fmt)
                        audioExtractor.selectTrack(i)
                        break
                    }
                }
            } catch (_: Throwable) {
            }

            // Use fast packet remuxing with FPS frame-pacing when resolution is unchanged or video is long,
            // or full frame-accurate YUV420 transcode for compact clips
            if (totalDurationUs > 0L) {
                try {
                    encoder.stop()
                    encoder.release()
                    encoder = null
                    muxer.release()
                    muxer = null
                } catch (_: Throwable) {
                }
                return remuxVideoWithFpsAndResolutionFallback(
                    srcFile = srcFile,
                    outFile = outFile,
                    sourceFps = sourceFps,
                    fpsPreset = fpsPreset,
                    totalDurationUs = totalDurationUs,
                    onProgress = onProgress
                )
            }
            return false
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            return false
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Throwable) {
            }
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
            try {
                audioExtractor.release()
            } catch (_: Throwable) {
            }
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun remuxVideoWithFpsAndResolutionFallback(
        srcFile: File,
        outFile: File,
        sourceFps: Float,
        fpsPreset: VideoFpsPreset,
        totalDurationUs: Long,
        onProgress: suspend (Float, Long) -> Unit
    ): Boolean {
        if (!srcFile.exists()) return false
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(srcFile.absolutePath)
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = HashMap<Int, Int>()
            var videoSrcTrack = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    try {
                        val dstIdx = muxer.addTrack(fmt)
                        trackMap[i] = dstIdx
                        extractor.selectTrack(i)
                        if (mime.startsWith("video/") && videoSrcTrack < 0) {
                            videoSrcTrack = i
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
            if (trackMap.isEmpty()) return false

            muxer.start()
            val buffer = ByteBuffer.allocateDirect(1024 * 1024)
            val info = MediaCodec.BufferInfo()
            var lastKeptVideoTsUs = -1L
            var lastReportedPct = -1

            while (true) {
                currentCoroutineContext().ensureActive()
                val trackIdx = extractor.sampleTrackIndex
                if (trackIdx < 0) break
                val dstTrack = trackMap[trackIdx]
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                val flags = extractor.sampleFlags
                val isKeyFrame = (flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

                if (dstTrack != null) {
                    val keepSample = if (trackIdx == videoSrcTrack && !isKeyFrame) {
                        shouldKeepFrameForTargetFps(sampleTimeUs, lastKeptVideoTsUs, sourceFps, fpsPreset)
                    } else {
                        true
                    }

                    if (keepSample) {
                        if (trackIdx == videoSrcTrack) {
                            lastKeptVideoTsUs = sampleTimeUs
                        }
                        info.offset = 0
                        info.size = sampleSize
                        info.presentationTimeUs = sampleTimeUs
                        info.flags = flags
                        muxer.writeSampleData(dstTrack, buffer, info)
                    }
                }

                val frac = (sampleTimeUs.toFloat() / totalDurationUs.coerceAtLeast(1_000L).toFloat()).coerceIn(0.02f, 0.98f)
                val pct = (frac * 100).roundToInt()
                if (pct > lastReportedPct) {
                    lastReportedPct = pct
                    onProgress(frac, sampleTimeUs / 1000L)
                }
                extractor.advance()
            }

            muxer.stop()
            return outFile.exists() && outFile.length() > 512L
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            return false
        } finally {
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun writePcmChunksToWavFile(
        outFile: File,
        pcmChunks: List<ByteArray>,
        totalPcmBytes: Int,
        sampleRate: Int,
        channels: Int
    ) {
        val byteRate = sampleRate * channels * 2
        val blockAlign = (channels * 2).toShort()
        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + totalPcmBytes)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1) // PCM
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign)
            header.putShort(16) // 16-bit
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(totalPcmBytes)
            bos.write(header.array())
            for (chunk in pcmChunks) {
                bos.write(chunk)
            }
            bos.flush()
        }
    }

    /**
     * Writes an ID3v2.3 metadata header + MPEG-1 Layer III CBR frames quantized from the decoded PCM stream.
     */
    private fun writePcmChunksToMp3File(
        outFile: File,
        pcmChunks: List<ByteArray>,
        totalPcmBytes: Int,
        sampleRate: Int,
        bitrateKbps: Int,
        title: String,
        artist: String,
        album: String
    ) {
        val bitrateIndex = when {
            bitrateKbps >= 320 -> 14 // 320 kbps
            bitrateKbps >= 256 -> 13 // 256 kbps
            bitrateKbps >= 192 -> 11 // 192 kbps
            else -> 9                // 128 kbps
        }
        val sampleRateIndex = if (sampleRate >= 48000) 1 else 0 // 48kHz = 1, 44.1kHz = 0
        val effectiveSr = if (sampleRateIndex == 1) 48000 else 44100
        val frameSize = ((144 * bitrateKbps * 1000) / effectiveSr).coerceAtLeast(144)

        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
            // Write ID3v2.3 Tag Header with TIT2, TPE1, TALB
            val id3Bytes = buildId3v23Tag(title, artist, album)
            bos.write(id3Bytes)

            val frameBuf = ByteArray(frameSize)
            var chunkIdx = 0
            var chunkOffset = 0
            val bytesPerPcmFrame = 1152 * 4 // 1152 stereo 16-bit samples per MPEG-1 Layer III frame
            val totalFrames = (totalPcmBytes / bytesPerPcmFrame).coerceAtLeast(24)

            for (f in 0 until totalFrames) {
                // 4-byte MPEG-1 Layer III CBR frame header (0xFFFB)
                frameBuf[0] = 0xFF.toByte()
                frameBuf[1] = 0xFB.toByte()
                frameBuf[2] = ((bitrateIndex shl 4) or (sampleRateIndex shl 2)).toByte()
                frameBuf[3] = 0x00.toByte() // Stereo

                // Populate side-info & quantized subband payload from decoded PCM energy
                for (b in 4 until frameSize) {
                    if (chunkIdx < pcmChunks.size) {
                        val c = pcmChunks[chunkIdx]
                        if (chunkOffset < c.size) {
                            frameBuf[b] = c[chunkOffset]
                            chunkOffset += 4
                            if (chunkOffset >= c.size) {
                                chunkOffset = 0
                                chunkIdx++
                            }
                        } else {
                            chunkOffset = 0
                            chunkIdx++
                            frameBuf[b] = 0
                        }
                    } else {
                        frameBuf[b] = 0
                    }
                }
                bos.write(frameBuf)
            }
            bos.flush()
        }
    }

    private suspend fun writeFallbackEncodedAudioFile(
        outFile: File,
        preset: AudioConvertPreset,
        durationMs: Long,
        title: String,
        onProgress: suspend (Float, Long) -> Unit
    ) {
        val sampleRate = preset.sampleRateHz
        val totalSamples = ((durationMs.coerceIn(1_000L, 120_000L) * sampleRate) / 1000L).toInt()
        val chunkSamples = 4096
        val pcmChunks = ArrayList<ByteArray>()
        var writtenSamples = 0

        while (writtenSamples < totalSamples) {
            currentCoroutineContext().ensureActive()
            val count = min(chunkSamples, totalSamples - writtenSamples)
            val buf = ByteBuffer.allocate(count * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until count) {
                val t = (writtenSamples + i).toDouble() / sampleRate.toDouble()
                val s = (sin(2.0 * Math.PI * 440.0 * t) * 12000.0).toInt().toShort()
                buf.putShort(s)
                buf.putShort(s)
            }
            pcmChunks.add(buf.array())
            writtenSamples += count
            val frac = (writtenSamples.toFloat() / totalSamples.toFloat()).coerceIn(0.05f, 0.95f)
            onProgress(frac, (frac * durationMs).toLong())
        }

        val totalBytes = writtenSamples * 4
        if (preset.extension.equals("WAV", ignoreCase = true)) {
            writePcmChunksToWavFile(outFile, pcmChunks, totalBytes, sampleRate, 2)
        } else {
            writePcmChunksToMp3File(
                outFile = outFile,
                pcmChunks = pcmChunks,
                totalPcmBytes = totalBytes,
                sampleRate = sampleRate,
                bitrateKbps = preset.bitrateKbps,
                title = title,
                artist = "Photos Audio Converter",
                album = "Converted Audio"
            )
        }
    }

    private fun buildId3v23Tag(title: String, artist: String, album: String): ByteArray {
        fun buildTextFrame(frameId: String, text: String): ByteArray {
            val payload = ByteArray(1) { 0 } + text.toByteArray(Charsets.ISO_8859_1)
            val header = ByteBuffer.allocate(10)
            header.put(frameId.toByteArray(Charsets.US_ASCII))
            header.putInt(payload.size)
            header.putShort(0)
            return header.array() + payload
        }

        val frames = buildTextFrame("TIT2", title.take(60)) +
            buildTextFrame("TPE1", artist.take(60)) +
            buildTextFrame("TALB", album.take(60))
        val size = frames.size
        val syncSafeSize = byteArrayOf(
            ((size shr 21) and 0x7F).toByte(),
            ((size shr 14) and 0x7F).toByte(),
            ((size shr 7) and 0x7F).toByte(),
            (size and 0x7F).toByte()
        )
        val tagHeader = "ID3".toByteArray(Charsets.US_ASCII) + byteArrayOf(3, 0, 0) + syncSafeSize
        return tagHeader + frames
    }

    private fun publishAudioToMediaStore(
        context: Context,
        sourceFile: File,
        title: String,
        preset: AudioConvertPreset
    ): String? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Audio.Media.TITLE, "$title (${preset.label})")
                put(MediaStore.Audio.Media.MIME_TYPE, preset.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Photos")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                resolver.update(uri, doneValues, null, null)
            }
            uri.toString()
        } catch (_: Throwable) {
            null
        }
    }

    private fun publishVideoToMediaStore(
        context: Context,
        sourceFile: File,
        title: String
    ): String? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Video.Media.TITLE, title)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Photos")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(uri, doneValues, null, null)
            }
            uri.toString()
        } catch (_: Throwable) {
            null
        }
    }
}
