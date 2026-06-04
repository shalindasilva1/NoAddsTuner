package com.example.tuner.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sin

class YinPitchDetectorTest {

    @Test
    fun testDetectPitch_PureSineWave() {
        val sampleRate = 44100f
        val targetFreq = 440f // A4 note
        val bufferSize = 2048
        val buffer = FloatArray(bufferSize)

        // Generate pure sine wave at 440Hz
        for (i in 0 until bufferSize) {
            buffer[i] = sin(2.0 * Math.PI * targetFreq * i / sampleRate).toFloat()
        }

        val detector = YinPitchDetector()
        val detectedFreq = detector.detectPitch(buffer, sampleRate)

        // The YIN detector with parabolic interpolation should be accurate within 1.0 Hz
        assertEquals(targetFreq, detectedFreq, 1.0f)
    }

    @Test
    fun testDetectPitch_GuitarLowE() {
        val sampleRate = 44100f
        val targetFreq = 82.41f // Guitar E2 string
        val bufferSize = 4096
        val buffer = FloatArray(bufferSize)

        for (i in 0 until bufferSize) {
            buffer[i] = sin(2.0 * Math.PI * targetFreq * i / sampleRate).toFloat()
        }

        val detector = YinPitchDetector()
        val detectedFreq = detector.detectPitch(buffer, sampleRate, minFreq = 60f)

        // Should be within 0.5Hz for E2
        assertEquals(targetFreq, detectedFreq, 0.5f)
    }
}
