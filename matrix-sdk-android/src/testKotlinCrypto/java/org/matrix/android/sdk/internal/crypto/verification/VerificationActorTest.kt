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

package org.matrix.android.sdk.internal.crypto.verification.org.matrix.android.sdk.internal.crypto.verification

import android.util.Base64
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.amshove.kluent.fail
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.internal.assertNotEquals
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotBeEqualTo
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.MatrixTest
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.internal.crypto.verification.FakeCryptoStoreForVerification
import org.matrix.android.sdk.internal.crypto.verification.VerificationActor
import org.matrix.android.sdk.internal.crypto.verification.VerificationActorHelper
import org.matrix.android.sdk.internal.crypto.verification.VerificationIntent

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationActorTest : MatrixTest {

    val transportScope = CoroutineScope(SupervisorJob())

    @Before
    fun setUp() {
        // QR code needs that
        mockkStatic(Base64::class)
        every {
            Base64.encodeToString(any(), any())
        } answers {
            val array = firstArg<ByteArray>()
            java.util.Base64.getEncoder().encodeToString(array)
        }

        every {
            Base64.decode(any<String>(), any())
        } answers {
            val array = firstArg<String>()
            java.util.Base64.getDecoder().decode(array)
        }

        // to mock canonical json
        mockkConstructor(JSONObject::class)
        every { anyConstructed<JSONObject>().keys() } returns emptyList<String>().listIterator()

//        mockkConstructor(KotlinSasTransaction::class)
//        every { anyConstructed<KotlinSasTransaction>().getSAS() } returns mockk<OlmSAS>() {
//            every { publicKey } returns "Tm9JRGVhRmFrZQo="
//        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `If ready both side should support sas and Qr show and scan`() = runTest {
        val testData = VerificationActorHelper().setUpActors()
        val aliceActor = testData.aliceActor
        val bobActor = testData.bobActor

        val completableDeferred = CompletableDeferred<PendingVerificationRequest>()

        transportScope.launch {
            bobActor.eventFlow.collect {
                if (it is VerificationEvent.RequestAdded) {
                    completableDeferred.complete(it.request)
                    return@collect cancel()
                }
            }
        }

        aliceActor.requestVerification(listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SHOW, VerificationMethod.QR_CODE_SCAN))

        val bobIncomingRequest = completableDeferred.await()
        bobIncomingRequest.state shouldBeEqualTo EVerificationState.Requested

        val aliceReadied = CompletableDeferred<PendingVerificationRequest>()

        transportScope.launch {
            aliceActor.eventFlow.collect {
                if (it is VerificationEvent.RequestUpdated && it.request.state == EVerificationState.Ready) {
                    aliceReadied.complete(it.request)
                    return@collect cancel()
                }
            }
        }

        // test ready
        val bobReadied = awaitDeferrable<PendingVerificationRequest?> {
            bobActor.send(
                    VerificationIntent.ActionReadyRequest(
                            bobIncomingRequest.transactionId,
                            methods = listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SHOW, VerificationMethod.QR_CODE_SCAN),
                            it
                    )
            )
        }

        val readiedAliceSide = aliceReadied.await()

        readiedAliceSide.isSasSupported shouldBeEqualTo true
        readiedAliceSide.weShouldDisplayQRCode shouldBeEqualTo true

        bobReadied shouldNotBe null
        bobReadied!!.isSasSupported shouldBeEqualTo true
        bobReadied.weShouldDisplayQRCode shouldBeEqualTo true

        bobReadied.qrCodeText shouldNotBe null
        readiedAliceSide.qrCodeText shouldNotBe null
    }

    @Test
    fun `Test alice can show but not scan QR`() = runTest {
        val testData = VerificationActorHelper().setUpActors()
        val aliceActor = testData.aliceActor
        val bobActor = testData.bobActor

        println("Alice sends a request")
        val outgoingRequest = aliceActor.requestVerification(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SHOW)
        )

        // wait for bob to get it
        println("Wait for bob to get it")
        waitForBobToSeeIncomingRequest(bobActor, outgoingRequest)

        println("let bob ready it")
        val bobReady = bobActor.readyVerification(
                outgoingRequest.transactionId,
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW)
        )

        println("Wait for alice to get the ready")
        retryUntil {
            awaitDeferrable<PendingVerificationRequest?> {
                aliceActor.send(VerificationIntent.GetExistingRequest(outgoingRequest.transactionId, outgoingRequest.otherUserId, it))
            }?.state == EVerificationState.Ready
        }

        val aliceReady = awaitDeferrable<PendingVerificationRequest?> {
            aliceActor.send(VerificationIntent.GetExistingRequest(outgoingRequest.transactionId, outgoingRequest.otherUserId, it))
        }!!

        aliceReady.isSasSupported shouldBeEqualTo bobReady.isSasSupported

        // alice can't scan so there should not be option to do so
        assertEquals("Alice should not show scan option", false, aliceReady.weShouldShowScanOption)
        assertEquals("Alice should show QR as bob can scan", true, aliceReady.weShouldDisplayQRCode)

        assertEquals("Bob should be able to scan", true, bobReady.weShouldShowScanOption)
        assertEquals("Bob should not show QR as alice can scan", false, bobReady.weShouldDisplayQRCode)
    }

    @Test
    fun `If Bobs device does not understand any of the methods, it should not cancel the request`() = runTest {
        val testData = VerificationActorHelper().setUpActors()
        val aliceActor = testData.aliceActor
        val bobActor = testData.bobActor

        val outgoingRequest = aliceActor.requestVerification(
                listOf(VerificationMethod.SAS)
        )

        // wait for bob to get it
        waitForBobToSeeIncomingRequest(bobActor, outgoingRequest)

        println("let bob ready it")
        try {
            bobActor.readyVerification(
                    outgoingRequest.transactionId,
                    listOf(VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW)
            )
            // Upon receipt of Alice’s m.key.verification.request message, if Bob’s device does not understand any of the methods,
            // it should not cancel the request as one of his other devices may support the request
            fail("Ready should fail as no common methods")
        } catch (failure: Throwable) {
            // should throw
        }

        val bodSide = awaitDeferrable<PendingVerificationRequest?> {
            bobActor.send(VerificationIntent.GetExistingRequest(outgoingRequest.transactionId, FakeCryptoStoreForVerification.aliceMxId, it))
        }!!

        bodSide.state shouldNotBeEqualTo EVerificationState.Cancelled
    }

    @Test
    fun `Test bob can show but not scan QR`() = runTest {
        val testData = VerificationActorHelper().setUpActors()
        val aliceActor = testData.aliceActor
        val bobActor = testData.bobActor

        println("Alice sends a request")
        val outgoingRequest = aliceActor.requestVerification(
                listOf(VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW)
        )

        // wait for bob to get it
        println("Wait for bob to get it")
        waitForBobToSeeIncomingRequest(bobActor, outgoingRequest)

        println("let bob ready it")
        val bobReady = bobActor.readyVerification(
                outgoingRequest.transactionId,
                listOf(VerificationMethod.QR_CODE_SHOW)
        )

        println("Wait for alice to get the ready")
        retryUntil {
            awaitDeferrable<PendingVerificationRequest?> {
                aliceActor.send(VerificationIntent.GetExistingRequest(outgoingRequest.transactionId, outgoingRequest.otherUserId, it))
            }?.state == EVerificationState.Ready
        }

        val aliceReady = awaitDeferrable<PendingVerificationRequest?> {
            aliceActor.send(VerificationIntent.GetExistingRequest(outgoingRequest.transactionId, outgoingRequest.otherUserId, it))
        }!!

        assertEquals("Alice sas is not supported", false, aliceReady.isSasSupported)
        aliceReady.isSasSupported shouldBeEqualTo bobReady.isSasSupported

        // alice can't scan so there should not be option to do so
        assertEquals("Alice should show scan option", true, aliceReady.weShouldShowScanOption)
        assertEquals("Alice QR data should be null", null, aliceReady.qrCodeText)
        assertEquals("Alice should not show QR as bob can scan", false, aliceReady.weShouldDisplayQRCode)

        assertEquals("Bob should not should not show cam option as it can't scan", false, bobReady.weShouldShowScanOption)
        assertNotEquals("Bob QR data should be there", null, bobReady.qrCodeText)
        assertEquals("Bob should show QR as alice can scan", true, bobReady.weShouldDisplayQRCode)
    }

    @Test
    fun `Test verify to users that trust their cross signing keys`() = runTest {
        val testData = VerificationActorHelper().setUpActors()
        val aliceActor = testData.aliceActor
        val bobActor = testData.bobActor

        coEvery { testData.bobStore.instance.canCrossSign() } returns true
        coEvery { testData.aliceStore.instance.canCrossSign() } returns true

        fullSasVerification(bobActor, aliceActor)

        coVerify {
            testData.bobStore.instance.locallyTrustDevice(
                    FakeCryptoStoreForVerification.aliceMxId,
                    FakeCryptoStoreForVerification.aliceDevice1Id,
            )
        }

        coVerify {
            testData.bobStore.instance.trustUser(
                    FakeCryptoStoreForVerification.aliceMxId
            )
        }

        coVerify {
            testData.aliceStore.instance.locallyTrustDevice(
                    FakeCryptoStoreForVerification.bobMxId,
                    FakeCryptoStoreForVerification.bobDeviceId,
            )
        }

        coVerify {
            testData.aliceStore.instance.trustUser(
                    FakeCryptoStoreForVerification.bobMxId
            )
        }
    }

    @Test
    fun `Test user verification when alice do not trust her keys`() = runTest {
        val testData = VerificationActorHelper().setUpActors()
        val aliceActor = testData.aliceActor
        val bobActor = testData.bobActor

        coEvery { testData.bobStore.instance.canCrossSign() } returns true
        coEvery { testData.aliceStore.instance.canCrossSign() } returns false
        coEvery { testData.aliceStore.instance.getMyTrustedMasterKeyBase64() } returns null

        fullSasVerification(bobActor, aliceActor)

        coVerify {
            testData.bobStore.instance.locallyTrustDevice(
                    FakeCryptoStoreForVerification.aliceMxId,
                    FakeCryptoStoreForVerification.aliceDevice1Id,
            )
        }

        coVerify(exactly = 0) {
            testData.bobStore.instance.trustUser(
                    FakeCryptoStoreForVerification.aliceMxId
            )
        }

        coVerify {
            testData.aliceStore.instance.locallyTrustDevice(
                    FakeCryptoStoreForVerification.bobMxId,
                    FakeCryptoStoreForVerification.bobDeviceId,
            )
        }

        coVerify(exactly = 0) {
            testData.aliceStore.instance.trustUser(
                    FakeCryptoStoreForVerification.bobMxId
            )
        }
    }

    private suspend fun fullSasVerification(bobActor: VerificationActor, aliceActor: VerificationActor) {
        transportScope.launch {
            bobActor.eventFlow
                    .collect {
                        println("Bob flow 1 event $it")
                        if (it is VerificationEvent.RequestAdded) {
                            // auto accept
                            bobActor.readyVerification(
                                    it.transactionId,
                                    listOf(VerificationMethod.SAS)
                            )
                            // then start
                            bobActor.send(
                                    VerificationIntent.ActionStartSasVerification(
                                            FakeCryptoStoreForVerification.aliceMxId,
                                            it.transactionId,
                                            CompletableDeferred()
                                    )
                            )
                        }
                        return@collect cancel()
                    }
        }

        val aliceCode = CompletableDeferred<SasVerificationTransaction>()
        val bobCode = CompletableDeferred<SasVerificationTransaction>()

        aliceActor.eventFlow.onEach {
            println("Alice flow event $it")
            if (it is VerificationEvent.TransactionUpdated) {
                val sasVerificationTransaction = it.transaction as SasVerificationTransaction
                if (sasVerificationTransaction.state() is SasTransactionState.SasShortCodeReady) {
                    aliceCode.complete(sasVerificationTransaction)
                }
            }
        }.launchIn(transportScope)

        bobActor.eventFlow.onEach {
            println("Bob flow event $it")
            if (it is VerificationEvent.TransactionUpdated) {
                val sasVerificationTransaction = it.transaction as SasVerificationTransaction
                if (sasVerificationTransaction.state() is SasTransactionState.SasShortCodeReady) {
                    bobCode.complete(sasVerificationTransaction)
                }
            }
        }.launchIn(transportScope)

        println("Alice sends a request")
        val outgoingRequest = aliceActor.requestVerification(
                listOf(VerificationMethod.SAS)
        )

        // asserting the code won't help much here as all is mocked
        // we are checking state progression
        // Both transaction should be in sas ready
        val aliceCodeReadyTx = aliceCode.await()
        bobCode.await()

        // If alice accept the code, bob should pass to state mac received but code not comfirmed
        aliceCodeReadyTx.userHasVerifiedShortCode()

        retryUntil {
            val tx = bobActor.getTransactionBobPov(outgoingRequest.transactionId)
            val sasTx = tx as? SasVerificationTransaction
            val state = sasTx?.state()
            (state is SasTransactionState.SasMacReceived && !state.codeConfirmed)
        }

        val bobTransaction = bobActor.getTransactionBobPov(outgoingRequest.transactionId) as SasVerificationTransaction

        val bobDone = CompletableDeferred(Unit)
        val aliceDone = CompletableDeferred(Unit)
        transportScope.launch {
            bobActor.eventFlow
                    .collect {
                        println("Bob flow 1 event $it")
                        it.getRequest()?.let {
                            if (it.transactionId == outgoingRequest.transactionId && it.state == EVerificationState.Done) {
                                bobDone.complete(Unit)
                                return@collect cancel()
                            }
                        }
                    }
        }
        transportScope.launch {
            aliceActor.eventFlow
                    .collect {
                        println("Bob flow 1 event $it")
                        it.getRequest()?.let {
                            if (it.transactionId == outgoingRequest.transactionId && it.state == EVerificationState.Done) {
                                bobDone.complete(Unit)
                                return@collect cancel()
                            }
                        }
                    }
        }

        // mark as verified from bob side
        bobTransaction.userHasVerifiedShortCode()

        aliceDone.await()
        bobDone.await()
    }

    internal suspend fun VerificationActor.getTransactionBobPov(transactionId: String): VerificationTransaction? {
        return awaitDeferrable<VerificationTransaction?> {
            channel.send(
                    VerificationIntent.GetExistingTransaction(
                            transactionId = transactionId,
                            fromUser = FakeCryptoStoreForVerification.aliceMxId,
                            it
                    )
            )
        }
    }

    private suspend fun VerificationActor.requestVerification(methods: List<VerificationMethod>): PendingVerificationRequest {
        return awaitDeferrable<PendingVerificationRequest> {
            send(
                    VerificationIntent.ActionRequestVerification(
                            otherUserId = FakeCryptoStoreForVerification.bobMxId,
                            roomId = "aRoom",
                            methods = methods,
                            deferred = it
                    )
            )
        }
    }

    private suspend fun waitForBobToSeeIncomingRequest(bobActor: VerificationActor, aliceOutgoing: PendingVerificationRequest) {
        retryUntil {
            awaitDeferrable<PendingVerificationRequest?> {
                bobActor.send(
                        VerificationIntent.GetExistingRequest(
                                aliceOutgoing.transactionId,
                                FakeCryptoStoreForVerification.aliceMxId, it
                        )
                )
            }?.state == EVerificationState.Requested
        }
    }

    private val backoff = listOf(500L, 1_000L, 1_000L, 3_000L, 3_000L, 5_000L)

    private suspend fun retryUntil(condition: suspend (() -> Boolean)) {
        var tryCount = 0
        while (!condition()) {
            if (tryCount >= backoff.size) {
                fail("Retry Until Fialed")
            }
            withContext(Dispatchers.IO) {
                delay(backoff[tryCount])
            }
            tryCount++
        }
    }

    private suspend fun <T> awaitDeferrable(block: suspend ((CompletableDeferred<T>) -> Unit)): T {
        val deferred = CompletableDeferred<T>()
        block.invoke(deferred)
        return deferred.await()
    }

    private suspend fun VerificationActor.readyVerification(transactionId: String, methods: List<VerificationMethod>): PendingVerificationRequest {
        return awaitDeferrable<PendingVerificationRequest?> {
            send(
                    VerificationIntent.ActionReadyRequest(
                            transactionId,
                            methods = methods,
                            it
                    )
            )
        }!!
    }
}
