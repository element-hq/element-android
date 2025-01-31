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

// InvitedRoomSync represents a room invitation during server sync v2.
@JsonClass(generateAdapter = true)
data class InvitedRoomSync(

        /**
         * The state of a room that the user has been invited to. These state events may only have the 'sender', 'type', 'state_key'
         * and 'content' keys present. These events do not replace any state that the client already has for the room, for example if
         * the client has archived the room. Instead the client should keep two separate copies of the state: the one from the 'invite_state'
         * and one from the archived 'state'. If the client joins the room then the current state will be given as a delta against the
         * archived 'state' not the 'invite_state'.
         */
        @Json(name = "invite_state") val inviteState: RoomInviteState? = null
)
