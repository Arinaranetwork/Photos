// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.exifinterface.media.ExifInterface
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object SampleMediaGenerator {

    fun ensureSampleLibrary(context: Context): List<MediaItem> {
        val rootDir = File(context.filesDir, "studio_samples").apply { mkdirs() }
        val nowSec = System.currentTimeMillis() / 1000L
        val items = mutableListOf<MediaItem>()

        // 1. Generate Lossless Audio Tracks (Real playable 16-bit 44.1kHz Stereo WAV files + LRC + CUE)
        val track1File = File(rootDir, "Electric_Slate_Overture_24bit_96kHz.wav")
        if (!track1File.exists()) {
            writeSynthesizedWavFile(
                file = track1File,
                durationSeconds = 18,
                sampleRate = 44100,
                frequenciesHz = doubleArrayOf(220.0, 277.18, 329.63, 440.0, 659.25)
            )
        }
        val track1Lrc = File(rootDir, "Electric_Slate_Overture_24bit_96kHz.lrc")
        if (!track1Lrc.exists()) {
            track1Lrc.writeText(
                """
                [00:00.00] ♪ Electric Slate Overture — Studio Master (24-bit / 96kHz)
                [00:03.00] Precision analog warmth meets zero-latency digital DSP
                [00:06.50] 10-band graphic equalizer & real-time 60fps FFT spectrum active
                [00:10.00] Bit-perfect direct PCM signal path verified with zero clipping
                [00:14.00] Use A-B Section Loop to rehearse or inspect harmonic transients
                """.trimIndent()
            )
        }

        val track2File = File(rootDir, "Midnight_Analog_Horizon_Lossless.wav")
        if (!track2File.exists()) {
            writeSynthesizedWavFile(
                file = track2File,
                durationSeconds = 16,
                sampleRate = 44100,
                frequenciesHz = doubleArrayOf(196.0, 246.94, 293.66, 392.0, 587.33)
            )
        }
        val track2Lrc = File(rootDir, "Midnight_Analog_Horizon_Lossless.lrc")
        if (!track2Lrc.exists()) {
            track2Lrc.writeText(
                """
                [00:00.00] ♪ Midnight Analog Horizon — FLAC / WAV Reference
                [00:03.50] Deep sub-bass foundation at 31Hz and 62Hz bands
                [00:07.00] Switch between 5 independent queues with position memory
                [00:11.50] Headphone crossfeed & ReplayGain normalization ready
                """.trimIndent()
            )
        }

        val track3File = File(rootDir, "Tokyo_Rain_LoFi_Session_320k.wav")
        if (!track3File.exists()) {
            writeSynthesizedWavFile(
                file = track3File,
                durationSeconds = 14,
                sampleRate = 44100,
                frequenciesHz = doubleArrayOf(261.63, 329.63, 392.0, 493.88)
            )
        }

        val sampleCueFile = File(rootDir, "Electric_Slate_Overture_24bit_96kHz.cue")
        if (!sampleCueFile.exists()) {
            sampleCueFile.writeText(
                """
                PERFORMER "Arinara Audio Lab"
                TITLE "Electric Slate Master Sessions"
                FILE "Electric_Slate_Overture_24bit_96kHz.wav" WAVE
                  TRACK 01 AUDIO
                    TITLE "I. Sub-Bass Prelude"
                    PERFORMER "Arinara Audio Lab"
                    INDEX 01 00:00:00
                  TRACK 02 AUDIO
                    TITLE "II. Violet Harmonic Resonance"
                    PERFORMER "Arinara Audio Lab"
                    INDEX 01 00:06:00
                  TRACK 03 AUDIO
                    TITLE "III. Mastering Finale"
                    PERFORMER "Arinara Audio Lab"
                    INDEX 01 00:12:00
                """.trimIndent()
            )
        }

        items.add(
            MediaItem(
                id = "sample_audio_1",
                title = "Electric Slate Overture (Master)",
                artist = "Arinara Audio Lab",
                album = "Reference Masters",
                uriString = track1File.toURI().toString(),
                filePath = track1File.absolutePath,
                mimeType = "audio/wav",
                extension = "FLAC",
                mediaType = MediaType.AUDIO,
                sizeBytes = track1File.length(),
                dateAddedEpochSec = nowSec - 120,
                durationMs = 18_000L,
                bitrateKbps = 4608,
                sampleRateHz = 96000,
                bitDepth = 24,
                channels = 2,
                codecName = "FLAC / PCM",
                qualityBadge = "HI-RES 24B/96K",
                isLossless = true,
                isHiRes = true,
                isFavorite = true,
                dominantColorHex = "#7A5AF8"
            )
        )

        items.add(
            MediaItem(
                id = "sample_audio_2",
                title = "Midnight Analog Horizon",
                artist = "Arinara Audio Lab",
                album = "Reference Masters",
                uriString = track2File.toURI().toString(),
                filePath = track2File.absolutePath,
                mimeType = "audio/wav",
                extension = "WAV",
                mediaType = MediaType.AUDIO,
                sizeBytes = track2File.length(),
                dateAddedEpochSec = nowSec - 240,
                durationMs = 16_000L,
                bitrateKbps = 1411,
                sampleRateHz = 44100,
                bitDepth = 16,
                channels = 2,
                codecName = "WAV PCM",
                qualityBadge = "LOSSLESS WAV",
                isLossless = true,
                isHiRes = false,
                dominantColorHex = "#38BDF8"
            )
        )

        items.add(
            MediaItem(
                id = "sample_audio_3",
                title = "Tokyo Rain Lo-Fi Session",
                artist = "Kira Beats",
                album = "Study & Focus",
                uriString = track3File.toURI().toString(),
                filePath = track3File.absolutePath,
                mimeType = "audio/wav",
                extension = "MP3",
                mediaType = MediaType.AUDIO,
                sizeBytes = track3File.length(),
                dateAddedEpochSec = nowSec - 360,
                durationMs = 14_000L,
                bitrateKbps = 320,
                sampleRateHz = 48000,
                bitDepth = 16,
                channels = 2,
                codecName = "MP3 CBR",
                qualityBadge = "320K HQ",
                isLossless = false,
                isHiRes = false,
                dominantColorHex = "#10B981"
            )
        )

        // 2. Generate High-Resolution Sample Photographs & RAW Pro Stills with real EXIF metadata
        val photo1File = File(rootDir, "Neon_Shibuya_Crossing_ProRAW.jpg")
        if (!photo1File.exists()) {
            writeSamplePhotoWithExif(
                file = photo1File,
                width = 1600,
                height = 1200,
                primaryColor = 0xFF7A5AF8.toInt(),
                secondaryColor = 0xFF0D1117.toInt(),
                accentColor = 0xFF38BDF8.toInt(),
                titleOverlay = "SHIBUYA NEON 50MP RAW",
                make = "Sony",
                model = "ILCE-7RM5",
                focalLength = "35/1",
                fNumber = "1.4",
                iso = "160",
                exposureTime = "1/125"
            )
        }

        val photo2File = File(rootDir, "Alpine_Starlight_Astro_FrameA.jpg")
        if (!photo2File.exists()) {
            writeSamplePhotoWithExif(
                file = photo2File,
                width = 1600,
                height = 1200,
                primaryColor = 0xFF1E293B.toInt(),
                secondaryColor = 0xFF0F172A.toInt(),
                accentColor = 0xFFF59E0B.toInt(),
                titleOverlay = "ASTRO BURST #1 (ISO 800)",
                make = "Nikon",
                model = "Z8",
                focalLength = "24/1",
                fNumber = "1.8",
                iso = "800",
                exposureTime = "4/1"
            )
        }

        val photo3File = File(rootDir, "Alpine_Starlight_Astro_FrameB.jpg")
        if (!photo3File.exists()) {
            writeSamplePhotoWithExif(
                file = photo3File,
                width = 1600,
                height = 1200,
                primaryColor = 0xFF1F2736.toInt(),
                secondaryColor = 0xFF0D1117.toInt(),
                accentColor = 0xFF10B981.toInt(),
                titleOverlay = "ASTRO BURST #2 (SHARP)",
                make = "Nikon",
                model = "Z8",
                focalLength = "24/1",
                fNumber = "2.0",
                iso = "640",
                exposureTime = "4/1"
            )
        }

        val animGifFile = File(rootDir, "Kinetic_Prism_Loop_60fps.jpg")
        if (!animGifFile.exists()) {
            writeSamplePhotoWithExif(
                file = animGifFile,
                width = 1200,
                height = 1200,
                primaryColor = 0xFFEC4899.toInt(),
                secondaryColor = 0xFF7A5AF8.toInt(),
                accentColor = 0xFFF8FAFC.toInt(),
                titleOverlay = "ANIMATED 24-FRAME LOOP",
                make = "Arinara",
                model = "MotionSynth",
                focalLength = "50/1",
                fNumber = "2.8",
                iso = "100",
                exposureTime = "1/60"
            )
        }

        items.add(
            MediaItem(
                id = "sample_photo_1",
                title = "Neon Shibuya Crossing (DNG RAW)",
                artist = "Sony A7R V",
                album = "RAW Studio",
                uriString = photo1File.toURI().toString(),
                filePath = photo1File.absolutePath,
                mimeType = "image/jpeg",
                extension = "DNG",
                mediaType = MediaType.RAW_PHOTO,
                sizeBytes = photo1File.length(),
                dateAddedEpochSec = nowSec - 60,
                width = 1600,
                height = 1200,
                qualityBadge = "RAW DNG",
                isLossless = true,
                isHdr = true,
                isFavorite = true,
                dominantColorHex = "#7A5AF8",
                cameraMakeModel = "Sony ILCE-7RM5",
                focalLengthMm = "35mm",
                apertureFNumber = "f/1.4",
                isoSpeed = "ISO 160",
                exposureTimeSec = "1/125s",
                hasGpsCoordinates = true
            )
        )

        items.add(
            MediaItem(
                id = "sample_photo_2",
                title = "Alpine Starlight Burst A",
                artist = "Nikon Z8",
                album = "Camera Roll",
                uriString = photo2File.toURI().toString(),
                filePath = photo2File.absolutePath,
                mimeType = "image/jpeg",
                extension = "JPG",
                mediaType = MediaType.PHOTO,
                sizeBytes = photo2File.length(),
                dateAddedEpochSec = nowSec - 180,
                width = 1600,
                height = 1200,
                qualityBadge = "PRO JPG",
                dominantColorHex = "#F59E0B",
                cameraMakeModel = "Nikon Z8",
                focalLengthMm = "24mm",
                apertureFNumber = "f/1.8",
                isoSpeed = "ISO 800",
                exposureTimeSec = "4.0s",
                hasGpsCoordinates = true
            )
        )

        items.add(
            MediaItem(
                id = "sample_photo_3",
                title = "Alpine Starlight Burst B (Sharp)",
                artist = "Nikon Z8",
                album = "Camera Roll",
                uriString = photo3File.toURI().toString(),
                filePath = photo3File.absolutePath,
                mimeType = "image/jpeg",
                extension = "JPG",
                mediaType = MediaType.PHOTO,
                sizeBytes = photo3File.length(),
                dateAddedEpochSec = nowSec - 175,
                width = 1600,
                height = 1200,
                qualityBadge = "PRO JPG",
                dominantColorHex = "#10B981",
                cameraMakeModel = "Nikon Z8",
                focalLengthMm = "24mm",
                apertureFNumber = "f/2.0",
                isoSpeed = "ISO 640",
                exposureTimeSec = "4.0s",
                hasGpsCoordinates = true
            )
        )

        items.add(
            MediaItem(
                id = "sample_anim_1",
                title = "Kinetic Prism Sequence",
                artist = "Motion Lab",
                album = "Animated Loops",
                uriString = animGifFile.toURI().toString(),
                filePath = animGifFile.absolutePath,
                mimeType = "image/gif",
                extension = "GIF",
                mediaType = MediaType.ANIMATED,
                sizeBytes = animGifFile.length(),
                dateAddedEpochSec = nowSec - 300,
                durationMs = 4000L,
                width = 1200,
                height = 1200,
                fps = 24f,
                qualityBadge = "GIF ANIM",
                dominantColorHex = "#EC4899"
            )
        )

        // 3. Generate Episodic Theater Videos + Dual Subtitle (.SRT) files for Netflix-style Theater Player
        val subFileEn = File(rootDir, "Cyberpunk_Odyssey_S01E01.en.srt")
        if (!subFileEn.exists()) {
            subFileEn.writeText(
                """
                1
                00:00:00,000 --> 00:00:04,500
                [Intro Theme Playing — Tap '+85s Skip Intro' or Hold for 2.0x Speed]

                2
                00:00:04,500 --> 00:00:09,000
                Welcome to Sector 9. The quantum relay is operating at full capacity.

                3
                00:00:09,000 --> 00:00:14,000
                Activate Bedtime Touch Lock on the left edge so accidental touches never interrupt.

                4
                00:00:14,000 --> 00:00:18,000
                Dual subtitles and searchable transcript jump-to-timestamp are live.
                """.trimIndent()
            )
        }

        val subFileJp = File(rootDir, "Cyberpunk_Odyssey_S01E01.jp.srt")
        if (!subFileJp.exists()) {
            subFileJp.writeText(
                """
                1
                00:00:00,000 --> 00:00:04,500
                [イントロ再生中 — +85秒スキップまたは長押しで2倍速]

                2
                00:00:04,500 --> 00:00:09,000
                セクター9へようこそ。量子リレーは最大出力で稼働中です。

                3
                00:00:09,000 --> 00:00:14,000
                画面ロックを有効にすると、寝ながら視聴しても誤操作を防げます。

                4
                00:00:14,000 --> 00:00:18,000
                デュアル字幕と字幕トランスクリプト検索が利用可能です。
                """.trimIndent()
            )
        }

        items.add(
            MediaItem(
                id = "sample_video_s01e01",
                title = "Cyberpunk Odyssey - S01E01 - Awakening",
                artist = "Arinara Cinema",
                album = "Cyberpunk Odyssey S01",
                uriString = track1File.toURI().toString(),
                filePath = track1File.absolutePath,
                mimeType = "video/mp4",
                extension = "MKV",
                mediaType = MediaType.VIDEO,
                sizeBytes = 48_500_000L,
                dateAddedEpochSec = nowSec - 90,
                durationMs = 18_000L,
                width = 3840,
                height = 2160,
                fps = 60f,
                bitrateKbps = 24500,
                sampleRateHz = 48000,
                bitDepth = 24,
                channels = 6,
                codecName = "HEVC 10-bit + FLAC 5.1",
                qualityBadge = "4K 60",
                isLossless = true,
                isHdr = true,
                isFavorite = true,
                seasonNumber = 1,
                episodeNumber = 1,
                seriesTitle = "Cyberpunk Odyssey",
                resumePositionMs = 6_200L,
                dominantColorHex = "#7A5AF8"
            )
        )

        items.add(
            MediaItem(
                id = "sample_video_s01e02",
                title = "Cyberpunk Odyssey - S01E02 - Ghost Protocol",
                artist = "Arinara Cinema",
                album = "Cyberpunk Odyssey S01",
                uriString = track2File.toURI().toString(),
                filePath = track2File.absolutePath,
                mimeType = "video/mp4",
                extension = "MKV",
                mediaType = MediaType.VIDEO,
                sizeBytes = 52_100_000L,
                dateAddedEpochSec = nowSec - 80,
                durationMs = 16_000L,
                width = 3840,
                height = 2160,
                fps = 60f,
                bitrateKbps = 22800,
                sampleRateHz = 48000,
                bitDepth = 24,
                channels = 6,
                codecName = "HEVC 10-bit + E-AC3",
                qualityBadge = "4K 60",
                isHdr = true,
                seasonNumber = 1,
                episodeNumber = 2,
                seriesTitle = "Cyberpunk Odyssey",
                resumePositionMs = 3_800L,
                dominantColorHex = "#38BDF8"
            )
        )

        items.add(
            MediaItem(
                id = "sample_video_s01e03",
                title = "Cyberpunk Odyssey - S01E03 - Singularity",
                artist = "Arinara Cinema",
                album = "Cyberpunk Odyssey S01",
                uriString = track3File.toURI().toString(),
                filePath = track3File.absolutePath,
                mimeType = "video/mp4",
                extension = "MP4",
                mediaType = MediaType.VIDEO,
                sizeBytes = 39_400_000L,
                dateAddedEpochSec = nowSec - 70,
                durationMs = 14_000L,
                width = 1920,
                height = 1080,
                fps = 24f,
                bitrateKbps = 12400,
                sampleRateHz = 48000,
                bitDepth = 16,
                channels = 2,
                codecName = "AV1 + AAC-LC",
                qualityBadge = "1080p",
                seasonNumber = 1,
                episodeNumber = 3,
                seriesTitle = "Cyberpunk Odyssey",
                resumePositionMs = 0L,
                dominantColorHex = "#10B981"
            )
        )

        return items
    }

    fun writeSynthesizedWavFile(
        file: File,
        durationSeconds: Int,
        sampleRate: Int = 44100,
        frequenciesHz: DoubleArray = doubleArrayOf(220.0, 329.63, 440.0)
    ) {
        val numChannels = 2
        val bitsPerSample = 16
        val totalFrames = durationSeconds * sampleRate
        val dataSize = totalFrames * numChannels * (bitsPerSample / 8)
        val totalFileSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(totalFileSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1.toShort()) // AudioFormat = 1 (PCM)
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * (bitsPerSample / 8))
            header.putShort((numChannels * (bitsPerSample / 8)).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            fos.write(header.array())

            val chunkFrames = 2048
            val pcmBuffer = ByteBuffer.allocate(chunkFrames * numChannels * 2).order(ByteOrder.LITTLE_ENDIAN)
            var frameIdx = 0
            while (frameIdx < totalFrames) {
                pcmBuffer.clear()
                val count = minOf(chunkFrames, totalFrames - frameIdx)
                for (i in 0 until count) {
                    val t = (frameIdx + i).toDouble() / sampleRate.toDouble()
                    val chordIdx = ((t / 2.5).toInt()) % frequenciesHz.size
                    val baseFreq = frequenciesHz[chordIdx]
                    val fifthFreq = baseFreq * 1.4983
                    val octaveFreq = baseFreq * 2.0
                    val subFreq = baseFreq * 0.5

                    val env = (0.65 + 0.35 * sin(2.0 * PI * 0.5 * t))
                    val rawL = (
                        0.40 * sin(2.0 * PI * baseFreq * t) +
                            0.25 * sin(2.0 * PI * fifthFreq * t) +
                            0.20 * sin(2.0 * PI * subFreq * t) +
                            0.15 * sin(2.0 * PI * octaveFreq * t)
                        ) * env
                    val rawR = (
                        0.40 * sin(2.0 * PI * (baseFreq * 1.002) * t) +
                            0.25 * sin(2.0 * PI * fifthFreq * t + 0.4) +
                            0.20 * sin(2.0 * PI * subFreq * t) +
                            0.15 * sin(2.0 * PI * octaveFreq * t + 0.8)
                        ) * env

                    val sampleL = (rawL * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
                    val sampleR = (rawR * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
                    pcmBuffer.putShort(sampleL)
                    pcmBuffer.putShort(sampleR)
                }
                fos.write(pcmBuffer.array(), 0, count * numChannels * 2)
                frameIdx += count
            }
        }
    }

    private fun writeSamplePhotoWithExif(
        file: File,
        width: Int,
        height: Int,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        titleOverlay: String,
        make: String,
        model: String,
        focalLength: String,
        fNumber: String,
        iso: String,
        exposureTime: String
    ) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(secondaryColor, primaryColor, secondaryColor),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.65f,
                height * 0.4f,
                width * 0.45f,
                intArrayOf(accentColor, 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.65f, height * 0.4f, width * 0.45f, glowPaint)

        // Draw high-contrast geometric test grid for Focus Peaking & Magnifier Loupe inspection
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55F8FAFC
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val step = 120
        for (x in 0..width step step) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step step) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        // Draw concentric focus rings
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        for (r in 60..300 step 60) {
            canvas.drawCircle(width * 0.5f, height * 0.5f, r.toFloat(), ringPaint)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            textSize = 48f
            isFakeBoldText = true
        }
        canvas.drawText(titleOverlay, 80f, height - 120f, textPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 30f
        }
        canvas.drawText("$make $model • f/$fNumber • ${exposureTime}s • ISO $iso", 80f, height - 68f, subPaint)

        FileOutputStream(file).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 94, fos)
        }
        bmp.recycle()

        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_MAKE, make)
            exif.setAttribute(ExifInterface.TAG_MODEL, model)
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, focalLength)
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, fNumber)
            exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, iso)
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, exposureTime)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:25 09:30:00")
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "35/1,39/1,34/1")
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "139/1,42/1,2/1")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            exif.saveAttributes()
        } catch (_: Exception) {
        }
    }
}
