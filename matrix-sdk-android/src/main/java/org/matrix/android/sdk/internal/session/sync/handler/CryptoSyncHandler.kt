/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.handler

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_OLM
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.OlmEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.verification.DefaultVerificationService
import org.matrix.android.sdk.internal.session.sync.ProgressReporter
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("CryptoSyncHandler", LoggerTag.CRYPTO)

internal class CryptoSyncHandler @Inject constructor(
        private val cryptoService: DefaultCryptoService,
        private val verificationService: DefaultVerificationService
) {

    suspend fun handleToDevice(toDevice: ToDeviceSyncResponse, progressReporter: ProgressReporter? = null) {
        val total = toDevice.events?.size ?: 0
        toDevice.events
                ?.filter { isSupportedToDevice(it) }
                ?.forEachIndexed { index, event ->
                    progressReporter?.reportProgress(index * 100F / total)
                    // Decrypt event if necessary
                    Timber.tag(loggerTag.value).i("To device event from ${event.senderId} of type:${event.type}")
                    decryptToDeviceEvent(event, null)
                    if (event.getClearType() == EventType.MESSAGE &&
                            event.getClearContent()?.toModel<MessageContent>()?.msgType == "m.bad.encrypted") {
                        Timber.tag(loggerTag.value).e("handleToDeviceEvent() : Warning: Unable to decrypt to-device event : ${event.content}")
                    } else {
                        verificationService.onToDeviceEvent(event)
                        cryptoService.onToDeviceEvent(event)
                    }
                }
    }

    private val unsupportedPlainToDeviceEventTypes = listOf(
            EventType.ROOM_KEY,
            EventType.FORWARDED_ROOM_KEY,
            EventType.SEND_SECRET
    )

    private fun isSupportedToDevice(event: Event): Boolean {
        val algorithm = event.content?.get("algorithm") as? String
        val type = event.type.orEmpty()
        return if (event.isEncrypted()) {
            algorithm == MXCRYPTO_ALGORITHM_OLM
        } else {
            // some clear events are not allowed
            type !in unsupportedPlainToDeviceEventTypes
        }.also {
            if (!it) {
                Timber.tag(loggerTag.value)
                        .w("Ignoring unsupported to device event ${event.type} alg:${algorithm}")
            }
        }
    }

    fun onSyncCompleted(syncResponse: SyncResponse) {
        cryptoService.onSyncCompleted(syncResponse)
    }

    /**
     * Decrypt an encrypted event.
     *
     * @param event the event to decrypt
     * @param timelineId the timeline identifier
     * @return true if the event has been decrypted
     */
    private suspend fun decryptToDeviceEvent(event: Event, timelineId: String?): Boolean {
        Timber.v("## CRYPTO | decryptToDeviceEvent")
        if (event.getClearType() == EventType.ENCRYPTED) {
            var result: MXEventDecryptionResult? = null
            try {
                result = cryptoService.decryptEvent(event, timelineId ?: "")
            } catch (exception: MXCryptoError) {
                event.mCryptoError = (exception as? MXCryptoError.Base)?.errorType // setCryptoError(exception.cryptoError)
                val senderKey = event.content.toModel<OlmEventContent>()?.senderKey ?: "<unknown sender key>"
                // try to find device id to ease log reading
                val deviceId = cryptoService.getCryptoDeviceInfo(event.senderId!!).firstOrNull {
                    it.identityKey() == senderKey
                }?.deviceId ?: senderKey
                Timber.e("## CRYPTO | Failed to decrypt to device event from ${event.senderId}|$deviceId reason:<${event.mCryptoError ?: exception}>")
            } catch (failure: Throwable) {
                Timber.e(failure, "## CRYPTO | Failed to decrypt to device event from ${event.senderId}")
            }

            if (null != result) {
                event.mxDecryptionResult = OlmDecryptionResult(
                        payload = result.clearEvent,
                        senderKey = result.senderCurve25519Key,
                        keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                        isSafe = result.isSafe
                )
                return true
            } else {
                // Could happen for to device events
                // None of the known session could decrypt the message
                // In this case unwedging process might have been started (rate limited)
                Timber.e("## CRYPTO | ERROR NULL DECRYPTION RESULT from ${event.senderId}")
            }
        }

        return false
    }
}
