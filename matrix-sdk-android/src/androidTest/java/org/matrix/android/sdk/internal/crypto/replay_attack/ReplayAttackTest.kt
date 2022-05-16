/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.replay_attack

import android.util.Log
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class ReplayAttackTest : InstrumentedTest {

    @Test
    fun replayAttackTest() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)

        val e2eRoomID = cryptoTestData.roomId

        // Alice
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!

        // Bob
        val bobSession = cryptoTestData.secondSession
        val bobRoomPOV = bobSession!!.roomService().getRoom(e2eRoomID)!!

        assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)
        Log.v("##REPLAY ATTACK", "Alice and Bob are in roomId: $e2eRoomID")


        val sentEvents = testHelper.sendTextMessage(aliceRoomPOV, "Hello", 20)

//        val aliceMessageId: String? = sendMessageInRoom(aliceRoomPOV, "Hello Bob, I am Alice!", testHelper)
        Assert.assertTrue("Message should be sent", sentEvents.size ==  20)
        Log.v("##REPLAY ATTACK", "Alice sent message to roomId: $e2eRoomID")

        // Bob should be able to decrypt the message
//        testHelper.waitWithLatch { latch ->
//            testHelper.retryPeriodicallyWithLatch(latch) {
//                val timelineEvent = bobSession.roomService().getRoom(e2eRoomID)?.timelineService()?.getTimelineEvent(aliceMessageId!!)
//                (timelineEvent != null &&
//                        timelineEvent.isEncrypted() &&
//                        timelineEvent.root.getClearType() == EventType.MESSAGE).also {
//                    if (it) {
//                        Log.v("#E2E TEST", "Bob can decrypt the message: ${timelineEvent?.root?.getDecryptedTextSummary()}")
//                    }
//                }
//            }
//        }
//
//        // Create a new user
//        val arisSession = testHelper.createAccount("aris", SessionTestParams(true))
//        Log.v("#E2E TEST", "Aris user created")
//
//        // Alice invites new user to the room
//        testHelper.runBlockingTest {
//            Log.v("#E2E TEST", "Alice invites ${arisSession.myUserId}")
//            aliceRoomPOV.membershipService().invite(arisSession.myUserId)
//        }
//
//        waitForAndAcceptInviteInRoom(arisSession, e2eRoomID, testHelper)
//
//        ensureMembersHaveJoined(aliceSession, arrayListOf(arisSession), e2eRoomID, testHelper)
//        Log.v("#E2E TEST", "Aris has joined roomId: $e2eRoomID")
//
//        when (roomHistoryVisibility) {
//            RoomHistoryVisibility.WORLD_READABLE,
//            RoomHistoryVisibility.SHARED,
//            null
//                                         -> {
//                // Aris should be able to decrypt the message
//                testHelper.waitWithLatch { latch ->
//                    testHelper.retryPeriodicallyWithLatch(latch) {
//                        val timelineEvent = arisSession.roomService().getRoom(e2eRoomID)?.timelineService()?.getTimelineEvent(aliceMessageId!!)
//                        (timelineEvent != null &&
//                                timelineEvent.isEncrypted() &&
//                                timelineEvent.root.getClearType() == EventType.MESSAGE
//                                ).also {
//                                    if (it) {
//                                        Log.v("#E2E TEST", "Aris can decrypt the message: ${timelineEvent?.root?.getDecryptedTextSummary()}")
//                                    }
//                                }
//                    }
//                }
//            }
//            RoomHistoryVisibility.INVITED,
//            RoomHistoryVisibility.JOINED -> {
//                // Aris should not even be able to get the message
//                testHelper.waitWithLatch { latch ->
//                    testHelper.retryPeriodicallyWithLatch(latch) {
//                        val timelineEvent = arisSession.roomService().getRoom(e2eRoomID)
//                                ?.timelineService()
//                                ?.getTimelineEvent(aliceMessageId!!)
//                        timelineEvent == null
//                    }
//                }
//            }
//        }

//        testHelper.signOutAndClose(arisSession)
        cryptoTestData.cleanUp(testHelper)
    }

    private fun sendMessageInRoom(aliceRoomPOV: Room, text: String, testHelper: CommonTestHelper): String? {
        aliceRoomPOV.sendService().sendTextMessage(text)
        var sentEventId: String? = null
        testHelper.waitWithLatch(4 * TestConstants.timeOutMillis) { latch ->
            val timeline = aliceRoomPOV.timelineService().createTimeline(null, TimelineSettings(60))
            timeline.start()
            testHelper.retryPeriodicallyWithLatch(latch) {
                val decryptedMsg = timeline.getSnapshot()
                        .filter { it.root.getClearType() == EventType.MESSAGE }
                        .also { list ->
                            val message = list.joinToString(",", "[", "]") { "${it.root.type}|${it.root.sendState}" }
                            Log.v("#E2E TEST", "Timeline snapshot is $message")
                        }
                        .filter { it.root.sendState == SendState.SYNCED }
                        .firstOrNull { it.root.getClearContent().toModel<MessageContent>()?.body?.startsWith(text) == true }
                sentEventId = decryptedMsg?.eventId
                decryptedMsg != null
            }

            timeline.dispose()
        }
        return sentEventId
    }
}
