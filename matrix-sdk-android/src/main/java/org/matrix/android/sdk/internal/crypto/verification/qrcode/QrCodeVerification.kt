/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.QRCodeVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.crypto.verification.VerificationListenersHolder
import org.matrix.rustcomponents.sdk.crypto.CryptoStoreException
import org.matrix.rustcomponents.sdk.crypto.QrCode
import org.matrix.rustcomponents.sdk.crypto.QrCodeState
import timber.log.Timber

/** Class representing a QR code based verification flow. */
internal class QrCodeVerification @AssistedInject constructor(
        @Assisted private var inner: QrCode,
        private val olmMachine: OlmMachine,
        private val sender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val verificationListenersHolder: VerificationListenersHolder,
) : QrCodeVerificationTransaction {

    @AssistedFactory
    interface Factory {
        fun create(inner: QrCode): QrCodeVerification
    }

    override val method: VerificationMethod
        get() = VerificationMethod.QR_CODE_SCAN

    private val innerMachine = olmMachine.inner()

    private fun dispatchTxUpdated() {
        refreshData()
        verificationListenersHolder.dispatchTxUpdated(this)
    }

    /** Generate, if possible, data that should be encoded as a QR code for QR code verification.
     *
     * QR code verification can't verify devices between two users, so in the case that
     * we're verifying another user and we don't have or trust our cross signing identity
     * no QR code will be generated.
     *
     * @return A ISO_8859_1 encoded string containing data that should be encoded as a QR code.
     * The string contains data as specified in the [QR code format] part of the Matrix spec.
     * The list of bytes as defined in the spec are then encoded using ISO_8859_1 to get a string.
     *
     * [QR code format]: https://spec.matrix.org/unstable/client-server-api/#qr-code-format
     */
    override val qrCodeText: String?
        get() {
            val data = inner.generateQrCode()

            // TODO Why are we encoding this to ISO_8859_1? If we're going to encode, why not base64?
            return data?.fromBase64()?.toString(Charsets.ISO_8859_1)
        }

    /** Pass the data from a scanned QR code into the QR code verification object */
//    override suspend fun userHasScannedOtherQrCode(otherQrCodeText: String) {
//        request.scanQrCode(otherQrCodeText)
//        dispatchTxUpdated()
//    }

    /** Confirm that the other side has indeed scanned the QR code we presented. */
    override suspend fun otherUserScannedMyQrCode() {
        confirm()
    }

    /** Cancel the QR code verification, denying that the other side has scanned the QR code. */
    override suspend fun otherUserDidNotScannedMyQrCode() {
        // TODO Is this code correct here? The old code seems to do this
        cancelHelper(CancelCode.MismatchedKeys)
    }

    override fun state(): QRCodeVerificationState {
        Timber.v("SAS QR state${inner.state()}")
        return when (inner.state()) {
            // / The QR verification has been started.
            QrCodeState.Started -> QRCodeVerificationState.Reciprocated
            // / The QR verification has been scanned by the other side.
            QrCodeState.Scanned -> QRCodeVerificationState.WaitingForScanConfirmation
            // / The scanning of the QR code has been confirmed by us.
            QrCodeState.Confirmed -> QRCodeVerificationState.WaitingForOtherDone
            // / We have successfully scanned the QR code and are able to send a
            // / reciprocation event.
            QrCodeState.Reciprocated -> QRCodeVerificationState.WaitingForOtherDone
            // / The verification process has been successfully concluded.
            QrCodeState.Done -> QRCodeVerificationState.Done
            is QrCodeState.Cancelled -> QRCodeVerificationState.Cancelled
        }
    }

    /** Get the unique id of this verification. */
    override val transactionId: String
        get() = inner.flowId()

    /** Get the user id of the other user participating in this verification flow. */
    override val otherUserId: String
        get() = inner.otherUserId()

    /** Get the device id of the other user's device participating in this verification flow. */
    override var otherDeviceId: String?
        get() = inner.otherDeviceId()
        @Suppress("UNUSED_PARAMETER")
        set(value) {
        }

    /** Did the other side initiate this verification flow. */
    override val isIncoming: Boolean
        get() = !inner.weStarted()

    /** Cancel the verification flow.
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to m.user.
     *
     * Cancelling the verification request will also cancel the parent VerificationRequest.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     * */
    override suspend fun cancel() {
        cancelHelper(CancelCode.User)
    }

    /** Cancel the verification flow.
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to the given CancelCode.
     *
     * Cancelling the verification request will also cancel the parent VerificationRequest.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     *
     * @param code The cancel code that should be given as the reason for the cancellation.
     * */
    override suspend fun cancel(code: CancelCode) {
        cancelHelper(code)
    }

    /** Is this verification happening over to-device messages. */
    override fun isToDeviceTransport(): Boolean {
        return inner.roomId() == null
    }

    /** Confirm the QR code verification.
     *
     * This confirms that the other side has scanned our QR code and sends
     * out a m.key.verification.done event to the other side.
     *
     * The method turns into a noop if we're not yet ready to confirm the scanning,
     * i.e. we didn't yet receive a m.key.verification.start event from the other side.
     */
    @Throws(CryptoStoreException::class)
    private suspend fun confirm() {
        val result = withContext(coroutineDispatchers.io) {
            inner.confirm()
        } ?: return
        dispatchTxUpdated()
        try {
            for (verificationRequest in result.requests) {
                sender.sendVerificationRequest(verificationRequest)
            }
            val signatureRequest = result.signatureRequest
            if (signatureRequest != null) {
                sender.sendSignatureUpload(signatureRequest)
            }
        } catch (failure: Throwable) {
            cancelHelper(CancelCode.UserError)
        }
    }

    private suspend fun cancelHelper(code: CancelCode) = withContext(NonCancellable) {
        val request = inner.cancel(code.value) ?: return@withContext
        dispatchTxUpdated()
        tryOrNull("Fail to send cancel verification request") {
            sender.sendVerificationRequest(request, retryCount = Int.MAX_VALUE)
        }
    }

    /** Fetch fresh data from the Rust side for our verification flow. */
    private fun refreshData() {
        innerMachine.getVerification(inner.otherUserId(), inner.flowId())
                ?.asQr()?.let {
                    inner = it
                }
//        when (val verification = innerMachine.getVerification(request.otherUser(), request.flowId())) {
//            is Verification.QrCodeV1 -> {
//                inner = verification.qrcode
//            }
//            else                     -> {
//            }
//        }

        return
    }

    override fun toString(): String {
        return "QrCodeVerification(" +
                "qrCodeText=$qrCodeText, " +
                "state=${state()}, " +
                "transactionId='$transactionId', " +
                "otherUserId='$otherUserId', " +
                "otherDeviceId=$otherDeviceId, " +
                "isIncoming=$isIncoming)"
    }
}
