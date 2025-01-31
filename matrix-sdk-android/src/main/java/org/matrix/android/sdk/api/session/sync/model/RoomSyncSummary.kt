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

@JsonClass(generateAdapter = true)
data class RoomSyncSummary(

        /**
         * Present only if the room has no m.room.name or m.room.canonical_alias.
         *
         *
         * Lists the mxids of the first 5 members in the room who are currently joined or invited (ordered by stream ordering as seen on the server,
         * to avoid it jumping around if/when topological order changes). As the heroes’ membership status changes, the list changes appropriately
         * (sending the whole new list in the next /sync response). This list always excludes the current logged in user. If there are no joined or
         * invited users, it lists the parted and banned ones instead.  Servers can choose to send more or less than 5 members if they must, but 5
         * seems like a good enough number for most naming purposes.  Clients should use all the provided members to name the room, but may truncate
         * the list if helpful for UX
         */
        @Json(name = "m.heroes") val heroes: List<String> = emptyList(),

        /**
         * The number of m.room.members in state 'joined' (including the syncing user) (can be null).
         */
        @Json(name = "m.joined_member_count") val joinedMembersCount: Int? = null,

        /**
         * The number of m.room.members in state 'invited' (can be null).
         */
        @Json(name = "m.invited_member_count") val invitedMembersCount: Int? = null
)
