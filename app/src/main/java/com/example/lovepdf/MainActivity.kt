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
import com.example.lovepdf.core.pdf.PdfOutputStore
import com.example.lovepdf.feature.home.HomeScreen
import com.example.lovepdf.feature.home.HomeViewModel
import com.example.lovepdf.feature.merge.MergeScreen
import com.example.lovepdf.feature.split.SplitScreen
import com.example.lovepdf.feature.tools.MergeState
import com.example.lovepdf.feature.tools.PdfSplitViewModel
import com.example.lovepdf.feature.tools.PdfToolsViewModel
import com.example.lovepdf.feature.viewer.PdfSearchViewModel
import com.example.lovepdf.feature.viewer.PdfViewerScreen
import com.example.lovepdf.feature.viewer.PdfViewerViewModel
import com.example.lovepdf.feature.viewer.ViewerState
import com.example.lovepdf.ui.theme.LovePDFTheme

/**
 * Screens the app can show.
 *
 * Plain state rather than a navigation library. Three destinations with no deep links,
 * no nested graphs and no arguments beyond what the ViewModels already hold — a nav
 * dependency would add a manifest of concepts to describe something a sealed interface
 * describes completely.
 */
private sealed interface Screen {
    data object Home : Screen
    data object Merge : Screen
    data object Split : Screen
    data object Viewer : Screen
}

class MainActivity : ComponentActivity() {

    private var pendingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        pendingUri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data

        setContent {
            // Null means "follow the system"; the in-app toggle overrides from then on.
            var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val darkMode = darkOverride ?: isSystemInDarkTheme()

            LovePDFTheme(darkTheme = darkMode) {
                var screen by rememberSaveable(
                    stateSaver = ScreenSaver
                ) { mutableStateOf<Screen>(Screen.Home) }

                val viewer: PdfViewerViewModel = viewModel()
                val tools: PdfToolsViewModel = viewModel()
                val splitter: PdfSplitViewModel = viewModel()
                val search: PdfSearchViewModel = viewModel()
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

                // Tracked so the viewer can hand its document to the splitter without
                // reopening it through a picker.
                var openDocumentUri by rememberSaveable { mutableStateOf<String?>(null) }
                val searchState by search.state.collectAsState()

                // Dropping the index when the document changes matters more than it
                // looks: searching a new file against the previous file's text would
                // return hits on pages that don't contain the word.
                LaunchedEffect(openDocumentUri) {
                    search.onDocumentChanged(openDocumentUri?.let { Uri.parse(it) })
                }

                // Held until the viewer resolves the document's display name, which is
                // the only point a useful recents entry can be written.
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

                // API 26–28 still needs a runtime grant to write to public Downloads.
                // On newer devices MediaStore handles it and this never fires.
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

                fun startSplit(vm: PdfSplitViewModel, fileName: String) {
                    if (needsStorageGrant()) {
                        pendingWrite = PendingWrite.Split to fileName
                        storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        vm.run(fileName)
                    }
                }

                // Recents need a display name, which only exists once the document is
                // open. Recording here keeps that knowledge in one place.
                LaunchedEffect(viewerState, pendingRecord) {
                    val ready = viewerState as? ViewerState.Ready
                    val uri = pendingRecord
                    if (ready != null && uri != null) {
                        home.record(Uri.parse(uri), ready.fileName)
                        pendingRecord = null
                    }
                }

                // A finished merge goes straight to the reader. Stopping to confirm
                // would only ask the user to acknowledge something they already asked
                // for.
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
                            // Forward pushes in from the right, back slides out to it —
                            // the direction tells you where you are in the stack.
                            if (targetState == Screen.Home) {
                                (fadeIn() + slideInHorizontally { -it / 5 }) togetherWith
                                    (fadeOut() + slideOutHorizontally { it })
                            } else {
                                (fadeIn() + slideInHorizontally { it }) togetherWith
                                    (fadeOut() + slideOutHorizontally { -it / 5 })
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
                                onShareApp = { shareApp() },
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
                                        splitter.load(Uri.parse(it))
                                        screen = Screen.Split
                                    }
                                },
                                searchState = searchState,
                                onSearchQueryChange = { query ->
                                    search.setQuery(openDocumentUri?.let { Uri.parse(it) }, query)
                                },
                                onSearchStep = search::step,
                                loadBitmap = viewer::pageBitmap,
                                loadThumbnail = viewer::thumbnailBitmap,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    /** Plain text share — no deep link to promise, so the store listing is the target. */
    private fun shareApp() {
        val message = buildString {
            append("Love PDF — a fast, clean PDF reader and toolkit for Android.\n")
            append("https://play.google.com/store/apps/details?id=$packageName")
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(send, "Share Love PDF"))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) pendingUri = intent.data
    }

    companion object {
        private const val MIME_PDF = "application/pdf"
    }
}

/** Which tool is waiting on the legacy storage grant. */
private enum class PendingWrite { Merge, Split }

private val ScreenSaver = androidx.compose.runtime.saveable.Saver<Screen, String>(
    save = {
        when (it) {
            Screen.Home -> "home"
            Screen.Merge -> "merge"
            Screen.Split -> "split"
            Screen.Viewer -> "viewer"
        }
    },
    restore = {
        when (it) {
            "merge" -> Screen.Merge
            "split" -> Screen.Split
            "viewer" -> Screen.Viewer
            else -> Screen.Home
        }
    },
)
