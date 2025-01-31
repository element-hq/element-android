/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Class representing an room key request body content.
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
