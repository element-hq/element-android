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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.crypto.model.OutgoingGossipingRequestState
import org.matrix.android.sdk.api.session.crypto.model.OutgoingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.createUniqueTxnId
import org.matrix.android.sdk.internal.crypto.util.RequestIdHelper
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class OutgoingGossipingRequestManager @Inject constructor(
        @SessionId private val sessionId: String,
        private val cryptoStore: IMXCryptoStore,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
        private val gossipingWorkManager: GossipingWorkManager) {

    /**
     * Send off a room key request, if we haven't already done so.
     *
     *
     * The `requestBody` is compared (with a deep-equality check) against
     * previous queued or sent requests and if it matches, no change is made.
     * Otherwise, a request is added to the pending list, and a job is started
     * in the background to send it.
     *
     * @param requestBody requestBody
     * @param recipients  recipients
     */
    fun sendRoomKeyRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            cryptoStore.getOrAddOutgoingRoomKeyRequest(requestBody, recipients)?.let {
                // Don't resend if it's already done, you need to cancel first (reRequest)
                if (it.state == OutgoingGossipingRequestState.SENDING || it.state == OutgoingGossipingRequestState.SENT) {
                    Timber.v("## CRYPTO - GOSSIP sendOutgoingRoomKeyRequest() : we already request for that session: $it")
                    return@launch
                }

                sendOutgoingGossipingRequest(it)
            }
        }
    }

    fun sendSecretShareRequest(secretName: String, recipients: Map<String, List<String>>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            // A bit dirty, but for better stability give other party some time to mark
            // devices trusted :/
            delay(1500)
            cryptoStore.getOrAddOutgoingSecretShareRequest(secretName, recipients)?.let {
                // TODO check if there is already one that is being sent?
                if (it.state == OutgoingGossipingRequestState.SENDING
                /**|| it.state == OutgoingGossipingRequestState.SENT*/
                ) {
                    Timber.v("## CRYPTO - GOSSIP sendSecretShareRequest() : we are already sending for that session: $it")
                    return@launch
                }

                sendOutgoingGossipingRequest(it)
            }
        }
    }

    /**
     * Cancel room key requests, if any match the given details
     *
     * @param requestBody requestBody
     */
    fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        cryptoCoroutineScope.launch(coroutineDispatchers.computation) {
            cancelRoomKeyRequest(requestBody, false)
        }
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     */
    fun resendRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        cryptoCoroutineScope.launch(coroutineDispatchers.computation) {
            cancelRoomKeyRequest(requestBody, true)
        }
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     * @param andResend   true to resend the key request
     */
    private fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody, andResend: Boolean) {
        val req = cryptoStore.getOutgoingRoomKeyRequest(requestBody) // no request was made for this key
                ?: return Unit.also {
                    Timber.v("## CRYPTO - GOSSIP cancelRoomKeyRequest() Unknown request $requestBody")
                }

        sendOutgoingRoomKeyRequestCancellation(req, andResend)
    }

    /**
     * Send the outgoing key request.
     *
     * @param request the request
     */
    private fun sendOutgoingGossipingRequest(request: OutgoingGossipingRequest) {
        Timber.v("## CRYPTO - GOSSIP sendOutgoingGossipingRequest() : Requesting keys $request")

        val params = SendGossipRequestWorker.Params(
                sessionId = sessionId,
                keyShareRequest = request as? OutgoingRoomKeyRequest,
                secretShareRequest = request as? OutgoingSecretRequest,
                txnId = createUniqueTxnId()
        )
        cryptoStore.updateOutgoingGossipingRequestState(request.requestId, OutgoingGossipingRequestState.SENDING)
        val workRequest = gossipingWorkManager.createWork<SendGossipRequestWorker>(WorkerParamsFactory.toData(params), true)
        gossipingWorkManager.postWork(workRequest)
    }

    /**
     * Given a OutgoingRoomKeyRequest, cancel it and delete the request record
     *
     * @param request the request
     */
    private fun sendOutgoingRoomKeyRequestCancellation(request: OutgoingRoomKeyRequest, resend: Boolean = false) {
        Timber.v("## CRYPTO - sendOutgoingRoomKeyRequestCancellation $request")
        val params = CancelGossipRequestWorker.Params.fromRequest(sessionId, request)
        cryptoStore.updateOutgoingGossipingRequestState(request.requestId, OutgoingGossipingRequestState.CANCELLING)

        val workRequest = gossipingWorkManager.createWork<CancelGossipRequestWorker>(WorkerParamsFactory.toData(params), true)
        gossipingWorkManager.postWork(workRequest)

        if (resend) {
            val reSendParams = SendGossipRequestWorker.Params(
                    sessionId = sessionId,
                    keyShareRequest = request.copy(requestId = RequestIdHelper.createUniqueRequestId()),
                    txnId = createUniqueTxnId()
            )
            val reSendWorkRequest = gossipingWorkManager.createWork<SendGossipRequestWorker>(WorkerParamsFactory.toData(reSendParams), true)
            gossipingWorkManager.postWork(reSendWorkRequest)
        }
    }
}
