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
 * Represents the membership of a user on a room.
 */
@JsonClass(generateAdapter = false)
enum class Membership {
    NONE,
    @Json(name = "invite") INVITE,
    @Json(name = "join") JOIN,
    @Json(name = "knock") KNOCK,
    @Json(name = "leave") LEAVE,
    @Json(name = "ban") BAN;

    fun isLeft(): Boolean {
        return this == KNOCK || this == LEAVE || this == BAN
    }

    fun isActive(): Boolean {
        return activeMemberships().contains(this)
    }

    companion object {
        fun activeMemberships(): List<Membership> {
            return listOf(INVITE, JOIN)
        }

        fun all(): List<Membership> {
            return values().asList()
        }
    }
}
