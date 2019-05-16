/*
 * Copyright 2016 OpenMarket Ltd
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
package im.vector.matrix.android.internal.crypto.model.event

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an encrypted event content
 */
@JsonClass(generateAdapter = true)
data class EncryptedEventContent(

        /**
         * the used algorithm
         */
        @Json(name = "algorithm")
        var algorithm: String? = null,

        /**
         * The encrypted event
         */
        @Json(name = "ciphertext")
        var ciphertext: String? = null,

        /**
         * The device id
         */
        @Json(name = "device_id")
        var deviceId: String? = null,

        /**
         * the sender key
         */
        @Json(name = "sender_key")
        var senderKey: String? = null,

        /**
         * The session id
         */
        @Json(name = "session_id")
        var sessionId: String? = null
)