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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.sync.handler.ShieldSummaryUpdater
import org.matrix.rustcomponents.sdk.crypto.Request
import org.matrix.rustcomponents.sdk.crypto.RequestType
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("OutgoingRequestsProcessor", LoggerTag.CRYPTO)

@SessionScope
internal class OutgoingRequestsProcessor @Inject constructor(
        private val requestSender: RequestSender,
        private val coroutineScope: CoroutineScope,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val shieldSummaryUpdater: ShieldSummaryUpdater,
        private val matrixConfiguration: MatrixConfiguration,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
) {

    private val lock: Mutex = Mutex()

    suspend fun processOutgoingRequests(olmMachine: OlmMachine,
                                        filter: (Request) -> Boolean = { true }
    ): Boolean {
        return lock.withLock {
            coroutineScope {
                val outgoingRequests = olmMachine.outgoingRequests()
                val filteredOutgoingRequests = outgoingRequests.filter(filter)
                Timber.v("OutgoingRequests to process: $filteredOutgoingRequests}")
                filteredOutgoingRequests.map {
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
                                true
                            }
                        }
                    }
                }.awaitAll().all { it }
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

    private suspend fun uploadKeys(olmMachine: OlmMachine, request: Request.KeysUpload): Boolean {
        return try {
            val response = requestSender.uploadKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_UPLOAD, response)
            true
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## uploadKeys(): error")
            false
        }
    }

    private suspend fun queryKeys(olmMachine: OlmMachine, request: Request.KeysQuery): Boolean {
        return try {
            val response = requestSender.queryKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_QUERY, response)
            shieldSummaryUpdater.refreshShieldsForRoomsWithMembers(request.users)
            coroutineScope.markMessageVerificationStatesAsDirty(request.users)
            true
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## queryKeys(): error")
            false
        }
    }

    private fun CoroutineScope.markMessageVerificationStatesAsDirty(userIds: List<String>) = launch(coroutineDispatchers.computation) {
        cryptoSessionInfoProvider.markMessageVerificationStateAsDirty(userIds)
    }

    private suspend fun sendToDevice(olmMachine: OlmMachine, request: Request.ToDevice): Boolean {
        return try {
            requestSender.sendToDevice(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.TO_DEVICE, "{}")
            true
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## sendToDevice(): error")
            matrixConfiguration.cryptoAnalyticsPlugin?.onFailToSendToDevice(throwable)
            false
        }
    }

    private suspend fun claimKeys(olmMachine: OlmMachine, request: Request.KeysClaim): Boolean {
        return try {
            val response = requestSender.claimKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_CLAIM, response)
            true
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## claimKeys(): error")
            false
        }
    }

    private suspend fun signatureUpload(olmMachine: OlmMachine, request: Request.SignatureUpload): Boolean {
        return try {
            val response = requestSender.sendSignatureUpload(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.SIGNATURE_UPLOAD, response)
            true
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## signatureUpload(): error")
            false
        }
    }

    private suspend fun sendRoomMessage(olmMachine: OlmMachine, request: Request.RoomMessage): Boolean {
        return try {
            val response = requestSender.sendRoomMessage(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.ROOM_MESSAGE, response)
            true
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## sendRoomMessage(): error")
            false
        }
    }
}
