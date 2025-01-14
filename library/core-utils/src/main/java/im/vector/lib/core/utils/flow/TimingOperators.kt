/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.flow

import android.os.SystemClock
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
    var windowStartTime = SystemClock.elapsedRealtime()
    var emitted = false
    collect { value ->
        val currentTime = SystemClock.elapsedRealtime()
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
