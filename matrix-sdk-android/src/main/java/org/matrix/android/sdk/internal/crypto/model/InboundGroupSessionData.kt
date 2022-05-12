/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InboundGroupSessionData(

        /** The room in which this session is used. */
        @Json(name = "room_id")
        var roomId: String? = null,

        /** The base64-encoded curve25519 key of the sender. */
        @Json(name = "sender_key")
        var senderKey: String? = null,

        /** Other keys the sender claims. */
        @Json(name = "keys_claimed")
        var keysClaimed: Map<String, String>? = null,

        /** Devices which forwarded this session to us (normally emty). */
        @Json(name = "forwarding_curve25519_key_chain")
        var forwardingCurve25519KeyChain: List<String>? = emptyList(),

        /** Not yet used, will be in backup v2
        val untrusted?: Boolean = false */

        /**
         * Flag that indicates whether or not the current inboundSession will be shared to
         *invited users to decrypt past messages
         */
        @Json(name = "shared_history")
        val sharedHistory: Boolean = false,

)
