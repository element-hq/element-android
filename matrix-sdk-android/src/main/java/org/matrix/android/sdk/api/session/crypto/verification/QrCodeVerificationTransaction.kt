/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.verification

interface QrCodeVerificationTransaction : VerificationTransaction {

    /**
     * To use to display a qr code, for the other user to scan it.
     */
    val qrCodeText: String?

    /**
     * Call when you have scan the other user QR code.
     */
    fun userHasScannedOtherQrCode(otherQrCodeText: String)

    /**
     * Call when you confirm that other user has scanned your QR code.
     */
    fun otherUserScannedMyQrCode()

    /**
     * Call when you do not confirm that other user has scanned your QR code.
     */
    fun otherUserDidNotScannedMyQrCode()
}
