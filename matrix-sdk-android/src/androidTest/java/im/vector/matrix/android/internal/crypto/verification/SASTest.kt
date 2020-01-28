/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.verification

import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.*
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.CryptoTestHelper
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationAccept
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationCancel
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationStart
import im.vector.matrix.android.internal.crypto.model.rest.toValue
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.*
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SASTest : InstrumentedTest {
    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_aliceStartThenAliceCancel() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.getVerificationService()
        val bobVerificationService = bobSession!!.getVerificationService()

        val bobTxCreatedLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                bobTxCreatedLatch.countDown()
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobVerificationService.addListener(bobListener)

        val txID = aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobSession.myUserId, bobSession.getMyDevice().deviceId)
        assertNotNull("Alice should have a started transaction", txID)

        val aliceKeyTx = aliceVerificationService.getExistingTransaction(bobSession.myUserId, txID!!)
        assertNotNull("Alice should have a started transaction", aliceKeyTx)

        mTestHelper.await(bobTxCreatedLatch)
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

        val bobListener2 = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == txID) {
                    if ((tx as SASDefaultVerificationTransaction).state === VerificationTxState.OnCancelled) {
                        cancelLatch.countDown()
                    }
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobVerificationService.addListener(bobListener2)

        aliceSasTx.cancel(CancelCode.User)
        mTestHelper.await(cancelLatch)

        assertEquals("Should be cancelled on alice side",
                VerificationTxState.Cancelled, aliceSasTx.state)
        assertEquals("Should be cancelled on bob side",
                VerificationTxState.OnCancelled, bobSasTx.state)

        assertEquals("Should be User cancelled on alice side",
                CancelCode.User, aliceSasTx.cancelledReason)
        assertEquals("Should be User cancelled on bob side",
                CancelCode.User, aliceSasTx.cancelledReason)

        assertNull(bobVerificationService.getExistingTransaction(aliceSession.myUserId, txID))
        assertNull(aliceVerificationService.getExistingTransaction(bobSession.myUserId, txID))

        cryptoTestData.close()
    }

    @Test
    fun test_key_agreement_protocols_must_include_curve25519() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession!!

        val protocols = listOf("meh_dont_know")
        val tid = "00000000"

        // Bob should receive a cancel
        var cancelReason: String? = null
        val cancelLatch = CountDownLatch(1)

        val bobListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == tid && tx.cancelledReason != null) {
                    cancelReason = tx.cancelledReason?.humanReadable
                    cancelLatch.countDown()
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobSession.getVerificationService().addListener(bobListener)

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
        val aliceDevice = aliceSession.getMyDevice().deviceId

        val aliceListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as IncomingSasVerificationTransaction).uxState === IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT) {
                    (tx as IncomingSasVerificationTransaction).performAccept()
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceSession.getVerificationService().addListener(aliceListener)

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, protocols = protocols)

        mTestHelper.await(cancelLatch)

        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReason)

        cryptoTestData.close()
    }

    @Test
    fun test_key_agreement_macs_Must_include_hmac_sha256() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

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
        val aliceDevice = aliceSession.getMyDevice().deviceId

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, mac = mac)

        mTestHelper.await(cancelLatch)

        val cancelReq = canceledToDeviceEvent!!.content.toModel<KeyVerificationCancel>()!!
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.close()
    }

    @Test
    fun test_key_agreement_short_code_include_decimal() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

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
        val aliceDevice = aliceSession.getMyDevice().deviceId

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, codes = codes)

        mTestHelper.await(cancelLatch)

        val cancelReq = canceledToDeviceEvent!!.content.toModel<KeyVerificationCancel>()!!
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.close()
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
                fromDevice = bobSession.getMyDevice().deviceId,
                method = VerificationMethod.SAS.toValue(),
                transactionID = tid,
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
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.getVerificationService()

        val aliceCreatedLatch = CountDownLatch(2)
        val aliceCancelledLatch = CountDownLatch(2)
        val createdTx = ArrayList<SASDefaultVerificationTransaction>()
        val aliceListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {
                createdTx.add(tx as SASDefaultVerificationTransaction)
                aliceCreatedLatch.countDown()
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as SASDefaultVerificationTransaction).state === VerificationTxState.OnCancelled) {
                    aliceCancelledLatch.countDown()
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceVerificationService.addListener(aliceListener)

        val bobUserId = bobSession!!.myUserId
        val bobDeviceId = bobSession.getMyDevice().deviceId
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId)
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId)

        mTestHelper.await(aliceCreatedLatch)
        mTestHelper.await(aliceCancelledLatch)

        cryptoTestData.close()
    }

    /**
     * Test that when alice starts a 'correct' request, bob agrees.
     */
    @Test
    fun test_aliceAndBobAgreement() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.getVerificationService()
        val bobVerificationService = bobSession!!.getVerificationService()

        var accepted: KeyVerificationAccept? = null
        var startReq: KeyVerificationStart? = null

        val aliceAcceptedLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.VerificationListener {
            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}

            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as SASDefaultVerificationTransaction).state === VerificationTxState.OnAccepted) {
                    val at = tx as SASDefaultVerificationTransaction
                    accepted = at.accepted as? KeyVerificationAccept
                    startReq = at.startReq as? KeyVerificationStart
                    aliceAcceptedLatch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as IncomingSasVerificationTransaction).uxState === IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT) {
                    val at = tx as IncomingSasVerificationTransaction
                    at.performAccept()
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.getMyDevice().deviceId
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId)
        mTestHelper.await(aliceAcceptedLatch)

        assertTrue("Should have receive a commitment", accepted!!.commitment?.trim()?.isEmpty() == false)

        // check that agreement is valid
        assertTrue("Agreed Protocol should be Valid", accepted!!.isValid())
        assertTrue("Agreed Protocol should be known by alice", startReq!!.keyAgreementProtocols!!.contains(accepted!!.keyAgreementProtocol))
        assertTrue("Hash should be known by alice", startReq!!.hashes!!.contains(accepted!!.hash))
        assertTrue("Hash should be known by alice", startReq!!.messageAuthenticationCodes!!.contains(accepted!!.messageAuthenticationCode))

        accepted!!.shortAuthenticationStrings?.forEach {
            assertTrue("all agreed Short Code should be known by alice", startReq!!.shortAuthenticationStrings!!.contains(it))
        }

        cryptoTestData.close()
    }

    @Test
    fun test_aliceAndBobSASCode() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.getVerificationService()
        val bobVerificationService = bobSession!!.getVerificationService()

        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as OutgoingSasVerificationTransaction).uxState
                when (uxState) {
                    OutgoingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        aliceSASLatch.countDown()
                    }
                    else                                                -> Unit
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceVerificationService.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

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

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.getMyDevice().deviceId
        val verificationSAS = aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId)
        mTestHelper.await(aliceSASLatch)
        mTestHelper.await(bobSASLatch)

        val aliceTx = aliceVerificationService.getExistingTransaction(bobUserId, verificationSAS!!) as SASDefaultVerificationTransaction
        val bobTx = bobVerificationService.getExistingTransaction(aliceSession.myUserId, verificationSAS) as SASDefaultVerificationTransaction

        assertEquals("Should have same SAS", aliceTx.getShortCodeRepresentation(SasMode.DECIMAL),
                bobTx.getShortCodeRepresentation(SasMode.DECIMAL))

        cryptoTestData.close()
    }

    @Test
    fun test_happyPath() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceVerificationService = aliceSession.getVerificationService()
        val bobVerificationService = bobSession!!.getVerificationService()

        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as OutgoingSasVerificationTransaction).uxState
                when (uxState) {
                    OutgoingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        tx.userHasVerifiedShortCode()
                    }
                    OutgoingSasVerificationTransaction.UxState.VERIFIED -> {
                        aliceSASLatch.countDown()
                    }
                    else                                                -> Unit
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceVerificationService.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationService.VerificationListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as IncomingSasVerificationTransaction).uxState
                when (uxState) {
                    IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT -> {
                        tx.performAccept()
                    }
                    IncomingSasVerificationTransaction.UxState.SHOW_SAS    -> {
                        tx.userHasVerifiedShortCode()
                    }
                    IncomingSasVerificationTransaction.UxState.VERIFIED    -> {
                        bobSASLatch.countDown()
                    }
                    else                                                   -> Unit
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.getMyDevice().deviceId
        aliceVerificationService.beginKeyVerification(VerificationMethod.SAS, bobUserId, bobDeviceId)
        mTestHelper.await(aliceSASLatch)
        mTestHelper.await(bobSASLatch)

        // Assert that devices are verified
        val bobDeviceInfoFromAlicePOV: CryptoDeviceInfo? = aliceSession.getDeviceInfo(bobUserId, bobDeviceId)
        val aliceDeviceInfoFromBobPOV: CryptoDeviceInfo? = bobSession.getDeviceInfo(aliceSession.myUserId, aliceSession.getMyDevice().deviceId)

        // latch wait a bit again
        Thread.sleep(1000)

        assertTrue("alice device should be verified from bob point of view", aliceDeviceInfoFromBobPOV!!.isVerified)
        assertTrue("bob device should be verified from alice point of view", bobDeviceInfoFromAlicePOV!!.isVerified)
        cryptoTestData.close()
    }
}
