/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.livelocation

import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent

/**
 * Aggregation info concerning a live location share.
 */
data class LiveLocationShareAggregatedSummary(
        val userId: String?,
        /**
         * Indicate whether the live is currently running.
         */
        val isActive: Boolean?,
        val endOfLiveTimestampMillis: Long?,
        val lastLocationDataContent: MessageBeaconLocationDataContent?,
)
