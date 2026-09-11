package com.example.lovepdf.core.pdf

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Memory cache for rendered pages, keyed by page index *and* render width so a page
 * rendered at preview quality and at zoomed quality can coexist.
 *
 * Bitmaps are deliberately never recycled: Compose may still be drawing one when the
 * cache evicts it, and recycling a bitmap mid-draw crashes. Dropping the reference and
 * letting GC reclaim it is both safe and fast enough.
 */
class PageBitmapCache(maxBytes: Int = defaultMaxBytes()) {

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    operator fun get(index: Int, widthPx: Int): Bitmap? = cache.get(key(index, widthPx))

    fun put(index: Int, widthPx: Int, bitmap: Bitmap) {
        cache.put(key(index, widthPx), bitmap)
    }

    /** Called when the system is under memory pressure. */
    fun trim() = cache.trimToSize(cache.maxSize() / 3)

    fun clear() = cache.evictAll()

    private fun key(index: Int, widthPx: Int) = "$index@$widthPx"

    companion object {
        private fun defaultMaxBytes(): Int {
            val available = Runtime.getRuntime().maxMemory()
            // A quarter of the heap: generous enough to hold a screenful of sharp pages
            // plus neighbours, conservative enough to leave room for zoomed renders.
            return (available / 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
