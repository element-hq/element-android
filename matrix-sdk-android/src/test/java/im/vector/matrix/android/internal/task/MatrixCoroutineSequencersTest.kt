/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.task

import kotlinx.coroutines.*
import org.junit.Test
import java.util.concurrent.Executors

class MatrixCoroutineSequencersTest {

    @Test
    fun sequencer_should_run_sequential() {
        val sequencer = MatrixCoroutineSequencers()
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        val jobs = listOf(
                GlobalScope.launch(dispatcher) {
                    sequencer.post("Sequencer1") { suspendingMethod("#3") }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post("Sequencer1") { suspendingMethod("#4") }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post("Sequencer2") { suspendingMethod("#5") }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post("Sequencer2") { suspendingMethod("#6") }
                },
                GlobalScope.launch(dispatcher) {
                    sequencer.post("Sequencer2") { suspendingMethod("#7") }
                }
        )
        Thread.sleep(5500)
        sequencer.cancelAll()
        runBlocking {
            jobs.joinAll()
        }
    }

    private suspend fun suspendingMethod(name: String): String = withContext(Dispatchers.Default) {
        println("BLOCKING METHOD $name STARTS")
        delay(3000)
        println("BLOCKING METHOD $name ENDS")
        name
    }

}
