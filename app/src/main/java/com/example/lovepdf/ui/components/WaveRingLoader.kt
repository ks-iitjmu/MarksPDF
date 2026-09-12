package com.example.lovepdf.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
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
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size - RingInset * 2)
                .graphicsLayer { rotationZ = rotation }
        ) {
            val stroke = strokeWidth.toPx()
            // Keep the outermost crest inside the canvas, stroke included.
            val radius = (min(this.size.width, this.size.height) / 2f - stroke / 2f) /
                (1f + Amplitude)

            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            val path = Path()
            for (i in 0..Segments) {
                val theta = (i.toFloat() / Segments) * 2f * PI.toFloat()
                val r = radius * (1f + Amplitude * sin(Lobes * theta + phase))
                path.run {
                    val x = cx + r * cos(theta)
                    val y = cy + r * sin(theta)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
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

        content()
    }
}

private val RingInset = 22.dp

private const val LogoFraction = 0.38f

private const val Lobes = 12f

private const val Amplitude = 0.085f

private const val Segments = 240
