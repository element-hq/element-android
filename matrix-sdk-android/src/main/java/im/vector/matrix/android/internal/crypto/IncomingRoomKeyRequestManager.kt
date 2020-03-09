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
import im.vector.matrix.android.api.crypto.MXCryptoConfig
import im.vector.matrix.android.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.keyshare.GossipingRequestListener
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.model.rest.GossipingDefaultContent
import im.vector.matrix.android.internal.crypto.model.rest.GossipingToDeviceObject
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class IncomingRoomKeyRequestManager @Inject constructor(
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val cryptoConfig: MXCryptoConfig,
        private val secretSecretCryptoProvider: ShareSecretCryptoProvider,
        private val roomDecryptorProvider: RoomDecryptorProvider) {

    // list of IncomingRoomKeyRequests/IncomingRoomKeyRequestCancellations
    // we received in the current sync.
    private val receiveGossipingRequests = ArrayList<IncomingShareRequestCommon>()
    private val receivedRequestCancellations = ArrayList<IncomingRequestCancellation>()

    // the listeners
    private val gossipingRequestListeners: MutableSet<GossipingRequestListener> = HashSet()

    init {
        receiveGossipingRequests.addAll(cryptoStore.getPendingIncomingGossipingRequests())
    }

    /**
     * Called when we get an m.room_key_request event
     * It must be called on CryptoThread
     *
     * @param event the announcement event.
     */
    fun onGossipingRequestEvent(event: Event) {
        Timber.v("## onGossipingRequestEvent type ${event.type} from user ${event.senderId}")
        val roomKeyShare = event.getClearContent().toModel<GossipingDefaultContent>()
        val ageLocalTs = event.unsignedData?.age?.let { System.currentTimeMillis() - it }
        when (roomKeyShare?.action) {
            GossipingToDeviceObject.ACTION_SHARE_REQUEST      -> {
                if (event.getClearType() == EventType.REQUEST_SECRET) {
                    IncomingSecretShareRequest.fromEvent(event)?.let {
                        if (event.senderId == credentials.userId && it.deviceId == credentials.deviceId) {
                            // ignore, it was sent by me as *
                            Timber.v("## onGossipingRequestEvent type ${event.type} ignore remote echo")
                        } else {
                            // save in DB
                            cryptoStore.storeIncomingGossipingRequest(it, ageLocalTs)
                            receiveGossipingRequests.add(it)
                        }
                    }
                } else if (event.getClearType() == EventType.ROOM_KEY_REQUEST) {
                    IncomingRoomKeyRequest.fromEvent(event)?.let {
                        if (event.senderId == credentials.userId && it.deviceId == credentials.deviceId) {
                            // ignore, it was sent by me as *
                            Timber.v("## onGossipingRequestEvent type ${event.type} ignore remote echo")
                        } else {
                            cryptoStore.storeIncomingGossipingRequest(it, ageLocalTs)
                            receiveGossipingRequests.add(it)
                        }
                    }
                }
            }
            GossipingToDeviceObject.ACTION_SHARE_CANCELLATION -> {
                IncomingRequestCancellation.fromEvent(event)?.let {
                    receivedRequestCancellations.add(it)
                }
            }
            else                                              -> {
                Timber.e("## onGossipingRequestEvent() : unsupported action ${roomKeyShare?.action}")
            }
        }
    }

    /**
     * Process any m.room_key_request or m.secret.request events which were queued up during the
     * current sync.
     * It must be called on CryptoThread
     */
    fun processReceivedGossipingRequests() {
        Timber.v("## processReceivedGossipingRequests()")

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

        receivedRequestCancellations?.forEach { request ->
            Timber.v("## processReceivedGossipingRequests() : m.room_key_request cancellation $request")
            // we should probably only notify the app of cancellations we told it
            // about, but we don't currently have a record of that, so we just pass
            // everything through.
            if (request.userId == credentials.userId && request.deviceId == credentials.deviceId) {
                // ignore remote echo
                return@forEach
            }
            val matchingIncoming = cryptoStore.getIncomingRoomKeyRequest(request.userId ?: "", request.deviceId ?: "", request.requestId ?: "")
            if (matchingIncoming == null) {
                // ignore that?
                return@forEach
            } else {
                // If it was accepted from this device, keep the information, do not mark as cancelled
                if (matchingIncoming.state != GossipingRequestState.ACCEPTED) {
                    onRoomKeyRequestCancellation(request)
                    cryptoStore.updateGossipingRequestState(request, GossipingRequestState.CANCELLED_BY_REQUESTER)
                }
            }
        }
    }

    private fun processIncomingRoomKeyRequest(request: IncomingRoomKeyRequest) {
        val userId = request.userId
        val deviceId = request.deviceId
        val body = request.requestBody
        val roomId = body!!.roomId
        val alg = body.algorithm

        Timber.v("## processIncomingRoomKeyRequest from $userId:$deviceId for $roomId / ${body.sessionId} id ${request.requestId}")
        if (userId == null || credentials.userId != userId) {
            // TODO: determine if we sent this device the keys already: in
            Timber.w("## processReceivedGossipingRequests() : Ignoring room key request from other user for now")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }
        // TODO: should we queue up requests we don't yet have keys for, in case they turn up later?
        // if we don't have a decryptor for this room/alg, we don't have
        // the keys for the requested events, and can drop the requests.
        val decryptor = roomDecryptorProvider.getRoomDecryptor(roomId, alg)
        if (null == decryptor) {
            Timber.w("## processReceivedGossipingRequests() : room key request for unknown $alg in room $roomId")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }
        if (!decryptor.hasKeysForKeyRequest(request)) {
            Timber.w("## processReceivedGossipingRequests() : room key request for unknown session ${body.sessionId!!}")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }

        if (credentials.deviceId == deviceId && credentials.userId == userId) {
            Timber.v("## processReceivedGossipingRequests() : oneself device - ignored")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }
        request.share = Runnable {
            decryptor.shareKeysWithDevice(request)
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.ACCEPTED)
        }
        request.ignore = Runnable {
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
        }
        // if the device is verified already, share the keys
        val device = cryptoStore.getUserDevice(userId, deviceId!!)
        if (device != null) {
            if (device.isVerified) {
                Timber.v("## processReceivedGossipingRequests() : device is already verified: sharing keys")
                request.share?.run()
                return
            }

            if (device.isBlocked) {
                Timber.v("## processReceivedGossipingRequests() : device is blocked -> ignored")
                cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
                return
            }
        }

        // As per config we automatically discard untrusted devices request
        if (cryptoConfig.discardRoomKeyRequestsFromUntrustedDevices) {
            Timber.v("## processReceivedGossipingRequests() : discardRoomKeyRequestsFromUntrustedDevices")
            // At this point the device is unknown, we don't want to bother user with that
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }

        // Pass to application layer to decide what to do
        onRoomKeyRequest(request)
    }

    private fun processIncomingSecretShareRequest(request: IncomingSecretShareRequest) {
        val secretName = request.secretName ?: return Unit.also {
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            Timber.v("## processIncomingSecretShareRequest() : Missing secret name")
        }

        val userId = request.userId
        if (userId == null || credentials.userId != userId) {
            Timber.e("## processIncomingSecretShareRequest() : Ignoring secret share request from other users")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }

        val deviceId = request.deviceId
                ?: return Unit.also {
                    Timber.e("## processIncomingSecretShareRequest() : Malformed request, no ")
                    cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
                }

        val device = cryptoStore.getUserDevice(userId, deviceId)
                ?: return Unit.also {
                    Timber.e("## processIncomingSecretShareRequest() : Received secret share request from unknown device ${request.deviceId}")
                    cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
                }

        if (!device.isVerified || device.isBlocked) {
            Timber.v("## processIncomingSecretShareRequest() : Ignoring secret share request from untrusted/blocked session $device")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }

        val isDeviceLocallyVerified = cryptoStore.getUserDevice(userId, deviceId)?.trustLevel?.isLocallyVerified()

        // Should SDK always Silently reject any request for the master key?
        when (secretName) {
            SELF_SIGNING_KEY_SSSS_NAME -> cryptoStore.getCrossSigningPrivateKeys()?.selfSigned
            USER_SIGNING_KEY_SSSS_NAME -> cryptoStore.getCrossSigningPrivateKeys()?.user
            else                       -> null
        }?.let { secretValue ->
            // TODO check if locally trusted and not outdated
            Timber.i("## processIncomingSecretShareRequest() : Sharing secret $secretName with $device locally trusted")
            if (isDeviceLocallyVerified == true) {
                secretSecretCryptoProvider.shareSecretWithDevice(request, secretValue)
                cryptoStore.updateGossipingRequestState(request, GossipingRequestState.ACCEPTED)
            }
            return
        }

        request.ignore = Runnable {
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
        }

        request.share = { secretValue ->
            secretSecretCryptoProvider.shareSecretWithDevice(request, secretValue)
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.ACCEPTED)
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
