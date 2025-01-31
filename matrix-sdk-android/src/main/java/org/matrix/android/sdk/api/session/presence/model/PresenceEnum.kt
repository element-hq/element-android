/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.presence.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PresenceEnum(val value: String) {
    @Json(name = "online")
    ONLINE("online"),

    @Json(name = "offline")
    OFFLINE("offline"),

    @Json(name = "unavailable")
    UNAVAILABLE("unavailable"),

    @Json(name = "org.matrix.msc3026.busy")
    BUSY("busy");

    companion object {
        fun from(s: String): PresenceEnum? = values().find { it.value == s }
    }
}
