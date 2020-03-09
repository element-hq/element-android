/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.startChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
internal class OutgoingGossipingRequestManager @Inject constructor(
        @SessionId private val sessionId: String,
        private val cryptoStore: IMXCryptoStore,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
        private val workManagerProvider: WorkManagerProvider) {

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
                    Timber.v("## sendOutgoingRoomKeyRequest() : we already request for that session: $it")
                    return@launch
                }

                sendOutgoingGossipingRequest(it)
            }
        }
    }

    fun sendSecretShareRequest(secretName: String, recipients: Map<String, List<String>>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            cryptoStore.getOrAddOutgoingSecretShareRequest(secretName, recipients)?.let {
                // TODO check if there is already one that is being sent?
                if (it.state == OutgoingGossipingRequestState.SENDING || it.state == OutgoingGossipingRequestState.SENT) {
                    Timber.v("## sendOutgoingRoomKeyRequest() : we already request for that session: $it")
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
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            cancelRoomKeyRequest(requestBody, false)
        }
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     */
    fun resendRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
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
        val req = cryptoStore.getOutgoingRoomKeyRequest(requestBody)
                ?: // no request was made for this key
                return Unit.also {
                    Timber.v("## cancelRoomKeyRequest() Unknown request")
                }

        sendOutgoingRoomKeyRequestCancellation(req, andResend)
    }

    /**
     * Send the outgoing key request.
     *
     * @param request the request
     */
    private fun sendOutgoingGossipingRequest(request: OutgoingGossipingRequest) {
        Timber.v("## sendOutgoingRoomKeyRequest() : Requesting keys $request")

        val params = SendGossipRequestWorker.Params(
                sessionId = sessionId,
                keyShareRequest = request as? OutgoingRoomKeyRequest,
                secretShareRequest = request as? OutgoingSecretRequest
        )
        cryptoStore.updateOutgoingGossipingRequestState(request.requestId, OutgoingGossipingRequestState.SENDING)
        val workRequest = createWork<SendGossipRequestWorker>(WorkerParamsFactory.toData(params), true)
        postWork(workRequest)
    }

    /**
     * Given a OutgoingRoomKeyRequest, cancel it and delete the request record
     *
     * @param request the request
     */
    private fun sendOutgoingRoomKeyRequestCancellation(request: OutgoingRoomKeyRequest, resend: Boolean = false) {
        Timber.v("$request")
        val params = CancelGossipRequestWorker.Params.fromRequest(sessionId, request)
        cryptoStore.updateOutgoingGossipingRequestState(request.requestId, OutgoingGossipingRequestState.CANCELLING)

        val workRequest = createWork<CancelGossipRequestWorker>(WorkerParamsFactory.toData(params), true)
        postWork(workRequest)

        if (resend) {
            val reSendParams = SendGossipRequestWorker.Params(
                    sessionId = sessionId,
                    keyShareRequest = request.copy(requestId = LocalEcho.createLocalEchoId())
            )
            val reSendWorkRequest = createWork<SendGossipRequestWorker>(WorkerParamsFactory.toData(reSendParams), true)
            postWork(reSendWorkRequest)
        }
    }

    private inline fun <reified W : ListenableWorker> createWork(data: Data, startChain: Boolean): OneTimeWorkRequest {
        return workManagerProvider.matrixOneTimeWorkRequestBuilder<W>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .startChain(startChain)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000L, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun postWork(workRequest: OneTimeWorkRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND): Cancelable {
        workManagerProvider.workManager
                .beginUniqueWork(this::class.java.name, policy, workRequest)
                .enqueue()

        return CancelableWork(workManagerProvider.workManager, workRequest.id)
    }
}
