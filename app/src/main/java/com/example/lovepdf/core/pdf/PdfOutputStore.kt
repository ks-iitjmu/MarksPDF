package com.example.lovepdf.core.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Creates output files in a "Love PDF" folder the user can actually find.
 *
 * Two paths, because the storage model changed at Android 10. On API 29+ this goes
 * through MediaStore, which needs no permission and handles name collisions by
 * appending a counter. Below that there's no MediaStore Downloads collection, so it
 * falls back to the public Downloads directory — which does need
 * WRITE_EXTERNAL_STORAGE, declared with `maxSdkVersion="28"` so it's never requested on
 * newer devices.
 *
 * App-specific storage was the easy alternative and is the wrong one: files there are
 * buried under Android/data and vanish on uninstall, which isn't where someone expects
 * a document they just created.
 */
object PdfOutputStore {

    const val FOLDER_NAME = "Love PDF"

    /** Only true on API 26–28, where public Downloads still needs a runtime grant. */
    val needsLegacyStoragePermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    suspend fun create(context: Context, displayName: String): Uri =
        withContext(Dispatchers.IO) {
            val fileName = withPdfExtension(displayName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createViaMediaStore(context, fileName)
            } else {
                createLegacyFile(fileName)
            }
        }

    /**
     * Clears the pending flag so the file becomes visible to other apps.
     *
     * Until this runs, a MediaStore entry created with IS_PENDING is invisible in the
     * file manager and unreadable by anything else. Skipping it leaves a file that looks
     * saved but can't be opened outside this app.
     */
    suspend fun finalise(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
        }
        Unit
    }

    /** Deletes a half-written file so a failed merge doesn't leave a broken PDF behind. */
    suspend fun discard(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        runCatching {
            if (uri.scheme == "file") {
                uri.path?.let { File(it).delete() }
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        }
        Unit
    }

    private fun createViaMediaStore(context: Context, fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        return context.contentResolver
            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Couldn't create the file in $FOLDER_NAME")
    }

    private fun createLegacyFile(fileName: String): Uri {
        val downloads = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloads, FOLDER_NAME)

        if (!folder.exists() && !folder.mkdirs()) {
            throw IOException("Couldn't create the $FOLDER_NAME folder")
        }

        return Uri.fromFile(uniqueFile(folder, fileName))
    }

    /** MediaStore de-duplicates names itself; on the legacy path we have to. */
    private fun uniqueFile(folder: File, fileName: String): File {
        val base = fileName.removeSuffix(".pdf")
        var candidate = File(folder, fileName)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(folder, "$base ($counter).pdf")
            counter++
        }
        return candidate
    }

    private fun withPdfExtension(name: String): String {
        val trimmed = name.trim().ifEmpty { "Merged document" }
        // Strip characters that are illegal in filenames on either storage path.
        val safe = trimmed.replace(Regex("[/\\\\:*?\"<>|]"), "").ifEmpty { "Merged document" }
        return if (safe.endsWith(".pdf", ignoreCase = true)) safe else "$safe.pdf"
    }
}
