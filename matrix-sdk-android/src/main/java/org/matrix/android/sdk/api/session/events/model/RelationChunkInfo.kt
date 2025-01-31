/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model

import com.squareup.moshi.JsonClass

/**
 * <code>
 *      {
 *          "type": "m.reaction",
 *          "key": "👍",
 *          "count": 3
 *      }
 * </code>
 */

@JsonClass(generateAdapter = true)
data class RelationChunkInfo(
        val type: String,
        val key: String,
        val count: Int
)
