package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.AnomalyDiscoveryEntity
import com.example.engine.AnomalyCatalog
import com.example.engine.AnomalySpec
import com.example.engine.SpriteGenerator
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

private enum class CodexFilter { ALL, OBSERVED, REDACTED }

@Composable
fun CodexScreen(
    discoveries: Map<String, AnomalyDiscoveryEntity>,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(CodexFilter.ALL) }
    var selectedSpec by remember { mutableStateOf<AnomalySpec?>(null) }

    val filteredList = remember(discoveries, filter) {
        AnomalyCatalog.all.filter { spec ->
            val found = discoveries.containsKey(spec.id)
            when (filter) {
                CodexFilter.ALL -> true
                CodexFilter.OBSERVED -> found
                CodexFilter.REDACTED -> !found
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CrtVoid)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.codex_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = FluorescentYellow
                )
                Text(
                    text = stringResource(R.string.codex_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedTan
                )
            }
            Surface(
                color = TapeHousingElevated,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.border(1.dp, PhosphorGreen.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "${discoveries.size} / ${AnomalyCatalog.all.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = PhosphorGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CodexFilterChip(
                label = stringResource(R.string.codex_filter_all),
                selected = filter == CodexFilter.ALL,
                tag = "codex_filter_all",
                onClick = { filter = CodexFilter.ALL }
            )
            CodexFilterChip(
                label = stringResource(R.string.codex_filter_observed),
                selected = filter == CodexFilter.OBSERVED,
                tag = "codex_filter_observed",
                onClick = { filter = CodexFilter.OBSERVED }
            )
            CodexFilterChip(
                label = stringResource(R.string.codex_filter_redacted),
                selected = filter == CodexFilter.REDACTED,
                tag = "codex_filter_redacted",
                onClick = { filter = CodexFilter.REDACTED }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MutedTan,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (filter == CodexFilter.OBSERVED)
                            "NO ANOMALIES CATALOGED YET"
                        else
                            "ALL ${AnomalyCatalog.all.size} ANOMALIES OBSERVED",
                        style = MaterialTheme.typography.titleLarge,
                        color = TapeWhite
                    )
                    Text(
                        text = if (filter == CodexFilter.OBSERVED)
                            "Start a walk, zoom 2.0x on suspicious props, and scan them to populate your local Codex."
                        else
                            "Every known Level 0 deviation is archived on Tape 09.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTan
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { spec ->
                    val discovery = discoveries[spec.id]
                    CodexCard(
                        spec = spec,
                        discovery = discovery,
                        onClick = {
                            if (discovery != null) selectedSpec = spec
                        }
                    )
                }
            }
        }
    }

    selectedSpec?.let { spec ->
        val discovery = discoveries[spec.id]
        if (discovery != null) {
            EvidenceDossierDialog(
                spec = spec,
                discovery = discovery,
                onDismiss = { selectedSpec = null }
            )
        }
    }
}

@Composable
private fun CodexFilterChip(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DampOchre,
            selectedLabelColor = TapeWhite,
            containerColor = TapeHousing,
            labelColor = MutedTan
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun CodexCard(
    spec: AnomalySpec,
    discovery: AnomalyDiscoveryEntity?,
    onClick: () -> Unit
) {
    val isDiscovered = discovery != null
    val borderColor = if (isDiscovered) PhosphorGreen.copy(alpha = 0.55f) else StaticHairline

    Surface(
        color = TapeHousing,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = isDiscovered, onClick = onClick)
            .testTag("codex_card_${spec.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Evidence Sprite Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDiscovered) CrtVoid else TapeHousingElevated)
                    .border(1.dp, StaticHairline, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isDiscovered) {
                    val bitmap = remember(spec.id) {
                        SpriteGenerator.getPreviewImage(spec.id, isAnomaly = true)
                    }
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        drawImage(
                            image = bitmap,
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.None
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = PhosphorGreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(16.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MutedTan.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "[REDACTED]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedTan.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (isDiscovered) spec.anomalyTitle.uppercase() else "UNOBSERVED DEVIATION",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDiscovered) FluorescentYellow else MutedTan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = spec.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedTan
                    )
                    Text(
                        text = if (discovery != null) "x${discovery.timesSpotted}" else "TIER ${spec.minDistortionTier}+",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (discovery != null) PhosphorGreen else WarningAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceDossierDialog(
    spec: AnomalySpec,
    discovery: AnomalyDiscoveryEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = TapeHousing,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(1.dp, PhosphorGreen.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EVIDENCE FILE // ${spec.category.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PhosphorGreen
                        )
                        Text(
                            text = spec.anomalyTitle.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = FluorescentYellow
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_dossier_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close_dialog),
                            tint = TapeWhite
                        )
                    }
                }

                // Side-by-side Baseline vs Anomaly comparison sprites
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpriteComparisonBox(
                        title = "BASELINE",
                        bitmap = remember(spec.id) { SpriteGenerator.getPreviewImage(spec.id, isAnomaly = false) },
                        accent = MutedTan,
                        modifier = Modifier.weight(1f)
                    )
                    SpriteComparisonBox(
                        title = "ANOMALY",
                        bitmap = remember(spec.id) { SpriteGenerator.getPreviewImage(spec.id, isAnomaly = true) },
                        accent = RecCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }

                DossierSection("DEVIATION TELL", spec.anomalyTell, FluorescentYellow)
                DossierSection("NORMAL BASELINE", spec.normalBaseline, MutedTan)
                DossierSection("OPERATOR FIELD NOTE", spec.fieldNote, TapeWhite)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TapeHousingElevated, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SIGHTINGS: ${discovery.timesSpotted}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PhosphorGreen
                    )
                    Text(
                        text = "MAX TIER SEEN: TIER ${discovery.bestDistortionTierSeen}",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun SpriteComparisonBox(
    title: String,
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CrtVoid)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = accent)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            drawImage(
                image = bitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
private fun DossierSection(label: String, body: String, accent: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = accent)
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = TapeWhite)
    }
}
