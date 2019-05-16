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
package im.vector.matrix.android.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the forward room key request body content
 */
@JsonClass(generateAdapter = true)
data class ForwardedRoomKeyContent(

        @Json(name = "algorithm")
        var algorithm: String? = null,

        @Json(name = "room_id")
        var roomId: String? = null,

        @Json(name = "sender_key")
        var senderKey: String? = null,

        @Json(name = "session_id")
        var sessionId: String? = null,

        @Json(name = "session_key")
        var sessionKey: String? = null,

        @Json(name = "forwarding_curve25519_key_chain")
        var forwardingCurve25519KeyChain: List<String>? = null,

        @Json(name = "sender_claimed_ed25519_key")
        var senderClaimedEd25519Key: String? = null
)