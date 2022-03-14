/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyShareRequest
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.system.measureTimeMillis

private val loggerTag = LoggerTag("IncomingKeyRequestManager", LoggerTag.CRYPTO)

@SessionScope
internal class IncomingKeyRequestManager @Inject constructor(
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        private val olmDevice: MXOlmDevice,
        private val messageEncrypter: MessageEncrypter,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sendToDeviceTask: SendToDeviceTask) {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val outgoingRequestScope = CoroutineScope(SupervisorJob() + dispatcher)
    val sequencer = SemaphoreCoroutineSequencer()

    private val incomingRequestBuffer = mutableListOf<ValidMegolmRequestBody>()

    // the listeners
    private val gossipingRequestListeners: MutableSet<GossipingRequestListener> = HashSet()

    enum class MegolmRequestAction {
        Request, Cancel
    }

    data class ValidMegolmRequestBody(
            val requestingUserId: String,
            val requestingDeviceId: String,
            val roomId: String,
            val senderKey: String,
            val sessionId: String,
            val action: MegolmRequestAction
    ) {
        fun shortDbgString() = "Request from $requestingUserId|$requestingDeviceId for session $sessionId in room $roomId"
    }

    private fun RoomKeyShareRequest.toValidMegolmRequest(senderId: String): ValidMegolmRequestBody? {
        val deviceId = requestingDeviceId ?: return null
        val body = body ?: return null
        val roomId = body.roomId ?: return null
        val sessionId = body.sessionId ?: return null
        val senderKey = body.senderKey ?: return null
        if (body.algorithm != MXCRYPTO_ALGORITHM_MEGOLM) return null
        val action = when (this.action) {
            "request"              -> MegolmRequestAction.Request
            "request_cancellation" -> MegolmRequestAction.Cancel
            else                   -> null
        } ?: return null
        return ValidMegolmRequestBody(
                requestingUserId = senderId,
                requestingDeviceId = deviceId,
                roomId = roomId,
                senderKey = senderKey,
                sessionId = sessionId,
                action = action
        )
    }

    fun addNewIncomingRequest(senderId: String, request: RoomKeyShareRequest) {
        outgoingRequestScope.launch {
            // It is important to handle requests in order
            sequencer.post {
                val validMegolmRequest = request.toValidMegolmRequest(senderId) ?: return@post Unit.also {
                    Timber.tag(loggerTag.value).w("Received key request for unknown algorithm ${request.body?.algorithm}")
                }

                // is there already one like that?
                val existing = incomingRequestBuffer.firstOrNull { it == validMegolmRequest }
                if (existing == null) {
                    when (validMegolmRequest.action) {
                        MegolmRequestAction.Request -> {
                            // just add to the buffer
                            incomingRequestBuffer.add(validMegolmRequest)
                        }
                        MegolmRequestAction.Cancel  -> {
                            // ignore, we can't cancel as it's not known (probably already processed)
                        }
                    }
                } else {
                    when (validMegolmRequest.action) {
                        MegolmRequestAction.Request -> {
                            // it's already in buffer, nop keep existing
                        }
                        MegolmRequestAction.Cancel  -> {
                            // discard the request in buffer
                            incomingRequestBuffer.remove(existing)
                        }
                    }
                }
            }
        }
    }

    fun processIncomingRequests() {
        outgoingRequestScope.launch {
            sequencer.post {
                measureTimeMillis {
                    Timber.tag(loggerTag.value).v("processIncomingKeyRequests : ${incomingRequestBuffer.size} request to process")
                    incomingRequestBuffer.forEach {
                        // should not happen, we only store requests
                        if (it.action != MegolmRequestAction.Request) return@forEach
                        try {
                            handleIncomingRequest(it)
                        } catch (failure: Throwable) {
                            // ignore and continue, should not happen
                            Timber.tag(loggerTag.value).w(failure, "processIncomingKeyRequests : failed to process request $it")
                        }
                    }
                    incomingRequestBuffer.clear()
                }.let { duration ->
                    Timber.tag(loggerTag.value).v("Finish processing incoming key request in $duration ms")
                }
            }
        }
    }

    private suspend fun handleIncomingRequest(request: ValidMegolmRequestBody) {
        // We don't want to download keys, if we don't know the device yet we won't share any how?
        val requestingDevice =
                cryptoStore.getUserDevice(request.requestingUserId, request.requestingDeviceId)
                        ?: return Unit.also {
                            Timber.tag(loggerTag.value).d("Ignoring key request: ${request.shortDbgString()}")
                        }

        cryptoStore.saveIncomingKeyRequestAuditTrail(
                request.roomId,
                request.sessionId,
                request.senderKey,
                MXCRYPTO_ALGORITHM_MEGOLM,
                request.requestingUserId,
                request.requestingDeviceId
        )

        val roomAlgorithm = // withContext(coroutineDispatchers.crypto) {
                cryptoStore.getRoomAlgorithm(request.roomId)
//        }
        if (roomAlgorithm != MXCRYPTO_ALGORITHM_MEGOLM) {
            // strange we received a request for a room that is not encrypted
            // maybe a broken state?
            Timber.tag(loggerTag.value).w("Received a key request in a room with unsupported alg:$roomAlgorithm , req:${request.shortDbgString()}")
            return
        }

        // Is it for one of our sessions?
        if (request.requestingUserId == credentials.userId) {
            Timber.tag(loggerTag.value).v("handling request from own user: megolm session ${request.sessionId}")

            if (request.requestingDeviceId == credentials.deviceId) {
                // ignore it's a remote echo
                return
            }
            // If it's verified we share from the early index we know
            // if not we check if it was originaly shared or not
            if (requestingDevice.isVerified) {
                // we share from the earliest known chain index
                shareMegolmKey(request, requestingDevice, null)
            } else {
                shareIfItWasPreviouslyShared(request, requestingDevice)
            }
        } else {
            Timber.tag(loggerTag.value).v("handling request from other user: megolm session ${request.sessionId}")
            if (requestingDevice.isBlocked) {
                // it's blocked, so send a withheld code
                sendWithheldForRequest(request, WithHeldCode.BLACKLISTED)
            } else {
                shareIfItWasPreviouslyShared(request, requestingDevice)
            }
        }
    }

    private suspend fun shareIfItWasPreviouslyShared(request: ValidMegolmRequestBody, requestingDevice: CryptoDeviceInfo) {
        // we don't reshare unless it was previously shared with
        val wasSessionSharedWithUser = withContext(coroutineDispatchers.crypto) {
            cryptoStore.getSharedSessionInfo(request.roomId, request.sessionId, requestingDevice)
        }
        if (wasSessionSharedWithUser.found && wasSessionSharedWithUser.chainIndex != null) {
            // we share from the index it was previously shared with
            shareMegolmKey(request, requestingDevice, wasSessionSharedWithUser.chainIndex.toLong())
        } else {
            sendWithheldForRequest(request, WithHeldCode.UNAUTHORISED)
            // TODO if it's our device we could delegate to the app layer to decide?
        }
    }

    private suspend fun sendWithheldForRequest(request: ValidMegolmRequestBody, code: WithHeldCode) {
        val withHeldContent = RoomKeyWithHeldContent(
                roomId = request.roomId,
                senderKey = request.senderKey,
                algorithm = MXCRYPTO_ALGORITHM_MEGOLM,
                sessionId = request.sessionId,
                codeString = code.value,
                fromDevice = credentials.deviceId
        )

        val params = SendToDeviceTask.Params(
                EventType.ROOM_KEY_WITHHELD,
                MXUsersDevicesMap<Any>().apply {
                    setObject(request.requestingUserId, request.requestingDeviceId, withHeldContent)
                }
        )
        try {
            withContext(coroutineDispatchers.io) {
                sendToDeviceTask.execute(params)
                Timber.tag(loggerTag.value)
                        .d("Send withheld $code req: ${request.shortDbgString()}")
            }

            cryptoStore.saveWithheldAuditTrail(
                    request.roomId,
                    request.sessionId,
                    request.senderKey,
                    MXCRYPTO_ALGORITHM_MEGOLM,
                    code,
                    request.requestingUserId,
                    request.requestingDeviceId
            )
        } catch (failure: Throwable) {
            // Ignore it's not that important?
            // do we want to fallback to a worker?
            Timber.tag(loggerTag.value)
                    .w("Failed to send withheld $code req: ${request.shortDbgString()} reason:${failure.localizedMessage}")
        }
    }

    private suspend fun shareMegolmKey(validRequest: ValidMegolmRequestBody,
                                       requestingDevice: CryptoDeviceInfo,
                                       chainIndex: Long?): Boolean {
        Timber.tag(loggerTag.value)
                .d("try to re-share Megolm Key at index $chainIndex for ${validRequest.shortDbgString()}")

        val devicesByUser = mapOf(validRequest.requestingUserId to listOf(requestingDevice))
        val usersDeviceMap = try {
            ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .w("Failed to establish olm session")
            sendWithheldForRequest(validRequest, WithHeldCode.NO_OLM)
            return false
        }

        val olmSessionResult = usersDeviceMap.getObject(requestingDevice.userId, requestingDevice.deviceId)
        if (olmSessionResult?.sessionId == null) {
            Timber.tag(loggerTag.value)
                    .w("reshareKey: no session with this device, probably because there were no one-time keys")
            sendWithheldForRequest(validRequest, WithHeldCode.NO_OLM)
            return false
        }
        val sessionHolder = try {
            olmDevice.getInboundGroupSession(validRequest.sessionId, validRequest.senderKey, validRequest.roomId)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .e(failure, "shareKeysWithDevice: failed to get session ${validRequest.requestingUserId}")
            // It's unavailable
            sendWithheldForRequest(validRequest, WithHeldCode.UNAVAILABLE)
            return false
        }

        val export = sessionHolder.mutex.withLock {
            sessionHolder.wrapper.exportKeys(chainIndex)
        } ?: return false.also {
            Timber.tag(loggerTag.value)
                    .e("shareKeysWithDevice: failed to export group session ${validRequest.sessionId}")
        }

        val payloadJson = mapOf(
                "type" to EventType.FORWARDED_ROOM_KEY,
                "content" to export
        )

        val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(requestingDevice))
        val sendToDeviceMap = MXUsersDevicesMap<Any>()
        sendToDeviceMap.setObject(requestingDevice.userId, requestingDevice.deviceId, encodedPayload)
        Timber.tag(loggerTag.value).d("reshareKey() : try sending session ${validRequest.sessionId} to ${requestingDevice.shortDebugString()}")
        val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
        return try {
            sendToDeviceTask.execute(sendToDeviceParams)
            Timber.tag(loggerTag.value)
                    .i("successfully re-shared session ${validRequest.sessionId} to ${requestingDevice.shortDebugString()}")
            cryptoStore.saveForwardKeyAuditTrail(
                    validRequest.roomId,
                    validRequest.sessionId,
                    validRequest.senderKey,
                    MXCRYPTO_ALGORITHM_MEGOLM,
                    requestingDevice.userId,
                    requestingDevice.deviceId,
                    chainIndex)
            true
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .e(failure, "fail to re-share session ${validRequest.sessionId} to ${requestingDevice.shortDebugString()}")
            false
        }
    }

    fun addRoomKeysRequestListener(listener: GossipingRequestListener) {
        synchronized(gossipingRequestListeners) {
            // TODO
            gossipingRequestListeners.add(listener)
        }
    }

    fun removeRoomKeysRequestListener(listener: GossipingRequestListener) {
        synchronized(gossipingRequestListeners) {
            // TODO
            gossipingRequestListeners.remove(listener)
        }
    }

    fun close() {
        try {
            outgoingRequestScope.cancel("User Terminate")
            incomingRequestBuffer.clear()
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).w("Failed to shutDown request manager")
        }
    }
}
