/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.crypto.verification

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SCAN
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_QR_CODE_SHOW
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import java.util.UUID

/**
 * Stores current pending verification requests.
 */
data class PendingVerificationRequest(
        val ageLocalTs: Long,
        val isIncoming: Boolean = false,
        val localId: String = UUID.randomUUID().toString(),
        val otherUserId: String,
        val roomId: String?,
        val transactionId: String? = null,
        val requestInfo: ValidVerificationInfoRequest? = null,
        val readyInfo: ValidVerificationInfoReady? = null,
        val cancelConclusion: CancelCode? = null,
        val isSuccessful: Boolean = false,
        val handledByOtherSession: Boolean = false,
        // In case of to device it is sent to a list of devices
        val targetDevices: List<String>? = null
) {
    val isReady: Boolean = readyInfo != null
    val isSent: Boolean = transactionId != null

    val isFinished: Boolean = isSuccessful || cancelConclusion != null

    /**
     * SAS is supported if I support it and the other party support it.
     */
    fun isSasSupported(): Boolean {
        return requestInfo?.methods?.contains(VERIFICATION_METHOD_SAS).orFalse() &&
                readyInfo?.methods?.contains(VERIFICATION_METHOD_SAS).orFalse()
    }

    /**
     * Other can show QR code if I can scan QR code and other can show QR code.
     */
    fun otherCanShowQrCode(): Boolean {
        return if (isIncoming) {
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
    fun otherCanScanQrCode(): Boolean {
        return if (isIncoming) {
            requestInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN).orFalse() &&
                    readyInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SHOW).orFalse()
        } else {
            requestInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SHOW).orFalse() &&
                    readyInfo?.methods?.contains(VERIFICATION_METHOD_QR_CODE_SCAN).orFalse()
        }
    }
}
