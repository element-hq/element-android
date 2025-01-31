/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room

/**
 * Enum to sort room list.
 */
enum class RoomSortOrder {
    /**
     * Sort room list by room ascending name.
     */
    NAME,

    /**
     * Sort room list by room descending last activity.
     */
    ACTIVITY,

    /**
     * Sort room list by room priority and last activity: favorite room first, low priority room last,
     * then descending last activity.
     */
    PRIORITY_AND_ACTIVITY,

    /**
     * Do not sort room list. Useful if the order does not matter. Order can be indeterminate.
     */
    NONE
}
