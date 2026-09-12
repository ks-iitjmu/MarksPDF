package com.kunalsharma.markspdf.core.pdf

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
import androidx.core.graphics.createBitmap

data class PdfPageSize(val width: Int, val height: Int) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
}

class PdfDocumentSource private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val scratchFile: File?,
    val pageSizes: List<PdfPageSize>,
) : AutoCloseable {

    private val mutex = Mutex()
    private var closed = false

    val pageCount: Int get() = pageSizes.size

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

            val bitmap = createBitmap(width, height, config)
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
            val sizes = List(renderer.pageCount) { i ->
                renderer.openPage(i).use { PdfPageSize(it.width, it.height) }
            }
            return PdfDocumentSource(pfd, renderer, scratch, sizes)
        }

        private fun copyToCache(context: Context, uri: Uri): File {
            val out = File.createTempFile("RidhimaPDF   `", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: throw IOException("Can't read this file")
            return out
        }
    }
}
