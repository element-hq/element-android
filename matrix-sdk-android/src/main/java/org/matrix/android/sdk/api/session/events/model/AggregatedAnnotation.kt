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
 *     {
 *       "chunk": [
 *            {
 *                "type": "m.reaction",
 *                "key": "👍",
 *                "count": 3
 *            }
 *           ],
 *           "limited": false,
 *           "count": 1
 *     },
 * </code>
 */

@JsonClass(generateAdapter = true)
data class AggregatedAnnotation(
        override val limited: Boolean? = false,
        override val count: Int? = 0,
        val chunk: List<RelationChunkInfo>? = null

) : UnsignedRelationInfo
