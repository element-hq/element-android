/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.typing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TypingBody(
        // Required. Whether the user is typing or not. If false, the timeout key can be omitted.
        @Json(name = "typing")
        val typing: Boolean,
        // The length of time in milliseconds to mark this user as typing.
        @Json(name = "timeout")
        val timeout: Int?
)
