/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.query

/**
 * Filter to be used to do room queries regarding the space hierarchy.
 * @see [org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams]
 */
sealed interface SpaceFilter {
    /**
     * Used to get all the rooms that are not in any space.
     */
    object OrphanRooms : SpaceFilter

    /**
     * Used to get all the rooms that have the provided space in their parent hierarchy.
     */
    data class ActiveSpace(val spaceId: String) : SpaceFilter

    /**
     * Used to get all the rooms that do not have the provided space in their parent hierarchy.
     */
    data class ExcludeSpace(val spaceId: String) : SpaceFilter

    /**
     * Used to apply no filtering to the space.
     */
    object NoFilter : SpaceFilter
}

/**
 * Return a [SpaceFilter.ActiveSpace] if the String is not null, or [SpaceFilter.OrphanRooms].
 */
fun String?.toActiveSpaceOrOrphanRooms(): SpaceFilter = this?.let { SpaceFilter.ActiveSpace(it) } ?: SpaceFilter.OrphanRooms

/**
 * Return a [SpaceFilter.ActiveSpace] if the String is not null, or [SpaceFilter.NoFilter].
 */
fun String?.toActiveSpaceOrNoFilter(): SpaceFilter = this?.let { SpaceFilter.ActiveSpace(it) } ?: SpaceFilter.NoFilter
