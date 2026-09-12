package com.example.lovepdf.feature.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lovepdf.core.pdf.PdfPageSize

@Composable
fun PdfPageItem(
    index: Int,
    size: PdfPageSize,
    renderWidthPx: Int,
    loadBitmap: suspend (index: Int, widthPx: Int) -> android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
    nightMode: Boolean = false,
) {
    var preview by remember(index) { mutableStateOf<ImageBitmap?>(null) }
    var sharp by remember(index) { mutableStateOf<ImageBitmap?>(null) }

    val previewWidth = remember(renderWidthPx) {
        (renderWidthPx / PREVIEW_DIVISOR).coerceAtLeast(80)
    }

    LaunchedEffect(index, previewWidth) {
        if (sharp == null) preview = loadBitmap(index, previewWidth)?.asImageBitmap()
    }

    LaunchedEffect(index, renderWidthPx) {
        sharp = loadBitmap(index, renderWidthPx)?.asImageBitmap()
    }

    Surface(
        modifier = modifier
            .aspectRatio(size.aspectRatio)
            .semantics { contentDescription = "Page ${index + 1}" },
        shape = MaterialTheme.shapes.medium,
        color = if (nightMode) NightPageColor else MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            val bitmap = sharp ?: preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
                    colorFilter = if (nightMode) PdfNightFilter else null,
                )
            }
        }
    }
}

private const val PREVIEW_DIVISOR = 5

internal val PdfNightFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

private val NightPageColor = Color(0xFF000000)
