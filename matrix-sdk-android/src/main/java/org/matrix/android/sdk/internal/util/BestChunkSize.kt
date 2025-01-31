/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
