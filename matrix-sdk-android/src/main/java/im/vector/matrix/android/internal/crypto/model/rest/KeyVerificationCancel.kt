/*
 * Copyright 2019 New Vector Ltd
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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.internal.crypto.verification.VerifInfoCancel

/**
 * To device event sent by either party to cancel a key verification.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationCancel(
        /**
         * the transaction ID of the verification to cancel
         */
        @Json(name = "transaction_id")
        override val transactionID: String? = null,

        /**
         * machine-readable reason for cancelling, see #CancelCode
         */
        override var code: String? = null,

        /**
         * human-readable reason for cancelling.  This should only be used if the receiving client does not understand the code given.
         */
        override var reason: String? = null
) : SendToDeviceObject, VerifInfoCancel {

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

    override fun isValid(): Boolean {
        if (transactionID.isNullOrBlank() || code.isNullOrBlank()) {
            return false
        }
        return true
    }
}
