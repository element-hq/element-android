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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class CountUpTimer(
        private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        private val clock: Clock = DefaultClock(),
        private val intervalInMs: Long = 1_000,
) {

    private var counterJob: Job? = null

    private val lastTime: AtomicLong = AtomicLong(clock.epochMillis())
    private val elapsedTime: AtomicLong = AtomicLong(0)

    private fun startCounter() {
        counterJob = coroutineScope.launch {
            while (true) {
                delay(intervalInMs - elapsedTime() % intervalInMs)
                tickListener?.onTick(elapsedTime())
            }
        }
    }

    var tickListener: TickListener? = null

    fun elapsedTime(): Long {
        return if (counterJob?.isActive == true) {
            val now = clock.epochMillis()
            elapsedTime.addAndGet(now - lastTime.getAndSet(now))
        } else {
            elapsedTime.get()
        }
    }

    fun start(initialTime: Long = 0L) {
        elapsedTime.set(initialTime)
        resume()
    }

    fun pause() {
        tickListener?.onTick(elapsedTime())
        counterJob?.cancel()
        counterJob = null
    }

    fun resume() {
        lastTime.set(clock.epochMillis())
        startCounter()
    }

    fun stop() {
        tickListener?.onTick(elapsedTime())
        counterJob?.cancel()
        counterJob = null
        elapsedTime.set(0L)
    }

    fun interface TickListener {
        fun onTick(milliseconds: Long)
    }
}
