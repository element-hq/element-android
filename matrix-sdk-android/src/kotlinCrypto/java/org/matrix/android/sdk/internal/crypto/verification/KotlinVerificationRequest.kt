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

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.android.sdk.internal.crypto.verification.qrcode.QrCodeData
import org.matrix.android.sdk.internal.crypto.verification.qrcode.toEncodedString

internal class KotlinVerificationRequest(
        val requestId: String,
        val incoming: Boolean,
        val otherUserId: String,
        var state: EVerificationState,
        val ageLocalTs: Long
) {

    var roomId: String? = null
    var qrCodeData: QrCodeData? = null
    var targetDevices: List<String>? = null
    var requestInfo: ValidVerificationInfoRequest? = null
    var readyInfo: ValidVerificationInfoReady? = null
    var cancelCode: CancelCode? = null

//    fun requestId() = requestId
//
//    fun incoming() = incoming
//
//    fun otherUserId() = otherUserId
//
//    fun roomId() = roomId
//
//    fun targetDevices() = targetDevices
//
//    fun state() = state
//
//    fun ageLocalTs() = ageLocalTs

    fun otherDeviceId(): String? {
        return if (incoming) {
            requestInfo?.fromDevice
        } else {
            readyInfo?.fromDevice
        }
    }

    fun cancelCode(): CancelCode? = cancelCode

    /**
     * SAS is supported if I support it and the other party support it.
     */
    private fun isSasSupported(): Boolean {
        return requestInfo?.methods?.contains(VERIFICATION_METHOD_SAS).orFalse() &&
                readyInfo?.methods?.contains(VERIFICATION_METHOD_SAS).orFalse()
    }

    /**
     * Other can show QR code if I can scan QR code and other can show QR code.
     */
    private fun otherCanShowQrCode(): Boolean {
        return if (incoming) {
            requestInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SHOW).orFalse() &&
                    readyInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN).orFalse()
        } else {
            requestInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN).orFalse() &&
                    readyInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SHOW).orFalse()
        }
    }

    /**
     * Other can scan QR code if I can show QR code and other can scan QR code.
     */
    private fun otherCanScanQrCode(): Boolean {
        return if (incoming) {
            requestInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN).orFalse() &&
                    readyInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SHOW).orFalse()
        } else {
            requestInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SHOW).orFalse() &&
                    readyInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN).orFalse()
        }
    }

    fun qrCodeText() = qrCodeData?.toEncodedString()

    override fun toString(): String {
        return toPendingVerificationRequest().toString()
    }

    fun toPendingVerificationRequest(): PendingVerificationRequest {
        return PendingVerificationRequest(
                ageLocalTs = ageLocalTs,
                state = state,
                isIncoming = incoming,
                otherUserId = otherUserId,
                roomId = roomId,
                transactionId = requestId,
                cancelConclusion = cancelCode,
                isFinished = isFinished(),
                handledByOtherSession = state == EVerificationState.HandledByOtherSession,
                targetDevices = targetDevices,
                qrCodeText = qrCodeText(),
                isSasSupported = isSasSupported(),
                weShouldShowScanOption = otherCanShowQrCode(),
                weShouldDisplayQRCode = otherCanScanQrCode(),
                otherDeviceId = otherDeviceId()
        )
    }

    fun isFinished() = state == EVerificationState.Cancelled || state == EVerificationState.Done
}
