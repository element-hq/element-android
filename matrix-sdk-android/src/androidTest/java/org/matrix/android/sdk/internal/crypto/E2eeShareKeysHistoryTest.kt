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
import org.amshove.kluent.internal.assertNotEquals
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.query.QueryStringValue
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
import org.matrix.android.sdk.api.session.room.model.shouldShareHistory
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSuspendingCryptoTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.wrapWithTimeout

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
    private fun testShareHistoryWithRoomVisibility(roomHistoryVisibility: RoomHistoryVisibility? = null) =
            runSuspendingCryptoTest(context()) { cryptoTestHelper, testHelper ->
                val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true, roomHistoryVisibility)

                val e2eRoomID = cryptoTestData.roomId

                // Alice
                val aliceSession = cryptoTestData.firstSession.also {
                    it.cryptoService().enableShareKeyOnInvite(true)
                }
                val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!

                // Bob
                val bobSession = cryptoTestData.secondSession!!.also {
                    it.cryptoService().enableShareKeyOnInvite(true)
                }
                val bobRoomPOV = bobSession.roomService().getRoom(e2eRoomID)!!

                assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)
                Log.v("#E2E TEST", "Alice and Bob are in roomId: $e2eRoomID")

                val aliceMessageId: String? = sendMessageInRoom(aliceRoomPOV, "Hello Bob, I am Alice!", testHelper)
                Assert.assertTrue("Message should be sent", aliceMessageId != null)
                Log.v("#E2E TEST", "Alice sent message to roomId: $e2eRoomID")

                // Bob should be able to decrypt the message
                testHelper.retryPeriodically {
                    val timelineEvent = bobSession.roomService().getRoom(e2eRoomID)?.timelineService()?.getTimelineEvent(aliceMessageId!!)
                    (timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE).also {
                        if (it) {
                            Log.v("#E2E TEST", "Bob can decrypt the message: ${timelineEvent?.root?.getDecryptedTextSummary()}")
                        }
                    }
                }

                // Create a new user
                val arisSession = testHelper.createAccount("aris", SessionTestParams(true)).also {
                    it.cryptoService().enableShareKeyOnInvite(true)
                }
                Log.v("#E2E TEST", "Aris user created")

                // Alice invites new user to the room
                Log.v("#E2E TEST", "Alice invites ${arisSession.myUserId}")
                aliceRoomPOV.membershipService().invite(arisSession.myUserId)

                waitForAndAcceptInviteInRoom(arisSession, e2eRoomID, testHelper)

                ensureMembersHaveJoined(aliceSession, arrayListOf(arisSession), e2eRoomID, testHelper)
                Log.v("#E2E TEST", "Aris has joined roomId: $e2eRoomID")

                when (roomHistoryVisibility) {
                    RoomHistoryVisibility.WORLD_READABLE,
                    RoomHistoryVisibility.SHARED,
                    null
                    -> {
                        // Aris should be able to decrypt the message
                        testHelper.retryPeriodically {
                            val timelineEvent = arisSession.roomService().getRoom(e2eRoomID)?.timelineService()?.getTimelineEvent(aliceMessageId!!)
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
                    RoomHistoryVisibility.INVITED,
                    RoomHistoryVisibility.JOINED -> {
                        // Aris should not even be able to get the message
                        testHelper.retryPeriodically {
                            val timelineEvent = arisSession.roomService().getRoom(e2eRoomID)
                                    ?.timelineService()
                                    ?.getTimelineEvent(aliceMessageId!!)
                            timelineEvent == null
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
    ) = runSuspendingCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true, initRoomHistoryVisibility)
        val e2eRoomID = cryptoTestData.roomId

        // Alice
        val aliceSession = cryptoTestData.firstSession.also {
            it.cryptoService().enableShareKeyOnInvite(true)
        }
        val aliceRoomPOV = aliceSession.roomService().getRoom(e2eRoomID)!!
//        val aliceCryptoStore = (aliceSession.cryptoService() as DefaultCryptoService).cryptoStoreForTesting

        // Bob
        val bobSession = cryptoTestData.secondSession!!

        val bobRoomPOV = bobSession.roomService().getRoom(e2eRoomID)!!

        assertEquals(bobRoomPOV.roomSummary()?.joinedMembersCount, 2)
        Log.v("#E2E TEST ROTATION", "Alice and Bob are in roomId: $e2eRoomID")

        val aliceMessageId: String? = sendMessageInRoom(aliceRoomPOV, "Hello Bob, I am Alice!", testHelper)
        Assert.assertTrue("Message should be sent", aliceMessageId != null)
        Log.v("#E2E TEST ROTATION", "Alice sent message to roomId: $e2eRoomID")

        // Bob should be able to decrypt the message
        var firstAliceMessageMegolmSessionId: String? = null
        val bobRoomPov = bobSession.roomService().getRoom(e2eRoomID)
        testHelper.retryPeriodically {
            val timelineEvent = bobRoomPov
                    ?.timelineService()
                    ?.getTimelineEvent(aliceMessageId!!)
            (timelineEvent != null &&
                    timelineEvent.isEncrypted() &&
                    timelineEvent.root.getClearType() == EventType.MESSAGE).also {
                if (it) {
                    firstAliceMessageMegolmSessionId = timelineEvent?.root?.content?.get("session_id") as? String
                    Log.v(
                            "#E2E TEST",
                            "Bob can decrypt the message (sid:$firstAliceMessageMegolmSessionId): ${timelineEvent?.root?.getDecryptedTextSummary()}"
                    )
                }
            }
        }

        Assert.assertNotNull("megolm session id can't be null", firstAliceMessageMegolmSessionId)

        var secondAliceMessageSessionId: String? = null
        sendMessageInRoom(aliceRoomPOV, "Other msg", testHelper)?.let { secondMessage ->
            testHelper.launch {
                testHelper.retryPeriodically {
                    val timelineEvent = bobRoomPov
                            ?.timelineService()
                            ?.getTimelineEvent(secondMessage)
                    (timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE).also {
                        if (it) {
                            secondAliceMessageSessionId = timelineEvent?.root?.content?.get("session_id") as? String
                            Log.v(
                                    "#E2E TEST",
                                    "Bob can decrypt the message (sid:$secondAliceMessageSessionId): ${timelineEvent?.root?.getDecryptedTextSummary()}"
                            )
                        }
                    }
                }
            }
        }
        assertEquals("No rotation needed session should be the same", firstAliceMessageMegolmSessionId, secondAliceMessageSessionId)
        Log.v("#E2E TEST ROTATION", "No rotation needed yet")

        // Let's change the room history visibility
        aliceRoomPOV.stateService()
                .sendStateEvent(
                        eventType = EventType.STATE_ROOM_HISTORY_VISIBILITY,
                        stateKey = "",
                        body = RoomHistoryVisibilityContent(
                                historyVisibilityStr = nextRoomHistoryVisibility.historyVisibilityStr
                        ).toContent()
                )

        // ensure that the state did synced down
        testHelper.retryPeriodically {
            aliceRoomPOV.stateService().getStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY, QueryStringValue.IsEmpty)?.content
                    ?.toModel<RoomHistoryVisibilityContent>()?.historyVisibility == nextRoomHistoryVisibility.historyVisibility
        }

        testHelper.retryPeriodically {
            val roomVisibility = aliceSession.getRoom(e2eRoomID)!!
                    .stateService()
                    .getStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY, QueryStringValue.IsEmpty)
                    ?.content
                    ?.toModel<RoomHistoryVisibilityContent>()
            Log.v("#E2E TEST ROTATION", "Room visibility changed from: ${initRoomHistoryVisibility.name} to: ${roomVisibility?.historyVisibility?.name}")
            roomVisibility?.historyVisibility == nextRoomHistoryVisibility.historyVisibility
        }

        var aliceThirdMessageSessionId: String? = null
        sendMessageInRoom(aliceRoomPOV, "Message after visibility change", testHelper)?.let { thirdMessage ->
            testHelper.retryPeriodically {
                val timelineEvent = bobRoomPov
                        ?.timelineService()
                        ?.getTimelineEvent(thirdMessage)
                (timelineEvent != null &&
                        timelineEvent.isEncrypted() &&
                        timelineEvent.root.getClearType() == EventType.MESSAGE).also {
                    if (it) {
                        aliceThirdMessageSessionId = timelineEvent?.root?.content?.get("session_id") as? String
                    }
                }
            }
        }

        when {
            initRoomHistoryVisibility.shouldShareHistory() == nextRoomHistoryVisibility.historyVisibility?.shouldShareHistory() -> {
                assertEquals("Session shouldn't have been rotated", secondAliceMessageSessionId, aliceThirdMessageSessionId)
                Log.v("#E2E TEST ROTATION", "Rotation is not needed")
            }
            initRoomHistoryVisibility.shouldShareHistory() != nextRoomHistoryVisibility.historyVisibility!!.shouldShareHistory() -> {
                assertNotEquals("Session should have been rotated", secondAliceMessageSessionId, aliceThirdMessageSessionId)
                Log.v("#E2E TEST ROTATION", "Rotation is needed!")
            }
        }

        cryptoTestData.cleanUp(testHelper)
    }

    private suspend fun sendMessageInRoom(aliceRoomPOV: Room, text: String, testHelper: CommonTestHelper): String? {
        return testHelper.sendTextMessageSuspending(aliceRoomPOV, text, 1).firstOrNull()?.eventId
    }

    private suspend fun ensureMembersHaveJoined(aliceSession: Session, otherAccounts: List<Session>, e2eRoomID: String, testHelper: CommonTestHelper) {
        testHelper.retryPeriodically {
            otherAccounts.map {
                aliceSession.roomService().getRoomMember(it.myUserId, e2eRoomID)?.membership
            }.all {
                it == Membership.JOIN
            }
        }
    }

    private suspend fun waitForAndAcceptInviteInRoom(otherSession: Session, e2eRoomID: String, testHelper: CommonTestHelper) {
        testHelper.retryPeriodically {
            val roomSummary = otherSession.roomService().getRoomSummary(e2eRoomID)
            (roomSummary != null && roomSummary.membership == Membership.INVITE).also {
                if (it) {
                    Log.v("#E2E TEST", "${otherSession.myUserId} can see the invite from alice")
                }
            }
        }

        wrapWithTimeout(60_000) {
            Log.v("#E2E TEST", "${otherSession.myUserId} tries to join room $e2eRoomID")
            try {
                otherSession.roomService().joinRoom(e2eRoomID)
            } catch (ex: JoinRoomFailure.JoinedWithTimeout) {
                // it's ok we will wait after
            }
        }

        Log.v("#E2E TEST", "${otherSession.myUserId} waiting for join echo ...")
        testHelper.retryPeriodically {
            val roomSummary = otherSession.roomService().getRoomSummary(e2eRoomID)
            roomSummary != null && roomSummary.membership == Membership.JOIN
        }
    }
}
