package com.kunalsharma.markspdf.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

enum class PdfPageFit {
    MatchImage,
    A4,
}
object ImageDecoder {

    suspend fun decode(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = context.contentResolver.openInputStream(uri)
            ?: return@withContext null
        opened.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val stream = context.contentResolver.openInputStream(uri)
            ?: return@withContext null
        val decoded = stream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        val rotation = readRotation(context, uri)
        if (rotation == 0f) decoded else rotate(decoded, rotation)
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= maxDimension || height / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        return sample
    }

    private fun readRotation(context: Context, uri: Uri): Float {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    fun flatten(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap

        val opaque = createBitmap(bitmap.width, bitmap.height)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        opaque.setHasAlpha(false)
        bitmap.recycle()
        return opaque
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
    private const val MAX_DIMENSION = 2400
}
