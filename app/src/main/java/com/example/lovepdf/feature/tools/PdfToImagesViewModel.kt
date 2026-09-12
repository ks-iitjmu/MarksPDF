package com.example.lovepdf.feature.tools

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovepdf.core.pdf.ExportQuality
import com.example.lovepdf.core.pdf.ImageFormat
import com.example.lovepdf.core.pdf.PdfOutputStore
import com.example.lovepdf.core.pdf.PdfRasterizer
import com.example.lovepdf.core.pdf.pdfPageCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PdfToImagesState {
    data object NoFile : PdfToImagesState
    data object Loading : PdfToImagesState
    data class Ready(val name: String, val pageCount: Int) : PdfToImagesState
    data class Working(val progress: Float, val done: Int, val total: Int) : PdfToImagesState
    data class Done(val count: Int, val folderLabel: String) : PdfToImagesState
    data class Failed(val message: String) : PdfToImagesState
}

class PdfToImagesViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<PdfToImagesState>(PdfToImagesState.NoFile)
    val state: StateFlow<PdfToImagesState> = _state.asStateFlow()

    private val _format = MutableStateFlow(ImageFormat.Jpeg)
    val format: StateFlow<ImageFormat> = _format.asStateFlow()

    private val _quality = MutableStateFlow(ExportQuality.Standard)
    val quality: StateFlow<ExportQuality> = _quality.asStateFlow()

    private var source: Uri? = null

    fun load(uri: Uri) {
        source = uri
        viewModelScope.launch {
            _state.value = PdfToImagesState.Loading
            _state.value = try {
                val context = getApplication<Application>()
                PdfToImagesState.Ready(displayName(uri), pdfPageCount(context, uri))
            } catch (_: Exception) {
                PdfToImagesState.Failed(
                    "This file couldn't be read. It may be damaged or password protected."
                )
            }
        }
    }

    fun setFormat(next: ImageFormat) {
        _format.value = next
    }

    fun setQuality(next: ExportQuality) {
        _quality.value = next
    }

    fun export() {
        val uri = source ?: return
        val ready = _state.value as? PdfToImagesState.Ready ?: return
        val context = getApplication<Application>()
        val folder = ready.name.removeSuffix(".pdf")
        var written = emptyList<Uri>()

        viewModelScope.launch {
            _state.value = PdfToImagesState.Working(0f, 0, ready.pageCount)
            _state.value = try {
                written = PdfRasterizer.exportPages(
                    context = context,
                    source = uri,
                    folderName = folder,
                    format = _format.value,
                    quality = _quality.value,
                ) { done, total ->
                    _state.value = PdfToImagesState.Working(
                        progress = if (total > 0) done.toFloat() / total else 0f,
                        done = done,
                        total = total,
                    )
                }

                PdfToImagesState.Done(written.size, PdfOutputStore.folderLabel(folder))
            } catch (_: Exception) {
                written.forEach { PdfOutputStore.discard(context, it) }
                PdfToImagesState.Failed(
                    "The export couldn't be completed. There may not be enough free space on the device."
                )
            }
        }
    }

    fun reset() {
        source = null
        _state.value = PdfToImagesState.NoFile
    }

    private suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
    }
}
