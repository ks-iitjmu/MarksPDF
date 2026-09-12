package com.kunalsharma.markspdf.core.pdf

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
import androidx.annotation.RequiresApi

object PdfOutputStore {

    const val FOLDER_NAME = "MarksPDF"
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
    suspend fun createInFolder(
        context: Context,
        folderName: String,
        displayName: String,
        mimeType: String,
    ): Uri = withContext(Dispatchers.IO) {
        val safeFolder = sanitise(folderName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME/$safeFolder"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Couldn't create the file in $safeFolder")
        } else {
            val downloads = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(File(downloads, FOLDER_NAME), safeFolder)
            if (!folder.exists() && !folder.mkdirs()) {
                throw IOException("Couldn't create the $safeFolder folder")
            }
            Uri.fromFile(uniqueFile(folder, displayName))
        }
    }
    fun folderLabel(folderName: String): String = "$FOLDER_NAME/${sanitise(folderName)}"

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

    @RequiresApi(Build.VERSION_CODES.Q)
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
    suspend fun saveCopy(context: Context, source: Uri, displayName: String): Uri =
        withContext(Dispatchers.IO) {
            val target = create(context, displayName)
            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: throw IOException("Couldn't read the document")
                val output = context.contentResolver.openOutputStream(target)
                    ?: throw IOException("Couldn't write to $FOLDER_NAME")

                input.use { from -> output.use { to -> from.copyTo(to) } }
                finalise(context, target)
                target
            } catch (e: Exception) {
                discard(context, target)
                throw e
            }
        }

    private fun withPdfExtension(name: String): String {
        val safe = sanitise(name)
        return if (safe.endsWith(".pdf", ignoreCase = true)) safe else "$safe.pdf"
    }
    private fun sanitise(name: String): String =
        name.trim()
            .removeSuffix(".pdf")
            .replace(Regex("[/\\\\:*?\"<>|]"), "")
            .trim()
            .ifEmpty { "Untitled" }
}
