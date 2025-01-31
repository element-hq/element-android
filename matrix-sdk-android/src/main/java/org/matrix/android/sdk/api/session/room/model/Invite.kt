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
 * Subclass representing a search API response.
 */
@JsonClass(generateAdapter = true)
data class Invite(
        @Json(name = "display_name") val displayName: String,
        @Json(name = "signed") val signed: Signed

)
