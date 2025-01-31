/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.session.events.model.Content

data class EditAggregatedSummary(
        val latestContent: Content? = null,
        // The list of the eventIDs used to build the summary (might be out of sync if chunked received from message chunk)
        val sourceEvents: List<String>,
        val localEchos: List<String>,
        val lastEditTs: Long = 0
)
