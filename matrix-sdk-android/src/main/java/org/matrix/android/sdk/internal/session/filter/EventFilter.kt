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
 * Represents "Filter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
internal data class EventFilter(
        /**
         * The maximum number of events to return.
         */
        @Json(name = "limit") val limit: Int? = null,
        /**
         * A list of senders IDs to include. If this list is absent then all senders are included.
         */
        @Json(name = "senders") val senders: List<String>? = null,
        /**
         * A list of sender IDs to exclude. If this list is absent then no senders are excluded.
         * A matching sender will be excluded even if it is listed in the 'senders' filter.
         */
        @Json(name = "not_senders") val notSenders: List<String>? = null,
        /**
         * A list of event types to include. If this list is absent then all event types are included.
         * A '*' can be used as a wildcard to match any sequence of characters.
         */
        @Json(name = "types") val types: List<String>? = null,
        /**
         * A list of event types to exclude. If this list is absent then no event types are excluded.
         * A matching type will be excluded even if it is listed in the 'types' filter.
         * A '*' can be used as a wildcard to match any sequence of characters.
         */
        @Json(name = "not_types") val notTypes: List<String>? = null
) {
    fun hasData(): Boolean {
        return limit != null ||
                senders != null ||
                notSenders != null ||
                types != null ||
                notTypes != null
    }
}
