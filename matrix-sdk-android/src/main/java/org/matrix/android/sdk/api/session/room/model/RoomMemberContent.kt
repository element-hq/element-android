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
import org.matrix.android.sdk.api.session.events.model.UnsignedData

/**
 * Class representing the EventType.STATE_ROOM_MEMBER state event content.
 */
@JsonClass(generateAdapter = true)
data class RoomMemberContent(
        @Json(name = "membership") val membership: Membership,
        @Json(name = "reason") val reason: String? = null,
        @Json(name = "displayname") val displayName: String? = null,
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        @Json(name = "is_direct") val isDirect: Boolean = false,
        @Json(name = "third_party_invite") val thirdPartyInvite: Invite? = null,
        @Json(name = "unsigned") val unsignedData: UnsignedData? = null
) {
    val safeReason
        get() = reason?.takeIf { it.isNotBlank() }
}
