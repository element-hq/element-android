package im.vector.matrix.android.internal.util

import arrow.core.Try
import kotlinx.coroutines.delay

suspend fun <T> retry(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100, // 0.1 second
        maxDelay: Long = 10_000,    // 10 second
        factor: Double = 2.0,
        block: suspend () -> Try<T>): Try<T> {

    var currentDelay = initialDelay
    repeat(times - 1) {
        val blockResult = block()
        if (blockResult.isSuccess()) {
            return blockResult
        } else {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block()
}