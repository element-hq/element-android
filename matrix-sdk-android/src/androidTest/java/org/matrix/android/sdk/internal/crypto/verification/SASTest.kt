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

package org.matrix.android.sdk.internal.crypto.verification

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.OutgoingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasMode
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.model.rest.toValue
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SASTest : InstrumentedTest {

    @Test
    fun test_aliceStartThenAliceCancel() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession!!.cryptoService().verificationService()

        val bobTxCreatedLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                bobTxCreatedLatch.countDown()
            }
        }
        bobVerificationService.addListener(bobListener)

        val txID = aliceVerificationService.beginKeyVerification(VerificationMethod.SAS,
                bobSession.myUserId,
                bobSession.cryptoService().getMyDevice().deviceId,
                null)
        assertNotNull("Alice should have a started transaction", txID)

        val aliceKeyTx = aliceVerificationService.getExistingTransaction(bobSession.myUserId, txID!!)
        assertNotNull("Alice should have a started transaction", aliceKeyTx)

        testHelper.await(bobTxCreatedLatch)
        bobVerificationService.removeListener(bobListener)

        val bobKeyTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, txID)

        assertNotNull("Bob should have started verif transaction", bobKeyTx)
        assertTrue(bobKeyTx is SASDefaultVerificationTransaction)
        assertNotNull("Bob should have starting a SAS transaction", bobKeyTx)
        assertTrue(aliceKeyTx is SASDefaultVerificationTransaction)
        assertEquals("Alice and Bob have same transaction id", aliceKeyTx!!.transactionId, bobKeyTx!!.transactionId)

        val aliceSasTx = aliceKeyTx as SASDefaultVerificationTransaction?
        val bobSasTx = bobKeyTx as SASDefaultVerificationTransaction?

        assertEquals("Alice state should be started", VerificationTxState.Started, aliceSasTx!!.state)
        assertEquals("Bob state should be started by alice", VerificationTxState.OnStarted, bobSasTx!!.state)

        // Let's cancel from alice side
        val cancelLatch = CountDownLatch(1)

        val bobListener2 = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == txID) {
                    val immutableState = (tx as SASDefaultVerificationTransaction).state
                    if (immutableState is VerificationTxState.Cancelled && !immutableState.byMe) {
                        cancelLatch.countDown()
                    }
                }
            }
        }
        bobVerificationService.addListener(bobListener2)

        aliceSasTx.cancel(CancelCode.User)
        testHelper.await(cancelLatch)

        assertTrue("Should be cancelled on alice side", aliceSasTx.state is VerificationTxState.Cancelled)
        assertTrue("Should be cancelled on bob side", bobSasTx.state is VerificationTxState.Cancelled)

        val aliceCancelState = aliceSasTx.state as VerificationTxState.Cancelled
        val bobCancelState = bobSasTx.state as VerificationTxState.Cancelled

        assertTrue("Should be cancelled by me on alice side", aliceCancelState.byMe)
        assertFalse("Should be cancelled by other on bob side", bobCancelState.byMe)

        assertEquals("Should be User cancelled on alice side", CancelCode.User, aliceCancelState.cancelCode)
        assertEquals("Should be User cancelled on bob side", CancelCode.User, bobCancelState.cancelCode)

        assertNull(bobVerificationService.getExistingTransaction(aliceSession.myUserId, txID))
        assertNull(aliceVerificationService.getExistingTransaction(bobSession.myUserId, txID))

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_key_agreement_protocols_must_include_curve25519() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        fail("Not passing for the moment")
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession!!

        val protocols = listOf("meh_dont_know")
        val tid = "00000000"

        // Bob should receive a cancel
        var cancelReason: CancelCode? = null
        val cancelLatch = CountDownLatch(1)

        val bobListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == tid && tx.state is VerificationTxState.Cancelled) {
                    cancelReason = (tx.state as VerificationTxState.Cancelled).cancelCode
                    cancelLatch.countDown()
                }
            }
        }
        bobSession.cryptoService().verificationService().addListener(bobListener)

        // TODO bobSession!!.dataHandler.addListener(object : MXEventListener() {
        // TODO     override fun onToDeviceEvent(event: Event?) {
        // TODO         if (event!!.getType() == CryptoEvent.EVENT_TYPE_KEY_VERIFICATION_CANCEL) {
        // TODO             if (event.contentAsJsonObject?.get("transaction_id")?.asString == tid) {
        // TODO                 canceledToDeviceEvent = event
        // TODO                 cancelLatch.countDown()
        // TODO             }
        // TODO         }
        // TODO     }
        // TODO })

        val aliceSession = cryptoTestData.firstSession
        val aliceUserID = aliceSession.myUserId
        val aliceDevice = aliceSession.cryptoService().getMyDevice().deviceId

        val aliceListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as IncomingSasVerificationTransaction).uxState === IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT) {
                    (tx as IncomingSasVerificationTransaction).performAccept()
                }
            }
        }
        aliceSession.cryptoService().verificationService().addListener(aliceListener)

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, protocols = protocols)

        testHelper.await(cancelLatch)

        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod, cancelReason)

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_key_agreement_macs_Must_include_hmac_sha256() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        fail("Not passing for the moment")
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession!!

        val mac = listOf("shaBit")
        val tid = "00000000"

        // Bob should receive a cancel
        var canceledToDeviceEvent: Event? = null
        val cancelLatch = CountDownLatch(1)
        // TODO bobSession!!.dataHandler.addListener(object : MXEventListener() {
        // TODO     override fun onToDeviceEvent(event: Event?) {
        // TODO         if (event!!.getType() == CryptoEvent.EVENT_TYPE_KEY_VERIFICATION_CANCEL) {
        // TODO             if (event.contentAsJsonObject?.get("transaction_id")?.asString == tid) {
        // TODO                 canceledToDeviceEvent = event
        // TODO                 cancelLatch.countDown()
        // TODO             }
        // TODO         }
        // TODO     }
        // TODO })

        val aliceSession = cryptoTestData.firstSession
        val aliceUserID = aliceSession.myUserId
        val aliceDevice = aliceSession.cryptoService().getMyDevice().deviceId

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, mac = mac)

        testHelper.await(cancelLatch)

        val cancelReq = canceledToDeviceEvent!!.content.toModel<KeyVerificationCancel>()!!
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_key_agreement_short_code_include_decimal() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        fail("Not passing for the moment")
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession!!

        val codes = listOf("bin", "foo", "bar")
        val tid = "00000000"

        // Bob should receive a cancel
        var canceledToDeviceEvent: Event? = null
        val cancelLatch = CountDownLatch(1)
        // TODO bobSession!!.dataHandler.addListener(object : MXEventListener() {
        // TODO     override fun onToDeviceEvent(event: Event?) {
        // TODO         if (event!!.getType() == CryptoEvent.EVENT_TYPE_KEY_VERIFICATION_CANCEL) {
        // TODO             if (event.contentAsJsonObject?.get("transaction_id")?.asString == tid) {
        // TODO                 canceledToDeviceEvent = event
        // TODO                 cancelLatch.countDown()
        // TODO             }
        // TODO         }
        // TODO     }
        // TODO })

        val aliceSession = cryptoTestData.firstSession
        val aliceUserID = aliceSession.myUserId
        val aliceDevice = aliceSession.cryptoService().getMyDevice().deviceId

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, codes = codes)

        testHelper.await(cancelLatch)

        val cancelReq = canceledToDeviceEvent!!.content.toModel<KeyVerificationCancel>()!!
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.cleanUp(testHelper)
    }

    private fun fakeBobStart(bobSession: Session,
                             aliceUserID: String?,
                             aliceDevice: String?,
                             tid: String,
                             protocols: List<String> = SASDefaultVerificationTransaction.KNOWN_AGREEMENT_PROTOCOLS,
                             hashes: List<String> = SASDefaultVerificationTransaction.KNOWN_HASHES,
                             mac: List<String> = SASDefaultVerificationTransaction.KNOWN_MACS,
                             codes: List<String> = SASDefaultVerificationTransaction.KNOWN_SHORT_CODES) {
        val startMessage = KeyVerificationStart(
                fromDevice = bobSession.cryptoService().getMyDevice().deviceId,
                method = VerificationMethod.SAS.toValue(),
                transactionId = tid,
                keyAgreementProtocols = protocols,
                hashes = hashes,
                messageAuthenticationCodes = mac,
                shortAuthenticationStrings = codes
        )

        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(aliceUserID, aliceDevice, startMessage)

        // TODO val sendLatch = CountDownLatch(1)
        // TODO bobSession.cryptoRestClient.sendToDevice(
        // TODO         EventType.KEY_VERIFICATION_START,
        // TODO         contentMap,
        // TODO         tid,
        // TODO         TestMatrixCallback<Void>(sendLatch)
        // TODO )
    }

    // any two devices may only have at most one key verification in flight at a time.
    // If a device has two verifications in progress with the same device, then it should cancel both verifications.
    @Test
    fun test_aliceStartTwoRequests() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()

        val aliceCreatedLatch = CountDownLatch(2)
        val aliceCancelledLatch = CountDownLatch(2)
        val createdTx = mutableListOf<SASDefaultVerificationTransaction>()
        val aliceListener = object : VerificationService.Listener {
            override fun transactionCreated(tx: VerificationTransaction) {
                createdTx.add(tx as SASDefaultVerificationTransaction)
                aliceCreatedLatch.countDown()
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as SASDefaultVerificationTransaction).state is VerificationTxState.Cancelled && !(tx.state as VerificationTxState.Cancelled).byMe) {
                    aliceCancelledLatch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobUserId = bobSession!!.myUserId
        val bobDeviceId = bobSession.cryptoService().getMyDevice().deviceId
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)

        testHelper.await(aliceCreatedLatch)
        testHelper.await(aliceCancelledLatch)

        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * Test that when alice starts a 'correct' request, bob agrees.
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_aliceAndBobAgreement() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession!!.cryptoService().verificationService()

        var accepted: ValidVerificationInfoAccept? = null
        var startReq: ValidVerificationInfoStart.SasVerificationInfoStart? = null

        val aliceAcceptedLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                Log.v("TEST", "== aliceTx state ${tx.state} => ${(tx as? OutgoingSasVerificationTransaction)?.uxState}")
                if ((tx as SASDefaultVerificationTransaction).state === VerificationTxState.OnAccepted) {
                    val at = tx as SASDefaultVerificationTransaction
                    accepted = at.accepted
                    startReq = at.startReq
                    aliceAcceptedLatch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                Log.v("TEST", "== bobTx state ${tx.state} => ${(tx as? IncomingSasVerificationTransaction)?.uxState}")
                if ((tx as IncomingSasVerificationTransaction).uxState === IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT) {
                    bobVerificationService.removeListener(this)
                    val at = tx as IncomingSasVerificationTransaction
                    at.performAccept()
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.cryptoService().getMyDevice().deviceId
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        testHelper.await(aliceAcceptedLatch)

        assertTrue("Should have receive a commitment", accepted!!.commitment?.trim()?.isEmpty() == false)

        // check that agreement is valid
        assertTrue("Agreed Protocol should be Valid", accepted != null)
        assertTrue("Agreed Protocol should be known by alice", startReq!!.keyAgreementProtocols.contains(accepted!!.keyAgreementProtocol))
        assertTrue("Hash should be known by alice", startReq!!.hashes.contains(accepted!!.hash))
        assertTrue("Hash should be known by alice", startReq!!.messageAuthenticationCodes.contains(accepted!!.messageAuthenticationCode))

        accepted!!.shortAuthenticationStrings.forEach {
            assertTrue("all agreed Short Code should be known by alice", startReq!!.shortAuthenticationStrings.contains(it))
        }

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    fun test_aliceAndBobSASCode() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession!!.cryptoService().verificationService()

        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as OutgoingSasVerificationTransaction).uxState
                when (uxState) {
                    OutgoingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        aliceSASLatch.countDown()
                    }
                    else                                                -> Unit
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as IncomingSasVerificationTransaction).uxState
                when (uxState) {
                    IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT -> {
                        tx.performAccept()
                    }
                    else                                                   -> Unit
                }
                if (uxState === IncomingSasVerificationTransaction.UxState.SHOW_SAS) {
                    bobSASLatch.countDown()
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.cryptoService().getMyDevice().deviceId
        val verificationSAS = aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        testHelper.await(aliceSASLatch)
        testHelper.await(bobSASLatch)

        val aliceTx = aliceVerificationService.getExistingTransaction(bobUserId, verificationSAS!!) as SASDefaultVerificationTransaction
        val bobTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, verificationSAS) as SASDefaultVerificationTransaction

        assertEquals("Should have same SAS", aliceTx.getShortCodeRepresentation(SasMode.DECIMAL),
                bobTx.getShortCodeRepresentation(SasMode.DECIMAL))

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    fun test_happyPath() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession!!.cryptoService().verificationService()

        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.Listener {
            var matchOnce = true
            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as OutgoingSasVerificationTransaction).uxState
                Log.v("TEST", "== aliceState ${uxState.name}")
                when (uxState) {
                    OutgoingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        tx.userHasVerifiedShortCode()
                    }
                    OutgoingSasVerificationTransaction.UxState.VERIFIED -> {
                        if (matchOnce) {
                            matchOnce = false
                            aliceSASLatch.countDown()
                        }
                    }
                    else                                                -> Unit
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.Listener {
            var acceptOnce = true
            var matchOnce = true
            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as IncomingSasVerificationTransaction).uxState
                Log.v("TEST", "== bobState ${uxState.name}")
                when (uxState) {
                    IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT -> {
                        if (acceptOnce) {
                            acceptOnce = false
                            tx.performAccept()
                        }
                    }
                    IncomingSasVerificationTransaction.UxState.SHOW_SAS    -> {
                        if (matchOnce) {
                            matchOnce = false
                            tx.userHasVerifiedShortCode()
                        }
                    }
                    IncomingSasVerificationTransaction.UxState.VERIFIED    -> {
                        bobSASLatch.countDown()
                    }
                    else                                                   -> Unit
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.cryptoService().getMyDevice().deviceId
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        testHelper.await(aliceSASLatch)
        testHelper.await(bobSASLatch)

        // Assert that devices are verified
        val bobDeviceInfoFromAlicePOV: CryptoDeviceInfo? = aliceSession.cryptoService().getDeviceInfo(bobUserId, bobDeviceId)
        val aliceDeviceInfoFromBobPOV: CryptoDeviceInfo? = bobSession.cryptoService().getDeviceInfo(aliceSession.myUserId, aliceSession.cryptoService().getMyDevice().deviceId)

        assertTrue("alice device should be verified from bob point of view", aliceDeviceInfoFromBobPOV!!.isVerified)
        assertTrue("bob device should be verified from alice point of view", bobDeviceInfoFromAlicePOV!!.isVerified)
        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    fun test_ConcurrentStart() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession!!.cryptoService().verificationService()

        val req = aliceVerificationService.requestKeyVerificationInDMs(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                bobSession.myUserId,
                cryptoTestData.roomId
        )

        var requestID: String? = null

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val prAlicePOV = aliceVerificationService.getExistingVerificationRequests(bobSession.myUserId).firstOrNull()
                requestID = prAlicePOV?.transactionId
                Log.v("TEST", "== alicePOV is $prAlicePOV")
                prAlicePOV?.transactionId != null && prAlicePOV.localId == req.localId
            }
        }

        Log.v("TEST", "== requestID is $requestID")

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val prBobPOV = bobVerificationService.getExistingVerificationRequests(aliceSession.myUserId).firstOrNull()
                Log.v("TEST", "== prBobPOV is $prBobPOV")
                prBobPOV?.transactionId == requestID
            }
        }

        bobVerificationService.readyPendingVerification(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                aliceSession.myUserId,
                requestID!!
        )

        // wait for alice to get the ready
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val prAlicePOV = aliceVerificationService.getExistingVerificationRequests(bobSession.myUserId).firstOrNull()
                Log.v("TEST", "== prAlicePOV is $prAlicePOV")
                prAlicePOV?.transactionId == requestID && prAlicePOV?.isReady != null
            }
        }

        // Start concurrent!
        aliceVerificationService.beginKeyVerificationInDMs(
                VerificationMethod.SAS,
                requestID!!,
                cryptoTestData.roomId,
                bobSession.myUserId,
                bobSession.sessionParams.deviceId!!)

        bobVerificationService.beginKeyVerificationInDMs(
                VerificationMethod.SAS,
                requestID!!,
                cryptoTestData.roomId,
                aliceSession.myUserId,
                aliceSession.sessionParams.deviceId!!)

        // we should reach SHOW SAS on both
        var alicePovTx: SasVerificationTransaction?
        var bobPovTx: SasVerificationTransaction?

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                alicePovTx = aliceVerificationService.getExistingTransaction(bobSession.myUserId, requestID!!) as? SasVerificationTransaction
                Log.v("TEST", "== alicePovTx is $alicePovTx")
                alicePovTx?.state == VerificationTxState.ShortCodeReady
            }
        }
        // wait for alice to get the ready
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                bobPovTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, requestID!!) as? SasVerificationTransaction
                Log.v("TEST", "== bobPovTx is $bobPovTx")
                bobPovTx?.state == VerificationTxState.ShortCodeReady
            }
        }

        cryptoTestData.cleanUp(testHelper)
    }
}
