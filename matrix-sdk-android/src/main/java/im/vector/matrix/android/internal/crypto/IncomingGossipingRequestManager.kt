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
import im.vector.matrix.android.api.auth.data.sessionId
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
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class IncomingGossipingRequestManager @Inject constructor(
        @SessionId private val sessionId: String,
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val cryptoConfig: MXCryptoConfig,
        private val gossipingWorkManager: GossipingWorkManager,
        private val roomDecryptorProvider: RoomDecryptorProvider) {

    // list of IncomingRoomKeyRequests/IncomingRoomKeyRequestCancellations
    // we received in the current sync.
    private val receivedGossipingRequests = ArrayList<IncomingShareRequestCommon>()
    private val receivedRequestCancellations = ArrayList<IncomingRequestCancellation>()

    // the listeners
    private val gossipingRequestListeners: MutableSet<GossipingRequestListener> = HashSet()

    init {
        receivedGossipingRequests.addAll(cryptoStore.getPendingIncomingGossipingRequests())
    }

    // Recently verified devices (map of deviceId and timestamp)
    private val recentlyVerifiedDevices = HashMap<String, Long>()

    /**
     * Called when a session has been verified.
     * This information can be used by the manager to decide whether or not to fullfil gossiping requests
     */
    fun onVerificationCompleteForDevice(deviceId: String) {
        // For now we just keep an in memory cache
        synchronized(recentlyVerifiedDevices) {
            recentlyVerifiedDevices[deviceId] = System.currentTimeMillis()
        }
    }

    private fun hasBeenVerifiedLessThanFiveMinutesFromNow(deviceId: String): Boolean {
        val verifTimestamp: Long?
        synchronized(recentlyVerifiedDevices) {
            verifTimestamp = recentlyVerifiedDevices[deviceId]
        }
        if (verifTimestamp == null) return false

        val age = System.currentTimeMillis() - verifTimestamp

        return age < FIVE_MINUTES_IN_MILLIS
    }

    /**
     * Called when we get an m.room_key_request event
     * It must be called on CryptoThread
     *
     * @param event the announcement event.
     */
    fun onGossipingRequestEvent(event: Event) {
        Timber.v("## GOSSIP onGossipingRequestEvent type ${event.type} from user ${event.senderId}")
        val roomKeyShare = event.getClearContent().toModel<GossipingDefaultContent>()
        val ageLocalTs = event.unsignedData?.age?.let { System.currentTimeMillis() - it }
        when (roomKeyShare?.action) {
            GossipingToDeviceObject.ACTION_SHARE_REQUEST      -> {
                if (event.getClearType() == EventType.REQUEST_SECRET) {
                    IncomingSecretShareRequest.fromEvent(event)?.let {
                        if (event.senderId == credentials.userId && it.deviceId == credentials.deviceId) {
                            // ignore, it was sent by me as *
                            Timber.v("## GOSSIP onGossipingRequestEvent type ${event.type} ignore remote echo")
                        } else {
                            // save in DB
                            cryptoStore.storeIncomingGossipingRequest(it, ageLocalTs)
                            receivedGossipingRequests.add(it)
                        }
                    }
                } else if (event.getClearType() == EventType.ROOM_KEY_REQUEST) {
                    IncomingRoomKeyRequest.fromEvent(event)?.let {
                        if (event.senderId == credentials.userId && it.deviceId == credentials.deviceId) {
                            // ignore, it was sent by me as *
                            Timber.v("## GOSSIP onGossipingRequestEvent type ${event.type} ignore remote echo")
                        } else {
                            cryptoStore.storeIncomingGossipingRequest(it, ageLocalTs)
                            receivedGossipingRequests.add(it)
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
                Timber.e("## GOSSIP onGossipingRequestEvent() : unsupported action ${roomKeyShare?.action}")
            }
        }
    }

    /**
     * Process any m.room_key_request or m.secret.request events which were queued up during the
     * current sync.
     * It must be called on CryptoThread
     */
    fun processReceivedGossipingRequests() {
        val roomKeyRequestsToProcess = receivedGossipingRequests.toList()
        receivedGossipingRequests.clear()
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
            Timber.v("## GOSSIP processReceivedGossipingRequests() : m.room_key_request cancellation $request")
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

        Timber.v("## GOSSIP processIncomingRoomKeyRequest from $userId:$deviceId for $roomId / ${body.sessionId} id ${request.requestId}")
        if (userId == null || credentials.userId != userId) {
            // TODO: determine if we sent this device the keys already: in
            Timber.w("## GOSSIP processReceivedGossipingRequests() : Ignoring room key request from other user for now")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }
        // TODO: should we queue up requests we don't yet have keys for, in case they turn up later?
        // if we don't have a decryptor for this room/alg, we don't have
        // the keys for the requested events, and can drop the requests.
        val decryptor = roomDecryptorProvider.getRoomDecryptor(roomId, alg)
        if (null == decryptor) {
            Timber.w("## GOSSIP processReceivedGossipingRequests() : room key request for unknown $alg in room $roomId")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }
        if (!decryptor.hasKeysForKeyRequest(request)) {
            Timber.w("## GOSSIP processReceivedGossipingRequests() : room key request for unknown session ${body.sessionId!!}")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }

        if (credentials.deviceId == deviceId && credentials.userId == userId) {
            Timber.v("## GOSSIP processReceivedGossipingRequests() : oneself device - ignored")
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
                Timber.v("## GOSSIP processReceivedGossipingRequests() : device is already verified: sharing keys")
                request.share?.run()
                return
            }

            if (device.isBlocked) {
                Timber.v("## GOSSIP processReceivedGossipingRequests() : device is blocked -> ignored")
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
            Timber.v("## GOSSIP processIncomingSecretShareRequest() : Missing secret name")
        }

        val userId = request.userId
        if (userId == null || credentials.userId != userId) {
            Timber.e("## GOSSIP processIncomingSecretShareRequest() : Ignoring secret share request from other users")
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            return
        }

        val deviceId = request.deviceId
                ?: return Unit.also {
                    Timber.e("## GOSSIP processIncomingSecretShareRequest() : Malformed request, no ")
                    cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
                }

        val device = cryptoStore.getUserDevice(userId, deviceId)
                ?: return Unit.also {
                    Timber.e("## GOSSIP processIncomingSecretShareRequest() : Received secret share request from unknown device ${request.deviceId}")
                    cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
                }

        if (!device.isVerified || device.isBlocked) {
            Timber.v("## GOSSIP processIncomingSecretShareRequest() : Ignoring secret share request from untrusted/blocked session $device")
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
            Timber.i("## GOSSIP processIncomingSecretShareRequest() : Sharing secret $secretName with $device locally trusted")
            if (isDeviceLocallyVerified == true && hasBeenVerifiedLessThanFiveMinutesFromNow(deviceId)) {
                val params = SendGossipWorker.Params(
                        sessionId = sessionId,
                        secretValue = secretValue,
                        request = request
                )

                cryptoStore.updateGossipingRequestState(request, GossipingRequestState.ACCEPTING)
                val workRequest = gossipingWorkManager.createWork<SendGossipWorker>(WorkerParamsFactory.toData(params), true)
                gossipingWorkManager.postWork(workRequest)
            } else {
                Timber.v("## GOSSIP processIncomingSecretShareRequest() : Can't share secret $secretName with $device, verification too old")
                cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
            }
            return
        }

        request.ignore = Runnable {
            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.REJECTED)
        }

        request.share = { secretValue ->

            val params = SendGossipWorker.Params(
                    sessionId = userId,
                    secretValue = secretValue,
                    request = request
            )

            cryptoStore.updateGossipingRequestState(request, GossipingRequestState.ACCEPTING)
            val workRequest = gossipingWorkManager.createWork<SendGossipWorker>(WorkerParamsFactory.toData(params), true)
            gossipingWorkManager.postWork(workRequest)
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
                    Timber.e(e, "## GOSSIP onRoomKeyRequest() failed")
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
                    Timber.e(e, "## GOSSIP onRoomKeyRequestCancellation() failed")
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

    companion object {
        private const val FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000
    }
}
