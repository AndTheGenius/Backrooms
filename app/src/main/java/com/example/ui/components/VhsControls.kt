package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.BannerTone
import com.example.ui.HudBanner
import com.example.ui.theme.CrtVoid
import com.example.ui.theme.FluorescentYellow
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.RecCrimson
import com.example.ui.theme.TapeHousing
import com.example.ui.theme.TapeWhite
import com.example.ui.theme.WarningAmber
import kotlin.math.hypot
import kotlin.math.min

/**
 * Tactile analog thumbstick for smooth walking forward/back and strafing inside the 3D Backrooms.
 */
@Composable
fun VirtualThumbstick(
    onMoveChanged: (forward: Float, strafe: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(124.dp)
            .testTag("virtual_thumbstick")
            .semantics { contentDescription = "Movement joystick" }
            .pointerInput(Unit) {
                val maxRadius = size.width * 0.36f
                detectDragGestures(
                    onDragStart = { isPressed = true },
                    onDragEnd = {
                        isPressed = false
                        knobOffset = Offset.Zero
                        onMoveChanged(0f, 0f)
                    },
                    onDragCancel = {
                        isPressed = false
                        knobOffset = Offset.Zero
                        onMoveChanged(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val dist = hypot(raw.x, raw.y)
                        val clamped = if (dist > maxRadius && dist > 0f) {
                            Offset(raw.x / dist * maxRadius, raw.y / dist * maxRadius)
                        } else {
                            raw
                        }
                        knobOffset = clamped
                        val forward = (-clamped.y / maxRadius).coerceIn(-1f, 1f)
                        val strafe = (clamped.x / maxRadius).coerceIn(-1f, 1f)
                        onMoveChanged(forward, strafe)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) * 0.46f
            val knobRadius = outerRadius * 0.38f

            // Translucent dark CRT pad
            drawCircle(
                color = CrtVoid.copy(alpha = 0.56f),
                radius = outerRadius,
                center = center
            )
            // Outer phosphor ring
            drawCircle(
                color = if (isPressed) FluorescentYellow.copy(alpha = 0.85f)
                else TapeWhite.copy(alpha = 0.36f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Subtle directional crosshair ticks
            val tickLen = 7.dp.toPx()
            val tickColor = TapeWhite.copy(alpha = 0.25f)
            drawLine(tickColor, Offset(center.x, center.y - outerRadius + 4f), Offset(center.x, center.y - outerRadius + tickLen + 4f), 1.dp.toPx())
            drawLine(tickColor, Offset(center.x, center.y + outerRadius - 4f), Offset(center.x, center.y + outerRadius - tickLen - 4f), 1.dp.toPx())
            drawLine(tickColor, Offset(center.x - outerRadius + 4f, center.y), Offset(center.x - outerRadius + tickLen + 4f, center.y), 1.dp.toPx())
            drawLine(tickColor, Offset(center.x + outerRadius - 4f, center.y), Offset(center.x + outerRadius - tickLen - 4f, center.y), 1.dp.toPx())

            // Tactile draggable knob
            val knobPos = center + knobOffset
            drawCircle(
                color = if (isPressed) FluorescentYellow.copy(alpha = 0.92f)
                else TapeWhite.copy(alpha = 0.72f),
                radius = knobRadius,
                center = knobPos
            )
            drawCircle(
                color = CrtVoid.copy(alpha = 0.55f),
                radius = knobRadius * 0.48f,
                center = knobPos,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * Primary camcorder SCAN / REPORT trigger with animated circular sweep ring.
 */
@Composable
fun VhsScanButton(
    hasTarget: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        isScanning -> PhosphorGreen
        hasTarget -> FluorescentYellow
        else -> TapeWhite.copy(alpha = 0.55f)
    }

    val scanDesc = stringResource(R.string.cd_scan_button)
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(CrtVoid.copy(alpha = 0.70f))
            .clickable(onClick = onScanClick)
            .testTag("scan_button")
            .semantics { contentDescription = scanDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 3.dp.toPx()
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = size.minDimension / 2f - strokePx,
                style = Stroke(width = strokePx)
            )
            if (isScanning) {
                drawArc(
                    color = PhosphorGreen,
                    startAngle = -90f,
                    sweepAngle = 360f * scanProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt)
                )
            } else if (hasTarget) {
                drawCircle(
                    color = FluorescentYellow,
                    radius = size.minDimension / 2f - strokePx,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = if (isScanning) "${(scanProgress * 100).toInt()}%" else stringResource(R.string.hud_scan),
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}

/**
 * Full-bleed camcorder viewfinder reticle & subtle CRT radial vignette.
 */
@Composable
fun ViewfinderReticleOverlay(
    hasTarget: Boolean,
    targetDistance: Float?,
    isScanning: Boolean,
    scanProgress: Float,
    isZoomed: Boolean,
    distortionTier: Int,
    modifier: Modifier = Modifier
) {
    val reticleColor = when {
        isScanning -> PhosphorGreen
        hasTarget -> FluorescentYellow
        else -> TapeWhite.copy(alpha = 0.45f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle CRT corner vignette that darkens at deeper Distortion Tiers
            val vignetteAlpha = (0.34f + (distortionTier - 1) * 0.08f).coerceAtMost(0.68f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, CrtVoid.copy(alpha = vignetteAlpha)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.72f
                )
            )

            // Center autofocus viewfinder bracket
            val cx = size.width / 2f
            val cy = size.height / 2f
            val half = if (isZoomed) 48.dp.toPx() else if (hasTarget) 32.dp.toPx() else 24.dp.toPx()
            val arm = 10.dp.toPx()
            val stroke = 2.dp.toPx()

            // Top-left corner
            drawLine(reticleColor, Offset(cx - half, cy - half), Offset(cx - half + arm, cy - half), stroke)
            drawLine(reticleColor, Offset(cx - half, cy - half), Offset(cx - half, cy - half + arm), stroke)
            // Top-right corner
            drawLine(reticleColor, Offset(cx + half, cy - half), Offset(cx + half - arm, cy - half), stroke)
            drawLine(reticleColor, Offset(cx + half, cy - half), Offset(cx + half, cy - half + arm), stroke)
            // Bottom-left corner
            drawLine(reticleColor, Offset(cx - half, cy + half), Offset(cx - half + arm, cy + half), stroke)
            drawLine(reticleColor, Offset(cx - half, cy + half), Offset(cx - half, cy + half - arm), stroke)
            // Bottom-right corner
            drawLine(reticleColor, Offset(cx + half, cy + half), Offset(cx + half - arm, cy + half), stroke)
            drawLine(reticleColor, Offset(cx + half, cy + half), Offset(cx + half, cy + half - arm), stroke)

            // Center dot
            drawCircle(reticleColor, radius = 2.dp.toPx(), center = Offset(cx, cy))

            // Horizontal scan laser sweep line inside bracket while scanning
            if (isScanning) {
                val sweepY = (cy - half) + (half * 2f) * scanProgress
                drawLine(
                    color = PhosphorGreen,
                    start = Offset(cx - half + 2f, sweepY),
                    end = Offset(cx + half - 2f, sweepY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Dynamic autofocus status tag just below the reticle
        if (hasTarget || isScanning) {
            val label = when {
                isScanning -> "ANALYZING SIGNAL... ${(scanProgress * 100).toInt()}%"
                targetDistance != null -> "OBJ LOCKED [%.1fm] — ZOOM OR SCAN".format(targetDistance * 1.65f)
                else -> "OBJ LOCKED"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 112.dp)
                    .background(CrtVoid.copy(alpha = 0.72f), RoundedCornerShape(3.dp))
                    .border(1.dp, reticleColor.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = reticleColor
                )
            }
        }
    }
}

@Composable
fun VhsBannerOverlay(
    banner: HudBanner?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        if (banner != null) {
            val (accent, icon) = when (banner.tone) {
                BannerTone.CONFIRM -> PhosphorGreen to Icons.Filled.CheckCircle
                BannerTone.ERROR -> RecCrimson to Icons.Filled.ErrorOutline
                BannerTone.WARNING -> WarningAmber to Icons.Filled.WarningAmber
                BannerTone.INFO -> FluorescentYellow to Icons.Filled.Info
            }
            Surface(
                color = TapeHousing.copy(alpha = 0.92f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = banner.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TapeWhite
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats an eerie 1994 VHS date & timecode that progressively corrupts at higher Distortion Tiers.
 */
fun formatVhsTimestamp(elapsedSeconds: Float, distortionTier: Int): String {
    val total = elapsedSeconds.toInt()
    val mins = (total / 60) % 60
    val secs = total % 60
    val datePart = when {
        distortionTier >= 5 -> "OCT 99 19??"
        distortionTier == 4 -> "OCT ?? 1994"
        distortionTier == 3 && (total % 4 == 0) -> "OCT 14 199█"
        else -> "OCT 14 1994"
    }
    val timePart = if (distortionTier >= 5 && (total % 3 == 0)) {
        "SP 88:%02d:??".format(secs)
    } else {
        "SP 00:%02d:%02d".format(mins, secs)
    }
    return "$datePart  $timePart"
}
