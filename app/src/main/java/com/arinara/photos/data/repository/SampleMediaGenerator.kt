// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.data.repository

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object SampleMediaGenerator {

    fun purgeLegacySamples(context: Context) {
        try {
            val legacySamples = File(context.filesDir, "studio_samples")
            if (legacySamples.exists()) {
                legacySamples.deleteRecursively()
            }
            val legacyExports = File(context.filesDir, "studio_exports")
            if (legacyExports.exists()) {
                legacyExports.deleteRecursively()
            }
        } catch (_: Exception) {
        }
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
            header.putInt(16)
            header.putShort(1.toShort())
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
}
