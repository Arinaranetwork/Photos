// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.arinara.photos.data.model.AlbumBucket
import com.arinara.photos.data.model.MediaFilter
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import com.arinara.photos.data.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context?) {

    private val _allItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val allItems: StateFlow<List<MediaItem>> = _allItems.asStateFlow()

    private val favoriteIds = mutableSetOf<String>()
    private val vaultedIds = mutableSetOf<String>()
    private val trashedTimestamps = mutableMapOf<String, Long>()
    private val resumePositions = mutableMapOf<String, Long>()
    private val customTagOverrides = mutableMapOf<String, TagOverride>()
    private val voiceNotesByMediaId = mutableMapOf<String, String>()

    data class TagOverride(
        val title: String,
        val artist: String,
        val album: String
    )

    suspend fun refreshMediaLibrary(): List<MediaItem> = withContext(Dispatchers.IO) {
        val loaded = mutableListOf<MediaItem>()
        val ctx = context
        if (ctx != null) {
            val samples = SampleMediaGenerator.ensureSampleLibrary(ctx)
            loaded.addAll(samples)
            loaded.addAll(scanStudioExportsDir(ctx))
            loaded.addAll(queryDeviceMediaStore(ctx))
        }
        val deduplicated = loaded
            .distinctBy { it.id }
            .map { applyStateOverrides(it) }
        _allItems.value = deduplicated
        deduplicated
    }

    fun loadWithCustomItemsForTest(items: List<MediaItem>) {
        _allItems.value = items.map { applyStateOverrides(it) }
    }

    fun addOrUpdateMediaItem(item: MediaItem) {
        val current = _allItems.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        val updated = applyStateOverrides(item)
        if (idx >= 0) {
            current[idx] = updated
        } else {
            current.add(0, updated)
        }
        _allItems.value = current
    }

    fun toggleFavorite(mediaId: String) {
        if (favoriteIds.contains(mediaId)) {
            favoriteIds.remove(mediaId)
        } else {
            favoriteIds.add(mediaId)
        }
        refreshInMemoryOverrides()
    }

    fun setVaulted(mediaId: String, vaulted: Boolean) {
        if (vaulted) vaultedIds.add(mediaId) else vaultedIds.remove(mediaId)
        refreshInMemoryOverrides()
    }

    fun moveToTrash(mediaId: String) {
        trashedTimestamps[mediaId] = System.currentTimeMillis() / 1000L
        refreshInMemoryOverrides()
    }

    fun restoreFromTrash(mediaId: String) {
        trashedTimestamps.remove(mediaId)
        refreshInMemoryOverrides()
    }

    fun updateResumePosition(mediaId: String, positionMs: Long) {
        resumePositions[mediaId] = positionMs.coerceAtLeast(0L)
        refreshInMemoryOverrides()
    }

    fun updateMediaTags(mediaId: String, title: String, artist: String, album: String) {
        customTagOverrides[mediaId] = TagOverride(title.trim(), artist.trim(), album.trim())
        refreshInMemoryOverrides()
    }

    fun attachVoiceNote(mediaId: String, voiceNoteFilePath: String?) {
        if (voiceNoteFilePath == null) {
            voiceNotesByMediaId.remove(mediaId)
        } else {
            voiceNotesByMediaId[mediaId] = voiceNoteFilePath
        }
        refreshInMemoryOverrides()
    }

    fun filterAndSort(
        items: List<MediaItem>,
        filter: MediaFilter,
        sortOrder: SortOrder,
        searchQuery: String = "",
        includeVaulted: Boolean = false,
        onlyVaulted: Boolean = false,
        onlyTrashed: Boolean = false
    ): List<MediaItem> {
        val q = searchQuery.trim().lowercase()
        val filtered = items.filter { item ->
            if (onlyTrashed) return@filter item.isTrashed
            if (item.isTrashed) return@filter false
            if (onlyVaulted) return@filter item.isVaulted
            if (!includeVaulted && item.isVaulted) return@filter false

            val matchesFilter = when (filter) {
                MediaFilter.ALL -> true
                MediaFilter.PHOTOS -> item.mediaType == MediaType.PHOTO
                MediaFilter.RAW_PRO -> item.mediaType == MediaType.RAW_PHOTO || item.extension.equals("DNG", true)
                MediaFilter.ANIMATED -> item.mediaType == MediaType.ANIMATED
                MediaFilter.VIDEO -> item.mediaType == MediaType.VIDEO
                MediaFilter.AUDIO_LOSSLESS -> item.mediaType == MediaType.AUDIO
                MediaFilter.FAVORITES -> item.isFavorite
                MediaFilter.RECENT_CUTS -> item.isStudioExport
            }
            if (!matchesFilter) return@filter false

            if (q.isEmpty()) {
                true
            } else {
                item.title.lowercase().contains(q) ||
                    item.artist.lowercase().contains(q) ||
                    item.album.lowercase().contains(q) ||
                    item.extension.lowercase().contains(q) ||
                    item.qualityBadge.lowercase().contains(q) ||
                    item.codecName.lowercase().contains(q) ||
                    (item.cameraMakeModel?.lowercase()?.contains(q) == true)
            }
        }

        return when (sortOrder) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateAddedEpochSec }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.dateAddedEpochSec }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            SortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            SortOrder.QUALITY_DESC -> filtered.sortedByDescending {
                (it.width * it.height) + (it.bitrateKbps * 1000) + (it.sampleRateHz * it.bitDepth)
            }
        }
    }

    fun buildAlbumBuckets(items: List<MediaItem>): List<AlbumBucket> {
        val activeItems = items.filter { !it.isTrashed && !it.isVaulted }
        return activeItems
            .groupBy { it.album.ifBlank { "Unsorted" } }
            .map { (albumName, group) ->
                val folderPath = group.firstOrNull()?.filePath?.substringBeforeLast('/') ?: "/storage/emulated/0/$albumName"
                AlbumBucket(
                    id = "album_${albumName.lowercase().replace(' ', '_')}",
                    name = albumName,
                    folderPath = folderPath,
                    items = group.sortedByDescending { it.dateAddedEpochSec },
                    isPinned = albumName in setOf("RAW Studio", "Reference Masters", "Cyberpunk Odyssey S01")
                )
            }
            .sortedWith(compareByDescending<AlbumBucket> { it.isPinned }.thenByDescending { it.itemCount })
    }

    private fun refreshInMemoryOverrides() {
        _allItems.value = _allItems.value.map { applyStateOverrides(it) }
    }

    private fun applyStateOverrides(item: MediaItem): MediaItem {
        val fav = if (favoriteIds.contains(item.id)) true else item.isFavorite
        val vaulted = vaultedIds.contains(item.id)
        val trashedAt = trashedTimestamps[item.id] ?: if (item.isTrashed) item.trashedAtEpochSec else 0L
        val resumeMs = resumePositions[item.id] ?: item.resumePositionMs
        val tag = customTagOverrides[item.id]
        val voiceNote = voiceNotesByMediaId[item.id] ?: item.voiceNotePath
        return item.copy(
            title = tag?.title ?: item.title,
            artist = tag?.artist ?: item.artist,
            album = tag?.album ?: item.album,
            isFavorite = fav,
            isVaulted = vaulted,
            isTrashed = trashedAt > 0L,
            trashedAtEpochSec = trashedAt,
            resumePositionMs = resumeMs,
            voiceNotePath = voiceNote
        )
    }

    private fun scanStudioExportsDir(ctx: Context): List<MediaItem> {
        val exportDir = File(ctx.filesDir, "studio_exports")
        if (!exportDir.exists()) return emptyList()
        val files = exportDir.listFiles()?.toList().orEmpty()
        return files.mapNotNull { file ->
            if (!file.isFile) return@mapNotNull null
            val ext = file.extension.uppercase().ifEmpty { "WAV" }
            val mediaType = MediaItem.classifyExtension(ext)
            val isLossless = ext in setOf("WAV", "FLAC", "ALAC", "PNG", "DNG")
            val badge = MediaItem.computeQualityBadge(
                mediaType = mediaType,
                extension = ext,
                width = if (mediaType == MediaType.PHOTO) 1920 else 0,
                height = if (mediaType == MediaType.PHOTO) 1080 else 0,
                fps = 30f,
                bitDepth = if (isLossless) 24 else 16,
                sampleRateHz = if (isLossless) 48000 else 44100,
                bitrateKbps = if (isLossless) 1411 else 320
            )
            MediaItem(
                id = "export_${file.name}",
                title = file.nameWithoutExtension,
                artist = "Photos Studio Export",
                album = "Studio Exports",
                uriString = file.toURI().toString(),
                filePath = file.absolutePath,
                mimeType = when (mediaType) {
                    MediaType.AUDIO -> "audio/wav"
                    MediaType.VIDEO -> "video/mp4"
                    else -> "image/png"
                },
                extension = ext,
                mediaType = mediaType,
                sizeBytes = file.length(),
                dateAddedEpochSec = file.lastModified() / 1000L,
                durationMs = 10_000L,
                qualityBadge = badge,
                isLossless = isLossless,
                isStudioExport = true
            )
        }
    }

    private fun queryDeviceMediaStore(ctx: Context): List<MediaItem> {
        val results = mutableListOf<MediaItem>()
        try {
            val resolver = ctx.contentResolver
            // 1. Images
            val imgProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imgProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < 150) {
                    count++
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1) ?: "Image_$id"
                    val bucket = cursor.getString(2) ?: "Camera Roll"
                    val path = cursor.getString(3) ?: ""
                    val mime = cursor.getString(4) ?: "image/jpeg"
                    val size = cursor.getLong(5)
                    val dateSec = cursor.getLong(6)
                    val w = cursor.getInt(7)
                    val h = cursor.getInt(8)
                    val ext = name.substringAfterLast('.', "JPG").uppercase()
                    val type = MediaItem.classifyExtension(ext, mime)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    results.add(
                        MediaItem(
                            id = "ms_img_$id",
                            title = name.substringBeforeLast('.'),
                            artist = bucket,
                            album = bucket,
                            uriString = uri.toString(),
                            filePath = path,
                            mimeType = mime,
                            extension = ext,
                            mediaType = type,
                            sizeBytes = size,
                            dateAddedEpochSec = dateSec,
                            width = w,
                            height = h,
                            qualityBadge = MediaItem.computeQualityBadge(type, ext, w, h, 0f, 16, 0, 0),
                            isLossless = type == MediaType.RAW_PHOTO || ext == "PNG"
                        )
                    )
                }
            }

            // 2. Video
            val vidProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                vidProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < 100) {
                    count++
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1) ?: "Video_$id"
                    val bucket = cursor.getString(2) ?: "Videos"
                    val path = cursor.getString(3) ?: ""
                    val mime = cursor.getString(4) ?: "video/mp4"
                    val size = cursor.getLong(5)
                    val dateSec = cursor.getLong(6)
                    val durationMs = cursor.getLong(7)
                    val w = cursor.getInt(8)
                    val h = cursor.getInt(9)
                    val ext = name.substringAfterLast('.', "MP4").uppercase()
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val epInfo = parseEpisodeMetadata(name, bucket)
                    results.add(
                        MediaItem(
                            id = "ms_vid_$id",
                            title = name.substringBeforeLast('.'),
                            artist = bucket,
                            album = bucket,
                            uriString = uri.toString(),
                            filePath = path,
                            mimeType = mime,
                            extension = ext,
                            mediaType = MediaType.VIDEO,
                            sizeBytes = size,
                            dateAddedEpochSec = dateSec,
                            durationMs = durationMs,
                            width = w,
                            height = h,
                            fps = 30f,
                            codecName = ext,
                            qualityBadge = MediaItem.computeQualityBadge(MediaType.VIDEO, ext, w, h, 30f, 16, 48000, 8000),
                            seasonNumber = epInfo.first,
                            episodeNumber = epInfo.second,
                            seriesTitle = epInfo.third
                        )
                    )
                }
            }

            // 3. Audio
            val audProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DURATION
            )
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audProjection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < 150) {
                    count++
                    val id = cursor.getLong(0)
                    val title = cursor.getString(1) ?: "Audio_$id"
                    val artist = cursor.getString(2) ?: "Unknown Artist"
                    val album = cursor.getString(3) ?: "Music"
                    val path = cursor.getString(4) ?: ""
                    val mime = cursor.getString(5) ?: "audio/mpeg"
                    val size = cursor.getLong(6)
                    val dateSec = cursor.getLong(7)
                    val durationMs = cursor.getLong(8)
                    val ext = path.substringAfterLast('.', "MP3").uppercase()
                    val isLossless = ext in setOf("FLAC", "WAV", "ALAC", "AIFF", "WV", "APE")
                    val bitrate = if (durationMs > 0L) ((size * 8L) / durationMs).toInt().coerceIn(64, 9216) else 320
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    results.add(
                        MediaItem(
                            id = "ms_aud_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            uriString = uri.toString(),
                            filePath = path,
                            mimeType = mime,
                            extension = ext,
                            mediaType = MediaType.AUDIO,
                            sizeBytes = size,
                            dateAddedEpochSec = dateSec,
                            durationMs = durationMs,
                            bitrateKbps = bitrate,
                            sampleRateHz = if (isLossless) 48000 else 44100,
                            bitDepth = if (isLossless) 24 else 16,
                            codecName = ext,
                            qualityBadge = MediaItem.computeQualityBadge(
                                MediaType.AUDIO,
                                ext,
                                0,
                                0,
                                0f,
                                if (isLossless) 24 else 16,
                                if (isLossless) 48000 else 44100,
                                bitrate
                            ),
                            isLossless = isLossless,
                            isHiRes = isLossless && bitrate > 2000
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not yet granted; sample library remains active
        } catch (_: Exception) {
        }
        return results
    }

    companion object {
        private val sXeYRegex = Regex("""(?i)(.*?)[._\-\s]+S(\d{1,2})E(\d{1,3})""")
        private val epRegex = Regex("""(?i)(.*?)[._\-\s]+(?:Episode|Ep)[._\-\s]*(\d{1,3})""")

        fun parseEpisodeMetadata(fileName: String, fallbackSeries: String): Triple<Int?, Int?, String?> {
            val clean = fileName.substringBeforeLast('.')
            sXeYRegex.find(clean)?.let { m ->
                val series = m.groupValues[1].replace('.', ' ').replace('_', ' ').trim().ifEmpty { fallbackSeries }
                val s = m.groupValues[2].toIntOrNull() ?: 1
                val e = m.groupValues[3].toIntOrNull() ?: 1
                return Triple(s, e, series)
            }
            epRegex.find(clean)?.let { m ->
                val series = m.groupValues[1].replace('.', ' ').replace('_', ' ').trim().ifEmpty { fallbackSeries }
                val e = m.groupValues[2].toIntOrNull() ?: 1
                return Triple(1, e, series)
            }
            return Triple(null, null, null)
        }
    }
}
