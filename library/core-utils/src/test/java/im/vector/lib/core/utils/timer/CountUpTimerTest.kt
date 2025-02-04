/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.timer

import im.vector.lib.core.utils.test.fakes.FakeClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
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
        // Given
        every { fakeClock.epochMillis() } answers { currentTime }
        val tickListener = mockk<CountUpTimer.TickListener>(relaxed = true)
        val timer = CountUpTimer(
                coroutineScope = this,
                clock = fakeClock,
                intervalInMs = AN_INTERVAL,
        ).also { it.tickListener = tickListener }

        // When
        timer.start()
        advanceTimeBy(AN_INTERVAL / 2) // no tick
        timer.pause() // tick
        advanceTimeBy(AN_INTERVAL * 10) // no tick
        timer.resume() // no tick
        advanceTimeBy(AN_INTERVAL * 4) // tick * 4
        timer.stop() // tick

        // Then
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
        // Given
        every { fakeClock.epochMillis() } answers { currentTime }
        val tickListener = mockk<CountUpTimer.TickListener>(relaxed = true)
        val timer = CountUpTimer(
                coroutineScope = this,
                clock = fakeClock,
                intervalInMs = AN_INTERVAL,
        ).also { it.tickListener = tickListener }

        // When
        timer.start(AN_INITIAL_TIME)
        advanceTimeBy(AN_INTERVAL) // tick
        timer.pause() // tick
        advanceTimeBy(AN_INTERVAL * 10) // no tick
        timer.resume() // no tick
        advanceTimeBy(AN_INTERVAL * 4) // tick * 4
        timer.stop() // tick

        // Then
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

    @Test
    fun `when stopping the timer on tick, the stop action is called twice and the timer ticks twice`() = runTest {
        // Given
        every { fakeClock.epochMillis() } answers { currentTime }
        val timer = spyk(
                CountUpTimer(
                        coroutineScope = this,
                        clock = fakeClock,
                        intervalInMs = AN_INTERVAL,
                )
        )
        val tickListener = mockk<CountUpTimer.TickListener> {
            every { onTick(any()) } answers { timer.stop() }
        }
        timer.tickListener = tickListener

        // When
        timer.start()
        advanceTimeBy(AN_INTERVAL * 10)

        // Then
        verify(exactly = 2) { timer.stop() } // one call at the first tick, a second time because of the tick of the first stop
        verify(exactly = 2) { tickListener.onTick(any()) } // one after reaching the first interval, a second after the stop action
    }

    @Test
    fun `when pausing the timer on tick, the pause action is called twice and the timer ticks twice`() = runTest {
        // Given
        every { fakeClock.epochMillis() } answers { currentTime }
        val timer = spyk(
                CountUpTimer(
                        coroutineScope = this,
                        clock = fakeClock,
                        intervalInMs = AN_INTERVAL,
                )
        )
        val tickListener = mockk<CountUpTimer.TickListener> {
            every { onTick(any()) } answers { timer.pause() }
        }
        timer.tickListener = tickListener

        // When
        timer.start()
        advanceTimeBy(AN_INTERVAL * 10)

        // Then
        verify(exactly = 2) { timer.pause() } // one call at the first tick, a second time because of the tick of the first pause
        verify(exactly = 2) { tickListener.onTick(any()) } // one after reaching the first interval, a second after the pause action
    }
}
