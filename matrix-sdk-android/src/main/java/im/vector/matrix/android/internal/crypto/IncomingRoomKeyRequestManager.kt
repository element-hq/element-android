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

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.keyshare.GossipingRequestListener
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.model.rest.GossipingToDeviceObject
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class IncomingRoomKeyRequestManager @Inject constructor(
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val crossSigningService: CrossSigningService,
        private val secrSecretCryptoProvider: ShareSecretCryptoProvider,
        private val roomDecryptorProvider: RoomDecryptorProvider) {

    // list of IncomingRoomKeyRequests/IncomingRoomKeyRequestCancellations
    // we received in the current sync.
    private val receiveGossipingRequests = ArrayList<IncomingShareRequestCommon>()
    private val receivedRequestCancellations = ArrayList<IncomingRequestCancellation>()

    // the listeners
    private val gossipingRequestListeners: MutableSet<GossipingRequestListener> = HashSet()

    init {
        receiveGossipingRequests.addAll(cryptoStore.getPendingIncomingSecretShareRequests())
        receiveGossipingRequests.addAll(cryptoStore.getPendingIncomingRoomKeyRequests())
    }

    /**
     * Called when we get an m.room_key_request event
     * It must be called on CryptoThread
     *
     * @param event the announcement event.
     */
    fun onGossipingRequestEvent(event: Event) {
        val roomKeyShare = event.getClearContent().toModel<GossipingToDeviceObject>()
        when (roomKeyShare?.action) {
            GossipingToDeviceObject.ACTION_SHARE_REQUEST      -> {
                if (event.getClearType() == EventType.REQUEST_SECRET) {
                    IncomingSecretShareRequest.fromEvent(event)?.let {
                        receiveGossipingRequests.add(it)
                    }
                } else if (event.getClearType() == EventType.ROOM_KEY_REQUEST) {
                    IncomingRoomKeyRequest.fromEvent(event)?.let {
                        receiveGossipingRequests.add(it)
                    }
                }
            }
            GossipingToDeviceObject.ACTION_SHARE_CANCELLATION -> IncomingRequestCancellation.fromEvent(event)?.let { receivedRequestCancellations.add(it) }
            else                                              -> Timber.e("## onGossipingRequestEvent() : unsupported action ${roomKeyShare?.action}")
        }
    }

    /**
     * Process any m.room_key_request or m.secret.request events which were queued up during the
     * current sync.
     * It must be called on CryptoThread
     */
    fun processReceivedGossipingRequests() {
        val roomKeyRequestsToProcess = receiveGossipingRequests.toList()
        receiveGossipingRequests.clear()
        for (request in roomKeyRequestsToProcess) {
            if (request is IncomingRoomKeyRequest) {
                processIncomingRoomKeyRequest(request)
            } else if (request is IncomingSecretShareRequest) {
                processIncomingSecretShareRequest(request)
            }
        }

        var receivedRequestCancellations: List<IncomingRequestCancellation>? = null

        synchronized(this.receivedRequestCancellations) {
            if (this.receivedRequestCancellations.isNotEmpty()) {
                receivedRequestCancellations = this.receivedRequestCancellations.toList()
                this.receivedRequestCancellations.clear()
            }
        }

        if (null != receivedRequestCancellations) {
            for (request in receivedRequestCancellations!!) {
                Timber.v("## ## processReceivedGossipingRequests() : m.room_key_request cancellation for " + request.userId
                        + ":" + request.deviceId + " id " + request.requestId)

                // we should probably only notify the app of cancellations we told it
                // about, but we don't currently have a record of that, so we just pass
                // everything through.
                onRoomKeyRequestCancellation(request)
                cryptoStore.deleteIncomingRoomKeyRequest(request)
            }
        }
    }

    private fun processIncomingRoomKeyRequest(request: IncomingRoomKeyRequest) {
        val userId = request.userId
        val deviceId = request.deviceId
        val body = request.requestBody
        val roomId = body!!.roomId
        val alg = body.algorithm

        Timber.v("m.room_key_request from $userId:$deviceId for $roomId / ${body.sessionId} id ${request.requestId}")
        if (userId == null || credentials.userId != userId) {
            // TODO: determine if we sent this device the keys already: in
            Timber.w("## processReceivedGossipingRequests() : Ignoring room key request from other user for now")
            cryptoStore.deleteIncomingRoomKeyRequest(request)
            return
        }
        // TODO: should we queue up requests we don't yet have keys for, in case they turn up later?
        // if we don't have a decryptor for this room/alg, we don't have
        // the keys for the requested events, and can drop the requests.
        val decryptor = roomDecryptorProvider.getRoomDecryptor(roomId, alg)
        if (null == decryptor) {
            Timber.w("## processReceivedGossipingRequests() : room key request for unknown $alg in room $roomId")
            return
        }
        if (!decryptor.hasKeysForKeyRequest(request)) {
            Timber.w("## processReceivedGossipingRequests() : room key request for unknown session ${body.sessionId!!}")
            cryptoStore.deleteIncomingRoomKeyRequest(request)
            return
        }

        if (credentials.deviceId == deviceId && credentials.userId == userId) {
            Timber.v("## processReceivedGossipingRequests() : oneself device - ignored")
            cryptoStore.deleteIncomingRoomKeyRequest(request)
            return
        }
        request.share = Runnable {
            decryptor.shareKeysWithDevice(request)
            cryptoStore.deleteIncomingRoomKeyRequest(request)
        }
        request.ignore = Runnable {
            cryptoStore.deleteIncomingRoomKeyRequest(request)
        }
        // if the device is verified already, share the keys
        val device = cryptoStore.getUserDevice(userId, deviceId!!)
        if (device != null) {
            if (device.isVerified) {
                Timber.v("## processReceivedGossipingRequests() : device is already verified: sharing keys")
                cryptoStore.deleteIncomingRoomKeyRequest(request)
                request.share?.run()
                return
            }

            if (device.isBlocked) {
                Timber.v("## processReceivedGossipingRequests() : device is blocked -> ignored")
                cryptoStore.deleteIncomingRoomKeyRequest(request)
                return
            }
        }

        // If cross signing is available on account we automatically discard untrust devices request
        if (cryptoStore.getMyCrossSigningInfo() != null) {
            // At this point the device is unknown, we don't want to bother user with that
            cryptoStore.deleteIncomingRoomKeyRequest(request)
            return
        }

        cryptoStore.storeIncomingRoomKeyRequest(request)

        // Legacy, pass to application layer to decide what to do
        onRoomKeyRequest(request)
    }

    private fun processIncomingSecretShareRequest(request: IncomingSecretShareRequest) {
        val secretName = request.secretName ?: return Unit.also {
            cryptoStore.deleteIncomingSecretRequest(request)
            Timber.v("## processIncomingSecretShareRequest() : Missing secret name")
        }

        val userId = request.userId
        if (userId == null || credentials.userId != userId) {
            Timber.e("## processIncomingSecretShareRequest() : Ignoring secret share request from other user")
            cryptoStore.deleteIncomingRoomKeyRequest(request)
            return
        }

        when (secretName) {
            SELF_SIGNING_KEY_SSSS_NAME -> cryptoStore.getCrossSigningPrivateKeys()?.selfSigned
            USER_SIGNING_KEY_SSSS_NAME -> cryptoStore.getCrossSigningPrivateKeys()?.user
            else                       -> null
        }?.let { secretValue ->
            // TODO check if locally trusted and not outdated
            if (cryptoStore.getUserDevice(userId, request.deviceId ?: "")?.trustLevel?.isLocallyVerified() == true) {
                secrSecretCryptoProvider.shareSecretWithDevice(request, secretValue)
                cryptoStore.deleteIncomingRoomKeyRequest(request)
            }
            return
        }

        request.ignore = Runnable {
            cryptoStore.deleteIncomingRoomKeyRequest(request)
        }

        request.share = { secretValue ->
            secrSecretCryptoProvider.shareSecretWithDevice(request, secretValue)
            cryptoStore.deleteIncomingRoomKeyRequest(request)
        }

        onShareRequest(request)
    }

    /**
     * Dispatch onRoomKeyRequest
     *
     * @param request the request
     */
    private fun onRoomKeyRequest(request: IncomingRoomKeyRequest) {
        synchronized(gossipingRequestListeners) {
            for (listener in gossipingRequestListeners) {
                try {
                    listener.onRoomKeyRequest(request)
                } catch (e: Exception) {
                    Timber.e(e, "## onRoomKeyRequest() failed")
                }
            }
        }
    }

    /**
     * Ask for a value to the listeners, and take the first one
     */
    private fun onShareRequest(request: IncomingSecretShareRequest) {
        synchronized(gossipingRequestListeners) {
            for (listener in gossipingRequestListeners) {
                try {
                    if (listener.onSecretShareRequest(request)) {
                        return
                    }
                } catch (e: Exception) {
                    Timber.e(e, "## onRoomKeyRequest() failed")
                }
            }
        }
        // Not handled, ignore
        request.ignore?.run()
    }

    /**
     * A room key request cancellation has been received.
     *
     * @param request the cancellation request
     */
    private fun onRoomKeyRequestCancellation(request: IncomingRequestCancellation) {
        synchronized(gossipingRequestListeners) {
            for (listener in gossipingRequestListeners) {
                try {
                    listener.onRoomKeyRequestCancellation(request)
                } catch (e: Exception) {
                    Timber.e(e, "## onRoomKeyRequestCancellation() failed")
                }
            }
        }
    }

    fun addRoomKeysRequestListener(listener: GossipingRequestListener) {
        synchronized(gossipingRequestListeners) {
            gossipingRequestListeners.add(listener)
        }
    }

    fun removeRoomKeysRequestListener(listener: GossipingRequestListener) {
        synchronized(gossipingRequestListeners) {
            gossipingRequestListeners.remove(listener)
        }
    }
}
