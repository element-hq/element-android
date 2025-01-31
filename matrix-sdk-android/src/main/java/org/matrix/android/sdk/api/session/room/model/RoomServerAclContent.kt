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
 * Class representing the EventType.STATE_ROOM_SERVER_ACL state event content
 * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#m-room-server-acl
 */
@JsonClass(generateAdapter = true)
data class RoomServerAclContent(
        /**
         * True to allow server names that are IP address literals. False to deny.
         * Defaults to true if missing or otherwise not a boolean.
         * This is strongly recommended to be set to false as servers running with IP literal names are strongly
         * discouraged in order to require legitimate homeservers to be backed by a valid registered domain name.
         */
        @Json(name = "allow_ip_literals")
        val allowIpLiterals: Boolean = true,

        /**
         * The server names to allow in the room, excluding any port information. Wildcards may be used to cover
         * a wider range of hosts, where * matches zero or more characters and ? matches exactly one character.
         *
         * This defaults to an empty list when not provided, effectively disallowing every server.
         */
        @Json(name = "allow")
        val allowList: List<String> = emptyList(),

        /**
         * The server names to disallow in the room, excluding any port information. Wildcards may be used to cover
         * a wider range of hosts, where * matches zero or more characters and ? matches exactly one character.
         *
         * This defaults to an empty list when not provided.
         */
        @Json(name = "deny")
        val denyList: List<String> = emptyList()

) {
    companion object {
        const val ALL = "*"
    }
}
