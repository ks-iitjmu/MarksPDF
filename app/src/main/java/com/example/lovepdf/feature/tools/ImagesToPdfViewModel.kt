package com.example.lovepdf.feature.tools

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovepdf.core.pdf.PdfOutputStore
import com.example.lovepdf.core.pdf.PdfPageFit
import com.example.lovepdf.core.pdf.PdfToolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImageItem(val uri: Uri, val name: String)

sealed interface ImagesToPdfState {
    data object Editing : ImagesToPdfState
    data class Working(val progress: Float) : ImagesToPdfState
    data class Done(val result: Uri) : ImagesToPdfState
    data class Failed(val message: String) : ImagesToPdfState
}

class ImagesToPdfViewModel(app: Application) : AndroidViewModel(app) {

    private val _items = MutableStateFlow<List<ImageItem>>(emptyList())
    val items: StateFlow<List<ImageItem>> = _items.asStateFlow()

    private val _fit = MutableStateFlow(PdfPageFit.MatchImage)
    val fit: StateFlow<PdfPageFit> = _fit.asStateFlow()

    private val _state = MutableStateFlow<ImagesToPdfState>(ImagesToPdfState.Editing)
    val state: StateFlow<ImagesToPdfState> = _state.asStateFlow()

    fun addSources(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val existing = _items.value.map { it.uri }.toSet()
            val added = uris.filterNot { it in existing }.map { ImageItem(it, displayName(it)) }
            _items.value += added
        }
    }

    fun remove(index: Int) {
        _items.value = _items.value.filterIndexed { i, _ -> i != index }
    }

    fun move(index: Int, delta: Int) {
        val list = _items.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        list.add(target, list.removeAt(index))
        _items.value = list
    }

    fun setFit(next: PdfPageFit) {
        _fit.value = next
    }

    fun create(fileName: String) {
        val sources = _items.value.map { it.uri }
        if (sources.isEmpty()) return

        val context = getApplication<Application>()
        var destination: Uri? = null

        viewModelScope.launch {
            _state.value = ImagesToPdfState.Working(0f)
            _state.value = try {
                val target = PdfOutputStore.create(context, fileName)
                destination = target

                PdfToolkit.imagesToPdf(
                    context = context,
                    sources = sources,
                    destination = target,
                    fit = _fit.value,
                ) { done, total ->
                    _state.value = ImagesToPdfState.Working(
                        if (total > 0) done.toFloat() / total else 0f
                    )
                }

                PdfOutputStore.finalise(context, target)
                ImagesToPdfState.Done(target)
            } catch (_: Exception) {
                destination?.let { PdfOutputStore.discard(context, it) }
                ImagesToPdfState.Failed(
                    "The PDF couldn't be created. One of the images may be in an unsupported format."
                )
            }
        }
    }

    fun suggestedName(): String {
        val stamp = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
        return "LovePDF_scan_$stamp"
    }

    fun reset() {
        _items.value = emptyList()
        _fit.value = PdfPageFit.MatchImage
        _state.value = ImagesToPdfState.Editing
    }

    fun dismissResult() {
        _state.value = ImagesToPdfState.Editing
    }

    private suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Image"
    }
}
