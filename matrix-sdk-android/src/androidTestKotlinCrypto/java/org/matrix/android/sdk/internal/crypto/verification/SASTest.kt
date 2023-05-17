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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.dbgState
import org.matrix.android.sdk.api.session.crypto.verification.getTransaction
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SASTest : InstrumentedTest {

    val scope = CoroutineScope(SupervisorJob())

    @Test
    fun test_aliceStartThenAliceCancel() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

        Log.d("#E2E", "verification: doE2ETestWithAliceAndBobInARoom")
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
        Log.d("#E2E", "verification: initializeCrossSigning")
        cryptoTestData.initializeCrossSigning(cryptoTestHelper)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession!!.cryptoService().verificationService()

        Log.d("#E2E", "verification: requestVerificationAndWaitForReadyState")
        val txId = SasVerificationTestHelper(testHelper)
                .requestVerificationAndWaitForReadyState(scope, cryptoTestData, listOf(VerificationMethod.SAS))

        Log.d("#E2E", "verification: startKeyVerification")
        aliceVerificationService.startKeyVerification(
                VerificationMethod.SAS,
                bobSession.myUserId,
                txId
        )

        Log.d("#E2E", "verification: ensure bob has received start")
        testHelper.retryWithBackoff {
            Log.d("#E2E", "verification: ${bobVerificationService.getExistingVerificationRequest(aliceSession.myUserId, txId)?.state}")
            bobVerificationService.getExistingVerificationRequest(aliceSession.myUserId, txId)?.state == EVerificationState.Started
        }

        val bobKeyTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, txId)

        assertNotNull("Bob should have started verif transaction", bobKeyTx)
        assertTrue(bobKeyTx is SasVerificationTransaction)

        val aliceKeyTx = aliceVerificationService.getExistingTransaction(bobSession.myUserId, txId)
        assertTrue(aliceKeyTx is SasVerificationTransaction)

        assertEquals("Alice and Bob have same transaction id", aliceKeyTx!!.transactionId, bobKeyTx!!.transactionId)

        val aliceCancelled = CompletableDeferred<SasTransactionState.Cancelled>()
        aliceVerificationService.requestEventFlow().onEach {
            Log.d("#E2E", "alice flow event $it | ${it.getTransaction()?.dbgState()}")
            val tx = it.getTransaction()
            if (tx?.transactionId == txId && tx is SasVerificationTransaction) {
                if (tx.state() is SasTransactionState.Cancelled) {
                    aliceCancelled.complete(tx.state() as SasTransactionState.Cancelled)
                }
            }
        }.launchIn(scope)

        val bobCancelled = CompletableDeferred<SasTransactionState.Cancelled>()
        bobVerificationService.requestEventFlow().onEach {
            Log.d("#E2E", "bob flow event $it | ${it.getTransaction()?.dbgState()}")
            val tx = it.getTransaction()
            if (tx?.transactionId == txId && tx is SasVerificationTransaction) {
                if (tx.state() is SasTransactionState.Cancelled) {
                    bobCancelled.complete(tx.state() as SasTransactionState.Cancelled)
                }
            }
        }.launchIn(scope)

        aliceVerificationService.cancelVerificationRequest(bobSession.myUserId, txId)

        val cancelledAlice = aliceCancelled.await()
        val cancelledBob = bobCancelled.await()

        assertEquals("Should be User cancelled on alice side", CancelCode.User, cancelledAlice.cancelCode)
        assertEquals("Should be User cancelled on bob side", CancelCode.User, cancelledBob.cancelCode)

        assertNull(bobVerificationService.getExistingTransaction(aliceSession.myUserId, txId))
        assertNull(aliceVerificationService.getExistingTransaction(bobSession.myUserId, txId))
    }

    /*
@Test
@Ignore("This test will be ignored until it is fixed")
fun test_key_agreement_protocols_must_include_curve25519() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
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
            tx as SasVerificationTransaction
            if (tx.transactionId == tid && tx.state() is SasTransactionState.Cancelled) {
                cancelReason = (tx.state() as SasTransactionState.Cancelled).cancelCode
                cancelLatch.countDown()
            }
        }
    }
//        bobSession.cryptoService().verificationService().addListener(bobListener)

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
    val aliceDevice = aliceSession.cryptoService().getMyCryptoDevice().deviceId

    val aliceListener = object : VerificationService.Listener {
        override fun transactionUpdated(tx: VerificationTransaction) {
            tx as SasVerificationTransaction
            if (tx.state() is SasTransactionState.SasStarted) {
                runBlocking {
                    tx.acceptVerification()
                }
            }
        }
    }
//        aliceSession.cryptoService().verificationService().addListener(aliceListener)

    fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, protocols = protocols)

    testHelper.await(cancelLatch)

    assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod, cancelReason)
}

@Test
@Ignore("This test will be ignored until it is fixed")
fun test_key_agreement_macs_Must_include_hmac_sha256() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
    fail("Not passing for the moment")
    val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

    val bobSession = cryptoTestData.secondSession!!

    val mac = listOf("shaBit")
    val tid = "00000000"

    // Bob should receive a cancel
    val canceledToDeviceEvent: Event? = null
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
    val aliceDevice = aliceSession.cryptoService().getMyCryptoDevice().deviceId

    fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, mac = mac)

    testHelper.await(cancelLatch)
    val cancelReq = canceledToDeviceEvent!!.content.toModel<KeyVerificationCancel>()!!
    assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)
}

@Test
@Ignore("This test will be ignored until it is fixed")
fun test_key_agreement_short_code_include_decimal() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
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
    val aliceDevice = aliceSession.cryptoService().getMyCryptoDevice().deviceId

    fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, codes = codes)

    testHelper.await(cancelLatch)

    val cancelReq = canceledToDeviceEvent!!.content.toModel<KeyVerificationCancel>()!!
    assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)
}

private suspend fun fakeBobStart(
        bobSession: Session,
        aliceUserID: String?,
        aliceDevice: String?,
        tid: String,
        protocols: List<String> = SasVerificationTransaction.KNOWN_AGREEMENT_PROTOCOLS,
        hashes: List<String> = SasVerificationTransaction.KNOWN_HASHES,
        mac: List<String> = SasVerificationTransaction.KNOWN_MACS,
        codes: List<String> = SasVerificationTransaction.KNOWN_SHORT_CODES
) {
    val startMessage = KeyVerificationStart(
            fromDevice = bobSession.cryptoService().getMyCryptoDevice().deviceId,
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
fun test_aliceStartTwoRequests() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
    val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

    val aliceSession = cryptoTestData.firstSession
    val bobSession = cryptoTestData.secondSession

    val aliceVerificationService = aliceSession.cryptoService().verificationService()

    val aliceCreatedLatch = CountDownLatch(2)
    val aliceCancelledLatch = CountDownLatch(1)
    val createdTx = mutableListOf<VerificationTransaction>()
    val aliceListener = object : VerificationService.Listener {
        override fun transactionCreated(tx: VerificationTransaction) {
            createdTx.add(tx)
            aliceCreatedLatch.countDown()
        }

        override fun transactionUpdated(tx: VerificationTransaction) {
            tx as SasVerificationTransaction
            if (tx.state() is SasTransactionState.Cancelled && !(tx.state() as SasTransactionState.Cancelled).byMe) {
                aliceCancelledLatch.countDown()
            }
        }
    }
//        aliceVerificationService.addListener(aliceListener)

    val bobUserId = bobSession!!.myUserId
    val bobDeviceId = bobSession.cryptoService().getMyCryptoDevice().deviceId

    // TODO
//        aliceSession.cryptoService().downloadKeysIfNeeded(listOf(bobUserId), forceDownload = true)
//        aliceVerificationService.beginKeyVerification(listOf(VerificationMethod.SAS), bobUserId, bobDeviceId)
//        aliceVerificationService.beginKeyVerification(bobUserId, bobDeviceId)
//        testHelper.await(aliceCreatedLatch)
//        testHelper.await(aliceCancelledLatch)

    cryptoTestData.cleanUp(testHelper)
}

/**
     * Test that when alice starts a 'correct' request, bob agrees.
     */
//    @Test
//    @Ignore("This test will be ignored until it is fixed")
//    fun test_aliceAndBobAgreement() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
//        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
//
//        val aliceSession = cryptoTestData.firstSession
//        val bobSession = cryptoTestData.secondSession
//
//        val aliceVerificationService = aliceSession.cryptoService().verificationService()
//        val bobVerificationService = bobSession!!.cryptoService().verificationService()
//
//        val aliceAcceptedLatch = CountDownLatch(1)
//        val aliceListener = object : VerificationService.Listener {
//            override fun transactionUpdated(tx: VerificationTransaction) {
//                if (tx.state() is VerificationTxState.OnAccepted) {
//                    aliceAcceptedLatch.countDown()
//                }
//            }
//        }
//        aliceVerificationService.addListener(aliceListener)
//
//        val bobListener = object : VerificationService.Listener {
//            override fun transactionUpdated(tx: VerificationTransaction) {
//                if (tx.state() is VerificationTxState.OnStarted && tx is SasVerificationTransaction) {
//                    bobVerificationService.removeListener(this)
//                    runBlocking {
//                        tx.acceptVerification()
//                    }
//                }
//            }
//        }
//        bobVerificationService.addListener(bobListener)
//
//        val bobUserId = bobSession.myUserId
//        val bobDeviceId = runBlocking {
//            bobSession.cryptoService().getMyCryptoDevice().deviceId
//        }
//
//        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
//        testHelper.await(aliceAcceptedLatch)
//
//        aliceVerificationService.getExistingTransaction(bobUserId, )
//
//        assertTrue("Should have receive a commitment", accepted!!.commitment?.trim()?.isEmpty() == false)
//
//        // check that agreement is valid
//        assertTrue("Agreed Protocol should be Valid", accepted != null)
//        assertTrue("Agreed Protocol should be known by alice", startReq!!.keyAgreementProtocols.contains(accepted!!.keyAgreementProtocol))
//        assertTrue("Hash should be known by alice", startReq!!.hashes.contains(accepted!!.hash))
//        assertTrue("Hash should be known by alice", startReq!!.messageAuthenticationCodes.contains(accepted!!.messageAuthenticationCode))
//
//        accepted!!.shortAuthenticationStrings.forEach {
//            assertTrue("all agreed Short Code should be known by alice", startReq!!.shortAuthenticationStrings.contains(it))
//        }
//    }

//    @Test
//    fun test_aliceAndBobSASCode() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
//        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
//        cryptoTestData.initializeCrossSigning(cryptoTestHelper)
//        val sasTestHelper = SasVerificationTestHelper(testHelper, cryptoTestHelper)
//        val aliceSession = cryptoTestData.firstSession
//        val bobSession = cryptoTestData.secondSession!!
//        val transactionId = sasTestHelper.requestVerificationAndWaitForReadyState(cryptoTestData, supportedMethods)
//
//        val latch = CountDownLatch(2)
//        val aliceListener = object : VerificationService.Listener {
//            override fun transactionUpdated(tx: VerificationTransaction) {
//                Timber.v("Alice transactionUpdated: ${tx.state()}")
//                latch.countDown()
//            }
//        }
//        aliceSession.cryptoService().verificationService().addListener(aliceListener)
//        val bobListener = object : VerificationService.Listener {
//            override fun transactionUpdated(tx: VerificationTransaction) {
//                Timber.v("Bob transactionUpdated: ${tx.state()}")
//                latch.countDown()
//            }
//        }
//        bobSession.cryptoService().verificationService().addListener(bobListener)
//        aliceSession.cryptoService().verificationService().beginKeyVerification(VerificationMethod.SAS, bobSession.myUserId, transactionId)
//
//        testHelper.await(latch)
//        val aliceTx =
//                aliceSession.cryptoService().verificationService().getExistingTransaction(bobSession.myUserId, transactionId) as SasVerificationTransaction
//        val bobTx = bobSession.cryptoService().verificationService().getExistingTransaction(aliceSession.myUserId, transactionId) as SasVerificationTransaction
//
//        assertEquals("Should have same SAS", aliceTx.getDecimalCodeRepresentation(), bobTx.getDecimalCodeRepresentation())
//
//        val aliceTx = aliceVerificationService.getExistingTransaction(bobUserId, verificationSAS!!) as SASDefaultVerificationTransaction
//        val bobTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, verificationSAS) as SASDefaultVerificationTransaction
//
//        assertEquals(
//                "Should have same SAS", aliceTx.getShortCodeRepresentation(SasMode.DECIMAL),
//                bobTx.getShortCodeRepresentation(SasMode.DECIMAL)
//        )
//    }

@Test
fun test_happyPath() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
    val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
    cryptoTestData.initializeCrossSigning(cryptoTestHelper)
    val sasVerificationTestHelper = SasVerificationTestHelper(testHelper, cryptoTestHelper)
    val transactionId = sasVerificationTestHelper.requestVerificationAndWaitForReadyState(cryptoTestData, listOf(VerificationMethod.SAS))
    val aliceSession = cryptoTestData.firstSession
    val bobSession = cryptoTestData.secondSession

    val aliceVerificationService = aliceSession.cryptoService().verificationService()
    val bobVerificationService = bobSession!!.cryptoService().verificationService()

    val verifiedLatch = CountDownLatch(2)
    val aliceListener = object : VerificationService.Listener {

        override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
            Timber.v("RequestUpdated pr=$pr")
        }

        var matched = false
        var verified = false
        override fun transactionUpdated(tx: VerificationTransaction) {
            if (tx !is SasVerificationTransaction) return
            Timber.v("Alice transactionUpdated: ${tx.state()} on thread:${Thread.currentThread()}")
            when (tx.state()) {
                SasTransactionState.SasShortCodeReady -> {
                    if (!matched) {
                        matched = true
                        runBlocking {
                            delay(500)
                            tx.userHasVerifiedShortCode()
                        }
                    }
                }
                is SasTransactionState.Done -> {
                    if (!verified) {
                        verified = true
                        verifiedLatch.countDown()
                    }
                }
                else -> Unit
            }
        }
    }
//        aliceVerificationService.addListener(aliceListener)

    val bobListener = object : VerificationService.Listener {
        var accepted = false
        var matched = false
        var verified = false

        override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
            Timber.v("RequestUpdated: pr=$pr")
        }

        override fun transactionUpdated(tx: VerificationTransaction) {
            if (tx !is SasVerificationTransaction) return
            Timber.v("Bob transactionUpdated: ${tx.state()} on thread: ${Thread.currentThread()}")
            when (tx.state()) {
//                    VerificationTxState.SasStarted ->  {
//                        if (!accepted) {
//                            accepted = true
//                            runBlocking {
//                                tx.acceptVerification()
//                            }
//                        }
//                    }
                SasTransactionState.SasShortCodeReady -> {
                    if (!matched) {
                        matched = true
                        runBlocking {
                            delay(500)
                            tx.userHasVerifiedShortCode()
                        }
                    }
                }
                is SasTransactionState.Done -> {
                    if (!verified) {
                        verified = true
                        verifiedLatch.countDown()
                    }
                }
                else -> Unit
            }
        }
    }
//        bobVerificationService.addListener(bobListener)

    val bobUserId = bobSession.myUserId
    val bobDeviceId = runBlocking {
        bobSession.cryptoService().getMyCryptoDevice().deviceId
    }
    aliceVerificationService.startKeyVerification(VerificationMethod.SAS, bobUserId, transactionId)

    Timber.v("Await after beginKey ${Thread.currentThread()}")
    testHelper.await(verifiedLatch)

    // Assert that devices are verified
    val bobDeviceInfoFromAlicePOV: CryptoDeviceInfo? = aliceSession.cryptoService().getCryptoDeviceInfo(bobUserId, bobDeviceId)
    val aliceDeviceInfoFromBobPOV: CryptoDeviceInfo? =
            bobSession.cryptoService().getCryptoDeviceInfo(aliceSession.myUserId, aliceSession.cryptoService().getMyCryptoDevice().deviceId)

    assertTrue("alice device should be verified from bob point of view", aliceDeviceInfoFromBobPOV!!.isVerified)
    assertTrue("bob device should be verified from alice point of view", bobDeviceInfoFromAlicePOV!!.isVerified)
}

@Test
fun test_ConcurrentStart() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
    val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()
    cryptoTestData.initializeCrossSigning(cryptoTestHelper)
    val aliceSession = cryptoTestData.firstSession
    val bobSession = cryptoTestData.secondSession!!

    val aliceVerificationService = aliceSession.cryptoService().verificationService()
    val bobVerificationService = bobSession.cryptoService().verificationService()

    val req = aliceVerificationService.requestKeyVerificationInDMs(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                bobSession.myUserId,
                cryptoTestData.roomId
        )

    val requestID = req.transactionId

    Log.v("TEST", "== requestID is $requestID")

    testHelper.retryPeriodically {
        val prBobPOV = bobVerificationService.getExistingVerificationRequests(aliceSession.myUserId).firstOrNull()
        Log.v("TEST", "== prBobPOV is $prBobPOV")
        prBobPOV?.transactionId == requestID
    }

    bobVerificationService.readyPendingVerification(
            listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
            aliceSession.myUserId,
            requestID
    )

    // wait for alice to get the ready
    testHelper.retryPeriodically {
        val prAlicePOV = aliceVerificationService.getExistingVerificationRequests(bobSession.myUserId).firstOrNull()
        Log.v("TEST", "== prAlicePOV is $prAlicePOV")
        prAlicePOV?.transactionId == requestID && prAlicePOV.state == EVerificationState.Ready
    }

    // Start concurrent!
    aliceVerificationService.startKeyVerification(
            method = VerificationMethod.SAS,
            otherUserId = bobSession.myUserId,
            requestId = requestID,
    )

    bobVerificationService.startKeyVerification(
            method = VerificationMethod.SAS,
            otherUserId = aliceSession.myUserId,
            requestId = requestID,
    )

    // we should reach SHOW SAS on both
    var alicePovTx: SasVerificationTransaction?
    var bobPovTx: SasVerificationTransaction?

    testHelper.retryPeriodically {
        alicePovTx = aliceVerificationService.getExistingTransaction(bobSession.myUserId, requestID) as? SasVerificationTransaction
        Log.v("TEST", "== alicePovTx is $alicePovTx")
        alicePovTx?.state() == SasTransactionState.SasShortCodeReady
    }
    // wait for alice to get the ready
    testHelper.retryPeriodically {
        bobPovTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, requestID) as? SasVerificationTransaction
        Log.v("TEST", "== bobPovTx is $bobPovTx")
        bobPovTx?.state() == SasTransactionState.SasShortCodeReady
    }
}

     */
}
