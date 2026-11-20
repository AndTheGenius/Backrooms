package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BackroomsBiome
import com.example.engine.BackroomsWorld
import com.example.engine.RaycasterRenderer
import com.example.ui.CareerStats
import com.example.ui.theme.CrtVoid
import com.example.ui.theme.DampOchre
import com.example.ui.theme.FluorescentYellow
import com.example.ui.theme.MutedTan
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.RecCrimson
import com.example.ui.theme.StaticHairline
import com.example.ui.theme.TapeHousing
import com.example.ui.theme.TapeHousingElevated
import com.example.ui.theme.TapeWhite
import com.example.ui.theme.WarningAmber
import kotlin.math.sin

/**
 * Found-footage VHS Tape Deck title screen featuring a live animated raycasted preview of
 * whichever Backrooms environment channel is tuned (all 8 biomes remain connected on foot!).
 */
@Composable
fun TapeDeckScreen(
    careerStats: CareerStats,
    selectedBiome: BackroomsBiome,
    onSelectBiome: (BackroomsBiome) -> Unit,
    onInsertTape: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Live ambient raycasted preview switching immediately when the player tunes a sector channel!
    val previewWorld = remember(selectedBiome) { BackroomsWorld(seed = 199409L, startingBiome = selectedBiome) }
    val previewRenderer = remember { RaycasterRenderer(width = 180, height = 112) }
    var frameTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedBiome) {
        var lastNanos = 0L
        var elapsed = 0f
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    elapsed += dt
                    val sway = sin(elapsed * 0.42f) * 0.28f * dt
                    previewWorld.update(dt, forwardInput = 0.24f, strafeInput = 0f, turnDelta = sway)
                    previewRenderer.render(
                        world = previewWorld,
                        fovPlane = 0.66f,
                        timeSeconds = elapsed,
                        distortionIntensity = 0.14f + (sin(elapsed * 0.3f) * 0.06f).coerceAtLeast(0f),
                        distortionTier = 2,
                        staticBurst = 0f,
                        scanActive = false
                    )
                    frameTick++
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
        // Background live 3D corridor feed of the selected environment
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick
            drawImage(
                image = previewRenderer.imageBitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CrtVoid.copy(alpha = 0.78f),
                        CrtVoid.copy(alpha = 0.42f),
                        CrtVoid.copy(alpha = 0.90f),
                        CrtVoid
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(max = 580.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top VHS Camcorder Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RecCrimson)
                    )
                    Text(
                        text = "VCR-HI8 // STANDBY",
                        style = MaterialTheme.typography.labelMedium,
                        color = TapeWhite
                    )
                }
                Text(
                    text = "OCT 14 1994",
                    style = MaterialTheme.typography.labelMedium,
                    color = FluorescentYellow
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Asymmetric Hero Title Lockup
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = RecCrimson.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.border(1.dp, RecCrimson.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = RecCrimson,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "FOUND FOOTAGE SURVEY // TAPE 09",
                            style = MaterialTheme.typography.labelSmall,
                            color = TapeWhite
                        )
                    }
                }

                Text(
                    text = "LEVEL ZERO",
                    style = MaterialTheme.typography.displayLarge,
                    color = FluorescentYellow
                )
                Text(
                    text = stringResource(R.string.tape_deck_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTan
                )
            }

            // Starting Environment Channel Selector (8 Interconnected Biomes!)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STARTING ENVIRONMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = FluorescentYellow
                    )
                    Text(
                        text = "ALL 8 CONNECTED ON FOOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedTan
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BackroomsBiome.entries, key = { it.name }) { biome ->
                        val selected = biome == selectedBiome
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectBiome(biome) },
                            label = {
                                Text(
                                    text = biome.shortLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DampOchre,
                                selectedLabelColor = TapeWhite,
                                containerColor = TapeHousing.copy(alpha = 0.85f),
                                labelColor = MutedTan
                            ),
                            modifier = Modifier.testTag("biome_chip_${biome.name.lowercase()}")
                        )
                    }
                }
            }

            // Career Telemetry Strip
            Surface(
                color = TapeHousing.copy(alpha = 0.86f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StaticHairline, RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryStat(
                        label = "DEEPEST WALK",
                        value = "%.1fm".format(careerStats.bestDistanceMeters),
                        accent = FluorescentYellow
                    )
                    TelemetryStat(
                        label = "CODEX LOGGED",
                        value = "${careerStats.discoveredCount}/${careerStats.totalCatalogCount}",
                        accent = PhosphorGreen
                    )
                    TelemetryStat(
                        label = "PEAK TIER",
                        value = "TIER ${careerStats.highestTierReached}",
                        accent = WarningAmber
                    )
                }
            }

            // Primary Action CTA
            Button(
                onClick = onInsertTape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FluorescentYellow,
                    contentColor = CrtVoid
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("insert_tape_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.action_insert_tape)} (${selectedBiome.shortLabel})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Field Briefing Protocol Card
            Surface(
                color = TapeHousingElevated.copy(alpha = 0.84f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StaticHairline, RoundedCornerShape(6.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint = FluorescentYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "OPERATOR PROTOCOL",
                            style = MaterialTheme.typography.titleLarge,
                            color = TapeWhite
                        )
                    }

                    ProtocolRow(
                        tag = "01",
                        text = "Explore 8 connected Backrooms sectors: Level 0, Poolrooms, Amusement Park, Gym, Indoor Garden, Camera Room, Pharmacy, and Antique Shop."
                    )
                    ProtocolRow(
                        tag = "02",
                        text = "Use 2.0x OPTICAL ZOOM to inspect realistic anomalies (3-legged chairs, 5-eyed dolls, cosmic mirrors, Slenderman dolls, vinyl players, dementia pills, indoor trees & grass) before pressing SCAN."
                    )
                    ProtocolRow(
                        tag = "03",
                        text = "Walk as far as you want with no death or timer—reality keeps warping as rare watching wall eyes follow your steps."
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = MutedTan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LOCAL OFFLINE ARCHIVE • ${careerStats.totalRuns} TAPES RECORDED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedTan
                )
            }
        }
    }
}

@Composable
private fun TelemetryStat(
    label: String,
    value: String,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedTan
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent
        )
    }
}

@Composable
private fun ProtocolRow(tag: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[$tag]",
            style = MaterialTheme.typography.labelMedium,
            color = FluorescentYellow
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TapeWhite.copy(alpha = 0.88f)
        )
    }
}
