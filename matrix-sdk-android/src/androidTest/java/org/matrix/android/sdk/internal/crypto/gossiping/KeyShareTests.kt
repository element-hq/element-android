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
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
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
import org.matrix.android.sdk.internal.crypto.model.rest.UserPasswordAuth
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class KeyShareTests : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_DoNotSelfShareIfNotTrusted() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        // Create an encrypted room and add a message
        val roomId = mTestHelper.doSync<String> {
            aliceSession.createRoom(
                    CreateRoomParams().apply {
                        visibility = RoomDirectoryVisibility.PRIVATE
                        enableEncryption()
                    },
                    it
            )
        }
        val room = aliceSession.getRoom(roomId)
        assertNotNull(room)
        Thread.sleep(4_000)
        assertTrue(room?.isEncrypted() == true)
        val sentEventId = mTestHelper.sendTextMessage(room!!, "My Message", 1).first().eventId

        // Open a new sessionx

        val aliceSession2 = mTestHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))

        val roomSecondSessionPOV = aliceSession2.getRoom(roomId)

        val receivedEvent = roomSecondSessionPOV?.getTimeLineEvent(sentEventId)
        assertNotNull(receivedEvent)
        assert(receivedEvent!!.isEncrypted())

        try {
            aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            fail("should fail")
        } catch (failure: Throwable) {
        }

        val outgoingRequestsBefore = aliceSession2.cryptoService().getOutgoingRoomKeyRequests()
        // Try to request
        aliceSession2.cryptoService().requestRoomKeyForEvent(receivedEvent.root)

        val waitLatch = CountDownLatch(1)
        val eventMegolmSessionId = receivedEvent.root.content.toModel<EncryptedEventContent>()?.sessionId

        var outGoingRequestId: String? = null

        mTestHelper.retryPeriodicallyWithLatch(waitLatch) {
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
        mTestHelper.await(waitLatch)

        Log.v("TEST", "=======> Outgoing requet Id is $outGoingRequestId")

        val outgoingRequestAfter = aliceSession2.cryptoService().getOutgoingRoomKeyRequests()

        // We should have a new request
        Assert.assertTrue(outgoingRequestAfter.size > outgoingRequestsBefore.size)
        Assert.assertNotNull(outgoingRequestAfter.first { it.sessionId == eventMegolmSessionId })

        // The first session should see an incoming request
        // the request should be refused, because the device is not trusted
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
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
            aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
            fail("should fail")
        } catch (failure: Throwable) {
        }

        // Mark the device as trusted
        aliceSession.cryptoService().setDeviceVerification(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true), aliceSession.myUserId,
                aliceSession2.sessionParams.deviceId ?: "")

        // Re request
        aliceSession2.cryptoService().reRequestRoomKeyForEvent(receivedEvent.root)

        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
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
        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession2.cryptoService().getOutgoingRoomKeyRequests().let {
                    it.any { it.requestBody?.sessionId == eventMegolmSessionId && it.state == OutgoingGossipingRequestState.CANCELLED }
                }
            }
        }

        try {
            aliceSession2.cryptoService().decryptEvent(receivedEvent.root, "foo")
        } catch (failure: Throwable) {
            fail("should have been able to decrypt")
        }

        mTestHelper.signOutAndClose(aliceSession)
        mTestHelper.signOutAndClose(aliceSession2)
    }

    @Test
    fun test_ShareSSSSSecret() {
        val aliceSession1 = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        mTestHelper.doSync<Unit> {
            aliceSession1.cryptoService().crossSigningService()
                    .initializeCrossSigning(UserPasswordAuth(
                            user = aliceSession1.myUserId,
                            password = TestConstants.PASSWORD
                    ), it)
        }

        // Also bootstrap keybackup on first session
        val creationInfo = mTestHelper.doSync<MegolmBackupCreationInfo> {
            aliceSession1.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, it)
        }
        val version = mTestHelper.doSync<KeysVersion> {
            aliceSession1.cryptoService().keysBackupService().createKeysBackupVersion(creationInfo, it)
        }
        // Save it for gossiping
        aliceSession1.cryptoService().keysBackupService().saveBackupRecoveryKey(creationInfo.recoveryKey, version = version.version)

        val aliceSession2 = mTestHelper.logIntoAccount(aliceSession1.myUserId, SessionTestParams(true))

        val aliceVerificationService1 = aliceSession1.cryptoService().verificationService()
        val aliceVerificationService2 = aliceSession2.cryptoService().verificationService()

        // force keys download
        mTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            aliceSession1.cryptoService().downloadKeys(listOf(aliceSession1.myUserId), true, it)
        }
        mTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
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

        mTestHelper.waitWithLatch { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                aliceSession1.cryptoService().getDeviceInfo(aliceSession1.myUserId, aliceSession2.sessionParams.deviceId ?: "")?.isVerified == true
            }
        }

        assertNotNull(session1ShortCode)
        Log.d("#TEST", "session1ShortCode: $session1ShortCode")
        assertNotNull(session2ShortCode)
        Log.d("#TEST", "session2ShortCode: $session2ShortCode")
        assertEquals(session1ShortCode, session2ShortCode)

        // SSK and USK private keys should have been shared

        mTestHelper.waitWithLatch(60_000) { latch ->
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                Log.d("#TEST", "CAN XS :${aliceSession2.cryptoService().crossSigningService().getMyCrossSigningKeys()}")
                aliceSession2.cryptoService().crossSigningService().canCrossSign()
            }
        }

        // Test that key backup key has been shared to
        mTestHelper.waitWithLatch(60_000) { latch ->
            val keysBackupService = aliceSession2.cryptoService().keysBackupService()
            mTestHelper.retryPeriodicallyWithLatch(latch) {
                Log.d("#TEST", "Recovery :${keysBackupService.getKeyBackupRecoveryKeyInfo()?.recoveryKey}")
                keysBackupService.getKeyBackupRecoveryKeyInfo()?.recoveryKey == creationInfo.recoveryKey
            }
        }

        mTestHelper.signOutAndClose(aliceSession1)
        mTestHelper.signOutAndClose(aliceSession2)
    }
}
