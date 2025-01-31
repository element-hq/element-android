/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.query

/**
 * To filter by Room category.
 * @see [org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams]
 */
enum class RoomCategoryFilter {
    /**
     * Get only the DM, i.e. the rooms referenced in `m.direct` account data.
     */
    ONLY_DM,

    /**
     * Get only the Room, not the DM, i.e. the rooms not referenced in `m.direct` account data.
     */
    ONLY_ROOMS,

    /**
     * Get the room with non-0 notifications.
     */
    ONLY_WITH_NOTIFICATIONS,
}
