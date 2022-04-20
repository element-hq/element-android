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

package org.matrix.android.sdk.internal.crypto.encryption

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EncryptionTest : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)

    @Test
    fun test_EncryptionEvent() {
        performTest(roomShouldBeEncrypted = false) { room ->
            // Send an encryption Event as an Event (and not as a state event)
            room.sendEvent(
                    eventType = EventType.STATE_ROOM_ENCRYPTION,
                    content = EncryptionEventContent(algorithm = MXCRYPTO_ALGORITHM_MEGOLM).toContent()
            )
        }
    }

    @Test
    fun test_EncryptionStateEvent() {
        performTest(roomShouldBeEncrypted = true) { room ->
            runBlocking {
                // Send an encryption Event as a State Event
                room.sendStateEvent(
                        eventType = EventType.STATE_ROOM_ENCRYPTION,
                        stateKey = "",
                        body = EncryptionEventContent(algorithm = MXCRYPTO_ALGORITHM_MEGOLM).toContent()
                )
            }
        }
    }

    private fun performTest(roomShouldBeEncrypted: Boolean, action: (Room) -> Unit) {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceInARoom(encryptedRoom = false)

        val aliceSession = cryptoTestData.firstSession
        val room = aliceSession.getRoom(cryptoTestData.roomId)!!

        room.isEncrypted() shouldBe false

        val timeline = room.createTimeline(null, TimelineSettings(10))
        val latch = CountDownLatch(1)
        val timelineListener = object : Timeline.Listener {
            override fun onTimelineFailure(throwable: Throwable) {
            }

            override fun onNewTimelineEvents(eventIds: List<String>) {
                // noop
            }

            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                val newMessages = snapshot
                        .filter { it.root.sendState == SendState.SYNCED }
                        .filter { it.root.getClearType() == EventType.STATE_ROOM_ENCRYPTION }

                if (newMessages.isNotEmpty()) {
                    timeline.removeListener(this)
                    latch.countDown()
                }
            }
        }
        timeline.start()
        timeline.addListener(timelineListener)

        action.invoke(room)
        testHelper.await(latch)
        timeline.dispose()
        testHelper.waitWithLatch {
            room.isEncrypted() shouldBe roomShouldBeEncrypted
            it.countDown()
        }
        cryptoTestData.cleanUp(testHelper)
    }
}
