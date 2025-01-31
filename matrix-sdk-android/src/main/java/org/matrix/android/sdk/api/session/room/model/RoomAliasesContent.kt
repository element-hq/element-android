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
 * Class representing the EventType.STATE_ROOM_ALIASES state event content
 * Note that this Event has been deprecated, see
 * - https://matrix.org/docs/spec/client_server/r0.6.1#historical-events
 * - https://github.com/matrix-org/matrix-doc/pull/2432
 */
@JsonClass(generateAdapter = true)
data class RoomAliasesContent(
        @Json(name = "aliases") val aliases: List<String> = emptyList()
)
