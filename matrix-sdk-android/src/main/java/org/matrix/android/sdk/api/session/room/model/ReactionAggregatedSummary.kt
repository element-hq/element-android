/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

data class ReactionAggregatedSummary(
        val key: String,                // "👍"
        val count: Int,                 // 8
        val addedByMe: Boolean,         // true
        val firstTimestamp: Long,       // unix timestamp
        val sourceEvents: List<String>,
        val localEchoEvents: List<String>
)
