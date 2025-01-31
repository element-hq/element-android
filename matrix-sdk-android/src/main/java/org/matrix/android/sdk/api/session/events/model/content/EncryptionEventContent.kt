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

/**
 * Class representing an encrypted event content.
 */
@JsonClass(generateAdapter = true)
data class EncryptionEventContent(
        /**
         * Required. The encryption algorithm to be used to encrypt messages sent in this room. Must be 'm.megolm.v1.aes-sha2'.
         */
        @Json(name = "algorithm")
        val algorithm: String?,

        /**
         * How long the session should be used before changing it. 604800000 (a week) is the recommended default.
         */
        @Json(name = "rotation_period_ms")
        val rotationPeriodMs: Long? = null,

        /**
         * How many messages should be sent before changing the session. 100 is the recommended default.
         */
        @Json(name = "rotation_period_msgs")
        val rotationPeriodMsgs: Long? = null
)
