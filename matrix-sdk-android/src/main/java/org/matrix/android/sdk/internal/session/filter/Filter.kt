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
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Class which can be parsed to a filter json string. Used for POST and GET
 * Have a look here for further information:
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
internal data class Filter(
        /**
         * List of event fields to include. If this list is absent then all fields are included. The entries may
         * include '.' characters to indicate sub-fields. So ['content.body'] will include the 'body' field of the
         * 'content' object. A literal '.' character in a field name may be escaped using a '\'. A server may
         * include more fields than were requested.
         */
        @Json(name = "event_fields") val eventFields: List<String>? = null,
        /**
         * The format to use for events. 'client' will return the events in a format suitable for clients.
         * 'federation' will return the raw event as received over federation. The default is 'client'. One of: ["client", "federation"]
         */
        @Json(name = "event_format") val eventFormat: String? = null,
        /**
         * The presence updates to include.
         */
        @Json(name = "presence") val presence: EventFilter? = null,
        /**
         * The user account data that isn't associated with rooms to include.
         */
        @Json(name = "account_data") val accountData: EventFilter? = null,
        /**
         * Filters to be applied to room data.
         */
        @Json(name = "room") val room: RoomFilter? = null
) {

    fun toJSONString(): String {
        return MoshiProvider.providesMoshi().adapter(Filter::class.java).toJson(this)
    }
}
