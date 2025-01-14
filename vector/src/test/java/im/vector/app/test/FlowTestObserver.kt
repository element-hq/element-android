/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    fun assertLatestValue(predicate: (T) -> Boolean): FlowTestObserver<T> {
        assertTrue(predicate(values.last()))
        return this
    }

    fun assertLatestValue(value: T): FlowTestObserver<T> {
        assertEquals(value, values.last())
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
