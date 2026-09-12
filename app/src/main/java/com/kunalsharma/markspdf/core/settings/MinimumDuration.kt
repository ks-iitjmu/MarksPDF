package com.kunalsharma.markspdf.core.settings

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

suspend fun <T> withMinimumDuration(
    millis: Long = DEFAULT_MINIMUM_MILLIS,
    block: suspend () -> T,
): T {
    val start = SystemClock.elapsedRealtime()
    val result = block()
    val elapsed = SystemClock.elapsedRealtime() - start
    if (elapsed < millis) delay((millis - elapsed).milliseconds)
    return result
}

const val DEFAULT_MINIMUM_MILLIS = 1500L
