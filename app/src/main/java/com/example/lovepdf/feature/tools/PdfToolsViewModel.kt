package com.example.lovepdf.feature.tools

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovepdf.core.pdf.PdfOutputStore
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

data class MergeItem(val uri: Uri, val name: String)

sealed interface MergeState {
    data object Editing : MergeState
    data object Working : MergeState
    data class Done(val result: Uri) : MergeState
    data class Failed(val message: String) : MergeState
}

class PdfToolsViewModel(app: Application) : AndroidViewModel(app) {

    private val _items = MutableStateFlow<List<MergeItem>>(emptyList())
    val items: StateFlow<List<MergeItem>> = _items.asStateFlow()

    private val _mergeState = MutableStateFlow<MergeState>(MergeState.Editing)
    val mergeState: StateFlow<MergeState> = _mergeState.asStateFlow()

    val canMerge: Boolean get() = _items.value.size >= 2

    fun addSources(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val existing = _items.value.map { it.uri }.toSet()
            val added = uris
                .filterNot { it in existing }
                .map { MergeItem(it, displayName(it)) }
            _items.value = _items.value + added
        }
    }

    fun remove(index: Int) {
        _items.value = _items.value.filterIndexed { i, _ -> i != index }
    }

    /** Moves an item one position up or down; the list order is the merge order. */
    fun move(index: Int, delta: Int) {
        val list = _items.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        _items.value = list
    }

    /**
     * Creates the output file, merges into it, then publishes it.
     *
     * The destination is created here rather than handed in from a save dialog: the app
     * owns its output folder, so the only thing left for the user to decide is the name.
     */
    fun merge(fileName: String) {
        val sources = _items.value.map { it.uri }
        if (sources.size < 2) return

        viewModelScope.launch {
            _mergeState.value = MergeState.Working

            val context = getApplication<Application>()
            var destination: Uri? = null

            _mergeState.value = try {
                val target = PdfOutputStore.create(context, fileName)
                destination = target
                PdfToolkit.merge(context, sources, target)
                PdfOutputStore.finalise(context, target)
                MergeState.Done(target)
            } catch (e: Exception) {
                // A half-written PDF is worse than none — it looks like a real file and
                // fails only when opened.
                destination?.let { PdfOutputStore.discard(context, it) }
                MergeState.Failed(
                    "The merge didn't finish. One of the files may be damaged or password protected."
                )
            }
        }
    }

    /**
     * Default name offered in the rename dialog.
     *
     * Fixed prefix rather than derived from the first file: merging an already-merged
     * file produced names like "report merged merged". A timestamp keeps successive
     * merges distinguishable in the folder without relying on MediaStore's "(1)" suffix.
     */
    fun suggestedName(): String {
        val stamp = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
        return "LovePDF_merge_$stamp"
    }

    /** Called when leaving the merge screen, so the next visit starts clean. */
    fun reset() {
        _items.value = emptyList()
        _mergeState.value = MergeState.Editing
    }

    fun dismissResult() {
        _mergeState.value = MergeState.Editing
    }

    private suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
    }
}
