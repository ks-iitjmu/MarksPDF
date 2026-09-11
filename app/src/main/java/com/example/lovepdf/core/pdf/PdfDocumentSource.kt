package com.example.lovepdf.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Intrinsic size of a page in PDF points (1/72 inch). */
data class PdfPageSize(val width: Int, val height: Int) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
}

/**
 * Thin, thread-safe wrapper around the platform PdfRenderer.
 *
 * PdfRenderer allows exactly one open page at a time and is not thread-safe, so every
 * render goes through a mutex. Rendering itself happens on the IO dispatcher, never on
 * the main thread — this is the single biggest factor in keeping scrolling jank-free.
 */
class PdfDocumentSource private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val scratchFile: File?,
    val pageSizes: List<PdfPageSize>,
) : AutoCloseable {

    private val mutex = Mutex()
    private var closed = false

    val pageCount: Int get() = pageSizes.size

    /**
     * Renders index into a bitmap targetWidthPx wide, height derived from the page
     * aspect ratio. Returns null if the document has been closed underneath us.
     */
    suspend fun render(
        index: Int,
        targetWidthPx: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed || index !in 0 until pageCount) return@withLock null

            val size = pageSizes[index]
            val width = targetWidthPx.coerceAtLeast(1)
            val height = (width / size.aspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, config)
            // PdfRenderer composites onto whatever is already in the bitmap and does not
            // clear it, so an un-erased bitmap renders black behind the page content.
            bitmap.eraseColor(android.graphics.Color.WHITE)

            renderer.openPage(index).use { page ->
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            bitmap
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
        scratchFile?.delete()
    }

    companion object {
        suspend fun open(context: Context, uri: Uri): PdfDocumentSource =
            withContext(Dispatchers.IO) {
                // Most SAF providers hand back a seekable fd, which PdfRenderer needs.
                // Some (cloud, streaming) don't — fall back to a cache copy.
                runCatching { openDirect(context, uri) }
                    .getOrElse { openViaCache(context, uri) }
            }

        private fun openDirect(context: Context, uri: Uri): PdfDocumentSource {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IOException("Can't open this file")
            return try {
                build(pfd, PdfRenderer(pfd), scratch = null)
            } catch (e: Exception) {
                runCatching { pfd.close() }
                throw e
            }
        }

        private fun openViaCache(context: Context, uri: Uri): PdfDocumentSource {
            val scratch = copyToCache(context, uri)
            val pfd = ParcelFileDescriptor.open(scratch, ParcelFileDescriptor.MODE_READ_ONLY)
            return try {
                build(pfd, PdfRenderer(pfd), scratch)
            } catch (e: Exception) {
                runCatching { pfd.close() }
                scratch.delete()
                throw e
            }
        }

        private fun build(
            pfd: ParcelFileDescriptor,
            renderer: PdfRenderer,
            scratch: File?
        ): PdfDocumentSource {
            // Page sizes are read once up front so the LazyColumn knows every item's
            // height immediately — that's what stops the scrollbar jumping around.
            val sizes = List(renderer.pageCount) { i ->
                renderer.openPage(i).use { PdfPageSize(it.width, it.height) }
            }
            return PdfDocumentSource(pfd, renderer, scratch, sizes)
        }

        private fun copyToCache(context: Context, uri: Uri): File {
            val out = File.createTempFile("lovepdf_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: throw IOException("Can't read this file")
            return out
        }
    }
}
