/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.sync.handler

import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.OlmEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.algorithms.DecryptionResult
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
        toDevice.events?.forEachIndexed { index, event ->
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
    private suspend fun decryptToDeviceEvent(event: Event, timelineId: String?) {
        Timber.v("## CRYPTO | decryptToDeviceEvent")
        if (event.getClearType() == EventType.ENCRYPTED) {
            when (val result = cryptoService.decryptEvent(event, timelineId ?: "")) {
                is DecryptionResult.Success -> {
                    val decryptionResult = result.decryptedResult
                    event.mxDecryptionResult = OlmDecryptionResult(
                            payload = decryptionResult.clearEvent,
                            senderKey = decryptionResult.senderCurve25519Key,
                            keysClaimed = decryptionResult.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                            forwardingCurve25519KeyChain = decryptionResult.forwardingCurve25519KeyChain
                    )
                }
                is DecryptionResult.Failure -> {
                    val exception = result.error
                    event.mCryptoError = (exception as? MXCryptoError.Base)?.errorType ?: MXCryptoError.ErrorType.UNABLE_TO_DECRYPT
                    event.mCryptoWithHeldCode = result.withheldInfo
                    val senderKey = event.content.toModel<OlmEventContent>()?.senderKey ?: "<unknown sender key>"
                    // try to find device id to ease log reading
                    val deviceId = event.senderId?.let {
                        cryptoService.getCryptoDeviceInfo(it).firstOrNull {
                            it.identityKey() == senderKey
                        }?.deviceId
                    } ?: senderKey
                    Timber.e("## CRYPTO | Failed to decrypt to device event from ${event.senderId}|$deviceId reason:<${event.mCryptoError ?: exception}>")
                }
            }
        }
    }
}
