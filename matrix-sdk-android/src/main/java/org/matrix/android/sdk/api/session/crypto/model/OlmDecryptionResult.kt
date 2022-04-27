/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict

/**
 * This class represents the decryption result.
 */
@JsonClass(generateAdapter = true)
data class OlmDecryptionResult(
        /**
         * The decrypted payload (with properties 'type', 'content')
         */
        @Json(name = "payload") val payload: JsonDict? = null,

        /**
         * keys that the sender of the event claims ownership of:
         * map from key type to base64-encoded key.
         */
        @Json(name = "keysClaimed") val keysClaimed: Map<String, String>? = null,

        /**
         * The curve25519 key that the sender of the event is known to have ownership of.
         */
        @Json(name = "senderKey") val senderKey: String? = null,

        /**
         * Devices which forwarded this session to us (normally empty).
         */
        @Json(name = "forwardingCurve25519KeyChain") val forwardingCurve25519KeyChain: List<String>? = null
)
