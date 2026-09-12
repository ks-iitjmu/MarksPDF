package com.example.lovepdf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { ThumbWidth.toPx() }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(valueRange)

    var widthPx by remember { mutableFloatStateOf(0f) }

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.secondaryContainer

    fun reportValueAt(x: Float) {
        if (widthPx <= 0f) return
        val usable = (widthPx - thumbWidthPx).coerceAtLeast(1f)
        val position = ((x - thumbWidthPx / 2f) / usable).coerceIn(0f, 1f)
        val range = currentRange
        currentOnValueChange(range.start + position * (range.endInclusive - range.start))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .progressSemantics(value, valueRange)
            .pointerInput(Unit) {
                detectTapGestures { reportValueAt(it.x) }
            }
            .pointerInput(Unit) {
                var dragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragX = it.x },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragX += amount
                        reportValueAt(dragX)
                    },
                )
            }
    ) {
        widthPx = size.width

        val trackHeight = TrackHeight.toPx()
        val trackTop = (size.height - trackHeight) / 2f
        val radius = CornerRadius(trackHeight / 2f)

        val usable = (size.width - thumbWidthPx).coerceAtLeast(1f)
        val thumbCentre = thumbWidthPx / 2f + fraction * usable
        val gap = TrackGap.toPx()
        val activeWidth = (thumbCentre - gap).coerceAtLeast(0f)
        if (activeWidth > 0f) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, trackTop),
                size = Size(activeWidth, trackHeight),
                cornerRadius = radius,
            )
        }

        val inactiveStart = (thumbCentre + gap).coerceAtMost(size.width)
        if (inactiveStart < size.width) {
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(inactiveStart, trackTop),
                size = Size(size.width - inactiveStart, trackHeight),
                cornerRadius = radius,
            )
            val dotCentre = size.width - trackHeight / 2f
            if (dotCentre > inactiveStart + StopDotRadius.toPx()) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.55f),
                    radius = StopDotRadius.toPx(),
                    center = Offset(dotCentre, size.height / 2f),
                )
            }
        }

        val thumbHeight = ThumbHeight.toPx()
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(thumbCentre - thumbWidthPx / 2f, (size.height - thumbHeight) / 2f),
            size = Size(thumbWidthPx, thumbHeight),
            cornerRadius = CornerRadius(thumbWidthPx / 2f),
        )
    }
}

private val SliderHeight = 48.dp
private val TrackHeight = 22.dp
private val ThumbWidth = 5.dp
private val ThumbHeight = 38.dp
private val TrackGap = 7.dp
private val StopDotRadius = 2.5.dp
