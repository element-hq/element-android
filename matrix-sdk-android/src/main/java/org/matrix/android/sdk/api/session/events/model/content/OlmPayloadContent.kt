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
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Class representing the OLM payload content
 */
@JsonClass(generateAdapter = true)
data class OlmPayloadContent(
        /**
         * The room id
         */
        @Json(name = "room_id")
        val roomId: String? = null,

        /**
         * The sender
         */
        @Json(name = "sender")
        val sender: String? = null,

        /**
         * The recipient
         */
        @Json(name = "recipient")
        val recipient: String? = null,

        /**
         * the recipient keys
         */
        @Json(name = "recipient_keys")
        val recipientKeys: Map<String, String>? = null,

        /**
         * The keys
         */
        @Json(name = "keys")
        val keys: Map<String, String>? = null
) {
    fun toJsonString(): String {
        return MoshiProvider.providesMoshi().adapter(OlmPayloadContent::class.java).toJson(this)
    }

    companion object {
        fun fromJsonString(str: String): OlmPayloadContent? {
            return MoshiProvider.providesMoshi().adapter(OlmPayloadContent::class.java).fromJson(str)
        }
    }
}
