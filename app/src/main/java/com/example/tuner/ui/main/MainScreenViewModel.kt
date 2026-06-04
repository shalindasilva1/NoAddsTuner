package com.example.tuner.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tuner.audio.AudioTunerProcessor
import com.example.tuner.audio.ToneGenerator
import com.example.tuner.data.TuningNote
import com.example.tuner.data.TuningPreset
import com.example.tuner.data.calculateCentsDeviation
import com.example.tuner.data.findClosestNote
import com.example.tuner.data.transpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CapoOption(
    val fret: Int,
    val playKey: String
)

data class TunerUiState(
    // Tuner Tab States
    val currentFrequency: Float = -1f,
    val amplitude: Float = 0f,
    val isSilent: Boolean = true,
    val selectedPreset: TuningPreset = TuningPreset.ALL_PRESETS.first(),
    val targetNote: TuningNote? = null,
    val centsDeviation: Float = 0f,
    val isRecording: Boolean = false,
    val activeToneIndex: Int? = null, // Index of preset notes currently playing
    val capoFret: Int = 0,            // Pitch offset for tuner tab (0 to 12 semitones)
    
    // Tab Navigation State
    val selectedTab: Int = 0,         // 0 = Tuner, 1 = Capo Finder
    
    // Capo Finder Tab States
    val selectedTargetNote: String = "C", // Song key root note (C, C#, etc.)
    val isMinor: Boolean = false,         // True if key is minor, false if major
    val capoOptions: List<CapoOption> = emptyList() // Calculated optimal capo placements
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val audioProcessor = AudioTunerProcessor(application)
    private val toneGenerator = ToneGenerator()

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var smoothedFrequency = -1f

    init {
        // Calculate initial capo finder key options (C Major)
        calculateCapoOptions("C", false)

        // Collect pitch detection results from AudioProcessor
        viewModelScope.launch {
            audioProcessor.tunerResult.collect { result ->
                if (result.isSilent || result.frequency <= 0f) {
                    smoothedFrequency = -1f
                    _uiState.update { state ->
                        state.copy(
                            currentFrequency = -1f,
                            amplitude = result.amplitude,
                            isSilent = true,
                            targetNote = null,
                            centsDeviation = 0f
                        )
                    }
                } else {
                    // Apply exponential moving average (smoothing filter) to avoid needle jitter
                    smoothedFrequency = if (smoothedFrequency < 0f) {
                        result.frequency
                    } else {
                        val alpha = 0.25f // Adjust responsiveness (lower is smoother)
                        alpha * result.frequency + (1f - alpha) * smoothedFrequency
                    }

                    _uiState.update { state ->
                        // Transpose the preset strings if capo mode is active
                        val activePreset = if (state.capoFret > 0) {
                            state.selectedPreset.transpose(state.capoFret)
                        } else {
                            state.selectedPreset
                        }

                        val closest = findClosestNote(smoothedFrequency, activePreset)
                        val cents = calculateCentsDeviation(smoothedFrequency, closest.frequency)
                        
                        state.copy(
                            currentFrequency = smoothedFrequency,
                            amplitude = result.amplitude,
                            isSilent = false,
                            targetNote = closest,
                            centsDeviation = cents.coerceIn(-50f, 50f)
                        )
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // Tab Navigation Actions
    // ----------------------------------------------------
    fun selectTab(tabIndex: Int) {
        stopTonePlayback()
        if (tabIndex == 1) {
            stopTuning() // Save battery by disabling microphone on Capo Finder screen
        } else {
            startTuning() // Automatically resume microphone on Tuner screen
        }
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    // ----------------------------------------------------
    // Tuner Tab Actions
    // ----------------------------------------------------
    fun startTuning() {
        if (_uiState.value.isRecording) return
        stopTonePlayback() // Stop reference tone
        _uiState.update { it.copy(isRecording = true) }
        audioProcessor.start(viewModelScope)
    }

    fun stopTuning() {
        if (!_uiState.value.isRecording) return
        _uiState.update { it.copy(isRecording = false) }
        audioProcessor.stop()
    }

    fun selectPreset(preset: TuningPreset) {
        stopTonePlayback()
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun setCapoFret(fret: Int) {
        stopTonePlayback()
        _uiState.update { it.copy(capoFret = fret) }
    }

    fun toggleReferenceTone(noteIndex: Int) {
        val currentState = _uiState.value
        if (currentState.activeToneIndex == noteIndex) {
            stopTonePlayback()
        } else {
            stopTuning() // Stop listening to mic
            
            // Play the transposed pitch
            val activePreset = if (currentState.capoFret > 0) {
                currentState.selectedPreset.transpose(currentState.capoFret)
            } else {
                currentState.selectedPreset
            }
            val note = activePreset.notes[noteIndex]
            
            _uiState.update { it.copy(activeToneIndex = noteIndex) }
            toneGenerator.startTone(note.frequency, viewModelScope)
        }
    }

    fun stopTonePlayback() {
        if (_uiState.value.activeToneIndex != null) {
            toneGenerator.stopTone()
            _uiState.update { it.copy(activeToneIndex = null) }
        }
    }

    // ----------------------------------------------------
    // Capo Finder Tab Actions
    // ----------------------------------------------------
    fun setTargetNoteAndMode(note: String, isMinor: Boolean) {
        _uiState.update { it.copy(selectedTargetNote = note, isMinor = isMinor) }
        calculateCapoOptions(note, isMinor)
    }

    private fun calculateCapoOptions(targetNote: String, isMinor: Boolean) {
        val chromaticScale = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val targetIndex = chromaticScale.indexOf(targetNote)
        if (targetIndex == -1) return

        val options = mutableListOf<CapoOption>()
        
        // Open shapes list based on major vs minor
        val openShapes = if (isMinor) {
            listOf("A", "E", "D") // maps to Am, Em, Dm
        } else {
            listOf("C", "A", "G", "E", "D") // maps to C, A, G, E, D
        }

        for (fret in 0..11) {
            val playKeyIndex = (targetIndex - fret + 12) % 12
            val playKey = chromaticScale[playKeyIndex]
            if (openShapes.contains(playKey)) {
                val shapeName = if (isMinor) "${playKey}m" else playKey
                options.add(CapoOption(fret, shapeName))
            }
        }
        
        options.sortBy { it.fret }
        _uiState.update { it.copy(capoOptions = options) }
    }

    override fun onCleared() {
        super.onCleared()
        audioProcessor.stop()
        toneGenerator.stopTone()
    }
}
