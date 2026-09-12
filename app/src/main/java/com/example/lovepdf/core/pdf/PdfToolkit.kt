package com.example.lovepdf.core.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object PdfToolkit {

    suspend fun merge(
        context: Context,
        sources: List<Uri>,
        destination: Uri,
    ): Unit = withContext(Dispatchers.IO) {
        require(sources.size >= 2) { "Merging needs at least two files" }

        val resolver = context.contentResolver
        val opened = mutableListOf<java.io.InputStream>()

        try {
            val merger = PDFMergerUtility()

            val output = resolver.openOutputStream(destination)
                ?: throw IOException("Couldn't write to the chosen location")

            output.use { out ->
                merger.destinationStream = out

                sources.forEach { uri ->
                    val input = resolver.openInputStream(uri)
                        ?: throw IOException("Couldn't read one of the selected files")
                    opened += input
                    merger.addSource(input)
                }

                merger.mergeDocuments(
                    MemoryUsageSetting
                        .setupMixed(MAX_MEMORY_BYTES)
                        .setTempDir(context.cacheDir)
                )
            }
        } finally {
            opened.forEach { runCatching { it.close() } }
        }
    }

    suspend fun extractRange(
        context: Context,
        source: Uri,
        fromPage: Int,
        toPage: Int,
        destination: Uri,
    ): Unit = withContext(Dispatchers.IO) {
        openDocument(context, source).use { document ->
            require(fromPage in 0 until document.numberOfPages) { "Start page out of range" }
            require(toPage in fromPage until document.numberOfPages) { "End page out of range" }

            PDDocument().use { output ->
                for (index in fromPage..toPage) {
                    output.importPage(document.getPage(index))
                }
                write(context, output, destination)
            }
        }
    }

    suspend fun splitIntoChunks(
        context: Context,
        source: Uri,
        pagesPerFile: Int,
        destinationFor: suspend (partIndex: Int, partCount: Int) -> Uri,
    ): List<Uri> = withContext(Dispatchers.IO) {
        require(pagesPerFile >= 1) { "Each file needs at least one page" }

        openDocument(context, source).use { document ->
            val splitter = Splitter().apply { setSplitAtPage(pagesPerFile) }
            val parts = splitter.split(document)
            val written = mutableListOf<Uri>()

            try {
                parts.forEachIndexed { index, part ->
                    val target = destinationFor(index, parts.size)
                    write(context, part, target)
                    written += target
                }
            } finally {
                parts.forEach { runCatching { it.close() } }
            }
            written
        }
    }

    suspend fun imagesToPdf(
        context: Context,
        sources: List<Uri>,
        destination: Uri,
        fit: PdfPageFit,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Unit = withContext(Dispatchers.IO) {
        require(sources.isNotEmpty()) { "Pick at least one image" }

        PDDocument().use { document ->
            sources.forEachIndexed { index, uri ->
                val bitmap = ImageDecoder.decode(context, uri)?.let { ImageDecoder.flatten(it) }
                if (bitmap != null) {
                    val image = JPEGFactory.createFromImage(document, bitmap, JPEG_QUALITY)

                    val box = when (fit) {
                        PdfPageFit.MatchImage -> {
                            val aspect = bitmap.width.toFloat() / bitmap.height
                            PDRectangle(STANDARD_WIDTH, STANDARD_WIDTH / aspect)
                        }

                        PdfPageFit.A4 -> PDRectangle.A4
                    }

                    val page = PDPage(box)
                    document.addPage(page)

                    val margin = if (fit == PdfPageFit.A4) A4_MARGIN else 0f
                    val scale = minOf(
                        (box.width - margin * 2) / bitmap.width,
                        (box.height - margin * 2) / bitmap.height,
                    )
                    val drawWidth = bitmap.width * scale
                    val drawHeight = bitmap.height * scale

                    PDPageContentStream(document, page).use { stream ->
                        stream.drawImage(
                            image,
                            (box.width - drawWidth) / 2f,
                            (box.height - drawHeight) / 2f,
                            drawWidth,
                            drawHeight,
                        )
                    }

                    bitmap.recycle()
                }

                onProgress(index + 1, sources.size)
            }

            if (document.numberOfPages == 0) {
                throw IOException("None of the selected images could be read")
            }

            write(context, document, destination)
        }
    }

    suspend fun pageCount(context: Context, source: Uri): Int = withContext(Dispatchers.IO) {
        openDocument(context, source).use { it.numberOfPages }
    }

    private fun openDocument(context: Context, source: Uri): PDDocument {
        val input = context.contentResolver.openInputStream(source)
            ?: throw IOException("Couldn't read the file")
        return input.use {
            PDDocument.load(
                it,
                MemoryUsageSetting.setupMixed(MAX_MEMORY_BYTES).setTempDir(context.cacheDir)
            )
        }
    }

    private fun write(context: Context, document: PDDocument, destination: Uri) {
        val output = context.contentResolver.openOutputStream(destination)
            ?: throw IOException("Couldn't write to the chosen location")
        output.use { document.save(it) }
    }

    private const val MAX_MEMORY_BYTES = 50L * 1024 * 1024

    private const val STANDARD_WIDTH = 595.28f
    private const val A4_MARGIN = 28f
    private const val JPEG_QUALITY = 0.85f
}
