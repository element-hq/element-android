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
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

/**
 * Class representing an encrypted event content.
 */
@JsonClass(generateAdapter = true)
data class EncryptedEventContent(

        /**
         * The used algorithm.
         */
        @Json(name = "algorithm")
        val algorithm: String? = null,

        /**
         * The encrypted event.
         */
        @Json(name = "ciphertext")
        val ciphertext: String? = null,

        /**
         * The device id.
         */
        @Json(name = "device_id")
        val deviceId: String? = null,

        /**
         * The sender key.
         */
        @Json(name = "sender_key")
        val senderKey: String? = null,

        /**
         * The session id.
         */
        @Json(name = "session_id")
        val sessionId: String? = null,

        // Relation context is in clear in encrypted message
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent? = null
)
