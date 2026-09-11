package com.example.lovepdf.feature.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/**
 * Holds the pinch-zoom transform for the page list.
 *
 * [scale] and [offset] drive a graphicsLayer, so a pinch is a pure GPU transform — no
 * relayout, no re-render, no dropped frames. [settledScale] only updates once the
 * gesture ends, and that's what triggers a crisp re-render at the new resolution.
 */
@Stable
class PdfZoomState(
    private val minScale: Float = 1f,
    private val maxScale: Float = 6f,
) {
    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    /** The zoom level the pages should be *rendered* at. Lags [scale] until settle. */
    var settledScale by mutableFloatStateOf(1f)
        private set

    val isZoomed: Boolean get() = scale > 1.001f

    private val animatable = Animatable(1f)

    /**
     * Applies one gesture frame. Returns the vertical pan that couldn't be absorbed
     * because we hit the top or bottom of the zoomed viewport — the caller forwards
     * that to the list so panning flows into scrolling without a seam.
     */
    fun onGesture(centroid: Offset, pan: Offset, zoomChange: Float, viewport: IntSize): Float {
        val next = (scale * zoomChange).coerceIn(minScale, maxScale)
        val ratio = next / scale

        // Keep the point under the user's fingers pinned while scaling.
        val raw = (offset - centroid) * ratio + centroid + pan
        scale = next

        val maxX = (scale - 1f) * viewport.width
        val maxY = (scale - 1f) * viewport.height

        val clampedY = raw.y.coerceIn(-maxY, 0f)
        offset = Offset(raw.x.coerceIn(-maxX, 0f), clampedY)

        return raw.y - clampedY
    }

    fun settle() {
        settledScale = scale
    }

    /** Double-tap: toggle between fit-width and a comfortable reading zoom. */
    suspend fun toggle(focus: Offset, viewport: IntSize) {
        val target = if (isZoomed) 1f else 2.5f
        val start = scale
        animatable.snapTo(start)
        animatable.animateTo(target) {
            val ratio = value / scale
            val raw = (offset - focus) * ratio + focus
            scale = value
            val maxX = (scale - 1f) * viewport.width
            val maxY = (scale - 1f) * viewport.height
            offset = Offset(raw.x.coerceIn(-maxX, 0f), raw.y.coerceIn(-maxY, 0f))
        }
        settle()
    }

    fun reset() {
        scale = 1f
        settledScale = 1f
        offset = Offset.Zero
    }
}

@Composable
fun rememberPdfZoomState(): PdfZoomState = remember { PdfZoomState() }

/**
 * Pinch to zoom, drag to pan when zoomed.
 *
 * The key detail: at scale 1 this modifier consumes nothing, so single-finger drags go
 * straight through to the LazyColumn and keep its native fling. It only claims the
 * gesture once a second finger lands, or once the content is already zoomed in.
 */
fun Modifier.pdfZoomGestures(
    state: PdfZoomState,
    onOverscrollY: (Float) -> Unit,
): Modifier = this.pointerInput(state) {
    awaitEachGesture {
        var claimed = false
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.count { it.pressed }

            if (down >= 2 || (down == 1 && state.isZoomed)) claimed = true

            if (claimed) {
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                if (zoom != 1f || pan != Offset.Zero) {
                    val centroid = event.calculateCentroid(useCurrent = true)
                    val leftover = state.onGesture(centroid, pan, zoom, size)
                    if (leftover != 0f) onOverscrollY(leftover)
                    event.changes.forEach { if (it.pressed) it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })

        if (claimed) state.settle()
    }
}

/** Single tap toggles the chrome; double tap zooms in on the tapped point. */
fun Modifier.pdfTapGestures(
    onSingleTap: () -> Unit,
    onDoubleTap: (Offset, IntSize) -> Unit,
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onTap = { onSingleTap() },
        onDoubleTap = { position -> onDoubleTap(position, size) }
    )
}
