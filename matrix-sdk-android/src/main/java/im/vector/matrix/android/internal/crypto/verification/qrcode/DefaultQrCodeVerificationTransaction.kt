/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.verification.qrcode

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.QrCodeVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationTxState
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.model.rest.SignatureUploadResponse
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.verification.DefaultVerificationTransaction
import im.vector.matrix.android.internal.crypto.verification.VerificationInfo
import im.vector.matrix.android.internal.crypto.verification.VerificationInfoStart
import timber.log.Timber
import kotlin.properties.Delegates

internal class DefaultQrCodeVerificationTransaction(
        override val transactionId: String,
        override val otherUserId: String,
        override var otherDeviceId: String?,
        private val crossSigningService: CrossSigningService,
        private val cryptoStore: IMXCryptoStore,
        private val myGeneratedSecret: String,
        override val qrCodeText: String,
        val deviceId: String,
        override val isIncoming: Boolean
) : DefaultVerificationTransaction(transactionId, otherUserId, otherDeviceId, isIncoming), QrCodeVerificationTransaction {

    override var cancelledReason: CancelCode? = null

    override var state by Delegates.observable(VerificationTxState.None) { _, _, _ ->
        listeners.forEach {
            try {
                it.transactionUpdated(this)
            } catch (e: Throwable) {
                Timber.e(e, "## Error while notifying listeners")
            }
        }
    }

    override fun userHasScannedRemoteQrCode(otherQrCodeText: String): CancelCode? {
        val qrCodeData = otherQrCodeText.toQrCodeData() ?: return CancelCode.QrCodeInvalid

        // Perform some checks
        if (qrCodeData.action != QrCodeData.ACTION_VERIFY) {
            return CancelCode.QrCodeInvalid
        }

        if (qrCodeData.userId != otherUserId) {
            return CancelCode.UserMismatchError
        }

        if (qrCodeData.requestEventId != transactionId) {
            return CancelCode.QrCodeInvalid
        }

        // check master key
        if (qrCodeData.otherUserKey != crossSigningService.getUserCrossSigningKeys(otherUserId)?.masterKey()?.unpaddedBase64PublicKey) {
            return CancelCode.MismatchedKeys
        }

        val otherDevices = cryptoStore.getUserDevices(otherUserId)
        qrCodeData.keys.keys.forEach { key ->
            Timber.w("Checking key $key")
            val fingerprint = otherDevices?.get(key)?.fingerprint()
            if (fingerprint != null && fingerprint != qrCodeData.keys[key]) {
                return CancelCode.MismatchedKeys
            }
        }

        // All checks are correct

        // Trust the other user
        trust()
        state = VerificationTxState.Verified

        // Send the shared secret so that sender can trust me
        // qrCodeData.sharedSecret will be used to send the start request
        start(qrCodeData.sharedSecret)

        return null
    }

    fun start(remoteSecret: String) {
        if (state != VerificationTxState.None) {
            Timber.e("## SAS O: start verification from invalid state")
            // should I cancel??
            throw IllegalStateException("Interactive Key verification already started")
        }

        val startMessage = transport.createStartForQrCode(
                deviceId,
                transactionId,
                remoteSecret
        )

        transport.sendToOther(
                EventType.KEY_VERIFICATION_START,
                startMessage,
                VerificationTxState.Started,
                CancelCode.User,
                null
        )
    }

    override fun acceptVerificationEvent(senderId: String, info: VerificationInfo) {
    }

    override fun cancel() {
        cancel(CancelCode.User)
    }

    override fun cancel(code: CancelCode) {
        cancelledReason = code
        state = VerificationTxState.Cancelled
        transport.cancelTransaction(transactionId, otherUserId, otherDeviceId ?: "", code)
    }

    override fun isToDeviceTransport() = false

    // Remote user has scanned our QR code. check that the secret matched, so we can trust him
    fun onStartReceived(startReq: VerificationInfoStart) {
        if (startReq.sharedSecret == myGeneratedSecret) {
            // Ok, we can trust the other user
            trust()
        } else {
            // Display a warning
            cancelledReason = CancelCode.QrCodeInvalid
            state = VerificationTxState.OnCancelled
        }
    }

    private fun trust() {
        crossSigningService.trustUser(otherUserId, object : MatrixCallback<SignatureUploadResponse> {
            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## QR Verification: Failed to trust User $otherUserId")
            }
        })

        // TODO Sign devices

    }

    // TODO Send the done event
}
