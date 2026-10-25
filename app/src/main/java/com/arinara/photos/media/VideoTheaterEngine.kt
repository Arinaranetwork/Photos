// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

data class SubtitleCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val primaryText: String,
    val secondaryText: String? = null
)

data class VideoChapter(
    val index: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long
)

data class FolderSkipRule(
    val albumOrSeries: String,
    val skipIntroDurationMs: Long = 85_000L,
    val autoSkipIntro: Boolean = false,
    val autoPlayNextEpisode: Boolean = true
)

data class VideoSceneBookmark(
    val id: String,
    val videoId: String,
    val timestampMs: Long,
    val caption: String
)

enum class VideoAspectMode(val label: String) {
    FIT("Original"),
    FILL_CROP("Zoom Crop"),
    RATIO_16_9("16:9"),
    RATIO_21_9("21:9 Cinema"),
    RATIO_4_3("4:3")
}

class VideoTheaterEngine {

    private val folderSkipRules = mutableMapOf<String, FolderSkipRule>()
    private val sceneBookmarks = mutableListOf<VideoSceneBookmark>()

    fun getContinueWatchingItems(allMedia: List<MediaItem>): List<MediaItem> {
        return allMedia
            .filter {
                it.mediaType == MediaType.VIDEO &&
                    !it.isTrashed &&
                    !it.isVaulted &&
                    it.resumePositionMs > 0L &&
                    (it.durationMs <= 0L || it.resumeProgressFraction < 0.96f)
            }
            .sortedByDescending { it.dateAddedEpochSec }
    }

    fun getSeriesEpisodes(allMedia: List<MediaItem>, currentVideo: MediaItem): List<MediaItem> {
        val seriesKey = currentVideo.seriesTitle ?: currentVideo.album
        return allMedia
            .filter {
                it.mediaType == MediaType.VIDEO &&
                    !it.isTrashed &&
                    ((it.seriesTitle != null && it.seriesTitle.equals(seriesKey, ignoreCase = true)) ||
                        it.album.equals(currentVideo.album, ignoreCase = true))
            }
            .sortedWith(
                compareBy<MediaItem> { it.seasonNumber ?: 1 }
                    .thenBy { it.episodeNumber ?: Int.MAX_VALUE }
                    .thenBy { it.title }
            )
    }

    fun findNextEpisode(allMedia: List<MediaItem>, currentVideo: MediaItem): MediaItem? {
        val episodes = getSeriesEpisodes(allMedia, currentVideo)
        val idx = episodes.indexOfFirst { it.id == currentVideo.id }
        return if (idx >= 0 && idx + 1 < episodes.size) episodes[idx + 1] else null
    }

    fun setFolderSkipRule(rule: FolderSkipRule) {
        folderSkipRules[rule.albumOrSeries.lowercase()] = rule
    }

    fun getFolderSkipRule(video: MediaItem): FolderSkipRule {
        val key = (video.seriesTitle ?: video.album).lowercase()
        return folderSkipRules[key] ?: FolderSkipRule(albumOrSeries = video.seriesTitle ?: video.album)
    }

    fun computeSkipIntroTargetMs(currentPositionMs: Long, durationMs: Long, skipDeltaMs: Long = 85_000L): Long {
        val safeDur = durationMs.coerceAtLeast(1_000L)
        // If testing on short sample clips (< 90s), skip 25% of duration or 4s so the button is immediately verifiable
        val effectiveDelta = if (safeDur <= 90_000L) minOf(skipDeltaMs, (safeDur * 0.25f).toLong().coerceAtLeast(3_000L)) else skipDeltaMs
        return (currentPositionMs + effectiveDelta).coerceAtMost((safeDur - 500L).coerceAtLeast(0L))
    }

    fun defaultChaptersForVideo(durationMs: Long): List<VideoChapter> {
        val dur = durationMs.coerceAtLeast(12_000L)
        val q1 = dur / 4L
        val q2 = (dur * 2L) / 4L
        val q3 = (dur * 3L) / 4L
        return listOf(
            VideoChapter(1, "I. Opening & Credits", 0L, q1),
            VideoChapter(2, "II. Rising Action", q1, q2),
            VideoChapter(3, "III. Climax Sequence", q2, q3),
            VideoChapter(4, "IV. Resolution & Outro", q3, dur)
        )
    }

    fun addSceneBookmark(videoId: String, timestampMs: Long, caption: String): VideoSceneBookmark {
        val bm = VideoSceneBookmark(
            id = "vbm_${System.currentTimeMillis()}_${sceneBookmarks.size}",
            videoId = videoId,
            timestampMs = timestampMs.coerceAtLeast(0L),
            caption = caption.ifBlank { "Scene @ ${MediaItem.formatDurationMillis(timestampMs)}" }
        )
        sceneBookmarks.add(bm)
        return bm
    }

    fun getSceneBookmarks(videoId: String): List<VideoSceneBookmark> =
        sceneBookmarks.filter { it.videoId == videoId }.sortedBy { it.timestampMs }

    fun generateStoryboardContactSheet(
        context: Context,
        videoItem: MediaItem,
        rows: Int = 4,
        cols: Int = 4
    ): MediaItem {
        val exportDir = File(context.filesDir, "studio_exports").apply { mkdirs() }
        val cleanName = videoItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_")
        val outFile = File(exportDir, "${cleanName}_Storyboard_${rows}x${cols}.png")

        val width = 1600
        val height = 1200
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Header Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 34f
            isFakeBoldText = true
        }
        canvas.drawText("STORYBOARD CONTACT SHEET: ${videoItem.title}", 40f, 52f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 22f
        }
        canvas.drawText(
            "${videoItem.resolutionLabel} • ${videoItem.codecName} • ${videoItem.formattedDuration} • ${videoItem.formattedSize}",
            40f,
            88f,
            subPaint
        )

        val topMargin = 112f
        val pad = 16f
        val cellW = (width - pad * (cols + 1)) / cols
        val cellH = (height - topMargin - pad * (rows + 1)) / rows
        val totalTiles = rows * cols
        val dur = videoItem.durationMs.coerceAtLeast(16_000L)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF7A5AF8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 20f
            isFakeBoldText = true
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val x = pad + c * (cellW + pad)
                val y = topMargin + pad + r * (cellH + pad)
                val tsMs = (dur * idx) / totalTiles.coerceAtLeast(1)

                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        x, y, x + cellW, y + cellH,
                        intArrayOf(0xFF161C26.toInt(), 0xFF7A5AF8.toInt(), 0xFF1F2736.toInt()),
                        floatArrayOf(0f, ((idx + 1).toFloat() / totalTiles), 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(x, y, x + cellW, y + cellH, tilePaint)
                canvas.drawRect(x, y, x + cellW, y + cellH, borderPaint)
                canvas.drawText(
                    "#${idx + 1} • ${MediaItem.formatDurationMillis(tsMs)}",
                    x + 12f,
                    y + cellH - 14f,
                    stampPaint
                )
            }
        }

        FileOutputStream(outFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bmp.recycle()

        return MediaItem(
            id = "export_${outFile.name}",
            title = "${videoItem.title} (${rows}x${cols} Storyboard)",
            artist = "Video Storyboard Studio",
            album = "Studio Exports",
            uriString = outFile.toURI().toString(),
            filePath = outFile.absolutePath,
            mimeType = "image/png",
            extension = "PNG",
            mediaType = MediaType.PHOTO,
            sizeBytes = outFile.length(),
            dateAddedEpochSec = System.currentTimeMillis() / 1000L,
            width = width,
            height = height,
            qualityBadge = "PRO PNG",
            isLossless = true,
            isStudioExport = true
        )
    }

    companion object {
        private val srtTimestampRegex = Regex(
            """(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})"""
        )

        fun parseSrtSubtitles(srtContent: String): List<SubtitleCue> {
            val cues = mutableListOf<SubtitleCue>()
            val blocks = srtContent.trim().split(Regex("""\r?\n\r?\n"""))
            blocks.forEachIndexed { idx, block ->
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.size >= 2) {
                    val timeLineIdx = lines.indexOfFirst { srtTimestampRegex.containsMatchIn(it) }
                    if (timeLineIdx >= 0) {
                        val match = srtTimestampRegex.find(lines[timeLineIdx])!!
                        val startMs = toMillis(
                            match.groupValues[1],
                            match.groupValues[2],
                            match.groupValues[3],
                            match.groupValues[4]
                        )
                        val endMs = toMillis(
                            match.groupValues[5],
                            match.groupValues[6],
                            match.groupValues[7],
                            match.groupValues[8]
                        )
                        val text = lines.drop(timeLineIdx + 1).joinToString(" ")
                            .replace(Regex("""<[^>]+>"""), "")
                            .trim()
                        if (text.isNotEmpty()) {
                            cues.add(
                                SubtitleCue(
                                    index = idx + 1,
                                    startMs = startMs,
                                    endMs = endMs,
                                    primaryText = text
                                )
                            )
                        }
                    }
                }
            }
            return cues.sortedBy { it.startMs }
        }

        fun mergeDualSubtitles(primary: List<SubtitleCue>, secondary: List<SubtitleCue>): List<SubtitleCue> {
            if (secondary.isEmpty()) return primary
            return primary.map { p ->
                val match = secondary.firstOrNull { s ->
                    abs(s.startMs - p.startMs) <= 1200L
                }
                p.copy(secondaryText = match?.primaryText)
            }
        }

        fun findActiveSubtitleCue(
            cues: List<SubtitleCue>,
            positionMs: Long,
            subtitleOffsetMs: Long = 0L
        ): SubtitleCue? {
            val adjusted = (positionMs - subtitleOffsetMs).coerceAtLeast(0L)
            return cues.firstOrNull { adjusted in it.startMs..it.endMs }
        }

        fun searchSubtitleTranscript(cues: List<SubtitleCue>, query: String): List<SubtitleCue> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return cues
            return cues.filter {
                it.primaryText.lowercase().contains(q) ||
                    (it.secondaryText?.lowercase()?.contains(q) == true)
            }
        }

        fun applyBrightnessGestureDelta(currentLevel: Float, deltaFraction: Float): Float {
            // Range: -0.80f (Extra-Dim sub-zero night mode) to +1.00f (100% display brightness)
            return (currentLevel + deltaFraction).coerceIn(-0.80f, 1.00f)
        }

        fun applyVolumeBoostGestureDelta(currentPercent: Int, deltaPercent: Int): Int {
            // Range: 0% to 200% (100..200% engages LoudnessEnhancer preamp boost)
            return (currentPercent + deltaPercent).coerceIn(0, 200)
        }

        fun computeFrameStepTargetMs(currentPositionMs: Long, fps: Float, direction: Int, durationMs: Long): Long {
            val safeFps = if (fps > 1f) fps else 30f
            val frameDurationMs = (1000f / safeFps).roundToInt().toLong().coerceAtLeast(16L)
            return (currentPositionMs + direction * frameDurationMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
        }

        private fun toMillis(hh: String, mm: String, ss: String, ms: String): Long {
            val h = hh.toLongOrNull() ?: 0L
            val m = mm.toLongOrNull() ?: 0L
            val s = ss.toLongOrNull() ?: 0L
            val milli = ms.toLongOrNull() ?: 0L
            return (h * 3_600_000L) + (m * 60_000L) + (s * 1_000L) + milli
        }
    }
}
