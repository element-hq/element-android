/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 *  Rooms can claim parents via the m.space.parent state event.
 * {
 *  "type": "m.space.parent",
 *  "state_key": "!space:example.com",
 *  "content": {
 *      "via": ["example.com"],
 *      "canonical": true,
 *  }
 * }
 */
@JsonClass(generateAdapter = true)
data class SpaceParentContent(
        /**
         * Key which gives a list of candidate servers that can be used to join the parent.
         * Parents where via is not present are ignored.
         */
        @Json(name = "via") val via: List<String>? = null,
        /**
         * Canonical determines whether this is the main parent for the space.
         * When a user joins a room with a canonical parent, clients may switch to view the room
         * in the context of that space, peeking into it in order to find other rooms and group them together.
         * In practice, well behaved rooms should only have one canonical parent, but given this is not enforced:
         * if multiple are present the client should select the one with the lowest room ID, as determined via a lexicographic utf-8 ordering.
         */
        @Json(name = "canonical") val canonical: Boolean? = false
)
