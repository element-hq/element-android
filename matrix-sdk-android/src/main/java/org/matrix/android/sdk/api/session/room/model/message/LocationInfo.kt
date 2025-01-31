/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocationInfo(
        /**
         * Required. RFC5870 formatted geo uri 'geo:latitude,longitude;u=uncertainty' like 'geo:40.05,29.24;u=30' representing this location.
         */
        @Json(name = "uri") val geoUri: String? = null,

        /**
         * Required. A description of the location e.g. 'Big Ben, London, UK', or some kind
         * of content description for accessibility e.g. 'location attachment'.
         */
        @Json(name = "description") val description: String? = null
)
