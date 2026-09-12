package com.example.lovepdf

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.lovepdf.core.pdf.PdfOutputStore
import com.example.lovepdf.feature.home.HomeScreen
import com.example.lovepdf.feature.images.ImagesToPdfScreen
import com.example.lovepdf.feature.images.PdfToImagesScreen
import com.example.lovepdf.feature.home.HomeViewModel
import com.example.lovepdf.feature.merge.MergeScreen
import com.example.lovepdf.feature.splash.OpeningScreen
import com.example.lovepdf.feature.split.SplitScreen
import com.example.lovepdf.feature.tools.MergeState
import com.example.lovepdf.feature.tools.ImagesToPdfState
import com.example.lovepdf.feature.tools.ImagesToPdfViewModel
import com.example.lovepdf.feature.tools.PdfSplitViewModel
import com.example.lovepdf.feature.tools.PdfToImagesViewModel
import com.example.lovepdf.feature.tools.PdfToolsViewModel
import com.example.lovepdf.feature.viewer.PdfViewerScreen
import com.example.lovepdf.feature.viewer.PdfViewerViewModel
import com.example.lovepdf.feature.viewer.ViewerState
import com.example.lovepdf.ui.theme.Motion
import com.example.lovepdf.ui.theme.LovePDFTheme
import androidx.core.net.toUri
import kotlin.time.Duration.Companion.milliseconds


private sealed interface Screen {
    data object Home : Screen
    data object Merge : Screen
    data object Split : Screen
    data object Images : Screen
    data object Export : Screen
    data object Viewer : Screen
}

class MainActivity : ComponentActivity() {

    private var pendingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        pendingUri = incomingUri(intent)

        setContent {
            var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val darkMode = darkOverride ?: isSystemInDarkTheme()

            LovePDFTheme(darkTheme = darkMode) {
                var opening by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(OPENING_MILLIS.milliseconds)
                    opening = false
                }

                var screen by rememberSaveable(
                    stateSaver = ScreenSaver
                ) { mutableStateOf<Screen>(Screen.Home) }

                val viewer: PdfViewerViewModel = viewModel()
                val tools: PdfToolsViewModel = viewModel()
                val splitter: PdfSplitViewModel = viewModel()
                val images: ImagesToPdfViewModel = viewModel()
                val exporter: PdfToImagesViewModel = viewModel()
                val home: HomeViewModel = viewModel()

                val viewerState by viewer.state.collectAsState()
                val mergeItems by tools.items.collectAsState()
                val mergeState by tools.mergeState.collectAsState()
                val recents by home.recents.collectAsState()
                val splitState by splitter.state.collectAsState()
                val splitMode by splitter.mode.collectAsState()
                val splitFrom by splitter.fromPage.collectAsState()
                val splitTo by splitter.toPage.collectAsState()
                val splitParts by splitter.parts.collectAsState()
                val imageItems by images.items.collectAsState()
                val imageFit by images.fit.collectAsState()
                val imagesState by images.state.collectAsState()
                val exportState by exporter.state.collectAsState()
                val exportFormat by exporter.format.collectAsState()
                val exportQuality by exporter.quality.collectAsState()

                var openDocumentUri by rememberSaveable { mutableStateOf<String?>(null) }

                var pendingRecord by rememberSaveable { mutableStateOf<String?>(null) }

                fun openDocument(uri: Uri) {
                    viewer.open(uri)
                    pendingRecord = uri.toString()
                    openDocumentUri = uri.toString()
                    screen = Screen.Viewer
                }

                val filePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        openDocument(uri)
                    }
                }

                val mergeFilePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenMultipleDocuments()
                ) { uris -> tools.addSources(uris) }

                val splitFilePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) splitter.load(uri) }

                val imagePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenMultipleDocuments()
                ) { uris -> images.addSources(uris) }

                val exportFilePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) exporter.load(uri) }

                var pendingWrite by remember {
                    mutableStateOf<Pair<PendingWrite, String>?>(null)
                }
                val storagePermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val request = pendingWrite
                    pendingWrite = null
                    if (granted && request != null) {
                        when (request.first) {
                            PendingWrite.Merge -> tools.merge(request.second)
                            PendingWrite.Split -> splitter.run(request.second)
                            PendingWrite.Images -> images.create(request.second)
                            PendingWrite.Export -> exporter.export()
                        }
                    }
                }

                fun needsStorageGrant(): Boolean =
                    PdfOutputStore.needsLegacyStoragePermission &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ) != PackageManager.PERMISSION_GRANTED

                fun startMerge(fileName: String) {
                    if (needsStorageGrant()) {
                        pendingWrite = PendingWrite.Merge to fileName
                        storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        tools.merge(fileName)
                    }
                }

                fun startImages(fileName: String) {
                    if (needsStorageGrant()) {
                        pendingWrite = PendingWrite.Images to fileName
                        storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        images.create(fileName)
                    }
                }

                fun startExport() {
                    if (needsStorageGrant()) {
                        pendingWrite = PendingWrite.Export to ""
                        storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        exporter.export()
                    }
                }

                fun startSplit(vm: PdfSplitViewModel, fileName: String) {
                    if (needsStorageGrant()) {
                        pendingWrite = PendingWrite.Split to fileName
                        storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        vm.run(fileName)
                    }
                }
                LaunchedEffect(viewerState, pendingRecord) {
                    val ready = viewerState as? ViewerState.Ready
                    val uri = pendingRecord
                    if (ready != null && uri != null) {
                        home.record(uri.toUri(), ready.fileName, ready.pageCount)
                        pendingRecord = null
                    }
                }
                LaunchedEffect(imagesState) {
                    val done = imagesState as? ImagesToPdfState.Done ?: return@LaunchedEffect
                    val result = done.result
                    images.reset()
                    openDocument(result)
                }

                LaunchedEffect(mergeState) {
                    val done = mergeState as? MergeState.Done ?: return@LaunchedEffect
                    val result = done.result
                    tools.reset()
                    openDocument(result)
                }

                LaunchedEffect(pendingUri) {
                    pendingUri?.let {
                        openDocument(it)
                        pendingUri = null
                    }
                }

                BackHandler(enabled = screen != Screen.Home) {
                    when (screen) {
                        Screen.Merge -> tools.reset()
                        Screen.Split -> splitter.reset()
                        Screen.Images -> images.reset()
                        Screen.Export -> exporter.reset()
                        Screen.Viewer -> viewer.closeDocument()
                        else -> Unit
                    }
                    screen = Screen.Home
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            if (targetState == Screen.Home) {
                                (fadeIn(Motion.enter()) +
                                    slideInHorizontally(Motion.slideSpec) { -it / 6 }) togetherWith
                                    (fadeOut(Motion.exit()) +
                                        slideOutHorizontally(Motion.slideSpec) { it / 3 })
                            } else {
                                (fadeIn(Motion.enter()) +
                                    slideInHorizontally(Motion.slideSpec) { it / 3 }) togetherWith
                                    (fadeOut(Motion.exit()) +
                                        slideOutHorizontally(Motion.slideSpec) { -it / 6 })
                            }
                        },
                        label = "screen",
                    ) { current ->
                        when (current) {
                            Screen.Home -> HomeScreen(
                                recents = recents,
                                darkMode = darkMode,
                                onToggleDarkMode = { darkOverride = !darkMode },
                                onOpenFile = { filePicker.launch(arrayOf(MIME_PDF)) },
                                onOpenRecent = { openDocument(it.uri) },
                                onForgetRecent = { home.forget(it.uri) },
                                onMerge = { screen = Screen.Merge },
                                onSplit = { screen = Screen.Split },
                                onImagesToPdf = { screen = Screen.Images },
                                onPdfToImages = { screen = Screen.Export },
                                onExitApp = { finish() },
                            )

                            Screen.Merge -> MergeScreen(
                                items = mergeItems,
                                mergeState = mergeState,
                                onBack = {
                                    tools.reset()
                                    screen = Screen.Home
                                },
                                onAddFiles = { mergeFilePicker.launch(arrayOf(MIME_PDF)) },
                                onRemove = tools::remove,
                                onMove = tools::move,
                                suggestedName = tools.suggestedName(),
                                onStartMerge = { name -> startMerge(name) },
                                onDismissResult = {
                                    tools.reset()
                                    screen = Screen.Home
                                },
                            )

                            Screen.Split -> SplitScreen(
                                state = splitState,
                                mode = splitMode,
                                fromPage = splitFrom,
                                toPage = splitTo,
                                parts = splitParts,
                                suggestedName = splitter.suggestedName(),
                                onBack = {
                                    splitter.reset()
                                    screen = Screen.Home
                                },
                                onPickFile = { splitFilePicker.launch(arrayOf(MIME_PDF)) },
                                onModeChange = splitter::setMode,
                                onFromPageChange = splitter::setFromPage,
                                onToPageChange = splitter::setToPage,
                                onPartsChange = splitter::setParts,
                                onRun = { name -> startSplit(splitter, name) },
                                onOpenResult = { uri ->
                                    splitter.reset()
                                    openDocument(uri)
                                },
                                onDismissResult = {
                                    splitter.reset()
                                    screen = Screen.Home
                                },
                            )

                            Screen.Images -> ImagesToPdfScreen(
                                items = imageItems,
                                fit = imageFit,
                                state = imagesState,
                                suggestedName = images.suggestedName(),
                                onBack = {
                                    images.reset()
                                    screen = Screen.Home
                                },
                                onAddImages = { imagePicker.launch(arrayOf(MIME_IMAGE)) },
                                onRemove = images::remove,
                                onMove = images::move,
                                onFitChange = images::setFit,
                                onCreate = { name -> startImages(name) },
                                onDismissResult = images::dismissResult,
                            )

                            Screen.Export -> PdfToImagesScreen(
                                state = exportState,
                                format = exportFormat,
                                quality = exportQuality,
                                onBack = {
                                    exporter.reset()
                                    screen = Screen.Home
                                },
                                onPickFile = { exportFilePicker.launch(arrayOf(MIME_PDF)) },
                                onFormatChange = exporter::setFormat,
                                onQualityChange = exporter::setQuality,
                                onExport = { startExport() },
                                onDismissResult = {
                                    exporter.reset()
                                    screen = Screen.Home
                                },
                            )

                            Screen.Viewer -> PdfViewerScreen(
                                state = viewerState,
                                darkMode = darkMode,
                                onToggleDarkMode = { darkOverride = !darkMode },
                                onOpenFile = { filePicker.launch(arrayOf(MIME_PDF)) },
                                onClose = {
                                    viewer.closeDocument()
                                    screen = Screen.Home
                                },
                                onSplitDocument = {
                                    openDocumentUri?.let {
                                        splitter.load(it.toUri())
                                        screen = Screen.Split
                                    }
                                },
                                loadBitmap = viewer::pageBitmap,
                                loadThumbnail = viewer::thumbnailBitmap,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = opening,
                        enter = fadeIn(),
                        exit = fadeOut(Motion.exit()),
                    ) {
                        OpeningScreen()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingUri(intent)?.let { pendingUri = it }
    }

    @Suppress("DEPRECATION")
    private fun incomingUri(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri

        else -> null
    }

    companion object {
        private const val MIME_PDF = "application/pdf"

        private const val OPENING_MILLIS = 1400L
        private const val MIME_IMAGE = "image/*"
    }
}

private enum class PendingWrite { Merge, Split, Images, Export }

private val ScreenSaver = androidx.compose.runtime.saveable.Saver<Screen, String>(
    save = {
        when (it) {
            Screen.Home -> "home"
            Screen.Merge -> "merge"
            Screen.Split -> "split"
            Screen.Images -> "images"
            Screen.Export -> "export"
            Screen.Viewer -> "viewer"
        }
    },
    restore = {
        when (it) {
            "merge" -> Screen.Merge
            "split" -> Screen.Split
            "images" -> Screen.Images
            "export" -> Screen.Export
            "viewer" -> Screen.Viewer
            else -> Screen.Home
        }
    },
)
