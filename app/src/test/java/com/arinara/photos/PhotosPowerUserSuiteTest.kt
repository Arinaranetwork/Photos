// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos

import com.arinara.photos.data.model.AudioQualityTier
import com.arinara.photos.data.model.LoopMode
import com.arinara.photos.data.model.MediaFilter
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import com.arinara.photos.data.model.SortOrder
import com.arinara.photos.data.repository.MediaRepository
import com.arinara.photos.data.repository.SampleMediaGenerator
import com.arinara.photos.media.AudioDspController
import com.arinara.photos.media.AudioOutputDevice
import com.arinara.photos.media.LoopTickDirective
import com.arinara.photos.media.MultiQueueManager
import com.arinara.photos.media.OutputSignalMode
import com.arinara.photos.media.QueueSlot
import com.arinara.photos.media.VideoTheaterEngine
import com.arinara.photos.media.inspector.MediaInspectorAndVaultEngine
import com.arinara.photos.media.trimmer.MediaTrimmerEngine
import com.arinara.photos.media.trimmer.TrimCutMode
import com.arinara.photos.media.trimmer.TrimJobConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PhotosPowerUserSuiteTest {

    private fun sampleItems(): List<MediaItem> = listOf(
        MediaItem(
            id = "aud_1",
            title = "Electric Slate Overture",
            artist = "Arinara Lab",
            album = "Reference Masters",
            uriString = "file:///tmp/aud1.flac",
            filePath = "/tmp/aud1.flac",
            mimeType = "audio/flac",
            extension = "FLAC",
            mediaType = MediaType.AUDIO,
            sizeBytes = 42_000_000L,
            dateAddedEpochSec = 1000L,
            durationMs = 18_000L,
            bitrateKbps = 4608,
            sampleRateHz = 96000,
            bitDepth = 24,
            codecName = "FLAC",
            qualityBadge = "HI-RES 24B/96K",
            isLossless = true,
            isHiRes = true
        ),
        MediaItem(
            id = "aud_2",
            title = "Midnight Analog Horizon",
            artist = "Arinara Lab",
            album = "Reference Masters",
            uriString = "file:///tmp/aud2.wav",
            filePath = "/tmp/aud2.wav",
            mimeType = "audio/wav",
            extension = "WAV",
            mediaType = MediaType.AUDIO,
            sizeBytes = 28_000_000L,
            dateAddedEpochSec = 950L,
            durationMs = 16_000L,
            bitrateKbps = 1411,
            sampleRateHz = 44100,
            bitDepth = 16,
            codecName = "WAV PCM",
            qualityBadge = "LOSSLESS WAV",
            isLossless = true
        ),
        MediaItem(
            id = "aud_3",
            title = "Tokyo Rain Session",
            artist = "Kira Beats",
            album = "Study Beats",
            uriString = "file:///tmp/aud3.mp3",
            filePath = "/tmp/aud3.mp3",
            mimeType = "audio/mpeg",
            extension = "MP3",
            mediaType = MediaType.AUDIO,
            sizeBytes = 6_000_000L,
            dateAddedEpochSec = 900L,
            durationMs = 14_000L,
            bitrateKbps = 320,
            sampleRateHz = 48000,
            bitDepth = 16,
            codecName = "MP3",
            qualityBadge = "320K HQ"
        ),
        MediaItem(
            id = "vid_e1",
            title = "Cyberpunk Odyssey - S01E01 - Awakening",
            artist = "Arinara Cinema",
            album = "Cyberpunk Odyssey S01",
            uriString = "file:///tmp/s01e01.mkv",
            filePath = "/tmp/s01e01.mkv",
            mimeType = "video/x-matroska",
            extension = "MKV",
            mediaType = MediaType.VIDEO,
            sizeBytes = 85_000_000L,
            dateAddedEpochSec = 1100L,
            durationMs = 20_000L,
            width = 3840,
            height = 2160,
            fps = 60f,
            qualityBadge = "4K 60",
            seasonNumber = 1,
            episodeNumber = 1,
            seriesTitle = "Cyberpunk Odyssey",
            resumePositionMs = 5_000L
        ),
        MediaItem(
            id = "vid_e2",
            title = "Cyberpunk Odyssey - S01E02 - Ghost Protocol",
            artist = "Arinara Cinema",
            album = "Cyberpunk Odyssey S01",
            uriString = "file:///tmp/s01e02.mkv",
            filePath = "/tmp/s01e02.mkv",
            mimeType = "video/x-matroska",
            extension = "MKV",
            mediaType = MediaType.VIDEO,
            sizeBytes = 90_000_000L,
            dateAddedEpochSec = 1200L,
            durationMs = 22_000L,
            width = 3840,
            height = 2160,
            fps = 60f,
            qualityBadge = "4K 60",
            seasonNumber = 1,
            episodeNumber = 2,
            seriesTitle = "Cyberpunk Odyssey",
            resumePositionMs = 0L
        ),
        MediaItem(
            id = "raw_1",
            title = "Alpine Starlight Burst A",
            artist = "Nikon Z8",
            album = "RAW Studio",
            uriString = "file:///tmp/burstA.dng",
            filePath = "/tmp/burstA.dng",
            mimeType = "image/x-adobe-dng",
            extension = "DNG",
            mediaType = MediaType.RAW_PHOTO,
            sizeBytes = 55_000_000L,
            dateAddedEpochSec = 1300L,
            width = 8256,
            height = 5504,
            qualityBadge = "RAW DNG",
            isLossless = true
        ),
        MediaItem(
            id = "raw_2",
            title = "Alpine Starlight Burst B",
            artist = "Nikon Z8",
            album = "RAW Studio",
            uriString = "file:///tmp/burstB.dng",
            filePath = "/tmp/burstB.dng",
            mimeType = "image/x-adobe-dng",
            extension = "DNG",
            mediaType = MediaType.RAW_PHOTO,
            sizeBytes = 52_000_000L,
            dateAddedEpochSec = 1310L,
            width = 8256,
            height = 5504,
            qualityBadge = "RAW DNG",
            isLossless = true
        )
    )

    @Test
    fun `A-001 universal media scanner classifies formats badges filters and builds albums`() {
        assertEquals(MediaType.RAW_PHOTO, MediaItem.classifyExtension("dng"))
        assertEquals(MediaType.ANIMATED, MediaItem.classifyExtension("gif"))
        assertEquals(MediaType.VIDEO, MediaItem.classifyExtension("mkv"))
        assertEquals(MediaType.AUDIO, MediaItem.classifyExtension("flac"))

        val hiResBadge = MediaItem.computeQualityBadge(
            mediaType = MediaType.AUDIO,
            extension = "FLAC",
            width = 0,
            height = 0,
            fps = 0f,
            bitDepth = 24,
            sampleRateHz = 96000,
            bitrateKbps = 4608
        )
        assertEquals("HI-RES 24B/96K", hiResBadge)

        val repo = MediaRepository(context = null)
        repo.loadWithCustomItemsForTest(sampleItems())

        val rawFiltered = repo.filterAndSort(repo.allItems.value, MediaFilter.RAW_PRO, SortOrder.DATE_DESC)
        assertEquals(2, rawFiltered.size)

        val searched = repo.filterAndSort(repo.allItems.value, MediaFilter.ALL, SortOrder.NAME_ASC, searchQuery = "Overture")
        assertEquals(1, searched.size)
        assertEquals("aud_1", searched.first().id)

        val albums = repo.buildAlbumBuckets(repo.allItems.value)
        assertTrue(albums.any { it.name == "RAW Studio" && it.isPinned })
    }

    @Test
    fun `A-002 multi-queue manager supports 5 slots playNext reorder 4-state loop LRC and CUE`() {
        val qm = MultiQueueManager()
        val audioTracks = sampleItems().filter { it.mediaType == MediaType.AUDIO }
        qm.setQueueTracks(audioTracks, startIndex = 0)

        // Play Next inserts at activeIndex + 1
        val extraTrack = audioTracks.last().copy(id = "aud_extra", title = "Up Next Special")
        qm.playNext(extraTrack)
        assertEquals("aud_extra", qm.activeQueueState.value.tracks[1].id)

        // Move Queue Item preserves active track index
        qm.selectTrackAt(1)
        assertEquals("aud_extra", qm.activeQueueState.value.currentTrack?.id)
        qm.moveQueueItem(fromIndex = 1, toIndex = 2)
        assertEquals(2, qm.activeQueueState.value.activeIndex)
        assertEquals("aud_extra", qm.activeQueueState.value.currentTrack?.id)

        // 5 Independent Multi-Queue Slots with millisecond position memory
        val studyState = qm.switchQueueSlot(QueueSlot.STUDY_LOFI, currentPlaybackPositionMs = 12_345L)
        assertEquals(QueueSlot.STUDY_LOFI, studyState.slot)
        qm.setQueueTracks(listOf(audioTracks.last()), startIndex = 0, startPositionMs = 4_500L)
        val backToMain = qm.switchQueueSlot(QueueSlot.MAIN, currentPlaybackPositionMs = 7_890L)
        assertEquals(12_345L, backToMain.savedPositionMs)
        val backToStudy = qm.switchQueueSlot(QueueSlot.STUDY_LOFI, currentPlaybackPositionMs = 12_345L)
        assertEquals(7_890L, backToStudy.savedPositionMs)

        // 4-State Loop & Precision A-B Loop with max iterations auto-exit
        assertEquals(LoopMode.OFF, qm.loopMode.value)
        assertEquals(LoopMode.QUEUE, qm.cycleLoopMode())
        assertEquals(LoopMode.TRACK, qm.cycleLoopMode())
        assertEquals(LoopMode.AB_SECTION, qm.cycleLoopMode())

        qm.markAbPointA(2_000L)
        qm.markAbPointB(5_000L)
        qm.setAbMaxIterations(2)

        val tick1 = qm.evaluatePositionTick(currentPositionMs = 5_100L, trackDurationMs = 18_000L)
        assertTrue(tick1 is LoopTickDirective.SeekToMs && tick1.targetMs == 2_000L)
        assertEquals(1, qm.abLoopState.value.completedIterations)

        val tick2 = qm.evaluatePositionTick(currentPositionMs = 5_100L, trackDurationMs = 18_000L)
        assertTrue(tick2 is LoopTickDirective.ContinuePlayback)
        assertEquals(LoopMode.OFF, qm.loopMode.value)

        // LRC Parser & Active Line Lookup
        val lrcLines = MultiQueueManager.parseLrcLyrics(
            """
            [00:01.50] First line
            [00:05.00] Second line
            """.trimIndent()
        )
        assertEquals(2, lrcLines.size)
        assertEquals(1, MultiQueueManager.findActiveLyricIndex(lrcLines, positionMs = 5_200L))

        // CUE Sheet Parser
        val cueSlices = MultiQueueManager.parseCueSheet(
            """
            PERFORMER "Arinara"
            TITLE "Master"
            FILE "master.wav" WAVE
              TRACK 01 AUDIO
                TITLE "Intro"
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "Finale"
                INDEX 01 00:06:00
            """.trimIndent(),
            sourceFilePath = "/tmp/master.wav",
            totalDurationMs = 18_000L
        )
        assertEquals(2, cueSlices.size)
        assertEquals(6_000L, cueSlices[1].startMs)
    }

    @Test
    fun `A-003 10-band EQ parametric AutoEq importer and bit-perfect signal path`() {
        val dsp = AudioDspController()
        dsp.setBandGain(0, 9.5f)
        assertEquals(9.5f, dsp.dspState.value.bandGainsDb[0], 0.01f)

        // Import AutoEq profile
        val imported = dsp.importAutoEqProfile(
            "Harman Reference",
            """
            Preamp: -3.5 dB
            Filter 1: ON LSC Fc 105 Hz Gain 5.5 dB Q 0.71
            Filter 2: ON PK Fc 3200 Hz Gain 3.0 dB Q 1.80
            """.trimIndent()
        )
        assertEquals(-3.5f, imported.preampDb, 0.01f)
        assertTrue(dsp.dspState.value.isParametricMode)

        // Per-Output Device Auto-Preset binding
        dsp.switchOutputDevice(AudioOutputDevice.BLUETOOTH_AUDIO)
        assertEquals("Bass Boost", dsp.dspState.value.activePresetName)

        // Bit-Perfect Direct PCM Signal Path Report
        dsp.setOutputSignalMode(OutputSignalMode.BIT_PERFECT_DIRECT_PCM)
        val report = dsp.buildSignalPathReport(
            codecName = "FLAC",
            bitDepth = 24,
            sampleRateHz = 96000,
            bitrateKbps = 4608,
            isLosslessSource = true
        )
        assertTrue(report.isBitPerfectActive)
        assertFalse(dsp.dspState.value.eqEnabled)

        val fft = dsp.generateSpectrumFrame(playbackPositionMs = 2_400L, isPlaying = true, isLossless = true)
        assertEquals(32, fft.size)
    }

    @Test
    fun `A-004 Netflix video theater engine continue watching episodes skip intro and dual subtitles`() {
        val engine = VideoTheaterEngine()
        val all = sampleItems()

        val continueList = engine.getContinueWatchingItems(all)
        assertEquals(1, continueList.size)
        assertEquals("vid_e1", continueList.first().id)

        val nextEp = engine.findNextEpisode(all, continueList.first())
        assertNotNull(nextEp)
        assertEquals("vid_e2", nextEp?.id)

        // +85s Skip Intro calculation
        val skippedPos = engine.computeSkipIntroTargetMs(currentPositionMs = 10_000L, durationMs = 1_200_000L, skipDeltaMs = 85_000L)
        assertEquals(95_000L, skippedPos)

        // Extra-Dim Brightness (-80%) and 200% Volume Boost math
        val extraDim = VideoTheaterEngine.applyBrightnessGestureDelta(currentLevel = -0.50f, deltaFraction = -0.60f)
        assertEquals(-0.80f, extraDim, 0.001f)
        val boostedVol = VideoTheaterEngine.applyVolumeBoostGestureDelta(currentPercent = 160, deltaPercent = 80)
        assertEquals(200, boostedVol)

        // Dual Subtitles & Clickable Transcript Search
        val enSrt = """
            1
            00:00:01,000 --> 00:00:04,000
            Welcome to Sector 9 quantum relay.
        """.trimIndent()
        val jpSrt = """
            1
            00:00:01,000 --> 00:00:04,000
            セクター9へようこそ。
        """.trimIndent()
        val merged = VideoTheaterEngine.mergeDualSubtitles(
            VideoTheaterEngine.parseSrtSubtitles(enSrt),
            VideoTheaterEngine.parseSrtSubtitles(jpSrt)
        )
        assertEquals(1, merged.size)
        assertEquals("セクター9へようこそ。", merged.first().secondaryText)

        val searchHits = VideoTheaterEngine.searchSubtitleTranscript(merged, "quantum")
        assertEquals(1, searchHits.size)
        assertEquals(1_000L, searchHits.first().startMs)
    }

    @Test
    fun `A-005 non-destructive trimmer slices WAV PCM and exports multi-segment and GIF without touching source`() {
        val tempDir = Files.createTempDirectory("photos_trim_test").toFile()
        try {
            val srcWav = File(tempDir, "source_master.wav")
            SampleMediaGenerator.writeSynthesizedWavFile(srcWav, durationSeconds = 6, sampleRate = 44100)
            val originalLength = srcWav.length()
            assertTrue(originalLength > 1000L)

            val srcItem = MediaItem(
                id = "src_wav",
                title = "Source Master",
                uriString = srcWav.toURI().toString(),
                filePath = srcWav.absolutePath,
                mimeType = "audio/wav",
                extension = "WAV",
                mediaType = MediaType.AUDIO,
                sizeBytes = originalLength,
                dateAddedEpochSec = 1000L,
                durationMs = 6_000L,
                isLossless = true
            )

            val trimmer = MediaTrimmerEngine()
            // 1. Trim Outside
            val resOutside = trimmer.executeTrimOrConvert(
                outputDir = tempDir,
                config = TrimJobConfig(
                    sourceItem = srcItem,
                    cutMode = TrimCutMode.TRIM_OUTSIDE,
                    startMs = 1_000L,
                    endMs = 4_000L,
                    qualityTier = AudioQualityTier.CD_LOSSLESS,
                    fadeInMs = 300L,
                    fadeOutMs = 300L
                )
            )
            assertEquals(1, resOutside.exportedItems.size)
            val cutFile = File(resOutside.exportedItems.first().filePath)
            assertTrue(cutFile.exists() && cutFile.length() > 44L && cutFile.length() < originalLength)
            // Source file untouched
            assertEquals(originalLength, srcWav.length())

            // 2. Multi-Segment 3-Part Split
            val resSplit = trimmer.executeTrimOrConvert(
                outputDir = tempDir,
                config = TrimJobConfig(
                    sourceItem = srcItem,
                    cutMode = TrimCutMode.MULTI_SEGMENT,
                    splitCount = 3,
                    qualityTier = AudioQualityTier.HIGH_QUALITY
                )
            )
            assertEquals(3, resSplit.exportedItems.size)

            // 3. Animated GIF89a Loop Export
            val gifItem = trimmer.exportAnimatedGifLoop(tempDir, srcItem, 1_000L, 3_000L, fps = 15)
            val gifFile = File(gifItem.filePath)
            assertTrue(gifFile.exists())
            val headerStr = String(gifFile.readBytes().take(6).toByteArray(), Charsets.US_ASCII)
            assertEquals("GIF89a", headerStr)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `A-006 inspector vault PIN authentication token renamer and duplicate burst finder`() {
        val engine = MediaInspectorAndVaultEngine()
        assertFalse(engine.isVaultUnlocked())
        assertFalse(engine.unlockVaultWithPin("9999"))
        assertTrue(engine.unlockVaultWithPin("2026"))
        assertTrue(engine.isVaultUnlocked())
        assertFalse(engine.isDecoyMode())

        engine.lockVault()
        assertTrue(engine.unlockVaultWithPin("0000"))
        assertTrue(engine.isDecoyMode())

        val previews = engine.previewBatchTokenRename(sampleItems().take(2), "{date}_{album}_{counter}")
        assertEquals(2, previews.size)
        assertTrue(previews[0].newName.endsWith("_001.flac"))

        val dupGroups = engine.findDuplicatesAndBurstGroups(sampleItems())
        assertTrue(dupGroups.isNotEmpty())
        assertTrue(dupGroups.first().reclaimableBytes > 0L)
    }
}
