/*
 * Copyright (c) 2022 New Vector Ltd
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

import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState

class KotlinQRVerification(
        override val qrCodeText: String?,
        override val state: VerificationTxState,
        override val method: VerificationMethod,
        override val transactionId: String,
        override val otherUserId: String,
        override val otherDeviceId: String?,
        override val isIncoming: Boolean) : QrCodeVerificationTransaction {

    override suspend fun userHasScannedOtherQrCode(otherQrCodeText: String) {
        TODO("Not yet implemented")
    }

    override suspend fun otherUserScannedMyQrCode() {
        TODO("Not yet implemented")
    }

    override suspend fun otherUserDidNotScannedMyQrCode() {
        TODO("Not yet implemented")
    }

    override suspend fun cancel() {
        TODO("Not yet implemented")
    }

    override suspend fun cancel(code: CancelCode) {
        TODO("Not yet implemented")
    }

    override fun isToDeviceTransport(): Boolean {
        TODO("Not yet implemented")
    }
}
