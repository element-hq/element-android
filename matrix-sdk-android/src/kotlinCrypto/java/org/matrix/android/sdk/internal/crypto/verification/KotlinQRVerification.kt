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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.QRCodeVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.internal.crypto.verification.qrcode.QrCodeData
import org.matrix.android.sdk.internal.crypto.verification.qrcode.toEncodedString

internal class KotlinQRVerification(
        private val channel: Channel<VerificationIntent>,
        var qrCodeData: QrCodeData?,
        override val method: VerificationMethod,
        override val transactionId: String,
        override val otherUserId: String,
        override val otherDeviceId: String?,
        override val isIncoming: Boolean,
        var state: QRCodeVerificationState,
        val isToDevice: Boolean
) : QrCodeVerificationTransaction {

    override fun state() = state

    override val qrCodeText: String?
        get() = qrCodeData?.toEncodedString()
//
//    var userMSKKeyToTrust: String? = null
//    var deviceKeysToTrust = mutableListOf<String>()

//    override suspend fun userHasScannedOtherQrCode(otherQrCodeText: String) {
//        TODO("Not yet implemented")
//    }

    override suspend fun otherUserScannedMyQrCode() {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                VerificationIntent.ActionConfirmCodeWasScanned(otherUserId, transactionId, deferred)
        )
        deferred.await()
    }

    override suspend fun otherUserDidNotScannedMyQrCode() {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                // TODO what cancel code??
                VerificationIntent.ActionCancel(transactionId, deferred)
        )
        deferred.await()
    }

    override suspend fun cancel() {
        cancel(CancelCode.User)
    }

    override suspend fun cancel(code: CancelCode) {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                VerificationIntent.ActionCancel(transactionId, deferred)
        )
        deferred.await()
    }

    override fun isToDeviceTransport() = isToDevice
}
