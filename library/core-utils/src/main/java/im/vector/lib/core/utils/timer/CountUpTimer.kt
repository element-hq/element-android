/*
 * Copyright 2020 New Vector Ltd
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
class CountUpTimer(initialTime: Long = 0L, private val intervalInMs: Long = 1_000) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val resumed: AtomicBoolean = AtomicBoolean(false)

    private val clock: Clock = DefaultClock()
    private val lastTime: AtomicLong = AtomicLong()
    private val elapsedTime: AtomicLong = AtomicLong(initialTime)

    init {
        startCounter()
    }

    private fun startCounter() {
        tickerFlow(coroutineScope, intervalInMs)
                .filter { resumed.get() }
                .map { elapsedTime() }
                .onEach { tickListener?.onTick(it) }
                .launchIn(coroutineScope)
    }

    var tickListener: TickListener? = null

    fun elapsedTime(): Long {
        return if (resumed.get()) {
            val now = clock.epochMillis()
            elapsedTime.addAndGet(now - lastTime.getAndSet(now))
        } else {
            elapsedTime.get()
        }
    }

    fun pause() {
        tickListener?.onTick(elapsedTime())
        resumed.set(false)
    }

    fun resume() {
        lastTime.set(clock.epochMillis())
        resumed.set(true)
    }

    fun stop() {
        tickListener?.onTick(elapsedTime())
        coroutineScope.cancel()
    }

    fun interface TickListener {
        fun onTick(milliseconds: Long)
    }
}
