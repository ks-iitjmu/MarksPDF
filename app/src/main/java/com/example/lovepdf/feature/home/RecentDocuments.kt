package com.example.lovepdf.feature.home

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class RecentDocument(
    val uri: Uri,
    val name: String,
    val openedAt: Long,
)

/**
 * Recently opened documents, kept in SharedPreferences as a small JSON array.
 *
 * No database and no DataStore: this list is capped at [MAX_ENTRIES] and is read once
 * when the home screen appears. A Room dependency to store twelve rows would cost more
 * in build time and APK size than it could ever save.
 *
 * Entries store the SAF uri, which only stays readable if a persistable permission was
 * taken when the file was first opened. If the user later revokes it or the file moves,
 * opening from here fails the same way any missing file would — that's handled where the
 * document is opened, not here.
 */
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
                )
            }.sortedByDescending { it.openedAt }
        }.getOrDefault(emptyList())
    }

    fun record(uri: Uri, name: String) {
        // Re-opening a file moves it to the top rather than adding a duplicate.
        val updated = (listOf(RecentDocument(uri, name, System.currentTimeMillis())) +
            all().filterNot { it.uri == uri })
            .take(MAX_ENTRIES)

        val array = JSONArray()
        updated.forEach { doc ->
            array.put(
                JSONObject().apply {
                    put("uri", doc.uri.toString())
                    put("name", doc.name)
                    put("openedAt", doc.openedAt)
                }
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun remove(uri: Uri) {
        val remaining = all().filterNot { it.uri == uri }
        val array = JSONArray()
        remaining.forEach { doc ->
            array.put(
                JSONObject().apply {
                    put("uri", doc.uri.toString())
                    put("name", doc.name)
                    put("openedAt", doc.openedAt)
                }
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    private companion object {
        const val PREFS = "love_pdf_recents"
        const val KEY = "documents"
        const val MAX_ENTRIES = 12
    }
}
