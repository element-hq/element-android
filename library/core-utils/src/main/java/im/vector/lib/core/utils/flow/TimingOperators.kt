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

package im.vector.lib.core.utils.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

@ExperimentalCoroutinesApi
fun <T> Flow<T>.chunk(durationInMillis: Long): Flow<List<T>> {
    require(durationInMillis > 0) { "Duration should be greater than 0" }
    return flow {
        coroutineScope {
            val events = ArrayList<T>()
            val ticker = fixedPeriodTicker(durationInMillis)
            try {
                val upstreamValues = produce(capacity = Channel.CONFLATED) {
                    collect { value -> send(value) }
                }
                while (isActive) {
                    var hasTimedOut = false
                    select<Unit> {
                        upstreamValues.onReceive {
                            events.add(it)
                        }
                        ticker.onReceive {
                            hasTimedOut = true
                        }
                    }
                    if (hasTimedOut && events.isNotEmpty()) {
                        emit(events.toList())
                        events.clear()
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // drain remaining events
                if (events.isNotEmpty()) emit(events.toList())
            } finally {
                ticker.cancel()
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var windowStartTime = System.currentTimeMillis()
    var emitted = false
    collect { value ->
        val currentTime = System.currentTimeMillis()
        val delta = currentTime - windowStartTime
        if (delta >= windowDuration) {
            windowStartTime += delta / windowDuration * windowDuration
            emitted = false
        }
        if (!emitted) {
            emit(value)
            emitted = true
        }
    }
}

@ExperimentalCoroutinesApi
fun tickerFlow(scope: CoroutineScope, delayMillis: Long, initialDelayMillis: Long = delayMillis): Flow<Unit> {
    return scope.fixedPeriodTicker(delayMillis, initialDelayMillis).consumeAsFlow()
}

@ExperimentalCoroutinesApi
private fun CoroutineScope.fixedPeriodTicker(delayMillis: Long, initialDelayMillis: Long = delayMillis): ReceiveChannel<Unit> {
    require(delayMillis >= 0) { "Expected non-negative delay, but has $delayMillis ms" }
    require(initialDelayMillis >= 0) { "Expected non-negative initial delay, but has $initialDelayMillis ms" }
    return produce(capacity = 0) {
        delay(initialDelayMillis)
        while (true) {
            channel.send(Unit)
            delay(delayMillis)
        }
    }
}
