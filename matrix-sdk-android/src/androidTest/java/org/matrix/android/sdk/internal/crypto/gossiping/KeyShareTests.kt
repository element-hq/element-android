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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.internal.crypto.GossipingRequestState
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestState
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class KeyShareTests : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_DoNotSelfShareIfNotTrusted() {
        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        // Create an encrypted room and add a message
        val roomId = commonTestHelper.runBlockingTest {
            aliceSession.createRoom(
                    CreateRoomParams().apply {
                        visibility = RoomDirectoryVisibility.PRIVATE
                        enableEncryption()
                    }
            )
        }
        val room = aliceSession.getRoom(roomId)
        assertNotNull(room)
        Thread.sleep(4_000)
        assertTrue(room?.isEncrypted() == true)
        val sentEventId = commonTestHelper.sendTextMessage(room!!, "My Message", 1).first().eventId

        // Open a new sessionx

        val aliceSession2 = commonTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))

        val roomSecondSessionPOV = aliceSession2.getRoom(roomId)

        val receivedEvent = roomSecondSessionPOV?.getTimelineEvent(sentEventId)
        assertNotNull(receivedEvent)
        assert(receivedEvent!!.isEncrypted())

        try {
            commonTestHelper.runBlockingTest {
                aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            }
            fail("should fail")
        } catch (failure: Throwable) {
        }

        val outgoingRequestsBefore = aliceSession2.cryptoService().getOutgoingRoomKeyRequests()
        // Try to request
        aliceSession2.cryptoService().requestRoomKeyForEvent(receivedEvent.root)

        val eventMegolmSessionId = receivedEvent.root.content.toModel<EncryptedEventContent>()?.sessionId

        var outGoingRequestId: String? = null

        commonTestHelper.waitWithLatch {  latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession2.cryptoService().getOutgoingRoomKeyRequests()
                        .filter { req ->
                            // filter out request that was known before
                            !outgoingRequestsBefore.any { req.requestId == it.requestId }
                        }
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
                aliceSession.cryptoService().getIncomingRoomKeyRequests().let {
                    Log.v("TEST", "Incoming request Session 1 (looking for $outGoingRequestId)")
                    Log.v("TEST", "=========================")
                    it.forEach { keyRequest ->
                        Log.v("TEST", "[ts${keyRequest.localCreationTimestamp}] requestId ${keyRequest.requestId}, for sessionId ${keyRequest.requestBody?.sessionId} is ${keyRequest.state}")
                    }
                    Log.v("TEST", "=========================")
                }

                val incoming = aliceSession.cryptoService().getIncomingRoomKeyRequests().firstOrNull { it.requestId == outGoingRequestId }
                incoming?.state == GossipingRequestState.REJECTED
            }
        }

        try {
            commonTestHelper.runBlockingTest {
                aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            }
            fail("should fail")
        } catch (failure: Throwable) {
        }

        // Mark the device as trusted
        aliceSession.cryptoService().setDeviceVerification(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true), aliceSession.myUserId,
                aliceSession2.sessionParams.deviceId ?: "")

        // Re request
        aliceSession2.cryptoService().reRequestRoomKeyForEvent(receivedEvent.root)

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession.cryptoService().getIncomingRoomKeyRequests().let {
                    Log.v("TEST", "Incoming request Session 1")
                    Log.v("TEST", "=========================")
                    it.forEach {
                        Log.v("TEST", "requestId ${it.requestId}, for sessionId ${it.requestBody?.sessionId} is ${it.state}")
                    }
                    Log.v("TEST", "=========================")

                    it.any { it.requestBody?.sessionId == eventMegolmSessionId && it.state == GossipingRequestState.ACCEPTED }
                }
            }
        }

        Thread.sleep(6_000)
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession2.cryptoService().getOutgoingRoomKeyRequests().let {
                    it.any { it.requestBody?.sessionId == eventMegolmSessionId && it.state == OutgoingGossipingRequestState.CANCELLED }
                }
            }
        }

        try {
            commonTestHelper.runBlockingTest {
                aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            }
        } catch (failure: Throwable) {
            fail("should have been able to decrypt")
        }

        commonTestHelper.signOutAndClose(aliceSession)
        commonTestHelper.signOutAndClose(aliceSession2)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_ShareSSSSSecret() {
        val aliceSession1 = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        commonTestHelper.doSync<Unit> {
            aliceSession1.cryptoService().crossSigningService()
                    .initializeCrossSigning(
                            object : UserInteractiveAuthInterceptor {
                                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                    promise.resume(
                                            UserPasswordAuth(
                                                    user = aliceSession1.myUserId,
                                                    password = TestConstants.PASSWORD
                                            )
                                    )
                                }
                            }, it)
        }

        // Also bootstrap keybackup on first session
        val creationInfo = commonTestHelper.doSync<MegolmBackupCreationInfo> {
            aliceSession1.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, it)
        }
        val version = commonTestHelper.doSync<KeysVersion> {
            aliceSession1.cryptoService().keysBackupService().createKeysBackupVersion(creationInfo, it)
        }
        // Save it for gossiping
        aliceSession1.cryptoService().keysBackupService().saveBackupRecoveryKey(creationInfo.recoveryKey, version = version.version)

        val aliceSession2 = commonTestHelper.logIntoAccount(aliceSession1.myUserId, SessionTestParams(true))

        val aliceVerificationService1 = aliceSession1.cryptoService().verificationService()
        val aliceVerificationService2 = aliceSession2.cryptoService().verificationService()

        // force keys download
        commonTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            aliceSession1.cryptoService().downloadKeys(listOf(aliceSession1.myUserId), true, it)
        }
        commonTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            aliceSession2.cryptoService().downloadKeys(listOf(aliceSession2.myUserId), true, it)
        }

        var session1ShortCode: String? = null
        var session2ShortCode: String? = null

        aliceVerificationService1.addListener(object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                Log.d("#TEST", "AA: tx incoming?:${tx.isIncoming} state ${tx.state}")
                if (tx is SasVerificationTransaction) {
                    if (tx.state == VerificationTxState.OnStarted) {
                        (tx as IncomingSasVerificationTransaction).performAccept()
                    }
                    if (tx.state == VerificationTxState.ShortCodeReady) {
                        session1ShortCode = tx.getDecimalCodeRepresentation()
                        Thread.sleep(500)
                        tx.userHasVerifiedShortCode()
                    }
                }
            }
        })

        aliceVerificationService2.addListener(object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                Log.d("#TEST", "BB: tx incoming?:${tx.isIncoming} state ${tx.state}")
                if (tx is SasVerificationTransaction) {
                    if (tx.state == VerificationTxState.ShortCodeReady) {
                        session2ShortCode = tx.getDecimalCodeRepresentation()
                        Thread.sleep(500)
                        tx.userHasVerifiedShortCode()
                    }
                }
            }
        })

        val txId = "m.testVerif12"
        aliceVerificationService2.beginKeyVerification(VerificationMethod.SAS, aliceSession1.myUserId, aliceSession1.sessionParams.deviceId
                ?: "", txId)

        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession1.cryptoService().getDeviceInfo(aliceSession1.myUserId, aliceSession2.sessionParams.deviceId ?: "")?.isVerified == true
            }
        }

        assertNotNull(session1ShortCode)
        Log.d("#TEST", "session1ShortCode: $session1ShortCode")
        assertNotNull(session2ShortCode)
        Log.d("#TEST", "session2ShortCode: $session2ShortCode")
        assertEquals(session1ShortCode, session2ShortCode)

        // SSK and USK private keys should have been shared

        commonTestHelper.waitWithLatch(60_000) { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                Log.d("#TEST", "CAN XS :${aliceSession2.cryptoService().crossSigningService().getMyCrossSigningKeys()}")
                aliceSession2.cryptoService().crossSigningService().canCrossSign()
            }
        }

        // Test that key backup key has been shared to
        commonTestHelper.waitWithLatch(60_000) { latch ->
            val keysBackupService = aliceSession2.cryptoService().keysBackupService()
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                Log.d("#TEST", "Recovery :${keysBackupService.getKeyBackupRecoveryKeyInfo()?.recoveryKey}")
                keysBackupService.getKeyBackupRecoveryKeyInfo()?.recoveryKey == creationInfo.recoveryKey
            }
        }

        commonTestHelper.signOutAndClose(aliceSession1)
        commonTestHelper.signOutAndClose(aliceSession2)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_ImproperKeyShareBug() {
        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        commonTestHelper.doSync<Unit> {
            aliceSession.cryptoService().crossSigningService()
                    .initializeCrossSigning(
                            object : UserInteractiveAuthInterceptor {
                                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                    promise.resume(
                                            UserPasswordAuth(
                                                    user = aliceSession.myUserId,
                                                    password = TestConstants.PASSWORD,
                                                    session = flowResponse.session
                                            )
                                    )
                                }
                            }, it)
        }

        // Create an encrypted room and send a couple of messages
        val roomId = commonTestHelper.runBlockingTest {
            aliceSession.createRoom(
                    CreateRoomParams().apply {
                        visibility = RoomDirectoryVisibility.PRIVATE
                        enableEncryption()
                    }
            )
        }
        val roomAlicePov = aliceSession.getRoom(roomId)
        assertNotNull(roomAlicePov)
        Thread.sleep(1_000)
        assertTrue(roomAlicePov?.isEncrypted() == true)
        val secondEventId = commonTestHelper.sendTextMessage(roomAlicePov!!, "Message", 3)[1].eventId

        // Create bob session

        val bobSession = commonTestHelper.createAccount(TestConstants.USER_BOB, SessionTestParams(true))
        commonTestHelper.doSync<Unit> {
            bobSession.cryptoService().crossSigningService()
                    .initializeCrossSigning(
                            object : UserInteractiveAuthInterceptor {
                                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                    promise.resume(
                                            UserPasswordAuth(
                                                    user = bobSession.myUserId,
                                                    password = TestConstants.PASSWORD,
                                                    session = flowResponse.session
                                            )
                                    )
                                }
                            }, it)
        }

        // Let alice invite bob
        commonTestHelper.runBlockingTest {
            roomAlicePov.invite(bobSession.myUserId, null)
        }

        commonTestHelper.runBlockingTest {
            bobSession.joinRoom(roomAlicePov.roomId, null, emptyList())
        }

        // we want to discard alice outbound session
        aliceSession.cryptoService().discardOutboundSession(roomAlicePov.roomId)

        // and now resend a new message to reset index to 0
        commonTestHelper.sendTextMessage(roomAlicePov, "After", 1)

        val roomRoomBobPov = aliceSession.getRoom(roomId)
        val beforeJoin = roomRoomBobPov!!.getTimelineEvent(secondEventId)

        var dRes = tryOrNull {
            commonTestHelper.runBlockingTest {
                bobSession.cryptoService().decryptEvent(beforeJoin!!.root, "")
            }
        }

        assert(dRes == null)

        // Try to re-ask the keys

        bobSession.cryptoService().reRequestRoomKeyForEvent(beforeJoin!!.root)

        Thread.sleep(3_000)

        // With the bug the first session would have improperly reshare that key :/
        dRes = tryOrNull {
            commonTestHelper.runBlockingTest {
                bobSession.cryptoService().decryptEvent(beforeJoin.root, "")
            }
        }
        Log.d("#TEST", "KS: sgould not decrypt that ${beforeJoin.root.getClearContent().toModel<MessageContent>()?.body}")
        assert(dRes?.clearEvent == null)
    }
}
