/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.MatrixTest

@FixMethodOrder(MethodSorters.JVM)
class MathUtilTest : MatrixTest {

    @Test
    fun testComputeBestChunkSize0() = doTest(0, 100, 1, 0)

    @Test
    fun testComputeBestChunkSize1to99() {
        for (i in 1..99) {
            doTest(i, 100, 1, i)
        }
    }

    @Test
    fun testComputeBestChunkSize100() = doTest(100, 100, 1, 100)

    @Test
    fun testComputeBestChunkSize101() = doTest(101, 100, 2, 51)

    @Test
    fun testComputeBestChunkSize199() = doTest(199, 100, 2, 100)

    @Test
    fun testComputeBestChunkSize200() = doTest(200, 100, 2, 100)

    @Test
    fun testComputeBestChunkSize201() = doTest(201, 100, 3, 67)

    @Test
    fun testComputeBestChunkSize240() = doTest(240, 100, 3, 80)

    private fun doTest(listSize: Int, limit: Int, expectedNumberOfChunks: Int, expectedChunkSize: Int) {
        val result = computeBestChunkSize(listSize, limit)

        result.numberOfChunks shouldBeEqualTo expectedNumberOfChunks
        result.chunkSize shouldBeEqualTo expectedChunkSize

        // Test that the result make sense, when we use chunked()
        if (result.chunkSize > 0) {
            generateSequence { "a" }
                    .take(listSize)
                    .chunked(result.chunkSize)
                    .shouldHaveSize(result.numberOfChunks)
        }
    }
}
