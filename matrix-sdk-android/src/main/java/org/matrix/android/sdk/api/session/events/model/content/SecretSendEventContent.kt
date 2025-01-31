/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an encrypted event content.
 */
@JsonClass(generateAdapter = true)
data class SecretSendEventContent(
        @Json(name = "request_id") val requestId: String,
        @Json(name = "secret") val secretValue: String
)
