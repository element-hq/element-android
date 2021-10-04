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
        VerificationMethod.SAS          -> VERIFICATION_METHOD_SAS
        VerificationMethod.QR_CODE_SCAN -> VERIFICATION_METHOD_QR_CODE_SCAN
        VerificationMethod.QR_CODE_SHOW -> VERIFICATION_METHOD_QR_CODE_SHOW
    }
}
