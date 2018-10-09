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

package im.vector.matrix.android.internal.legacy.crypto;

import android.os.Handler;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.crypto.data.MXUsersDevicesMap;
import im.vector.matrix.android.internal.legacy.data.cryptostore.IMXCryptoStore;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyRequest;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MXOutgoingRoomKeyRequestManager {
    private static final String LOG_TAG = MXOutgoingRoomKeyRequestManager.class.getSimpleName();

    private static final int SEND_KEY_REQUESTS_DELAY_MS = 500;

    // the linked session
    private MXSession mSession;

    // working handler (should not be the UI thread)
    private Handler mWorkingHandler;

    // store
    private IMXCryptoStore mCryptoStore;

    // running
    public boolean mClientRunning;

    // transaction counter
    private int mTxnCtr;

    // sanity check to ensure that we don't end up with two concurrent runs
    // of mSendOutgoingRoomKeyRequestsTimer
    private boolean mSendOutgoingRoomKeyRequestsRunning;

    /**
     * Constructor
     *
     * @param session the session
     * @param crypto  the crypto engine
     */
    public MXOutgoingRoomKeyRequestManager(MXSession session, MXCrypto crypto) {
        mSession = session;
        mWorkingHandler = crypto.getEncryptingThreadHandler();
        mCryptoStore = crypto.getCryptoStore();
    }

    /**
     * Called when the client is started. Sets background processes running.
     */
    public void start() {
        mClientRunning = true;
        startTimer();
    }

    /**
     * Called when the client is stopped. Stops any running background processes.
     */
    public void stop() {
        mClientRunning = false;
    }

    /**
     * Make up a new transaction id
     *
     * @return {string} a new, unique, transaction id
     */
    private String makeTxnId() {
        return "m" + System.currentTimeMillis() + "." + mTxnCtr++;
    }

    /**
     * Send off a room key request, if we haven't already done so.
     * <p>
     * The `requestBody` is compared (with a deep-equality check) against
     * previous queued or sent requests and if it matches, no change is made.
     * Otherwise, a request is added to the pending list, and a job is started
     * in the background to send it.
     *
     * @param requestBody requestBody
     * @param recipients  recipients
     */
    public void sendRoomKeyRequest(final Map<String, String> requestBody, final List<Map<String, String>> recipients) {
        mWorkingHandler.post(new Runnable() {
            @Override
            public void run() {
                OutgoingRoomKeyRequest req = mCryptoStore.getOrAddOutgoingRoomKeyRequest(
                        new OutgoingRoomKeyRequest(requestBody, recipients, makeTxnId(), OutgoingRoomKeyRequest.RequestState.UNSENT));


                if (req.mState == OutgoingRoomKeyRequest.RequestState.UNSENT) {
                    startTimer();
                }
            }
        });
    }

    /**
     * Cancel room key requests, if any match the given details
     *
     * @param requestBody requestBody
     */
    public void cancelRoomKeyRequest(final Map<String, String> requestBody) {
        cancelRoomKeyRequest(requestBody, false);
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     */
    public void resendRoomKeyRequest(final Map<String, String> requestBody) {
        cancelRoomKeyRequest(requestBody, true);
    }

    /**
     * Cancel room key requests, if any match the given details, and resend
     *
     * @param requestBody requestBody
     * @param andResend   true to resend the key request
     */
    private void cancelRoomKeyRequest(final Map<String, String> requestBody, boolean andResend) {
        OutgoingRoomKeyRequest req = mCryptoStore.getOutgoingRoomKeyRequest(requestBody);

        if (null == req) {
            // no request was made for this key
            return;
        }

        if (req.mState == OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING
                || req.mState == OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND) {
            // nothing to do here
        } else if ((req.mState == OutgoingRoomKeyRequest.RequestState.UNSENT)
                || (req.mState == OutgoingRoomKeyRequest.RequestState.FAILED)) {
            Log.d(LOG_TAG, "## cancelRoomKeyRequest() : deleting unnecessary room key request for " + requestBody);
            mCryptoStore.deleteOutgoingRoomKeyRequest(req.mRequestId);
        } else if (req.mState == OutgoingRoomKeyRequest.RequestState.SENT) {
            if (andResend) {
                req.mState = OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND;
            } else {
                req.mState = OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING;
            }
            req.mCancellationTxnId = makeTxnId();
            mCryptoStore.updateOutgoingRoomKeyRequest(req);
            sendOutgoingRoomKeyRequestCancellation(req);
        }
    }


    /**
     * Start the background timer to send queued requests, if the timer isn't already running.
     */
    private void startTimer() {
        mWorkingHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSendOutgoingRoomKeyRequestsRunning) {
                    return;
                }

                mWorkingHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSendOutgoingRoomKeyRequestsRunning) {
                            Log.d(LOG_TAG, "## startTimer() : RoomKeyRequestSend already in progress!");
                            return;
                        }

                        mSendOutgoingRoomKeyRequestsRunning = true;
                        sendOutgoingRoomKeyRequests();
                    }
                }, SEND_KEY_REQUESTS_DELAY_MS);
            }
        });
    }

    // look for and send any queued requests. Runs itself recursively until
    // there are no more requests, or there is an error (in which case, the
    // timer will be restarted before the promise resolves).
    private void sendOutgoingRoomKeyRequests() {
        if (!mClientRunning) {
            mSendOutgoingRoomKeyRequestsRunning = false;
            return;
        }

        Log.d(LOG_TAG, "## sendOutgoingRoomKeyRequests() :  Looking for queued outgoing room key requests");
        OutgoingRoomKeyRequest outgoingRoomKeyRequest = mCryptoStore.getOutgoingRoomKeyRequestByState(
                new HashSet<>(Arrays.asList(OutgoingRoomKeyRequest.RequestState.UNSENT,
                        OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING,
                        OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND)));

        if (null == outgoingRoomKeyRequest) {
            Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequests() : No more outgoing room key requests");
            mSendOutgoingRoomKeyRequestsRunning = false;
            return;
        }

        if (OutgoingRoomKeyRequest.RequestState.UNSENT == outgoingRoomKeyRequest.mState) {
            sendOutgoingRoomKeyRequest(outgoingRoomKeyRequest);
        } else {
            sendOutgoingRoomKeyRequestCancellation(outgoingRoomKeyRequest);
        }
    }

    /**
     * Send the outgoing key request.
     *
     * @param request the request
     */
    private void sendOutgoingRoomKeyRequest(final OutgoingRoomKeyRequest request) {
        Log.d(LOG_TAG, "## sendOutgoingRoomKeyRequest() : Requesting keys " + request.mRequestBody
                + " from " + request.mRecipients + " id " + request.mRequestId);

        Map<String, Object> requestMessage = new HashMap<>();
        requestMessage.put("action", "request");
        requestMessage.put("requesting_device_id", mCryptoStore.getDeviceId());
        requestMessage.put("request_id", request.mRequestId);
        requestMessage.put("body", request.mRequestBody);

        sendMessageToDevices(requestMessage, request.mRecipients, request.mRequestId, new ApiCallback<Void>() {
            private void onDone(final OutgoingRoomKeyRequest.RequestState state) {
                mWorkingHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (request.mState != OutgoingRoomKeyRequest.RequestState.UNSENT) {
                            Log.d(LOG_TAG, "## sendOutgoingRoomKeyRequest() : Cannot update room key request from UNSENT as it was already updated to "
                                    + request.mState);
                        } else {
                            request.mState = state;
                            mCryptoStore.updateOutgoingRoomKeyRequest(request);
                        }

                        mSendOutgoingRoomKeyRequestsRunning = false;
                        startTimer();
                    }
                });
            }

            @Override
            public void onSuccess(Void info) {
                Log.d(LOG_TAG, "## sendOutgoingRoomKeyRequest succeed");
                onDone(OutgoingRoomKeyRequest.RequestState.SENT);
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequest failed " + e.getMessage(), e);
                onDone(OutgoingRoomKeyRequest.RequestState.FAILED);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequest failed " + e.getMessage());
                onDone(OutgoingRoomKeyRequest.RequestState.FAILED);
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequest failed " + e.getMessage(), e);
                onDone(OutgoingRoomKeyRequest.RequestState.FAILED);
            }
        });
    }

    /**
     * Given a RoomKeyRequest, cancel it and delete the request record
     *
     * @param request the request
     */
    private void sendOutgoingRoomKeyRequestCancellation(final OutgoingRoomKeyRequest request) {
        Log.d(LOG_TAG, "## sendOutgoingRoomKeyRequestCancellation() : Sending cancellation for key request for " + request.mRequestBody
                + " to " + request.mRecipients
                + " cancellation id  " + request.mCancellationTxnId);

        Map<String, Object> requestMessageMap = new HashMap<>();
        requestMessageMap.put("action", RoomKeyRequest.ACTION_REQUEST_CANCELLATION);
        requestMessageMap.put("requesting_device_id", mCryptoStore.getDeviceId());
        requestMessageMap.put("request_id", request.mCancellationTxnId);

        sendMessageToDevices(requestMessageMap, request.mRecipients, request.mCancellationTxnId, new ApiCallback<Void>() {
            private void onDone() {
                mWorkingHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCryptoStore.deleteOutgoingRoomKeyRequest(request.mRequestId);
                        mSendOutgoingRoomKeyRequestsRunning = false;
                        startTimer();
                    }
                });
            }


            @Override
            public void onSuccess(Void info) {
                Log.d(LOG_TAG, "## sendOutgoingRoomKeyRequestCancellation() : done");
                boolean resend = request.mState == OutgoingRoomKeyRequest.RequestState.CANCELLATION_PENDING_AND_WILL_RESEND;

                onDone();

                // Resend the request with a new ID
                if (resend) {
                    sendRoomKeyRequest(request.mRequestBody, request.mRecipients);
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequestCancellation failed " + e.getMessage(), e);
                onDone();
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequestCancellation failed " + e.getMessage());
                onDone();
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## sendOutgoingRoomKeyRequestCancellation failed " + e.getMessage(), e);
                onDone();
            }
        });
    }

    /**
     * Send a RoomKeyRequest to a list of recipients
     *
     * @param message       the message
     * @param recipients    the recipients.
     * @param transactionId the transaction id
     * @param callback      the asynchronous callback.
     */
    private void sendMessageToDevices(final Map<String, Object> message,
                                      List<Map<String, String>> recipients,
                                      String transactionId,
                                      final ApiCallback<Void> callback) {
        MXUsersDevicesMap<Map<String, Object>> contentMap = new MXUsersDevicesMap<>();

        for (Map<String, String> recipient : recipients) {
            contentMap.setObject(message, recipient.get("userId"), recipient.get("deviceId"));
        }

        mSession.getCryptoRestClient().sendToDevice(Event.EVENT_TYPE_ROOM_KEY_REQUEST, contentMap, transactionId, callback);
    }
}
