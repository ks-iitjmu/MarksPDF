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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lovepdf.core.pdf.PdfPageSize
import com.example.lovepdf.ui.components.FabMenuAction
import com.example.lovepdf.ui.components.FabMenuScrim
import com.example.lovepdf.ui.components.WaveRingLoader
import com.example.lovepdf.ui.components.ToolsFabMenu
import com.example.lovepdf.ui.theme.pillSurfaceColor
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
    searchState: SearchState,
    onSearchQueryChange: (String) -> Unit,
    onSearchStep: (Int) -> Int?,
    loadBitmap: suspend (Int, Int) -> android.graphics.Bitmap?,
    loadThumbnail: suspend (Int, Int) -> android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
) {
    // Tapping the page hides everything but the document. For a reader this matters more
    // than any amount of polish on the controls themselves — most of the time the best
    // interface is none.
    var chromeVisible by remember { mutableStateOf(true) }
    var toolsExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var thumbnailsVisible by rememberSaveable { mutableStateOf(false) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var jumpRequest by remember { mutableStateOf(0 to 0) }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // ToolbarHeight is the pill itself; ContentGap is the space below it. Without the
    // gap the first page starts flush against the pill and the two read as one slab.
    val topInset = statusBarHeight + ToolbarHeight + ToolbarMargin * 2 + ContentGap
    val continuousBottomInset = FabSize + ToolbarMargin * 4 + ContentGap
    // The FAB is allowed to float over a page rather than having space reserved for it:
    // reserving a FAB-sized strip on every page shrinks the page on every screen to
    // avoid an overlap that only matters in one corner.
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
                        // A tap with the menu open means "put it away", not "go
                        // immersive" — otherwise the menu and the chrome both vanish and
                        // it reads as the tap having gone wrong.
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
            TitlePill(
                title = (state as? ViewerState.Ready)?.fileName ?: "Love PDF",
                subtitle = (state as? ViewerState.Ready)?.let {
                    "${currentIndex + 1} of ${it.pageCount}"
                },
                onBack = onClose,
                onOpenFile = onOpenFile,
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
            visible = chromeVisible && isReady,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    bottom = ToolbarMargin * 2 + if (thumbnailsVisible) RailHeight else 0.dp,
                ),
        ) {
            SearchFab(
                expanded = searchExpanded,
                state = searchState,
                onExpandedChange = { searchExpanded = it },
                onQueryChange = { query ->
                    onSearchQueryChange(query)
                },
                onStep = { delta ->
                    onSearchStep(delta)?.let { page ->
                        jumpRequest = jumpRequest.first + 1 to page
                    }
                },
            )
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

/**
 * Back on the left, identity in the middle, open on the right.
 *
 * The two side buttons are the same size, so the centred title actually sits on the
 * screen's centre line rather than being pushed off by a wider side. The page counter
 * is a subtitle here rather than its own floating pill — it's the same fact the title
 * describes, so two separate elements were competing to tell you where you are.
 */
@Composable
private fun TitlePill(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onOpenFile: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = ToolbarMargin)
            .fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        color = pillSurfaceColor(),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to home",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onOpenFile) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = "Open a PDF",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
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
            jumpRequest = jumpRequest,
            topContentPadding = topContentPadding,
            bottomContentPadding = bottomContentPadding,
            onPageChanged = onCurrentIndexChange,
            onToggleChrome = onToggleChrome,
        )
    }
}

/**
 * Page-by-page horizontal reader, used while the thumbnail rail is open.
 *
 * The zoom transform is applied to the pager, not to each page inside it. Transforming
 * the page alone meant the magnified page stayed trapped in its own slot and got clipped
 * at the slot's edges — you could zoom in but never pan out to the parts you'd zoomed
 * towards. Transforming the pager lets a zoomed page spread across the whole viewport,
 * which is how the continuous reader has always behaved.
 *
 * Pairing paging with the rail is deliberate: the rail is for navigating to a specific
 * page, and once you're navigating rather than reading, swiping one page at a time
 * matches what you're doing.
 */
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

    // A hair of clearance from the screen edges while the page sits at its natural size,
    // and none once magnified — at that point every pixel of width is wanted and the
    // margin is just a border you're trying to see past. Keyed off settledScale rather
    // than the live scale so the relayout happens after the pinch, not during it.
    val edgePadding = if (zoom.settledScale > 1.001f) 0.dp else PageEdgePadding

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) }
    }

    LaunchedEffect(jumpRequest) {
        if (jumpRequest.first > 0) pagerState.scrollToPage(jumpRequest.second)
    }

    // Swiping and panning are both horizontal drags, so they'd fight each other. Locking
    // the pager while zoomed means a drag pans the magnified page, which is what you
    // want at that moment; zoom back out and swiping returns.
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

            // Fit the whole page in its slot: constrained by height for tall pages, by
            // width for wide ones.
            val fittedWidthPx = minOf(
                slotWidthPx,
                slotHeightPx.coerceAtLeast(1f) * size.aspectRatio,
            )
            val renderWidthPx = (fittedWidthPx * zoom.settledScale)
                .roundToInt()
                .coerceAtLeast(1)

            // Which dimension runs out first decides which one the page is pinned to.
            // Always deriving width from height (or the reverse) overflows the slot the
            // other way whenever a page is proportioned differently from the screen.
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

/** Continuous vertical scrolling — the default reading mode. */
@Composable
private fun ContinuousReader(
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
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val zoom = rememberPdfZoomState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var viewportWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { onPageChanged(it) }
    }

    // Jumps land instantly rather than animating. Animating to a search hit 300 pages
    // away would scroll through every page in between, rendering each one on the way.
    LaunchedEffect(jumpRequest) {
        if (jumpRequest.first > 0) listState.scrollToItem(jumpRequest.second)
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
        Text(
            text = "Nothing open yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Pick a PDF to start reading.",
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
            text = "Opening",
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

/** Clearance from the screen edges in paged mode before any zoom is applied. */
private val PageEdgePadding = 6.dp
/** Matches the title pill's real height: a 48dp icon button plus its 6dp padding. */
private val ToolbarHeight = 60.dp

/** Clear space between the chrome and the document on both edges. */
private val ContentGap = 16.dp
private val ToolbarMargin = 8.dp
private val FabSize = 56.dp
