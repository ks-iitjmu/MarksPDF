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

/**
 * Rounded rectangle with a scalloped edge, matching the ripple in the loading ring.
 *
 * The wave rides along a *rounded* rectangle rather than a sharp one. Offsetting a sharp
 * rectangle leaves a 90-degree break at each corner that no amount of sampling smooths
 * out, because the discontinuity is in the normal direction, not in the sampling.
 * Sweeping the corners as quarter-arcs makes the normal turn gradually, so the ripple
 * carries round the corner instead of stopping at it.
 *
 * The wave is a function of arc length around the whole perimeter, and the wave count is
 * rounded to a whole number. Both are needed for the curve to meet itself at the start —
 * a fractional count closes the shape on a step, which at this size looks like a
 * rendering fault rather than a design.
 */
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

        // Inset by the amplitude so crests land on the layout bounds rather than being
        // clipped by them.
        val left = amp
        val top = amp
        val right = (size.width - amp).coerceAtLeast(left + 1f)
        val bottom = (size.height - amp).coerceAtLeast(top + 1f)

        val width = right - left
        val height = bottom - top

        // A radius larger than half the short side would make the arcs overlap.
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
                left = left,
                top = top,
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

    /**
     * Walks the rounded rectangle clockwise from the start of the top edge, returning
     * the point at [distance] along the perimeter together with its outward normal.
     */
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

        // Top edge, left to right.
        if (d < straightX) {
            return Offset(left + radius + d, top) to Offset(0f, -1f)
        }
        d -= straightX

        // Top-right corner, sweeping from pointing up to pointing right.
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

    /** On a circle the outward normal is the same direction as the radius. */
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

/** Enough samples that the edge reads as curved at thumbnail size. */
private const val SampleCount = 360
