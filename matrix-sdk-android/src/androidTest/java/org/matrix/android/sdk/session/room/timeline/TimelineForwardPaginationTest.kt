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
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.internal.assertEquals
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
class TimelineForwardPaginationTest : InstrumentedTest {

//    @Rule
//    @JvmField
//    val mRetryTestRule = RetryTestRule()

    /**
     * This test ensure that if we click to permalink, we will be able to go back to the live
     */
    @Test
    fun forwardPaginationTest() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val numberOfMessagesToSend = 90
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        // Alice sends X messages
        val message = "Message from Alice"
        val sentMessages = commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                message,
                numberOfMessagesToSend)

        // Alice clear the cache and restart the sync
        commonTestHelper.clearCacheAndSync(aliceSession)
        val aliceTimeline = roomFromAlicePOV.createTimeline(null, TimelineSettings(30))
        aliceTimeline.start()

        // Alice sees the 10 last message of the room, and can only navigate BACKWARD
        run {
            val lock = CountDownLatch(1)
            val eventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Alice timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root.content}")
                }

                // Ok, we have the 10 last messages of the initial sync
                snapshot.size == 10 &&
                        snapshot.all { it.root.content.toModel<MessageContent>()?.body?.startsWith(message).orFalse() }
            }

            // Open the timeline at last sent message
            aliceTimeline.addListener(eventsListener)
            commonTestHelper.await(lock)
            aliceTimeline.removeAllListeners()

            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
        }

        // Alice navigates to the first message of the room, which is not in its database. A GET /context is performed
        // Then she can paginate BACKWARD and FORWARD
        run {
            val lock = CountDownLatch(1)
            val aliceEventsListener = commonTestHelper.createEventListener(lock) { snapshot ->
                Timber.e("Alice timeline updated: with ${snapshot.size} events:")
                snapshot.forEach {
                    Timber.w(" event ${it.root.content}")
                }

                // The event is not in db, so it is fetch alone
                snapshot.size == 1 &&
                        snapshot.all { it.root.content.toModel<MessageContent>()?.body?.startsWith("Message from Alice").orFalse() }
            }

            aliceTimeline.addListener(aliceEventsListener)

            // Restart the timeline to the first sent event
            aliceTimeline.restartWithEventId(sentMessages.last().eventId)

            commonTestHelper.await(lock)
            aliceTimeline.removeAllListeners()

            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeTrue()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeTrue()
        }

        // Alice paginates BACKWARD and FORWARD of 50 events each
        // Then she can only navigate FORWARD
        run {
            val snapshot = runBlocking {
                aliceTimeline.awaitPaginate(Timeline.Direction.BACKWARDS, 50)
                aliceTimeline.awaitPaginate(Timeline.Direction.FORWARDS, 50)
            }
            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeTrue()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()

            assertEquals(EventType.STATE_ROOM_CREATE, snapshot.lastOrNull()?.root?.getClearType())
            // 6 for room creation item (backward pagination), 1 for the context, and 50 for the forward pagination
            // 6 + 1 + 50
            assertEquals(57, snapshot.size)
        }

        // Alice paginates once again FORWARD for 50 events
        // All the timeline is retrieved, she cannot paginate anymore in both direction
        run {
            // Ask for a forward pagination
            val snapshot = runBlocking {
                aliceTimeline.awaitPaginate(Timeline.Direction.FORWARDS, 50)
            }
            // 6 for room creation item (backward pagination),and numberOfMessagesToSend (all the message of the room)
            snapshot.size == 6 + numberOfMessagesToSend &&
                    snapshot.checkSendOrder(message, numberOfMessagesToSend, 0)

            // The timeline is fully loaded
            aliceTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS).shouldBeFalse()
            aliceTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS).shouldBeFalse()
        }

        aliceTimeline.dispose()

        cryptoTestData.cleanUp(commonTestHelper)
    }
}
