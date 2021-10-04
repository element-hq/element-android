/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.util

import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.logger.LoggerTag
import timber.log.Timber

internal fun <T> Collection<T>.logLimit(maxQuantity: Int = 5): String {
    return buildString {
        append(size)
        append(" item(s)")
        if (size > maxQuantity) {
            append(", first $maxQuantity items")
        }
        append(": ")
        append(this@logLimit.take(maxQuantity))
    }
}

internal suspend fun <T> logDuration(message: String,
                                     loggerTag: LoggerTag,
                                     block: suspend () -> T): T {
    Timber.tag(loggerTag.value).d("$message -- BEGIN")
    val start = System.currentTimeMillis()
    val result = logRamUsage(message) {
        block()
    }
    val duration = System.currentTimeMillis() - start
    Timber.tag(loggerTag.value).d("$message -- END duration: $duration ms")

    return result
}

internal suspend fun <T> logRamUsage(message: String, block: suspend () -> T): T {
    return if (BuildConfig.DEBUG) {
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val freeMemoryInMb = runtime.freeMemory() / 1048576L
        val usedMemInMBStart = runtime.totalMemory() / 1048576L - freeMemoryInMb
        Timber.d("$message -- BEGIN (free memory: $freeMemoryInMb MB)")
        val result = block()
        runtime.gc()
        val usedMemInMBEnd = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val usedMemInMBDiff = usedMemInMBEnd - usedMemInMBStart
        Timber.d("$message -- END RAM usage: $usedMemInMBDiff MB")
        result
    } else {
        block()
    }
}
