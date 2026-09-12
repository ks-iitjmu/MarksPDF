package com.example.lovepdf.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgressBar(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    val transition = rememberInfiniteTransition(label = "wavyProgress")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
    ) {
        val stroke = StrokeWidth.toPx()
        val midY = size.height / 2f
        val amplitude = Amplitude.toPx()
        val wavelength = Wavelength.toPx()
        val gap = Gap.toPx()

        val filledEnd = size.width * progress

        val trackStart = (filledEnd + gap).coerceAtMost(size.width)
        if (trackStart < size.width) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(trackStart, midY - stroke / 2f),
                size = Size(size.width - trackStart, stroke),
                cornerRadius = CornerRadius(stroke / 2f),
            )

            val dotCentre = size.width - stroke / 2f
            if (dotCentre > trackStart + stroke) {
                drawCircle(
                    color = color,
                    radius = DotRadius.toPx(),
                    center = Offset(dotCentre, midY),
                )
            }
        }

        val waveEnd = (filledEnd - gap).coerceAtLeast(0f)
        if (waveEnd > 1f) {
            val path = Path()
            val steps = (waveEnd / 2f).toInt().coerceIn(2, 400)

            for (i in 0..steps) {
                val x = waveEnd * (i.toFloat() / steps)

                val taper = ((waveEnd - x) / wavelength).coerceIn(0f, 1f)
                val y = midY + amplitude * taper *
                    sin(2f * PI.toFloat() * x / wavelength - phase)

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private val BarHeight = 24.dp
private val StrokeWidth = 6.dp
private val Amplitude = 5.dp
private val Wavelength = 30.dp
private val Gap = 8.dp
private val DotRadius = 3.dp
