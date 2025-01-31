/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.session.events.model.Content

/**
 * Events can relates to other events, this object keeps a summary
 * of all events that are referencing the 'eventId' event via the RelationType.REFERENCE.
 */
data class ReferencesAggregatedSummary(
        val content: Content?,
        val sourceEvents: List<String>,
        val localEchos: List<String>
)
