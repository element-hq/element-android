/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.model.rest

import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod

internal const val VERIFICATION_METHOD_SAS = "m.sas.v1"
internal const val VERIFICATION_METHOD_SCAN = "m.qr_code.scan.v1"

internal fun VerificationMethod.toValue(): String {
    return when (this) {
        VerificationMethod.SAS  -> VERIFICATION_METHOD_SAS
        VerificationMethod.SCAN -> VERIFICATION_METHOD_SCAN
    }
}

internal val supportedVerificationMethods =
        listOf(
                VERIFICATION_METHOD_SAS,
                VERIFICATION_METHOD_SCAN
        )
