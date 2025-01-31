/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
