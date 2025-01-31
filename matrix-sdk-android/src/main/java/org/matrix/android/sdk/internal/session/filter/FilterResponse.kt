/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.filter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents the body which is the response when creating a filter on the server
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
internal data class FilterResponse(
        /**
         * Required. The ID of the filter that was created. Cannot start with a { as this character
         * is used to determine if the filter provided is inline JSON or a previously declared
         * filter by homeservers on some APIs.
         */
        @Json(name = "filter_id") val filterId: String
)
