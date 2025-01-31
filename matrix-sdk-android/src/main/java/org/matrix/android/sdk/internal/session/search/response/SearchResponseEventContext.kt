/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.JsonDict

@JsonClass(generateAdapter = true)
internal data class SearchResponseEventContext(
        // Events just before the result.
        @Json(name = "events_before")
        val eventsBefore: List<Event>,
        // Events just after the result.
        @Json(name = "events_after")
        val eventsAfter: List<Event>,
        // Pagination token for the start of the chunk
        @Json(name = "start")
        val start: String? = null,
        // Pagination token for the end of the chunk
        @Json(name = "end")
        val end: String? = null,
        // The historic profile information of the users that sent the events returned. The string key is the user ID for which the profile belongs to.
        @Json(name = "profile_info")
        val profileInfo: Map<String, JsonDict>? = null
)
