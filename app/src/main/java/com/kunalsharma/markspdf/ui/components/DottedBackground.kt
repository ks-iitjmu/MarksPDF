package com.kunalsharma.markspdf.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.dottedBackground(
    color: Color = defaultDotColor(),
    spacing: Dp = DotSpacing,
    radius: Dp = DotRadius,
): Modifier = this.drawBehind {
    val step = spacing.toPx()
    val dot = radius.toPx()
    if (step <= 0f) return@drawBehind

    var y = step / 2f
    while (y < size.height) {
        var x = step / 2f
        while (x < size.width) {
            drawCircle(color = color, radius = dot, center = Offset(x, y))
            x += step
        }
        y += step
    }
}
@Composable
private fun defaultDotColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() < 0.5f) {
        scheme.onSurface.copy(alpha = 0.10f)
    } else {
        scheme.primary.copy(alpha = 0.22f)
    }
}

private val DotSpacing = 20.dp
private val DotRadius = 1.dp
