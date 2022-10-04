/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
