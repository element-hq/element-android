/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Class representing the OLM payload content.
 */
@JsonClass(generateAdapter = true)
data class OlmPayloadContent(
        /**
         * The room id.
         */
        @Json(name = "room_id")
        val roomId: String? = null,

        /**
         * The sender.
         */
        @Json(name = "sender")
        val sender: String? = null,

        /**
         * The recipient.
         */
        @Json(name = "recipient")
        val recipient: String? = null,

        /**
         * The recipient keys.
         */
        @Json(name = "recipient_keys")
        val recipientKeys: Map<String, String>? = null,

        /**
         * The keys.
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
