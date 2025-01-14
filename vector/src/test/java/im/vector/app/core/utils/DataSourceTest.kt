/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import im.vector.app.test.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldContainSame
import org.junit.Test

class DataSourceTest {

    @Test
    fun `given PublishDataSource, when posting values before observing, then no value is observed`() = runTest {
        val publishDataSource = PublishDataSource<Int>()
        publishDataSource.post(0)
        publishDataSource.post(1)

        publishDataSource.stream()
                .test(this)
                .assertNoValues()
                .finish()
    }

    @Test
    fun `given PublishDataSource with a large enough buffer size, when posting values after observing, then all values are observed`() = runTest {
        val valuesToPost = listOf(2, 3, 4, 5, 6, 7, 8, 9)
        val publishDataSource = PublishDataSource<Int>(bufferSize = valuesToPost.size)
        publishDataSource.test(testScheduler, valuesToPost, valuesToPost)
    }

    @Test
    fun `given PublishDataSource with a too small buffer size, when posting values after observing, then we are missing some values`() = runTest {
        val valuesToPost = listOf(2, 3, 4, 5, 6, 7, 8, 9)
        val expectedValues = listOf(2, 9)
        val publishDataSource = PublishDataSource<Int>(bufferSize = 1)
        publishDataSource.test(testScheduler, valuesToPost, expectedValues)
    }

    private suspend fun PublishDataSource<Int>.test(testScheduler: TestCoroutineScheduler, valuesToPost: List<Int>, expectedValues: List<Int>) {
        val values = ArrayList<Int>()
        val job = stream()
                .onEach {
                    // Artificial delay to make consumption longer than production
                    delay(10)
                    values.add(it)
                }
                .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        valuesToPost.forEach {
            post(it)
        }
        withContext(Dispatchers.Default) {
            delay(11L * valuesToPost.size)
        }
        job.cancel()

        values shouldContainSame expectedValues
    }
}
