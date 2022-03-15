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

package org.matrix.android.sdk.internal.crypto

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.crypto.model.event.RoomKeyContent

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class PreShareKeysTest : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)

    @Test
    fun ensure_outbound_session_happy_path() {
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val e2eRoomID = testData.roomId
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!

        // clear any outbound session
        aliceSession.cryptoService().discardOutboundSession(e2eRoomID)

        val preShareCount = bobSession.cryptoService().getGossipingEvents().count {
            it.senderId == aliceSession.myUserId &&
                    it.getClearType() == EventType.ROOM_KEY
        }

        assertEquals("Bob should not have receive any key from alice at this point", 0, preShareCount)
        Log.d("#Test", "Room Key Received from alice $preShareCount")

        // Force presharing of new outbound key
        testHelper.doSync<Unit> {
            aliceSession.cryptoService().prepareToEncrypt(e2eRoomID, it)
        }

        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val newGossipCount = bobSession.cryptoService().getGossipingEvents().count {
                    it.senderId == aliceSession.myUserId &&
                            it.getClearType() == EventType.ROOM_KEY
                }
                newGossipCount > preShareCount
            }
        }

        val latest = bobSession.cryptoService().getGossipingEvents().lastOrNull {
            it.senderId == aliceSession.myUserId &&
                    it.getClearType() == EventType.ROOM_KEY
        }

        val content = latest?.getClearContent().toModel<RoomKeyContent>()
        assertNotNull("Bob should have received and decrypted a room key event from alice", content)
        assertEquals("Wrong room", e2eRoomID, content!!.roomId)
        val megolmSessionId = content.sessionId!!

        val sharedIndex = aliceSession.cryptoService().getSharedWithInfo(e2eRoomID, megolmSessionId)
                .getObject(bobSession.myUserId, bobSession.sessionParams.deviceId)

        assertEquals("The session received by bob should match what alice sent", 0, sharedIndex)

        // Just send a real message as test
        val sentEvent = testHelper.sendTextMessage(aliceSession.getRoom(e2eRoomID)!!, "Allo", 1).first()

        assertEquals("Unexpected megolm session", megolmSessionId, sentEvent.root.content.toModel<EncryptedEventContent>()?.sessionId,)
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEvent.eventId)?.root?.getClearType() == EventType.MESSAGE
            }
        }

        testData.cleanUp(testHelper)
    }
}
