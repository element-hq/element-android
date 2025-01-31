/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an sharekey content.
 */
@JsonClass(generateAdapter = true)
data class RoomKeyContent(

        @Json(name = "algorithm")
        val algorithm: String? = null,

        @Json(name = "room_id")
        val roomId: String? = null,

        @Json(name = "session_id")
        val sessionId: String? = null,

        @Json(name = "session_key")
        val sessionKey: String? = null,

        // should be a Long but it is sometimes a double
        @Json(name = "chain_index")
        val chainIndex: Any? = null,

        /**
         * MSC3061 Identifies keys that were sent when the room's visibility setting was set to world_readable or shared.
         */
        @Json(name = "org.matrix.msc3061.shared_history")
        val sharedHistory: Boolean? = false

)
