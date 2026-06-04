package com.example.tuner.ui.main

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.tuner.theme.*
import com.example.tuner.data.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(context.applicationContext as Application)
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (!hasPermission) {
                PermissionRequestScreen(
                    onGrantClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            } else {
                val pagerState = rememberPagerState(initialPage = 0) { 2 }
                
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.selectTab(pagerState.currentPage)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    PageIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().weight(1f)
                    ) { page ->
                        if (page == 0) {
                            TunerDashboard(
                                viewModel = viewModel,
                                onPresetSelect = { viewModel.selectPreset(it) },
                                onCapoSelect = { viewModel.setCapoFret(it) },
                                onStringClick = { viewModel.toggleReferenceTone(it) },
                                onStartTuning = { viewModel.startTuning() },
                                onStopTuning = { viewModel.stopTuning() },
                                onStopTone = { viewModel.stopTonePlayback() }
                            )
                        } else {
                            CapoFinderScreen(
                                viewModel = viewModel,
                                onSelectionChange = { note, isMinor ->
                                    viewModel.setTargetNoteAndMode(note, isMinor)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(CardBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MicIcon(
                tint = ColorInTune,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Microphone Access Required",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To detect the pitch of your guitar strings, the application needs permission to listen to your instrument's audio.",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorInTune,
                contentColor = DarkBackground
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Grant Permission",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TunerDashboard(
    viewModel: MainScreenViewModel,
    onPresetSelect: (TuningPreset) -> Unit,
    onCapoSelect: (Int) -> Unit,
    onStringClick: (Int) -> Unit,
    onStartTuning: () -> Unit,
    onStopTuning: () -> Unit,
    onStopTone: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var lastInTune by remember { mutableStateOf(false) }

    // Start listening on launch
    LaunchedEffect(Unit) {
        onStartTuning()
    }

    // Auto-trigger haptic vibration when the guitar transitions to "in tune"
    val isInTune = !state.isSilent && abs(state.centsDeviation) <= 3f
    LaunchedEffect(isInTune) {
        if (isInTune && !lastInTune) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastInTune = isInTune
    }

    // Clean up tone/mic when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            onStopTuning()
            onStopTone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 1. Selector Row (Preset & Capo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1.2f)) {
                PresetDropdown(
                    selectedPreset = state.selectedPreset,
                    onPresetSelect = {
                        onPresetSelect(it)
                        onStartTuning() // Resume tuning on selection
                    }
                )
            }
            Box(modifier = Modifier.weight(0.8f)) {
                CapoDropdown(
                    selectedCapo = state.capoFret,
                    onCapoSelect = {
                        onCapoSelect(it)
                        onStartTuning() // Resume tuning on selection
                    }
                )
            }
        }

        // 2. Main Tuner Visualizers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            TunerNeedleGauge(
                centsDeviation = state.centsDeviation,
                isSilent = state.isSilent
            )

            Spacer(modifier = Modifier.height(16.dp))

            NoteDisplay(
                targetNote = state.targetNote,
                centsDeviation = state.centsDeviation,
                currentFrequency = state.currentFrequency,
                isSilent = state.isSilent
            )
        }

        // 3. Guitar String Selection Grid (Bottom)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.activeToneIndex != null) "Listening to reference tone" else "Tap string to play reference tone",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val activeNotes = if (state.capoFret > 0) {
                state.selectedPreset.transpose(state.capoFret).notes
            } else {
                state.selectedPreset.notes
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                activeNotes.forEachIndexed { index, note ->
                    val isActive = state.activeToneIndex == index
                    val isDetected = !state.isSilent && state.targetNote?.displayName == note.displayName

                    GuitarStringPeg(
                        note = note,
                        isActive = isActive,
                        isDetected = isDetected,
                        onClick = {
                            onStringClick(index)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Button: Toggle manual tone listening vs real-time microphone tuning
            Button(
                onClick = {
                    if (state.isRecording) {
                        onStopTuning()
                    } else {
                        onStartTuning()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRecording) ColorFlat.copy(alpha = 0.2f) else SurfaceVariant,
                    contentColor = if (state.isRecording) ColorFlat else TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isRecording) {
                    StopIcon(
                        tint = ColorFlat,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Listen",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isRecording) "Mute Microphone" else "Enable Auto Tuner",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TunerNeedleGauge(centsDeviation: Float, isSilent: Boolean) {
    // Animate needle deviation with a spring effect for high-fidelity physical realism
    val animatedCents by animateFloatAsState(
        targetValue = if (isSilent) 0f else centsDeviation,
        animationSpec = spring(
            dampingRatio = 0.55f, // Dynamic bounce
            stiffness = Spring.StiffnessLow
        ),
        label = "NeedleCents"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height - 10.dp.toPx()
            val radius = height - 30.dp.toPx()

            val startAngle = 210f
            val sweepAngle = 120f

            // 1. Draw outer circular arc path
            drawArc(
                color = SurfaceVariant,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw center "in-tune" sweet spot (glow background)
            drawArc(
                color = ColorInTune.copy(alpha = 0.15f),
                startAngle = 265f,
                sweepAngle = 10f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw Tick Marks along the arc
            val totalTicks = 21
            for (i in 0 until totalTicks) {
                val fraction = i.toFloat() / (totalTicks - 1)
                val angleDeg = startAngle + fraction * sweepAngle
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isCenter = i == 10
                val isMajor = i % 5 == 0

                val tickLength = when {
                    isCenter -> 16.dp.toPx()
                    isMajor -> 10.dp.toPx()
                    else -> 6.dp.toPx()
                }

                val tickColor = when {
                    isCenter -> ColorInTune
                    isMajor -> TextSecondary
                    else -> TextMuted
                }

                val strokeWidth = when {
                    isCenter -> 3.dp.toPx()
                    isMajor -> 2.dp.toPx()
                    else -> 1.2f.dp.toPx()
                }

                val startX = centerX + (radius - tickLength) * Math.cos(angleRad).toFloat()
                val startY = centerY + (radius - tickLength) * Math.sin(angleRad).toFloat()
                val endX = centerX + radius * Math.cos(angleRad).toFloat()
                val endY = centerY + radius * Math.sin(angleRad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw Tuning Needle
            val needleAngleDeg = 270f + animatedCents * 1.2f
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = radius - 8.dp.toPx()

            val needleColor = when {
                isSilent -> ColorMutedNeedle
                abs(centsDeviation) <= 3f -> ColorInTune
                centsDeviation < 0f -> ColorFlat
                else -> ColorSharp
            }

            val needleEndX = centerX + needleLength * Math.cos(needleAngleRad).toFloat()
            val needleEndY = centerY + needleLength * Math.sin(needleAngleRad).toFloat()

            // Draw a subtle needle glow
            if (!isSilent) {
                drawLine(
                    color = needleColor.copy(alpha = 0.25f),
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw primary needle line
            drawLine(
                color = needleColor,
                start = Offset(centerX, centerY),
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 2.5f.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Draw base pivot hub circles
            drawCircle(
                color = CardBackground,
                radius = 12.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = needleColor,
                radius = 5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}

@Composable
fun NoteDisplay(
    targetNote: TuningNote?,
    centsDeviation: Float,
    currentFrequency: Float,
    isSilent: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = Color.Black)
                .background(CardBackground, CircleShape)
                .border(
                    width = 2.dp,
                    color = when {
                        isSilent -> SurfaceVariant
                        abs(centsDeviation) <= 3f -> ColorInTune
                        centsDeviation < 0f -> ColorFlat
                        else -> ColorSharp
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isSilent || targetNote == null) {
                    MicIcon(
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "READY",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                } else {
                    Text(
                        text = targetNote.name,
                        color = TextPrimary,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Octave ${targetNote.octave}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val (line1Text, line1Color, line1Size) = if (isSilent || targetNote == null) {
                Triple("Pluck a string...", TextSecondary, 16.sp)
            } else {
                val devText = when {
                    abs(centsDeviation) <= 3f -> "IN TUNE"
                    centsDeviation < 0f -> "${abs(centsDeviation.roundToInt())} cents flat"
                    else -> "${centsDeviation.roundToInt()} cents sharp"
                }
                val devColor = when {
                    abs(centsDeviation) <= 3f -> ColorInTune
                    centsDeviation < 0f -> ColorFlat
                    else -> ColorSharp
                }
                Triple(devText, devColor, 18.sp)
            }

            val line2Text = if (isSilent || targetNote == null) {
                ""
            } else {
                String.format("%.1f Hz / Target: %.1f Hz", currentFrequency, targetNote.frequency)
            }

            Text(
                text = line1Text,
                color = line1Color,
                fontSize = line1Size,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (line2Text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = line2Text,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun GuitarStringPeg(
    note: TuningNote,
    isActive: Boolean,
    isDetected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive || isDetected) 1.15f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "StringPegScale"
    )

    val borderBrush = if (isActive) {
        Brush.sweepGradient(listOf(ColorInTune, ColorSharp, ColorInTune))
    } else {
        null
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .size(54.dp)
            .shadow(
                elevation = if (isActive) 8.dp else 2.dp,
                shape = CircleShape
            )
            .background(
                color = when {
                    isActive -> ColorInTune.copy(alpha = 0.25f)
                    isDetected -> ColorInTune
                    else -> CardBackground
                },
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = when {
                    isActive -> ColorInTune
                    isDetected -> ColorInTune
                    else -> SurfaceVariant
                },
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = note.name,
                color = when {
                    isActive -> ColorInTune
                    isDetected -> DarkBackground
                    else -> TextPrimary
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = note.octave.toString(),
                color = when {
                    isActive -> ColorInTune.copy(alpha = 0.7f)
                    isDetected -> DarkBackground.copy(alpha = 0.7f)
                    else -> TextSecondary
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PresetDropdown(
    selectedPreset: TuningPreset,
    onPresetSelect: (TuningPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                MicIcon(
                    tint = ColorInTune,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedPreset.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand dropdown",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            TuningPreset.ALL_PRESETS.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = preset.name,
                            color = if (preset == selectedPreset) ColorInTune else TextPrimary,
                            fontWeight = if (preset == selectedPreset) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onPresetSelect(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MicIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Draw capsule
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f, w * 0.15f)
        )
        
        // 2. Draw stand (U-shape)
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, w * 0.5f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // 3. Draw vertical stem
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.75f),
            end = Offset(w * 0.5f, h * 0.9f),
            strokeWidth = 2.dp.toPx()
        )
        
        // 4. Draw horizontal base
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.9f),
            end = Offset(w * 0.65f, h * 0.9f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun StopIcon(modifier: Modifier = Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(color = tint, shape = RoundedCornerShape(2.dp))
    )
}

@Composable
fun CapoDropdown(
    selectedCapo: Int,
    onCapoSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (selectedCapo == 0) "No Capo" else "Capo $selectedCapo",
                    color = if (selectedCapo > 0) ColorSharp else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand dropdown",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            for (fret in 0..12) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (fret == 0) "No Capo (Open)" else "Capo Fret $fret",
                            color = if (fret == selectedCapo) ColorSharp else TextPrimary,
                            fontWeight = if (fret == selectedCapo) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onCapoSelect(fret)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val isSelected = pagerState.currentPage == index
                val size by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 6.dp,
                    label = "IndicatorSize"
                )
                val color = if (isSelected) ColorInTune else TextMuted
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(size)
                        .background(color, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (pagerState.currentPage == 0) "TUNER" else "CAPO FINDER",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
