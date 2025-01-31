/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the EventType.STATE_ROOM_CANONICAL_ALIAS state event content.
 */
@JsonClass(generateAdapter = true)
data class RoomCanonicalAliasContent(
        /**
         * The canonical alias for the room. If not present, null, or empty the room should be considered to have no canonical alias.
         */
        @Json(name = "alias") val canonicalAlias: String? = null,

        /**
         * Alternative aliases the room advertises.
         * This list can have aliases despite the alias field being null, empty, or otherwise not present.
         */
        @Json(name = "alt_aliases") val alternativeAliases: List<String>? = null
)
