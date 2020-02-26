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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.model.rest.ShareRequestCancellation
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyShareRequest
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.createBackgroundHandler
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@SessionScope
internal class OutgoingRoomKeyRequestManager @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
        private val sendToDeviceTask: SendToDeviceTask,
        private val taskExecutor: TaskExecutor) {

    // running
    private var isClientRunning: Boolean = false

    // transaction counter
    private var txnCtr: Int = 0

    // sanity check to ensure that we don't end up with two concurrent runs
    // of sendOutgoingRoomKeyRequestsTimer
    private val sendOutgoingRoomKeyRequestsRunning = AtomicBoolean(false)

    /**
     * Called when the client is started. Sets background processes running.
     */
    fun start() {
        isClientRunning = true
        startTimer()
    }

    /**
     * Called when the client is stopped. Stops any running background processes.
     */
    fun stop() {
        isClientRunning = false
        stopTimer()
    }

    /**
     * Make up a new transaction id
     *
     * @return {string} a new, unique, transaction id
     */
    private fun makeTxnId(): String {
        return "m" + System.currentTimeMillis() + "." + txnCtr++
    }

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
    fun sendRoomKeyRequest(requestBody: RoomKeyRequestBody?, recipients: List<Map<String, String>>) {
        val req = cryptoStore.getOrAddOutgoingRoomKeyRequest(
                OutgoingRoomKeyRequest(requestBody, recipients, makeTxnId(), ShareRequestState.UNSENT))

        if (req?.state == ShareRequestState.UNSENT) {
            startTimer()
        }
    }

    /**
     * Cancel room key requests, if any match the given details
     *
     * @param requestBody requestBody
     */
    fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        BACKGROUND_HANDLER.post {
            cancelRoomKeyRequest(requestBody, false)
        }
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     */
    fun resendRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        BACKGROUND_HANDLER.post {
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
                return

        Timber.v("cancelRoomKeyRequest: requestId: " + req.requestId + " state: " + req.state + " andResend: " + andResend)

        when (req.state) {
            ShareRequestState.CANCELLATION_PENDING,
            ShareRequestState.CANCELLATION_PENDING_AND_WILL_RESEND -> {
                // nothing to do here
            }
            ShareRequestState.UNSENT,
            ShareRequestState.FAILED                               -> {
                Timber.v("## cancelRoomKeyRequest() : deleting unnecessary room key request for $requestBody")
                cryptoStore.deleteOutgoingRoomKeyRequest(req.requestId)
            }
            ShareRequestState.SENT                                 -> {
                if (andResend) {
                    req.state = ShareRequestState.CANCELLATION_PENDING_AND_WILL_RESEND
                } else {
                    req.state = ShareRequestState.CANCELLATION_PENDING
                }
                req.cancellationTxnId = makeTxnId()
                cryptoStore.updateOutgoingRoomKeyRequest(req)
                sendOutgoingRoomKeyRequestCancellation(req)
            }
        }
    }

    /**
     * Start the background timer to send queued requests, if the timer isn't already running.
     */
    private fun startTimer() {
        if (sendOutgoingRoomKeyRequestsRunning.get()) {
            return
        }
        BACKGROUND_HANDLER.postDelayed(Runnable {
            if (sendOutgoingRoomKeyRequestsRunning.get()) {
                Timber.v("## startTimer() : RoomKeyRequestSend already in progress!")
                return@Runnable
            }

            sendOutgoingRoomKeyRequestsRunning.set(true)
            sendOutgoingRoomKeyRequests()
        }, SEND_KEY_REQUESTS_DELAY_MS.toLong())
    }

    private fun stopTimer() {
        BACKGROUND_HANDLER.removeCallbacksAndMessages(null)
    }

    // look for and send any queued requests. Runs itself recursively until
    // there are no more requests, or there is an error (in which case, the
    // timer will be restarted before the promise resolves).
    private fun sendOutgoingRoomKeyRequests() {
        if (!isClientRunning) {
            sendOutgoingRoomKeyRequestsRunning.set(false)
            return
        }

        Timber.v("## sendOutgoingRoomKeyRequests() :  Looking for queued outgoing room key requests")
        val outgoingRoomKeyRequest = cryptoStore.getOutgoingRoomKeyRequestByState(
                setOf(ShareRequestState.UNSENT,
                        ShareRequestState.CANCELLATION_PENDING,
                        ShareRequestState.CANCELLATION_PENDING_AND_WILL_RESEND))

        if (null == outgoingRoomKeyRequest) {
            Timber.v("## sendOutgoingRoomKeyRequests() : No more outgoing room key requests")
            sendOutgoingRoomKeyRequestsRunning.set(false)
            return
        }

        if (ShareRequestState.UNSENT === outgoingRoomKeyRequest.state) {
            sendOutgoingRoomKeyRequest(outgoingRoomKeyRequest)
        } else {
            sendOutgoingRoomKeyRequestCancellation(outgoingRoomKeyRequest)
        }
    }

    /**
     * Send the outgoing key request.
     *
     * @param request the request
     */
    private fun sendOutgoingRoomKeyRequest(request: OutgoingRoomKeyRequest) {
        Timber.v("## sendOutgoingRoomKeyRequest() : Requesting keys " + request.requestBody
                + " from " + request.recipients + " id " + request.requestId)

        val requestMessage = RoomKeyShareRequest(
                requestingDeviceId = cryptoStore.getDeviceId(),
                requestId = request.requestId,
                body = request.requestBody
        )

        sendMessageToDevices(requestMessage, request.recipients, request.requestId, object : MatrixCallback<Unit> {
            private fun onDone(state: ShareRequestState) {
                if (request.state !== ShareRequestState.UNSENT) {
                    Timber.v("## sendOutgoingRoomKeyRequest() : Cannot update room key request from UNSENT as it was already updated to ${request.state}")
                } else {
                    request.state = state
                    cryptoStore.updateOutgoingRoomKeyRequest(request)
                }

                sendOutgoingRoomKeyRequestsRunning.set(false)
                startTimer()
            }

            override fun onSuccess(data: Unit) {
                Timber.v("## sendOutgoingRoomKeyRequest succeed")
                onDone(ShareRequestState.SENT)
            }

            override fun onFailure(failure: Throwable) {
                Timber.e("## sendOutgoingRoomKeyRequest failed")
                onDone(ShareRequestState.FAILED)
            }
        })
    }

    /**
     * Given a OutgoingRoomKeyRequest, cancel it and delete the request record
     *
     * @param request the request
     */
    private fun sendOutgoingRoomKeyRequestCancellation(request: OutgoingRoomKeyRequest) {
        Timber.v("## sendOutgoingRoomKeyRequestCancellation() : Sending cancellation for key request for " + request.requestBody
                + " to " + request.recipients
                + " cancellation id  " + request.cancellationTxnId)

        val roomKeyShareCancellation = ShareRequestCancellation(
                requestingDeviceId = cryptoStore.getDeviceId(),
                requestId = request.cancellationTxnId
        )

        sendMessageToDevices(roomKeyShareCancellation, request.recipients, request.cancellationTxnId, object : MatrixCallback<Unit> {
            private fun onDone() {
                cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
                sendOutgoingRoomKeyRequestsRunning.set(false)
                startTimer()
            }

            override fun onSuccess(data: Unit) {
                Timber.v("## sendOutgoingRoomKeyRequestCancellation() : done")
                val resend = request.state === ShareRequestState.CANCELLATION_PENDING_AND_WILL_RESEND

                onDone()

                // Resend the request with a new ID
                if (resend) {
                    sendRoomKeyRequest(request.requestBody, request.recipients)
                }
            }

            override fun onFailure(failure: Throwable) {
                Timber.e("## sendOutgoingRoomKeyRequestCancellation failed")
                onDone()
            }
        })
    }

    /**
     * Send a SendToDeviceObject to a list of recipients
     *
     * @param message       the message
     * @param recipients    the recipients.
     * @param transactionId the transaction id
     * @param callback      the asynchronous callback.
     */
    private fun sendMessageToDevices(message: Any,
                                     recipients: List<Map<String, String>>,
                                     transactionId: String?,
                                     callback: MatrixCallback<Unit>) {
        val contentMap = MXUsersDevicesMap<Any>()

        for (recipient in recipients) {
            // TODO Change this two hard coded key to something better
            contentMap.setObject(recipient["userId"], recipient["deviceId"], message)
        }
        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(EventType.ROOM_KEY_REQUEST, contentMap, transactionId)) {
                    this.callback = callback
                    this.callbackThread = TaskThread.CALLER
                    this.executionThread = TaskThread.CALLER
                }
                .executeBy(taskExecutor)
    }

    companion object {
        private const val SEND_KEY_REQUESTS_DELAY_MS = 500

        private val BACKGROUND_HANDLER = createBackgroundHandler("OutgoingRoomKeyRequest")
    }
}
