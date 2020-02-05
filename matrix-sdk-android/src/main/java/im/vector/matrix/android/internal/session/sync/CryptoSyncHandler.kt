/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.crypto.verification.DefaultVerificationService
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.session.sync.model.ToDeviceSyncResponse
import timber.log.Timber
import javax.inject.Inject

internal class CryptoSyncHandler @Inject constructor(private val cryptoService: DefaultCryptoService,
                                                     private val verificationService: DefaultVerificationService) {

    fun handleToDevice(toDevice: ToDeviceSyncResponse, initialSyncProgressService: DefaultInitialSyncProgressService? = null) {
        val total = toDevice.events?.size ?: 0
        toDevice.events?.forEachIndexed { index, event ->
            initialSyncProgressService?.reportProgress(((index / total.toFloat()) * 100).toInt())
            // Decrypt event if necessary
            decryptEvent(event, null)
            if (event.getClearType() == EventType.MESSAGE
                    && event.getClearContent()?.toModel<MessageContent>()?.msgType == "m.bad.encrypted") {
                Timber.e("## handleToDeviceEvent() : Warning: Unable to decrypt to-device event : ${event.content}")
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
     * Decrypt an encrypted event
     *
     * @param event      the event to decrypt
     * @param timelineId the timeline identifier
     * @return true if the event has been decrypted
     */
    private fun decryptEvent(event: Event, timelineId: String?): Boolean {
        if (event.getClearType() == EventType.ENCRYPTED) {
            var result: MXEventDecryptionResult? = null
            try {
                result = cryptoService.decryptEvent(event, timelineId ?: "")
            } catch (exception: MXCryptoError) {
                event.mCryptoError = (exception as? MXCryptoError.Base)?.errorType // setCryptoError(exception.cryptoError)
            }

            if (null != result) {
//                event.mxDecryptionResult = MXDecryptionResult(
//                        payload = result.clearEvent,
//                        keysClaimed = map
//                )
                // TODO persist that?
                event.mxDecryptionResult = OlmDecryptionResult(
                        payload = result.clearEvent,
                        senderKey = result.senderCurve25519Key,
                        keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                )
//                event.setClearData(result)
                return true
            }
        }

        return false
    }
}
