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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.event.OlmEventContent
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

private const val SEND_TO_DEVICE_RETRY_COUNT = 3

private val loggerTag = LoggerTag("CryptoSyncHandler", LoggerTag.CRYPTO)

@SessionScope
internal class EventDecryptor @Inject constructor(
        private val cryptoCoroutineScope: CoroutineScope,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val roomDecryptorProvider: RoomDecryptorProvider,
        private val messageEncrypter: MessageEncrypter,
        private val sendToDeviceTask: SendToDeviceTask,
        private val deviceListManager: DeviceListManager,
        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        private val cryptoStore: IMXCryptoStore
) {

    /**
     * Rate limit unwedge attempt, should we persist that?
     */
    private val lastNewSessionForcedDates = mutableMapOf<WedgedDeviceInfo, Long>()

    data class WedgedDeviceInfo(
            val userId: String,
            val senderKey: String?
    )

    private val wedgedDevices = mutableListOf<WedgedDeviceInfo>()

    /**
     * Decrypt an event
     *
     * @param event    the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the MXEventDecryptionResult data, or throw in case of error
     */
    @Throws(MXCryptoError::class)
    suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        return internalDecryptEvent(event, timeline)
    }

    /**
     * Decrypt an event asynchronously
     *
     * @param event    the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @param callback the callback to return data or null
     */
    fun decryptEventAsync(event: Event, timeline: String, callback: MatrixCallback<MXEventDecryptionResult>) {
        // is it needed to do that on the crypto scope??
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            runCatching {
                internalDecryptEvent(event, timeline)
            }.foldToCallback(callback)
        }
    }

    /**
     * Decrypt an event
     *
     * @param event    the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the MXEventDecryptionResult data, or null in case of error
     */
    @Throws(MXCryptoError::class)
    private suspend fun internalDecryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        val eventContent = event.content
        if (eventContent == null) {
            Timber.tag(loggerTag.value).e("decryptEvent : empty event content")
            throw MXCryptoError.Base(MXCryptoError.ErrorType.BAD_ENCRYPTED_MESSAGE, MXCryptoError.BAD_ENCRYPTED_MESSAGE_REASON)
        } else {
            val algorithm = eventContent["algorithm"]?.toString()
            val alg = roomDecryptorProvider.getOrCreateRoomDecryptor(event.roomId, algorithm)
            if (alg == null) {
                val reason = String.format(MXCryptoError.UNABLE_TO_DECRYPT_REASON, event.eventId, algorithm)
                Timber.tag(loggerTag.value).e("decryptEvent() : $reason")
                throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, reason)
            } else {
                try {
                    return alg.decryptEvent(event, timeline)
                } catch (mxCryptoError: MXCryptoError) {
                    Timber.tag(loggerTag.value).d("internalDecryptEvent : Failed to decrypt ${event.eventId} reason: $mxCryptoError")
                    if (algorithm == MXCRYPTO_ALGORITHM_OLM) {
                        if (mxCryptoError is MXCryptoError.Base &&
                                mxCryptoError.errorType == MXCryptoError.ErrorType.BAD_ENCRYPTED_MESSAGE) {
                            // need to find sending device
                            val olmContent = event.content.toModel<OlmEventContent>()
                            if (event.senderId != null && olmContent?.senderKey != null) {
                                markOlmSessionForUnwedging(event.senderId, olmContent.senderKey)
                            } else {
                                Timber.tag(loggerTag.value).d("Can't mark as wedge malformed")
                            }
                        }
                    }
                    throw mxCryptoError
                }
            }
        }
    }

    private fun markOlmSessionForUnwedging(senderId: String, senderKey: String) {
        val info = WedgedDeviceInfo(senderId, senderKey)
        if (!wedgedDevices.contains(info)) {
            Timber.tag(loggerTag.value).d("Marking device from $senderId key:$senderKey as wedged")
            wedgedDevices.add(info)
        }
    }

    // coroutineDispatchers.crypto scope
    suspend fun unwedgeDevicesIfNeeded() {
        // handle wedged devices
        // Some olm decryption have failed and some device are wedged
        // we should force start a new session for those
        Timber.tag(loggerTag.value).v("Unwedging:  ${wedgedDevices.size} are wedged")
        // get the one that should be retried according to rate limit
        val now = System.currentTimeMillis()
        val toUnwedge = wedgedDevices.filter {
            val lastForcedDate = lastNewSessionForcedDates[it] ?: 0
            if (now - lastForcedDate < DefaultCryptoService.CRYPTO_MIN_FORCE_SESSION_PERIOD_MILLIS) {
                Timber.tag(loggerTag.value).d("Unwedging, New session for $it already forced with device at $lastForcedDate")
                return@filter false
            }
            // let's already mark that we tried now
            lastNewSessionForcedDates[it] = now
            true
        }

        if (toUnwedge.isEmpty()) {
            Timber.tag(loggerTag.value).v("Nothing to unwedge")
            return
        }
        Timber.tag(loggerTag.value).d("Unwedging, trying to create new session for ${toUnwedge.size} devices")

        toUnwedge
                .chunked(100) // safer to chunk if we ever have lots of wedged devices
                .forEach { wedgedList ->
                    val groupedByUserId = wedgedList.groupBy { it.userId }
                    // lets download keys if needed
                    withContext(coroutineDispatchers.io) {
                        deviceListManager.downloadKeys(groupedByUserId.keys.toList(), false)
                    }

                    // find the matching devices
                    groupedByUserId
                            .map { groupedByUser ->
                                val userId = groupedByUser.key
                                val wedgeSenderKeysForUser = groupedByUser.value.map { it.senderKey }
                                val knownDevices = cryptoStore.getUserDevices(userId)?.values.orEmpty()
                                userId to wedgeSenderKeysForUser.mapNotNull { senderKey ->
                                    knownDevices.firstOrNull { it.identityKey() == senderKey }
                                }
                            }
                            .toMap()
                            .let { deviceList ->
                                try {
                                    // force creating new outbound session and mark them as most recent to
                                    // be used for next encryption (dummy)
                                    val sessionToUse = ensureOlmSessionsForDevicesAction.handle(deviceList, true)
                                    Timber.tag(loggerTag.value).d("Unwedging, found ${sessionToUse.map.size} to send dummy to")

                                    // Now send a dummy message on that session so the other side knows about it.
                                    val payloadJson = mapOf(
                                            "type" to EventType.DUMMY
                                    )
                                    val sendToDeviceMap = MXUsersDevicesMap<Any>()
                                    sessionToUse.map.values
                                            .flatMap { it.values }
                                            .map { it.deviceInfo }
                                            .forEach { deviceInfo ->
                                                Timber.tag(loggerTag.value).v("encrypting dummy to ${deviceInfo.deviceId}")
                                                val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
                                                sendToDeviceMap.setObject(deviceInfo.userId, deviceInfo.deviceId, encodedPayload)
                                            }

                                    // now let's send that
                                    val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
                                    withContext(coroutineDispatchers.io) {
                                        sendToDeviceTask.executeRetry(sendToDeviceParams, remainingRetry = SEND_TO_DEVICE_RETRY_COUNT)
                                    }
                                } catch (failure: Throwable) {
                                    deviceList.flatMap { it.value }.joinToString { it.shortDebugString() }.let {
                                        Timber.tag(loggerTag.value).e(failure, "## Failed to unwedge devices: $it}")
                                    }
                                }
                            }
                }
    }
}
