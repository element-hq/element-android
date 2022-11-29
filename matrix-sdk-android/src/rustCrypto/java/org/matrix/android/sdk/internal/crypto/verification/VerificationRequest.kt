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

package org.matrix.android.sdk.internal.crypto.verification

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.crypto.verification.qrcode.QrCodeVerification
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.rustcomponents.sdk.crypto.QrCode
import org.matrix.rustcomponents.sdk.crypto.VerificationRequest as InnerVerificationRequest

fun InnerVerificationRequest.dbgString(): String {
    val that = this
    return buildString {
        append("InnerVerificationRequest(")
        append("isDone=${that.isDone()},")
        append("isReady=${that.isReady()},")
        append("isPassive=${that.isPassive()},")
        append("weStarted=${that.weStarted()},")
        append("isCancelled=${that.isCancelled()}")
        append(")")
    }
}

/** A verification request object
 *
 * This represents a verification flow that starts with a m.key.verification.request event
 *
 * Once the VerificationRequest gets to a ready state users can transition into the different
 * concrete verification flows.
 */
internal class VerificationRequest @AssistedInject constructor(
        @Assisted private var innerVerificationRequest: InnerVerificationRequest,
        olmMachine: OlmMachine,
        private val requestSender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val verificationListenersHolder: VerificationListenersHolder,
        private val sasVerificationFactory: SasVerification.Factory,
        private val qrCodeVerificationFactory: QrCodeVerification.Factory,
        private val clock: Clock,
) {

    private val innerOlmMachine = olmMachine.inner()

    @AssistedFactory
    interface Factory {
        fun create(innerVerificationRequest: InnerVerificationRequest): VerificationRequest
    }

    internal fun dispatchRequestUpdated() {
        val tx = toPendingVerificationRequest()
        verificationListenersHolder.dispatchRequestUpdated(tx)
    }

    /** Get the flow ID of this verification request
     *
     * This is either the transaction ID if the verification is happening
     * over to-device events, or the event ID of the m.key.verification.request
     * event that initiated the flow.
     */
    internal fun flowId(): String {
        return innerVerificationRequest.flowId()
    }

    /** The user ID of the other user that is participating in this verification flow */
    internal fun otherUser(): String {
        return innerVerificationRequest.otherUserId()
    }

    /** The device ID of the other user's device that is participating in this verification flow
     *
     * This will we null if we're initiating the request and the other side
     * didn't yet accept the verification flow.
     * */
    internal fun otherDeviceId(): String? {
        refreshData()
        return innerVerificationRequest.otherDeviceId()
    }

    /** Did we initiate this verification flow */
    internal fun weStarted(): Boolean {
        return innerVerificationRequest.weStarted()
    }

    /** Get the id of the room where this verification is happening
     *
     * Will be null if the verification is not happening inside a room.
     */
    internal fun roomId(): String? {
        return innerVerificationRequest.roomId()
    }

    /** Did the non-initiating side respond with a m.key.verification.read event
     *
     * Once the verification request is ready, we're able to transition into a
     * concrete verification flow, i.e. we can show/scan a QR code or start emoji
     * verification.
     */
    internal fun isReady(): Boolean {
        refreshData()
        return innerVerificationRequest.isReady()
    }

    /** Did we advertise that we're able to scan QR codes */
    internal fun canScanQrCodes(): Boolean {
        refreshData()
        return innerVerificationRequest.ourSupportedMethods()?.contains(VERIFICATION_METHOD_QR_CODE_SCAN) ?: false
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

        val request = innerVerificationRequest.accept(stringMethods)
                ?: return // should throw here?
//        val request = innerOlmMachine.acceptVerificationRequest(
//                innerVerificationRequest.otherUserId(),
//                innerVerificationRequest.flowId,
//                stringMethods
//        ) ?: return

        try {
            dispatchRequestUpdated()
            requestSender.sendVerificationRequest(request)

            if (innerVerificationRequest.isReady()) {
                activeQRCode = innerVerificationRequest.startQrVerification()
            }
        } catch (failure: Throwable) {
            cancel(CancelCode.UserError)
        }
    }

    var activeQRCode: QrCode? = null

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
        return withContext(coroutineDispatchers.io) {
            val result = innerVerificationRequest.startSasVerification()
                    ?: return@withContext null
//            sasStartResult.request
//            val result = innerOlmMachine.startSasVerification(innerVerificationRequest.otherUserId, innerVerificationRequest.flowId) ?: return@withContext null
            try {
                requestSender.sendVerificationRequest(result.request)
                sasVerificationFactory.create(result.sas)
            } catch (failure: Throwable) {
                cancel(CancelCode.UserError)
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
//        val result = innerOlmMachine.scanQrCode(otherUser(), flowId(), encodedData) ?: return null
        val result = innerVerificationRequest.scanQrCode(encodedData) ?: return null
        try {
            requestSender.sendVerificationRequest(result.request)
        } catch (failure: Throwable) {
            cancel(CancelCode.UserError)
            return null
        }
        return qrCodeVerificationFactory.create(this, result.qr)
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
        activeQRCode = innerVerificationRequest.startQrVerification()
//        val qrcode = innerOlmMachine.startQrVerification(innerVerificationRequest.otherUserId, innerVerificationRequest.flowId)
        return if (activeQRCode != null) {
            TODO("Is this reciprocate or just doing nothing?")
//            activeQRCode.
//            qrCodeVerificationFactory.create(this, qrcode)
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
    internal suspend fun cancel(cancelCode: CancelCode = CancelCode.User) = withContext(NonCancellable) {
        // TODO damir how to add the code?
        val request = innerVerificationRequest.cancel() ?: return@withContext
//        val request = innerOlmMachine.cancelVerification(
//                innerVerificationRequest.otherUserId,
//                innerVerificationRequest.flowId,
//                cancelCode.value
//        ) ?: return@withContext
        dispatchRequestUpdated()
        tryOrNull("Fail to send cancel request") {
            requestSender.sendVerificationRequest(request, retryCount = Int.MAX_VALUE)
        }
    }

    /** Fetch fresh data from the Rust side for our verification flow */
    private fun refreshData() {
        val request = innerOlmMachine.getVerificationRequest(innerVerificationRequest.otherUserId(), innerVerificationRequest.flowId())

        if (request != null) {
            innerVerificationRequest = request
        }
    }

    private fun state(): EVerificationState {
        if (innerVerificationRequest.isCancelled()) {
            return EVerificationState.Cancelled
        }
        if (innerVerificationRequest.isPassive()) {
            return EVerificationState.HandledByOtherSession
        }
        if (innerVerificationRequest.isDone()) {
            return EVerificationState.Done
        }

        val started = innerOlmMachine.getVerification(otherUser(), flowId())
        if (started != null) {
            val asSas = started.asSas()
            if (asSas != null) {
                return if (asSas.weStarted()) {
                    EVerificationState.WeStarted
                } else {
                    EVerificationState.Started
                }
            }
            // TODO QR??
        }
        if (innerVerificationRequest.isReady()) {
            return EVerificationState.Ready
        }
        return if (weStarted()) {
            EVerificationState.WaitingForReady
        } else {
            EVerificationState.Requested
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
        val cancelInfo = innerVerificationRequest.cancelInfo()
        val cancelCode =
                if (cancelInfo != null) {
                    safeValueOf(cancelInfo.cancelCode)
                } else {
                    null
                }

        val ourMethods = innerVerificationRequest.ourSupportedMethods()
        val theirMethods = innerVerificationRequest.theirSupportedMethods()
        val otherDeviceId = innerVerificationRequest.otherDeviceId()

//        var requestInfo: ValidVerificationInfoRequest? = null
//        var readyInfo: ValidVerificationInfoReady? = null
//
//        if (innerVerificationRequest.weStarted && ourMethods != null) {
//            requestInfo =
//                    ValidVerificationInfoRequest(
//                            transactionId = innerVerificationRequest.flowId,
//                            fromDevice = innerOlmMachine.deviceId(),
//                            methods = ourMethods,
//                            timestamp = null,
//                    )
//        } else if (!innerVerificationRequest.weStarted && ourMethods != null) {
//            readyInfo =
//                    ValidVerificationInfoReady(
//                            transactionId = innerVerificationRequest.flowId,
//                            fromDevice = innerOlmMachine.deviceId(),
//                            methods = ourMethods,
//                    )
//        }
//
//        if (innerVerificationRequest.weStarted && theirMethods != null && otherDeviceId != null) {
//            readyInfo =
//                    ValidVerificationInfoReady(
//                            transactionId = innerVerificationRequest.flowId,
//                            fromDevice = otherDeviceId,
//                            methods = theirMethods,
//                    )
//        } else if (!innerVerificationRequest.weStarted && theirMethods != null && otherDeviceId != null) {
//            requestInfo =
//                    ValidVerificationInfoRequest(
//                            transactionId = innerVerificationRequest.flowId,
//                            fromDevice = otherDeviceId,
//                            methods = theirMethods,
//                            timestamp = clock.epochMillis(),
//                    )
//        }

        innerVerificationRequest.startQrVerification()

        return PendingVerificationRequest(
                // Creation time
                ageLocalTs = clock.epochMillis(),
                state = state(),
                // Who initiated the request
                isIncoming = !innerVerificationRequest.weStarted(),
                // Local echo id, what to do here?
                otherDeviceId = innerVerificationRequest.otherDeviceId(),
                // other user
                otherUserId = innerVerificationRequest.otherUserId(),
                // room id
                roomId = innerVerificationRequest.roomId(),
                // transaction id
                transactionId = innerVerificationRequest.flowId(),
                // cancel code if there is one
                cancelConclusion = cancelCode,
                isFinished = innerVerificationRequest.isDone() || innerVerificationRequest.isCancelled(),
                // did another device answer the request
                handledByOtherSession = innerVerificationRequest.isPassive(),
                // devices that should receive the events we send out
                targetDevices = otherDeviceId?.let { listOf(it) },
                // TODO qr,
                qrCodeText = activeQRCode?.generateQrCode(),
                isSasSupported = ourMethods.canSas() && theirMethods.canSas(),
                weShouldDisplayQRCode = theirMethods.canScanQR() && ourMethods.canShowQR(),
                weShouldShowScanOption = ourMethods.canScanQR() && theirMethods.canShowQR()
        )
    }

    override fun toString(): String {
        return super.toString() + "\n${innerVerificationRequest.dbgString()}"
    }

    private fun List<String>?.canSas() = orEmpty().contains(VERIFICATION_METHOD_SAS)
    private fun List<String>?.canShowQR() = orEmpty().contains(VERIFICATION_METHOD_RECIPROCATE) && orEmpty().contains(VERIFICATION_METHOD_QR_CODE_SHOW)
    private fun List<String>?.canScanQR() = orEmpty().contains(VERIFICATION_METHOD_RECIPROCATE) && orEmpty().contains(VERIFICATION_METHOD_QR_CODE_SCAN)
}
