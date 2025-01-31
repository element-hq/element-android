/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.timeline

// TODO Move to internal, strange?
data class TimelineEventFilters(
        /**
         * A flag to filter edit events.
         */
        val filterEdits: Boolean = false,
        /**
         * A flag to filter redacted events.
         */
        val filterRedacted: Boolean = false,
        /**
         * A flag to filter useless events, such as membership events without any change.
         */
        val filterUseless: Boolean = false,
        /**
         * A flag to filter by types. It should be used with [allowedTypes] field.
         */
        val filterTypes: Boolean = false,
        /**
         * If [filterTypes] is true, the list of types allowed by the list.
         */
        val allowedTypes: List<EventTypeFilter> = emptyList()
)
