package com.example.lovepdf.core.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Write operations on PDFs.
 *
 * Separate from [PdfDocumentSource] on purpose. The viewer runs on the platform's
 * PdfRenderer, which is hardware-accelerated and free but strictly read-only. Editing
 * needs PDFBox, which is pure Java, far heavier, and has no business being in the
 * rendering path. Keeping them apart means the reader stays fast no matter how much
 * editing machinery accumulates here.
 */
object PdfToolkit {

    /**
     * Concatenates [sources] in order into [destination].
     *
     * Memory is capped at 50 MB before spilling to disk. PDFBox defaults to holding the
     * whole merge in RAM, which is fine for a few small files and an instant
     * OutOfMemoryError on a phone when someone merges a stack of scanned documents.
     * The temp directory has to be set explicitly too — PDFBox otherwise falls back to
     * `java.io.tmpdir`, which on Android points at a path that doesn't exist.
     */
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

    /**
     * Copies pages [fromPage]..[toPage] (zero-based, inclusive) into [destination].
     *
     * Uses `importPage` rather than `addPage`. `addPage` inserts a reference to a page
     * still owned by the source document, so the output only stays valid while the
     * source is open — it saves fine and then fails to open later. `importPage` copies
     * the page and the resources it depends on.
     */
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

    /**
     * Splits [source] into chunks of [pagesPerFile] pages.
     *
     * [destinationFor] is called once per output part, so storage decisions stay out of
     * here — this only needs somewhere to write.
     */
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

    /**
     * Extracts the text of every page, in order.
     *
     * Done in one pass over an open document rather than reopening per page: PDFBox
     * parses the whole file on load, so opening it 770 times to read 770 pages would
     * repeat that parse every time.
     *
     * Pages that fail to strip yield an empty string instead of aborting. A single
     * malformed page is common in real PDFs and is no reason to lose search across the
     * rest of the document.
     */
    suspend fun extractPageTexts(
        context: Context,
        source: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<String> = withContext(Dispatchers.IO) {
        openDocument(context, source).use { document ->
            val stripper = PDFTextStripper()
            val total = document.numberOfPages

            (0 until total).map { index ->
                val text = runCatching {
                    stripper.startPage = index + 1
                    stripper.endPage = index + 1
                    stripper.getText(document)
                }.getOrDefault("")

                onProgress(index + 1, total)
                text
            }
        }
    }

    /** Page count, needed by the split UI before anything is chosen. */
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
}
