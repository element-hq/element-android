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
import im.vector.matrix.android.internal.crypto.verification.VerificationInfo

/**
 * Requests a key verification with another user's devices.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationRequest(

        @Json(name = "from_device")
        val fromDevice: String,
        /** The verification methods supported by the sender. */
        val methods: List<String> = listOf(KeyVerificationStart.VERIF_METHOD_SAS),
        /**
         *  The POSIX timestamp in milliseconds for when the request was made.
         *  If the request is in the future by more than 5 minutes or more than 10 minutes in the past,
         *  the message should be ignored by the receiver.
         */
        val timestamp: Int,

        @Json(name = "transaction_id")
        var transactionID: String? = null

) : SendToDeviceObject, VerificationInfo {

    override fun isValid(): Boolean {
        // TODO
        return true
    }
}
