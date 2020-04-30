/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.session.room.timeline

import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.CryptoTestHelper
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import timber.log.Timber
import java.util.concurrent.CountDownLatch

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class TimelineForwardPaginationTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(commonTestHelper)

    /**
     * This test ensure that if we click to permalink, we will be able to go back to the live
     */
    @Test
    fun forwardPaginationTest() {
        val numberOfMessagesToSend = 90
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        // Alice sends X messages
        val sentMessages = commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                "Message from Alice, long enough to observe the problem, if it is not long enough, there is not always the problem",
                numberOfMessagesToSend)

        // Alice clear the cache
        commonTestHelper.doSync<Unit> {
            aliceSession.clearCache(it)
        }

        aliceSession.startSync(true)

        val aliceTimeline = roomFromAlicePOV.createTimeline(null, TimelineSettings(30))
        aliceTimeline.start()

        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Alice timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root.content}")
                }

                // Ok, we have the 10 first messages of the initial sync
                snapshot.size == 10
            }

            // Open the timeline at last sent message
            aliceTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
            aliceTimeline.removeAllListeners()

            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
        }

        run {
            val lock = CountDownLatch(1)
            val aliceEventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Alice timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root.content}")
                }

                // The event is not in db, so it is fetch alone
                snapshot.size == 1
            }

            aliceTimeline.addListener(aliceEventsListener)

            // Restart the timeline to the first sent event
            aliceTimeline.restartWithEventId(sentMessages.last().eventId)

            commonTestHelper.await(lock)
            aliceTimeline.removeAllListeners()

            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeTrue()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
        }

        run {
            val lock = CountDownLatch(1)
            val aliceEventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Alice timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root.content}")
                }

                // Alice can see the first event of the room (so Back pagination has worked)
                snapshot.lastOrNull()?.root?.getClearType() == EventType.STATE_ROOM_CREATE
                        // 6 for room creation item (backward pagination), 1 for the context, and 50 for the forward pagination
                        && snapshot.size == 6 + 1 + 50
            }

            aliceTimeline.addListener(aliceEventsListener)

            // Restart the timeline to the first sent event
            // We ask to load event backward and forward
            aliceTimeline.paginate(Timeline.Direction.BACKWARDS, 50)
            aliceTimeline.paginate(Timeline.Direction.FORWARDS, 50)

            commonTestHelper.await(lock)
            aliceTimeline.removeAllListeners()

            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeTrue()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()
        }

        run {
            val lock = CountDownLatch(1)
            val aliceEventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Alice timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root.content}")
                }

                // 6 for room creation item (backward pagination),and numberOfMessagesToSend (all the message of the room)
                snapshot.size == 6 + numberOfMessagesToSend
            }

            aliceTimeline.addListener(aliceEventsListener)

            // Ask for a forward pagination
            aliceTimeline.paginate(Timeline.Direction.FORWARDS, 50)

            commonTestHelper.await(lock)
            aliceTimeline.removeAllListeners()

            // The timeline is fully loaded
            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()
        }

        aliceTimeline.dispose()

        cryptoTestData.cleanUp(commonTestHelper)
    }
}
