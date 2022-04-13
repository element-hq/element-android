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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.SendToDeviceObject
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoCancel

/**
 * To device event sent by either party to cancel a key verification.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationCancel(
        /**
         * the transaction ID of the verification to cancel
         */
        @Json(name = "transaction_id")
        override val transactionId: String? = null,

        /**
         * machine-readable reason for cancelling, see #CancelCode
         */
        override val code: String? = null,

        /**
         * human-readable reason for cancelling.  This should only be used if the receiving client does not understand the code given.
         */
        override val reason: String? = null
) : SendToDeviceObject, VerificationInfoCancel {

    companion object {
        fun create(tid: String, cancelCode: CancelCode): KeyVerificationCancel {
            return KeyVerificationCancel(
                    tid,
                    cancelCode.value,
                    cancelCode.humanReadable
            )
        }
    }

    override fun toSendToDeviceObject() = this
}
