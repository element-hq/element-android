/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.session.crypto.verification

/**
 * Stores current pending verification requests.
 */
data class PendingVerificationRequest(
        val ageLocalTs: Long,
        val state: EVerificationState,
        val isIncoming: Boolean = false,
//        val localId: String = UUID.randomUUID().toString(),
        val otherUserId: String,
        val otherDeviceId: String?,
        // in case of verification via room, it will be not null
        val roomId: String?,
        val transactionId: String, // ? = null,
//        val requestInfo: ValidVerificationInfoRequest? = null,
//        val readyInfo: ValidVerificationInfoReady? = null,
        val cancelConclusion: CancelCode? = null,
        val isFinished: Boolean = false,
        val handledByOtherSession: Boolean = false,
        // In case of to device it is sent to a list of devices
        val targetDevices: List<String>? = null,
        // if available store here the qr code to show
        val qrCodeText: String? = null,
        val isSasSupported: Boolean = false,
        val weShouldShowScanOption: Boolean = false,
        val weShouldDisplayQRCode: Boolean = false,

        ) {
//    val isReady: Boolean = readyInfo != null
//
}
