/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    // To ensure that the regular tick value is an exact multiple of `intervalInMs`
    private val specialRound = SpecialRound(intervalInMs)

    private fun startCounter() {
        counterJob?.cancel()
        counterJob = coroutineScope.launch {
            while (true) {
                delay(intervalInMs - elapsedTime() % intervalInMs)
                tickListener?.onTick(specialRound.round(elapsedTime()))
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

    /**
     * Start a new timer with the initial given time, if any.
     * If the timer is already started, it will be restarted.
     */
    fun start(initialTime: Long = 0L) {
        elapsedTime.set(initialTime)
        lastTime.set(clock.epochMillis())
        startCounter()
    }

    /**
     * Pause the timer at the current time.
     */
    fun pause() {
        pauseAndTick()
    }

    /**
     * Resume the timer from the current time.
     * Does nothing if the timer is already running.
     */
    fun resume() {
        if (counterJob?.isActive != true) {
            lastTime.set(clock.epochMillis())
            startCounter()
        }
    }

    /**
     * Stop and reset the timer.
     */
    fun stop() {
        pauseAndTick()
        elapsedTime.set(0L)
    }

    private fun pauseAndTick() {
        if (counterJob?.isActive == true) {
            // get the elapsed time before cancelling the timer
            val elapsedTime = elapsedTime()
            // cancel the timer before ticking
            counterJob?.cancel()
            counterJob = null
            // tick with the computed elapsed time
            tickListener?.onTick(elapsedTime)
        }
    }

    fun interface TickListener {
        fun onTick(milliseconds: Long)
    }
}
