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
 * Enum for [RoomJoinRulesContent] : https://matrix.org/docs/spec/client_server/r0.4.0#m-room-join-rules
 */
@JsonClass(generateAdapter = false)
enum class RoomJoinRules(val value: String) {
    @Json(name = "public") PUBLIC("public"),
    @Json(name = "invite") INVITE("invite"),
    @Json(name = "knock") KNOCK("knock"),
    @Json(name = "private") PRIVATE("private"),
    @Json(name = "restricted") RESTRICTED("restricted")
}
