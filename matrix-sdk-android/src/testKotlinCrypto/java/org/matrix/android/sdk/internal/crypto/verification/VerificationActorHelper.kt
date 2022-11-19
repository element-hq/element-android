/*
 * Copyright (c) 2022 New Vector Ltd
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

import dagger.Lazy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationReadyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.internal.crypto.SecretShareManager
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.UUID

internal class VerificationActorHelper {

    data class TestData(
            val aliceActor: VerificationActor,
            val bobActor: VerificationActor,
    )

    val actorAScope = CoroutineScope(SupervisorJob())
    val actorBScope = CoroutineScope(SupervisorJob())
    val transportScope = CoroutineScope(SupervisorJob())

    var bobChannel: SendChannel<VerificationIntent>? = null
    var aliceChannel: SendChannel<VerificationIntent>? = null

    fun setUpActors(): TestData {
        val aliceTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.aliceMxId) { bobChannel }
        val bobTransportLayer = mockTransportTo(FakeCryptoStoreForVerification.bobMxId) { aliceChannel }

        val aliceActor = fakeActor(
                actorAScope,
                FakeCryptoStoreForVerification.aliceMxId,
                FakeCryptoStoreForVerification(StoreMode.Alice).instance,
                aliceTransportLayer,
                mockk<Lazy<CrossSigningService>> {
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
                mockk<Lazy<CrossSigningService>> {
                    every {
                        get()
                    } returns mockk<CrossSigningService>(relaxed = true)
                }
        )
        bobChannel = bobActor.channel

        return TestData(
                aliceActor,
                bobActor
        )
    }

    private fun mockTransportTo(fromUser: String, otherChannel: (() -> SendChannel<VerificationIntent>?)): VerificationTransportLayer {
        return mockk<VerificationTransportLayer> {
            coEvery { sendToOther(any(), any(), any()) } answers {
                val request = firstArg<KotlinVerificationRequest>()
                val type = secondArg<String>()
                val info = thirdArg<VerificationInfo<*>>()

                transportScope.launch(Dispatchers.IO) {
                    when (type) {
                        EventType.KEY_VERIFICATION_READY -> {
                            val readyContent = info.asValidObject()
                            otherChannel()?.send(
                                    VerificationIntent.OnReadyReceived(
                                            transactionId = request.requestId,
                                            fromUser = fromUser,
                                            viaRoom = request.roomId,
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
