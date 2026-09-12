package com.example.lovepdf.feature.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.lovepdf.core.pdf.PdfPageSize
import com.example.lovepdf.ui.components.FabMenuAction
import com.example.lovepdf.ui.components.AppLogo
import com.example.lovepdf.ui.components.PillHeader
import com.example.lovepdf.ui.components.PillIconButton
import com.example.lovepdf.ui.components.FabMenuScrim
import com.example.lovepdf.ui.components.WaveRingLoader
import com.example.lovepdf.ui.components.ToolsFabMenu
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PdfViewerScreen(
    state: ViewerState,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenFile: () -> Unit,
    onClose: () -> Unit,
    onSplitDocument: () -> Unit,
    loadBitmap: suspend (Int, Int) -> android.graphics.Bitmap?,
    loadThumbnail: suspend (Int, Int) -> android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
) {
    var chromeVisible by remember { mutableStateOf(true) }
    var toolsExpanded by remember { mutableStateOf(false) }
    var thumbnailsVisible by rememberSaveable { mutableStateOf(false) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var jumpRequest by remember { mutableStateOf(0 to 0) }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topInset = statusBarHeight + ToolbarHeight + ToolbarMargin * 2 + ContentGap
    val continuousBottomInset = FabSize + ToolbarMargin * 4 + ContentGap
    val pagedBottomInset = RailHeight + ContentGap
    val bottomInset = if (thumbnailsVisible) pagedBottomInset else continuousBottomInset

    val isReady = state is ViewerState.Ready

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(Modifier.fillMaxSize()) {
            when (state) {
                is ViewerState.Empty -> EmptyState(onOpenFile)
                is ViewerState.Opening -> BusyState()
                is ViewerState.Failed -> ErrorState(state.message, onOpenFile)
                is ViewerState.Ready -> PageScroller(
                    pageSizes = state.pageSizes,
                    loadBitmap = loadBitmap,
                    nightMode = darkMode,
                    pagedMode = thumbnailsVisible,
                    currentIndex = currentIndex,
                    jumpRequest = jumpRequest,
                    onCurrentIndexChange = { currentIndex = it },
                    onToggleChrome = {
                        if (toolsExpanded) toolsExpanded = false else chromeVisible = !chromeVisible
                    },
                    topContentPadding = topInset,
                    bottomContentPadding = bottomInset,
                )
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            PillHeader(
                title = (state as? ViewerState.Ready)?.fileName ?: "LovePDF",
                subtitle = (state as? ViewerState.Ready)?.let {
                    "${currentIndex + 1} of ${it.pageCount}"
                },
                leading = {
                    PillIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        label = "Back to home",
                        onClick = onClose,
                    )
                },
                trailing = {
                    PillIconButton(
                        icon = Icons.Outlined.FolderOpen,
                        label = "Open a PDF",
                        onClick = onOpenFile,
                    )
                },
            )
        }

        FabMenuScrim(expanded = toolsExpanded, onDismiss = { toolsExpanded = false })

        AnimatedVisibility(
            visible = thumbnailsVisible && chromeVisible && isReady,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val ready = state as? ViewerState.Ready
            if (ready != null) {
                PdfThumbnailRail(
                    pageSizes = ready.pageSizes,
                    currentPage = currentIndex + 1,
                    loadThumbnail = loadThumbnail,
                    nightMode = darkMode,
                    onPageSelected = { index ->
                        jumpRequest = jumpRequest.first + 1 to index
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = 20.dp,
                    bottom = ToolbarMargin * 2 + if (thumbnailsVisible) RailHeight else 0.dp,
                ),
        ) {
            ToolsFabMenu(
                expanded = toolsExpanded,
                onExpandedChange = { toolsExpanded = it },
                actions = buildList {
                    if (isReady) {
                        add(
                            FabMenuAction(
                                icon = Icons.Outlined.GridView,
                                label = if (thumbnailsVisible) "Hide pages" else "Browse pages",
                                active = thumbnailsVisible,
                                onClick = { thumbnailsVisible = !thumbnailsVisible },
                            )
                        )
                        add(
                            FabMenuAction(
                                icon = Icons.Outlined.ContentCut,
                                label = "Split",
                                onClick = onSplitDocument,
                            )
                        )
                    }
                    add(
                        FabMenuAction(
                            icon = if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            label = if (darkMode) "Light theme" else "Dark theme",
                            active = darkMode,
                            onClick = onToggleDarkMode,
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun PageScroller(
    pageSizes: List<PdfPageSize>,
    loadBitmap: suspend (Int, Int) -> android.graphics.Bitmap?,
    nightMode: Boolean,
    pagedMode: Boolean,
    currentIndex: Int,
    jumpRequest: Pair<Int, Int>,
    onCurrentIndexChange: (Int) -> Unit,
    onToggleChrome: () -> Unit,
    topContentPadding: androidx.compose.ui.unit.Dp,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
) {
    if (pagedMode) {
        PagedReader(
            pageSizes = pageSizes,
            loadBitmap = loadBitmap,
            nightMode = nightMode,
            startIndex = currentIndex,
            jumpRequest = jumpRequest,
            topContentPadding = topContentPadding,
            bottomContentPadding = bottomContentPadding,
            onPageChanged = onCurrentIndexChange,
            onToggleChrome = onToggleChrome,
        )
    } else {
        ContinuousReader(
            pageSizes = pageSizes,
            loadBitmap = loadBitmap,
            nightMode = nightMode,
            startIndex = currentIndex,
            topContentPadding = topContentPadding,
            bottomContentPadding = bottomContentPadding,
            onPageChanged = onCurrentIndexChange,
            onToggleChrome = onToggleChrome,
        )
    }
}
@Composable
private fun PagedReader(
    pageSizes: List<PdfPageSize>,
    loadBitmap: suspend (Int, Int) -> android.graphics.Bitmap?,
    nightMode: Boolean,
    startIndex: Int,
    jumpRequest: Pair<Int, Int>,
    topContentPadding: androidx.compose.ui.unit.Dp,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onPageChanged: (Int) -> Unit,
    onToggleChrome: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, pageSizes.lastIndex),
        pageCount = { pageSizes.size },
    )
    val zoom = rememberPdfZoomState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var viewportWidthPx by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }

    val edgePadding = if (zoom.settledScale > 1.001f) 0.dp else PageEdgePadding

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }

    LaunchedEffect(jumpRequest) {
        if (jumpRequest.first > 0) pagerState.scrollToPage(jumpRequest.second)
    }
    LaunchedEffect(pagerState.currentPage) { zoom.reset() }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged {
                viewportWidthPx = it.width
                viewportHeightPx = it.height
            }
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoom.isZoomed,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topContentPadding,
                    bottom = bottomContentPadding,
                    start = edgePadding,
                    end = edgePadding,
                )
                .pdfZoomGestures(state = zoom, onOverscrollY = { })
                .pdfTapGestures(
                    onSingleTap = onToggleChrome,
                    onDoubleTap = { position, viewport ->
                        scope.launch { zoom.toggle(position, viewport) }
                    },
                )
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            pageSpacing = PageGutter,
        ) { index ->
            val size = pageSizes[index]

            val slotWidthPx = viewportWidthPx.toFloat() -
                with(density) { (edgePadding * 2).toPx() }
            val slotHeightPx = viewportHeightPx.toFloat() -
                with(density) { (topContentPadding + bottomContentPadding).toPx() }
            val fittedWidthPx = minOf(
                slotWidthPx,
                slotHeightPx.coerceAtLeast(1f) * size.aspectRatio,
            )
            val renderWidthPx = (fittedWidthPx * zoom.settledScale)
                .roundToInt()
                .coerceAtLeast(1)
            val heightBound = slotWidthPx / slotHeightPx.coerceAtLeast(1f) > size.aspectRatio

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PdfPageItem(
                    index = index,
                    size = size,
                    renderWidthPx = renderWidthPx,
                    loadBitmap = loadBitmap,
                    nightMode = nightMode,
                    modifier = Modifier
                        .then(
                            if (heightBound) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()
                        )
                        .aspectRatio(size.aspectRatio),
                )
            }
        }
    }
}

@Composable
private fun ContinuousReader(
    pageSizes: List<PdfPageSize>,
    loadBitmap: suspend (Int, Int) -> android.graphics.Bitmap?,
    nightMode: Boolean,
    startIndex: Int,
    topContentPadding: androidx.compose.ui.unit.Dp,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onPageChanged: (Int) -> Unit,
    onToggleChrome: () -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val zoom = rememberPdfZoomState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var viewportWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { onPageChanged(it) }
    }

    val renderWidthPx by remember(viewportWidthPx) {
        derivedStateOf {
            val horizontalPaddingPx = with(density) { (PageGutter * 2).toPx() }
            ((viewportWidthPx - horizontalPaddingPx) * zoom.settledScale)
                .roundToInt()
                .coerceAtLeast(1)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewportWidthPx = it.width }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pdfZoomGestures(
                    state = zoom,
                    onOverscrollY = { leftover ->
                        scope.launch { listState.scrollBy(-leftover / zoom.scale) }
                    }
                )
                .pdfTapGestures(
                    onSingleTap = onToggleChrome,
                    onDoubleTap = { position, viewport ->
                        scope.launch { zoom.toggle(position, viewport) }
                    }
                )
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            contentPadding = PaddingValues(
                start = PageGutter,
                end = PageGutter,
                top = topContentPadding,
                bottom = bottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(PageGutter),
        ) {
            items(count = pageSizes.size, key = { it }) { index ->
                PdfPageItem(
                    index = index,
                    size = pageSizes[index],
                    renderWidthPx = renderWidthPx,
                    loadBitmap = loadBitmap,
                    nightMode = nightMode,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onOpenFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLogo(size = 64.dp, modifier = Modifier.padding(bottom = 20.dp))
        Text(
            text = "Nothing open yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Open a PDF to start reading.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onOpenFile) { Text("Open a PDF") }
    }
}

@Composable
private fun BusyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WaveRingLoader()
        Text(
            text = "Opening document",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 28.dp),
        )
    }
}

@Composable
private fun ErrorState(message: String, onOpenFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(onClick = onOpenFile) { Text("Pick another file") }
    }
}

private val PageGutter = 12.dp

private val PageEdgePadding = 6.dp
private val ToolbarHeight = 60.dp

private val ContentGap = 16.dp
private val ToolbarMargin = 8.dp
private val FabSize = 56.dp
