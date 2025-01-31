/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class JsonPusherData(
        /**
         * Required if kind is http. The URL to use to send notifications to.
         * MUST be an HTTPS URL with a path of /_matrix/push/v1/notify.
         */
        @Json(name = "url")
        val url: String? = null,

        /**
         * The format to send notifications in to Push Gateways if the kind is http.
         * Currently the only format available is 'event_id_only'.
         */
        @Json(name = "format")
        val format: String? = null,

        @Json(name = "brand")
        val brand: String? = null
)
