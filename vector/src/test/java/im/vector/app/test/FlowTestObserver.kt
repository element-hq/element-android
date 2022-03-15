/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

fun <T> Flow<T>.test(scope: CoroutineScope): FlowTestObserver<T> {
    return FlowTestObserver(scope, this)
}

class FlowTestObserver<T>(
        scope: CoroutineScope,
        flow: Flow<T>
) {
    private val values = mutableListOf<T>()
    private val job: Job = flow
            .onEach {
                values.add(it)
            }.launchIn(scope)

    fun assertNoValues() = assertValues(emptyList())

    fun assertValues(vararg values: T) = assertValues(values.toList())

    fun assertValue(position: Int, predicate: (T) -> Boolean): FlowTestObserver<T> {
        assertTrue(predicate(values[position]))
        return this
    }

    fun assertValues(values: List<T>): FlowTestObserver<T> {
        assertEquals(values, this.values)
        return this
    }

    fun finish() {
        job.cancel()
    }
}
