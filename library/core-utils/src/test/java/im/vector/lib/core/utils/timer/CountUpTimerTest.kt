/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.lib.core.utils.timer

import im.vector.lib.core.utils.test.fakes.FakeClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val AN_INTERVAL = 500L
private const val AN_INITIAL_TIME = 2_333L

@OptIn(ExperimentalCoroutinesApi::class)
internal class CountUpTimerTest {

    private val fakeClock = FakeClock()

    @Test
    fun `when pausing and resuming the timer, the timer ticks the right values at the right moments`() = runTest {
        every { fakeClock.epochMillis() } answers { currentTime }
        val tickListener = mockk<CountUpTimer.TickListener>(relaxed = true)
        val timer = CountUpTimer(
                coroutineScope = this,
                clock = fakeClock,
                intervalInMs = AN_INTERVAL,
        ).also { it.tickListener = tickListener }

        timer.start()
        advanceTimeBy(AN_INTERVAL / 2) // no tick
        timer.pause() // tick
        advanceTimeBy(AN_INTERVAL * 10) // no tick
        timer.resume() // no tick
        advanceTimeBy(AN_INTERVAL * 4) // tick * 4
        timer.stop() // tick

        verifySequence {
            tickListener.onTick(AN_INTERVAL / 2)
            tickListener.onTick(AN_INTERVAL)
            tickListener.onTick(AN_INTERVAL * 2)
            tickListener.onTick(AN_INTERVAL * 3)
            tickListener.onTick(AN_INTERVAL * 4)
            tickListener.onTick(AN_INTERVAL * 4 + AN_INTERVAL / 2)
        }
    }

    @Test
    fun `given an initial time, the timer ticks the right values at the right moments`() = runTest {
        every { fakeClock.epochMillis() } answers { currentTime }
        val tickListener = mockk<CountUpTimer.TickListener>(relaxed = true)
        val timer = CountUpTimer(
                coroutineScope = this,
                clock = fakeClock,
                intervalInMs = AN_INTERVAL,
        ).also { it.tickListener = tickListener }

        timer.start(AN_INITIAL_TIME)
        advanceTimeBy(AN_INTERVAL) // tick
        timer.pause() // tick
        advanceTimeBy(AN_INTERVAL * 10) // no tick
        timer.resume() // no tick
        advanceTimeBy(AN_INTERVAL * 4) // tick * 4
        timer.stop() // tick

        val offset = AN_INITIAL_TIME % AN_INTERVAL
        verifySequence {
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL - offset)
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL)
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL * 2 - offset)
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL * 3 - offset)
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL * 4 - offset)
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL * 5 - offset)
            tickListener.onTick(AN_INITIAL_TIME + AN_INTERVAL * 5)
        }
    }
}
