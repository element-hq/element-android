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

package org.matrix.android.sdk.internal.crypto.gossiping

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.OutgoingRoomKeyRequestState
import org.matrix.android.sdk.api.session.crypto.RequestResult
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.RetryTestRule
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.mustFail

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
@Ignore
class KeyShareTests : InstrumentedTest {

    @get:Rule val rule = RetryTestRule(3)

    @Test
    fun test_DoNotSelfShareIfNotTrusted() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->

        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        Log.v("TEST", "=======> AliceSession 1 is ${aliceSession.sessionParams.deviceId}")

        // Create an encrypted room and add a message
        val roomId = commonTestHelper.runBlockingTest {
            aliceSession.roomService().createRoom(
                    CreateRoomParams().apply {
                        visibility = RoomDirectoryVisibility.PRIVATE
                        enableEncryption()
                    }
            )
        }
        val room = aliceSession.getRoom(roomId)
        assertNotNull(room)
        Thread.sleep(4_000)
        assertTrue(room?.roomCryptoService()?.isEncrypted() == true)

        val sentEvent = commonTestHelper.sendTextMessage(room!!, "My Message", 1).first()
        val sentEventId = sentEvent.eventId
        val sentEventText = sentEvent.getLastMessageContent()?.body

        // Open a new session
        val aliceSession2 = commonTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(false))
        // block key requesting for now as decrypt will send requests (room summary is trying to decrypt)
        aliceSession2.cryptoService().enableKeyGossiping(false)
        commonTestHelper.syncSession(aliceSession2)

        Log.v("TEST", "=======> AliceSession 2 is ${aliceSession2.sessionParams.deviceId}")

        val roomSecondSessionPOV = aliceSession2.getRoom(roomId)

        val receivedEvent = roomSecondSessionPOV?.getTimelineEvent(sentEventId)
        assertNotNull(receivedEvent)
        assert(receivedEvent!!.isEncrypted())

        commonTestHelper.runBlockingTest {
            mustFail {
                aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            }
        }

        val outgoingRequestsBefore = aliceSession2.cryptoService().getOutgoingRoomKeyRequests()
        assertEquals("There should be no request as it's disabled", 0, outgoingRequestsBefore.size)

        // Try to request
        aliceSession2.cryptoService().enableKeyGossiping(true)
        aliceSession2.cryptoService().requestRoomKeyForEvent(receivedEvent.root)

        val eventMegolmSessionId = receivedEvent.root.content.toModel<EncryptedEventContent>()?.sessionId

        var outGoingRequestId: String? = null

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession2.cryptoService().getOutgoingRoomKeyRequests()
                        .let {
                            val outgoing = it.firstOrNull { it.sessionId == eventMegolmSessionId }
                            outGoingRequestId = outgoing?.requestId
                            outgoing != null
                        }
            }
        }
        Log.v("TEST", "=======> Outgoing requet Id is $outGoingRequestId")

        val outgoingRequestAfter = aliceSession2.cryptoService().getOutgoingRoomKeyRequests()

        // We should have a new request
        Assert.assertTrue(outgoingRequestAfter.size > outgoingRequestsBefore.size)
        Assert.assertNotNull(outgoingRequestAfter.first { it.sessionId == eventMegolmSessionId })

        // The first session should see an incoming request
        // the request should be refused, because the device is not trusted
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                // DEBUG LOGS
//                aliceSession.cryptoService().getIncomingRoomKeyRequests().let {
//                    Log.v("TEST", "Incoming request Session 1 (looking for $outGoingRequestId)")
//                    Log.v("TEST", "=========================")
//                    it.forEach { keyRequest ->
//                        Log.v("TEST", "[ts${keyRequest.localCreationTimestamp}] requestId ${keyRequest.requestId}, for sessionId ${keyRequest.requestBody?.sessionId}")
//                    }
//                    Log.v("TEST", "=========================")
//                }

                val incoming = aliceSession.cryptoService().getIncomingRoomKeyRequests().firstOrNull { it.requestId == outGoingRequestId }
                incoming != null
            }
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                // DEBUG LOGS
                aliceSession2.cryptoService().getOutgoingRoomKeyRequests().forEach { keyRequest ->
                    Log.v("TEST", "=========================")
                    Log.v("TEST", "requestId ${keyRequest.requestId}, for sessionId ${keyRequest.requestBody?.sessionId}")
                    Log.v("TEST", "replies -> ${keyRequest.results.joinToString { it.toString() }}")
                    Log.v("TEST", "=========================")
                }

                val outgoing = aliceSession2.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.requestId == outGoingRequestId }
                val reply = outgoing?.results?.firstOrNull { it.userId == aliceSession.myUserId && it.fromDevice == aliceSession.sessionParams.deviceId }
                val resultCode = (reply?.result as? RequestResult.Failure)?.code
                resultCode == WithHeldCode.UNVERIFIED
            }
        }

        commonTestHelper.runBlockingTest {
            mustFail {
                aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            }
        }

        // Mark the device as trusted
        aliceSession.cryptoService().setDeviceVerification(
                DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true), aliceSession.myUserId,
                aliceSession2.sessionParams.deviceId ?: ""
        )

        // Re request
        aliceSession2.cryptoService().reRequestRoomKeyForEvent(receivedEvent.root)

        cryptoTestHelper.ensureCanDecrypt(listOf(receivedEvent.eventId), aliceSession2, roomId, listOf(sentEventText ?: ""))

        commonTestHelper.signOutAndClose(aliceSession)
        commonTestHelper.signOutAndClose(aliceSession2)
    }

    // See E2ESanityTest for a test regarding secret sharing

    /**
     * Test that the sender of a message accepts to re-share to another user
     * if the key was originally shared with him
     */
    @Test
    fun test_reShareIfWasIntendedToBeShared() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->

        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = testData.firstSession
        val roomFromAlice = aliceSession.getRoom(testData.roomId)!!
        val bobSession = testData.secondSession!!

        val sentEvent = commonTestHelper.sendTextMessage(roomFromAlice, "Hello", 1).first()
        val sentEventMegolmSession = sentEvent.root.content.toModel<EncryptedEventContent>()!!.sessionId!!

        // bob should be able to decrypt
        cryptoTestHelper.ensureCanDecrypt(listOf(sentEvent.eventId), bobSession, testData.roomId, listOf(sentEvent.getLastMessageContent()?.body ?: ""))

        // Let's try to request any how.
        // As it was share previously alice should accept to reshare
        bobSession.cryptoService().reRequestRoomKeyForEvent(sentEvent.root)

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val outgoing = bobSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val aliceReply = outgoing?.results?.firstOrNull { it.userId == aliceSession.myUserId && it.fromDevice == aliceSession.sessionParams.deviceId }
                aliceReply != null && aliceReply.result is RequestResult.Success
            }
        }
    }

    /**
     * Test that our own devices accept to reshare to unverified device if it was shared initialy
     * if the key was originally shared with him
     */
    @Test
    fun test_reShareToUnverifiedIfWasIntendedToBeShared() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->

        val testData = cryptoTestHelper.testHelper.launch { cryptoTestHelper.doE2ETestWithAliceInARoom(true) }
        val aliceSession = testData.firstSession
        val roomFromAlice = aliceSession.getRoom(testData.roomId)!!

        val aliceNewSession = commonTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))

        // we wait for alice first session to be aware of that session?
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val newSession = aliceSession.cryptoService().getUserDevices(aliceSession.myUserId)
                        .firstOrNull { it.deviceId == aliceNewSession.sessionParams.deviceId }
                newSession != null
            }
        }
        val sentEvent = commonTestHelper.sendTextMessage(roomFromAlice, "Hello", 1).first()
        val sentEventMegolmSession = sentEvent.root.content.toModel<EncryptedEventContent>()!!.sessionId!!

        // Let's try to request any how.
        // As it was share previously alice should accept to reshare
        aliceNewSession.cryptoService().reRequestRoomKeyForEvent(sentEvent.root)

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val ownDeviceReply =
                        outgoing?.results?.firstOrNull { it.userId == aliceSession.myUserId && it.fromDevice == aliceSession.sessionParams.deviceId }
                ownDeviceReply != null && ownDeviceReply.result is RequestResult.Success
            }
        }
    }

    /**
     * Tests that keys reshared with own verified session are done from the earliest known index
     */
    @Test
    fun test_reShareFromTheEarliestKnownIndexWithOwnVerifiedSession() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->

        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!
        val roomFromBob = bobSession.getRoom(testData.roomId)!!

        val sentEvents = commonTestHelper.sendTextMessage(roomFromBob, "Hello", 3)
        val sentEventMegolmSession = sentEvents.first().root.content.toModel<EncryptedEventContent>()!!.sessionId!!

        // Let alice now add a new session
        val aliceNewSession = commonTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(false))
        aliceNewSession.cryptoService().enableKeyGossiping(false)
        commonTestHelper.syncSession(aliceNewSession)

        // we wait bob first session to be aware of that session?
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val newSession = bobSession.cryptoService().getUserDevices(aliceSession.myUserId)
                        .firstOrNull { it.deviceId == aliceNewSession.sessionParams.deviceId }
                newSession != null
            }
        }

        val newEvent = commonTestHelper.sendTextMessage(roomFromBob, "The New", 1).first()
        val newEventId = newEvent.eventId
        val newEventText = newEvent.getLastMessageContent()!!.body

        // alice should be able to decrypt the new one
        cryptoTestHelper.ensureCanDecrypt(listOf(newEventId), aliceNewSession, testData.roomId, listOf(newEventText))
        // but not the first one!
        cryptoTestHelper.ensureCannotDecrypt(sentEvents.map { it.eventId }, aliceNewSession, testData.roomId)

        // All should be using the same session id
        sentEvents.forEach {
            assertEquals(sentEventMegolmSession, it.root.content.toModel<EncryptedEventContent>()!!.sessionId)
        }
        assertEquals(sentEventMegolmSession, newEvent.root.content.toModel<EncryptedEventContent>()!!.sessionId)

        // Request a first time, bob should reply with unauthorized and alice should reply with unverified
        aliceNewSession.cryptoService().enableKeyGossiping(true)
        aliceNewSession.cryptoService().reRequestRoomKeyForEvent(newEvent.root)

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val ownDeviceReply = outgoing?.results
                        ?.firstOrNull { it.userId == aliceSession.myUserId && it.fromDevice == aliceSession.sessionParams.deviceId }
                val result = ownDeviceReply?.result
                Log.v("TEST", "own device result is $result")
                result != null && result is RequestResult.Failure && result.code == WithHeldCode.UNVERIFIED
            }
        }

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val bobDeviceReply = outgoing?.results
                        ?.firstOrNull { it.userId == bobSession.myUserId && it.fromDevice == bobSession.sessionParams.deviceId }
                val result = bobDeviceReply?.result
                Log.v("TEST", "bob device result is $result")
                result != null && result is RequestResult.Success && result.chainIndex > 0
            }
        }

        // it's a success but still can't decrypt first message
        cryptoTestHelper.ensureCannotDecrypt(sentEvents.map { it.eventId }, aliceNewSession, testData.roomId)

        // Mark the new session as verified
        aliceSession.cryptoService()
                .verificationService()
                .markedLocallyAsManuallyVerified(aliceNewSession.myUserId, aliceNewSession.sessionParams.deviceId!!)

        // Let's now try to request
        aliceNewSession.cryptoService().reRequestRoomKeyForEvent(sentEvents.first().root)

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                // DEBUG LOGS
                aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().forEach { keyRequest ->
                    Log.v("TEST", "=========================")
                    Log.v("TEST", "requestId ${keyRequest.requestId}, for sessionId ${keyRequest.requestBody?.sessionId}")
                    Log.v("TEST", "replies -> ${keyRequest.results.joinToString { it.toString() }}")
                    Log.v("TEST", "=========================")
                }
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val ownDeviceReply =
                        outgoing?.results?.firstOrNull { it.userId == aliceSession.myUserId && it.fromDevice == aliceSession.sessionParams.deviceId }
                val result = ownDeviceReply?.result
                result != null && result is RequestResult.Success && result.chainIndex == 0
            }
        }

        // now the new session should be able to decrypt all!
        cryptoTestHelper.ensureCanDecrypt(
                sentEvents.map { it.eventId },
                aliceNewSession,
                testData.roomId,
                sentEvents.map { it.getLastMessageContent()!!.body }
        )

        // Additional test, can we check that bob replied successfully but with a ratcheted key
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val bobReply = outgoing?.results?.firstOrNull { it.userId == bobSession.myUserId }
                val result = bobReply?.result
                result != null && result is RequestResult.Success && result.chainIndex == 3
            }
        }

        commonTestHelper.signOutAndClose(aliceNewSession)
        commonTestHelper.signOutAndClose(aliceSession)
        commonTestHelper.signOutAndClose(bobSession)
    }

    /**
     * Tests that we don't cancel a request to early on first forward if the index is not good enough
     */
    @Test
    fun test_dontCancelToEarly() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!
        val roomFromBob = bobSession.getRoom(testData.roomId)!!

        val sentEvents = commonTestHelper.sendTextMessage(roomFromBob, "Hello", 3)
        val sentEventMegolmSession = sentEvents.first().root.content.toModel<EncryptedEventContent>()!!.sessionId!!

        // Let alice now add a new session
        val aliceNewSession = commonTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))

        // we wait bob first session to be aware of that session?
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val newSession = bobSession.cryptoService().getUserDevices(aliceSession.myUserId)
                        .firstOrNull { it.deviceId == aliceNewSession.sessionParams.deviceId }
                newSession != null
            }
        }

        val newEvent = commonTestHelper.sendTextMessage(roomFromBob, "The New", 1).first()
        val newEventId = newEvent.eventId
        val newEventText = newEvent.getLastMessageContent()!!.body

        // alice should be able to decrypt the new one
        cryptoTestHelper.ensureCanDecrypt(listOf(newEventId), aliceNewSession, testData.roomId, listOf(newEventText))
        // but not the first one!
        cryptoTestHelper.ensureCannotDecrypt(sentEvents.map { it.eventId }, aliceNewSession, testData.roomId)

        // All should be using the same session id
        sentEvents.forEach {
            assertEquals(sentEventMegolmSession, it.root.content.toModel<EncryptedEventContent>()!!.sessionId)
        }
        assertEquals(sentEventMegolmSession, newEvent.root.content.toModel<EncryptedEventContent>()!!.sessionId)

        // Mark the new session as verified
        aliceSession.cryptoService()
                .verificationService()
                .markedLocallyAsManuallyVerified(aliceNewSession.myUserId, aliceNewSession.sessionParams.deviceId!!)

        // /!\ Stop initial alice session syncing so that it can't reply
        aliceSession.cryptoService().enableKeyGossiping(false)
        aliceSession.syncService().stopSync()

        // Let's now try to request
        aliceNewSession.cryptoService().reRequestRoomKeyForEvent(sentEvents.first().root)

        // Should get a reply from bob and not from alice
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                //  Log.d("#TEST", "outgoing key requests :${aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().joinToString { it.sessionId ?: "?" }}")
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val bobReply = outgoing?.results?.firstOrNull { it.userId == bobSession.myUserId }
                val result = bobReply?.result
                result != null && result is RequestResult.Success && result.chainIndex == 3
            }
        }

        val outgoingReq = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }

        assertNull("We should not have a reply from first session", outgoingReq!!.results.firstOrNull { it.fromDevice == aliceSession.sessionParams.deviceId })
        assertEquals("The request should not be canceled", OutgoingRoomKeyRequestState.SENT, outgoingReq.state)

        // let's wake up alice
        aliceSession.cryptoService().enableKeyGossiping(true)
        aliceSession.syncService().startSync(true)

        // We should now get a reply from first session
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
                val ownDeviceReply =
                        outgoing?.results?.firstOrNull { it.userId == aliceSession.myUserId && it.fromDevice == aliceSession.sessionParams.deviceId }
                val result = ownDeviceReply?.result
                result != null && result is RequestResult.Success && result.chainIndex == 0
            }
        }

        // It should be in sent then cancel
        val outgoing = aliceNewSession.cryptoService().getOutgoingRoomKeyRequests().firstOrNull { it.sessionId == sentEventMegolmSession }
        assertEquals("The request should be canceled", OutgoingRoomKeyRequestState.SENT_THEN_CANCELED, outgoing!!.state)

        commonTestHelper.signOutAndClose(aliceNewSession)
        commonTestHelper.signOutAndClose(aliceSession)
        commonTestHelper.signOutAndClose(bobSession)
    }
}
