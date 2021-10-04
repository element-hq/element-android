/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

import kotlin.math.ceil

internal data class BestChunkSize(
        val numberOfChunks: Int,
        val chunkSize: Int
) {
    fun shouldChunk() = numberOfChunks > 1
}

internal fun computeBestChunkSize(listSize: Int, limit: Int): BestChunkSize {
    return if (listSize <= limit) {
        BestChunkSize(
                numberOfChunks = 1,
                chunkSize = listSize
        )
    } else {
        val numberOfChunks = ceil(listSize / limit.toDouble()).toInt()
        // Round on next Int
        val chunkSize = ceil(listSize / numberOfChunks.toDouble()).toInt()

        BestChunkSize(
                numberOfChunks = numberOfChunks,
                chunkSize = chunkSize
        )
    }
}
