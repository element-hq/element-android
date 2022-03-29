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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.GossipingToDeviceObject
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyShareRequest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.DefaultSendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import timber.log.Timber
import java.util.Stack
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.system.measureTimeMillis

private val loggerTag = LoggerTag("OutgoingKeyRequestManager", LoggerTag.CRYPTO)

/**
 * This class is responsible for sending key requests to other devices when a message failed to decrypt.
 * It's lifecycle is based on the sync pulse:
 *    - You can post queries for session, or report when you got a session
 *    - At the end of the sync (onSyncComplete) it will then process all the posted request and send to devices
 * If a request failed it will be retried at the end of the next sync
 */
@SessionScope
internal class OutgoingKeyRequestManager @Inject constructor(
        @SessionId private val sessionId: String,
        @UserId private val myUserId: String,
        private val cryptoStore: IMXCryptoStore,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoConfig: MXCryptoConfig,
        private val inboundGroupSessionStore: InboundGroupSessionStore,
        private val sendToDeviceTask: DefaultSendToDeviceTask,
        private val perSessionBackupQueryRateLimiter: PerSessionBackupQueryRateLimiter) {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val outgoingRequestScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sequencer = SemaphoreCoroutineSequencer()

    // We only have one active key request per session, so we don't request if it's already requested
    // But it could make sense to check more the backup, as it's evolving.
    // We keep a stack as we consider that the key requested last is more likely to be on screen?
    private val requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup = Stack<Pair<String, String>>()

    fun requestKeyForEvent(event: Event, force: Boolean) {
        val (targets, body) = getRoomKeyRequestTargetForEvent(event) ?: return
        val index = ratchetIndexForMessage(event) ?: 0
        postRoomKeyRequest(body, targets, index, force)
    }

    private fun getRoomKeyRequestTargetForEvent(event: Event): Pair<Map<String, List<String>>, RoomKeyRequestBody>? {
        val sender = event.senderId ?: return null
        val encryptedEventContent = event.content.toModel<EncryptedEventContent>() ?: return null.also {
            Timber.tag(loggerTag.value).e("getRoomKeyRequestTargetForEvent Failed to re-request key, null content")
        }
        if (encryptedEventContent.algorithm != MXCRYPTO_ALGORITHM_MEGOLM) return null

        val senderDevice = encryptedEventContent.deviceId
        val recipients = if (cryptoConfig.limitRoomKeyRequestsToMyDevices) {
            mapOf(
                    myUserId to listOf("*")
            )
        } else {
            if (event.senderId == myUserId) {
                mapOf(
                        myUserId to listOf("*")
                )
            } else {
                // for the case where you share the key with a device that has a broken olm session
                // The other user might Re-shares a megolm session key with devices if the key has already been
                // sent to them.
                mapOf(
                        myUserId to listOf("*"),

                        // TODO we might not have deviceId in the future due to https://github.com/matrix-org/matrix-spec-proposals/pull/3700
                        // so in this case query to all
                        sender to listOf(senderDevice ?: "*")
                )
            }
        }

        val requestBody = RoomKeyRequestBody(
                roomId = event.roomId,
                algorithm = encryptedEventContent.algorithm,
                senderKey = encryptedEventContent.senderKey,
                sessionId = encryptedEventContent.sessionId
        )
        return recipients to requestBody
    }

    private fun ratchetIndexForMessage(event: Event): Int? {
        val encryptedContent = event.content.toModel<EncryptedEventContent>() ?: return null
        if (encryptedContent.algorithm != MXCRYPTO_ALGORITHM_MEGOLM) return null
        return encryptedContent.ciphertext?.fromBase64()?.inputStream()?.reader()?.let {
            tryOrNull {
                val megolmVersion = it.read()
                if (megolmVersion != 3) return@tryOrNull null
                /** Int tag */
                /** Int tag */
                if (it.read() != 8) return@tryOrNull null
                it.read()
            }
        }
    }

    fun postRoomKeyRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>, fromIndex: Int, force: Boolean = false) {
        outgoingRequestScope.launch {
            sequencer.post {
                internalQueueRequest(requestBody, recipients, fromIndex, force)
            }
        }
    }

    /**
     * Typically called when we the session as been imported or received meanwhile
     */
    fun postCancelRequestForSessionIfNeeded(sessionId: String, roomId: String, senderKey: String, fromIndex: Int) {
        outgoingRequestScope.launch {
            sequencer.post {
                internalQueueCancelRequest(sessionId, roomId, senderKey, fromIndex)
            }
        }
    }

    fun onSelfCrossSigningTrustChanged(newTrust: Boolean) {
        if (newTrust) {
            // we were previously not cross signed, but we are now
            // so there is now more chances to get better replies for existing request
            // Let's forget about sent request so that next time we try to decrypt we will resend requests
            // We don't resend all because we don't want to generate a bulk of traffic
            outgoingRequestScope.launch {
                sequencer.post {
                    cryptoStore.deleteOutgoingRoomKeyRequestInState(OutgoingRoomKeyRequestState.SENT)
                }

                sequencer.post {
                    delay(1000)
                    perSessionBackupQueryRateLimiter.refreshBackupInfoIfNeeded(true)
                }
            }
        }
    }

    fun onRoomKeyForwarded(sessionId: String,
                           algorithm: String,
                           roomId: String,
                           senderKey: String,
                           fromDevice: String?,
                           fromIndex: Int,
                           event: Event) {
        Timber.tag(loggerTag.value).d("Key forwarded for $sessionId from ${event.senderId}|$fromDevice at index $fromIndex")
        outgoingRequestScope.launch {
            sequencer.post {
                cryptoStore.updateOutgoingRoomKeyReply(
                        roomId = roomId,
                        sessionId = sessionId,
                        algorithm = algorithm,
                        senderKey = senderKey,
                        fromDevice = fromDevice,
                        // strip out encrypted stuff as it's just a trail?
                        event = event.copy(
                                type = event.getClearType(),
                                content = mapOf(
                                        "chain_index" to fromIndex
                                )
                        )
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
                        roomId = roomId,
                        sessionId = sessionId,
                        algorithm = algorithm,
                        senderKey = senderKey,
                        fromDevice = fromDevice,
                        event = event
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

    private fun internalQueueCancelRequest(sessionId: String, roomId: String, senderKey: String, localKnownChainIndex: Int) {
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
                    if (request.fromIndex >= localKnownChainIndex) {
                        // we have a good index we can cancel
                        cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
                    }
                }
                OutgoingRoomKeyRequestState.SENT                                 -> {
                    // It was already sent, and index satisfied we can cancel
                    if (request.fromIndex >= localKnownChainIndex) {
                        cryptoStore.updateOutgoingRoomKeyRequestState(request.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING)
                    }
                }
                OutgoingRoomKeyRequestState.CANCELLATION_PENDING                 -> {
                    // It is already marked to be cancelled
                }
                OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND -> {
                    if (request.fromIndex >= localKnownChainIndex) {
                        // we just want to cancel now
                        cryptoStore.updateOutgoingRoomKeyRequestState(request.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING)
                    }
                }
                OutgoingRoomKeyRequestState.SENT_THEN_CANCELED                   -> {
                    // was already canceled
                    // if we need a better index, should we resend?
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

    private fun internalQueueRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>, fromIndex: Int, force: Boolean) {
        if (!cryptoStore.isKeyGossipingEnabled()) {
            // we might want to try backup?
            if (requestBody.roomId != null && requestBody.sessionId != null) {
                requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.push(requestBody.roomId to requestBody.sessionId)
            }
            Timber.tag(loggerTag.value).d("discarding request for ${requestBody.sessionId} as gossiping is disabled")
            return
        }

        Timber.tag(loggerTag.value).d("Queueing key request for ${requestBody.sessionId} force:$force")
        val existing = cryptoStore.getOutgoingRoomKeyRequest(requestBody)
        Timber.tag(loggerTag.value).v("Queueing key request exiting is ${existing?.state}")
        when (existing?.state) {
            null                                                             -> {
                // create a new one
                cryptoStore.getOrAddOutgoingRoomKeyRequest(requestBody, recipients, fromIndex)
            }
            OutgoingRoomKeyRequestState.UNSENT                               -> {
                // nothing it's new or not yet handled
            }
            OutgoingRoomKeyRequestState.SENT                                 -> {
                // it was already requested
                Timber.tag(loggerTag.value).d("The session ${requestBody.sessionId} is already requested")
                if (force) {
                    // update to UNSENT
                    Timber.tag(loggerTag.value).d(".. force to request  ${requestBody.sessionId}")
                    cryptoStore.updateOutgoingRoomKeyRequestState(existing.requestId, OutgoingRoomKeyRequestState.CANCELLATION_PENDING_AND_WILL_RESEND)
                } else {
                    if (existing.roomId != null && existing.sessionId != null) {
                        requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.push(existing.roomId to existing.sessionId)
                    }
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
            OutgoingRoomKeyRequestState.SENT_THEN_CANCELED                   -> {
                if (force) {
                    cryptoStore.deleteOutgoingRoomKeyRequest(existing.requestId)
                    cryptoStore.getOrAddOutgoingRoomKeyRequest(requestBody, recipients, fromIndex)
                }
            }
        }

        if (existing != null && existing.fromIndex >= fromIndex) {
            // update the required index
            cryptoStore.updateOutgoingRoomKeyRequiredIndex(existing.requestId, fromIndex)
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
                    OutgoingRoomKeyRequestState.SENT_THEN_CANCELED,
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
                requestDiscardedBecauseAlreadySentThatCouldBeTriedWithBackup.pop().let { (roomId, sessionId) ->
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
            // let's see what's the index
            val knownIndex = tryOrNull {
                inboundGroupSessionStore.getInboundGroupSession(sessionId, request.requestBody?.senderKey ?: "")?.wrapper?.firstKnownIndex
            }
            if (knownIndex != null && knownIndex <= request.fromIndex) {
                // we found the key in backup with good enough index, so we can just mark as cancelled, no need to send request
                Timber.tag(loggerTag.value).v("Megolm session $sessionId successfully restored from backup, do not send request")
                cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
                return
            }
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
            // The request cancellation was sent, we don't delete yet because we want
            // to keep trace of the sent replies
            cryptoStore.updateOutgoingRoomKeyRequestState(request.requestId, OutgoingRoomKeyRequestState.SENT_THEN_CANCELED)
            true
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).v("Failed to cancel request ${request.requestId} for session $sessionId targets:${request.recipients}")
            false
        }
    }

    private suspend fun handleRequestToCancelWillResend(request: OutgoingKeyRequest) {
        if (handleRequestToCancel(request)) {
            // this will create a new unsent request with no replies that will be process in the following call
            cryptoStore.deleteOutgoingRoomKeyRequest(request.requestId)
            request.requestBody?.let { cryptoStore.getOrAddOutgoingRoomKeyRequest(it, request.recipients, request.fromIndex) }
        }
    }
}
