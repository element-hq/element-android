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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.IVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationReadyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.internal.crypto.SecretShareManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.verification.FakeCryptoStoreForVerification
import org.matrix.android.sdk.internal.crypto.verification.StoreMode
import org.matrix.android.sdk.internal.crypto.verification.VerificationActor
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfo
import org.matrix.android.sdk.internal.crypto.verification.VerificationIntent
import org.matrix.android.sdk.internal.crypto.verification.VerificationTransportLayer
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationActorTest {

    val transportScope = CoroutineScope(SupervisorJob())
    val actorAScope = CoroutineScope(SupervisorJob())
    val actorBScope = CoroutineScope(SupervisorJob())

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
    fun `Request and accept`() = runTest {
        var bobChannel: SendChannel<VerificationIntent>? = null
        var aliceChannel: SendChannel<VerificationIntent>? = null

        val aliceTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.aliceMxId) { bobChannel }
        val bobTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.bobMxId) { aliceChannel }

        val aliceActor = fakeActor(
                actorAScope,
                FakeCryptoStoreForVerification.aliceMxId,
                FakeCryptoStoreForVerification(StoreMode.Alice).instance,
                aliceTransportLayer,
                mockk<dagger.Lazy<CrossSigningService>> {
                    every {
                        get()
                    } returns mockk<CrossSigningService>(relaxed = true)
                }
        )
        aliceChannel = aliceActor.channel

        val bobActor = fakeActor(
                actorBScope,
                FakeCryptoStoreForVerification.aliceMxId,
                FakeCryptoStoreForVerification(StoreMode.Alice).instance,
                bobTransportLayer,
                mockk<dagger.Lazy<CrossSigningService>> {
                    every {
                        get()
                    } returns mockk<CrossSigningService>(relaxed = true)
                }
        )
        bobChannel = bobActor.channel

        val completableDeferred = CompletableDeferred<PendingVerificationRequest>()

        transportScope.launch {
            bobActor.eventFlow.collect {
                if (it is VerificationEvent.RequestAdded) {
                    completableDeferred.complete(it.request)
                    return@collect cancel()
                }
            }
        }

        awaitDeferrable<PendingVerificationRequest> {
            aliceActor.send(
                    VerificationIntent.ActionRequestVerification(
                            otherUserId = FakeCryptoStoreForVerification.bobMxId,
                            roomId = "aRoom",
                            methods = listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SHOW, VerificationMethod.QR_CODE_SCAN),
                            deferred = it
                    )
            )
        }

        val bobIncomingRequest = completableDeferred.await()
        bobIncomingRequest.state shouldBeEqualTo EVerificationState.Requested

        val aliceReadied = CompletableDeferred<PendingVerificationRequest>()

        val theJob = transportScope.launch {
            aliceActor.eventFlow.collect {
                if (it is VerificationEvent.RequestUpdated && it.request.state == EVerificationState.Ready) {
                    aliceReadied.complete(it.request)
                    return@collect cancel()
                }
            }
        }

        // test ready
        awaitDeferrable<PendingVerificationRequest?> {
            bobActor.send(
                    VerificationIntent.ActionReadyRequest(
                            bobIncomingRequest.transactionId,
                            methods = listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SHOW, VerificationMethod.QR_CODE_SCAN),
                            it
                    )
            )
        }

        val readiedAliceSide = aliceReadied.await()

        println("transporte scope active? ${transportScope.isActive}")
        println("the job? ${theJob.isActive}")

        readiedAliceSide.isSasSupported shouldBeEqualTo true
        readiedAliceSide.otherCanScanQrCode shouldBeEqualTo true
    }

    private suspend fun <T> awaitDeferrable(block: suspend ((CompletableDeferred<T>) -> Unit)): T {
        val deferred = CompletableDeferred<T>()
        block.invoke(deferred)
        return deferred.await()
    }

    private fun mockTransportTo(fromUser: String, otherChannel: (() -> SendChannel<VerificationIntent>?)): VerificationTransportLayer {
        return mockk<VerificationTransportLayer> {
            coEvery { sendToOther(any(), any(), any()) } answers {
                val request = firstArg<IVerificationRequest>()
                val type = secondArg<String>()
                val info = thirdArg<VerificationInfo<*>>()

                transportScope.launch(Dispatchers.IO) {
                    when (type) {
                        EventType.KEY_VERIFICATION_READY -> {
                            val readyContent = info.asValidObject()
                            otherChannel()?.send(
                                    VerificationIntent.OnReadyReceived(
                                            transactionId = request.requestId(),
                                            fromUser = fromUser,
                                            viaRoom = request.roomId(),
                                            readyInfo = readyContent as ValidVerificationInfoReady,
                                    )
                            )
                        }
                    }
                }
            }
            coEvery { sendInRoom(any(), any(), any(), any()) } answers {
                val type = secondArg<String>()
                val roomId = thirdArg<String>()
                val content = arg<Content>(3)

                val fakeEventId = UUID.randomUUID().toString()
                transportScope.launch(Dispatchers.IO) {
                    when (type) {
                        EventType.MESSAGE -> {
                            val requestContent = content.toModel<MessageVerificationRequestContent>()?.copy(
                                    transactionId = fakeEventId
                            )?.asValidObject()
                            otherChannel()?.send(
                                    VerificationIntent.OnVerificationRequestReceived(
                                            requestContent!!,
                                            senderId = FakeCryptoStoreForVerification.aliceMxId,
                                            roomId = roomId,
                                            timeStamp = 0
                                    )
                            )
                        }
                        EventType.KEY_VERIFICATION_READY -> {
                            val readyContent = content.toModel<MessageVerificationReadyContent>()
                                    ?.asValidObject()
                            otherChannel()?.send(
                                    VerificationIntent.OnReadyReceived(
                                            transactionId = readyContent!!.transactionId,
                                            fromUser = fromUser,
                                            viaRoom = roomId,
                                            readyInfo = readyContent,
                                    )
                            )
                        }
                    }
                }
                fakeEventId
            }
        }
    }

    @Test
    fun `Every testing`() {
        val mockStore = mockk<IMXCryptoStore>()
        every { mockStore.getDeviceId() } returns "A"
        println("every ${mockStore.getDeviceId()}")
        every { mockStore.getDeviceId() } returns "B"
        println("every ${mockStore.getDeviceId()}")

        every { mockStore.getDeviceId() } returns "A"
        every { mockStore.getDeviceId() } returns "B"
        println("every ${mockStore.getDeviceId()}")

        every { mockStore.getCrossSigningInfo(any()) } returns null
        every { mockStore.getCrossSigningInfo("alice") } returns MXCrossSigningInfo("alice", emptyList(), false)

        println("XS ${mockStore.getCrossSigningInfo("alice")}")
        println("XS ${mockStore.getCrossSigningInfo("bob")}")
    }

    private fun fakeActor(
            scope: CoroutineScope,
            userId: String,
            cryptoStore: IMXCryptoStore,
            transportLayer: VerificationTransportLayer,
            crossSigningService: dagger.Lazy<CrossSigningService>,
    ): VerificationActor {
        return VerificationActor(
                scope,
//                channel = channel,
                clock = mockk<Clock> {
                    every { epochMillis() } returns System.currentTimeMillis()
                },
                myUserId = userId,
                cryptoStore = cryptoStore,
                secretShareManager = mockk<SecretShareManager> {},
                transportLayer = transportLayer,
                crossSigningService = crossSigningService,
                setDeviceVerificationAction = SetDeviceVerificationAction(
                        cryptoStore = cryptoStore,
                        userId = userId,
                        defaultKeysBackupService = mockk {
                            coEvery { checkAndStartKeysBackup() } coAnswers { }
                        }
                )
        )
    }
}
