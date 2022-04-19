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
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoKey
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoKeyFactory

/**
 * Sent by both devices to send their ephemeral Curve25519 public key to the other device.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationKey(
        /**
         * the ID of the transaction that the message is part of
         */
        @Json(name = "transaction_id") override val transactionId: String? = null,

        /**
         * The device’s ephemeral public key, as an unpadded base64 string
         */
        @Json(name = "key") override val key: String? = null

) : SendToDeviceObject, VerificationInfoKey {

    companion object : VerificationInfoKeyFactory {
        override fun create(tid: String, pubKey: String): KeyVerificationKey {
            return KeyVerificationKey(tid, pubKey)
        }
    }

    override fun toSendToDeviceObject() = this
}
