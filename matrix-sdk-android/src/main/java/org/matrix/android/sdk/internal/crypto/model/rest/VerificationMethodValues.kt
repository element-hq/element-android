/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model.rest

import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod

internal const val VERIFICATION_METHOD_SAS = "m.sas.v1"

// Qr code
// Ref: https://github.com/uhoreg/matrix-doc/blob/qr_key_verification/proposals/1543-qr_code_key_verification.md#verification-methods
internal const val VERIFICATION_METHOD_QR_CODE_SHOW = "m.qr_code.show.v1"
internal const val VERIFICATION_METHOD_QR_CODE_SCAN = "m.qr_code.scan.v1"
internal const val VERIFICATION_METHOD_RECIPROCATE = "m.reciprocate.v1"

internal fun VerificationMethod.toValue(): String {
    return when (this) {
        VerificationMethod.SAS -> VERIFICATION_METHOD_SAS
        VerificationMethod.QR_CODE_SCAN -> VERIFICATION_METHOD_QR_CODE_SCAN
        VerificationMethod.QR_CODE_SHOW -> VERIFICATION_METHOD_QR_CODE_SHOW
    }
}
