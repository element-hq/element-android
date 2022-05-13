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

package org.matrix.android.sdk.internal.crypto.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.crypto.OlmMachine
import timber.log.Timber
import uniffi.olm.Request
import uniffi.olm.RequestType

private val loggerTag = LoggerTag("OutgoingRequestsProcessor", LoggerTag.CRYPTO)

internal class NetworkRequestsProcessor(private val requestSender: RequestSender,
                                        private val coroutineScope: CoroutineScope,
                                        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
                                        private val shieldComputer: ShieldComputer,) {

    fun interface ShieldComputer {
        suspend fun compute(userIds: List<String>): RoomEncryptionTrustLevel
    }

    private val lock: Mutex = Mutex()

    suspend fun processOutgoingRequests(olmMachine: OlmMachine) {
        lock.withLock {
            coroutineScope {
                Timber.v("OutgoingRequests: ${olmMachine.outgoingRequests()}")
                olmMachine.outgoingRequests().map {
                    when (it) {
                        is Request.KeysUpload      -> {
                            async {
                                uploadKeys(olmMachine, it)
                            }
                        }
                        is Request.KeysQuery       -> {
                            async {
                                queryKeys(olmMachine, it)
                            }
                        }
                        is Request.ToDevice        -> {
                            async {
                                sendToDevice(olmMachine, it)
                            }
                        }
                        is Request.KeysClaim       -> {
                            async {
                                claimKeys(olmMachine, it)
                            }
                        }
                        is Request.RoomMessage     -> {
                            async {
                                sendRoomMessage(olmMachine, it)
                            }
                        }
                        is Request.SignatureUpload -> {
                            async {
                                signatureUpload(olmMachine, it)
                            }
                        }
                        is Request.KeysBackup      -> {
                            async {
                                // The rust-sdk won't ever produce KeysBackup requests here,
                                // those only get explicitly created.
                            }
                        }
                    }
                }.joinAll()
            }
        }
    }

    suspend fun processRequestRoomKey(olmMachine: OlmMachine, event: Event) {
        val requestPair = olmMachine.requestRoomKey(event)
        val cancellation = requestPair.cancellation
        val request = requestPair.keyRequest

        when (cancellation) {
            is Request.ToDevice -> {
                sendToDevice(olmMachine, cancellation)
            }
            else                -> Unit
        }
        when (request) {
            is Request.ToDevice -> {
                sendToDevice(olmMachine, request)
            }
            else                -> Unit
        }
    }

    private suspend fun uploadKeys(olmMachine: OlmMachine, request: Request.KeysUpload) {
        try {
            val response = requestSender.uploadKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_UPLOAD, response)
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## uploadKeys(): error")
        }
    }

    private suspend fun queryKeys(olmMachine: OlmMachine, request: Request.KeysQuery) {
        try {
            val response = requestSender.queryKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_QUERY, response)
            coroutineScope.updateShields(request.users)
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## queryKeys(): error")
        }
    }

    private fun CoroutineScope.updateShields(userIds: List<String>) = launch {
        cryptoSessionInfoProvider.getRoomsWhereUsersAreParticipating(userIds).forEach { roomId ->
            val userGroup = cryptoSessionInfoProvider.getUserListForShieldComputation(roomId)
            val shield = shieldComputer.compute(userGroup)
            cryptoSessionInfoProvider.updateShieldForRoom(roomId, shield)
        }
    }

    private suspend fun sendToDevice(olmMachine: OlmMachine, request: Request.ToDevice) {
        try {
            requestSender.sendToDevice(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.TO_DEVICE, "{}")
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## sendToDevice(): error")
        }
    }

    private suspend fun claimKeys(olmMachine: OlmMachine, request: Request.KeysClaim) {
        try {
            val response = requestSender.claimKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_CLAIM, response)
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## claimKeys(): error")
        }
    }

    private suspend fun signatureUpload(olmMachine: OlmMachine, request: Request.SignatureUpload) {
        try {
            val response = requestSender.sendSignatureUpload(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.SIGNATURE_UPLOAD, response)
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## signatureUpload(): error")
        }
    }

    private suspend fun sendRoomMessage(olmMachine: OlmMachine, request: Request.RoomMessage) {
        try {
            val response = requestSender.sendRoomMessage(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.ROOM_MESSAGE, response)
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## sendRoomMessage(): error")
        }
    }
}
