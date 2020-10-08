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
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest

internal interface VerificationInfoRequest : VerificationInfo<ValidVerificationInfoRequest> {

    /**
     * Required. The device ID which is initiating the request.
     */
    val fromDevice: String?

    /**
     * Required. The verification methods supported by the sender.
     */
    val methods: List<String>?

    /**
     * The POSIX timestamp in milliseconds for when the request was made.
     * If the request is in the future by more than 5 minutes or more than 10 minutes in the past,
     * the message should be ignored by the receiver.
     */
    val timestamp: Long?

    override fun asValidObject(): ValidVerificationInfoRequest? {
        // FIXME No check on Timestamp?
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validFromDevice = fromDevice?.takeIf { it.isNotEmpty() } ?: return null
        val validMethods = methods?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoRequest(
                validTransactionId,
                validFromDevice,
                validMethods,
                timestamp
        )
    }
}
