/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
 * Class representing the EventType.STATE_ROOM_JOIN_RULES state event content.
 */
@JsonClass(generateAdapter = true)
data class RoomJoinRulesContent(
        @Json(name = "join_rule") val joinRulesStr: String? = null,
        /**
         * If the allow key is an empty list (or not a list at all),
         * then no users are allowed to join without an invite.
         * Each entry is expected to be an object with the following keys:
         */
        @Json(name = "allow") val allowList: List<RoomJoinRulesAllowEntry>? = null
) {
    val joinRules: RoomJoinRules? = when (joinRulesStr) {
        "public" -> RoomJoinRules.PUBLIC
        "invite" -> RoomJoinRules.INVITE
        "knock" -> RoomJoinRules.KNOCK
        "private" -> RoomJoinRules.PRIVATE
        "restricted" -> RoomJoinRules.RESTRICTED
        else -> {
            Timber.w("Invalid value for RoomJoinRules: `$joinRulesStr`")
            null
        }
    }
}
