package com.example.lovepdf.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.core.graphics.createBitmap

enum class ImageFormat(val mimeType: String, val extension: String) {
    Jpeg("image/jpeg", "jpg"),
    Png("image/png", "png"),
}

enum class ExportQuality(val targetWidthPx: Int) {
    Standard(1240),
    High(2480),
}
object PdfRasterizer {

    suspend fun exportPages(
        context: Context,
        source: Uri,
        folderName: String,
        format: ImageFormat,
        quality: ExportQuality,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Uri> = withContext(Dispatchers.IO) {
        val descriptor = context.contentResolver.openFileDescriptor(source, "r")
            ?: throw IOException("Couldn't read the document")

        val written = mutableListOf<Uri>()

        descriptor.use { fd ->
            PdfRenderer(fd).use { renderer ->
                val total = renderer.pageCount
                val digits = total.toString().length

                for (index in 0 until total) {
                    val bitmap = renderPage(renderer, index, quality.targetWidthPx)

                    val name = "page_${(index + 1).toString().padStart(digits, '0')}." +
                        format.extension

                    val target = PdfOutputStore.createInFolder(
                        context = context,
                        folderName = folderName,
                        displayName = name,
                        mimeType = format.mimeType,
                    )
                    written += target

                    val output = context.contentResolver.openOutputStream(target)
                        ?: throw IOException("Couldn't write $name")

                    output.use { stream ->
                        val codec = when (format) {
                            ImageFormat.Jpeg -> Bitmap.CompressFormat.JPEG
                            ImageFormat.Png -> Bitmap.CompressFormat.PNG
                        }
                        bitmap.compress(codec, JPEG_QUALITY, stream)
                    }

                    PdfOutputStore.finalise(context, target)
                    bitmap.recycle()
                    onProgress(index + 1, total)
                }
            }
        }

        written
    }

    private fun renderPage(renderer: PdfRenderer, index: Int, targetWidthPx: Int): Bitmap {
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width
            val height = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = createBitmap(targetWidthPx, height)
            Canvas(bitmap).drawColor(Color.WHITE)

            page.render(
                bitmap,
                null,
                Matrix().apply { setScale(scale, scale) },
                PdfRenderer.Page.RENDER_MODE_FOR_PRINT,
            )

            return bitmap
        }
    }
    private const val JPEG_QUALITY = 92
}

suspend fun pdfPageCount(context: Context, source: Uri): Int = withContext(Dispatchers.IO) {
    val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(source, "r")
        ?: throw IOException("Couldn't read the document")
    descriptor.use { fd -> PdfRenderer(fd).use { it.pageCount } }
}
