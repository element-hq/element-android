/*
 * Copyright 2019 New Vector Ltd
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

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.keyshare.RoomKeysRequestListener
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyShare
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import timber.log.Timber
import java.util.*

internal class IncomingRoomKeyRequestManager(
        private val mCredentials: Credentials,
        private val mCryptoStore: IMXCryptoStore,
        private val mRoomDecryptorProvider: RoomDecryptorProvider) {


    // list of IncomingRoomKeyRequests/IncomingRoomKeyRequestCancellations
    // we received in the current sync.
    private val mReceivedRoomKeyRequests = ArrayList<IncomingRoomKeyRequest>()
    private val mReceivedRoomKeyRequestCancellations = ArrayList<IncomingRoomKeyRequestCancellation>()

    // the listeners
    private val mRoomKeysRequestListeners: MutableSet<RoomKeysRequestListener> = HashSet()

    init {
        mReceivedRoomKeyRequests.addAll(mCryptoStore.getPendingIncomingRoomKeyRequests())
    }

    /**
     * Called when we get an m.room_key_request event
     * This method must be called on getEncryptingThreadHandler() thread.
     *
     * @param event the announcement event.
     */
    fun onRoomKeyRequestEvent(event: Event) {
        val roomKeyShare = event.content.toModel<RoomKeyShare>()

        when (roomKeyShare?.action) {
            RoomKeyShare.ACTION_SHARE_REQUEST      -> synchronized(mReceivedRoomKeyRequests) {
                mReceivedRoomKeyRequests.add(IncomingRoomKeyRequest(event))
            }
            RoomKeyShare.ACTION_SHARE_CANCELLATION -> synchronized(mReceivedRoomKeyRequestCancellations) {
                mReceivedRoomKeyRequestCancellations.add(IncomingRoomKeyRequestCancellation(event))
            }
            else                                   -> Timber.e("## onRoomKeyRequestEvent() : unsupported action " + roomKeyShare?.action)
        }
    }

    /**
     * Process any m.room_key_request events which were queued up during the
     * current sync.
     */
    fun processReceivedRoomKeyRequests() {
        var receivedRoomKeyRequests: List<IncomingRoomKeyRequest>? = null

        synchronized(mReceivedRoomKeyRequests) {
            if (!mReceivedRoomKeyRequests.isEmpty()) {
                receivedRoomKeyRequests = ArrayList(mReceivedRoomKeyRequests)
                mReceivedRoomKeyRequests.clear()
            }
        }

        if (null != receivedRoomKeyRequests) {
            for (request in receivedRoomKeyRequests!!) {
                val userId = request.mUserId!!
                val deviceId = request.mDeviceId
                val body = request.mRequestBody
                val roomId = body!!.roomId
                val alg = body.algorithm

                Timber.d("m.room_key_request from " + userId + ":" + deviceId + " for " + roomId + " / " + body.sessionId + " id " + request.mRequestId)

                if (!TextUtils.equals(mCredentials.userId, userId)) {
                    // TODO: determine if we sent this device the keys already: in
                    Timber.e("## processReceivedRoomKeyRequests() : Ignoring room key request from other user for now")
                    return
                }

                // todo: should we queue up requests we don't yet have keys for,
                // in case they turn up later?

                // if we don't have a decryptor for this room/alg, we don't have
                // the keys for the requested events, and can drop the requests.

                val decryptor = mRoomDecryptorProvider.getRoomDecryptor(roomId, alg)

                if (null == decryptor) {
                    Timber.e("## processReceivedRoomKeyRequests() : room key request for unknown $alg in room $roomId")
                    continue
                }

                if (!decryptor.hasKeysForKeyRequest(request)) {
                    Timber.e("## processReceivedRoomKeyRequests() : room key request for unknown session " + body.sessionId!!)
                    mCryptoStore.deleteIncomingRoomKeyRequest(request)
                    continue
                }

                if (TextUtils.equals(deviceId, mCredentials.deviceId) && TextUtils.equals(mCredentials.userId, userId)) {
                    Timber.d("## processReceivedRoomKeyRequests() : oneself device - ignored")
                    mCryptoStore.deleteIncomingRoomKeyRequest(request)
                    continue
                }

                request.mShare = Runnable {
                    decryptor.shareKeysWithDevice(request)
                    mCryptoStore.deleteIncomingRoomKeyRequest(request)
                }

                request.mIgnore = Runnable { mCryptoStore.deleteIncomingRoomKeyRequest(request) }

                // if the device is verified already, share the keys
                val device = mCryptoStore.getUserDevice(deviceId!!, userId)

                if (null != device) {
                    if (device.isVerified) {
                        Timber.d("## processReceivedRoomKeyRequests() : device is already verified: sharing keys")
                        mCryptoStore.deleteIncomingRoomKeyRequest(request)
                        request.mShare!!.run()
                        continue
                    }

                    if (device.isBlocked) {
                        Timber.d("## processReceivedRoomKeyRequests() : device is blocked -> ignored")
                        mCryptoStore.deleteIncomingRoomKeyRequest(request)
                        continue
                    }
                }

                mCryptoStore.storeIncomingRoomKeyRequest(request)
                onRoomKeyRequest(request)
            }
        }

        var receivedRoomKeyRequestCancellations: List<IncomingRoomKeyRequestCancellation>? = null

        synchronized(mReceivedRoomKeyRequestCancellations) {
            if (!mReceivedRoomKeyRequestCancellations.isEmpty()) {
                receivedRoomKeyRequestCancellations = mReceivedRoomKeyRequestCancellations.toList()
                mReceivedRoomKeyRequestCancellations.clear()
            }
        }

        if (null != receivedRoomKeyRequestCancellations) {
            for (request in receivedRoomKeyRequestCancellations!!) {
                Timber.d("## ## processReceivedRoomKeyRequests() : m.room_key_request cancellation for " + request.mUserId
                        + ":" + request.mDeviceId + " id " + request.mRequestId)

                // we should probably only notify the app of cancellations we told it
                // about, but we don't currently have a record of that, so we just pass
                // everything through.
                onRoomKeyRequestCancellation(request)
                mCryptoStore.deleteIncomingRoomKeyRequest(request)
            }
        }
    }

    /**
     * Dispatch onRoomKeyRequest
     *
     * @param request the request
     */
    private fun onRoomKeyRequest(request: IncomingRoomKeyRequest) {
        synchronized(mRoomKeysRequestListeners) {
            for (listener in mRoomKeysRequestListeners) {
                try {
                    listener.onRoomKeyRequest(request)
                } catch (e: Exception) {
                    Timber.e(e, "## onRoomKeyRequest() failed")
                }

            }
        }
    }


    /**
     * A room key request cancellation has been received.
     *
     * @param request the cancellation request
     */
    private fun onRoomKeyRequestCancellation(request: IncomingRoomKeyRequestCancellation) {
        synchronized(mRoomKeysRequestListeners) {
            for (listener in mRoomKeysRequestListeners) {
                try {
                    listener.onRoomKeyRequestCancellation(request)
                } catch (e: Exception) {
                    Timber.e(e, "## onRoomKeyRequestCancellation() failed")
                }

            }
        }
    }

    fun addRoomKeysRequestListener(listener: RoomKeysRequestListener) {
        synchronized(mRoomKeysRequestListeners) {
            mRoomKeysRequestListeners.add(listener)
        }
    }

    fun removeRoomKeysRequestListener(listener: RoomKeysRequestListener) {
        synchronized(mRoomKeysRequestListeners) {
            mRoomKeysRequestListeners.remove(listener)
        }
    }

}