/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.verification.prepareMethods
import timber.log.Timber
import uniffi.olm.OlmMachine
import uniffi.olm.VerificationRequest

/** A verification request object
 *
 * This represents a verification flow that starts with a m.key.verification.request event
 *
 * Once the VerificationRequest gets to a ready state users can transition into the different
 * concrete verification flows.
 */
internal class VerificationRequest(
        private val machine: OlmMachine,
        private var inner: VerificationRequest,
        private val sender: RequestSender,
        private val listeners: ArrayList<VerificationService.Listener>,
) {
    private val uiHandler = Handler(Looper.getMainLooper())

    internal fun dispatchRequestUpdated() {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.verificationRequestUpdated(this.toPendingVerificationRequest())
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    /** Get the flow ID of this verification request
     *
     * This is either the transaction ID if the verification is happening
     * over to-device events, or the event ID of the m.key.verification.request
     * event that initiated the flow.
     */
    internal fun flowId(): String {
        return this.inner.flowId
    }

    /** The user ID of the other user that is participating in this verification flow */
    internal fun otherUser(): String {
        return this.inner.otherUserId
    }

    /** The device ID of the other user's device that is participating in this verification flow
     *
     * This will we null if we're initiating the request and the other side
     * didn't yet accept the verification flow.
     * */
    internal fun otherDeviceId(): String? {
        refreshData()
        return this.inner.otherDeviceId
    }

    /** Did we initiate this verification flow */
    internal fun weStarted(): Boolean {
        return this.inner.weStarted
    }

    /** Get the id of the room where this verification is happening
     *
     * Will be null if the verification is not happening inside a room.
     */
    internal fun roomId(): String? {
        return this.inner.roomId
    }

    /** Did the non-initiating side respond with a m.key.verification.read event
     *
     * Once the verification request is ready, we're able to transition into a
     * concrete verification flow, i.e. we can show/scan a QR code or start emoji
     * verification.
     */
    internal fun isReady(): Boolean {
        refreshData()
        return this.inner.isReady
    }

    /** Did we advertise that we're able to scan QR codes */
    internal fun canScanQrCodes(): Boolean {
        refreshData()
        return this.inner.ourMethods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN) ?: false
    }

    /** Accept the verification request advertising the given methods as supported
     *
     * This will send out a m.key.verification.ready event advertising support for
     * the given verification methods to the other side. After this method call, the
     * verification request will be considered to be ready and will be able to transition
     * into concrete verification flows.
     *
     * The method turns into a noop, if the verification flow has already been accepted
     * and is in the ready state, which can be checked with the isRead() method.
     *
     * @param methods The list of VerificationMethod that we wish to advertise to the other
     * side as supported.
     */
    suspend fun acceptWithMethods(methods: List<VerificationMethod>) {
        val stringMethods = prepareMethods(methods)

        val request = this.machine.acceptVerificationRequest(
                this.inner.otherUserId,
                this.inner.flowId,
                stringMethods
        )

        if (request != null) {
            this.sender.sendVerificationRequest(request)
            this.dispatchRequestUpdated()
        }
    }

    /** Transition from a ready verification request into emoji verification
     *
     * This method will move the verification forward into emoji verification,
     * it will send out a m.key.verification.start event with the method set to
     * m.sas.v1.
     *
     * Note: This method will be a noop and return null if the verification request
     * isn't considered to be ready, you can check if the request is ready using the
     * isReady() method.
     *
     * @return A freshly created SasVerification object that represents the newly started
     * emoji verification, or null if we can't yet transition into emoji verification.
     */
    internal suspend fun startSasVerification(): SasVerification? {
        return withContext(Dispatchers.IO) {
            val result = machine.startSasVerification(inner.otherUserId, inner.flowId)

            if (result != null) {
                sender.sendVerificationRequest(result.request)
                SasVerification(machine, result.sas, sender, listeners)
            } else {
                null
            }
        }
    }

    /** Scan a QR code and transition into QR code verification
     *
     * This method will move the verification forward into QR code verification.
     * It will send out a m.key.verification.start event with the method
     * set to m.reciprocate.v1.
     *
     * Note: This method will be a noop and return null if the verification request
     * isn't considered to be ready, you can check if the request is ready using the
     * isReady() method.
     *
     * @return A freshly created QrCodeVerification object that represents the newly started
     * QR code verification, or null if we can't yet transition into QR code verification.
     */
    internal suspend fun scanQrCode(data: String): QrCodeVerification? {
        // TODO again, what's the deal with ISO_8859_1?
        val byteArray = data.toByteArray(Charsets.ISO_8859_1)
        val encodedData = byteArray.toBase64NoPadding()
        val result = this.machine.scanQrCode(this.otherUser(), this.flowId(), encodedData) ?: return null

        this.sender.sendVerificationRequest(result.request)
        return QrCodeVerification(this.machine, this, result.qr, this.sender, this.listeners)
    }

    /** Transition into a QR code verification to display a QR code
     *
     * This method will move the verification forward into QR code verification.
     * It will not send out any event out, it should instead be used to display
     * a QR code which then can be scanned out of bound by the other side.
     *
     * A m.key.verification.start event with the method set to m.reciprocate.v1
     * incoming from the other side will only be accepted if this method is called
     * and the QR code verification is successfully initiated.
     *
     * Note: This method will be a noop and return null if the verification request
     * isn't considered to be ready, you can check if the request is ready using the
     * isReady() method.
     *
     * @return A freshly created QrCodeVerification object that represents the newly started
     * QR code verification, or null if we can't yet transition into QR code verification.
     */
    internal fun startQrVerification(): QrCodeVerification? {
        val qrcode = this.machine.startQrVerification(this.inner.otherUserId, this.inner.flowId)

        return if (qrcode != null) {
            QrCodeVerification(
                    this.machine,
                    this,
                    qrcode,
                    this.sender,
                    this.listeners,
            )
        } else {
            null
        }
    }

    /** Cancel the verification flow
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to m.user.
     *
     * Cancelling the verification request will also cancel any QrcodeVerification and
     * SasVerification objects that are related to this verification request.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     */
    internal suspend fun cancel() {
        val request = this.machine.cancelVerification(
                this.inner.otherUserId,
                this.inner.flowId,
                CancelCode.User.value
        )

        if (request != null) {
            this.sender.sendVerificationRequest(request)
            this.dispatchRequestUpdated()
        }
    }

    /** Fetch fresh data from the Rust side for our verification flow */
    private fun refreshData() {
        val request = this.machine.getVerificationRequest(this.inner.otherUserId, this.inner.flowId)

        if (request != null) {
            this.inner = request
        }
    }

    /** Convert the VerificationRequest into a PendingVerificationRequest
     *
     * The public interface of the VerificationService dispatches the data class
     * PendingVerificationRequest, this method allows us to easily transform this
     * request into the data class. It fetches fresh info from the Rust side before
     * it does the transform.
     *
     * @return The PendingVerificationRequest that matches data from this VerificationRequest.
     */
    internal fun toPendingVerificationRequest(): PendingVerificationRequest {
        refreshData()
        val cancelInfo = this.inner.cancelInfo
        val cancelCode =
                if (cancelInfo != null) {
                    safeValueOf(cancelInfo.cancelCode)
                } else {
                    null
                }

        val ourMethods = this.inner.ourMethods
        val theirMethods = this.inner.theirMethods
        val otherDeviceId = this.inner.otherDeviceId

        var requestInfo: ValidVerificationInfoRequest? = null
        var readyInfo: ValidVerificationInfoReady? = null

        if (this.inner.weStarted && ourMethods != null) {
            requestInfo =
                    ValidVerificationInfoRequest(
                            this.inner.flowId,
                            this.machine.deviceId(),
                            ourMethods,
                            null,
                    )
        } else if (!this.inner.weStarted && ourMethods != null) {
            readyInfo =
                    ValidVerificationInfoReady(
                            this.inner.flowId,
                            this.machine.deviceId(),
                            ourMethods,
                    )
        }

        if (this.inner.weStarted && theirMethods != null && otherDeviceId != null) {
            readyInfo =
                    ValidVerificationInfoReady(
                            this.inner.flowId,
                            otherDeviceId,
                            theirMethods,
                    )
        } else if (!this.inner.weStarted && theirMethods != null && otherDeviceId != null) {
            requestInfo =
                    ValidVerificationInfoRequest(
                            this.inner.flowId,
                            otherDeviceId,
                            theirMethods,
                            System.currentTimeMillis(),
                    )
        }

        return PendingVerificationRequest(
                // Creation time
                System.currentTimeMillis(),
                // Who initiated the request
                !this.inner.weStarted,
                // Local echo id, what to do here?
                this.inner.flowId,
                // other user
                this.inner.otherUserId,
                // room id
                this.inner.roomId,
                // transaction id
                this.inner.flowId,
                // val requestInfo: ValidVerificationInfoRequest? = null,
                requestInfo,
                // val readyInfo: ValidVerificationInfoReady? = null,
                readyInfo,
                // cancel code if there is one
                cancelCode,
                // are we done/successful
                this.inner.isDone,
                // did another device answer the request
                this.inner.isPassive,
                // devices that should receive the events we send out
                null,
        )
    }
}
