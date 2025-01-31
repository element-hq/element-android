/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

// RoomInviteState represents the state of a room that the user has been invited to.
@JsonClass(generateAdapter = true)
data class RoomInviteState(

        /**
         * List of state events (array of MXEvent).
         */
        @Json(name = "events") val events: List<Event> = emptyList()
)
