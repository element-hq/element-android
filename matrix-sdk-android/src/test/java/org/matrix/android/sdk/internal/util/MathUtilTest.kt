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

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.MatrixTest

@FixMethodOrder(MethodSorters.JVM)
class MathUtilTest : MatrixTest {

    @Test
    fun testGetBestChunkSize0() = doTest(0, 100, 1, 0)

    @Test
    fun testGetBestChunkSize1() = doTest(1, 100, 1, 1)

    @Test
    fun testGetBestChunkSize5() = doTest(5, 100, 1, 5)

    @Test
    fun testGetBestChunkSize99() = doTest(99, 100, 1, 99)

    @Test
    fun testGetBestChunkSize100() = doTest(100, 100, 1, 100)

    @Test
    fun testGetBestChunkSize101() = doTest(101, 100, 2, 51)

    @Test
    fun testGetBestChunkSize199() = doTest(199, 100, 2, 100)

    @Test
    fun testGetBestChunkSize200() = doTest(200, 100, 2, 100)

    @Test
    fun testGetBestChunkSize201() = doTest(201, 100, 3, 67)

    @Test
    fun testGetBestChunkSize240() = doTest(240, 100, 3, 80)

    private fun doTest(listSize: Int, limit: Int, expectedNumberOfChunks: Int, expectedChunkSize: Int) {
        val result = getBetsChunkSize(listSize, limit)

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
