/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import timber.log.Timber
import uniffi.olm.CryptoStoreErrorException
import uniffi.olm.OlmMachine
import uniffi.olm.OutgoingVerificationRequest
import uniffi.olm.QrCode
import uniffi.olm.Verification

internal class QrCodeVerification(
        private val machine: OlmMachine,
        private var request: VerificationRequest,
        private var inner: QrCode?,
        private val sender: RequestSender,
        private val listeners: ArrayList<VerificationService.Listener>,
) : QrCodeVerificationTransaction {
    private val uiHandler = Handler(Looper.getMainLooper())

    private fun dispatchTxUpdated() {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.transactionUpdated(this)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    /** Generate, if possible, data that should be encoded as a QR code for QR code verification.
     *
     * QR code verification can't verify devices between two users, so in the case that
     * we're verifying another user and we don't have or trust our cross signing identity
     * no QR code will be generated.
     */
    override val qrCodeText: String?
        get() {
            val data = this.inner?.let { this.machine.generateQrCode(it.otherUserId, it.flowId) }

            // TODO Why are we encoding this to ISO_8859_1? If we're going to encode, why not base64?
            return data?.fromBase64()?.toString(Charsets.ISO_8859_1)
        }

    /** Pass the data from a scanned QR code into the QR code verification object */
    override fun userHasScannedOtherQrCode(otherQrCodeText: String) {
        runBlocking {
            request.scanQrCode(otherQrCodeText)
        }
        dispatchTxUpdated()
    }

    /** Confirm that the other side has indeed scanned the QR code we presented */
    override fun otherUserScannedMyQrCode() {
        runBlocking { confirm() }
    }

    /** Cancel the QR code verification, denying that the other side has scanned the QR code */
    override fun otherUserDidNotScannedMyQrCode() {
        // TODO Is this code correct here? The old code seems to do this
        cancelHelper(CancelCode.MismatchedKeys)
    }

    override var state: VerificationTxState
        get() {
            refreshData()
            val inner = this.inner
            val cancelInfo = inner?.cancelInfo

            return if (inner != null) {
                when {
                    cancelInfo != null     -> {
                        val cancelCode = safeValueOf(cancelInfo.cancelCode)
                        val byMe = cancelInfo.cancelledByUs
                        VerificationTxState.Cancelled(cancelCode, byMe)
                    }
                    inner.isDone           -> VerificationTxState.Verified
                    inner.reciprocated     -> VerificationTxState.Started
                    inner.hasBeenConfirmed -> VerificationTxState.WaitingOtherReciprocateConfirm
                    inner.otherSideScanned -> VerificationTxState.QrScannedByOther
                    else                   -> {
                        VerificationTxState.None
                    }
                }
            } else {
                VerificationTxState.None
            }
        }
        @Suppress("UNUSED_PARAMETER")
        set(value) {
        }

    /** Get the unique id of this verification */
    override val transactionId: String
        get() = this.request.flowId()

    /** Get the user id of the other user participating in this verification flow */
    override val otherUserId: String
        get() = this.request.otherUser()

    /** Get the device id of the other user's device participating in this verification flow */
    override var otherDeviceId: String?
        get() = this.request.otherDeviceId()
        @Suppress("UNUSED_PARAMETER")
        set(value) {
        }

    /** Did the other side initiate this verification flow */
    override val isIncoming: Boolean
        get() = !this.request.weStarted()

    /** Cancel the verification flow */
    override fun cancel() {
        cancelHelper(CancelCode.User)
    }

    /** Cancel the verification with the given cancel code */
    override fun cancel(code: CancelCode) {
        cancelHelper(code)
    }

    /** Is this verification happening over to-device messages */
    override fun isToDeviceTransport(): Boolean {
        return this.request.roomId() == null
    }

    /** Confirm the QR code verification
     *
     * This confirms that the other side has scanned our QR code.
     */
    @Throws(CryptoStoreErrorException::class)
    private suspend fun confirm() {
        val request = withContext(Dispatchers.IO)
        {
            machine.confirmVerification(request.otherUser(), request.flowId())
        }

        if (request != null) {
            this.sender.sendVerificationRequest(request)
        }
    }

    private fun cancelHelper(code: CancelCode) {
        val request = this.machine.cancelVerification(this.request.otherUser(), this.request.flowId(), code.value)

        if (request != null) {
            sendRequest(request)
        }
    }

    /** Send out a verification request in a blocking manner*/
    private fun sendRequest(request: OutgoingVerificationRequest) {
        runBlocking { sender.sendVerificationRequest(request) }

        refreshData()
        dispatchTxUpdated()
    }

    /** Fetch fetch data from the Rust side for our verification flow */
    private fun refreshData() {
        when (val verification = this.machine.getVerification(this.request.otherUser(), this.request.flowId())) {
            is Verification.QrCodeV1 -> {
                this.inner = verification.qrcode
            }
            else                     -> {
            }
        }

        return
    }
}
