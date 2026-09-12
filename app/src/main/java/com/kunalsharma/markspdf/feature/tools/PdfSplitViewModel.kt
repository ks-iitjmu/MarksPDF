package com.kunalsharma.markspdf.feature.tools

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import com.kunalsharma.markspdf.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunalsharma.markspdf.core.pdf.PdfOutputStore
import com.kunalsharma.markspdf.core.settings.withMinimumDuration
import com.kunalsharma.markspdf.core.pdf.PdfToolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

enum class SplitMode {
    Range,

    EveryPage,

    Parts,
}

sealed interface SplitState {
    data object NoFile : SplitState
    data object Loading : SplitState
    data class Ready(val name: String, val pageCount: Int) : SplitState
    data object Working : SplitState
    data class Done(val results: List<Uri>) : SplitState
    data class Failed(val message: String) : SplitState
}

class PdfSplitViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<SplitState>(SplitState.NoFile)
    val state: StateFlow<SplitState> = _state.asStateFlow()

    private val _mode = MutableStateFlow(SplitMode.Range)
    val mode: StateFlow<SplitMode> = _mode.asStateFlow()

    private val _fromPage = MutableStateFlow(1)
    val fromPage: StateFlow<Int> = _fromPage.asStateFlow()

    private val _toPage = MutableStateFlow(1)
    val toPage: StateFlow<Int> = _toPage.asStateFlow()

    private val _parts = MutableStateFlow(2)
    val parts: StateFlow<Int> = _parts.asStateFlow()

    private var source: Uri? = null

    fun load(uri: Uri) {
        source = uri
        viewModelScope.launch {
            _state.value = SplitState.Loading
            _state.value = withMinimumDuration {
                try {
                    val context = getApplication<Application>()
                    val pages = PdfToolkit.pageCount(context, uri)
                    _fromPage.value = 1
                    _toPage.value = pages
                    _parts.value = 2.coerceAtMost(pages.coerceAtLeast(2))
                    SplitState.Ready(displayName(uri), pages)
                } catch (_: Exception) {
                    SplitState.Failed(getApplication<Application>().getString(R.string.error_read_failed))
                }
            }
        }
    }

    fun setMode(next: SplitMode) {
        _mode.value = next
    }

    fun setFromPage(value: Int) {
        val pages = (_state.value as? SplitState.Ready)?.pageCount ?: return
        _fromPage.value = value.coerceIn(1, pages)
        if (_toPage.value < _fromPage.value) _toPage.value = _fromPage.value
    }

    fun setToPage(value: Int) {
        val pages = (_state.value as? SplitState.Ready)?.pageCount ?: return
        _toPage.value = value.coerceIn(1, pages)
        if (_fromPage.value > _toPage.value) _fromPage.value = _toPage.value
    }

    fun setParts(value: Int) {
        val pages = (_state.value as? SplitState.Ready)?.pageCount ?: return
        _parts.value = value.coerceIn(2, pages.coerceAtLeast(2))
    }

    fun run(baseName: String) {
        val uri = source ?: return
        val ready = _state.value as? SplitState.Ready ?: return
        val context = getApplication<Application>()
        val written = mutableListOf<Uri>()

        viewModelScope.launch {
            _state.value = SplitState.Working
            _state.value = withMinimumDuration {
                try {
                    val results = when (_mode.value) {
                        SplitMode.Range -> {
                            val target = PdfOutputStore.create(context, baseName)
                            written += target
                            PdfToolkit.extractRange(
                                context = context,
                                source = uri,
                                fromPage = _fromPage.value - 1,
                                toPage = _toPage.value - 1,
                                destination = target,
                            )
                            PdfOutputStore.finalise(context, target)
                            listOf(target)
                        }

                        SplitMode.EveryPage -> chunked(context, baseName, 1, written)

                        SplitMode.Parts -> {
                            val perFile = ceil(ready.pageCount.toDouble() / _parts.value).toInt()
                            chunked(context, baseName, perFile.coerceAtLeast(1), written)
                        }
                    }
                    SplitState.Done(results)
                } catch (_: Exception) {
                    written.forEach { PdfOutputStore.discard(context, it) }
                    SplitState.Failed(getApplication<Application>().getString(R.string.error_split_failed))
                }
            }
        }
    }

    private suspend fun chunked(
        context: Application,
        baseName: String,
        pagesPerFile: Int,
        written: MutableList<Uri>,
    ): List<Uri> {
        val results = PdfToolkit.splitIntoChunks(
            context = context,
            source = source ?: return emptyList(),
            pagesPerFile = pagesPerFile,
        ) { index, count ->
            val width = count.toString().length
            val suffix = (index + 1).toString().padStart(width, '0')
            PdfOutputStore.create(context, "${baseName}_$suffix").also { written += it }
        }
        results.forEach { PdfOutputStore.finalise(context, it) }
        return results
    }

    fun suggestedName(): String {
        val base = (_state.value as? SplitState.Ready)?.name?.removeSuffix(".pdf")
        return if (base.isNullOrBlank()) "MarksPDF_split" else "${base}_split"
    }

    fun reset() {
        source = null
        _state.value = SplitState.NoFile
        _mode.value = SplitMode.Range
    }

    private suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: getApplication<Application>().getString(R.string.fallback_document)
    }
}
