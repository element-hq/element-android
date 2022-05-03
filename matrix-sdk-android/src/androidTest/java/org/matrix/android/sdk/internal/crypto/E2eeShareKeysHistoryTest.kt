/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
import androidx.test.filters.LargeTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.shouldShareHistory
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class E2eeShareKeysHistoryTest : InstrumentedTest {

    @Test
    fun testShareMessagesHistoryWithRoomWorldReadable() {
        testShareHistoryWithRoomVisibility(RoomHistoryVisibility.WORLD_READABLE)
    }

    @Test
    fun testShareMessagesHistoryWithRoomShared() {
        testShareHistoryWithRoomVisibility(RoomHistoryVisibility.SHARED)
    }

    @Test
    fun testShareMessagesHistoryWithRoomJoined() {
        testShareHistoryWithRoomVisibility(RoomHistoryVisibility.JOINED)
    }

    @Test
    fun testShareMessagesHistoryWithRoomInvited() {
        testShareHistoryWithRoomVisibility(RoomHistoryVisibility.INVITED)
    }

    /**
     * In this test we create a room and test that new members
     * can decrypt history when the room visibility is
     * RoomHistoryVisibility.SHARED or RoomHistoryVisibility.WORLD_READABLE.
     * We should not be able to view messages/decrypt otherwise
     */
    private fun testShareHistoryWithRoomVisibility(roomHistoryVisibility: RoomHistoryVisibility? = null) {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true, roomHistoryVisibility)

        val e2eRoomID = cryptoTestData.roomId

        // Alice
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!

        // Bob
        val bobSession = cryptoTestData.secondSession
        val bobRoomPOV = bobSession!!.roomService().getRoom(e2eRoomID)!!

        assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)
        Log.v("#E2E TEST", "Alice and Bob are in roomId: $e2eRoomID")

        val aliceMessageId: String? = sendMessageInRoom(aliceRoomPOV, "Hello Bob, I am Alice!", testHelper)
        Assert.assertTrue("Message should be sent", aliceMessageId != null)
        Log.v("#E2E TEST", "Alice sent message to roomId: $e2eRoomID")

        // Bob should be able to decrypt the message
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val timelineEvent = bobSession.roomService().getRoom(e2eRoomID)?.getTimelineEvent(aliceMessageId!!)
                (timelineEvent != null &&
                        timelineEvent.isEncrypted() &&
                        timelineEvent.root.getClearType() == EventType.MESSAGE).also {
                    if (it) {
                        Log.v("#E2E TEST", "Bob can decrypt the message: ${timelineEvent?.root?.getDecryptedTextSummary()}")
                    }
                }
            }
        }

        // Create a new user
        val arisSession = testHelper.createAccount("aris", SessionTestParams(true))
        Log.v("#E2E TEST", "Aris user created")

        // Alice invites new user to the room
        testHelper.runBlockingTest {
            Log.v("#E2E TEST", "Alice invites ${arisSession.myUserId}")
            aliceRoomPOV.invite(arisSession.myUserId)
        }

        waitForAndAcceptInviteInRoom(arisSession, e2eRoomID, testHelper)

        ensureMembersHaveJoined(aliceSession, arrayListOf(arisSession), e2eRoomID, testHelper)
        Log.v("#E2E TEST", "Aris has joined roomId: $e2eRoomID")

        when (roomHistoryVisibility) {
            RoomHistoryVisibility.WORLD_READABLE,
            RoomHistoryVisibility.SHARED,
            null
                                         -> {
                // Aris should be able to decrypt the message
                testHelper.waitWithLatch { latch ->
                    testHelper.retryPeriodicallyWithLatch(latch) {
                        val timelineEvent = arisSession.roomService().getRoom(e2eRoomID)?.getTimelineEvent(aliceMessageId!!)
                        (timelineEvent != null &&
                                timelineEvent.isEncrypted() &&
                                timelineEvent.root.getClearType() == EventType.MESSAGE
                                ).also {
                                    if (it) {
                                        Log.v("#E2E TEST", "Aris can decrypt the message: ${timelineEvent?.root?.getDecryptedTextSummary()}")
                                    }
                                }
                    }
                }
            }
            RoomHistoryVisibility.INVITED,
            RoomHistoryVisibility.JOINED -> {
                // Aris should not even be able to get the message
                testHelper.waitWithLatch { latch ->
                    testHelper.retryPeriodicallyWithLatch(latch) {
                        val timelineEvent = arisSession.roomService().getRoom(e2eRoomID)?.getTimelineEvent(aliceMessageId!!)
                        timelineEvent == null
                    }
                }
            }
        }

        testHelper.signOutAndClose(arisSession)
        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    fun testNeedsRotationFromWorldReadableToShared() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("shared"))
    }

    @Test
    fun testNeedsRotationFromWorldReadableToInvited() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("invited"))
    }

    @Test
    fun testNeedsRotationFromWorldReadableToJoined() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("joined"))
    }

    @Test
    fun testNeedsRotationFromSharedToWorldReadable() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.SHARED, RoomHistoryVisibilityContent("world_readable"))
    }

    @Test
    fun testNeedsRotationFromSharedToInvited() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.SHARED, RoomHistoryVisibilityContent("invited"))
    }

    @Test
    fun testNeedsRotationFromSharedToJoined() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.SHARED, RoomHistoryVisibilityContent("joined"))
    }

    @Test
    fun testNeedsRotationFromInvitedToShared() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("shared"))
    }

    @Test
    fun testNeedsRotationFromInvitedToWorldReadable() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("world_readable"))
    }

    @Test
    fun testNeedsRotationFromInvitedToJoined() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("joined"))
    }

    @Test
    fun testNeedsRotationFromJoinedToShared() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("shared"))
    }

    @Test
    fun testNeedsRotationFromJoinedToInvited() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("invited"))
    }

    @Test
    fun testNeedsRotationFromJoinedToWorldReadable() {
        testRotationDueToVisibilityChange(RoomHistoryVisibility.WORLD_READABLE, RoomHistoryVisibilityContent("world_readable"))
    }

    /**
     * In this test we will test that a rotation is needed when
     * When the room's history visibility setting changes to world_readable or shared
     * from invited or joined, or changes to invited or joined from world_readable or shared,
     * senders that support this flag must rotate their megolm sessions.
     */
    private fun testRotationDueToVisibilityChange(
            initRoomHistoryVisibility: RoomHistoryVisibility,
            nextRoomHistoryVisibility: RoomHistoryVisibilityContent
    ) {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true, initRoomHistoryVisibility)
        val e2eRoomID = cryptoTestData.roomId

        // Alice
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!
        val aliceCryptoStore = (aliceSession.cryptoService() as DefaultCryptoService).cryptoStoreForTesting

        // Bob
        val bobSession = cryptoTestData.secondSession
        val bobRoomPOV = bobSession!!.roomService().getRoom(e2eRoomID)!!

        assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)
        Log.v("#E2E TEST ROTATION", "Alice and Bob are in roomId: $e2eRoomID")

        val aliceMessageId: String? = sendMessageInRoom(aliceRoomPOV, "Hello Bob, I am Alice!", testHelper)
        Assert.assertTrue("Message should be sent", aliceMessageId != null)
        Log.v("#E2E TEST ROTATION", "Alice sent message to roomId: $e2eRoomID")

        // Bob should be able to decrypt the message
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val timelineEvent = bobSession.roomService().getRoom(e2eRoomID)?.getTimelineEvent(aliceMessageId!!)
                (timelineEvent != null &&
                        timelineEvent.isEncrypted() &&
                        timelineEvent.root.getClearType() == EventType.MESSAGE).also {
                    if (it) {
                        Log.v("#E2E TEST", "Bob can decrypt the message: ${timelineEvent?.root?.getDecryptedTextSummary()}")
                    }
                }
            }
        }

        // Rotation has already been done so we do not need to rotate again
        assertEquals(aliceCryptoStore.needsRotationDueToVisibilityChange(e2eRoomID), false)
        Log.v("#E2E TEST ROTATION", "No rotation needed yet")

        // Let's change the room history visibility
        testHelper.waitWithLatch {
            aliceRoomPOV.sendStateEvent(
                    eventType = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                    stateKey = "",
                    body = RoomHistoryVisibilityContent(_historyVisibility = nextRoomHistoryVisibility._historyVisibility).toContent()
            )
            it.countDown()
        }
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val roomVisibility = aliceSession.getRoom(e2eRoomID)!!
                        .getStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY)
                        ?.content
                        ?.toModel<RoomHistoryVisibilityContent>()
                Log.v("#E2E TEST ROTATION", "Room visibility changed from: ${initRoomHistoryVisibility.name} to: ${roomVisibility?.historyVisibility?.name}")
                roomVisibility?.historyVisibility == nextRoomHistoryVisibility.historyVisibility
            }
        }

        when {
            initRoomHistoryVisibility.shouldShareHistory() == nextRoomHistoryVisibility.historyVisibility?.shouldShareHistory()  -> {
                assertEquals(aliceCryptoStore.needsRotationDueToVisibilityChange(e2eRoomID), false)
                Log.v("#E2E TEST ROTATION", "Rotation is not needed")
            }
            initRoomHistoryVisibility.shouldShareHistory() != nextRoomHistoryVisibility.historyVisibility!!.shouldShareHistory() -> {
                assertEquals(aliceCryptoStore.needsRotationDueToVisibilityChange(e2eRoomID), true)
                Log.v("#E2E TEST ROTATION", "Rotation is needed!")
            }
        }

        cryptoTestData.cleanUp(testHelper)
    }

    private fun sendMessageInRoom(aliceRoomPOV: Room, text: String, testHelper: CommonTestHelper): String? {
        aliceRoomPOV.sendTextMessage(text)
        var sentEventId: String? = null
        testHelper.waitWithLatch(4 * TestConstants.timeOutMillis) { latch ->
            val timeline = aliceRoomPOV.createTimeline(null, TimelineSettings(60))
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

    private fun ensureMembersHaveJoined(aliceSession: Session, otherAccounts: List<Session>, e2eRoomID: String, testHelper: CommonTestHelper) {
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                otherAccounts.map {
                    aliceSession.roomService().getRoomMember(it.myUserId, e2eRoomID)?.membership
                }.all {
                    it == Membership.JOIN
                }
            }
        }
    }

    private fun waitForAndAcceptInviteInRoom(otherSession: Session, e2eRoomID: String, testHelper: CommonTestHelper) {
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val roomSummary = otherSession.roomService().getRoomSummary(e2eRoomID)
                (roomSummary != null && roomSummary.membership == Membership.INVITE).also {
                    if (it) {
                        Log.v("#E2E TEST", "${otherSession.myUserId} can see the invite from alice")
                    }
                }
            }
        }

        testHelper.runBlockingTest(60_000) {
            Log.v("#E2E TEST", "${otherSession.myUserId} tries to join room $e2eRoomID")
            try {
                otherSession.roomService().joinRoom(e2eRoomID)
            } catch (ex: JoinRoomFailure.JoinedWithTimeout) {
                // it's ok we will wait after
            }
        }

        Log.v("#E2E TEST", "${otherSession.myUserId} waiting for join echo ...")
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val roomSummary = otherSession.roomService().getRoomSummary(e2eRoomID)
                roomSummary != null && roomSummary.membership == Membership.JOIN
            }
        }
    }
}
