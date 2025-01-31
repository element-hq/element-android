/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.accountdata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content

/**
 * This is a simplified Event with just a type and a content.
 * Currently used types are defined in [UserAccountDataTypes]
 */
@JsonClass(generateAdapter = true)
data class UserAccountDataEvent(
        @Json(name = "type") val type: String,
        @Json(name = "content") val content: Content
)
