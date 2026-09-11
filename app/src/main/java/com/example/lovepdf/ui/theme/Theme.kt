package com.example.lovepdf.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Fallback palette for when dynamic color isn't available (API < 31) or is turned off.
 * Seeded around a deep rose, which reads as "Love PDF" without being candy-pink.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF9A4055),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3F0016),
    secondary = Color(0xFF75565C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF2C151A),
    tertiary = Color(0xFF7A5732),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBA),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
    outline = Color(0xFF847375),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1BF),
    onPrimary = Color(0xFF5D1129),
    primaryContainer = Color(0xFF7B293E),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE4BDC3),
    onSecondary = Color(0xFF43292F),
    secondaryContainer = Color(0xFF5C3F45),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFECBE90),
    onTertiary = Color(0xFF452B08),
    tertiaryContainer = Color(0xFF5F401D),
    onTertiaryContainer = Color(0xFFFFDCBA),
    background = Color(0xFF1A1113),
    onBackground = Color(0xFFF0DEE0),
    surface = Color(0xFF1A1113),
    onSurface = Color(0xFFF0DEE0),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C4),
    outline = Color(0xFF9F8C8F),
)

/**
 * Stand-in for the Expressive shape scale, which is locked behind internal APIs in
 * material3 1.4.0. Expressive's corner scale is wider and rounder than baseline M3 —
 * the jump from "small" to "extraLarge" is far more dramatic, and that contrast is a
 * large part of why the look reads as Expressive.
 */
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Surface colour for floating pills — the toolbar and the thumbnail rail.
 *
 * White in light mode. In dark mode a white capsule would be a glaring slab over a dark
 * page, so it takes the darkest surface tone instead. Both pills read this, so they
 * can't drift apart.
 */
@Composable
fun pillSurfaceColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color.White
    }

@Composable
fun LovePDFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        content = content
    )
}
