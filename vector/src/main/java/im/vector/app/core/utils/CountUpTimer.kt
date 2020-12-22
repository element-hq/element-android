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

package im.vector.app.core.utils

import android.os.Handler
import android.os.SystemClock

class CountUpTimer(private val intervalInMs: Long) {

    private var startTimestamp: Long = 0
    private var delayTime: Long = 0
    private var lastPauseTimestamp: Long = 0
    private var isRunning: Boolean = false

    var tickListener: TickListener? = null

    private val tickHandler: Handler = Handler()
    private val tickSelector = Runnable {
        if (isRunning) {
            tickListener?.onTick(time)
            startTicking()
        }
    }

    init {
        reset()
    }

    /**
     * Reset the timer, also clears all laps information. Running status will not affected
     */
    fun reset() {
        startTimestamp = SystemClock.elapsedRealtime()
        delayTime = 0
        lastPauseTimestamp = startTimestamp
    }

    /**
     * Pause the timer
     */
    fun pause() {
        if (isRunning) {
            lastPauseTimestamp = SystemClock.elapsedRealtime()
            isRunning = false
            stopTicking()
        }
    }

    /**
     * Resume the timer
     */
    fun resume() {
        if (!isRunning) {
            val currentTime: Long = SystemClock.elapsedRealtime()
            delayTime += currentTime - lastPauseTimestamp
            isRunning = true
            startTicking()
        }
    }
    val time: Long
        get() = if (isRunning) {
                SystemClock.elapsedRealtime() - startTimestamp - delayTime
        } else {
            lastPauseTimestamp - startTimestamp - delayTime
        }

    private fun startTicking() {
        tickHandler.removeCallbacksAndMessages(null)
        val time = time
        val remainingTimeInInterval = intervalInMs - time % intervalInMs
        tickHandler.postDelayed(tickSelector, remainingTimeInInterval)
    }

    private fun stopTicking() {
        tickHandler.removeCallbacksAndMessages(null)
    }


    interface TickListener {
        fun onTick(milliseconds: Long)
    }

}