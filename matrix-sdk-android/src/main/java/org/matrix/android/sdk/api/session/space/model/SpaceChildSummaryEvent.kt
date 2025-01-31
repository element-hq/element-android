/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content

@JsonClass(generateAdapter = true)
data class SpaceChildSummaryEvent(
        @Json(name = "type") val type: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "content") val content: Content? = null,
        @Json(name = "sender") val senderId: String? = null,
        @Json(name = "origin_server_ts") val originServerTs: Long? = null,
)
