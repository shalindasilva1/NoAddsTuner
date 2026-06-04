package com.example.tuner.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class TunerResult(
    val frequency: Float,
    val amplitude: Float,
    val isSilent: Boolean
)

class AudioTunerProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AudioTunerProcessor"
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
        private const val NOISE_GATE_THRESHOLD = 0.008f // Amplitude threshold below which we ignore signal
    }

    private val pitchDetector = YinPitchDetector()
    private val _tunerResult = MutableStateFlow(TunerResult(-1f, 0f, true))
    val tunerResult: StateFlow<TunerResult> = _tunerResult.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRecording) return
        isRecording = true

        recordingJob = scope.launch(Dispatchers.Default) {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val recordBufferSize = minBufferSize.coerceAtLeast(BUFFER_SIZE * 2)
            
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    recordBufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed")
                    _tunerResult.value = TunerResult(-1f, 0f, true)
                    isRecording = false
                    return@launch
                }

                audioRecord?.startRecording()
                Log.d(TAG, "Audio recording started")

                val audioBuffer = ShortArray(BUFFER_SIZE)
                val floatBuffer = FloatArray(BUFFER_SIZE)

                while (isRecording) {
                    val readResult = audioRecord?.read(audioBuffer, 0, BUFFER_SIZE) ?: -1
                    if (readResult <= 0) continue

                    // 1. Convert PCM to Floats scaled between -1.0 and 1.0, and calculate RMS amplitude
                    var sumSquared = 0f
                    for (i in 0 until readResult) {
                        val sample = audioBuffer[i] / 32768f
                        floatBuffer[i] = sample
                        sumSquared += sample * sample
                    }
                    val rms = sqrt(sumSquared / readResult)

                    // 2. Check if RMS amplitude is above noise gate
                    if (rms < NOISE_GATE_THRESHOLD) {
                        _tunerResult.value = TunerResult(-1f, rms, true)
                        continue
                    }

                    // 3. Perform pitch detection
                    val detectedPitch = pitchDetector.detectPitch(floatBuffer, SAMPLE_RATE.toFloat())

                    _tunerResult.value = TunerResult(
                        frequency = detectedPitch,
                        amplitude = rms,
                        isSilent = false
                    )
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission is missing", e)
                _tunerResult.value = TunerResult(-1f, 0f, true)
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio recording loop", e)
            } finally {
                releaseResources()
            }
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        _tunerResult.value = TunerResult(-1f, 0f, true)
    }

    private fun releaseResources() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord resources", e)
        }
        audioRecord = null
    }
}
