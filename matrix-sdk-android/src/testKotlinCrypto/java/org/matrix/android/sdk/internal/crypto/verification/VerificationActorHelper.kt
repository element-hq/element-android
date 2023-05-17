/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationReadyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.ValidVerificationDone
import java.util.UUID

internal class VerificationActorHelper {

    data class TestData(
            val aliceActor: VerificationActor,
            val bobActor: VerificationActor,
            val aliceStore: FakeCryptoStoreForVerification,
            val bobStore: FakeCryptoStoreForVerification,
    )

    private val actorAScope = CoroutineScope(SupervisorJob())
    private val actorBScope = CoroutineScope(SupervisorJob())
    private val transportScope = CoroutineScope(SupervisorJob())

    private var bobChannel: SendChannel<VerificationIntent>? = null
    private var aliceChannel: SendChannel<VerificationIntent>? = null

    fun setUpActors(): TestData {
        val aliceTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.aliceMxId) { listOf(bobChannel) }
        val bobTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.bobMxId) { listOf(aliceChannel) }

        val fakeAliceStore = FakeCryptoStoreForVerification(StoreMode.Alice)
        val aliceActor = fakeActor(
                actorAScope,
                FakeCryptoStoreForVerification.aliceMxId,
                fakeAliceStore.instance,
                aliceTransportLayer,
        )
        aliceChannel = aliceActor.channel

        val fakeBobStore = FakeCryptoStoreForVerification(StoreMode.Bob)
        val bobActor = fakeActor(
                actorBScope,
                FakeCryptoStoreForVerification.bobMxId,
                fakeBobStore.instance,
                bobTransportLayer
        )
        bobChannel = bobActor.channel

        return TestData(
                aliceActor = aliceActor,
                bobActor = bobActor,
                aliceStore = fakeAliceStore,
                bobStore = fakeBobStore
        )
    }

//    fun setupMultipleSessions() {
//        val aliceTargetChannels = mutableListOf<Channel<VerificationIntent>>()
//        val aliceTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.aliceMxId) { aliceTargetChannels }
//        val bobTargetChannels = mutableListOf<Channel<VerificationIntent>>()
//        val bobTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.bobMxId) { bobTargetChannels }
//        val bob2TargetChannels = mutableListOf<Channel<VerificationIntent>>()
//        val bob2TransportLayer = mockTransportTo(FakeCryptoStoreForVerification.bobMxId) { bob2TargetChannels }
//
//        val fakeAliceStore = FakeCryptoStoreForVerification(StoreMode.Alice)
//        val aliceActor = fakeActor(
//                actorAScope,
//                FakeCryptoStoreForVerification.aliceMxId,
//                fakeAliceStore.instance,
//                aliceTransportLayer,
//        )
//
//        val fakeBobStore1 = FakeCryptoStoreForVerification(StoreMode.Bob)
//        val bobActor = fakeActor(
//                actorBScope,
//                FakeCryptoStoreForVerification.bobMxId,
//                fakeBobStore1.instance,
//                bobTransportLayer
//        )
//
//        val actorCScope = CoroutineScope(SupervisorJob())
//        val fakeBobStore2 = FakeCryptoStoreForVerification(StoreMode.Bob)
//        every { fakeBobStore2.instance.getMyDeviceId() } returns FakeCryptoStoreForVerification.bobDeviceId2
//        every { fakeBobStore2.instance.getMyDevice() } returns FakeCryptoStoreForVerification.aBobDevice2
//
//        val bobActor2 = fakeActor(
//                actorCScope,
//                FakeCryptoStoreForVerification.bobMxId,
//                fakeBobStore2.instance,
//                bobTransportLayer
//        )
//
//        aliceTargetChannels.add(bobActor.channel)
//        aliceTargetChannels.add(bobActor2.channel)
//
//        bobTargetChannels.add(aliceActor.channel)
//        bobTargetChannels.add(bobActor2.channel)
//
//        bob2TargetChannels.add(aliceActor.channel)
//        bob2TargetChannels.add(bobActor.channel)
//    }

    private fun mockTransportTo(fromUser: String, otherChannel: (() -> List<SendChannel<VerificationIntent>?>)): VerificationTransportLayer {
        return mockk {
            coEvery { sendToOther(any(), any(), any()) } answers {
                val request = firstArg<KotlinVerificationRequest>()
                val type = secondArg<String>()
                val info = thirdArg<VerificationInfo<*>>()

                transportScope.launch(Dispatchers.IO) {
                    when (type) {
                        EventType.KEY_VERIFICATION_READY -> {
                            val readyContent = info.asValidObject()
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnReadyReceived(
                                                transactionId = request.requestId,
                                                fromUser = fromUser,
                                                viaRoom = request.roomId,
                                                readyInfo = readyContent as ValidVerificationInfoReady,
                                        )
                                )
                            }
                        }
                        EventType.KEY_VERIFICATION_START -> {
                            val startContent = info.asValidObject()
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnStartReceived(
                                                fromUser = fromUser,
                                                viaRoom = request.roomId,
                                                validVerificationInfoStart = startContent as ValidVerificationInfoStart,
                                        )
                                )
                            }
                        }
                        EventType.KEY_VERIFICATION_ACCEPT -> {
                            val content = info.asValidObject()
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnAcceptReceived(
                                                fromUser = fromUser,
                                                viaRoom = request.roomId,
                                                validAccept = content as ValidVerificationInfoAccept,
                                        )
                                )
                            }
                        }
                        EventType.KEY_VERIFICATION_KEY -> {
                            val content = info.asValidObject()
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnKeyReceived(
                                                fromUser = fromUser,
                                                viaRoom = request.roomId,
                                                validKey = content as ValidVerificationInfoKey,
                                        )
                                )
                            }
                        }
                        EventType.KEY_VERIFICATION_MAC -> {
                            val content = info.asValidObject()
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnMacReceived(
                                                fromUser = fromUser,
                                                viaRoom = request.roomId,
                                                validMac = content as ValidVerificationInfoMac,
                                        )
                                )
                            }
                        }
                        EventType.KEY_VERIFICATION_DONE -> {
                            val content = info.asValidObject()
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnDoneReceived(
                                                fromUser = fromUser,
                                                viaRoom = request.roomId,
                                                transactionId = (content as ValidVerificationDone).transactionId,
                                        )
                                )
                            }
                        }
                    }
                }
            }
            coEvery { sendInRoom(any(), any(), any()) } answers {
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
                            otherChannel().onEach {
                                it?.send(
                                        VerificationIntent.OnVerificationRequestReceived(
                                                requestContent!!,
                                                senderId = FakeCryptoStoreForVerification.aliceMxId,
                                                roomId = roomId,
                                                timeStamp = 0
                                        )
                                )
                            }
                        }
                        EventType.KEY_VERIFICATION_READY -> {
                            val readyContent = content.toModel<MessageVerificationReadyContent>()
                                    ?.asValidObject()
                            otherChannel().onEach {
                                it?.send(
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
                }
                fakeEventId
            }
        }
    }

    private fun fakeActor(
            scope: CoroutineScope,
            userId: String,
            cryptoStore: VerificationTrustBackend,
            transportLayer: VerificationTransportLayer,
    ): VerificationActor {
        return VerificationActor(
                scope,
                clock = mockk {
                    every { epochMillis() } returns System.currentTimeMillis()
                },
                myUserId = userId,
                verificationTrustBackend = cryptoStore,
                secretShareManager = mockk {},
                transportLayer = transportLayer,
                verificationRequestsStore = VerificationRequestsStore(),
                olmPrimitiveProvider = mockk {
                    every { provideOlmSas() } returns mockk {
                        every { publicKey } returns "Tm9JRGVhRmFrZQo="
                        every { setTheirPublicKey(any()) } returns Unit
                        every { generateShortCode(any(), any()) } returns byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                        every { calculateMac(any(), any()) } returns "mic mac mec"
                    }
                    every { sha256(any()) } returns "fake_hash"
                }
        )
    }
}
