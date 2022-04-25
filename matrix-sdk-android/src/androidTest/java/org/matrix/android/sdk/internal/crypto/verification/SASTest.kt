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
import kotlinx.coroutines.runBlocking
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
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.model.rest.toValue
import timber.log.Timber
import java.util.UUID
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

        val myDevice = testHelper.runBlockingTest {
            bobSession.cryptoService().getMyDevice()
        }
        val txID = testHelper.runBlockingTest {
            aliceVerificationService.beginKeyVerification(VerificationMethod.SAS,
                    bobSession.myUserId,
                    myDevice.deviceId,
                    null)
        }
        assertNotNull("Alice should have a started transaction", txID)

        val aliceKeyTx = aliceVerificationService.getExistingTransaction(bobSession.myUserId, txID!!)
        assertNotNull("Alice should have a started transaction", aliceKeyTx)

        testHelper.await(bobTxCreatedLatch)
        bobVerificationService.removeListener(bobListener)

        val bobKeyTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, txID)

        assertNotNull("Bob should have started verif transaction", bobKeyTx)
        assertTrue(bobKeyTx is SasVerificationTransaction)
        assertNotNull("Bob should have starting a SAS transaction", bobKeyTx)
        assertTrue(aliceKeyTx is SasVerificationTransaction)
        assertEquals("Alice and Bob have same transaction id", aliceKeyTx!!.transactionId, bobKeyTx!!.transactionId)


        assertEquals("Alice state should be started", VerificationTxState.Started, aliceKeyTx.state)
        assertEquals("Bob state should be started by alice", VerificationTxState.OnStarted, bobKeyTx.state)

        // Let's cancel from alice side
        val cancelLatch = CountDownLatch(1)

        val bobListener2 = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == txID) {
                    val immutableState = (tx as SasVerificationTransaction).state
                    if (immutableState is VerificationTxState.Cancelled && !immutableState.byMe) {
                        cancelLatch.countDown()
                    }
                }
            }
        }
        bobVerificationService.addListener(bobListener2)

        testHelper.runBlockingTest {
            aliceKeyTx.cancel(CancelCode.User)
        }
        testHelper.await(cancelLatch)

        assertTrue("Should be cancelled on alice side", aliceKeyTx.state is VerificationTxState.Cancelled)
        assertTrue("Should be cancelled on bob side", bobKeyTx.state is VerificationTxState.Cancelled)

        val aliceCancelState = aliceKeyTx.state as VerificationTxState.Cancelled
        val bobCancelState = bobKeyTx.state as VerificationTxState.Cancelled

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
        val aliceDevice = testHelper.runBlockingTest {
            aliceSession.cryptoService().getMyDevice().deviceId
        }

        val aliceListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.state is VerificationTxState.OnStarted && tx is SasVerificationTransaction) {
                    testHelper.runBlockingTest {
                        tx.acceptVerification()
                    }
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
        val aliceDevice = testHelper.runBlockingTest {
            aliceSession.cryptoService().getMyDevice().deviceId
        }

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
        val aliceDevice = testHelper.runBlockingTest {
            aliceSession.cryptoService().getMyDevice().deviceId
        }

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
                             protocols: List<String> = emptyList(),
                             hashes: List<String> = emptyList(),
                             mac: List<String> = emptyList(),
                             codes: List<String> = emptyList()) {
        val deviceId = runBlocking {
            bobSession.cryptoService().getMyDevice().deviceId
        }
        val startMessage = KeyVerificationStart(
                fromDevice = deviceId,
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
        val createdTx = mutableListOf<VerificationTransaction>()
        val aliceListener = object : VerificationService.Listener {
            override fun transactionCreated(tx: VerificationTransaction) {
                createdTx.add(tx)
                aliceCreatedLatch.countDown()
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.state is VerificationTxState.Cancelled && !(tx.state as VerificationTxState.Cancelled).byMe) {
                    aliceCancelledLatch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobUserId = bobSession!!.myUserId
        val bobDeviceId = testHelper.runBlockingTest {
            bobSession.cryptoService().getMyDevice().deviceId
        }
        testHelper.runBlockingTest {
            aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
            aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        }
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

        val aliceAcceptedLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.state is VerificationTxState.OnAccepted) {
                    aliceAcceptedLatch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.state is VerificationTxState.OnStarted && tx is SasVerificationTransaction) {
                    bobVerificationService.removeListener(this)
                    testHelper.runBlockingTest {
                        tx.acceptVerification()
                    }
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = runBlocking {
            bobSession.cryptoService().getMyDevice().deviceId
        }
        testHelper.runBlockingTest {
            aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        }
        testHelper.await(aliceAcceptedLatch)

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    fun test_aliceAndBobSASCode() {
        val testHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(testHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!

        cryptoTestHelper.initializeCrossSigning(aliceSession)
        cryptoTestHelper.initializeCrossSigning(bobSession)

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession.cryptoService().verificationService()

        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.Listener {
            override fun transactionUpdated(tx: VerificationTransaction) {
                when (tx.state) {
                    VerificationTxState.ShortCodeReady -> {
                        aliceSASLatch.countDown()
                    }
                    else                               -> Unit
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.Listener {

            override fun verificationRequestCreated(pr: PendingVerificationRequest) {

            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                val sasVerification = tx as SasVerificationTransaction
                when (tx.state) {
                    VerificationTxState.OnStarted      -> testHelper.runBlockingTest {
                        sasVerification.acceptVerification()
                    }
                    VerificationTxState.ShortCodeReady -> {
                        bobSASLatch.countDown()
                    }
                    else                               -> Unit
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        testHelper.runBlockingTest {
            aliceSession.cryptoService().downloadKeys(listOf(bobUserId), forceDownload = true)
            aliceVerificationService.requestKeyVerificationInDMs(listOf(VerificationMethod.SAS),bobUserId, cryptoTestData.roomId)
        }
        testHelper.await(aliceSASLatch)
        testHelper.await(bobSASLatch)

        /*
        val aliceTx = aliceVerificationService.getExistingTransaction(bobUserId, verificationSAS!!) as SasVerificationTransaction
        val bobTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, verificationSAS) as SasVerificationTransaction


        assertEquals("Should have same SAS", aliceTx.getDecimalCodeRepresentation(), bobTx.getDecimalCodeRepresentation())

         */

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
                if (tx !is SasVerificationTransaction) return
                when (tx.state) {
                    VerificationTxState.ShortCodeReady -> testHelper.runBlockingTest {
                        tx.userHasVerifiedShortCode()
                    }
                    VerificationTxState.Verified       -> {
                        if (matchOnce) {
                            matchOnce = false
                            aliceSASLatch.countDown()
                        }
                    }
                    else                               -> Unit
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.Listener {
            var acceptOnce = true
            var matchOnce = true
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx !is SasVerificationTransaction) return
                when (tx.state) {
                    VerificationTxState.OnStarted      -> testHelper.runBlockingTest {
                        if (acceptOnce) {
                            acceptOnce = false
                            tx.acceptVerification()
                        }
                    }
                    VerificationTxState.ShortCodeReady -> testHelper.runBlockingTest {
                        if (matchOnce) {
                            matchOnce = false
                            tx.userHasVerifiedShortCode()
                        }
                    }
                    VerificationTxState.Verified       -> {
                        bobSASLatch.countDown()
                    }
                    else                               -> Unit
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = runBlocking {
            bobSession.cryptoService().getMyDevice().deviceId
        }
        testHelper.runBlockingTest {
            aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId, null)
        }
        testHelper.await(aliceSASLatch)
        testHelper.await(bobSASLatch)

        // Assert that devices are verified
        val bobDeviceInfoFromAlicePOV: CryptoDeviceInfo? = testHelper.runBlockingTest {
            aliceSession.cryptoService().getDeviceInfo(bobUserId, bobDeviceId)
        }
        val aliceDeviceInfoFromBobPOV: CryptoDeviceInfo? = testHelper.runBlockingTest {
            bobSession.cryptoService().getDeviceInfo(aliceSession.myUserId, aliceSession.cryptoService().getMyDevice().deviceId)
        }
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

        val req = testHelper.runBlockingTest {
            aliceVerificationService.requestKeyVerificationInDMs(
                    listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                    bobSession.myUserId,
                    cryptoTestData.roomId
            )
        }

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

        testHelper.runBlockingTest {
            bobVerificationService.readyPendingVerification(
                    listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                    aliceSession.myUserId,
                    requestID!!
            )
        }

        // wait for alice to get the ready
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val prAlicePOV = aliceVerificationService.getExistingVerificationRequests(bobSession.myUserId).firstOrNull()
                Log.v("TEST", "== prAlicePOV is $prAlicePOV")
                prAlicePOV?.transactionId == requestID && prAlicePOV?.isReady != null
            }
        }

        // Start concurrent!
        testHelper.runBlockingTest {
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

        }

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
