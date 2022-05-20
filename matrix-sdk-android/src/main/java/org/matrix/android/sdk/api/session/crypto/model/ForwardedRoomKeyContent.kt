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

/**
 * Class representing the forward room key request body content
 * Ref: https://matrix.org/docs/spec/client_server/latest#m-forwarded-room-key
 */
@JsonClass(generateAdapter = true)
data class ForwardedRoomKeyContent(
        /**
         * Required. The encryption algorithm the key in this event is to be used with.
         */
        @Json(name = "algorithm")
        val algorithm: String? = null,

        /**
         * Required. The room where the key is used.
         */
        @Json(name = "room_id")
        val roomId: String? = null,

        /**
         * Required. The Curve25519 key of the device which initiated the session originally.
         */
        @Json(name = "sender_key")
        val senderKey: String? = null,

        /**
         * Required. The ID of the session that the key is for.
         */
        @Json(name = "session_id")
        val sessionId: String? = null,

        /**
         * Required. The key to be exchanged.
         */
        @Json(name = "session_key")
        val sessionKey: String? = null,

        /**
         * Required. Chain of Curve25519 keys. It starts out empty, but each time the key is forwarded to another device,
         * the previous sender in the chain is added to the end of the list. For example, if the key is forwarded
         * from A to B to C, this field is empty between A and B, and contains A's Curve25519 key between B and C.
         */
        @Json(name = "forwarding_curve25519_key_chain")
        val forwardingCurve25519KeyChain: List<String>? = null,

        /**
         * Required. The Ed25519 key of the device which initiated the session originally. It is 'claimed' because the
         * receiving device has no way to tell that the original room_key actually came from a device which owns the
         * private part of this key unless they have done device verification.
         */
        @Json(name = "sender_claimed_ed25519_key")
        val senderClaimedEd25519Key: String? = null
)
