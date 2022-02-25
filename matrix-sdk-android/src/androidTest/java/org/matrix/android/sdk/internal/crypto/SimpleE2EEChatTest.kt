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

package org.matrix.android.sdk.internal.crypto

import android.util.Log
import androidx.test.filters.LargeTest
import kotlinx.coroutines.delay
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class SimpleE2EEChatTest : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)

    /**
     * Simple test that create an e2ee room.
     * Some new members are added, and a message is sent.
     * We check that the message is e2e and can be decrypted.
     *
     * Additional users join, we check that they can't decrypt history
     *
     * Alice sends a new message, then check that the new one can be decrypted
     */
    @Test
    fun testSendingE2EEMessages() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // add some more users and invite them
        val otherAccounts = listOf("benoit", "valere", "ganfra") // , "adam", "manu")
                .map {
                    testHelper.createAccount(it, SessionTestParams(true))
                }

        Log.v("#E2E TEST", "All accounts created")
        // we want to invite them in the room
        otherAccounts.forEach {
            testHelper.runBlockingTest {
                Log.v("#E2E TEST", "Alice invites ${it.myUserId}")
                aliceRoomPOV.invite(it.myUserId)
            }
        }

        // All user should accept invite
        otherAccounts.forEach { otherSession ->
            waitForAndAcceptInviteInRoom(otherSession, e2eRoomID)
            Log.v("#E2E TEST", "${otherSession.myUserId} joined room $e2eRoomID")
        }

        // check that alice see them as joined (not really necessary?)
        ensureMembersHaveJoined(aliceSession, otherAccounts, e2eRoomID)

        Log.v("#E2E TEST", "All users have joined the room")

        Log.v("#E2E TEST", "Alice is sending the message")

        val text = "This is my message"
        val sentEventId: String? = sendMessageInRoom(aliceRoomPOV, text)
        //        val sentEvent = testHelper.sendTextMessage(aliceRoomPOV, "Hello all", 1).first()
        Assert.assertTrue("Message should be sent", sentEventId != null)

        // All should be able to decrypt
        otherAccounts.forEach { otherSession ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = otherSession.getRoom(e2eRoomID)?.getTimeLineEvent(sentEventId!!)
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // Add a new user to the room, and check that he can't decrypt
        val newAccount = listOf("adam") // , "adam", "manu")
                .map {
                    testHelper.createAccount(it, SessionTestParams(true))
                }

        newAccount.forEach {
            testHelper.runBlockingTest {
                Log.v("#E2E TEST", "Alice invites ${it.myUserId}")
                aliceRoomPOV.invite(it.myUserId)
            }
        }

        newAccount.forEach {
            waitForAndAcceptInviteInRoom(it, e2eRoomID)
        }

        ensureMembersHaveJoined(aliceSession, newAccount, e2eRoomID)

        // wait a bit
        testHelper.runBlockingTest {
            delay(3_000)
        }

        // check that messages are encrypted (uisi)
        newAccount.forEach { otherSession ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = otherSession.getRoom(e2eRoomID)?.getTimeLineEvent(sentEventId!!).also {
                        Log.v("#E2E TEST", "Event seen by new user ${it?.root?.getClearType()}|${it?.root?.mCryptoError}")
                    }
                    timeLineEvent != null &&
                            timeLineEvent.root.getClearType() == EventType.ENCRYPTED &&
                            timeLineEvent.root.mCryptoError == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID
                }
            }
        }

        // Let alice send a new message
        Log.v("#E2E TEST", "Alice sends a new message")

        val secondMessage = "2 This is my message"
        val secondSentEventId: String? = sendMessageInRoom(aliceRoomPOV, secondMessage)

        // new members should be able to decrypt it
        newAccount.forEach { otherSession ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = otherSession.getRoom(e2eRoomID)?.getTimeLineEvent(secondSentEventId!!).also {
                        Log.v("#E2E TEST", "Second Event seen by new user ${it?.root?.getClearType()}|${it?.root?.mCryptoError}")
                    }
                    timeLineEvent != null &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE &&
                            secondMessage.equals(timeLineEvent.root.getClearContent().toModel<MessageContent>()?.body)
                }
            }
        }

        otherAccounts.forEach {
            testHelper.signOutAndClose(it)
        }
        newAccount.forEach { testHelper.signOutAndClose(it) }

        cryptoTestData.cleanUp(testHelper)
    }

    private fun sendMessageInRoom(aliceRoomPOV: Room, text: String): String? {
        aliceRoomPOV.sendTextMessage(text)
        var sentEventId: String? = null
        testHelper.waitWithLatch(4 * 60_000) {
            val timeline = aliceRoomPOV.createTimeline(null, TimelineSettings(60))
            timeline.start()

            testHelper.retryPeriodicallyWithLatch(it) {
                val decryptedMsg = timeline.getSnapshot()
                        .filter { it.root.getClearType() == EventType.MESSAGE }
                        .also {
                            Log.v("#E2E TEST", "Timeline snapshot is ${it.map { "${it.root.type}|${it.root.sendState}" }.joinToString(",", "[", "]")}")
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

    private fun ensureMembersHaveJoined(aliceSession: Session, otherAccounts: List<Session>, e2eRoomID: String) {
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                otherAccounts.map {
                    aliceSession.getRoomMember(it.myUserId, e2eRoomID)?.membership
                }.all {
                    it == Membership.JOIN
                }
            }
        }
    }

    private fun waitForAndAcceptInviteInRoom(otherSession: Session, e2eRoomID: String) {
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val roomSummary = otherSession.getRoomSummary(e2eRoomID)
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
                otherSession.joinRoom(e2eRoomID)
            } catch (ex: JoinRoomFailure.JoinedWithTimeout) {
                // it's ok we will wait after
            }
        }

        Log.v("#E2E TEST", "${otherSession.myUserId} waiting for join echo ...")
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val roomSummary = otherSession.getRoomSummary(e2eRoomID)
                roomSummary != null && roomSummary.membership == Membership.JOIN
            }
        }
    }
}
