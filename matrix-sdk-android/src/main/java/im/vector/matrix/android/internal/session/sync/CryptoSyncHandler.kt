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

import android.text.TextUtils
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.crypto.MXDecryptionException
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.verification.DefaultSasVerificationService
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.session.sync.model.ToDeviceSyncResponse
import timber.log.Timber


internal class CryptoSyncHandler(private val cryptoManager: CryptoManager,
                                 private val sasVerificationService: DefaultSasVerificationService) {

    fun handleToDevice(toDevice: ToDeviceSyncResponse) {
        toDevice.events?.forEach { event ->
            // Decrypt event if necessary
            decryptEvent(event, null)

            if (TextUtils.equals(event.getClearType(), EventType.MESSAGE)
                    && event.mClearEvent?.content?.toModel<MessageContent>()?.type == "m.bad.encrypted") {
                Timber.e("## handleToDeviceEvent() : Warning: Unable to decrypt to-device event : " + event.content)
            } else {
                sasVerificationService.onToDeviceEvent(event)
                cryptoManager.onToDeviceEvent(event)
            }
        }
    }

    fun onSyncCompleted(syncResponse: SyncResponse, fromToken: String?, catchingUp: Boolean) {
        cryptoManager.onSyncCompleted(syncResponse)
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
                result = cryptoManager.decryptEvent(event, timelineId ?: "")
            } catch (exception: MXDecryptionException) {
                event.setCryptoError(exception.cryptoError)
            }

            if (null != result) {
                event.setClearData(result)
                return true
            }
        }

        return false
    }
}