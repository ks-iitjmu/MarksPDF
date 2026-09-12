package com.example.lovepdf.core.pdf

import android.graphics.Bitmap
import android.util.LruCache

class PageBitmapCache(maxBytes: Int = defaultMaxBytes()) {

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    operator fun get(index: Int, widthPx: Int): Bitmap? = cache.get(key(index, widthPx))

    fun put(index: Int, widthPx: Int, bitmap: Bitmap) {
        cache.put(key(index, widthPx), bitmap)
    }

    fun trim() = cache.trimToSize(cache.maxSize() / 3)

    fun clear() = cache.evictAll()

    private fun key(index: Int, widthPx: Int) = "$index@$widthPx"

    companion object {
        private fun defaultMaxBytes(): Int {
            val available = Runtime.getRuntime().maxMemory()
            return (available / 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
