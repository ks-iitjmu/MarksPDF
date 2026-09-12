package com.kunalsharma.markspdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF3F5D87),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7F8),
    onSecondaryContainer = Color(0xFF101F35),
    tertiary = Color(0xFF4A6087),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E3F8),
    onTertiaryContainer = Color(0xFF0F1D33),
    background = Color(0xFFF9FAFE),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9FAFE),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4FA),
    surfaceContainer = Color(0xFFECEFF6),
    surfaceContainerHigh = Color(0xFFE6E9F1),
    surfaceContainerHighest = Color(0xFFE0E4EC),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293042),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = Color(0xFFB9C7E4),
    onTertiary = Color(0xFF243148),
    tertiaryContainer = Color(0xFF3A4860),
    onTertiaryContainer = Color(0xFFD8E3F8),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun pillSurfaceColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color.White
    }

@Composable
fun pillButtonColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

@Composable
fun MarksPDFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = ExpressiveShapes,
        content = content,
    )
}
