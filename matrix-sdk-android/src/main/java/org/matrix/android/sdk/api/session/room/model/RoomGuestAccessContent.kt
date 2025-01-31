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
import timber.log.Timber

/**
 * Class representing the EventType.STATE_ROOM_GUEST_ACCESS state event content
 * Ref: https://matrix.org/docs/spec/client_server/latest#m-room-guest-access
 */
@JsonClass(generateAdapter = true)
data class RoomGuestAccessContent(
        // Required. Whether guests can join the room. One of: ["can_join", "forbidden"]
        @Json(name = "guest_access") val guestAccessStr: String? = null
) {
    val guestAccess: GuestAccess? = GuestAccess.values()
            .find { it.value == guestAccessStr }
            ?: run {
                Timber.w("Invalid value for GuestAccess: `$guestAccessStr`")
                null
            }
}

@JsonClass(generateAdapter = false)
enum class GuestAccess(val value: String) {
    @Json(name = "can_join") CanJoin("can_join"),
    @Json(name = "forbidden") Forbidden("forbidden")
}
