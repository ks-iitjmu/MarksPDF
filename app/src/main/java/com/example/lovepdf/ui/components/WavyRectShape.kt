package com.example.lovepdf.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class WavyRectShape(
    private val amplitude: Dp = 1.6.dp,
    private val waveLength: Dp = 12.dp,
    private val cornerRadius: Dp = 6.dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val amp = with(density) { amplitude.toPx() }
        val wave = with(density) { waveLength.toPx() }
        val right = (size.width - amp).coerceAtLeast(amp + 1f)
        val bottom = (size.height - amp).coerceAtLeast(amp + 1f)
        val width = right - amp
        val height = bottom - amp

        val radius = with(density) { cornerRadius.toPx() }
            .coerceAtMost(minOf(width, height) / 2f)
            .coerceAtLeast(0f)

        val straightX = (width - 2f * radius).coerceAtLeast(0f)
        val straightY = (height - 2f * radius).coerceAtLeast(0f)
        val arc = (PI.toFloat() / 2f) * radius
        val perimeter = 2f * straightX + 2f * straightY + 4f * arc

        val waves = (perimeter / wave).roundToInt().coerceAtLeast(4)

        val path = Path()
        for (i in 0..SampleCount) {
            val distance = perimeter * (i.toFloat() / SampleCount)
            val offset = amp * sin(2f * PI.toFloat() * waves * distance / perimeter)

            val (point, normal) = traverse(
                distance = distance,
                left = amp,
                top = amp,
                right = right,
                bottom = bottom,
                radius = radius,
                straightX = straightX,
                straightY = straightY,
                arc = arc,
            )

            val x = point.x + normal.x * offset
            val y = point.y + normal.y * offset
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        path.close()
        return Outline.Generic(path)
    }

    private fun traverse(
        distance: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        straightX: Float,
        straightY: Float,
        arc: Float,
    ): Pair<Offset, Offset> {
        var d = distance

        if (d < straightX) {
            return Offset(left + radius + d, top) to Offset(0f, -1f)
        }
        d -= straightX

        if (d < arc) {
            return onArc(
                centre = Offset(right - radius, top + radius),
                radius = radius,
                startAngle = -PI.toFloat() / 2f,
                sweep = (d / arc) * (PI.toFloat() / 2f),
            )
        }
        d -= arc

        if (d < straightY) {
            return Offset(right, top + radius + d) to Offset(1f, 0f)
        }
        d -= straightY

        if (d < arc) {
            return onArc(
                centre = Offset(right - radius, bottom - radius),
                radius = radius,
                startAngle = 0f,
                sweep = (d / arc) * (PI.toFloat() / 2f),
            )
        }
        d -= arc

        if (d < straightX) {
            return Offset(right - radius - d, bottom) to Offset(0f, 1f)
        }
        d -= straightX

        if (d < arc) {
            return onArc(
                centre = Offset(left + radius, bottom - radius),
                radius = radius,
                startAngle = PI.toFloat() / 2f,
                sweep = (d / arc) * (PI.toFloat() / 2f),
            )
        }
        d -= arc

        if (d < straightY) {
            return Offset(left, bottom - radius - d) to Offset(-1f, 0f)
        }
        d -= straightY

        return onArc(
            centre = Offset(left + radius, top + radius),
            radius = radius,
            startAngle = PI.toFloat(),
            sweep = (d / arc).coerceAtMost(1f) * (PI.toFloat() / 2f),
        )
    }

    private fun onArc(
        centre: Offset,
        radius: Float,
        startAngle: Float,
        sweep: Float,
    ): Pair<Offset, Offset> {
        val angle = startAngle + sweep
        val normal = Offset(cos(angle), sin(angle))
        return Offset(centre.x + normal.x * radius, centre.y + normal.y * radius) to normal
    }

    override fun equals(other: Any?): Boolean =
        other is WavyRectShape &&
            other.amplitude == amplitude &&
            other.waveLength == waveLength &&
            other.cornerRadius == cornerRadius

    override fun hashCode(): Int {
        var result = amplitude.hashCode()
        result = 31 * result + waveLength.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        return result
    }
}

private const val SampleCount = 360
