// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media

import com.arinara.photos.data.model.AudioBookmark
import com.arinara.photos.data.model.CueTrackSlice
import com.arinara.photos.data.model.LoopMode
import com.arinara.photos.data.model.LrcLyricLine
import com.arinara.photos.data.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

enum class QueueSlot(val slotIndex: Int, val displayName: String) {
    MAIN(0, "Main Queue"),
    STUDY_LOFI(1, "Study / Lo-Fi"),
    WORKOUT(2, "Workout"),
    PODCASTS(3, "Podcasts & Lectures"),
    LATE_NIGHT(4, "Late Night")
}

data class QueueSlotState(
    val slot: QueueSlot,
    val tracks: List<MediaItem> = emptyList(),
    val activeIndex: Int = 0,
    val savedPositionMs: Long = 0L,
    val isShuffled: Boolean = false
) {
    val currentTrack: MediaItem?
        get() = tracks.getOrNull(activeIndex)
}

data class AbLoopState(
    val startMs: Long? = null,
    val endMs: Long? = null,
    val completedIterations: Int = 0,
    val maxIterations: Int = 0 // 0 = infinite A-B loop
) {
    val isConfigured: Boolean
        get() = startMs != null && endMs != null && endMs > startMs
}

sealed interface LoopTickDirective {
    object ContinuePlayback : LoopTickDirective
    data class SeekToMs(val targetMs: Long) : LoopTickDirective
    data class AdvanceToTrack(val nextIndex: Int) : LoopTickDirective
    object StopEndOfQueue : LoopTickDirective
}

class MultiQueueManager {

    private val slotMap = LinkedHashMap<QueueSlot, QueueSlotState>().apply {
        QueueSlot.entries.forEach { slot ->
            put(slot, QueueSlotState(slot = slot))
        }
    }

    private val _activeSlot = MutableStateFlow(QueueSlot.MAIN)
    val activeSlot: StateFlow<QueueSlot> = _activeSlot.asStateFlow()

    private val _activeQueueState = MutableStateFlow(slotMap[QueueSlot.MAIN]!!)
    val activeQueueState: StateFlow<QueueSlotState> = _activeQueueState.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.OFF)
    val loopMode: StateFlow<LoopMode> = _loopMode.asStateFlow()

    private val _abLoopState = MutableStateFlow(AbLoopState())
    val abLoopState: StateFlow<AbLoopState> = _abLoopState.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<AudioBookmark>>(emptyList())
    val bookmarks: StateFlow<List<AudioBookmark>> = _bookmarks.asStateFlow()

    private val unshuffledBackupBySlot = mutableMapOf<QueueSlot, List<MediaItem>>()
    private val playedHistoryIds = ArrayDeque<String>()

    fun seedQueues(allAudioItems: List<MediaItem>) {
        if (allAudioItems.isEmpty()) return
        val currentMain = slotMap[QueueSlot.MAIN]
        if (currentMain == null || currentMain.tracks.isEmpty()) {
            slotMap[QueueSlot.MAIN] = QueueSlotState(
                slot = QueueSlot.MAIN,
                tracks = allAudioItems,
                activeIndex = 0,
                savedPositionMs = 0L
            )
            slotMap[QueueSlot.STUDY_LOFI] = QueueSlotState(
                slot = QueueSlot.STUDY_LOFI,
                tracks = allAudioItems.reversed(),
                activeIndex = 0,
                savedPositionMs = 0L
            )
            slotMap[QueueSlot.LATE_NIGHT] = QueueSlotState(
                slot = QueueSlot.LATE_NIGHT,
                tracks = allAudioItems.filter { it.isLossless }.ifEmpty { allAudioItems },
                activeIndex = 0,
                savedPositionMs = 0L
            )
            syncActiveFlow()
        }
    }

    fun getAllSlotStates(): List<QueueSlotState> = QueueSlot.entries.mapNotNull { slotMap[it] }

    fun switchQueueSlot(targetSlot: QueueSlot, currentPlaybackPositionMs: Long): QueueSlotState {
        val currentSlot = _activeSlot.value
        val currentState = slotMap[currentSlot]
        if (currentState != null) {
            slotMap[currentSlot] = currentState.copy(savedPositionMs = currentPlaybackPositionMs.coerceAtLeast(0L))
        }
        _activeSlot.value = targetSlot
        syncActiveFlow()
        return _activeQueueState.value
    }

    fun setQueueTracks(tracks: List<MediaItem>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val slot = _activeSlot.value
        val safeIdx = if (tracks.isEmpty()) 0 else startIndex.coerceIn(0, tracks.lastIndex)
        slotMap[slot] = QueueSlotState(
            slot = slot,
            tracks = tracks,
            activeIndex = safeIdx,
            savedPositionMs = startPositionMs,
            isShuffled = false
        )
        unshuffledBackupBySlot.remove(slot)
        syncActiveFlow()
    }

    fun selectTrackAt(index: Int, resetAbLoop: Boolean = true): MediaItem? {
        val slot = _activeSlot.value
        val state = slotMap[slot] ?: return null
        if (index !in state.tracks.indices) return null
        state.currentTrack?.let { rememberPlayed(it.id) }
        slotMap[slot] = state.copy(activeIndex = index, savedPositionMs = 0L)
        if (resetAbLoop && _loopMode.value == LoopMode.AB_SECTION) {
            _abLoopState.value = AbLoopState()
            _loopMode.value = LoopMode.OFF
        }
        syncActiveFlow()
        return slotMap[slot]?.currentTrack
    }

    fun playNext(item: MediaItem) {
        val slot = _activeSlot.value
        val state = slotMap[slot] ?: return
        val mutable = state.tracks.toMutableList()
        if (mutable.isEmpty()) {
            mutable.add(item)
            slotMap[slot] = state.copy(tracks = mutable, activeIndex = 0)
        } else {
            val insertIdx = (state.activeIndex + 1).coerceAtMost(mutable.size)
            mutable.add(insertIdx, item)
            slotMap[slot] = state.copy(tracks = mutable)
        }
        syncActiveFlow()
    }

    fun addToQueueTail(item: MediaItem) {
        val slot = _activeSlot.value
        val state = slotMap[slot] ?: return
        val mutable = state.tracks.toMutableList()
        mutable.add(item)
        slotMap[slot] = state.copy(tracks = mutable)
        syncActiveFlow()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val slot = _activeSlot.value
        val state = slotMap[slot] ?: return
        val list = state.tracks.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return

        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)

        val newActiveIndex = when {
            state.activeIndex == fromIndex -> toIndex
            fromIndex < state.activeIndex && toIndex >= state.activeIndex -> state.activeIndex - 1
            fromIndex > state.activeIndex && toIndex <= state.activeIndex -> state.activeIndex + 1
            else -> state.activeIndex
        }
        slotMap[slot] = state.copy(tracks = list, activeIndex = newActiveIndex.coerceIn(0, list.lastIndex))
        syncActiveFlow()
    }

    fun removeFromQueue(index: Int) {
        val slot = _activeSlot.value
        val state = slotMap[slot] ?: return
        val list = state.tracks.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        val newActive = when {
            list.isEmpty() -> 0
            index < state.activeIndex -> (state.activeIndex - 1).coerceAtLeast(0)
            index == state.activeIndex -> state.activeIndex.coerceAtMost(list.lastIndex)
            else -> state.activeIndex
        }
        slotMap[slot] = state.copy(tracks = list, activeIndex = newActive)
        syncActiveFlow()
    }

    fun toggleShuffle(random: Random = Random.Default) {
        val slot = _activeSlot.value
        val state = slotMap[slot] ?: return
        if (state.tracks.size <= 1) return

        if (!state.isShuffled) {
            unshuffledBackupBySlot[slot] = state.tracks
            val current = state.currentTrack
            val remaining = state.tracks.filterIndexed { idx, _ -> idx != state.activeIndex }
            // History-aware shuffle: push recently played tracks toward the end of the shuffled tail
            val fresh = remaining.filter { it.id !in playedHistoryIds }.shuffled(random)
            val recent = remaining.filter { it.id in playedHistoryIds }.shuffled(random)
            val newOrder = buildList {
                if (current != null) add(current)
                addAll(fresh)
                addAll(recent)
            }
            slotMap[slot] = state.copy(tracks = newOrder, activeIndex = 0, isShuffled = true)
        } else {
            val current = state.currentTrack
            val original = unshuffledBackupBySlot.remove(slot) ?: state.tracks
            val restoredIdx = if (current != null) {
                original.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            } else 0
            slotMap[slot] = state.copy(tracks = original, activeIndex = restoredIdx, isShuffled = false)
        }
        syncActiveFlow()
    }

    fun cycleLoopMode(): LoopMode {
        val next = _loopMode.value.next()
        _loopMode.value = next
        if (next != LoopMode.AB_SECTION) {
            _abLoopState.value = AbLoopState()
        }
        return next
    }

    fun setLoopMode(mode: LoopMode) {
        _loopMode.value = mode
        if (mode != LoopMode.AB_SECTION) {
            _abLoopState.value = AbLoopState()
        }
    }

    fun markAbPointA(positionMs: Long) {
        val current = _abLoopState.value
        val safeA = positionMs.coerceAtLeast(0L)
        val validEnd = current.endMs?.takeIf { it > safeA + 250L }
        _abLoopState.value = current.copy(startMs = safeA, endMs = validEnd, completedIterations = 0)
        _loopMode.value = LoopMode.AB_SECTION
    }

    fun markAbPointB(positionMs: Long) {
        val current = _abLoopState.value
        val start = current.startMs ?: 0L
        val safeB = positionMs.coerceAtLeast(start + 500L)
        _abLoopState.value = current.copy(startMs = start, endMs = safeB, completedIterations = 0)
        _loopMode.value = LoopMode.AB_SECTION
    }

    fun nudgeAbBounds(startDeltaMs: Long, endDeltaMs: Long, trackDurationMs: Long) {
        val current = _abLoopState.value
        val s = ((current.startMs ?: 0L) + startDeltaMs).coerceIn(0L, (trackDurationMs - 500L).coerceAtLeast(0L))
        val e = ((current.endMs ?: (s + 2000L)) + endDeltaMs).coerceIn(s + 500L, trackDurationMs.coerceAtLeast(s + 500L))
        _abLoopState.value = current.copy(startMs = s, endMs = e)
    }

    fun setAbMaxIterations(maxIterations: Int) {
        _abLoopState.value = _abLoopState.value.copy(maxIterations = maxIterations.coerceAtLeast(0))
    }

    fun clearAbLoop() {
        _abLoopState.value = AbLoopState()
        if (_loopMode.value == LoopMode.AB_SECTION) {
            _loopMode.value = LoopMode.OFF
        }
    }

    fun evaluatePositionTick(currentPositionMs: Long, trackDurationMs: Long): LoopTickDirective {
        val mode = _loopMode.value
        val ab = _abLoopState.value

        if (mode == LoopMode.AB_SECTION && ab.isConfigured) {
            val start = ab.startMs!!
            val end = ab.endMs!!
            if (currentPositionMs >= end) {
                val nextCount = ab.completedIterations + 1
                if (ab.maxIterations > 0 && nextCount >= ab.maxIterations) {
                    clearAbLoop()
                    return LoopTickDirective.ContinuePlayback
                }
                _abLoopState.value = ab.copy(completedIterations = nextCount)
                return LoopTickDirective.SeekToMs(start)
            }
        }

        if (trackDurationMs > 0L && currentPositionMs >= trackDurationMs - 120L) {
            val state = _activeQueueState.value
            return when (mode) {
                LoopMode.TRACK -> LoopTickDirective.SeekToMs(0L)
                LoopMode.QUEUE -> {
                    val nextIdx = if (state.tracks.isEmpty()) 0 else (state.activeIndex + 1) % state.tracks.size
                    LoopTickDirective.AdvanceToTrack(nextIdx)
                }
                LoopMode.OFF, LoopMode.AB_SECTION -> {
                    if (state.activeIndex + 1 < state.tracks.size) {
                        LoopTickDirective.AdvanceToTrack(state.activeIndex + 1)
                    } else {
                        LoopTickDirective.StopEndOfQueue
                    }
                }
            }
        }
        return LoopTickDirective.ContinuePlayback
    }

    fun saveActiveQueueAsM3u8(exportDir: File, playlistName: String): File {
        exportDir.mkdirs()
        val cleanName = playlistName.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").ifBlank { "QueueExport" }
        val file = File(exportDir, "$cleanName.m3u8")
        val tracks = _activeQueueState.value.tracks
        val sb = StringBuilder("#EXTM3U\n")
        tracks.forEach { item ->
            val sec = (item.durationMs / 1000L).coerceAtLeast(1L)
            sb.append("#EXTINF:$sec,${item.artist} - ${item.title}\n")
            sb.append("${item.filePath}\n")
        }
        file.writeText(sb.toString())
        return file
    }

    fun addBookmark(mediaId: String, timestampMs: Long, label: String, note: String = ""): AudioBookmark {
        val bm = AudioBookmark(
            id = "bm_${System.currentTimeMillis()}_${_bookmarks.value.size}",
            mediaId = mediaId,
            timestampMs = timestampMs.coerceAtLeast(0L),
            label = label.ifBlank { "Marker @ ${MediaItem.formatDurationMillis(timestampMs)}" },
            note = note
        )
        _bookmarks.value = (_bookmarks.value + bm).sortedBy { it.timestampMs }
        return bm
    }

    fun getBookmarksForMedia(mediaId: String): List<AudioBookmark> =
        _bookmarks.value.filter { it.mediaId == mediaId }.sortedBy { it.timestampMs }

    fun removeBookmark(bookmarkId: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == bookmarkId }
    }

    private fun rememberPlayed(mediaId: String) {
        playedHistoryIds.remove(mediaId)
        playedHistoryIds.addLast(mediaId)
        while (playedHistoryIds.size > 25) {
            playedHistoryIds.removeFirst()
        }
    }

    private fun syncActiveFlow() {
        val current = slotMap[_activeSlot.value] ?: return
        _activeQueueState.value = current
    }

    companion object {
        private val lrcLineRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
        private val cueTrackRegex = Regex("""(?i)^\s*TRACK\s+(\d+)\s+AUDIO""")
        private val cueTitleRegex = Regex("""(?i)^\s*TITLE\s+"([^"]+)"""")
        private val cuePerformerRegex = Regex("""(?i)^\s*PERFORMER\s+"([^"]+)"""")
        private val cueIndexRegex = Regex("""(?i)^\s*INDEX\s+01\s+(\d{2}):(\d{2}):(\d{2})""")

        fun parseLrcLyrics(lrcContent: String): List<LrcLyricLine> {
            val result = mutableListOf<LrcLyricLine>()
            lrcContent.lineSequence().forEach { rawLine ->
                val match = lrcLineRegex.find(rawLine.trim()) ?: return@forEach
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = match.groupValues[3]
                val ms = when (fracRaw.length) {
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val text = match.groupValues[4].trim()
                val totalMs = (min * 60_000L) + (sec * 1_000L) + ms
                if (text.isNotEmpty()) {
                    result.add(LrcLyricLine(timestampMs = totalMs, text = text))
                }
            }
            return result.sortedBy { it.timestampMs }
        }

        fun findActiveLyricIndex(lines: List<LrcLyricLine>, positionMs: Long, syncOffsetMs: Long = 0L): Int {
            if (lines.isEmpty()) return -1
            val adjusted = (positionMs - syncOffsetMs).coerceAtLeast(0L)
            var activeIdx = 0
            for (i in lines.indices) {
                if (adjusted >= lines[i].timestampMs) {
                    activeIdx = i
                } else {
                    break
                }
            }
            return activeIdx
        }

        fun parseCueSheet(cueText: String, sourceFilePath: String, totalDurationMs: Long = 18_000L): List<CueTrackSlice> {
            val rawSlices = mutableListOf<Triple<Int, Pair<String, String>, Long>>()
            var currentTrackNum: Int? = null
            var currentTitle = "Track"
            var currentPerformer = "Artist"
            var globalPerformer = "Unknown Artist"

            cueText.lineSequence().forEach { line ->
                val trimmed = line.trim()
                cueTrackRegex.find(trimmed)?.let { m ->
                    currentTrackNum = m.groupValues[1].toIntOrNull() ?: 1
                    currentTitle = "Track $currentTrackNum"
                    currentPerformer = globalPerformer
                }
                cueTitleRegex.find(trimmed)?.let { m ->
                    if (currentTrackNum != null) {
                        currentTitle = m.groupValues[1]
                    }
                }
                cuePerformerRegex.find(trimmed)?.let { m ->
                    if (currentTrackNum == null) {
                        globalPerformer = m.groupValues[1]
                    } else {
                        currentPerformer = m.groupValues[1]
                    }
                }
                cueIndexRegex.find(trimmed)?.let { m ->
                    val tNum = currentTrackNum
                    if (tNum != null) {
                        val mm = m.groupValues[1].toLongOrNull() ?: 0L
                        val ss = m.groupValues[2].toLongOrNull() ?: 0L
                        val ff = m.groupValues[3].toLongOrNull() ?: 0L // 75 frames per second in CD CUE
                        val startMs = (mm * 60_000L) + (ss * 1_000L) + ((ff * 1000L) / 75L)
                        rawSlices.add(Triple(tNum, currentTitle to currentPerformer, startMs))
                    }
                }
            }

            return rawSlices.mapIndexed { idx, entry ->
                val nextStart = rawSlices.getOrNull(idx + 1)?.third ?: totalDurationMs
                CueTrackSlice(
                    trackNumber = entry.first,
                    title = entry.second.first,
                    performer = entry.second.second,
                    startMs = entry.third,
                    endMs = nextStart.coerceAtLeast(entry.third + 500L),
                    sourceFilePath = sourceFilePath
                )
            }
        }

        fun formatLrcTimestamp(timestampMs: Long): String {
            val safe = timestampMs.coerceAtLeast(0L)
            val minutes = safe / 60_000L
            val seconds = (safe % 60_000L) / 1_000L
            val hundredths = (safe % 1_000L) / 10L
            return String.format("[%02d:%02d.%02d]", minutes, seconds, hundredths)
        }

        fun serializeLrcLyrics(lines: List<LrcLyricLine>): String {
            return lines
                .sortedBy { it.timestampMs }
                .joinToString("\n") { line ->
                    "${formatLrcTimestamp(line.timestampMs)} ${line.text.trim()}"
                }
        }

        fun stampPlainLyricLine(rawText: String, lineIndex: Int, timestampMs: Long): String {
            val rawLines = rawText.lines().toMutableList()
            if (lineIndex !in rawLines.indices) return rawText
            val existing = rawLines[lineIndex].trim()
            val cleanText = lrcLineRegex.find(existing)?.groupValues?.getOrNull(4)?.trim() ?: existing
            rawLines[lineIndex] = "${formatLrcTimestamp(timestampMs)} $cleanText"
            return rawLines.joinToString("\n")
        }

        fun computeSleepFadeMultiplier(remainingMs: Long, fadeWindowMs: Long = 30_000L): Float {
            if (remainingMs <= 0L) return 0f
            if (remainingMs >= fadeWindowMs) return 1f
            val linear = remainingMs.toFloat() / fadeWindowMs.toFloat()
            return (linear * linear).coerceIn(0f, 1f)
        }
    }
}
