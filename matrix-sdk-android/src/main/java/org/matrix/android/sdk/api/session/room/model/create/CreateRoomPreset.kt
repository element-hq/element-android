/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CreateRoomPreset {
    @Json(name = "private_chat")
    PRESET_PRIVATE_CHAT,

    @Json(name = "public_chat")
    PRESET_PUBLIC_CHAT,

    @Json(name = "trusted_private_chat")
    PRESET_TRUSTED_PRIVATE_CHAT
}
