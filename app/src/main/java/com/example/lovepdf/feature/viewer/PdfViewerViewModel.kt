package com.example.lovepdf.feature.viewer

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovepdf.core.pdf.PageBitmapCache
import com.example.lovepdf.core.pdf.PdfDocumentSource
import com.example.lovepdf.core.pdf.PdfPageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed interface ViewerState {
    data object Empty : ViewerState
    data object Opening : ViewerState
    data class Ready(
        val fileName: String,
        val pageSizes: List<PdfPageSize>,
    ) : ViewerState {
        val pageCount: Int get() = pageSizes.size
    }

    data class Failed(val message: String) : ViewerState
}

class PdfViewerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Empty)
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    private var document: PdfDocumentSource? = null
    private var cache = PageBitmapCache()
    private var openJob: Job? = null

    // Two concurrent renders keeps both CPU cores busy without letting a burst of
    // fling-triggered requests queue up behind stale pages.
    private val renderLimit = Semaphore(2)

    /** Separate lane so thumbnails never queue ahead of the page being read. */
    private val thumbnailLimit = Semaphore(1)

    fun open(uri: Uri) {
        openJob?.cancel()
        openJob = viewModelScope.launch {
            _state.value = ViewerState.Opening
            releaseDocument()
            val startedAt = SystemClock.elapsedRealtime()
            try {
                val context = getApplication<Application>()
                val source = PdfDocumentSource.open(context, uri)
                document = source
                val ready = ViewerState.Ready(
                    fileName = displayName(uri),
                    pageSizes = source.pageSizes
                )
                awaitMinimumLoaderTime(startedAt)
                _state.value = ready
            } catch (e: Exception) {
                _state.value = ViewerState.Failed(
                    "This file couldn't be opened. It may be damaged or password protected."
                )
            }
        }
    }

    /** User-facing close: releases the file and returns to the empty state. */
    fun closeDocument() {
        releaseDocument()
        _state.value = ViewerState.Empty
    }

    private fun releaseDocument() {
        document?.close()
        document = null
        cache.clear()
    }

    /**
     * Returns the page bitmap, rendering it if it isn't cached. Callers should invoke
     * this from a LaunchedEffect so scrolling past a page cancels its pending render.
     */
    suspend fun pageBitmap(index: Int, widthPx: Int): Bitmap? {
        val source = document ?: return null
        val width = widthPx.coerceIn(1, MAX_RENDER_WIDTH_PX)

        cache[index, width]?.let { return it }

        return renderLimit.withPermit {
            // Re-check: another coroutine may have rendered it while we waited.
            cache[index, width] ?: source.render(index, width)?.also {
                cache.put(index, width, it)
            }
        }
    }

    /**
     * Low-resolution render for the thumbnail rail.
     *
     * Deliberately on its own single permit rather than sharing [renderLimit]. Opening
     * the rail on a 300-page document queues a lot of work, and without a separate lane
     * those thumbnails would sit ahead of the page you're actually reading.
     *
     * Config stays ARGB_8888 even though these are small and opaque — PdfRenderer
     * rejects other bitmap configs.
     */
    suspend fun thumbnailBitmap(index: Int, widthPx: Int): Bitmap? {
        val source = document ?: return null
        val width = widthPx.coerceIn(1, MAX_THUMB_WIDTH_PX)

        cache[index, width]?.let { return it }

        return thumbnailLimit.withPermit {
            cache[index, width] ?: source.render(index, width)?.also {
                cache.put(index, width, it)
            }
        }
    }

    fun onLowMemory() = cache.trim()

    /**
     * Holds the loading animation on screen for at least [MIN_LOADER_MILLIS].
     *
     * This is a floor, not an added delay: a document that genuinely takes three seconds
     * to open waits zero extra time. It only stops small files from flashing the loader
     * for 80ms, which reads as a glitch rather than as loading.
     */
    private suspend fun awaitMinimumLoaderTime(startedAt: Long) {
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (elapsed < MIN_LOADER_MILLIS) delay(MIN_LOADER_MILLIS - elapsed)
    }

    private suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
    }

    override fun onCleared() {
        releaseDocument()
        super.onCleared()
    }

    companion object {
        /** Caps a single page render so extreme zoom can't allocate a huge bitmap. */
        const val MAX_RENDER_WIDTH_PX = 2600

        /** Thumbnails never need more than this, whatever the screen density. */
        const val MAX_THUMB_WIDTH_PX = 320

        /** Tune this to taste — drop it to 0 to remove the wait entirely. */
        private const val MIN_LOADER_MILLIS = 2200L
    }
}
