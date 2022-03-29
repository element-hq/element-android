/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.task

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.matrix.android.sdk.MatrixTest
import java.util.concurrent.Executors

class CoroutineSequencersTest : MatrixTest {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Test
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun sequencer_should_run_sequential() {
        val sequencer = SemaphoreCoroutineSequencer()
        val results = ArrayList<String>()

        val jobs = listOf(
                GlobalScope.launch(dispatcher) {
                    sequencer.post { suspendingMethod("#1") }
                            .also { results.add(it) }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post { suspendingMethod("#2") }
                            .also { results.add(it) }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post { suspendingMethod("#3") }
                            .also { results.add(it) }
                }
        )
        runTest {
            jobs.joinAll()
        }
        assertEquals(3, results.size)
        assertEquals(results[0], "#1")
        assertEquals(results[1], "#2")
        assertEquals(results[2], "#3")
    }

    @Test
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun sequencer_should_run_parallel() {
        val sequencer1 = SemaphoreCoroutineSequencer()
        val sequencer2 = SemaphoreCoroutineSequencer()
        val sequencer3 = SemaphoreCoroutineSequencer()
        val results = ArrayList<String>()
        val jobs = listOf(
                GlobalScope.launch(dispatcher) {
                    sequencer1.post { suspendingMethod("#1") }
                            .also { results.add(it) }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer2.post { suspendingMethod("#2") }
                            .also { results.add(it) }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer3.post { suspendingMethod("#3") }
                            .also { results.add(it) }
                }
        )
        runTest {
            jobs.joinAll()
        }
        assertEquals(3, results.size)
    }

    @Test
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun sequencer_should_jump_to_next_when_current_job_canceled() {
        val sequencer = SemaphoreCoroutineSequencer()
        val results = ArrayList<String>()
        val jobs = listOf(
                GlobalScope.launch(dispatcher) {
                    sequencer.post { suspendingMethod("#1") }
                            .also { results.add(it) }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post { suspendingMethod("#2") }
                            .also { results.add(it) }
                            .also { println("Result: $it") }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post { suspendingMethod("#3") }
                            .also { results.add(it) }
                }
        )
        // We are canceling the second job
        jobs[1].cancel()
        runTest {
            jobs.joinAll()
        }
        assertEquals(2, results.size)
    }

    private suspend fun suspendingMethod(name: String): String {
        println("BLOCKING METHOD $name STARTS on ${Thread.currentThread().name}")
        delay(1000)
        println("BLOCKING METHOD $name ENDS on ${Thread.currentThread().name}")
        return name
    }
}
