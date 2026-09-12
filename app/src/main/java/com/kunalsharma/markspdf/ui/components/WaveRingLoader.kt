package com.kunalsharma.markspdf.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WaveRingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 116.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = defaultDiscColor(),
    content: @Composable () -> Unit = { AppLogo(size = size * LogoFraction) },
) {
    val transition = rememberInfiniteTransition(label = "waveRing")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
        ) {
            val stroke = strokeWidth.toPx()
            // Divide by (1 + amplitude) so the outermost crest lands on the edge of the
            // component rather than being clipped by it.
            val radius = (min(this.size.width, this.size.height) / 2f - stroke / 2f) /
                (1f + Amplitude)

            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            val path = Path()
            for (i in 0..Segments) {
                val theta = (i.toFloat() / Segments) * 2f * PI.toFloat()
                val r = radius * (1f + Amplitude * sin(Lobes * theta + phase))
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        Box(
            modifier = Modifier
                .size(size * DiscFraction)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun defaultDiscColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() < 0.5f) {
        scheme.primaryContainer
    } else {
        scheme.surfaceContainerLowest
    }
}

private const val DiscFraction = 0.72f

private const val LogoFraction = 0.44f

private const val Lobes = 12f

private const val Amplitude = 0.085f

private const val Segments = 240
