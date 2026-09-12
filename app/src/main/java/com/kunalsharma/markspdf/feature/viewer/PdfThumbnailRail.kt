package com.kunalsharma.markspdf.feature.viewer

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kunalsharma.markspdf.R
import com.kunalsharma.markspdf.core.pdf.PdfPageSize
import com.kunalsharma.markspdf.ui.components.WavyRectShape

@Composable
fun PdfThumbnailRail(
    pageSizes: List<PdfPageSize>,
    currentPage: Int,
    loadThumbnail: suspend (index: Int, widthPx: Int) -> Bitmap?,
    onPageSelected: (Int) -> Unit,
    nightMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val railState = rememberLazyListState()
    val density = LocalDensity.current

    var railWidthPx by remember { mutableIntStateOf(0) }
    val sidePadding = with(density) {
        ((railWidthPx.toDp() - SelectedWidth) / 2).coerceAtLeast(0.dp)
    }

    LaunchedEffect(currentPage, sidePadding) {
        val target = (currentPage - 1).coerceIn(0, pageSizes.lastIndex)
        railState.animateScrollToItem(target)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RailHeight)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                    1f to MaterialTheme.colorScheme.surfaceContainer,
                )
            )
            .onSizeChanged { railWidthPx = it.width },
        contentAlignment = Alignment.BottomCenter,
    ) {
        LazyRow(
            state = railState,
            modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(ThumbGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(count = pageSizes.size, key = { it }) { index ->
                FilmstripFrame(
                    index = index,
                    selected = index == currentPage - 1,
                    loadThumbnail = loadThumbnail,
                    nightMode = nightMode,
                    onClick = { onPageSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun FilmstripFrame(
    index: Int,
    selected: Boolean,
    loadThumbnail: suspend (Int, Int) -> Bitmap?,
    nightMode: Boolean,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = remember { with(density) { (SelectedWidth * 2).toPx().toInt() } }

    var bitmap by remember(index) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(index) {
        bitmap = loadThumbnail(index, widthPx)?.asImageBitmap()
    }
    val width by animateDpAsState(
        targetValue = if (selected) SelectedWidth else UnselectedWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "frameWidth",
    )
    val shape = if (selected) SelectedFrameShape else FrameShape

    Box(
        modifier = Modifier
            .size(width = width, height = FrameHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = stringResource(R.string.cd_page_number, index + 1),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (selected) 3.dp else 0.dp),
                contentScale = ContentScale.Crop,
                colorFilter = if (nightMode) PdfNightFilter else null,
            )
        }
    }
}
val RailHeight = 108.dp

private val FrameHeight = 62.dp
private val SelectedWidth = 48.dp
private val UnselectedWidth = 34.dp
private val ThumbGap = 4.dp
private val FrameShape = RoundedCornerShape(4.dp)

private val SelectedFrameShape = WavyRectShape(
    amplitude = 1.5.dp,
    waveLength = 13.dp,
    cornerRadius = 6.dp,
)
