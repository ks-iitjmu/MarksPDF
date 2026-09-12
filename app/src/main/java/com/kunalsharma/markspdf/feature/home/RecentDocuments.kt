package com.kunalsharma.markspdf.feature.home

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

data class RecentDocument(
    val uri: Uri,
    val name: String,
    val openedAt: Long,
    val pages: Int = 0,
)

class RecentDocumentsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(): List<RecentDocument> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RecentDocument(
                    uri = Uri.parse(obj.getString("uri")),
                    name = obj.getString("name"),
                    openedAt = obj.getLong("openedAt"),
                    pages = obj.optInt("pages", 0),
                )
            }.sortedByDescending { it.openedAt }
        }.getOrDefault(emptyList())
    }

    fun record(uri: Uri, name: String, pages: Int) {
        val entry = RecentDocument(uri, name, System.currentTimeMillis(), pages)
        write((listOf(entry) + all().filterNot { it.uri == uri }).take(MAX_ENTRIES))
    }

    fun remove(uri: Uri) = write(all().filterNot { it.uri == uri })

    private fun write(documents: List<RecentDocument>) {
        val array = JSONArray()
        documents.forEach { doc ->
            array.put(
                JSONObject().apply {
                    put("uri", doc.uri.toString())
                    put("name", doc.name)
                    put("openedAt", doc.openedAt)
                    put("pages", doc.pages)
                }
            )
        }
        prefs.edit { putString(KEY, array.toString()) }
    }

    private companion object {
        const val PREFS = "marks_pdf_recents"
        const val KEY = "documents"
        const val MAX_ENTRIES = 12
    }
}
