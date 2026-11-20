package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LevelZeroColorScheme = darkColorScheme(
    primary = FluorescentYellow,
    onPrimary = CrtVoid,
    primaryContainer = DampOchre,
    onPrimaryContainer = TapeWhite,
    secondary = PhosphorGreen,
    onSecondary = CrtVoid,
    secondaryContainer = PhosphorGreenDim,
    onSecondaryContainer = TapeWhite,
    tertiary = WarningAmber,
    onTertiary = CrtVoid,
    error = RecCrimson,
    onError = TapeWhite,
    errorContainer = RecCrimsonDark,
    onErrorContainer = TapeWhite,
    background = CrtVoid,
    onBackground = TapeWhite,
    surface = TapeHousing,
    onSurface = TapeWhite,
    surfaceVariant = TapeHousingElevated,
    onSurfaceVariant = MutedTan,
    outline = StaticHairline,
    outlineVariant = TapeHousingHighlight
)

private val LevelZeroShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

@Composable
fun LevelZeroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LevelZeroColorScheme,
        typography = AppTypography,
        shapes = LevelZeroShapes,
        content = content
    )
}
