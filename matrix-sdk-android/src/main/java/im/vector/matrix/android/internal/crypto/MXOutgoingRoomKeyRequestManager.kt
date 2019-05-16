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

import android.os.Handler
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyShareCancellation
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyShareRequest
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import java.util.*

internal class MXOutgoingRoomKeyRequestManager(
        private val mCryptoStore: IMXCryptoStore,
        private val mSendToDeviceTask: SendToDeviceTask,
        private val mTaskExecutor: TaskExecutor) {

    // working handler (should not be the UI thread)
    private lateinit var mWorkingHandler: Handler

    // running
    var mClientRunning: Boolean = false

    // transaction counter
    private var mTxnCtr: Int = 0

    // sanity check to ensure that we don't end up with two concurrent runs
    // of mSendOutgoingRoomKeyRequestsTimer
    private var mSendOutgoingRoomKeyRequestsRunning: Boolean = false

    fun setWorkingHandler(encryptingThreadHandler: Handler) {
        mWorkingHandler = encryptingThreadHandler
    }

    /**
     * Called when the client is started. Sets background processes running.
     */
    fun start() {
        mClientRunning = true
        startTimer()
    }

    /**
     * Called when the client is stopped. Stops any running background processes.
     */
    fun stop() {
        mClientRunning = false
    }

    /**
     * Make up a new transaction id
     *
     * @return {string} a new, unique, transaction id
     */
    private fun makeTxnId(): String {
        return "m" + System.currentTimeMillis() + "." + mTxnCtr++
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
        mWorkingHandler.post {
            val req = mCryptoStore.getOrAddOutgoingRoomKeyRequest(
                    OutgoingRoomKeyRequest(requestBody, recipients, makeTxnId(), OutgoingRoomKeyRequest.RequestState.UNSENT))


            if (req!!.mState === OutgoingRoomKeyRequest.RequestState.UNSENT) {
                startTimer()
            }
        }
    }

    /**
     * Cancel room key requests, if any match the given details
     *
     * @param requestBody requestBody
     */
    fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        cancelRoomKeyRequest(requestBody, false)
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     */
    fun resendRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        cancelRoomKeyRequest(requestBody, true)
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     * @param andResend   true to resend the key request
     */
    private fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody, andResend: Boolean) {
        val req = mCryptoStore.getOutgoingRoomKeyRequest(requestBody)
                ?: // no request was made for this key
                return

        Timber.d("cancelRoomKeyRequest: requestId: " + req.mRequestId + " state: " + req.mState + " andResend: " + andResend)

        if (req.mState === OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING || req.mState === OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND) {
            // nothing to do here
        } else if (req.mState === OutgoingRoomKeyRequest.RequestState.UNSENT || req.mState === OutgoingRoomKeyRequest.RequestState.FAILED) {
            Timber.d("## cancelRoomKeyRequest() : deleting unnecessary room key request for $requestBody")
            mCryptoStore.deleteOutgoingRoomKeyRequest(req.mRequestId)
        } else if (req.mState === OutgoingRoomKeyRequest.RequestState.SENT) {
            if (andResend) {
                req.mState = OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND
            } else {
                req.mState = OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING
            }
            req.mCancellationTxnId = makeTxnId()
            mCryptoStore.updateOutgoingRoomKeyRequest(req)
            sendOutgoingRoomKeyRequestCancellation(req)
        }
    }


    /**
     * Start the background timer to send queued requests, if the timer isn't already running.
     */
    private fun startTimer() {
        mWorkingHandler.post(Runnable {
            if (mSendOutgoingRoomKeyRequestsRunning) {
                return@Runnable
            }

            mWorkingHandler.postDelayed(Runnable {
                if (mSendOutgoingRoomKeyRequestsRunning) {
                    Timber.d("## startTimer() : RoomKeyRequestSend already in progress!")
                    return@Runnable
                }

                mSendOutgoingRoomKeyRequestsRunning = true
                sendOutgoingRoomKeyRequests()
            }, SEND_KEY_REQUESTS_DELAY_MS.toLong())
        })
    }

    // look for and send any queued requests. Runs itself recursively until
    // there are no more requests, or there is an error (in which case, the
    // timer will be restarted before the promise resolves).
    private fun sendOutgoingRoomKeyRequests() {
        if (!mClientRunning) {
            mSendOutgoingRoomKeyRequestsRunning = false
            return
        }

        Timber.d("## sendOutgoingRoomKeyRequests() :  Looking for queued outgoing room key requests")
        val outgoingRoomKeyRequest = mCryptoStore.getOutgoingRoomKeyRequestByState(
                HashSet<OutgoingRoomKeyRequest.RequestState>(Arrays.asList<OutgoingRoomKeyRequest.RequestState>(OutgoingRoomKeyRequest.RequestState.UNSENT,
                        OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING,
                        OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND)))

        if (null == outgoingRoomKeyRequest) {
            Timber.e("## sendOutgoingRoomKeyRequests() : No more outgoing room key requests")
            mSendOutgoingRoomKeyRequestsRunning = false
            return
        }

        if (OutgoingRoomKeyRequest.RequestState.UNSENT === outgoingRoomKeyRequest.mState) {
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
        Timber.d("## sendOutgoingRoomKeyRequest() : Requesting keys " + request.mRequestBody
                + " from " + request.mRecipients + " id " + request.mRequestId)

        val requestMessage = RoomKeyShareRequest()
        requestMessage.requestingDeviceId = mCryptoStore.getDeviceId()
        requestMessage.requestId = request.mRequestId
        requestMessage.body = request.mRequestBody

        sendMessageToDevices(requestMessage, request.mRecipients, request.mRequestId, object : MatrixCallback<Unit> {
            private fun onDone(state: OutgoingRoomKeyRequest.RequestState) {
                mWorkingHandler.post {
                    if (request.mState !== OutgoingRoomKeyRequest.RequestState.UNSENT) {
                        Timber.d("## sendOutgoingRoomKeyRequest() : Cannot update room key request from UNSENT as it was already updated to " + request.mState)
                    } else {
                        request.mState = state
                        mCryptoStore.updateOutgoingRoomKeyRequest(request)
                    }

                    mSendOutgoingRoomKeyRequestsRunning = false
                    startTimer()
                }
            }

            override fun onSuccess(data: Unit) {
                Timber.d("## sendOutgoingRoomKeyRequest succeed")
                onDone(OutgoingRoomKeyRequest.RequestState.SENT)
            }

            override fun onFailure(failure: Throwable) {
                Timber.e("## sendOutgoingRoomKeyRequest failed")
                onDone(OutgoingRoomKeyRequest.RequestState.FAILED)
            }
        })
    }

    /**
     * Given a OutgoingRoomKeyRequest, cancel it and delete the request record
     *
     * @param request the request
     */
    private fun sendOutgoingRoomKeyRequestCancellation(request: OutgoingRoomKeyRequest) {
        Timber.d("## sendOutgoingRoomKeyRequestCancellation() : Sending cancellation for key request for " + request.mRequestBody
                + " to " + request.mRecipients
                + " cancellation id  " + request.mCancellationTxnId)

        val roomKeyShareCancellation = RoomKeyShareCancellation()
        roomKeyShareCancellation.requestingDeviceId = mCryptoStore.getDeviceId()
        roomKeyShareCancellation.requestId = request.mCancellationTxnId

        sendMessageToDevices(roomKeyShareCancellation, request.mRecipients, request.mCancellationTxnId, object : MatrixCallback<Unit> {
            private fun onDone() {
                mWorkingHandler.post {
                    mCryptoStore.deleteOutgoingRoomKeyRequest(request.mRequestId)
                    mSendOutgoingRoomKeyRequestsRunning = false
                    startTimer()
                }
            }


            override fun onSuccess(data: Unit) {
                Timber.d("## sendOutgoingRoomKeyRequestCancellation() : done")
                val resend = request.mState === OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND

                onDone()

                // Resend the request with a new ID
                if (resend) {
                    sendRoomKeyRequest(request.mRequestBody, request.mRecipients)
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
            contentMap.setObject(message, recipient["userId"], recipient["deviceId"])
        }

        mSendToDeviceTask.configureWith(SendToDeviceTask.Params(EventType.ROOM_KEY_REQUEST, contentMap, transactionId))
                .dispatchTo(callback)
                .executeBy(mTaskExecutor)
    }

    companion object {
        private const val SEND_KEY_REQUESTS_DELAY_MS = 500
    }
}
