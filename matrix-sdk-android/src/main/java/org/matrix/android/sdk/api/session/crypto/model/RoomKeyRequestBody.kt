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

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Class representing an room key request body content
 */
@JsonClass(generateAdapter = true)
data class RoomKeyRequestBody(
        @Json(name = "algorithm")
        val algorithm: String? = null,

        @Json(name = "room_id")
        val roomId: String? = null,

        @Json(name = "sender_key")
        val senderKey: String? = null,

        @Json(name = "session_id")
        val sessionId: String? = null
) {
    fun toJson(): String {
        return MoshiProvider.providesMoshi().adapter(RoomKeyRequestBody::class.java).toJson(this)
    }

    companion object {
        fun fromJson(json: String?): RoomKeyRequestBody? {
            return json?.let { MoshiProvider.providesMoshi().adapter(RoomKeyRequestBody::class.java).fromJson(it) }
        }
    }
}
