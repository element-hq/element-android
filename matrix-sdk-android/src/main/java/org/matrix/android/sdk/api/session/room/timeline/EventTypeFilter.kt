/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.timeline

data class EventTypeFilter(
        /**
         * Allowed event type.
         */
        val eventType: String,
        /**
         * Allowed state key. Set null if you want to allow all events,
         * otherwise allowed events will be filtered according to the given stateKey.
         */
        val stateKey: String?
)
