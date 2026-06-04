package com.example.tuner.data

import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.pow

data class TuningNote(
    val name: String,
    val frequency: Float,
    val octave: Int
) {
    val displayName: String
        get() = "$name$octave"
}

data class TuningPreset(
    val name: String,
    val notes: List<TuningNote> // Ordered from lowest pitch to highest pitch (typically strings 6 down to 1)
) {
    companion object {
        val ALL_PRESETS = listOf(
            TuningPreset(
                "Standard (EADGBE)",
                listOf(
                    TuningNote("E", 82.41f, 2),
                    TuningNote("A", 110.00f, 2),
                    TuningNote("D", 146.83f, 3),
                    TuningNote("G", 196.00f, 3),
                    TuningNote("B", 246.94f, 3),
                    TuningNote("E", 329.63f, 4)
                )
            ),
            TuningPreset(
                "Drop D (DADGBE)",
                listOf(
                    TuningNote("D", 73.42f, 2),
                    TuningNote("A", 110.00f, 2),
                    TuningNote("D", 146.83f, 3),
                    TuningNote("G", 196.00f, 3),
                    TuningNote("B", 246.94f, 3),
                    TuningNote("E", 329.63f, 4)
                )
            ),
            TuningPreset(
                "Half-Step Down (D#G#C#F#A#D#)",
                listOf(
                    TuningNote("Eb", 77.78f, 2),
                    TuningNote("Ab", 103.83f, 2),
                    TuningNote("Db", 138.59f, 3),
                    TuningNote("Gb", 185.00f, 3),
                    TuningNote("Bb", 233.08f, 3),
                    TuningNote("Eb", 311.13f, 4)
                )
            ),
            TuningPreset(
                "DADGAD",
                listOf(
                    TuningNote("D", 73.42f, 2),
                    TuningNote("A", 110.00f, 2),
                    TuningNote("D", 146.83f, 3),
                    TuningNote("G", 196.00f, 3),
                    TuningNote("A", 220.00f, 3),
                    TuningNote("D", 293.66f, 4)
                )
            ),
            TuningPreset(
                "Open G (DGDGBD)",
                listOf(
                    TuningNote("D", 73.42f, 2),
                    TuningNote("G", 98.00f, 2),
                    TuningNote("D", 146.83f, 3),
                    TuningNote("G", 196.00f, 3),
                    TuningNote("B", 246.94f, 3),
                    TuningNote("D", 293.66f, 4)
                )
            )
        )
    }
}

/**
 * Helper to calculate cents deviation between a frequency and target frequency.
 * Formula: cents = 1200 * log2(f / f_target)
 */
fun calculateCentsDeviation(frequency: Float, targetFrequency: Float): Float {
    if (frequency <= 0f || targetFrequency <= 0f) return 0f
    return 1200f * log2(frequency / targetFrequency)
}

/**
 * Returns the closest note in a preset for a given frequency.
 */
fun findClosestNote(frequency: Float, preset: TuningPreset): TuningNote {
    var closestNote = preset.notes.first()
    var minDiff = Float.MAX_VALUE
    
    for (note in preset.notes) {
        val diff = kotlin.math.abs(frequency - note.frequency)
        if (diff < minDiff) {
            minDiff = diff
            closestNote = note
        }
    }
    
    return closestNote
}

/**
 * Transposes a TuningNote up by a number of semitones.
 */
fun TuningNote.transpose(semitones: Int): TuningNote {
    if (semitones == 0) return this
    val scale = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    
    val normalizedName = when (name) {
        "Db" -> "C#"
        "Eb" -> "D#"
        "Gb" -> "F#"
        "Ab" -> "G#"
        "Bb" -> "A#"
        else -> name
    }
    
    val index = scale.indexOf(normalizedName)
    if (index == -1) {
        val newFreq = frequency * 2.0.pow(semitones.toDouble() / 12.0).toFloat()
        return TuningNote(name, newFreq, octave)
    }
    
    val targetIndex = (index + semitones) % 12
    val octaveShift = (index + semitones) / 12
    val newName = scale[targetIndex]
    val newOctave = octave + octaveShift
    val newFreq = frequency * 2.0.pow(semitones.toDouble() / 12.0).toFloat()
    
    return TuningNote(newName, newFreq, newOctave)
}

/**
 * Transposes an entire TuningPreset up by a number of semitones.
 */
fun TuningPreset.transpose(semitones: Int): TuningPreset {
    if (semitones == 0) return this
    return TuningPreset(
        name = name,
        notes = notes.map { it.transpose(semitones) }
    )
}
