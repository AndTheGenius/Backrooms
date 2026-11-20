package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.AmbientSoundEngine
import com.example.engine.AimTarget
import com.example.engine.AnomalyCatalog
import com.example.engine.BackroomsBiome
import com.example.engine.RaycasterRenderer
import com.example.ui.BackroomsViewModel
import com.example.ui.HapticEvent
import com.example.ui.PlayHudState
import com.example.ui.RunPhase
import com.example.ui.components.VhsBannerOverlay
import com.example.ui.components.VhsScanButton
import com.example.ui.components.ViewfinderReticleOverlay
import com.example.ui.components.VirtualThumbstick
import com.example.ui.components.formatVhsTimestamp
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CarnivalMagenta
import com.example.ui.theme.CrtVoid
import com.example.ui.theme.FluorescentYellow
import com.example.ui.theme.GardenEmerald
import com.example.ui.theme.GymSteel
import com.example.ui.theme.MutedTan
import com.example.ui.theme.PharmacyMint
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.PoolAqua
import com.example.ui.theme.RecCrimson
import com.example.ui.theme.StaticHairline
import com.example.ui.theme.TapeHousing
import com.example.ui.theme.TapeHousingElevated
import com.example.ui.theme.TapeWhite
import com.example.ui.theme.WarningAmber

@Composable
fun BackroomsPlayScreen(
    viewModel: BackroomsViewModel,
    onExitToDeck: () -> Unit,
    onOpenCodex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val renderer = remember { RaycasterRenderer(width = 240, height = 150) }
    val soundEngine = remember { AmbientSoundEngine() }

    // Continuous input state sampled each frame without causing recompositions
    var forwardInput by remember { mutableFloatStateOf(0f) }
    var strafeInput by remember { mutableFloatStateOf(0f) }
    var pendingTurnDelta by remember { mutableFloatStateOf(0f) }
    var currentFovPlane by remember { mutableFloatStateOf(0.66f) }
    var currentAimTarget by remember { mutableStateOf<AimTarget?>(null) }
    var frameCounter by remember { mutableIntStateOf(0) }

    // Start / stop synthesized fluorescent hum audio with screen lifecycle
    DisposableEffect(Unit) {
        soundEngine.start(scope)
        onDispose { soundEngine.stop() }
    }

    // Keep audio engine parameters synced with HUD state (across all 8 Backrooms environments)
    LaunchedEffect(hud.isMuted, hud.phase, hud.distortionIntensity, hud.isScanning, hud.currentBiome) {
        soundEngine.isMuted = hud.isMuted || hud.phase != RunPhase.PLAYING
        soundEngine.distortionIntensity = hud.distortionIntensity
        soundEngine.isScanning = hud.isScanning
        soundEngine.currentBiome = hud.currentBiome
    }

    // Intercept system back button during active walk to pause safely
    BackHandler(enabled = hud.phase == RunPhase.PLAYING) {
        viewModel.pauseRun()
    }

    // Real-time 60fps simulation & raycasting loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val state = viewModel.hudState.value

                    // Smoothly interpolate optical zoom FOV plane
                    val targetFov = if (state.isZoomed) 0.34f else 0.66f
                    currentFovPlane += (targetFov - currentFovPlane) * (dt * 12f).coerceAtMost(1f)

                    if (state.phase == RunPhase.PLAYING) {
                        val turn = pendingTurnDelta
                        pendingTurnDelta = 0f

                        viewModel.stepFrame(
                            dt = dt,
                            forwardInput = forwardInput,
                            strafeInput = strafeInput,
                            turnDelta = turn,
                            currentAim = currentAimTarget,
                            onFootstep = { soundEngine.playFootstep() },
                            onHaptic = { event ->
                                when (event) {
                                    HapticEvent.CONFIRM -> {
                                        soundEngine.playAnomalyConfirm()
                                        triggerHaptic(context, 40L)
                                    }
                                    HapticEvent.ERROR -> {
                                        soundEngine.playFalsePositive()
                                        triggerHaptic(context, 110L)
                                    }
                                    HapticEvent.SHIFT -> {
                                        soundEngine.playGeometryShift()
                                        triggerHaptic(context, 28L)
                                    }
                                }
                            }
                        )
                    }

                    val updated = viewModel.hudState.value
                    currentAimTarget = renderer.render(
                        world = viewModel.world,
                        fovPlane = currentFovPlane,
                        timeSeconds = updated.elapsedSeconds,
                        distortionIntensity = updated.distortionIntensity,
                        distortionTier = updated.distortionStage.tier,
                        staticBurst = updated.staticBurst,
                        scanActive = updated.isScanning
                    )
                    frameCounter++
                }
                lastNanos = now
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrtVoid)
    ) {
        // 1. FULL-BLEED 3D RAYCASTED VIEWPORT WITH DRAG-TO-LOOK
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("raycaster_viewport")
                .pointerInput(hud.isZoomed, hud.phase) {
                    detectDragGestures { change, dragAmount ->
                        if (hud.phase == RunPhase.PLAYING) {
                            change.consume()
                            val sensitivity = if (hud.isZoomed) 0.0026f else 0.0052f
                            pendingTurnDelta += dragAmount.x * sensitivity
                        }
                    }
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameCounter
            drawImage(
                image = renderer.imageBitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }

        // 2. VIEWFINDER RETICLE & VIGNETTE
        ViewfinderReticleOverlay(
            hasTarget = hud.hasTargetInRange,
            targetDistance = hud.lockedTargetDistance,
            isScanning = hud.isScanning,
            scanProgress = hud.scanProgress,
            isZoomed = hud.isZoomed,
            distortionTier = hud.distortionStage.tier
        )

        // 3. TOP CAMCORDER VHS OSD HEADER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: REC indicator, timestamp, and distance walked
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val blinkOn = ((hud.elapsedSeconds * 2f).toInt() % 2) == 0
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (blinkOn) RecCrimson else RecCrimson.copy(alpha = 0.25f))
                        )
                        Text(
                            text = stringResource(R.string.hud_rec),
                            style = MaterialTheme.typography.labelLarge,
                            color = TapeWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatVhsTimestamp(hud.elapsedSeconds, hud.distortionStage.tier),
                            style = MaterialTheme.typography.labelMedium,
                            color = TapeWhite.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudReadoutPill(
                            text = "DIST %.1fm".format(hud.distanceMeters),
                            accent = FluorescentYellow
                        )
                        HudReadoutPill(
                            text = "${hud.distortionStage.code}: ${hud.distortionStage.title}",
                            accent = when {
                                hud.distortionStage.tier >= 4 -> RecCrimson
                                hud.distortionStage.tier >= 2 -> WarningAmber
                                else -> PhosphorGreen
                            }
                        )
                    }
                }

                // Right: Audio Mute & Pause Controls (48dp touch targets)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CrtVoid.copy(alpha = 0.58f))
                            .testTag("audio_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (hud.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_audio_toggle),
                            tint = if (hud.isMuted) MutedTan else TapeWhite
                        )
                    }
                    IconButton(
                        onClick = { viewModel.pauseRun() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CrtVoid.copy(alpha = 0.58f))
                            .testTag("pause_tape_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = stringResource(R.string.cd_pause_tape),
                            tint = TapeWhite
                        )
                    }
                }
            }

            // Dynamic Environment Location Pill (8 Biomes) + Anomalies Logged Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val zoneColor = when (hud.currentBiome) {
                    BackroomsBiome.YELLOW_HALLS -> FluorescentYellow
                    BackroomsBiome.POOLROOMS -> PoolAqua
                    BackroomsBiome.AMUSEMENT_PARK -> CarnivalMagenta
                    BackroomsBiome.GYM -> GymSteel
                    BackroomsBiome.GARDEN -> GardenEmerald
                    BackroomsBiome.CAMERA_ROOM -> PhosphorGreen
                    BackroomsBiome.PHARMACY -> PharmacyMint
                    BackroomsBiome.ANTIQUE_SHOP -> AntiqueGold
                }
                HudReadoutPill(
                    text = hud.currentBiome.hudLocation,
                    accent = zoneColor
                )
                HudReadoutPill(
                    text = "FOUND: ${hud.anomaliesFound}",
                    accent = PhosphorGreen
                )
            }

            // Dynamic VHS Toast / Evidence Banner
            VhsBannerOverlay(banner = hud.activeBanner)
        }

        // 4. BOTTOM ERGONOMIC CONTROLS (Safe above navigation bar)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: Analog Movement Joystick
            VirtualThumbstick(
                onMoveChanged = { fwd, str ->
                    forwardInput = fwd
                    strafeInput = str
                }
            )

            // Right: Quick-Turn Helpers + 2.0x Zoom + Scan Trigger
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick-turn 45 deg chips for effortless corridor checking
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickTurnChip(
                        icon = Icons.AutoMirrored.Filled.RotateLeft,
                        contentDesc = stringResource(R.string.cd_turn_left),
                        tag = "turn_left_button",
                        onClick = { pendingTurnDelta -= 0.78f }
                    )
                    QuickTurnChip(
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        contentDesc = stringResource(R.string.cd_turn_right),
                        tag = "turn_right_button",
                        onClick = { pendingTurnDelta += 0.78f }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 2.0x Optical Zoom Toggle Button
                    val zoomAccent = if (hud.isZoomed) PhosphorGreen else TapeWhite
                    val zoomDesc = stringResource(R.string.cd_zoom_toggle)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CrtVoid.copy(alpha = 0.68f))
                            .border(
                                width = 1.5.dp,
                                color = if (hud.isZoomed) PhosphorGreen else StaticHairline,
                                shape = CircleShape
                            )
                            .clickable {
                                soundEngine.playZoomClick()
                                viewModel.toggleZoom()
                            }
                            .testTag("zoom_toggle_button")
                            .semantics { contentDescription = zoomDesc },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (hud.isZoomed) Icons.Filled.ZoomOut else Icons.Filled.ZoomIn,
                                contentDescription = null,
                                tint = zoomAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (hud.isZoomed) stringResource(R.string.hud_zoom_2x) else stringResource(R.string.hud_zoom_1x),
                                style = MaterialTheme.typography.labelSmall,
                                color = zoomAccent
                            )
                        }
                    }

                    // Primary Camcorder SCAN button
                    VhsScanButton(
                        hasTarget = hud.hasTargetInRange,
                        isScanning = hud.isScanning,
                        scanProgress = hud.scanProgress,
                        onScanClick = {
                            if (hud.isScanning) {
                                viewModel.cancelScan()
                            } else {
                                viewModel.startScan(currentAimTarget)
                            }
                        }
                    )
                }
            }
        }

        // 5. PAUSE OVERLAY
        if (hud.phase == RunPhase.PAUSED) {
            PauseTapeOverlay(
                hud = hud,
                onResume = { viewModel.resumeRun() },
                onEject = { viewModel.ejectTape() }
            )
        }

        // 6. RUN SUMMARY / TAPE EJECTED OVERLAY
        if (hud.phase == RunPhase.SUMMARY) {
            TapeSummaryOverlay(
                hud = hud,
                onWalkAgain = { viewModel.startNewRun() },
                onOpenCodex = onOpenCodex,
                onReturnDeck = onExitToDeck
            )
        }
    }
}

@Composable
private fun HudReadoutPill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .background(CrtVoid.copy(alpha = 0.64f), RoundedCornerShape(3.dp))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

@Composable
private fun QuickTurnChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CrtVoid.copy(alpha = 0.62f))
            .border(1.dp, StaticHairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TapeWhite,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PauseTapeOverlay(
    hud: PlayHudState,
    onResume: () -> Unit,
    onEject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrtVoid.copy(alpha = 0.82f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TapeHousing,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .border(1.dp, FluorescentYellow.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.hud_paused),
                    style = MaterialTheme.typography.headlineLarge,
                    color = FluorescentYellow
                )
                Text(
                    text = "DIST %.1fm  •  %s  •  %d FOUND".format(
                        hud.distanceMeters,
                        hud.distortionStage.code,
                        hud.anomaliesFound
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTan
                )
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FluorescentYellow,
                        contentColor = CrtVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("resume_tape_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.hud_resume), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onEject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("eject_tape_button")
                ) {
                    Text(
                        text = stringResource(R.string.hud_eject_tape),
                        style = MaterialTheme.typography.labelLarge,
                        color = RecCrimson
                    )
                }
            }
        }
    }
}

@Composable
private fun TapeSummaryOverlay(
    hud: PlayHudState,
    onWalkAgain: () -> Unit,
    onOpenCodex: () -> Unit,
    onReturnDeck: () -> Unit
) {
    val totalScans = hud.anomaliesFound + hud.falsePositives
    val accuracy = if (totalScans > 0) (hud.anomaliesFound * 100) / totalScans else 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrtVoid.copy(alpha = 0.88f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TapeHousing,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(1.dp, FluorescentYellow, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.summary_title_ejected),
                    style = MaterialTheme.typography.headlineMedium,
                    color = FluorescentYellow
                )

                Surface(
                    color = TapeHousingElevated,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStatRow("DISTANCE WALKED", "%.1f m".format(hud.distanceMeters), FluorescentYellow)
                        SummaryStatRow("ANOMALIES LOGGED", "${hud.anomaliesFound}", PhosphorGreen)
                        SummaryStatRow("SCAN ACCURACY", "$accuracy% (${hud.falsePositives} FALSE)", TapeWhite)
                        SummaryStatRow("PEAK DISTORTION", "TIER ${hud.peakTierReached}", WarningAmber)
                        SummaryStatRow("TAPE DURATION", formatDuration(hud.elapsedSeconds.toInt()), MutedTan)
                    }
                }

                if (hud.newlyUnlockedIds.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "NEW CODEX ENTRIES UNLOCKED:",
                            style = MaterialTheme.typography.labelMedium,
                            color = PhosphorGreen
                        )
                        hud.newlyUnlockedIds.forEach { id ->
                            val spec = AnomalyCatalog.getById(id)
                            if (spec != null) {
                                Text(
                                    text = "+ ${spec.anomalyTitle.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TapeWhite
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onWalkAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FluorescentYellow,
                        contentColor = CrtVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("walk_again_button")
                ) {
                    Text(stringResource(R.string.action_walk_again), style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenCodex,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("summary_codex_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_view_codex),
                            style = MaterialTheme.typography.labelMedium,
                            color = PhosphorGreen
                        )
                    }
                    OutlinedButton(
                        onClick = onReturnDeck,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("summary_deck_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_return_deck),
                            style = MaterialTheme.typography.labelMedium,
                            color = TapeWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedTan)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = valueColor)
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Suppress("DEPRECATION")
private fun triggerHaptic(context: Context, durationMs: Long) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator?.vibrate(durationMs)
            }
        }
    } catch (_: Throwable) {
        // Ignore on devices without haptics
    }
}
