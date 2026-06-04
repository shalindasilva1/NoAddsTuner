package com.example.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.sin

class ToneGenerator {

    companion object {
        private const val TAG = "ToneGenerator"
        private const val SAMPLE_RATE = 44100
        private const val FADE_SAMPLES = 2048 // Number of samples to ramp up/down to prevent popping noise
    }

    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null
    private var isPlaying = false

    fun startTone(frequency: Float, scope: CoroutineScope) {
        stopTone() // Reset active playback
        isPlaying = true

        playJob = scope.launch(Dispatchers.Default) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(2048))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val bufferSize = 1024
                val buffer = ShortArray(bufferSize)
                var angle = 0.0
                val angleIncrement = (2.0 * Math.PI * frequency) / SAMPLE_RATE
                
                var totalSamplesWritten = 0L

                while (isPlaying) {
                    for (i in 0 until bufferSize) {
                        var amplitude = 0.6f // Standard target amplitude to keep it pleasant
                        
                        // Apply fade-in to prevent sharp clicking noise when starting
                        if (totalSamplesWritten < FADE_SAMPLES) {
                            amplitude *= (totalSamplesWritten.toFloat() / FADE_SAMPLES)
                        }

                        buffer[i] = (sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
                        angle += angleIncrement
                        if (angle >= 2.0 * Math.PI) {
                            angle -= 2.0 * Math.PI
                        }
                        totalSamplesWritten++
                    }
                    audioTrack?.write(buffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio reference tone", e)
            } finally {
                releaseResources()
            }
        }
    }

    fun stopTone() {
        isPlaying = false
        playJob?.cancel()
        playJob = null
    }

    private fun releaseResources() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack resources", e)
        }
        audioTrack = null
    }
}
