/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.session.room.timeline

import androidx.test.filters.LargeTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.checkSendOrder
import timber.log.Timber
import java.util.concurrent.CountDownLatch

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class TimelinePreviousLastForwardTest : InstrumentedTest {

    /**
     * This test ensure that if we have a chunk in the timeline which is due to a sync, and we click to permalink, we will be able to go back to the live
     */

    @Test
    fun previousLastForwardTest() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val aliceRoomId = cryptoTestData.roomId

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)
        bobSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!
        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!

        val bobTimeline = roomFromBobPOV.createTimeline(null, TimelineSettings(30))
        bobTimeline.start()

        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Bob timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root}")
                }

                // Ok, we have the 8 first messages of the initial sync (room creation and bob invite and join events)
                snapshot.size == 8
            }

            bobTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
            bobTimeline.removeAllListeners()

            bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()
            bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
        }

        // Bob stop to sync
        bobSession.stopSync()

        val firstMessage = "First messages from Alice"
        // Alice sends 30 messages
        val firstMessageFromAliceId = commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                firstMessage,
                30)
                .last()
                .eventId

        // Bob start to sync
        bobSession.startSync(true)

        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Bob timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root}")
                }

                // Ok, we have the 10 last messages from Alice. This will be our future previous lastForward chunk
                snapshot.size == 10 &&
                        snapshot.all { it.root.content.toModel<MessageContent>()?.body?.startsWith(firstMessage).orFalse() }
            }

            bobTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
            bobTimeline.removeAllListeners()

            bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
            bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
        }

        // Bob stop to sync
        bobSession.stopSync()

        val secondMessage = "Second messages from Alice"
        // Alice sends again 30 messages
        commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                secondMessage,
                30)

        // Bob start to sync
        bobSession.startSync(true)

        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Bob timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root}")
                }

                // Ok, we have the 10 last messages from Alice. This will be our future previous lastForward chunk
                snapshot.size == 10 &&
                        snapshot.all { it.root.content.toModel<MessageContent>()?.body?.startsWith(secondMessage).orFalse() }
            }

            bobTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
            bobTimeline.removeAllListeners()

            bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
            bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
        }

        // Bob navigate to the first message sent from Alice
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Bob timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root}")
                }

                // The event is not in db, so it is fetch
                snapshot.size == 1
            }

            bobTimeline.addListener(eventsListener)

            // Restart the timeline to the first sent event
            bobTimeline.restartWithEventId(firstMessageFromAliceId)

            commonTestHelper.await(lock)
            bobTimeline.removeAllListeners()

            bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeTrue()
            bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
        }

        // Paginate in both direction
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Bob timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root}")
                }

                snapshot.size == 44 // 8 + 1 + 35
            }

            bobTimeline.addListener(eventsListener)

            // Paginate in both direction
            bobTimeline.paginate(Timeline.Direction.BACKWARDS, 50)
            // Ensure the chunk in the middle is included in the next pagination
            bobTimeline.paginate(Timeline.Direction.FORWARDS, 35)

            commonTestHelper.await(lock)
            bobTimeline.removeAllListeners()

            bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeTrue()
            bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()
        }

        // Bob scroll to the future, till the live
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Bob timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root}")
                }

                // Bob can see the first event of the room (so Back pagination has worked)
                snapshot.lastOrNull()?.root?.getClearType() == EventType.STATE_ROOM_CREATE &&
                        // 8 for room creation item 60 message from Alice
                        snapshot.size == 68 && // 8 + 60
                        snapshot.checkSendOrder(secondMessage, 30, 0) &&
                        snapshot.checkSendOrder(firstMessage, 30, 30)
            }

            bobTimeline.addListener(eventsListener)

            bobTimeline.paginate(Timeline.Direction.FORWARDS, 50)

            commonTestHelper.await(lock)
            bobTimeline.removeAllListeners()

            bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
            bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()
        }

        bobTimeline.dispose()

        cryptoTestData.cleanUp(commonTestHelper)
    }
}
