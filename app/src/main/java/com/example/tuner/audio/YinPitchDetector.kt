package com.example.tuner.audio

import kotlin.math.abs

class YinPitchDetector {

    companion object {
        private const val DEFAULT_THRESHOLD = 0.20f
    }

    /**
     * Estimates the pitch of the audio buffer in Hz.
     * Returns -1.0f if no pitch is detected (e.g. noise or silence).
     */
    fun detectPitch(
        buffer: FloatArray,
        sampleRate: Float,
        minFreq: Float = 60f,
        maxFreq: Float = 500f,
        threshold: Float = DEFAULT_THRESHOLD
    ): Float {
        // Guitar standard frequencies are E2 (82.4 Hz) to E4 (329.6 Hz).
        // A search range of 60 Hz to 500 Hz covers all standard, drop, and standard-varied guitar tunings.
        val maxLag = (sampleRate / minFreq).toInt().coerceAtMost(buffer.size / 2)
        val minLag = (sampleRate / maxFreq).toInt().coerceAtLeast(2)

        if (buffer.size < 2 * maxLag) {
            return -1.0f
        }

        val yinBuffer = FloatArray(maxLag)

        // Step 1: Difference function
        // d_tau = sum_{i=0}^{maxLag} (x_i - x_{i+tau})^2
        for (tau in 1 until maxLag) {
            var sum = 0f
            for (i in 0 until maxLag) {
                val diff = buffer[i] - buffer[i + tau]
                sum += diff * diff
            }
            yinBuffer[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference function
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until maxLag) {
            runningSum += yinBuffer[tau]
            if (runningSum > 0f) {
                yinBuffer[tau] = yinBuffer[tau] / (runningSum / tau)
            } else {
                yinBuffer[tau] = 1f
            }
        }

        // Step 3: Absolute thresholding
        // Find the first lag that falls below the threshold and is a local minimum.
        var tau = minLag
        var found = false
        while (tau < maxLag) {
            if (yinBuffer[tau] < threshold) {
                // Keep moving to find the local minimum
                while (tau + 1 < maxLag && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                found = true
                break
            }
            tau++
        }

        // If no lag falls below the threshold, find the absolute minimum in the valid lag range.
        if (!found) {
            var minVal = Float.MAX_VALUE
            var minTau = -1
            for (t in minLag until maxLag) {
                if (yinBuffer[t] < minVal) {
                    minVal = yinBuffer[t]
                    minTau = t
                }
            }
            if (minTau != -1 && yinBuffer[minTau] < 0.35f) { // Max tolerance for noisy signals
                tau = minTau
            } else {
                return -1.0f
            }
        }

        // Step 4: Parabolic interpolation
        var preciseTau = tau.toFloat()
        if (tau > 0 && tau < maxLag - 1) {
            val alpha = yinBuffer[tau - 1]
            val beta = yinBuffer[tau]
            val gamma = yinBuffer[tau + 1]
            val denom = alpha - 2f * beta + gamma
            if (abs(denom) > 1e-5f) {
                preciseTau = tau + 0.5f * (alpha - gamma) / denom
            }
        }

        return if (preciseTau > 0f) sampleRate / preciseTau else -1.0f
    }
}
