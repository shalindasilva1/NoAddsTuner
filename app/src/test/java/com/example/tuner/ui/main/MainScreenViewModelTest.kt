package com.example.tuner.ui.main

import com.example.tuner.data.TuningNote
import com.example.tuner.data.TuningPreset
import com.example.tuner.data.calculateCentsDeviation
import com.example.tuner.data.findClosestNote
import com.example.tuner.data.transpose
import org.junit.Assert.assertEquals
import org.junit.Test

class TuningCalculationsTest {

    @Test
    fun testCentsDeviation() {
        // Frequency is exactly equal, deviation should be 0
        assertEquals(0f, calculateCentsDeviation(440f, 440f), 0.01f)

        // One octave up (double frequency) is exactly +1200 cents
        assertEquals(1200f, calculateCentsDeviation(880f, 440f), 0.01f)

        // One octave down (half frequency) is exactly -1200 cents
        assertEquals(-1200f, calculateCentsDeviation(220f, 440f), 0.01f)

        // Check a typical deviation (e.g. Eb vs E)
        // E2 is 82.41 Hz, Eb2 is 77.78 Hz. That's -100 cents (1 semitone)
        assertEquals(-100f, calculateCentsDeviation(77.78f, 82.41f), 1.0f)
    }

    @Test
    fun testFindClosestNote() {
        val standardPreset = TuningPreset.ALL_PRESETS.first() // Standard EADGBE
        
        // 83 Hz should map to E2 (82.41 Hz)
        val note1 = findClosestNote(83.0f, standardPreset)
        assertEquals("E", note1.name)
        assertEquals(2, note1.octave)

        // 112 Hz should map to A2 (110 Hz)
        val note2 = findClosestNote(112.0f, standardPreset)
        assertEquals("A", note2.name)

        // 328 Hz should map to E4 (329.63 Hz)
        val note3 = findClosestNote(328.0f, standardPreset)
        assertEquals("E", note3.name)
        assertEquals(4, note3.octave)
    }

    @Test
    fun testTransposeNote() {
        val lowE = TuningNote("E", 82.41f, 2)
        
        // E2 transposed 1 semitone up is F2
        val transposed1 = lowE.transpose(1)
        assertEquals("F", transposed1.name)
        assertEquals(2, transposed1.octave)
        assertEquals(87.31f, transposed1.frequency, 0.1f) // 82.41 * 2^(1/12) ≈ 87.31
        
        // E2 transposed 12 semitones up is E3
        val transposed12 = lowE.transpose(12)
        assertEquals("E", transposed12.name)
        assertEquals(3, transposed12.octave)
        assertEquals(164.82f, transposed12.frequency, 0.2f)
    }

    @Test
    fun testTransposePreset() {
        val standardPreset = TuningPreset.ALL_PRESETS.first() // EADGBE
        
        // Capo on fret 3: standard EADGBE becomes G C F Bb D G
        val transposedPreset = standardPreset.transpose(3)
        assertEquals("G", transposedPreset.notes[0].name)
        assertEquals("C", transposedPreset.notes[1].name)
        assertEquals("F", transposedPreset.notes[2].name)
        assertEquals("A#", transposedPreset.notes[3].name) // A# is Bb
        assertEquals("D", transposedPreset.notes[4].name)
        assertEquals("G", transposedPreset.notes[5].name)
    }
}
