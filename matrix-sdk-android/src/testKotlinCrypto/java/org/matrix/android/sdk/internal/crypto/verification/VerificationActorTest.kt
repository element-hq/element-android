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
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.amshove.kluent.fail
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.internal.assertNotEquals
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.internal.crypto.verification.FakeCryptoStoreForVerification
import org.matrix.android.sdk.internal.crypto.verification.VerificationActor
import org.matrix.android.sdk.internal.crypto.verification.VerificationActorHelper
import org.matrix.android.sdk.internal.crypto.verification.VerificationIntent

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationActorTest {

    val transportScope = CoroutineScope(SupervisorJob())
//    val actorAScope = CoroutineScope(SupervisorJob())
//    val actorBScope = CoroutineScope(SupervisorJob())

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

//    @Test
//    fun `Every testing`() {
//        val mockStore = mockk<IMXCryptoStore>()
//        every { mockStore.getDeviceId() } returns "A"
//        println("every ${mockStore.getDeviceId()}")
//        every { mockStore.getDeviceId() } returns "B"
//        println("every ${mockStore.getDeviceId()}")
//
//        every { mockStore.getDeviceId() } returns "A"
//        every { mockStore.getDeviceId() } returns "B"
//        println("every ${mockStore.getDeviceId()}")
//
//        every { mockStore.getCrossSigningInfo(any()) } returns null
//        every { mockStore.getCrossSigningInfo("alice") } returns MXCrossSigningInfo("alice", emptyList(), false)
//
//        println("XS ${mockStore.getCrossSigningInfo("alice")}")
//        println("XS ${mockStore.getCrossSigningInfo("bob")}")
//    }

//    @Test
//    fun `Basic channel test`() {
// //        val sharedFlow = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 2, BufferOverflow.DROP_OLDEST)
//        val sharedFlow = MutableSharedFlow<Int>(replay = 0)//, extraBufferCapacity = 0, BufferOverflow.DROP_OLDEST)
//
//        val scope = CoroutineScope(SupervisorJob())
//        val deferred = CompletableDeferred<Unit>()
//        val listener = scope.launch {
//            sharedFlow.onEach {
//                println("L1 : Just collected $it")
//                delay(1000)
//                println("L1 : Just processed $it")
//                if (it == 2) {
//                    deferred.complete(Unit)
//                }
//            }.launchIn(scope)
//        }
//
// //        scope.launch {
// //            delay(700)
//        println("Pre Emit 1")
//        sharedFlow.tryEmit(1)
//        println("Emited 1")
//        sharedFlow.tryEmit(2)
//        println("Emited 2")
// //        }
//
// //        runBlocking {
// //            deferred.await()
// //        }
//
//        sharedFlow.onEach {
//            println("L2: Just collected $it")
//            delay(1000)
//            println("L2: Just processed $it")
//        }.launchIn(scope)
//
//
//        runBlocking {
//            deferred.await()
//        }
//
//        val now = System.currentTimeMillis()
//        println("Just give some time for execution")
//        val job = scope.launch { delay(10_000) }
//        runBlocking {
//            job.join()
//        }
//        println("enough ${System.currentTimeMillis() - now}")
//    }
}
