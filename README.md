# NoAdsTuner - Ads-Free Android Guitar Tuner & Capo Solver

NoAdsTuner is a lightweight, high-fidelity, and advertisement-free Android Guitar Tuner and Capo Solver app. Designed specifically for stage musicians, it features a sleek Obsidian dark theme, a stabilized spring-animated needle gauge, physical haptic feedback, and a gesture-driven Capo Solver.

Built entirely using **Kotlin**, **Jetpack Compose**, and **Material 3**, the app relies on custom digital signal processing (DSP) rather than heavy external libraries, keeping the build size minimal and performance high.

---

## Key Features

### 🎸 Real-Time Chromatic Tuner
* **YIN DSP Algorithm**: High-accuracy custom implementation of the YIN pitch detection algorithm, utilizing autocorrelation, cumulative mean normalization, absolute thresholding, and parabolic interpolation for sub-sample frequency detection.
* **EMA Frequency Smoothing**: Utilizes an Exponential Moving Average (EMA) filter on the detected frequency to eliminate needle jitter and deliver a steady visual experience.
* **Visual Spring Gauge**: A beautiful custom-drawn analog tuning needle with realistic spring physics.
* **Haptic Taps**: Automatically triggers a short physical haptic vibration on your phone the instant a string reaches the "In-Tune" state (within ±3 cents).
* **Reference Tone Pegs**: Tap any string peg at the bottom to play a pure sine-wave reference pitch. Features soft fade-in envelopes to avoid audio click artifacts.

### 📐 Capo Transposition & Solver
* **Capo Transposition**: Adjust the capo setting (from Fret 0 to 12) directly on the tuner screen. The target string frequencies and note names transpose automatically:
  $$f_{\text{transposed}} = f \times 2^{\text{fret}/12}$$
* **Easy Capo Finder**: A clean, dedicated screen accessible by swiping left/right.
  * **How it works**: Enter the key of your song (e.g., C, F#, Bb) and select **Major** or **Minor**.
  * **Result**: The solver instantly displays cards recommendation indicating which fret to put your capo on and which open chord shape to play (uses CAGED shapes for Major keys, and Am/Em/Dm shapes for Minor keys).

### 🎨 Dark Obsidian Aesthetics
* Sleek Obsidian-dark canvas layout tailored for ultimate stage and low-light visibility.
* High-contrast green (`In-Tune`), orange (`Flat`), and yellow (`Sharp`) accent colors.

---

## Technical Architecture

### UI & Navigation
* **HorizontalPager**: Screen navigation is completely swipable and gesture-based, featuring a subtle custom `PageIndicator` at the top center showing the current active tab (`TUNER` / `CAPO FINDER`).
* **Zero parent-recomposition lag**: State flows are collected locally within the sub-views (`TunerDashboard` and `CapoFinderScreen`) rather than in `MainActivity` or the outer `MainScreen`. This isolates real-time composition updates (30+ FPS during tuning) to the needle/Hz elements, making page swipes buttery smooth.

### Background Services & DSP
* **AudioRecord API**: Captures PCM 16-bit audio at 44.1 kHz on a background `Dispatchers.Default` thread, applying a noise gate (`0.008f` RMS amplitude) to filter out ambient room noise.
* **AudioTrack API**: Writes raw synthesized sine wave short buffers dynamically in background coroutines to play reference tones.
* **Non-Blocking Resource Disposal**: Cleaned up lifecycle hooks so microphone and speaker track disestablishments are executed asynchronously in background coroutines to prevent swipe lag.

---

## Code Structure

* **`com.example.tuner`**
  * 📁 **`audio`**
    * `YinPitchDetector.kt`: Core mathematical DSP implementation of the YIN pitch algorithm.
    * `AudioTunerProcessor.kt`: Controls background `AudioRecord` buffers and feeds them into the detector.
    * `ToneGenerator.kt`: Direct dynamic sine-wave audio output synthesizer.
  * 📁 **`data`**
    * `TuningNote.kt`: Holds target string note information, presets (Standard EADGBE, Drop D, DADGAD, Open G, Half-step Down), and capo transposition mathematical extensions.
  * 📁 **`theme`**
    * `Color.kt` / `Theme.kt`: Stage-focused Obsidian UI color configurations.
  * 📁 **`ui.main`**
    * `MainScreen.kt`: Container managing `HorizontalPager`, top indicator, and the interactive Tuner dashboard.
    * `CapoFinderScreen.kt`: Major/Minor picker screen providing capo step recommenders.
    * `MainScreenViewModel.kt`: Central state management, EMA smoothing filter, and transposition logic.

---

## Build & Run Instructions

### Prerequisites
* Android Studio (Koala or later recommended)
* JDK 17
* An Android device (API 24+) connected via ADB with USB debugging enabled.

### Compilation
Open a terminal in the project directory and run:

```bash
# Build the debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Install & Launch directly onto your connected device
./gradlew installDebug
```

---

## License
NoAdsTuner is open-source and released under the **MIT License**. It is and will always remain completely free and 100% advertisement-free.
