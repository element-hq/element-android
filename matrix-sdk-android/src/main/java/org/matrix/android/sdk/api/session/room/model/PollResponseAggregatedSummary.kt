/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model

data class PollResponseAggregatedSummary(
        val aggregatedContent: PollSummaryContent? = null,
        // If set the poll is closed (Clients SHOULD NOT consider responses after the close event)
        val closedTime: Long? = null,
        // Clients SHOULD validate that the option in the relationship is a valid option, and ignore the response if invalid
        val nbOptions: Int = 0,
        // The list of the eventIDs used to build the summary (might be out of sync if chunked received from message chunk)
        val sourceEvents: List<String>,
        val localEchos: List<String>
)
