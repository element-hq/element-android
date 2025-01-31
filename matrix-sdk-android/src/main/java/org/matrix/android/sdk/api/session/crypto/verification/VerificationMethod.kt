/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.verification

/**
 * Verification methods.
 */
enum class VerificationMethod {
    /**
     * Use it when your application supports the SAS verification method.
     */
    SAS,

    /**
     * Use it if your application is able to display QR codes.
     */
    QR_CODE_SHOW,

    /**
     * Use it if your application is able to scan QR codes.
     */
    QR_CODE_SCAN
}
