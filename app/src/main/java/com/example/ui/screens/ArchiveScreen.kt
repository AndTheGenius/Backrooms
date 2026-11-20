package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RunRecordEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ArchiveSort { BEST_DISTANCE, MOST_RECENT }

@Composable
fun ArchiveScreen(
    runsByDistance: List<RunRecordEntity>,
    runsByRecent: List<RunRecordEntity>,
    onClearArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMode by remember { mutableStateOf(ArchiveSort.BEST_DISTANCE) }
    var showClearDialog by remember { mutableStateOf(false) }

    val activeRuns = if (sortMode == ArchiveSort.BEST_DISTANCE) runsByDistance else runsByRecent
    val bestRun = runsByDistance.firstOrNull()

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.archive_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = FluorescentYellow
                )
                Text(
                    text = stringResource(R.string.archive_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedTan
                )
            }
            if (activeRuns.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("clear_archive_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Clear tape archive",
                        tint = MutedTan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = sortMode == ArchiveSort.BEST_DISTANCE,
                onClick = { sortMode = ArchiveSort.BEST_DISTANCE },
                label = { Text("BEST WALK", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DampOchre,
                    selectedLabelColor = TapeWhite,
                    containerColor = TapeHousing,
                    labelColor = MutedTan
                ),
                modifier = Modifier.testTag("archive_sort_best")
            )
            FilterChip(
                selected = sortMode == ArchiveSort.MOST_RECENT,
                onClick = { sortMode = ArchiveSort.MOST_RECENT },
                label = { Text("RECENT", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DampOchre,
                    selectedLabelColor = TapeWhite,
                    containerColor = TapeHousing,
                    labelColor = MutedTan
                ),
                modifier = Modifier.testTag("archive_sort_recent")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeRuns.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideocamOff,
                        contentDescription = null,
                        tint = MutedTan,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = stringResource(R.string.archive_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TapeWhite
                    )
                    Text(
                        text = stringResource(R.string.archive_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTan
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (bestRun != null) {
                    item(key = "personal_best") {
                        PersonalBestCard(bestRun)
                    }
                }
                itemsIndexed(activeRuns, key = { _, item -> item.id }) { index, run ->
                    RunRecordCard(
                        rank = index + 1,
                        run = run,
                        isRecordBest = bestRun?.id == run.id
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = TapeHousing,
            title = {
                Text(
                    text = "ERASE TAPE ARCHIVE?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RecCrimson
                )
            },
            text = {
                Text(
                    text = "This wipes saved run records from local storage. Your discovered Anomaly Codex entries will remain intact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TapeWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearArchive()
                    }
                ) {
                    Text("ERASE", color = RecCrimson, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL", color = TapeWhite, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun PersonalBestCard(best: RunRecordEntity) {
    Surface(
        color = TapeHousingElevated,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FluorescentYellow.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = FluorescentYellow,
                modifier = Modifier.size(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DEEPEST RECORDED EXPEDITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = FluorescentYellow
                )
                Text(
                    text = "%.1f METERS • %d ANOMALIES".format(best.distanceMeters, best.anomaliesFound),
                    style = MaterialTheme.typography.titleLarge,
                    color = TapeWhite
                )
            }
            Surface(
                color = CrtVoid,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.border(1.dp, WarningAmber.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "TIER ${best.peakDistortionTier}",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RunRecordCard(
    rank: Int,
    run: RunRecordEntity,
    isRecordBest: Boolean
) {
    val totalScans = run.anomaliesFound + run.falsePositives
    val accuracy = if (totalScans > 0) (run.anomaliesFound * 100) / totalScans else 100
    val formattedDate = remember(run.timestamp) {
        SimpleDateFormat("MMM dd • HH:mm", Locale.US).format(Date(run.timestamp))
    }

    Surface(
        color = TapeHousing,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isRecordBest) FluorescentYellow.copy(alpha = 0.45f) else StaticHairline,
                RoundedCornerShape(6.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAPE #%02d  //  %.1f m".format(rank, run.distanceMeters),
                    style = MaterialTheme.typography.titleMedium,
                    color = FluorescentYellow
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedTan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FOUND: ${run.anomaliesFound}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PhosphorGreen
                )
                Text(
                    text = "ACC: $accuracy%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TapeWhite
                )
                Text(
                    text = "PEAK: TIER ${run.peakDistortionTier}",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber
                )
                val mins = run.durationSeconds / 60
                val secs = run.durationSeconds % 60
                Text(
                    text = "%02d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedTan
                )
            }
        }
    }
}
