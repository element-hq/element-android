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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.GossipingToDeviceObject
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.OutgoingRoomKeyRequestState
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyShareRequest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.DefaultSendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import timber.log.Timber
import java.util.Stack
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.system.measureTimeMillis

private val loggerTag = LoggerTag("OutgoingGossipingRequestManager", LoggerTag.CRYPTO)

/**
 * This class is responsible for sending key requests to other devices when a message failed to decrypt.
 * It's lifecycle is based on the sync pulse:
 *    - You can post queries for session, or report when you got a session
 *    - At the end of the sync (onSyncComplete) it will then process all the posted request and send to devices
 * If a request failed it will be retried at the end of the next sync
 */
@SessionScope
internal class OutgoingGossipingRequestManager @Inject constructor(
        @SessionId private val sessionId: String,
        private val cryptoStore: IMXCryptoStore,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sendToDeviceTask: DefaultSendToDeviceTask,
        private val perSessionBackupQueryRateLimiter: PerSessionBackupQueryRateLimiter) {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val outgoingRequestScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sequencer = SemaphoreCoroutineSequencer()

    // We only have one active key request per session, so we don't request if it's already requested
    // But it could make sense to check more the backup, as it's evolving.
    // We keep a stack as we consider that the key requested last is more likely to be on screen?
    private val requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup = Stack<String>()

    fun postRoomKeyRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>, force: Boolean = false) {
        outgoingRequestScope.launch {
            sequencer.post {
                internalQueueRequest(requestBody, recipients, force)
            }
        }
    }

    /**
     * Typically called when we the session as been imported or received meanwhile
     */
    fun postCancelRequestForSessionIfNeeded(sessionId: String, roomId: String, senderKey: String) {
        outgoingRequestScope.launch {
            sequencer.post {
                internalQueueCancelRequest(sessionId, roomId, senderKey)
            }
        }
    }

    fun onRoomKeyForwarded(sessionId: String,
                           algorithm: String,
                           roomId: String,
                           senderKey: String,
                           fromDevice: String?,
                           event: Event) {
        Timber.tag(loggerTag.value).v("Key forwarded for $sessionId from ${event.senderId}|$fromDevice")
        outgoingRequestScope.launch {
            sequencer.post {
                cryptoStore.updateOutgoingRoomKeyReply(
                        roomId,
                        sessionId,
                        algorithm,
                        senderKey,
                        fromDevice,
                        event
                )
            }
        }
    }

    fun onRoomKeyWithHeld(sessionId: String,
                          algorithm: String,
                          roomId: String,
                          senderKey: String,
                          fromDevice: String?,
                          event: Event) {
        outgoingRequestScope.launch {
            sequencer.post {
                Timber.tag(loggerTag.value).d("Withheld received for $sessionId from ${event.senderId}|$fromDevice")
                cryptoStore.updateOutgoingRoomKeyReply(
                        roomId,
                        sessionId,
                        algorithm,
                        senderKey,
                        fromDevice,
                        event
                )
            }
        }
    }

    /**
     * Should be called after a sync, ideally if no catchup sync needed (as keys might arrive in those)
     */
    fun requireProcessAllPendingKeyRequests() {
        outgoingRequestScope.launch {
            sequencer.post {
                internalProcessPendingKeyRequests()
            }
        }
    }

    private fun internalQueueCancelRequest(sessionId: String, roomId: String, senderKey: String) {
        // do we have known requests for that session??
        Timber.tag(loggerTag.value).v("Cancel Key Request if needed for $sessionId")
        val knownRequest = cryptoStore.getOutgoingRoomKeyRequest(
                algorithm = MXCRYPTO_ALGORITHM_MEGOLM,
                roomId = roomId,
                sessionId = sessionId,
                senderKey = senderKey
        )
        if (knownRequest.isEmpty()) return Unit.also {
            Timber.tag(loggerTag.value).v("Handle Cancel Key Request for $sessionId -- Was not currently requested")
        }
        if (knownRequest.size > 1) {
            // It's worth logging, there should be only one
            Timber.tag(loggerTag.value).w("Found multiple requests for same sessionId $sessionId")
        }
        knownRequest.forEach { request ->
            when (request.state) {
                OutgoingRoomKeyRequestState.UNSENT                               -> {
                    cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
                }
                OutgoingRoomKeyRequestState.SENT                                 -> {
                    // It was already sent, so cancel
                    cryptoStore.updateOutgoingRoomKeyRequestState(request.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING)
                }
                OutgoingRoomKeyRequestState.CANCELLATION_PENDING                 -> {
                    // It is already marked to be cancelled
                }
                OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND -> {
                    // we just want to cancel now
                    cryptoStore.updateOutgoingRoomKeyRequestState(request.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING)
                }
            }
        }
    }

    fun close() {
        try {
            outgoingRequestScope.cancel("User Terminate")
            requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.clear()
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).w("Failed to shutDown request manager")
        }
    }

    private fun internalQueueRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>, force: Boolean) {
        Timber.tag(loggerTag.value).d("Queueing key request for ${requestBody.sessionId}")
        val existing = cryptoStore.getOrAddOutgoingRoomKeyRequest(requestBody, recipients)
        when (existing.state) {
            OutgoingRoomKeyRequestState.UNSENT                               -> {
                // nothing it's new or not yet handled
            }
            OutgoingRoomKeyRequestState.SENT                                 -> {
                // it was already requested
                Timber.tag(loggerTag.value).w("The session ${requestBody.sessionId} is already requested")
                if (force) {
                    // update to UNSENT
                    Timber.tag(loggerTag.value).w(".. force to request  ${requestBody.sessionId}")
                    cryptoStore.updateOutgoingRoomKeyRequestState(existing.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND)
                } else {
                    requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.push(existing.requestId)
                }
            }
            OutgoingRoomKeyRequestState.CANCELLATION_PENDING                 -> {
                // request is canceled only if I got the keys so what to do here...
                if (force) {
                    cryptoStore.updateOutgoingRoomKeyRequestState(existing.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND)
                }
            }
            OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND -> {
                // It's already going to resend
            }
        }
    }

    private suspend fun internalProcessPendingKeyRequests() {
        val toProcess = cryptoStore.getOutgoingRoomKeyRequests(OutgoingRoomKeyRequestState.pendingStates())
        Timber.tag(loggerTag.value).v("Processing all pending key requests (found ${toProcess.size} pending)")

        measureTimeMillis {
            toProcess.forEach {
                when (it.state) {
                    OutgoingRoomKeyRequestState.UNSENT                               -> handleUnsentRequest(it)
                    OutgoingRoomKeyRequestState.CANCELLATION_PENDING                 -> handleRequestToCancel(it)
                    OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND -> handleRequestToCancelWillResend(it)
                    OutgoingRoomKeyRequestState.SENT                                 -> {
                        // these are filtered out
                    }
                }
            }
        }.let {
            Timber.tag(loggerTag.value).v("Finish processing pending key request in $it ms")
        }

        val maxBackupCallsBySync = 60
        var currentCalls = 0
        measureTimeMillis {
            while (requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.isNotEmpty() && currentCalls < maxBackupCallsBySync) {
                requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.pop().let {
                    val req = cryptoStore.getOutgoingRoomKeyRequest(it)
                    val sessionId = req?.sessionId ?: return@let
                    val roomId = req.roomId ?: return@let
                    // we want to rate limit that somehow :/
                    perSessionBackupQueryRateLimiter.tryFromBackupIfPossible(sessionId, roomId)
                }
                currentCalls++
            }
        }.let {
            Timber.tag(loggerTag.value).v("Finish querying backup in $it ms")
        }
    }

    private suspend fun handleUnsentRequest(request: OutgoingKeyRequest) {
        // In order to avoid generating to_device traffic, we can first check if the key is backed up
        Timber.tag(loggerTag.value).v("Handling unsent request for megolm session ${request.sessionId} in ${request.roomId}")
        val sessionId = request.sessionId ?: return
        val roomId = request.roomId ?: return
        if (perSessionBackupQueryRateLimiter.tryFromBackupIfPossible(sessionId, roomId)) {
            // we found the key in backup, so we can just mark as cancelled, no need to send request
            Timber.tag(loggerTag.value).v("Megolm session $sessionId successfully restored from backup, do not send request")
            cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
            return
        }

        // we need to send the request
        val toDeviceContent = RoomKeyShareRequest(
                requestingDeviceId = cryptoStore.getDeviceId(),
                requestId = request.requestId,
                action = GossipingToDeviceObject.ACTION_SHARE_REQUEST,
                body = request.requestBody
        )
        val contentMap = MXUsersDevicesMap<Any>()
        request.recipients.forEach { userToDeviceMap ->
            userToDeviceMap.value.forEach { deviceId ->
                contentMap.setObject(userToDeviceMap.key, deviceId, toDeviceContent)
            }
        }

        val params = SendToDeviceTask.Params(
                eventType = EventType.ROOM_KEY_REQUEST,
                contentMap = contentMap,
                transactionId = request.requestId
        )
        try {
            withContext(coroutineDispatchers.io) {
                sendToDeviceTask.executeRetry(params, 3)
            }
            Timber.tag(loggerTag.value).d("Key request sent for $sessionId in room $roomId to ${request.recipients}")
            // The request was sent, so update state
            cryptoStore.updateOutgoingRoomKeyRequestState(request.requestId, OutgoingRoomKeyRequestState.SENT)
            // TODO update the audit trail
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).v("Failed to request $sessionId targets:${request.recipients}")
        }
    }

    private suspend fun handleRequestToCancel(request: OutgoingKeyRequest): Boolean {
        Timber.tag(loggerTag.value).v("handleRequestToCancel for megolm session ${request.sessionId}")
        // we have to cancel this
        val toDeviceContent = RoomKeyShareRequest(
                requestingDeviceId = cryptoStore.getDeviceId(),
                requestId = request.requestId,
                action = GossipingToDeviceObject.ACTION_SHARE_CANCELLATION
        )
        val contentMap = MXUsersDevicesMap<Any>()
        request.recipients.forEach { userToDeviceMap ->
            userToDeviceMap.value.forEach { deviceId ->
                contentMap.setObject(userToDeviceMap.key, deviceId, toDeviceContent)
            }
        }

        val params = SendToDeviceTask.Params(
                eventType = EventType.ROOM_KEY_REQUEST,
                contentMap = contentMap,
                transactionId = request.requestId
        )
        return try {
            withContext(coroutineDispatchers.io) {
                sendToDeviceTask.executeRetry(params, 3)
            }
            // The request cancellation was sent, we can forget about it
            cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
            // TODO update the audit trail
            true
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).v("Failed to cancel request ${request.requestId} for session $sessionId targets:${request.recipients}")
            false
        }
    }

    private suspend fun handleRequestToCancelWillResend(request: OutgoingKeyRequest) {
        if (handleRequestToCancel(request)) {
            // we have to create a new unsent one
            val body = request.requestBody ?: return
            // this will create a new unsent request that will be process in the following call
            cryptoStore.getOrAddOutgoingRoomKeyRequest(body, request.recipients)
        }
    }
}
