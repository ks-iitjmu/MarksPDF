package com.example.lovepdf.feature.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovepdf.core.pdf.PdfToolkit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    /** Page indices containing the query, in document order. */
    val matches: List<Int> = emptyList(),
    /** Position within [matches], or -1 when there's nothing to step through. */
    val current: Int = -1,
    val indexing: Boolean = false,
    val indexProgress: Float = 0f,
) {
    val hasMatches: Boolean get() = matches.isNotEmpty()
    val searched: Boolean get() = query.isNotBlank() && !indexing
}

/**
 * Finds text in the open document.
 *
 * The page text is extracted once and kept, so the wait happens on the first search and
 * every search after that is instant. Re-reading a 770-page file on each keystroke would
 * make the feature unusable, and the extracted text is small next to the page bitmaps
 * already in memory.
 */
class PdfSearchViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var indexedUri: Uri? = null
    private var pageTexts: List<String> = emptyList()
    private var indexJob: Job? = null
    private var searchJob: Job? = null

    /** Called when a document opens, so a stale index is never searched. */
    fun onDocumentChanged(uri: Uri?) {
        indexJob?.cancel()
        searchJob?.cancel()
        if (uri != indexedUri) {
            indexedUri = null
            pageTexts = emptyList()
        }
        _state.value = SearchState()
    }

    fun setQuery(uri: Uri?, query: String) {
        _state.value = _state.value.copy(query = query)
        if (uri == null) return

        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(matches = emptyList(), current = -1)
            return
        }

        searchJob = viewModelScope.launch {
            ensureIndexed(uri)
            applyQuery(query)
        }
    }

    private suspend fun ensureIndexed(uri: Uri) {
        if (indexedUri == uri && pageTexts.isNotEmpty()) return

        _state.value = _state.value.copy(indexing = true, indexProgress = 0f)
        pageTexts = runCatching {
            PdfToolkit.extractPageTexts(getApplication(), uri) { done, total ->
                _state.value = _state.value.copy(
                    indexProgress = if (total > 0) done.toFloat() / total else 0f
                )
            }
        }.getOrDefault(emptyList())

        indexedUri = uri
        _state.value = _state.value.copy(indexing = false, indexProgress = 1f)
    }

    private fun applyQuery(query: String) {
        val needle = query.trim()
        val matches = pageTexts.mapIndexedNotNull { index, text ->
            index.takeIf { text.contains(needle, ignoreCase = true) }
        }
        _state.value = _state.value.copy(
            matches = matches,
            current = if (matches.isEmpty()) -1 else 0,
        )
    }

    /**
     * Steps to the next or previous match, wrapping at both ends.
     *
     * Wrapping rather than stopping: with the count shown alongside, wrapping reads as
     * "back to the start", while a dead button reads as broken.
     */
    fun step(delta: Int): Int? {
        val current = _state.value
        if (current.matches.isEmpty()) return null

        val size = current.matches.size
        val next = ((current.current + delta) % size + size) % size
        _state.value = current.copy(current = next)
        return current.matches[next]
    }

    /** Page index of the current match, for jumping straight after a search. */
    fun currentPage(): Int? {
        val current = _state.value
        return current.matches.getOrNull(current.current)
    }

    fun clear() {
        searchJob?.cancel()
        _state.value = SearchState()
    }
}
