/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.timer

import im.vector.lib.core.utils.flow.tickerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CountUpTimer(private val intervalInMs: Long = 1_000) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val elapsedTime: AtomicLong = AtomicLong()
    private val resumed: AtomicBoolean = AtomicBoolean(false)

    init {
        startCounter()
    }

    private fun startCounter() {
        tickerFlow(coroutineScope, intervalInMs / 10)
                .filter { resumed.get() }
                .map { elapsedTime.addAndGet(intervalInMs / 10) }
                .filter { it % intervalInMs == 0L }
                .onEach {
                    tickListener?.onTick(it)
                }.launchIn(coroutineScope)
    }

    var tickListener: TickListener? = null

    fun elapsedTime(): Long {
        return elapsedTime.get()
    }

    fun pause() {
        resumed.set(false)
    }

    fun resume() {
        resumed.set(true)
    }

    fun stop() {
        coroutineScope.cancel()
    }

    interface TickListener {
        fun onTick(milliseconds: Long)
    }
}
