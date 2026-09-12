package com.kunalsharma.markspdf

import android.Manifest
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kunalsharma.markspdf.core.pdf.DocumentSharing
import com.kunalsharma.markspdf.core.pdf.PdfOutputStore
import com.kunalsharma.markspdf.feature.home.HomeScreen
import com.kunalsharma.markspdf.feature.images.ImagesToPdfScreen
import com.kunalsharma.markspdf.feature.images.PdfToImagesScreen
import com.kunalsharma.markspdf.feature.home.HomeViewModel
import com.kunalsharma.markspdf.feature.merge.MergeScreen
import com.kunalsharma.markspdf.core.settings.AppLanguage
import com.kunalsharma.markspdf.core.settings.AppSettings
import com.kunalsharma.markspdf.core.settings.withLocale
import com.kunalsharma.markspdf.feature.onboarding.IntroScreen
import com.kunalsharma.markspdf.feature.onboarding.LanguageDialog
import com.kunalsharma.markspdf.ui.components.dottedBackground
import com.kunalsharma.markspdf.feature.split.SplitScreen
import com.kunalsharma.markspdf.feature.tools.MergeState
import com.kunalsharma.markspdf.feature.tools.ImagesToPdfState
import com.kunalsharma.markspdf.feature.tools.ImagesToPdfViewModel
import com.kunalsharma.markspdf.feature.tools.PdfSplitViewModel
import com.kunalsharma.markspdf.feature.tools.PdfToImagesViewModel
import com.kunalsharma.markspdf.feature.tools.PdfToolsViewModel
import com.kunalsharma.markspdf.feature.viewer.PdfViewerScreen
import com.kunalsharma.markspdf.feature.viewer.PdfViewerViewModel
import com.kunalsharma.markspdf.feature.viewer.ViewerState
import com.kunalsharma.markspdf.ui.theme.Motion
import com.kunalsharma.markspdf.ui.theme.MarksPDFTheme
import androidx.core.net.toUri


private sealed interface Screen {
    data object Home : Screen
    data object Merge : Screen
    data object Split : Screen
    data object Images : Screen
    data object Export : Screen
    data object Viewer : Screen
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withLocale(AppSettings(newBase).languageTag()))
    }

    private var pendingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        pendingUri = incomingUri(intent)

        setContent {
            var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val darkMode = darkOverride ?: isSystemInDarkTheme()

            MarksPDFTheme(darkTheme = darkMode) {
                val settings = remember { AppSettings(this@MainActivity) }
                var showIntro by remember { mutableStateOf(!settings.hasSeenIntro()) }
                var language by remember {
                    mutableStateOf(
                        AppLanguage.fromTag(settings.languageTag()) ?: AppLanguage.English
                    )
                }
                var pickingLanguage by remember { mutableStateOf(false) }

                fun applyLanguage(next: AppLanguage) {
                    language = next
                    settings.setLanguageTag(next.tag)
                    recreate()
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
                        .dottedBackground()
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
                                onChangeLanguage = { pickingLanguage = true },
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
                                onSaveCopy = {
                                    val ready = viewerState as? ViewerState.Ready
                                    val uri = openDocumentUri
                                    if (ready != null && uri != null) {
                                        saveCopy(uri.toUri(), ready.fileName)
                                    }
                                },
                                onShare = { openDocumentUri?.let { share(it.toUri()) } },
                                onOpenWith = { openDocumentUri?.let { openWith(it.toUri()) } },
                                loadBitmap = viewer::pageBitmap,
                                loadThumbnail = viewer::thumbnailBitmap,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    // The introduction covers the app entirely until it's dismissed, so
                    // a first-time user never sees the home screen behind it.
                    if (showIntro) {
                        IntroScreen(
                            selectedLanguage = language,
                            onLanguageChange = { applyLanguage(it) },
                            onFinish = {
                                settings.setIntroSeen()
                                showIntro = false
                            },
                        )
                    }

                    if (pickingLanguage) {
                        LanguageDialog(
                            selected = language,
                            onSelect = { applyLanguage(it) },
                            onDismiss = { pickingLanguage = false },
                        )
                    }

                }
            }
        }
    }
    private fun saveCopy(uri: Uri, fileName: String) {
        lifecycleScope.launch {
            val message = try {
                PdfOutputStore.saveCopy(this@MainActivity, uri, fileName)
                R.string.toast_copy_saved
            } catch (_: Exception) {
                R.string.toast_copy_failed
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun share(uri: Uri) {
        startActivity(
            DocumentSharing.shareIntent(this, uri, getString(R.string.share_chooser_title))
        )
    }

    private fun openWith(uri: Uri) {
        startActivity(
            DocumentSharing.openWithIntent(this, uri, getString(R.string.open_with_title))
        )
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
