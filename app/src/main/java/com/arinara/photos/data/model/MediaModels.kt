// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.data.model

import kotlin.math.roundToInt

enum class MediaType(val displayLabel: String) {
    PHOTO("Photo"),
    RAW_PHOTO("RAW Pro"),
    ANIMATED("Animated"),
    VIDEO("Video"),
    AUDIO("Audio")
}

enum class AudioQualityTier(
    val badge: String,
    val title: String,
    val targetBitrateKbps: Int,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val isLossless: Boolean
) {
    HI_RES_LOSSLESS("HI-RES", "Hi-Res Lossless (24-bit/96kHz)", 4608, 96000, 24, true),
    CD_LOSSLESS("LOSSLESS", "CD Lossless (16-bit/44.1kHz)", 1411, 44100, 16, true),
    HIGH_QUALITY("320k", "High Quality (320 kbps)", 320, 48000, 16, false),
    BALANCED("192k", "Balanced (192 kbps)", 192, 44100, 16, false),
    VOICE_SAVER("96k", "Voice / Storage Saver (96 kbps)", 96, 32000, 16, false)
}

enum class LoopMode(val label: String, val shortBadge: String) {
    OFF("Loop Off", "OFF"),
    QUEUE("Loop Queue", "ALL"),
    TRACK("Loop 1 Track", "1x"),
    AB_SECTION("A-B Section Loop", "A-B");

    fun next(): LoopMode = when (this) {
        OFF -> QUEUE
        QUEUE -> TRACK
        TRACK -> AB_SECTION
        AB_SECTION -> OFF
    }
}

enum class MediaFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    RAW_PRO("RAW"),
    ANIMATED("GIF"),
    VIDEO("Videos"),
    AUDIO_LOSSLESS("Music"),
    FAVORITES("Favorites"),
    RECENT_CUTS("Exports")
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    NAME_ASC("Name"),
    SIZE_DESC("Size"),
    DURATION_DESC("Duration"),
    QUALITY_DESC("Quality")
}

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "Camera",
    val uriString: String,
    val filePath: String,
    val mimeType: String,
    val extension: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val dateAddedEpochSec: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 0f,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val channels: Int = 2,
    val codecName: String = "PCM",
    val qualityBadge: String = "PHOTO",
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
    val isHdr: Boolean = false,
    val isFavorite: Boolean = false,
    val isVaulted: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAtEpochSec: Long = 0L,
    val isStudioExport: Boolean = false,
    val voiceNotePath: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val seriesTitle: String? = null,
    val resumePositionMs: Long = 0L,
    val dominantColorHex: String = "#7A5AF8",
    val cameraMakeModel: String? = null,
    val focalLengthMm: String? = null,
    val apertureFNumber: String? = null,
    val isoSpeed: String? = null,
    val exposureTimeSec: String? = null,
    val hasGpsCoordinates: Boolean = false,
    val albumArtUriString: String? = null
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_073_741_824L -> String.format("%.2f GB", sizeBytes / 1_073_741_824.0)
            sizeBytes >= 1_048_576L -> String.format("%.1f MB", sizeBytes / 1_048_576.0)
            sizeBytes >= 1024L -> "${(sizeBytes / 1024.0).roundToInt()} KB"
            else -> "$sizeBytes B"
        }

    val formattedDuration: String
        get() = formatDurationMillis(durationMs)

    val resolutionLabel: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K UHD (${width}x${height})"
            width >= 2560 || height >= 1440 -> "2K QHD (${width}x${height})"
            width >= 1920 || height >= 1080 -> "1080p FHD (${width}x${height})"
            width > 0 && height > 0 -> "${width}x${height}"
            else -> extension.uppercase()
        }

    val audioSpecSummary: String
        get() {
            val khz = if (sampleRateHz % 1000 == 0) "${sampleRateHz / 1000}kHz" else String.format("%.1fkHz", sampleRateHz / 1000.0)
            return "${codecName.uppercase()} • ${bitDepth}-bit/$khz • ${bitrateKbps}kbps"
        }

    val resumeProgressFraction: Float
        get() = if (durationMs > 0L) (resumePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val remainingMinutesLabel: String
        get() {
            if (durationMs <= 0L) return ""
            val remMs = (durationMs - resumePositionMs).coerceAtLeast(0L)
            val mins = (remMs / 60_000L).toInt()
            val secs = ((remMs % 60_000L) / 1000L).toInt()
            return if (mins > 0) "${mins}m ${secs}s left" else "${secs}s left"
        }

    companion object {
        fun formatDurationMillis(ms: Long): String {
            if (ms <= 0L) return "00:00"
            val totalSeconds = ms / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0L) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        fun classifyExtension(extRaw: String, mimeRaw: String = ""): MediaType {
            val ext = extRaw.lowercase().trimStart('.')
            val mime = mimeRaw.lowercase()
            return when {
                ext in setOf("dng", "cr2", "cr3", "nef", "arw", "raf", "orf", "rw2", "pef") -> MediaType.RAW_PHOTO
                ext in setOf("gif", "apng") || mime == "image/gif" -> MediaType.ANIMATED
                ext in setOf("mp4", "mkv", "webm", "mov", "avi", "ts", "m2ts", "flv", "3gp", "m4v") || mime.startsWith("video/") -> MediaType.VIDEO
                ext in setOf("mp3", "flac", "wav", "alac", "aiff", "m4a", "aac", "ogg", "opus", "wma", "wv", "ape", "dsf", "dff") || mime.startsWith("audio/") -> MediaType.AUDIO
                else -> MediaType.PHOTO
            }
        }

        fun computeQualityBadge(
            mediaType: MediaType,
            extension: String,
            width: Int,
            height: Int,
            fps: Float,
            bitDepth: Int,
            sampleRateHz: Int,
            bitrateKbps: Int
        ): String {
            val ext = extension.uppercase()
            return when (mediaType) {
                MediaType.RAW_PHOTO -> "RAW $ext"
                MediaType.ANIMATED -> "$ext ANIM"
                MediaType.VIDEO -> {
                    val res = when {
                        width >= 3840 || height >= 2160 -> "4K"
                        width >= 2560 || height >= 1440 -> "2K"
                        width >= 1920 || height >= 1080 -> "1080p"
                        width >= 1280 || height >= 720 -> "720p"
                        else -> ext
                    }
                    val fpsTag = if (fps >= 59f) " 60" else ""
                    "$res$fpsTag"
                }
                MediaType.AUDIO -> {
                    val isLosslessExt = ext in setOf("FLAC", "WAV", "ALAC", "AIFF", "WV", "APE", "DSF")
                    when {
                        isLosslessExt && (bitDepth >= 24 || sampleRateHz >= 88200) -> "HI-RES ${bitDepth}B/${sampleRateHz / 1000}K"
                        isLosslessExt -> "LOSSLESS $ext"
                        bitrateKbps >= 300 -> "${bitrateKbps}K HQ"
                        bitrateKbps > 0 -> "${bitrateKbps}K $ext"
                        else -> ext
                    }
                }
                MediaType.PHOTO -> {
                    if (width >= 4000 || height >= 3000) "PRO $ext" else ext
                }
            }
        }
    }
}

data class AlbumBucket(
    val id: String,
    val name: String,
    val folderPath: String,
    val items: List<MediaItem>,
    val isPinned: Boolean = false
) {
    val itemCount: Int get() = items.size
    val totalSizeBytes: Long get() = items.sumOf { it.sizeBytes }
    val coverItem: MediaItem? get() = items.firstOrNull()
    val photoCount: Int get() = items.count { it.mediaType == MediaType.PHOTO || it.mediaType == MediaType.RAW_PHOTO || it.mediaType == MediaType.ANIMATED }
    val videoCount: Int get() = items.count { it.mediaType == MediaType.VIDEO }
    val audioCount: Int get() = items.count { it.mediaType == MediaType.AUDIO }
}

data class CueTrackSlice(
    val trackNumber: Int,
    val title: String,
    val performer: String,
    val startMs: Long,
    val endMs: Long,
    val sourceFilePath: String
)

data class LrcLyricLine(
    val timestampMs: Long,
    val text: String,
    val translationText: String? = null
)

data class AudioBookmark(
    val id: String,
    val mediaId: String,
    val timestampMs: Long,
    val label: String,
    val note: String = ""
)
