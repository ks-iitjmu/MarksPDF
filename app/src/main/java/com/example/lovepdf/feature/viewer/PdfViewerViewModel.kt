package com.example.lovepdf.feature.viewer

import android.annotation.SuppressLint
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
import kotlin.time.Duration.Companion.milliseconds

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

    private val renderLimit = Semaphore(2)
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
            } catch (_: Exception) {
                _state.value = ViewerState.Failed(
                    "This file couldn't be opened. It may be damaged or password protected."
                )
            }
        }
    }

    fun closeDocument() {
        releaseDocument()
        _state.value = ViewerState.Empty
    }

    private fun releaseDocument() {
        document?.close()
        document = null
        cache.clear()
    }
    suspend fun pageBitmap(index: Int, widthPx: Int): Bitmap? {
        val source = document ?: return null
        val width = widthPx.coerceIn(1, MAX_RENDER_WIDTH_PX)

        cache[index, width]?.let { return it }

        return renderLimit.withPermit {
            cache[index, width] ?: source.render(index, width)?.also {
                cache.put(index, width, it)
            }
        }
    }
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

    private suspend fun awaitMinimumLoaderTime(startedAt: Long) {
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (elapsed < MIN_LOADER_MILLIS) delay((MIN_LOADER_MILLIS - elapsed).milliseconds)
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

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        releaseDocument()
        super.onCleared()
    }

    companion object {
        const val MAX_RENDER_WIDTH_PX = 2600
        const val MAX_THUMB_WIDTH_PX = 320
        private const val MIN_LOADER_MILLIS = 320L
    }
}
